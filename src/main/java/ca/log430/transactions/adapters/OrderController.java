package ca.log430.transactions.adapters;

import ca.log430.transactions.domain.Response;
import ca.log430.transactions.domain.model.Ordre;
import ca.log430.transactions.ports.out.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/orders")
public class OrderController {

    OrderRepository orderRepository;
    Environment environment;
    public OrderController(OrderRepository orderRepository, Environment environment) {
        this.orderRepository = orderRepository;
        this.environment = environment;
    }

    // request Order
    @PostMapping
    public ResponseEntity<Response<Ordre>> createOrder(@RequestBody Ordre ordre, HttpServletRequest request) {


        // persist ordre

        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(new Response<>(null, "Missing or invalid Authorization header"));
            }

            String tokenUser = authHeader.substring(7);
            Integer userId = this.extractUserIdFromToken(tokenUser).orElse(null);

            if (userId == null) {
                return ResponseEntity.status(400).body(new Response<>(null, "User ID is required"));
            }
            if (!userId.equals(ordre.getUserId())) {
                return ResponseEntity.status(403).body(new Response<>(null, "User ID in token does not match user ID in order"));
            }
            // Call api to get current price
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            String token = this.environment.getProperty("token");
            headers.set("Authorization", "Bearer " + token); // <-- ton token JWT ici

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Appeler le service distant avec les headers
            ResponseEntity<JsonNode> responseRest = restTemplate.exchange(
                    "http://" + this.environment.getProperty("GATEWAY_HOST") + "/users/" + ordre.getUserId(),
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            JsonNode response = responseRest.getBody();

            int user_id = response.get("data").get("id").asInt();

            if (user_id != ordre.getUserId()) {
                return ResponseEntity.status(400).body(new Response<>(null, "User not found"));
            }

            ordre = this.orderRepository.save(ordre);
            return ResponseEntity.ok(new Response<>(ordre, null));


        } catch (Exception ex) {
            return ResponseEntity.status(500).body(new Response<>(null, ex.getMessage()));
        }
    }


    @GetMapping
    public ResponseEntity<Response<List<Ordre>>> getAllOrders(@RequestParam(required = false) Integer userId) {
        try {

            if (userId != null) {
                List<Ordre> userOrders = this.orderRepository.findOrdreByUserId(userId);
                return ResponseEntity.ok(new Response<>(userOrders, null));
            }
            List<Ordre> orders = this.orderRepository.findAll();


            return ResponseEntity.ok(new Response<>(orders, null));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(new Response<>(null, ex.getMessage()));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Response<Ordre>> getOrderById(@PathVariable Integer orderId) {
        try {
            Optional<Ordre> ordre = this.orderRepository.findById(orderId);

            if (ordre.isEmpty()) {
                return ResponseEntity.status(404).body(new Response<>(null, "Order not found"));
            }

            return ResponseEntity.ok(new Response<>(ordre.get(), null));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(new Response<>(null, ex.getMessage()));

        }
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Response<String>> deleteOrderById(@PathVariable Integer orderId, HttpServletRequest request) {
        try {

            Optional<Ordre> ordre = this.orderRepository.findById(orderId);
            if (ordre.isEmpty()) {
                return ResponseEntity.status(404).body(new Response<>(null, "Order not found"));
            }
            // check if userId in JWT Token in bearer authentification is the same as the userId of the order
            // get userId from bearer token
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(new Response<>(null, "Missing or invalid Authorization header"));
            }

            String tokenUser = authHeader.substring(7);
            Integer userId = this.extractUserIdFromToken(tokenUser).orElse(null);

            if (userId == null) {
                return ResponseEntity.status(400).body(new Response<>(null, "User ID is required"));
            }
            if (!userId.equals(ordre.get().getUserId())) {
                return ResponseEntity.status(403).body(new Response<>(null, "User ID in token does not match user ID in order"));
            }





            this.orderRepository.deleteById(orderId);

            return ResponseEntity.ok(new Response<>("Order deleted", null));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(new Response<>(null, ex.getMessage()));

        }
    }

    private Optional<Integer> extractUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            System.out.println("las bas");

            if (parts.length < 2) return Optional.empty();
            System.out.println("ici");
            String payload = parts[1];
            byte[] decoded = java.util.Base64.getUrlDecoder().decode(payload);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(decoded);

            // cherche d'abord "userId", sinon "id"
            if (node.has("userId") && node.get("userId").canConvertToInt()) {
                System.out.println("dans if ");

                return Optional.of(node.get("userId").asInt());
            }
            return Optional.of(Integer.valueOf(node.get("userId").asText()));
            /*System.out.println("exterieur if");
            System.out.println(node.get("userId"));

            if (node.has("id") && node.get("id").canConvertToInt()) {
                return Optional.of(node.get("id").asInt());
            }
            return Optional.empty();*/
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<Response<Ordre>> updateOrderById(@PathVariable Integer orderId, @RequestBody Ordre ordre, HttpServletRequest request) {
        try {
            Optional<Ordre> existingOrder = this.orderRepository.findById(orderId);
            if (existingOrder.isEmpty()) {
                return ResponseEntity.status(404).body(new Response<>(null, "Order not found"));
            }

            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(new Response<>(null, "Missing or invalid Authorization header"));
            }

            String tokenUser = authHeader.substring(7);
            Integer userId = this.extractUserIdFromToken(tokenUser).orElse(null);

            if (userId == null) {
                return ResponseEntity.status(400).body(new Response<>(null, "User ID is required"));
            }
            if (!userId.equals(ordre.getUserId())) {
                return ResponseEntity.status(403).body(new Response<>(null, "User ID in token does not match user ID in order"));
            }

            ordre.setId(orderId);
            ordre = this.orderRepository.save(ordre);
            return ResponseEntity.ok(new Response<>(ordre, null));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(new Response<>(null, ex.getMessage()));
        }

    }

}
