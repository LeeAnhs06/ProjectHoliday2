package org.example.projecttcg.service;

import lombok.RequiredArgsConstructor;
import org.example.projecttcg.model.*;
import org.example.projecttcg.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExternalApiService {

    private final GameRepository gameRepository;
    private final CardSetRepository cardSetRepository;
    private final CardRepository cardRepository;
    private final PriceRecordRepository priceRecordRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public List<Card> fetchAndSaveFromPokemonApi(String keyword) {
        String url = "https://api.pokemontcg.io/v2/cards?q=name:" + keyword.trim() + "*&pageSize=10";
        List<Card> results = new ArrayList<>();
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
                
                Game pokemonGame = gameRepository.findByCode("pokemon")
                        .orElseGet(() -> gameRepository.save(Game.builder().name("Pokemon TCG").code("pokemon").build()));

                for (Map<String, Object> cardData : dataList) {
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

                    // Check if card already exists
                    List<Card> existing = cardRepository.searchCards(name, pokemonGame.getId(), cardSet.getId(), null, null);
                    if (!existing.isEmpty()) {
                        results.add(existing.get(0));
                        continue;
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
                    results.add(savedCard);

                    // Parse actual TCGPlayer USD price from Pokémon TCG API response
                    double usdPrice = 1.0; // Default fallback
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
                    double realMarketPriceVnd = usdPrice * 25000; // Convert USD to VND

                    // Save price record
                    priceRecordRepository.save(PriceRecord.builder()
                            .card(savedCard)
                            .recordedAt(LocalDateTime.now())
                            .lowestPrice(realMarketPriceVnd * 0.9)
                            .medianPrice(realMarketPriceVnd)
                            .marketPrice(realMarketPriceVnd)
                            .dataSource("Pokémon TCG API (TCGPlayer)")
                            .build());
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi kết nối tới Pokemon TCG API: " + e.getMessage());
        }
        return results;
    }

    @Transactional
    public List<Card> fetchAndSaveFromYugiohApi(String keyword) {
        String url = "https://db.ygoprodeck.com/api/v7/cardinfo.php?fname=" + keyword;
        List<Card> results = new ArrayList<>();
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
                
                Game yugiohGame = gameRepository.findByCode("yugioh")
                        .orElseGet(() -> gameRepository.save(Game.builder().name("Yu-Gi-Oh!").code("yugioh").build()));

                CardSet defaultSet = cardSetRepository.findByCode("ygo_api_set")
                        .orElseGet(() -> cardSetRepository.save(CardSet.builder()
                                .game(yugiohGame)
                                .name("Yu-Gi-Oh! API Database")
                                .code("ygo_api_set")
                                .build()));

                // Limit to top 10 results to prevent overloading
                int limit = Math.min(dataList.size(), 10);
                for (int i = 0; i < limit; i++) {
                    Map<String, Object> cardData = dataList.get(i);
                    String name = (String) cardData.get("name");
                    Long id = ((Number) cardData.get("id")).longValue();
                    String type = (String) cardData.get("type");
                    String desc = (String) cardData.get("desc");

                    List<Map<String, Object>> cardImages = (List<Map<String, Object>>) cardData.get("card_images");
                    String imageUrl = null;
                    if (cardImages != null && !cardImages.isEmpty()) {
                        imageUrl = (String) cardImages.get(0).get("image_url");
                    }

                    // Check if card already exists
                    List<Card> existing = cardRepository.searchCards(name, yugiohGame.getId(), defaultSet.getId(), null, null);
                    if (!existing.isEmpty()) {
                        results.add(existing.get(0));
                        continue;
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
                    results.add(savedCard);

                    // Parse actual TCGPlayer USD price from Yu-Gi-Oh! API response
                    double usdPrice = 1.0; // Default fallback
                    try {
                        List<Map<String, Object>> cardPrices = (List<Map<String, Object>>) cardData.get("card_prices");
                        if (cardPrices != null && !cardPrices.isEmpty()) {
                            Map<String, Object> pricesMap = cardPrices.get(0);
                            if (pricesMap.containsKey("tcgplayer_price")) {
                                usdPrice = Double.parseDouble(pricesMap.get("tcgplayer_price").toString());
                            }
                        }
                    } catch (Exception ignored) {}
                    double realMarketPriceVnd = usdPrice * 25000; // Convert USD to VND

                    // Save price record
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
        } catch (Exception e) {
            System.err.println("Lỗi khi kết nối tới Yu-Gi-Oh! API: " + e.getMessage());
        }
        return results;
    }
}
