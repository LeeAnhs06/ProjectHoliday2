package org.example.projecttcg.controller;

import lombok.RequiredArgsConstructor;
import org.example.projecttcg.model.*;
import org.example.projecttcg.repository.CardRepository;
import org.example.projecttcg.repository.ListingRepository;
import org.example.projecttcg.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/seller")
@RequiredArgsConstructor
public class SellerController {

    private final ListingRepository listingRepository;
    private final CardRepository cardRepository;
    private final UserService userService;
    private final org.example.projecttcg.service.CardService cardService;
    private final org.example.projecttcg.service.CloudinaryService cloudinaryService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        List<Listing> myListings = listingRepository.findBySellerId(currentUser.getId());
        model.addAttribute("listings", myListings);
        return "seller/dashboard";
    }

    @PostMapping("/register")
    public String registerAsSeller(Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        userService.registerAsSeller(currentUser.getId());
        return "redirect:/seller/dashboard";
    }

    @GetMapping("/listings/new")
    public String newListingForm(Model model) {
        model.addAttribute("cards", cardRepository.findAll());
        model.addAttribute("conditions", Listing.CardCondition.values());
        model.addAttribute("cardService", cardService);
        return "seller/new-listing";
    }

    @PostMapping("/listings/new")
    public String createListing(
            @RequestParam Long cardId,
            @RequestParam Listing.CardCondition condition,
            @RequestParam Integer quantity,
            @RequestParam Double price,
            @RequestParam(required = false) org.springframework.web.multipart.MultipartFile imageFile,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String description,
            Model model) {
        
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        String finalImageUrl = imageUrl;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String uploadedUrl = cloudinaryService.uploadImage(imageFile);
                if (uploadedUrl != null) {
                    finalImageUrl = uploadedUrl;
                }
            } catch (Exception e) {
                // If it fails, default to card's default image URL
                finalImageUrl = card.getImageUrl();
            }
        }

        if (finalImageUrl == null || finalImageUrl.trim().isEmpty()) {
            finalImageUrl = card.getImageUrl();
        }

        Listing listing = Listing.builder()
                .seller(currentUser)
                .card(card)
                .cardCondition(condition)
                .quantity(quantity)
                .price(price)
                .imageUrls(finalImageUrl)
                .description(description)
                .status(Listing.ListingStatus.ACTIVE) // Auto active for simplicity in dev
                .build();

        listingRepository.save(listing);
        return "redirect:/seller/dashboard";
    }

    @PostMapping("/listings/delete/{id}")
    public String deleteListing(@PathVariable Long id, Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Listing listing = listingRepository.findById(id).orElse(null);
        if (listing != null && listing.getSeller().getId().equals(currentUser.getId())) {
            listingRepository.delete(listing);
        }
        return "redirect:/seller/dashboard";
    }
}
