package org.example.projecttcg.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "card_id")
    private Card card;

    private Double targetPrice;
    
    @Enumerated(EnumType.STRING)
    private Listing.CardCondition conditionFilter;
    
    private String notifyChannel; // "WEB", "EMAIL"
    private String frequency; // "INSTANT", "DAILY"

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum AlertStatus {
        ACTIVE, TRIGGERED, SNOOZED, DISABLED
    }
}
