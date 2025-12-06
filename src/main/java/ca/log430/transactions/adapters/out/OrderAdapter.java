package ca.log430.transactions.adapters.out;

import ca.log430.transactions.domain.model.Ordre;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;

import ca.log430.transactions.ports.out.OrderRepository;

import java.util.HashMap;

@Service
public class OrderAdapter {

    @Autowired
    OrderRepository repository;


    @Autowired
    JedisPool jedis;

    @Autowired
    KafkaTemplate<String, HashMap<String, String>> kafkaTemplate;

    Logger logger = org.apache.logging.log4j.LogManager.getLogger(OrderAdapter.class);


    public Ordre save(Ordre ordre, String email) {
        boolean isNew = (ordre.getId() == null);

        ordre = this.repository.save(ordre);

        HashMap<String, String> ordreMap = new HashMap<>();

        ordreMap.put("id", ordre.getId().toString());
        ordreMap.put("type", ordre.getType().toString());
        ordreMap.put("createdAt", ordre.getCreatedAt().toString());
        ordreMap.put("userId", ordre.getUserId().toString());
        ordreMap.put("amount", ordre.getAmount().toString());
        ordreMap.put("isFinished", Boolean.toString(ordre.isFinished()));

        ordreMap.put("email", email);

        if (ordre.getCarnet() != null) {
            ordreMap.put("carnetId", ordre.getCarnet().getId().toString());
        }

        if (isNew) {
            kafkaTemplate.send("newOrder", ordreMap);
            logger.info("Order saved with id " + ordre.getId());

        } else {
            kafkaTemplate.send("updateOrder", ordreMap);
        }

        return ordre;
    }



    public Page<Ordre> findAll(Integer pageNumber, Integer pageSize) {
        Pageable page = PageRequest.of(pageNumber, pageSize);
        return this.repository.findAll(page);
    }


    public Page<Ordre> findByUserId(Integer userId, int pageNumber, int pageSize) {
        // Implementation goes here
        Pageable page = PageRequest.of(pageNumber, pageSize);
        return this.repository.findAll(page);
    }

    /*public Page<Ordre> findByCarnet(Carnet carnet, int pageNumber, int pageSize) {
        return this.repository.findAll(page);
    }*/

}
