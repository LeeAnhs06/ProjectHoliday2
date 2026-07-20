package org.example.projecttcg.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "card_sets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "game_id")
    private Game game;

    @Column(nullable = false)
    private String name; // e.g. "Scarlet & Violet", "Legendary Collection"

    @Column(nullable = false)
    private String code; // e.g. "sv01", "lc"
}
