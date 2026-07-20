package org.example.projecttcg.controller;

import lombok.RequiredArgsConstructor;
import org.example.projecttcg.model.*;
import org.example.projecttcg.service.CardService;
import org.example.projecttcg.service.CollectionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/collection")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;
    private final CardService cardService;

    @GetMapping
    public String viewCollection(Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<CollectionItem> items = collectionService.getCollectionByUser(currentUser.getId());
        double portfolioValue = collectionService.calculatePortfolioValue(currentUser.getId());

        model.addAttribute("collectionItems", items);
        model.addAttribute("portfolioValue", portfolioValue);
        return "collection";
    }

    @PostMapping("/add")
    public String addToCollection(
            @RequestParam Long cardId,
            @RequestParam Listing.CardCondition condition,
            @RequestParam Integer quantity,
            @RequestParam Double purchasePrice,
            @RequestParam(required = false) String note,
            Model model) {
        
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Card card = cardService.findById(cardId);
        if (card != null) {
            CollectionItem item = CollectionItem.builder()
                    .user(currentUser)
                    .card(card)
                    .cardCondition(condition)
                    .quantity(quantity)
                    .purchasePrice(purchasePrice)
                    .note(note)
                    .build();
            collectionService.addToCollection(item);
        }

        return "redirect:/collection";
    }

    @PostMapping("/remove/{id}")
    public String removeFromCollection(@PathVariable Long id) {
        collectionService.removeFromCollection(id);
        return "redirect:/collection";
    }

    // Wishlist Endpoints
    @GetMapping("/wishlist")
    public String viewWishlist(Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<WishlistItem> items = collectionService.getWishlistByUser(currentUser.getId());
        model.addAttribute("wishlistItems", items);
        return "wishlist";
    }

    @PostMapping("/wishlist/add")
    public String addToWishlist(@RequestParam Long cardId, Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Card card = cardService.findById(cardId);
        if (card != null) {
            WishlistItem item = WishlistItem.builder().user(currentUser).card(card).build();
            collectionService.addToWishlist(item);
        }

        return "redirect:/collection/wishlist";
    }

    @PostMapping("/wishlist/remove")
    public String removeFromWishlist(@RequestParam Long cardId, Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        collectionService.removeFromWishlist(currentUser.getId(), cardId);
        return "redirect:/collection/wishlist";
    }
}
