package org.example.projecttcg.repository;

import org.example.projecttcg.model.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {
    
    @Query("SELECT c FROM Card c WHERE " +
           "(:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.collectorNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:gameId IS NULL OR c.cardSet.game.id = :gameId) AND " +
           "(:setId IS NULL OR c.cardSet.id = :setId) AND " +
           "(:rarity IS NULL OR LOWER(c.rarity) = LOWER(:rarity)) AND " +
           "(:language IS NULL OR LOWER(c.language) = LOWER(:language))")
    List<Card> searchCards(
        @Param("keyword") String keyword,
        @Param("gameId") Long gameId,
        @Param("setId") Long setId,
        @Param("rarity") String rarity,
        @Param("language") String language
    );

    @Query("SELECT c FROM Card c WHERE " +
           "(:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.collectorNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:gameId IS NULL OR c.cardSet.game.id = :gameId) AND " +
           "(:setId IS NULL OR c.cardSet.id = :setId) AND " +
           "(:rarity IS NULL OR LOWER(c.rarity) = LOWER(:rarity)) AND " +
           "(:language IS NULL OR LOWER(c.language) = LOWER(:language))")
    Page<Card> searchCardsPaginated(
        @Param("keyword") String keyword,
        @Param("gameId") Long gameId,
        @Param("setId") Long setId,
        @Param("rarity") String rarity,
        @Param("language") String language,
        Pageable pageable
    );
}
