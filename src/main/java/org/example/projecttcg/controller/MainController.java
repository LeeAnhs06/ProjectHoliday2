package org.example.projecttcg.controller;

import lombok.RequiredArgsConstructor;
import org.example.projecttcg.model.*;
import org.example.projecttcg.repository.CardSetRepository;
import org.example.projecttcg.repository.GameRepository;
import org.example.projecttcg.repository.ListingRepository;
import org.example.projecttcg.service.CardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final CardService cardService;
    private final GameRepository gameRepository;
    private final CardSetRepository cardSetRepository;
    private final ListingRepository listingRepository;
    private final org.example.projecttcg.service.ExternalApiService externalApiService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("games", gameRepository.findAll());
        model.addAttribute("recentCards", cardService.searchCards("", null, null, null, null));
        return "index";
    }

    @GetMapping("/search")
    public String search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long gameId,
            @RequestParam(required = false) Long setId,
            @RequestParam(required = false) String rarity,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        
        int pageSize = 12; // 12 cards per page
        org.springframework.data.domain.Page<Card> cardPage = cardService.searchCardsPaginated(keyword, gameId, setId, rarity, language, page, pageSize);
        
        // Dynamic fallback to external APIs if local database returns empty results
        if (cardPage.isEmpty() && keyword != null && !keyword.trim().isEmpty()) {
            if (gameId == null || gameId == 1) {
                externalApiService.fetchAndSaveFromPokemonApi(keyword);
            }
            if (gameId == null || gameId == 2) {
                externalApiService.fetchAndSaveFromYugiohApi(keyword);
            }
            // Re-query local database after syncing with external APIs
            cardPage = cardService.searchCardsPaginated(keyword, gameId, setId, rarity, language, page, pageSize);
        }

        model.addAttribute("cardsPage", cardPage);
        model.addAttribute("cards", cardPage.getContent());
        model.addAttribute("games", gameRepository.findAll());
        model.addAttribute("sets", cardSetRepository.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedGameId", gameId);
        model.addAttribute("selectedSetId", setId);
        model.addAttribute("selectedRarity", rarity);
        model.addAttribute("selectedLanguage", language);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", cardPage.getTotalPages());
        model.addAttribute("totalItems", cardPage.getTotalElements());
        
        return "search";
    }

    @GetMapping("/cards/{id}")
    public String cardDetails(@PathVariable Long id, Model model) {
        Card card = cardService.findById(id);
        if (card == null) {
            return "redirect:/";
        }
        
        double marketPrice = cardService.calculateMarketPrice(id);
        List<Listing> activeListings = listingRepository.findByCardIdAndStatus(id, Listing.ListingStatus.ACTIVE);
        
        model.addAttribute("card", card);
        model.addAttribute("marketPrice", marketPrice);
        model.addAttribute("listings", activeListings);
        
        return "card-details";
    }

    @GetMapping("/marketplace")
    public String marketplace(
            @RequestParam(required = false) Long gameId,
            @RequestParam(required = false) Long setId,
            @RequestParam(required = false) Listing.CardCondition condition,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            Model model) {
        
        List<Listing> listings = listingRepository.searchMarketplace(gameId, setId, condition, minPrice, maxPrice);
        
        model.addAttribute("listings", listings);
        model.addAttribute("games", gameRepository.findAll());
        model.addAttribute("sets", cardSetRepository.findAll());
        model.addAttribute("selectedGameId", gameId);
        model.addAttribute("selectedSetId", setId);
        model.addAttribute("selectedCondition", condition);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        
        return "marketplace";
    }
}
