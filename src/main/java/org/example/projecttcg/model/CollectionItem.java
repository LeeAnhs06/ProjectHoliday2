package org.example.projecttcg.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "collection_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "card_id")
    private Card card;

    @Enumerated(EnumType.STRING)
    private Listing.CardCondition cardCondition;

    private Integer quantity;
    private Double purchasePrice;
    
    @Column(columnDefinition = "TEXT")
    private String note;
    
    private String imageUrl;
}
