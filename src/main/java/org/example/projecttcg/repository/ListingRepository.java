package org.example.projecttcg.repository;

import org.example.projecttcg.model.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findByCardIdAndStatus(Long cardId, Listing.ListingStatus status);
    List<Listing> findBySellerId(Long sellerId);
    List<Listing> findByStatus(Listing.ListingStatus status);
    List<Listing> findBySellerIdAndStatus(Long sellerId, Listing.ListingStatus status);

    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' AND " +
           "(:gameId IS NULL OR l.card.cardSet.game.id = :gameId) AND " +
           "(:setId IS NULL OR l.card.cardSet.id = :setId) AND " +
           "(:condition IS NULL OR l.cardCondition = :condition) AND " +
           "(:minPrice IS NULL OR l.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR l.price <= :maxPrice)")
    List<Listing> searchMarketplace(
        @Param("gameId") Long gameId,
        @Param("setId") Long setId,
        @Param("condition") Listing.CardCondition condition,
        @Param("minPrice") Double minPrice,
        @Param("maxPrice") Double maxPrice
    );
}
