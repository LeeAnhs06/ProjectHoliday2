package org.example.projecttcg.controller;

import lombok.RequiredArgsConstructor;
import org.example.projecttcg.model.*;
import org.example.projecttcg.repository.CardRepository;
import org.example.projecttcg.service.PriceAlertService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class PriceAlertController {

    private final PriceAlertService priceAlertService;
    private final CardRepository cardRepository;

    @GetMapping
    public String viewAlerts(Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<PriceAlert> alerts = priceAlertService.getAlertsByUserId(currentUser.getId());
        model.addAttribute("alerts", alerts);
        return "alerts";
    }

    @PostMapping("/add")
    public String createAlert(
            @RequestParam Long cardId,
            @RequestParam Double targetPrice,
            @RequestParam(required = false) Listing.CardCondition conditionFilter,
            Model model) {
        
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        PriceAlert alert = PriceAlert.builder()
                .user(currentUser)
                .card(card)
                .targetPrice(targetPrice)
                .conditionFilter(conditionFilter)
                .notifyChannel("WEB")
                .frequency("INSTANT")
                .status(PriceAlert.AlertStatus.ACTIVE)
                .build();

        priceAlertService.createAlert(alert);
        return "redirect:/alerts";
    }

    @PostMapping("/disable/{id}")
    public String disableAlert(@PathVariable Long id) {
        priceAlertService.disableAlert(id);
        return "redirect:/alerts";
    }

    @PostMapping("/delete/{id}")
    public String deleteAlert(@PathVariable Long id) {
        priceAlertService.deleteAlert(id);
        return "redirect:/alerts";
    }
}
