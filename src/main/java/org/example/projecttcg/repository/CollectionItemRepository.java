package org.example.projecttcg.repository;

import org.example.projecttcg.model.CollectionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CollectionItemRepository extends JpaRepository<CollectionItem, Long> {
    List<CollectionItem> findByUserId(Long userId);
}
