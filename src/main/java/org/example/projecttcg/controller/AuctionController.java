package org.example.projecttcg.controller;

import lombok.RequiredArgsConstructor;
import org.example.projecttcg.model.*;
import org.example.projecttcg.repository.CardRepository;
import org.example.projecttcg.service.AuctionService;
import org.example.projecttcg.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;
    private final CardRepository cardRepository;
    private final org.example.projecttcg.repository.ListingRepository listingRepository;
    private final org.example.projecttcg.repository.CardSetRepository cardSetRepository;
    private final UserService userService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return userService.findByEmail(auth.getName());
        }
        return null;
    }

    @GetMapping("/auctions")
    public String listAuctions(Model model) {
        List<Auction> activeAuctions = auctionService.getActiveAuctions();
        model.addAttribute("auctions", activeAuctions);
        return "auctions";
    }

    @GetMapping("/auctions/{id}")
    public String auctionDetails(@PathVariable Long id, Model model) {
        Auction auction = auctionService.getAuctionById(id);
        if (auction == null) {
            return "redirect:/auctions";
        }
        
        User currentUser = getCurrentUser();
        List<AuctionBid> bids = auctionService.getBidsForAuction(id);
        model.addAttribute("auction", auction);
        model.addAttribute("bids", bids);
        
        // Check if the current user can bid
        boolean canBid = currentUser != null && !auction.getSeller().getId().equals(currentUser.getId()) && auction.getStatus() == Auction.AuctionStatus.ACTIVE;
        model.addAttribute("canBid", canBid);
        
        // Minimum bid calculations
        double minRequiredBid = auction.getCurrentPrice() + (bids.isEmpty() ? 0.0 : auction.getMinIncrement());
        model.addAttribute("minRequiredBid", minRequiredBid);
        
        return "auction-details";
    }

    @PostMapping("/auctions/{id}/bid")
    public String placeBid(
            @PathVariable Long id,
            @RequestParam Double bidPrice,
            RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            auctionService.placeBid(id, currentUser.getId(), bidPrice);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt giá thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/auctions/" + id;
    }

    @PostMapping("/auctions/{id}/pay")
    public String payAuction(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            auctionService.payAuction(id, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Thanh toán thành công! Thẻ bài đã thuộc về bạn.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/auctions/" + id;
    }

    @GetMapping("/seller/auctions/new")
    public String newAuctionForm(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null || (currentUser.getRole() != User.Role.SELLER && currentUser.getRole() != User.Role.ADMIN)) {
            return "redirect:/login";
        }
        
        model.addAttribute("activeListings", listingRepository.findBySellerIdAndStatus(currentUser.getId(), Listing.ListingStatus.ACTIVE));
        model.addAttribute("sets", cardSetRepository.findAll());
        model.addAttribute("conditions", Listing.CardCondition.values());
        return "seller/new-auction";
    }

    @PostMapping("/seller/auctions/new")
    public String createAuction(
            @RequestParam String source,
            @RequestParam(required = false) Long listingId,
            @RequestParam(required = false) String cardName,
            @RequestParam(required = false) Long setId,
            @RequestParam(required = false) String rarity,
            @RequestParam(required = false) String collectorNumber,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String cardDescription,
            @RequestParam Double startPrice,
            @RequestParam Double minIncrement,
            @RequestParam String endTimeStr,
            @RequestParam Listing.CardCondition condition,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null || (currentUser.getRole() != User.Role.SELLER && currentUser.getRole() != User.Role.ADMIN)) {
            return "redirect:/login";
        }

        try {
            Card card;
            if ("collection".equals(source)) {
                if (listingId == null) {
                    throw new IllegalArgumentException("Vui lòng chọn thẻ bài đang đăng bán.");
                }
                Listing listing = listingRepository.findById(listingId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin đăng bán tương ứng."));
                if (!listing.getSeller().getId().equals(currentUser.getId())) {
                    throw new IllegalArgumentException("Thẻ bài đăng bán này không thuộc về bạn.");
                }
                card = listing.getCard();
            } else if ("new".equals(source)) {
                if (cardName == null || cardName.trim().isEmpty() || setId == null) {
                    throw new IllegalArgumentException("Vui lòng điền đầy đủ tên thẻ bài và chọn Set.");
                }
                CardSet cardSet = cardSetRepository.findById(setId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bộ bài (Set)."));
                card = Card.builder()
                        .cardSet(cardSet)
                        .name(cardName.trim())
                        .rarity(rarity != null ? rarity.trim() : "Common")
                        .collectorNumber(collectorNumber != null ? collectorNumber.trim() : "N/A")
                        .language(language != null ? language.trim() : "EN")
                        .imageUrl(imageUrl != null ? imageUrl.trim() : "")
                        .description(cardDescription != null ? cardDescription.trim() : "Thẻ bài mới tải lên bởi người bán.")
                        .build();
                card = cardRepository.save(card);
            } else {
                throw new IllegalArgumentException("Nguồn gốc thẻ bài không hợp lệ.");
            }

            LocalDateTime endTime = LocalDateTime.parse(endTimeStr);
            auctionService.createAuction(currentUser.getId(), card.getId(), startPrice, minIncrement, endTime, condition, description);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo phiên đấu giá thẻ bài thành công!");
            return "redirect:/auctions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo đấu giá: " + e.getMessage());
            return "redirect:/seller/auctions/new";
        }
    }
}
