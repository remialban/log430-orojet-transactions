package ca.log430.transactions.sagas;

import ca.log430.transactions.adapters.out.OrderAdapter;
import ca.log430.transactions.domain.model.Ordre;
import ca.log430.transactions.domain.model.OrdreType;
import ca.log430.transactions.ports.out.BalanceService;
import ca.log430.transactions.ports.out.OrderRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Optional;

@Component
public class OrderMatchConsumer {

    private final OrderRepository orderRepository;
    private final OrderAdapter orderAdapter;
    private final KafkaTemplate<String, HashMap<String, String>> kafkaTemplate;
    private final BalanceService balanceService; // Service to check user balance

    private static final Logger logger = LogManager.getLogger(OrderMatchConsumer.class);

    @Autowired
    public OrderMatchConsumer(OrderRepository orderRepository, OrderAdapter orderAdapter, KafkaTemplate<String, HashMap<String, String>> kafkaTemplate, BalanceService balanceService) {
        this.orderRepository = orderRepository;
        this.orderAdapter = orderAdapter;
        this.kafkaTemplate = kafkaTemplate;
        this.balanceService = balanceService;
    }

    @Transactional
    @KafkaListener(topics = "orderMatchFound", groupId = "order-updater")
    public void onOrderMatchFound(HashMap<String, String> matchEvent) {
        Integer newOrderId = Integer.parseInt(matchEvent.get("newOrderId"));
        Integer matchedOrderId = Integer.parseInt(matchEvent.get("matchedOrderId"));

        try {
            Optional<Ordre> newOrderOpt = orderRepository.findById(newOrderId);
            Optional<Ordre> dbMatchedOrderOpt = orderRepository.findById(matchedOrderId);

            if (newOrderOpt.isEmpty() || dbMatchedOrderOpt.isEmpty()) {
                logger.warn("One or both orders for match ({} and {}) not found. Sending compensation event.", newOrderId, matchedOrderId);
                kafkaTemplate.send("orderMatchFailed", matchEvent);
                return;
            }
            
            Ordre newOrder = newOrderOpt.get();
            Ordre dbMatchedOrder = dbMatchedOrderOpt.get();

            // Identify buyer and seller
            Ordre buyer;
            Ordre seller;
            if (newOrder.getType() == OrdreType.ACHAT) {
                buyer = newOrder;
                seller = dbMatchedOrder;
            } else {
                buyer = dbMatchedOrder;
                seller = newOrder;
            }

            // --- BALANCE CHECK ---
            // This assumes the BalanceService might call another microservice.
            if (!balanceService.hasSufficientFunds(buyer.getUserId(), buyer.getAmount())) {
                logger.warn("Transaction failed for user {}: insufficient funds. Compensating.", buyer.getUserId());
                kafkaTemplate.send("orderMatchFailed", matchEvent);
                // Stop processing. No DB changes have been made, so no DB rollback is needed.
                return;
            }
            // --- END BALANCE CHECK ---

            // If we reach here, the user has funds. Proceed with the transaction.
            if (newOrder.isFinished() || dbMatchedOrder.isFinished()) {
                 logger.warn("One or both orders for match ({} and {}) are already finished. Sending compensation event.", newOrderId, matchedOrderId);
                kafkaTemplate.send("orderMatchFailed", matchEvent);
                return;
            }

            newOrder.setFinished(true);
            dbMatchedOrder.setFinished(true);

            orderAdapter.save(newOrder, "saga@system.com");
            orderAdapter.save(dbMatchedOrder, "saga@system.com");

            logger.info("Successfully matched and updated orders in DB: {} and {}", newOrder.getId(), dbMatchedOrder.getId());

            // Create and send an event for the balance update service
            HashMap<String, String> balanceUpdateEvent = new HashMap<>();
            balanceUpdateEvent.put("buyerUserId", buyer.getUserId().toString());
            balanceUpdateEvent.put("sellerUserId", seller.getUserId().toString());
            balanceUpdateEvent.put("amount", newOrder.getAmount().toString());
            balanceUpdateEvent.put("carnetId", newOrder.getCarnet().getId().toString());

            kafkaTemplate.send("transactionCompleted", balanceUpdateEvent);
            logger.info("Sent transactionCompleted event for users {} and {}", buyer.getUserId(), seller.getUserId());

        } catch (Exception e) {
            logger.error("Error during database update for order match. Sending orderMatchFailed event for matched order {}", matchedOrderId, e);
            kafkaTemplate.send("orderMatchFailed", matchEvent);
            throw new RuntimeException("Database update failed. Sent compensation event.", e);
        }
    }
}
