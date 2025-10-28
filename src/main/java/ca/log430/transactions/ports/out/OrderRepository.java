package ca.log430.transactions.ports.out;

import ca.log430.transactions.domain.model.Ordre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Ordre, Integer> {
    public Ordre save(Ordre ordre);

    public List<Ordre> findOrdreByUserId(Integer userId);


    Optional<Ordre> findById(Integer integer);


    void delete(Ordre entity);

    @Override
    void deleteById(Integer integer);
}
