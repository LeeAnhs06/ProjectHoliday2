package org.example.projecttcg.controller;

import lombok.RequiredArgsConstructor;
import org.example.projecttcg.model.*;
import org.example.projecttcg.service.OrderService;
import org.example.projecttcg.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    @GetMapping
    public String viewOrders(Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<Order> buyerOrders = orderService.getBuyerOrders(currentUser.getId());
        model.addAttribute("buyerOrders", buyerOrders);
        
        if (currentUser.getRole() == User.Role.SELLER || currentUser.getRole() == User.Role.ADMIN) {
            List<Order> sellerOrders = orderService.getSellerOrders(currentUser.getId());
            model.addAttribute("sellerOrders", sellerOrders);
        }

        return "orders";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Order order = orderService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Ensure user is authorized to view
        if (!order.getBuyer().getId().equals(currentUser.getId()) &&
            !order.getSeller().getId().equals(currentUser.getId()) &&
            currentUser.getRole() != User.Role.ADMIN) {
            return "redirect:/orders";
        }

        model.addAttribute("order", order);
        return "order-details";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam Order.OrderStatus status,
            Model model) {
        
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Order order = orderService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Authorize status transition
        boolean isBuyer = order.getBuyer().getId().equals(currentUser.getId());
        boolean isSeller = order.getSeller().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;

        if (!isBuyer && !isSeller && !isAdmin) {
            return "redirect:/orders";
        }

        // Validate status changes: e.g. seller confirms, buyer completes
        orderService.updateOrderStatus(id, status);
        return "redirect:/orders/" + id;
    }

    @PostMapping("/add-balance")
    public String addFakeBalance(@RequestParam Double amount, Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        userService.addBalance(currentUser.getId(), amount);
        return "redirect:/orders";
    }
}
