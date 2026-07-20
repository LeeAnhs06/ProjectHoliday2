package org.example.projecttcg.service;

import lombok.RequiredArgsConstructor;
import org.example.projecttcg.model.Listing;
import org.example.projecttcg.model.PriceAlert;
import org.example.projecttcg.repository.PriceAlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;

    public List<PriceAlert> getAlertsByUserId(Long userId) {
        return priceAlertRepository.findByUserId(userId);
    }

    @Transactional
    public PriceAlert createAlert(PriceAlert alert) {
        alert.setStatus(PriceAlert.AlertStatus.ACTIVE);
        return priceAlertRepository.save(alert);
    }

    @Transactional
    public void checkAndTriggerAlerts(Long cardId, double currentMarketPrice) {
        List<PriceAlert> activeAlerts = priceAlertRepository.findByCardIdAndStatus(cardId, PriceAlert.AlertStatus.ACTIVE);
        for (PriceAlert alert : activeAlerts) {
            if (currentMarketPrice <= alert.getTargetPrice()) {
                alert.setStatus(PriceAlert.AlertStatus.TRIGGERED);
                priceAlertRepository.save(alert);
                // System logs/notifies (can be shown in UI)
                System.out.println("ALERT TRIGGERED: Card " + cardId + " reached target price " + alert.getTargetPrice());
            }
        }
    }

    @Transactional
    public void snoozeAlert(Long alertId) {
        PriceAlert alert = priceAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alert.setStatus(PriceAlert.AlertStatus.SNOOZED);
        priceAlertRepository.save(alert);
    }

    @Transactional
    public void disableAlert(Long alertId) {
        PriceAlert alert = priceAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alert.setStatus(PriceAlert.AlertStatus.DISABLED);
        priceAlertRepository.save(alert);
    }

    @Transactional
    public void deleteAlert(Long alertId) {
        priceAlertRepository.deleteById(alertId);
    }
}
