package ca.log430.transactions.ports.out;

import ca.log430.transactions.domain.model.Action;
import ca.log430.transactions.domain.model.Carnet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionRepository extends JpaRepository<Action, Long> {

    public Carnet save(Carnet carnet);

    public List<Action> findAll();

    public List<Action> findByCarnet(Carnet carnet);

    public List<Action> findByUserId(Integer id);


}
