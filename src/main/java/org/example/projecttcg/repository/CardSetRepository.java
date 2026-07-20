package org.example.projecttcg.repository;

import org.example.projecttcg.model.CardSet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CardSetRepository extends JpaRepository<CardSet, Long> {
    List<CardSet> findByGameId(Long gameId);
    Optional<CardSet> findByCode(String code);
}
