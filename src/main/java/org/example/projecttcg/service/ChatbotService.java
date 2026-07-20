package org.example.projecttcg.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projecttcg.model.Card;
import org.example.projecttcg.model.Listing;
import org.example.projecttcg.repository.CardRepository;
import org.example.projecttcg.repository.ListingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.NumberFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final CardRepository cardRepository;
    private final ListingRepository listingRepository;
    private final CardService cardService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${spring.nvidia.api-key:}")
    private String nvidiaApiKey;

    public String getChatbotResponse(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Chào bạn, tôi có thể giúp gì cho bạn hôm nay?";
        }

        // 1. Fetch matching cards from database for Context
        List<Card> matchedCards = findMatchedCards(userMessage);
        
        // 2. Build system context string
        String context = buildRagContext(matchedCards);

        // System instructions to guide the AI model
        String systemInstruction = "Bạn là Trợ lý AI chuyên nghiệp của sàn giao dịch thẻ bài TCG Market (chỉ hỗ trợ Pokemon TCG và Yu-Gi-Oh!). "
                + "Nhiệm vụ của bạn là tư vấn thông tin thẻ bài và gợi ý mua bán dựa trên kho thẻ thực tế của website. "
                + "Khi người dùng hỏi về thẻ bài, hãy dùng ngữ cảnh (context) được cung cấp dưới đây để báo giá và hướng dẫn họ click vào link liên kết. "
                + "Định dạng các đường dẫn tới thẻ bài là: [Tên thẻ](/cards/{id}) hoặc dẫn tới tin bán cụ thể. "
                + "Nếu thẻ bài người dùng tìm không nằm trong ngữ cảnh, hãy tự trả lời dựa trên kiến thức TCG của bạn và nhắc nhở họ rằng thẻ này hiện chưa có tin đăng bán nào trên website của chúng tôi.\n\n"
                + context;

        // 3. Priority 1: Check if NVIDIA API key is configured (OpenAI-compatible)
        if (nvidiaApiKey != null && !nvidiaApiKey.trim().isEmpty() && !nvidiaApiKey.contains("YOUR_")) {
            log.info("[CHATBOT] Using NVIDIA NIM AI service.");
            try {
                String url = "https://integrate.api.nvidia.com/v1/chat/completions";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + nvidiaApiKey.trim());

                // OpenAI request format
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", "meta/llama-3.1-8b-instruct");
                requestBody.put("temperature", 0.2);
                requestBody.put("max_tokens", 1024);

                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", systemInstruction));
                messages.add(Map.of("role", "user", "content", userMessage));
                requestBody.put("messages", messages);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, entity, Map.class);
                Map<String, Object> response = responseEntity.getBody();

                if (response != null && response.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                    if (!choices.isEmpty()) {
                        Map<String, Object> firstChoice = choices.get(0);
                        if (firstChoice.containsKey("message")) {
                            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                            if (message.containsKey("content")) {
                                return (String) message.get("content");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("NVIDIA API call failed: {}", e.getMessage());
                return "Chào bạn, kết nối tới trợ lý NVIDIA đang bận. Dưới đây là kết quả tra cứu nhanh:\n\n" 
                        + generateMockReply(userMessage, matchedCards);
            }
        }

        // 4. Priority 2: Check if Gemini API key is configured
        if (geminiApiKey != null && !geminiApiKey.trim().isEmpty() && !geminiApiKey.contains("YOUR_")) {
            log.info("[CHATBOT] Using Google Gemini AI service.");
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey.trim();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                String prompt = systemInstruction + "\n\nNgười dùng nói: \"" + userMessage + "\"\nHãy trả lời một cách tự nhiên, ngắn gọn và hữu ích bằng tiếng Việt:";

                // Gemini request format
                Map<String, Object> requestBody = new HashMap<>();
                List<Map<String, Object>> contents = new ArrayList<>();
                Map<String, Object> contentMap = new HashMap<>();
                List<Map<String, Object>> parts = new ArrayList<>();
                Map<String, Object> partMap = new HashMap<>();
                partMap.put("text", prompt);
                parts.add(partMap);
                contentMap.put("parts", parts);
                contents.add(contentMap);
                requestBody.put("contents", contents);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, entity, Map.class);
                Map<String, Object> response = responseEntity.getBody();

                if (response != null && response.containsKey("candidates")) {
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                    if (!candidates.isEmpty()) {
                        Map<String, Object> candidate = candidates.get(0);
                        if (candidate.containsKey("content")) {
                            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                            if (content.containsKey("parts")) {
                                List<Map<String, Object>> resParts = (List<Map<String, Object>>) content.get("parts");
                                if (!resParts.isEmpty()) {
                                    return (String) resParts.get(0).get("text");
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Gemini API call failed: {}", e.getMessage());
                return "Chào bạn, kết nối tới trợ lý Gemini đang bận. Dưới đây là kết quả tra cứu nhanh:\n\n" 
                        + generateMockReply(userMessage, matchedCards);
            }
        }

        // 5. Fallback: Mock/Local Search Mode
        log.info("[CHATBOT] Using Mock local search mode (No API Key).");
        return generateMockReply(userMessage, matchedCards);
    }

    private List<Card> findMatchedCards(String userMessage) {
        String cleanMessage = userMessage.toLowerCase();
        List<Card> allCards = cardRepository.findAll();
        List<Card> matched = new ArrayList<>();
        
        for (Card card : allCards) {
            if (cleanMessage.contains(card.getName().toLowerCase()) || 
                card.getName().toLowerCase().contains(cleanMessage) ||
                (card.getCardSet() != null && cleanMessage.contains(card.getCardSet().getName().toLowerCase()))) {
                matched.add(card);
            }
        }

        // If no direct matches, return top 5 featured cards as default context
        if (matched.isEmpty()) {
            return allCards.subList(0, Math.min(allCards.size(), 5));
        }
        return matched;
    }

    private String buildRagContext(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return "Không có dữ liệu thẻ bài khả dụng.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[NGỮ CẢNH: DANH SÁCH THẺ BÀI THỰC TẾ TRÊN WEBSITE TCG MARKET]\n");
        Locale localeVND = new Locale("vi", "VN");
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(localeVND);

        for (Card card : cards) {
            double marketPrice = cardService.calculateMarketPrice(card.getId());
            List<Listing> activeListings = listingRepository.findByCardIdAndStatus(card.getId(), Listing.ListingStatus.ACTIVE);
            
            sb.append("- Thẻ bài: ").append(card.getName())
              .append(" (Mã số: ").append(card.getCollectorNumber()).append(")")
              .append(" | Game: ").append(card.getCardSet().getGame().getName())
              .append(" | Set: ").append(card.getCardSet().getName())
              .append(" | Giá thị trường: ").append(marketPrice > 0 ? currencyFormat.format(marketPrice) : "Chưa có giá")
              .append(" | Số tin đăng bán đang có: ").append(activeListings.size())
              .append(" | Đường dẫn xem chi tiết: /cards/").append(card.getId())
              .append("\n");
        }
        return sb.toString();
    }

    private String generateMockReply(String userMessage, List<Card> matchedCards) {
        String cleanMessage = userMessage.toLowerCase();
        
        // Simple heuristic replies
        if (cleanMessage.contains("hello") || cleanMessage.contains("chào") || cleanMessage.contains("hi")) {
            return "Xin chào! Tôi là Trợ lý TCG Market. Bạn cần tôi tìm kiếm thẻ bài hay báo giá giúp bạn thẻ nào không?";
        }
        
        if (cleanMessage.contains("luật chơi") || cleanMessage.contains("cách chơi")) {
            return "Hệ thống của chúng tôi hiện hỗ trợ giao dịch các thẻ bài Pokémon TCG và Yu-Gi-Oh!. Bạn có thể mua bài trực tiếp trên Chợ (Marketplace) hoặc đăng bài đấu giá (Auctions).";
        }

        // If cards are found, list them
        if (matchedCards != null && !matchedCards.isEmpty() && matchedCards.size() < 10) {
            StringBuilder sb = new StringBuilder("Tôi đã tìm thấy thông tin các thẻ bài khớp với câu hỏi của bạn trên website:\n\n");
            Locale localeVND = new Locale("vi", "VN");
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(localeVND);
            
            for (Card card : matchedCards) {
                double marketPrice = cardService.calculateMarketPrice(card.getId());
                sb.append("🔹 **").append(card.getName()).append("** (").append(card.getCardSet().getName()).append(")\n")
                  .append("  • Giá thị trường: ").append(marketPrice > 0 ? currencyFormat.format(marketPrice) : "Chưa có giá giao dịch")
                  .append("\n  • Link xem chi tiết: [Xem tại đây](/cards/").append(card.getId()).append(")\n\n");
            }
            return sb.toString();
        }

        return "Hiện tại tôi chưa tìm thấy thẻ bài nào khớp chính xác với từ khóa \"" + userMessage + "\". Bạn vui lòng kiểm tra lại tên thẻ bài (ví dụ: Pikachu, Charizard, Blue-Eyes White Dragon...) hoặc truy cập mục Sưu Tập / Marketplace để xem nhé!";
    }
}
