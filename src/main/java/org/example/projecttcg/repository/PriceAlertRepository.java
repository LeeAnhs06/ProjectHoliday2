package org.example.projecttcg.repository;

import org.example.projecttcg.model.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    List<PriceAlert> findByUserId(Long userId);
    List<PriceAlert> findByCardIdAndStatus(Long cardId, PriceAlert.AlertStatus status);
}
