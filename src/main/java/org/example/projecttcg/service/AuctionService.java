package org.example.projecttcg.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projecttcg.model.*;
import org.example.projecttcg.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionBidRepository auctionBidRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    public List<Auction> getActiveAuctions() {
        return auctionRepository.findByStatus(Auction.AuctionStatus.ACTIVE);
    }

    public Auction getAuctionById(Long id) {
        return auctionRepository.findById(id).orElse(null);
    }

    public List<AuctionBid> getBidsForAuction(Long auctionId) {
        return auctionBidRepository.findByAuctionIdOrderByBidPriceDesc(auctionId);
    }

    @Transactional
    public Auction createAuction(Long sellerId, Long cardId, Double startPrice, Double minIncrement, LocalDateTime endTime, Listing.CardCondition condition, String description) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người bán."));
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thẻ bài."));

        if (endTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải ở tương lai.");
        }

        Auction auction = Auction.builder()
                .seller(seller)
                .card(card)
                .startPrice(startPrice)
                .minIncrement(minIncrement)
                .currentPrice(startPrice)
                .endTime(endTime)
                .cardCondition(condition)
                .status(Auction.AuctionStatus.ACTIVE)
                .description(description)
                .build();

        return auctionRepository.save(auction);
    }

    @Transactional
    public AuctionBid placeBid(Long auctionId, Long userId, Double bidPrice) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));

        if (auction.getStatus() != Auction.AuctionStatus.ACTIVE || auction.getEndTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Phiên đấu giá đã kết thúc hoặc không còn hoạt động.");
        }

        if (auction.getSeller().getId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không thể đấu giá trên thẻ bài của chính mình.");
        }

        double minRequiredBid = auction.getCurrentPrice() + (auctionBidRepository.findByAuctionIdOrderByBidPriceDesc(auctionId).isEmpty() ? 0.0 : auction.getMinIncrement());
        if (bidPrice < minRequiredBid) {
            throw new IllegalArgumentException("Mức giá đặt phải tối thiểu là " + minRequiredBid + "đ.");
        }

        AuctionBid bid = AuctionBid.builder()
                .auction(auction)
                .user(user)
                .bidPrice(bidPrice)
                .bidTime(LocalDateTime.now())
                .build();

        AuctionBid savedBid = auctionBidRepository.save(bid);

        // Update current price
        auction.setCurrentPrice(bidPrice);

        // Anti-Sniping Mechanism: Extend by 3 minutes if bid placed in final 2 minutes
        if (auction.getEndTime().isBefore(LocalDateTime.now().plusMinutes(2))) {
            auction.setEndTime(auction.getEndTime().plusMinutes(3));
            log.info("Auction ID {} extended by 3 minutes due to a last-minute bid.", auctionId);
        }

        auctionRepository.save(auction);
        return savedBid;
    }

    @Scheduled(fixedRate = 30000) // Scan every 30 seconds
    @Transactional
    public void checkAndEndAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> activeExpired = auctionRepository.findByStatusAndEndTimeBefore(Auction.AuctionStatus.ACTIVE, now);

        for (Auction auction : activeExpired) {
            try {
                List<AuctionBid> bids = auctionBidRepository.findByAuctionIdOrderByBidPriceDesc(auction.getId());
                if (!bids.isEmpty()) {
                    AuctionBid highestBid = bids.get(0);
                    auction.setWinner(highestBid.getUser());
                    auction.setStatus(Auction.AuctionStatus.COMPLETED);
                    log.info("Auction ID {} completed. Winner is User ID {}.", auction.getId(), highestBid.getUser().getId());
                } else {
                    auction.setStatus(Auction.AuctionStatus.CANCELLED);
                    log.info("Auction ID {} cancelled. No bids were placed.", auction.getId());
                }
                auctionRepository.save(auction);
            } catch (Exception e) {
                log.error("Lỗi khi kết thúc phiên đấu giá ID " + auction.getId(), e);
            }
        }
    }

    @Transactional
    public void payAuction(Long auctionId, Long userId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá."));

        if (auction.getStatus() != Auction.AuctionStatus.COMPLETED) {
            throw new IllegalArgumentException("Phiên đấu giá chưa hoàn thành.");
        }

        if (auction.getWinner() == null || !auction.getWinner().getId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không phải là người chiến thắng phiên đấu giá này.");
        }

        if (auction.getIsPaid()) {
            throw new IllegalArgumentException("Hóa đơn đấu giá này đã được thanh toán rồi.");
        }

        User winner = auction.getWinner();
        User seller = auction.getSeller();
        double price = auction.getCurrentPrice();

        if (winner.getBalance() < price) {
            throw new IllegalArgumentException("Số dư tài khoản không đủ để thanh toán. Vui lòng nạp thêm tiền.");
        }

        winner.setBalance(winner.getBalance() - price);
        seller.setBalance(seller.getBalance() + price);

        userRepository.save(winner);
        userRepository.save(seller);

        auction.setIsPaid(true);
        auctionRepository.save(auction);
        log.info("Auction ID {} paid successfully. {} transferred from User {} to User {}.", auctionId, price, winner.getId(), seller.getId());
    }
}
