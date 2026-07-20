package org.example.projecttcg.controller;

import lombok.RequiredArgsConstructor;
import org.example.projecttcg.model.*;
import org.example.projecttcg.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final OrderRepository orderRepository;
    private final CardRepository cardRepository;
    private final CardSetRepository cardSetRepository;
    private final GameRepository gameRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("usersCount", userRepository.count());
        model.addAttribute("listingsCount", listingRepository.count());
        model.addAttribute("ordersCount", orderRepository.count());
        model.addAttribute("cardsCount", cardRepository.count());
        
        model.addAttribute("listings", listingRepository.findAll());
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("games", gameRepository.findAll());
        model.addAttribute("sets", cardSetRepository.findAll());
        return "admin/dashboard";
    }

    @PostMapping("/cards/new")
    public String createCard(
            @RequestParam Long setId,
            @RequestParam String name,
            @RequestParam String rarity,
            @RequestParam String collectorNumber,
            @RequestParam String language,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String description) {
        
        CardSet cardSet = cardSetRepository.findById(setId)
                .orElseThrow(() -> new IllegalArgumentException("Set not found"));

        Card card = Card.builder()
                .cardSet(cardSet)
                .name(name)
                .rarity(rarity)
                .collectorNumber(collectorNumber)
                .language(language)
                .imageUrl(imageUrl)
                .description(description)
                .build();

        cardRepository.save(card);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/listings/delete/{id}")
    public String deleteListing(@PathVariable Long id) {
        listingRepository.deleteById(id);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/users/status/{id}")
    public String updateUserStatus(@PathVariable Long id, @RequestParam User.UserStatus status) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setStatus(status);
            userRepository.save(user);
        }
        return "redirect:/admin/dashboard";
    }
}
