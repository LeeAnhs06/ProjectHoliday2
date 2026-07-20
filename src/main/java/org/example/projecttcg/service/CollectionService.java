package org.example.projecttcg.service;

import lombok.RequiredArgsConstructor;
import org.example.projecttcg.model.CollectionItem;
import org.example.projecttcg.model.WishlistItem;
import org.example.projecttcg.repository.CollectionItemRepository;
import org.example.projecttcg.repository.WishlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionService {

    private final CollectionItemRepository collectionItemRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final CardService cardService;

    // Collection Items
    public List<CollectionItem> getCollectionByUser(Long userId) {
        return collectionItemRepository.findByUserId(userId);
    }

    @Transactional
    public CollectionItem addToCollection(CollectionItem item) {
        return collectionItemRepository.save(item);
    }

    @Transactional
    public void removeFromCollection(Long itemId) {
        collectionItemRepository.deleteById(itemId);
    }

    public double calculatePortfolioValue(Long userId) {
        List<CollectionItem> items = getCollectionByUser(userId);
        double total = 0.0;
        for (CollectionItem item : items) {
            double marketPrice = cardService.calculateMarketPrice(item.getCard().getId());
            total += marketPrice * item.getQuantity();
        }
        return total;
    }

    // Wishlist Items
    public List<WishlistItem> getWishlistByUser(Long userId) {
        return wishlistItemRepository.findByUserId(userId);
    }

    @Transactional
    public WishlistItem addToWishlist(WishlistItem item) {
        if (wishlistItemRepository.existsByUserIdAndCardId(item.getUser().getId(), item.getCard().getId())) {
            return wishlistItemRepository.findByUserIdAndCardId(item.getUser().getId(), item.getCard().getId()).get();
        }
        return wishlistItemRepository.save(item);
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long cardId) {
        wishlistItemRepository.findByUserIdAndCardId(userId, cardId)
                .ifPresent(wishlistItemRepository::delete);
    }

    public boolean isInWishlist(Long userId, Long cardId) {
        return wishlistItemRepository.existsByUserIdAndCardId(userId, cardId);
    }
}
