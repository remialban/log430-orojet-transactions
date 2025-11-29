package ca.log430.transactions;

import ca.log430.transactions.domain.model.Ordre;
import ca.log430.transactions.domain.model.OrdreType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.resps.Tuple;

import java.util.List;


@Component
public class OnOrderListenner {

    Logger logger = LogManager.getLogger(OnOrderListenner.class);

    @Autowired
    JedisPool jedisPool;

    @KafkaListener(topics = "newOrder", groupId = "order-group", concurrency = "1")
    public void consumeOrder(Ordre ordre) {
        logger.info("Received new order : {} type: {} amount: {}", ordre.getId(), ordre.getType(), ordre.getAmount());

        try (var jedis = jedisPool.getResource()) {
            String key = "order:" + ordre.getCarnet().getId() + ":" + ordre.getType();

            String achatKey = "order:" + ordre.getCarnet().getId() + ":" + OrdreType.ACHAT;
            String venteKey = "order:" + ordre.getCarnet().getId() + ":" + OrdreType.VENTE;

            boolean isProcessed = false;

            if (ordre.getType() == OrdreType.ACHAT) {
                List<Tuple> results = jedis.zrangeByScoreWithScores(venteKey, Double.NEGATIVE_INFINITY, ordre.getAmount(), 0, 1);

                if (!results.isEmpty()) {
                    // Get the first matching order
                    Tuple tuple = results.getFirst();
                    double score = tuple.getScore();
                    double id = Double.parseDouble(tuple.getElement());

                    // process

                    jedis.zrem(venteKey, tuple.getElement());
                    isProcessed = true;
                }
            } else {
                List<Tuple> results = jedis.zrevrangeByScoreWithScores(achatKey, Double.POSITIVE_INFINITY, ordre.getAmount(), 0, 1);

                if (!results.isEmpty()) {
                    // Get the first matching order
                    Tuple tuple = results.getFirst();
                    double score = tuple.getScore();
                    double id = Double.parseDouble(tuple.getElement());



                    jedis.zrem(achatKey, tuple.getElement());
                    isProcessed = true;
                }

            }

            if (!isProcessed) {
                jedis.zadd(key, ordre.getAmount(), ordre.getId().toString());
            }

        } catch (Exception e) {
            logger.error("Error caching order in Redis: {}", e.getMessage());
        }

    }
}
