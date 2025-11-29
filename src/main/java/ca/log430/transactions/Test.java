package ca.log430.transactions;

import ca.log430.transactions.adapters.out.OrderAdapter;
import ca.log430.transactions.domain.model.Ordre;
import ca.log430.transactions.domain.model.OrdreType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class Test implements CommandLineRunner {

    @Autowired
    private OrderAdapter ordreAdapter;

    @Override
    public void run(String... args) {
        Ordre ordre = new Ordre();
        ordre.setAmount(100);
        ordre.setType(OrdreType.ACHAT);
        ordreAdapter.save(ordre);


        

    }

}
