package org.example.projecttcg.service;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projecttcg.model.*;
import org.example.projecttcg.repository.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardSyncService {

    @org.springframework.beans.factory.annotation.Value("${spring.pokemontcg.api-key:}")
    private String apiKey;

    private final GameRepository gameRepository;
    private final CardSetRepository cardSetRepository;
    private final CardRepository cardRepository;
    private final PriceRecordRepository priceRecordRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private final Map<String, SyncProgress> progressMap = new ConcurrentHashMap<>();

    private Map<String, Object> executePokemonApiRequest(String url) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            headers.set("X-Api-Key", apiKey.trim());
        }
        org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
        org.springframework.http.ResponseEntity<Map> responseEntity = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                entity,
                Map.class
        );
        return responseEntity.getBody();
    }

    @Data
    @Builder
    public static class SyncProgress {
        private String gameCode;
        private String status; // "RUNNING", "COMPLETED", "FAILED"
        private int processedCount;
        private int totalCount;
        private String message;
        private String error;
    }

    public SyncProgress getProgress(String gameCode) {
        return progressMap.get(gameCode);
    }

    @Async
    public void startPokemonSync(String keyword, int limit) {
        String gameCode = "pokemon";
        boolean isKeywordEmpty = (keyword == null || keyword.trim().isEmpty());
        SyncProgress progress = SyncProgress.builder()
                .gameCode(gameCode)
                .status("RUNNING")
                .processedCount(0)
                .totalCount(0)
                .message("Đang kết nối tới Pokemon TCG API...")
                .build();
        progressMap.put(gameCode, progress);

        try {
            Game pokemonGame = gameRepository.findByCode(gameCode)
                    .orElseGet(() -> gameRepository.save(Game.builder().name("Pokemon TCG").code(gameCode).build()));

            // Determine total count first by calling page 1 with small size
            String queryParam = isKeywordEmpty ? "" : "q=name:" + keyword.trim() + "*&";
            String initialUrl = "https://api.pokemontcg.io/v2/cards?" + queryParam + "pageSize=1&page=1";
            Map<String, Object> initResponse = executePokemonApiRequest(initialUrl);
            int totalCards = 0;
            if (initResponse != null && initResponse.containsKey("totalCount")) {
                totalCards = ((Number) initResponse.get("totalCount")).intValue();
            }

            if (totalCards == 0) {
                progress.setStatus("COMPLETED");
                progress.setMessage("Không tìm thấy thẻ bài nào.");
                return;
            }

            // Cap at the requested limit
            totalCards = Math.min(totalCards, limit);

            progress.setTotalCount(totalCards);
            progress.setMessage("Tìm thấy " + totalCards + " thẻ bài. Bắt đầu tải và lưu...");

            int pageSize = 100; // Optimal page size
            int totalPages = (int) Math.ceil((double) totalCards / pageSize);
            int processed = 0;

            for (int page = 1; page <= totalPages; page++) {
                String pageUrl = "https://api.pokemontcg.io/v2/cards?" + queryParam + "pageSize=" + pageSize + "&page=" + page;
                Map<String, Object> response = executePokemonApiRequest(pageUrl);

                if (response != null && response.containsKey("data")) {
                    List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");

                    for (Map<String, Object> cardData : dataList) {
                        if (processed >= totalCards) {
                            break;
                        }
                        try {
                            saveSinglePokemonCard(cardData, pokemonGame);
                        } catch (Exception e) {
                            log.error("Lỗi khi lưu thẻ bài Pokemon: " + e.getMessage());
                        }
                        processed++;
                        progress.setProcessedCount(processed);
                        progress.setMessage("Đang lưu thẻ bài: " + processed + "/" + totalCards);
                    }
                }

                // Sleep 1 second between pages to respect Rate Limits
                if (page < totalPages && processed < totalCards) {
                    Thread.sleep(1000);
                }
            }

            progress.setStatus("COMPLETED");
            progress.setMessage("Đồng bộ hoàn tất! Đã lưu " + processed + " thẻ bài.");

        } catch (Exception e) {
            log.error("Lỗi trong quá trình đồng bộ Pokemon: ", e);
            progress.setStatus("FAILED");
            progress.setError(e.getMessage());
            progress.setMessage("Đồng bộ thất bại: " + e.getMessage());
        }
    }

    @Async
    public void startYugiohSync(String keyword, int limit) {
        String gameCode = "yugioh";
        boolean isKeywordEmpty = (keyword == null || keyword.trim().isEmpty());
        SyncProgress progress = SyncProgress.builder()
                .gameCode(gameCode)
                .status("RUNNING")
                .processedCount(0)
                .totalCount(0)
                .message("Đang kết nối tới Yu-Gi-Oh! API...")
                .build();
        progressMap.put(gameCode, progress);

        try {
            Game yugiohGame = gameRepository.findByCode(gameCode)
                    .orElseGet(() -> gameRepository.save(Game.builder().name("Yu-Gi-Oh!").code(gameCode).build()));

            CardSet defaultSet = cardSetRepository.findByCode("ygo_api_set")
                    .orElseGet(() -> cardSetRepository.save(CardSet.builder()
                            .game(yugiohGame)
                            .name("Yu-Gi-Oh! API Database")
                            .code("ygo_api_set")
                            .build()));

            String url = isKeywordEmpty 
                    ? "https://db.ygoprodeck.com/api/v7/cardinfo.php" 
                    : "https://db.ygoprodeck.com/api/v7/cardinfo.php?fname=" + keyword.trim();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
                int totalCards = dataList.size();
                
                // Cap at the requested limit
                totalCards = Math.min(totalCards, limit);

                progress.setTotalCount(totalCards);
                progress.setMessage("Tìm thấy " + totalCards + " thẻ bài. Bắt đầu lưu...");

                int processed = 0;
                for (int i = 0; i < totalCards; i++) {
                    Map<String, Object> cardData = dataList.get(i);
                    try {
                        saveSingleYugiohCard(cardData, yugiohGame, defaultSet);
                    } catch (Exception e) {
                        log.error("Lỗi khi lưu thẻ bài Yu-Gi-Oh!: " + e.getMessage());
                    }
                    processed++;
                    progress.setProcessedCount(processed);
                    progress.setMessage("Đang lưu thẻ bài: " + processed + "/" + totalCards);

                    // Add micro sleep to prevent database locking if dataset is huge
                    if (processed % 50 == 0) {
                        Thread.sleep(100);
                    }
                }

                progress.setStatus("COMPLETED");
                progress.setMessage("Đồng bộ hoàn tất! Đã lưu " + processed + " thẻ bài.");
            } else {
                progress.setStatus("COMPLETED");
                progress.setMessage("Không tìm thấy thẻ bài nào.");
            }

        } catch (Exception e) {
            log.error("Lỗi trong quá trình đồng bộ Yu-Gi-Oh!: ", e);
            progress.setStatus("FAILED");
            progress.setError(e.getMessage());
            progress.setMessage("Đồng bộ thất bại: " + e.getMessage());
        }
    }

    private void saveSinglePokemonCard(Map<String, Object> cardData, Game pokemonGame) {
        String name = (String) cardData.get("name");
        String id = (String) cardData.get("id");
        String rarity = (String) cardData.get("rarity");

        Map<String, Object> setData = (Map<String, Object>) cardData.get("set");
        String setName = (String) setData.get("name");
        String setCode = (String) setData.get("id");

        Map<String, Object> imagesData = (Map<String, Object>) cardData.get("images");
        String imageUrl = (String) imagesData.get("large");

        CardSet cardSet = cardSetRepository.findByCode(setCode)
                .orElseGet(() -> cardSetRepository.save(CardSet.builder()
                        .game(pokemonGame)
                        .name(setName)
                        .code(setCode)
                        .build()));

        List<Card> existing = cardRepository.searchCards(name, pokemonGame.getId(), cardSet.getId(), null, null);
        if (!existing.isEmpty()) {
            return;
        }

        Card card = Card.builder()
                .cardSet(cardSet)
                .name(name)
                .rarity(rarity != null ? rarity : "Common")
                .collectorNumber(id)
                .language("EN")
                .isFoil(false)
                .imageUrl(imageUrl)
                .description("Dữ liệu tự động đồng bộ từ Pokémon TCG API.")
                .build();

        Card savedCard = cardRepository.save(card);

        double usdPrice = 1.0;
        if (cardData.containsKey("tcgplayer")) {
            try {
                Map<String, Object> tcgplayer = (Map<String, Object>) cardData.get("tcgplayer");
                if (tcgplayer != null && tcgplayer.containsKey("prices")) {
                    Map<String, Object> prices = (Map<String, Object>) tcgplayer.get("prices");
                    if (prices != null) {
                        for (String key : Arrays.asList("holofoil", "normal", "reverseHolofoil", "unlimitedHolofoil")) {
                            if (prices.containsKey(key)) {
                                Map<String, Object> priceDetail = (Map<String, Object>) prices.get(key);
                                if (priceDetail != null && priceDetail.get("market") != null) {
                                    usdPrice = ((Number) priceDetail.get("market")).doubleValue();
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        double realMarketPriceVnd = usdPrice * 25000;

        priceRecordRepository.save(PriceRecord.builder()
                .card(savedCard)
                .recordedAt(LocalDateTime.now())
                .lowestPrice(realMarketPriceVnd * 0.9)
                .medianPrice(realMarketPriceVnd)
                .marketPrice(realMarketPriceVnd)
                .dataSource("Pokémon TCG API (TCGPlayer)")
                .build());
    }

    private void saveSingleYugiohCard(Map<String, Object> cardData, Game yugiohGame, CardSet defaultSet) {
        String name = (String) cardData.get("name");
        Long id = ((Number) cardData.get("id")).longValue();
        String type = (String) cardData.get("type");
        String desc = (String) cardData.get("desc");

        List<Map<String, Object>> cardImages = (List<Map<String, Object>>) cardData.get("card_images");
        String imageUrl = null;
        if (cardImages != null && !cardImages.isEmpty()) {
            imageUrl = (String) cardImages.get(0).get("image_url");
        }

        List<Card> existing = cardRepository.searchCards(name, yugiohGame.getId(), defaultSet.getId(), null, null);
        if (!existing.isEmpty()) {
            return;
        }

        Card card = Card.builder()
                .cardSet(defaultSet)
                .name(name)
                .rarity(type)
                .collectorNumber(String.valueOf(id))
                .language("EN")
                .isFoil(false)
                .imageUrl(imageUrl)
                .description(desc)
                .build();

        Card savedCard = cardRepository.save(card);

        double usdPrice = 1.0;
        try {
            List<Map<String, Object>> cardPrices = (List<Map<String, Object>>) cardData.get("card_prices");
            if (cardPrices != null && !cardPrices.isEmpty()) {
                Map<String, Object> pricesMap = cardPrices.get(0);
                if (pricesMap.containsKey("tcgplayer_price")) {
                    usdPrice = Double.parseDouble(pricesMap.get("tcgplayer_price").toString());
                }
            }
        } catch (Exception ignored) {}
        double realMarketPriceVnd = usdPrice * 25000;

        priceRecordRepository.save(PriceRecord.builder()
                .card(savedCard)
                .recordedAt(LocalDateTime.now())
                .lowestPrice(realMarketPriceVnd * 0.9)
                .medianPrice(realMarketPriceVnd)
                .marketPrice(realMarketPriceVnd)
                .dataSource("YGOPRODeck API (TCGPlayer)")
                .build());
    }
}
