package org.example.projecttcg.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "listings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seller_id")
    private User seller;

    @ManyToOne(optional = false)
    @JoinColumn(name = "card_id")
    private Card card;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardCondition cardCondition;

    private Integer quantity;
    private Double price;

    @Column(columnDefinition = "TEXT")
    private String imageUrls; // comma separated actual images

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum CardCondition {
        MINT, NEAR_MINT, LIGHTLY_PLAYED, MODERATELY_PLAYED, HEAVILY_PLAYED, DAMAGED
    }

    public enum ListingStatus {
        DRAFT, PENDING_REVIEW, ACTIVE, RESERVED, SOLD, EXPIRED, REJECTED
    }
}
