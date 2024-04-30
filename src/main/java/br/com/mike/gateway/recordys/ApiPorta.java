package br.com.mike.gateway.recordys;


import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.relational.core.mapping.Column;

import java.io.Serializable;
import java.util.Date;

@RedisHash("ApiPorta")
public class ApiPorta implements Serializable {

    public ApiPorta(Long porta, String endpoint, Long quantidadeRequisicao, Date dataUltimaRequisicao) {
        this.porta = porta;
        this.endpoint = endpoint;
        this.quantidadeRequisicao = quantidadeRequisicao;
        this.dataUltimaRequisicao = dataUltimaRequisicao;
    }

    @Id
    private Long porta;
    @Column
    private String endpoint;
    private Long quantidadeRequisicao;
    private Date dataUltimaRequisicao;

    public Long getPorta() {
        return porta;
    }

    public void setPorta(Long porta) {
        this.porta = porta;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Long getQuantidadeRequisicao() {
        return quantidadeRequisicao;
    }

    public void setQuantidadeRequisicao(Long quantidadeRequisicao) {
        this.quantidadeRequisicao = quantidadeRequisicao;
    }

    public Date getDataUltimaRequisicao() {
        return dataUltimaRequisicao;
    }

    public void setDataUltimaRequisicao(Date dataUltimaRequisicao) {
        this.dataUltimaRequisicao = dataUltimaRequisicao;
    }
}
