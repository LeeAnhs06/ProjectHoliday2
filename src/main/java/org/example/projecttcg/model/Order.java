package org.example.projecttcg.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderCode;

    @ManyToOne(optional = false)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    // A single checkout might have multiple orders split by seller
    @ManyToOne(optional = false)
    @JoinColumn(name = "seller_id")
    private User seller;

    @Column(nullable = false)
    private String shippingAddress;

    private String paymentMethod; // e.g. "COD", "BANK_TRANSFER"
    
    @Builder.Default
    private Double platformFee = 0.0;
    @Builder.Default
    private Double totalAmount = 0.0; // including fee

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;

    public enum OrderStatus {
        CREATED, AWAITING_PAYMENT, PAID, SELLER_CONFIRMED, SHIPPING, DELIVERED, COMPLETED, DISPUTED, CANCELLED, REFUNDED
    }
}
