package org.example.projecttcg.repository;

import org.example.projecttcg.model.AuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {
    List<AuctionBid> findByAuctionIdOrderByBidPriceDesc(Long auctionId);
    List<AuctionBid> findByAuctionIdOrderByBidPriceAsc(Long auctionId);
}
