package ca.log430.transactions;

import ca.log430.transactions.domain.model.Ordre;

import ca.log430.transactions.domain.model.OrdreType;
import ca.log430.transactions.ports.out.OrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {
    private final OrderRepository orderRepository;

    public DataInitializer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void run(String... args) {
        if (orderRepository.count() == 0) {

            Ordre ordre1 = new Ordre();
            ordre1.setType(OrdreType.ACHAT);
            ordre1.setAmount(100);
            orderRepository.save(ordre1);

            Ordre ordre2 = new Ordre();
            ordre2.setType(OrdreType.VENTE);
            ordre2.setAmount(42);
            orderRepository.save(ordre2);

            System.out.println("✅ Données initiales insérées !");
        }
    }

}
