package org.example.projecttcg.repository;

import org.example.projecttcg.model.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    List<Auction> findByStatus(Auction.AuctionStatus status);
    List<Auction> findByStatusAndEndTimeBefore(Auction.AuctionStatus status, LocalDateTime dateTime);
    List<Auction> findBySellerId(Long sellerId);
    List<Auction> findByWinnerId(Long winnerId);
}
