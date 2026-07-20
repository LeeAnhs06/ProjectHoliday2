package org.example.projecttcg.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "games")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g. "Pokemon TCG", "Yu-Gi-Oh!"

    @Column(nullable = false, unique = true)
    private String code; // e.g. "pokemon", "yugioh"
}
