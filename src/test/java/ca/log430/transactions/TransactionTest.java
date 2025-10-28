package ca.log430.transactions;


import ca.log430.transactions.domain.model.Ordre;
import ca.log430.transactions.domain.model.OrdreType;
import ca.log430.transactions.ports.out.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TransactionTest {

    @Autowired
    private OrderRepository orderRepository;

    // add tests here if needed
    public void sampleTest() {
        assert(true);
    }

    @Test
    void createTransaction() {
        Ordre ordre = new Ordre();
        ordre.setType(OrdreType.ACHAT);
        ordre.setAmount(100);

        this.orderRepository.save(ordre);

        Ordre ordre2 = this.orderRepository.findById(ordre.getId()).orElse(null);

        assert(ordre2 != null);
        assert(ordre2.getType() == OrdreType.ACHAT);
        assert(ordre2.getAmount() == 100);

        this.orderRepository.deleteById(ordre2.getId());

        if (this.orderRepository.findById(ordre2.getId()).isPresent()) {
            assert(false);
        } else {
            assert(true);
        }

    }
}
