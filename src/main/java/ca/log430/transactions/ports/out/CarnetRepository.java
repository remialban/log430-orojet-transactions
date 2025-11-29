package ca.log430.transactions.ports.out;

import ca.log430.transactions.domain.model.Carnet;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarnetRepository extends JpaRepository<Carnet, Long> {

    public Carnet save(Carnet carnet);

    public List<Carnet> findAll();

}
