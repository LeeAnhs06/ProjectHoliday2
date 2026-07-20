package org.example.projecttcg.repository;

import org.example.projecttcg.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByUserId(Long userId);
    Optional<WishlistItem> findByUserIdAndCardId(Long userId, Long cardId);
    boolean existsByUserIdAndCardId(Long userId, Long cardId);
}
