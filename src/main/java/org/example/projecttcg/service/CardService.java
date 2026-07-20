package org.example.projecttcg.service;

import lombok.RequiredArgsConstructor;
import org.example.projecttcg.model.Card;
import org.example.projecttcg.model.Listing;
import org.example.projecttcg.model.PriceRecord;
import org.example.projecttcg.repository.CardRepository;
import org.example.projecttcg.repository.ListingRepository;
import org.example.projecttcg.repository.PriceRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final ListingRepository listingRepository;
    private final PriceRecordRepository priceRecordRepository;

    public List<Card> searchCards(String keyword, Long gameId, Long setId, String rarity, String language) {
        return cardRepository.searchCards(
                (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim(),
                gameId, setId,
                (rarity == null || rarity.trim().isEmpty()) ? null : rarity.trim(),
                (language == null || language.trim().isEmpty()) ? null : language.trim()
        );
    }

    public Page<Card> searchCardsPaginated(String keyword, Long gameId, Long setId, String rarity, String language, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return cardRepository.searchCardsPaginated(
                (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim(),
                gameId, setId,
                (rarity == null || rarity.trim().isEmpty()) ? null : rarity.trim(),
                (language == null || language.trim().isEmpty()) ? null : language.trim(),
                pageable
        );
    }

    public Card findById(Long id) {
        return cardRepository.findById(id).orElse(null);
    }

    public double calculateMarketPrice(Long cardId) {
        // Calculate based on active listings
        List<Listing> activeListings = listingRepository.findByCardIdAndStatus(cardId, Listing.ListingStatus.ACTIVE);
        if (!activeListings.isEmpty()) {
            double[] prices = activeListings.stream()
                    .mapToDouble(Listing::getPrice)
                    .sorted()
                    .toArray();
            int n = prices.length;
            if (n % 2 == 1) {
                return prices[n / 2];
            } else {
                return (prices[n / 2 - 1] + prices[n / 2]) / 2.0;
            }
        }
        
        // Fallback to latest price record
        List<PriceRecord> records = priceRecordRepository.findByCardIdOrderByRecordedAtAsc(cardId);
        if (!records.isEmpty()) {
            return records.get(records.size() - 1).getMarketPrice();
        }
        
        return 0.0; // No price data
    }
}
