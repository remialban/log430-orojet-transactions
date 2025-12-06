package ca.log430.transactions.sagas;

import ca.log430.transactions.domain.model.Ordre;
import ca.log430.transactions.domain.model.OrdreType;
import ca.log430.transactions.ports.out.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Component
public class OrderMatchingSaga {

    private final OrderRepository orderRepository;
    private final JedisPool jedisPool;
    private final KafkaTemplate<String, HashMap<String, String>> kafkaTemplate;

    private ObjectMapper objectMapper;

    private static final Logger logger = LogManager.getLogger(OrderMatchingSaga.class);

    @Autowired
    public OrderMatchingSaga(OrderRepository orderRepository, JedisPool jedisPool, KafkaTemplate<String, HashMap<String, String>> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.jedisPool = jedisPool;
        this.kafkaTemplate = kafkaTemplate;
    }

    public record MatchResult(Ordre matchedOrderFromRedis, String matchedOrderJson, String redisKeyForMatchingZSet) {}

    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = "newOrder", groupId = "order-matcher")
    public void onNewOrder(HashMap<String, String> orderMap) {
        Integer orderId = Integer.parseInt(orderMap.get("id"));
        Optional<Ordre> newOrderOpt = orderRepository.findById(orderId);

        if (newOrderOpt.isEmpty() || newOrderOpt.get().isFinished()) {
            logger.info("Order {} not found or already finished. Skipping saga processing.", orderId);
            return;
        }

        Ordre newOrder = newOrderOpt.get();

        try (Jedis jedis = jedisPool.getResource()) {
            Optional<MatchResult> matchResultOpt = matchOrStoreOrderInRedis(jedis, newOrder);

            if (matchResultOpt.isPresent()) {
                MatchResult result = matchResultOpt.get();
                HashMap<String, String> matchEvent = new HashMap<>();
                matchEvent.put("newOrderId", newOrder.getId().toString());
                matchEvent.put("matchedOrderId", result.matchedOrderFromRedis().getId().toString());
                matchEvent.put("matchedOrderJson", result.matchedOrderJson());
                matchEvent.put("redisKeyForMatchingZSet", result.redisKeyForMatchingZSet());

                kafkaTemplate.send("orderMatchFound", matchEvent);
                logger.info("Sent orderMatchFound event for new order {} and matched order {}", newOrder.getId(), result.matchedOrderFromRedis().getId());
            }

        } catch (Exception e) {
            logger.error("Unhandled exception in OrderMatchingSaga for order {}", orderId, e);
        }
    }

    private Optional<MatchResult> matchOrStoreOrderInRedis(Jedis jedis, Ordre newOrder) throws JsonProcessingException {
        OrdreType typeToMatch = (newOrder.getType() == OrdreType.ACHAT) ? OrdreType.VENTE : OrdreType.ACHAT;
        String redisKeyForMatchingZSet = "pending_orders:" + newOrder.getCarnet().getId() + ":" + typeToMatch;
        String redisKeyForNewOrderZSet = "pending_orders:" + newOrder.getCarnet().getId() + ":" + newOrder.getType();

        String matchedOrderJson = null;
        Ordre matchedOrderFromRedis = null;

        String minScore = "-inf";
        String maxScore = "+inf";
        boolean isBuyOrder = (newOrder.getType() == OrdreType.ACHAT);

        if (isBuyOrder) {
            maxScore = String.valueOf(newOrder.getAmount());
            List<String> potentialMatches = jedis.zrangeByScore(redisKeyForMatchingZSet, minScore, maxScore, 0, 1);
            if (!potentialMatches.isEmpty()) {
                matchedOrderJson = potentialMatches.get(0);
                matchedOrderFromRedis = objectMapper.readValue(matchedOrderJson, Ordre.class);
            }
        } else {
            minScore = String.valueOf(newOrder.getAmount());
            List<String> potentialMatches = jedis.zrevrangeByScore(redisKeyForMatchingZSet, maxScore, minScore, 0, 1);
            if (!potentialMatches.isEmpty()) {
                matchedOrderJson = potentialMatches.get(0);
                matchedOrderFromRedis = objectMapper.readValue(matchedOrderJson, Ordre.class);
            }
        }

        if (matchedOrderFromRedis != null) {
            long removedCount = jedis.zrem(redisKeyForMatchingZSet, matchedOrderJson);
            if (removedCount > 0) {
                logger.info("Match found and claimed in Redis for order {}: pending order {}", newOrder.getId(), matchedOrderFromRedis.getId());
                return Optional.of(new MatchResult(matchedOrderFromRedis, matchedOrderJson, redisKeyForMatchingZSet));
            } else {
                logger.warn("Failed to claim matched order {} from Redis. It was likely processed by another instance. Proceeding to place new order in Redis.", matchedOrderFromRedis.getId());
            }
        }

        String newOrderJson = objectMapper.writeValueAsString(newOrder);
        jedis.zadd(redisKeyForNewOrderZSet, newOrder.getAmount(), newOrderJson);
        logger.info("No match found for order {}. Stored in Redis ZSET under key: {} with score {}.", newOrder.getId(), redisKeyForNewOrderZSet, newOrder.getAmount());

        return Optional.empty();
    }
}
