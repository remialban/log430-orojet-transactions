package ca.log430.transactions.ports.out;

import ca.log430.transactions.domain.model.Ordre;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Ordre, Integer> {

    @CachePut(value = "ordre", key = "#result.id")
    public Ordre save(Ordre ordre);

    @Cacheable(value = "ordreByUser", key = "#userId")
    public List<Ordre> findOrdreByUserId(Integer userId);



    @Cacheable(value = "ordre", key = "#integer")
    Optional<Ordre> findById(Integer integer);

    @Caching(evict = {
            @CacheEvict(value = "ordre", key = "#entity.id"),
            @CacheEvict(value = "ordreByUser", key = "#entity.userId")
    })
    void delete(Ordre entity);


    @Caching(evict = {
            @CacheEvict(value = "ordre", key = "#entity.id"),
            @CacheEvict(value = "ordreByUser", key = "#entity.userId")
    })
    void deleteById(Integer integer);
}
