package org.example.projecttcg.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "set_id")
    private CardSet cardSet;

    @Column(nullable = false)
    private String name; // e.g. "Charizard ex"

    private String rarity; // e.g. "Rare Holo ex", "Secret Rare"
    private String collectorNumber; // e.g. "223/197"
    private String language; // e.g. "EN", "JP"
    
    @Builder.Default
    private Boolean isFoil = false;
    
    private String imageUrl;
    
    @Column(columnDefinition = "TEXT")
    private String description;
}
