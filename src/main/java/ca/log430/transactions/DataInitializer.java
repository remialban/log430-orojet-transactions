package ca.log430.transactions;

import ca.log430.transactions.domain.model.Action;
import ca.log430.transactions.domain.model.Carnet;
import ca.log430.transactions.domain.model.Ordre;

import ca.log430.transactions.domain.model.OrdreType;
import ca.log430.transactions.ports.out.ActionRepository;
import ca.log430.transactions.ports.out.CarnetRepository;
import ca.log430.transactions.ports.out.OrderRepository;
import jakarta.persistence.criteria.Order;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {
    private final OrderRepository orderRepository;
    private final CarnetRepository carnetRepository;
    private final ActionRepository actionRepository;

    public DataInitializer(OrderRepository orderRepository, CarnetRepository carnetRepository, ActionRepository actionRepository) {
        this.orderRepository = orderRepository;
        this.carnetRepository = carnetRepository;
        this.actionRepository = actionRepository;
    }

    @Override
    public void run(String... args) {


        if (carnetRepository.count() == 0) {

            Carnet carnet1 = new Carnet();
            carnet1.setName("Carnet principal");

            carnetRepository.save(carnet1);

            Carnet carnet2 = new Carnet();
            carnet2.setName("Carnet secondaire");
            carnetRepository.save(carnet2);

            Carnet carnet3 = new Carnet();
            carnet3.setName("Carnet tertiaire");
            carnetRepository.save(carnet3);


            System.out.println("✅ Carnets initiaux insérés !");
        }
        

        if (actionRepository.count() == 0) {
            List<Carnet> carnets = carnetRepository.findAll();
            Random random = new Random();

            for (int i = 0; i < carnets.size(); i++) {
                Carnet carnet = carnets.get(i);

                for (int j = 0; j < 10; j++) {
                    Action action = new Action();

                    action.setCarnet(carnet);
                    action.setUserId(random.nextInt(1,20));

                    actionRepository.save(action);
                }
            }


        }

        if (orderRepository.count() == 0) {
            Random random = new Random();


            List<Carnet> carnets = carnetRepository.findAll();
            List<Action> actions = actionRepository.findAll();


            for (int i = 0; i < random.nextInt(actions.size()); i++) {

                Action action = actions.get(random.nextInt(actions.size()));
                actions.remove(action);

                Ordre order = new Ordre();
                order.setCarnet(actions.get(i).getCarnet());
                order.setUserId(action.getUserId());
                order.setType(OrdreType.VENTE);
                order.setAmount(random.nextInt(1, 100));
                orderRepository.save(order);
            }


            for (int i = 0; i < 100; i++) {
                Ordre ordre1 = new Ordre();

                ordre1.setType(OrdreType.ACHAT);
                ordre1.setAmount(random.nextInt(0, 100));
                ordre1.setUserId(random.nextInt(1, 3));
                ordre1.setCarnet(carnets.get(random.nextInt(carnets.size())));
                orderRepository.save(ordre1);
            }

            System.out.println("✅ Données initiales insérées !");
        }

    }

}
