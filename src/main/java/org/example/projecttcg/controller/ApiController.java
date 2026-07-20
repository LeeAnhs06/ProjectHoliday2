package org.example.projecttcg.controller;

import lombok.RequiredArgsConstructor;
import org.example.projecttcg.model.PriceRecord;
import org.example.projecttcg.repository.PriceRecordRepository;
import org.springframework.web.bind.annotation.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final PriceRecordRepository priceRecordRepository;
    private final org.example.projecttcg.service.CardService cardService;
    private final org.example.projecttcg.service.ExternalApiService externalApiService;
    private final org.example.projecttcg.service.CardSyncService cardSyncService;

    @GetMapping("/cards/{cardId}/price-history")
    public List<Map<String, Object>> getPriceHistory(@PathVariable Long cardId) {
        List<PriceRecord> records = priceRecordRepository.findByCardIdOrderByRecordedAtAsc(cardId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        return records.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("date", r.getRecordedAt().format(formatter));
            map.put("price", r.getMarketPrice());
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/cards/search")
    public List<Map<String, Object>> searchCardsApi(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        List<org.example.projecttcg.model.Card> cards = cardService.searchCards(keyword, null, null, null, null);
        if (cards.isEmpty()) {
            // Fetch from APIs dynamically to populate database
            externalApiService.fetchAndSaveFromPokemonApi(keyword);
            externalApiService.fetchAndSaveFromYugiohApi(keyword);
            cards = cardService.searchCards(keyword, null, null, null, null);
        }
        
        return cards.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getName());
            map.put("setName", c.getCardSet().getName());
            map.put("gameName", c.getCardSet().getGame().getName());
            map.put("marketPrice", cardService.calculateMarketPrice(c.getId()));
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping("/admin/sync/start")
    public Map<String, Object> startSync(
            @RequestParam String gameCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "500") Integer limit) {
        
        Map<String, Object> response = new HashMap<>();
        String safeKeyword = (keyword == null) ? "" : keyword.trim();
        int safeLimit = (limit == null || limit <= 0) ? 500 : Math.min(limit, 1000);

        if ("pokemon".equalsIgnoreCase(gameCode)) {
            cardSyncService.startPokemonSync(safeKeyword, safeLimit);
            response.put("success", true);
            response.put("message", "Tiến trình đồng bộ Pokemon TCG đã bắt đầu ngầm (giới hạn " + safeLimit + " thẻ).");
        } else if ("yugioh".equalsIgnoreCase(gameCode)) {
            cardSyncService.startYugiohSync(safeKeyword, safeLimit);
            response.put("success", true);
            response.put("message", "Tiến trình đồng bộ Yu-Gi-Oh! đã bắt đầu ngầm (giới hạn " + safeLimit + " thẻ).");
        } else {
            response.put("success", false);
            response.put("message", "Trò chơi không hợp lệ: " + gameCode);
        }
        return response;
    }

    @GetMapping("/admin/sync/status")
    public org.example.projecttcg.service.CardSyncService.SyncProgress getSyncStatus(@RequestParam String gameCode) {
        org.example.projecttcg.service.CardSyncService.SyncProgress progress = cardSyncService.getProgress(gameCode);
        if (progress == null) {
            return org.example.projecttcg.service.CardSyncService.SyncProgress.builder()
                    .gameCode(gameCode)
                    .status("IDLE")
                    .message("Không có tiến trình nào đang chạy.")
                    .build();
        }
        return progress;
    }
}
