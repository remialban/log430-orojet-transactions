package ca.log430.transactions.ports.out;

import ca.log430.transactions.domain.model.Ordre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Ordre, Integer> {
    public Ordre save(Ordre ordre);

    public List<Ordre> findOrdreByUserId(Integer userId);


}
