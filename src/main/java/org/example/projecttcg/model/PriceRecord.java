package org.example.projecttcg.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "card_id")
    private Card card;

    private LocalDateTime recordedAt;
    
    private Double lowestPrice;
    private Double medianPrice;
    private Double marketPrice;
    
    private String dataSource; // e.g. "Internal Trade", "TcgPlayer API"
}
