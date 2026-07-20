package org.example.projecttcg.repository;

import org.example.projecttcg.model.PriceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long> {
    List<PriceRecord> findByCardIdOrderByRecordedAtAsc(Long cardId);
}
