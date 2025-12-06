package ca.log430.transactions.sagas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import ca.log430.transactions.domain.model.Ordre;

import java.util.HashMap;

@Component
public class OrderCompensationConsumer {

    private final JedisPool jedisPool;
    private ObjectMapper objectMapper;

    private static final Logger logger = LogManager.getLogger(OrderCompensationConsumer.class);

    @Autowired
    public OrderCompensationConsumer(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = "orderMatchFailed", groupId = "order-compensator")
    public void onOrderMatchFailed(HashMap<String, String> failedMatchEvent) {
        String matchedOrderId = failedMatchEvent.get("matchedOrderId");
        logger.info("Received orderMatchFailed event. Starting compensation for matched order {}", matchedOrderId);

        try (Jedis jedisCompensation = jedisPool.getResource()) {
            String matchedOrderJson = failedMatchEvent.get("matchedOrderJson");
            String redisKey = failedMatchEvent.get("redisKeyForMatchingZSet");
            Ordre matchedOrder = objectMapper.readValue(matchedOrderJson, Ordre.class);

            // Put the order back into the ZSET with its original score
            jedisCompensation.zadd(redisKey, matchedOrder.getAmount(), matchedOrderJson);
            
            logger.info("Successfully compensated Redis by putting order {} back in ZSET {}", matchedOrder.getId(), redisKey);
        } catch (Exception compEx) {
            // This is a critical failure. The message will be re-processed by Kafka.
            // In a production system, you might add a dead-letter queue here after several retries.
            logger.error("CRITICAL: Failed to compensate Redis for matched order {}. This may require manual intervention.", matchedOrderId, compEx);
            // Re-throw to allow Kafka to retry processing the message
            throw new RuntimeException("Failed to execute Redis compensation", compEx);
        }
    }
}
