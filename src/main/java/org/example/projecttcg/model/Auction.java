package org.example.projecttcg.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auctions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seller_id")
    private User seller;

    @ManyToOne(optional = false)
    @JoinColumn(name = "card_id")
    private Card card;

    @Column(nullable = false)
    private Double startPrice;

    @Column(nullable = false)
    private Double minIncrement;

    @Column(nullable = false)
    private Double currentPrice;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Listing.CardCondition cardCondition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    @Builder.Default
    private Boolean isPaid = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum AuctionStatus {
        ACTIVE, COMPLETED, CANCELLED
    }
}
