package org.example.projecttcg.controller;

import lombok.RequiredArgsConstructor;
import org.example.projecttcg.model.*;
import org.example.projecttcg.repository.ListingRepository;
import org.example.projecttcg.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.*;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final ListingRepository listingRepository;
    private final OrderService orderService;

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getCart(HttpSession session) {
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        Map<Long, Integer> cart = getCart(session);
        List<Map<String, Object>> cartItems = new ArrayList<>();
        double total = 0.0;

        // Group items by seller for display
        Map<User, List<Map<String, Object>>> groupedBySeller = new HashMap<>();

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            Listing listing = listingRepository.findById(entry.getKey()).orElse(null);
            if (listing != null && listing.getStatus() == Listing.ListingStatus.ACTIVE) {
                Map<String, Object> item = new HashMap<>();
                item.put("listing", listing);
                item.put("quantity", entry.getValue());
                double subtotal = listing.getPrice() * entry.getValue();
                item.put("subtotal", subtotal);
                total += subtotal;

                groupedBySeller.computeIfAbsent(listing.getSeller(), k -> new ArrayList<>()).add(item);
            }
        }

        model.addAttribute("groupedCart", groupedBySeller);
        model.addAttribute("cartTotal", total);
        return "cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long listingId, @RequestParam(defaultValue = "1") Integer quantity, HttpSession session) {
        Map<Long, Integer> cart = getCart(session);
        cart.put(listingId, cart.getOrDefault(listingId, 0) + quantity);
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateQuantity(@RequestParam Long listingId, @RequestParam Integer quantity, HttpSession session) {
        Map<Long, Integer> cart = getCart(session);
        if (quantity <= 0) {
            cart.remove(listingId);
        } else {
            Listing listing = listingRepository.findById(listingId).orElse(null);
            if (listing != null && quantity <= listing.getQuantity()) {
                cart.put(listingId, quantity);
            }
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String removeFromCart(@RequestParam Long listingId, HttpSession session) {
        Map<Long, Integer> cart = getCart(session);
        cart.remove(listingId);
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String checkoutForm(HttpSession session, Model model) {
        Map<Long, Integer> cart = getCart(session);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }
        
        double total = 0.0;
        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            Listing listing = listingRepository.findById(entry.getKey()).orElse(null);
            if (listing != null) {
                total += listing.getPrice() * entry.getValue();
            }
        }

        model.addAttribute("cartTotal", total);
        model.addAttribute("platformFee", total * 0.05);
        model.addAttribute("finalTotal", total * 1.05);
        return "checkout";
    }

    @PostMapping("/checkout")
    public String processCheckout(
            @RequestParam String shippingAddress,
            @RequestParam String paymentMethod,
            HttpSession session,
            Model model) {
        
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Map<Long, Integer> cart = getCart(session);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }

        List<OrderService.CartItemRequest> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            OrderService.CartItemRequest item = new OrderService.CartItemRequest();
            item.listingId = entry.getKey();
            item.quantity = entry.getValue();
            items.add(item);
        }

        try {
            orderService.checkout(currentUser, items, shippingAddress, paymentMethod);
            session.removeAttribute("cart"); // Clear cart
            return "redirect:/orders?success=true";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            
            // Re-populate price attributes to prevent template rendering errors
            double total = 0.0;
            for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
                Listing listing = listingRepository.findById(entry.getKey()).orElse(null);
                if (listing != null) {
                    total += listing.getPrice() * entry.getValue();
                }
            }
            model.addAttribute("cartTotal", total);
            model.addAttribute("platformFee", total * 0.05);
            model.addAttribute("finalTotal", total * 1.05);
            
            return "checkout";
        }
    }
}
