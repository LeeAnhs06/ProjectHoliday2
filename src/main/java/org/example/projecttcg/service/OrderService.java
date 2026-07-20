package org.example.projecttcg.service;

import lombok.RequiredArgsConstructor;
import org.example.projecttcg.model.*;
import org.example.projecttcg.repository.ListingRepository;
import org.example.projecttcg.repository.OrderRepository;
import org.example.projecttcg.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public static class CartItemRequest {
        public Long listingId;
        public Integer quantity;
    }

    @Transactional
    public List<Order> checkout(User buyer, List<CartItemRequest> items, String shippingAddress, String paymentMethod) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        // Group items by seller
        Map<User, List<CartItemRequest>> itemsBySeller = new HashMap<>();
        for (CartItemRequest itemReq : items) {
            Listing listing = listingRepository.findById(itemReq.listingId)
                    .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + itemReq.listingId));
            
            if (listing.getStatus() != Listing.ListingStatus.ACTIVE) {
                throw new IllegalArgumentException("Listing is not active: " + listing.getCard().getName());
            }
            if (listing.getQuantity() < itemReq.quantity) {
                throw new IllegalArgumentException("Insufficient stock for: " + listing.getCard().getName());
            }

            User seller = listing.getSeller();
            itemsBySeller.computeIfAbsent(seller, k -> new ArrayList<>()).add(itemReq);
        }

        List<Order> createdOrders = new ArrayList<>();

        for (Map.Entry<User, List<CartItemRequest>> entry : itemsBySeller.entrySet()) {
            User seller = entry.getKey();
            List<CartItemRequest> sellerItems = entry.getValue();

            String orderCode = "TCG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            Order order = Order.builder()
                    .orderCode(orderCode)
                    .buyer(buyer)
                    .seller(seller)
                    .shippingAddress(shippingAddress)
                    .paymentMethod(paymentMethod)
                    .status(paymentMethod.equals("COD") ? Order.OrderStatus.CREATED : Order.OrderStatus.AWAITING_PAYMENT)
                    .createdAt(LocalDateTime.now())
                    .build();

            double subtotal = 0.0;
            List<OrderItem> orderItems = new ArrayList<>();

            for (CartItemRequest itemReq : sellerItems) {
                Listing listing = listingRepository.findById(itemReq.listingId).get();
                
                // Lock inventory
                listing.setQuantity(listing.getQuantity() - itemReq.quantity);
                if (listing.getQuantity() == 0) {
                    listing.setStatus(Listing.ListingStatus.SOLD);
                }
                listingRepository.save(listing);

                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .listing(listing)
                        .price(listing.getPrice())
                        .quantity(itemReq.quantity)
                        .build();

                orderItems.add(orderItem);
                subtotal += listing.getPrice() * itemReq.quantity;
            }

            double platformFee = subtotal * 0.05; // 5% fee
            order.setPlatformFee(platformFee);
            order.setTotalAmount(subtotal + platformFee);
            order.setItems(orderItems);

            createdOrders.add(orderRepository.save(order));
        }

        return createdOrders;
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        Order.OrderStatus oldStatus = order.getStatus();
        if (oldStatus == newStatus) {
            return order;
        }

        order.setStatus(newStatus);

        // If completed, add revenue to seller balance
        if (newStatus == Order.OrderStatus.COMPLETED) {
            double netRevenue = order.getTotalAmount() - order.getPlatformFee();
            User seller = order.getSeller();
            seller.setBalance(seller.getBalance() + netRevenue);
            userRepository.save(seller);
        }

        // If cancelled or refunded, return inventory
        if (newStatus == Order.OrderStatus.CANCELLED || newStatus == Order.OrderStatus.REFUNDED) {
            for (OrderItem item : order.getItems()) {
                Listing listing = item.getListing();
                listing.setQuantity(listing.getQuantity() + item.getQuantity());
                if (listing.getStatus() == Listing.ListingStatus.SOLD) {
                    listing.setStatus(Listing.ListingStatus.ACTIVE);
                }
                listingRepository.save(listing);
            }
        }

        return orderRepository.save(order);
    }

    public List<Order> getBuyerOrders(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    public List<Order> getSellerOrders(Long sellerId) {
        return orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
    }

    public Optional<Order> findById(Long orderId) {
        return orderRepository.findById(orderId);
    }
}
