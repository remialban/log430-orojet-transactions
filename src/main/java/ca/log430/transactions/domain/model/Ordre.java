package ca.log430.transactions.domain.model;

// Entity :

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ordres")
public class Ordre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    private Integer id;

    @Version
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long version;

    // JPA annotations: enum, specify values :

    @Enumerated(EnumType.STRING)
    private OrdreType type;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    private Integer userId;

    private Integer amount;

    @ManyToOne(fetch = FetchType.EAGER)
    private Carnet carnet;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean isFinished = false;

    public Ordre(OrdreType type, LocalDateTime createdAt) {
        this.type = type;
        this.createdAt = createdAt;
    }
    public Ordre() {
        this.createdAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public OrdreType getType() {
        return type;
    }

    public void setType(OrdreType type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public Carnet getCarnet() {
        return carnet;
    }

    public void setCarnet(Carnet carnet) {
        this.carnet = carnet;
    }
}
