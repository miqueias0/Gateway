package br.com.mike.gateway.service;

import br.com.mike.gateway.recordys.ApiPorta;
import br.com.mike.gateway.repository.ApiPortaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;

@Service
public class RedisService {

    @Autowired
    private ApiPortaRepository repository;

    public RedisService() {
        obterDados();
    }

    public ApiPorta save(ApiPorta apiPorta) {
        return repository.save(apiPorta);
    }

    public ApiPorta findAllById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<ApiPorta> findAllByEndpoint(String endpoint) {
        List<ApiPorta> portas = repository.findAll();

        portas.removeIf(x -> {
            boolean verificarNumero = false;
            try {
                if (x.getEndpoint().split("_").length > 1) {
                    Long.parseLong(x.getEndpoint().split("_")[1]);
                }
                verificarNumero = x.getEndpoint().split("_").length <= 2;
            } catch (Exception ex) {

            }
            if (x.getEndpoint().split("_")[0].equalsIgnoreCase(endpoint) && verificarNumero) {
                return false;
            }
            return true;
        });

        return portas;
    }

    private Thread thread;

    private void obterDados() {
        thread = new Thread((Runnable) () -> {
            RestTemplate restTemplate = new RestTemplate();
            while (true) {
                List<ApiPorta> portas = null;
                try {
                    portas = requisicao(restTemplate);
                } catch (Exception e) {
                }
                if (portas != null) {
                    List<Long> list = new java.util.ArrayList<>(repository.findAll().stream().map(ApiPorta::getPorta).toList());
                    for (ApiPorta porta : portas) {
                        if (porta.getDataUltimaRequisicao() != null
                                && (new Date().getTime() - porta.getDataUltimaRequisicao().getTime()) >= 1000 * 60 * 10
                                && porta.getQuantidadeRequisicao() == 0) {
                            restTemplate.exchange(
                                    "http://localhost:8080/api/docker/derrubar?container=" + porta.getEndpoint(),
                                    HttpMethod.GET,
                                    null,
                                    new ParameterizedTypeReference<String>() {
                                    }
                            ).getBody();
                            continue;
                        }
                        repository.save(porta);
                        list.removeIf(x -> x.compareTo(porta.getPorta()) == 0);
                    }
                    repository.deleteAllById(list);
                }
            }
        });
        thread.start();
    }

    private List<ApiPorta> requisicao(RestTemplate restTemplate) throws Exception {
        List<ApiPorta> portas = null;
        try {
            Thread.sleep(1000);
            portas = restTemplate.exchange(
                    "http://localhost:8080/api/docker/obterLista",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ApiPorta>>() {
                    }
            ).getBody();
        } catch (Exception ex) {
            Thread.sleep(10000);
            throw new Exception();
        }
        return portas;
    }

}
