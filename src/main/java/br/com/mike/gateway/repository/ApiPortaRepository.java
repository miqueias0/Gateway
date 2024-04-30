package br.com.mike.gateway.repository;

import br.com.mike.gateway.recordys.ApiPorta;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiPortaRepository extends CrudRepository<ApiPorta, Long> {

    List<ApiPorta> findAll();

}
