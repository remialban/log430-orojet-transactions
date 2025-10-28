package ca.log430.transactions;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TransactionTest {

    // add tests here if needed
    public void sampleTest() {
        assert(true);
    }

    @Test
    void createTransaction() {
        Transaction transaction = new Transaction("tx123", 100.0, "user456");
        assert(transaction.getId().equals("tx123"));
        assert(transaction.getAmount() == 100.0);
        assert(transaction.getUserId().equals("user456"));
    }
}
