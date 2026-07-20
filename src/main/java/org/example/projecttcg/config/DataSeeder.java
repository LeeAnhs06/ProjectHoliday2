package org.example.projecttcg.config;

import lombok.RequiredArgsConstructor;
import org.example.projecttcg.model.*;
import org.example.projecttcg.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

        private final UserRepository userRepository;
        private final GameRepository gameRepository;
        private final CardSetRepository cardSetRepository;
        private final CardRepository cardRepository;
        private final PriceRecordRepository priceRecordRepository;
        private final ListingRepository listingRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        public void run(String... args) throws Exception {
                if (userRepository.count() > 0) {
                        return; // Data already seeded
                }

                // 1. Seed Users
                User admin = User.builder()
                                .email("admin@tcg.com")
                                .password(passwordEncoder.encode("adminpassword"))
                                .displayName("TCG Admin")
                                .role(User.Role.ADMIN)
                                .status(User.UserStatus.ACTIVE)
                                .balance(1000000.0)
                                .phone("0123456789")
                                .address("Hanoi, Vietnam")
                                .build();

                User buyer = User.builder()
                                .email("buyer@tcg.com")
                                .password(passwordEncoder.encode("buyerpassword"))
                                .displayName("John Buyer")
                                .role(User.Role.MEMBER)
                                .status(User.UserStatus.ACTIVE)
                                .balance(5000000.0) // Loaded with money
                                .phone("0987654321")
                                .address("Ho Chi Minh City, Vietnam")
                                .build();

                User seller1 = User.builder()
                                .email("seller1@tcg.com")
                                .password(passwordEncoder.encode("seller1password"))
                                .displayName("Dragon TCG Store")
                                .role(User.Role.SELLER)
                                .status(User.UserStatus.ACTIVE)
                                .balance(0.0)
                                .phone("0909090909")
                                .address("Danang, Vietnam")
                                .build();

                User seller2 = User.builder()
                                .email("seller2@tcg.com")
                                .password(passwordEncoder.encode("seller2password"))
                                .displayName("Gamer Paradise")
                                .role(User.Role.SELLER)
                                .status(User.UserStatus.ACTIVE)
                                .balance(0.0)
                                .phone("0808080808")
                                .address("Haiphong, Vietnam")
                                .build();

                userRepository.saveAll(Arrays.asList(admin, buyer, seller1, seller2));

                // 2. Seed Games
                Game pokemon = Game.builder().name("Pokemon TCG").code("pokemon").build();
                Game yugioh = Game.builder().name("Yu-Gi-Oh!").code("yugioh").build();
                gameRepository.saveAll(Arrays.asList(pokemon, yugioh));

                // 3. Seed Sets
                CardSet sv03 = CardSet.builder().game(pokemon).name("Obsidian Flames").code("sv03").build();
                CardSet lob = CardSet.builder().game(yugioh).name("Legend of Blue Eyes White Dragon").code("lob")
                                .build();
                cardSetRepository.saveAll(Arrays.asList(sv03, lob));

                // 4. Seed Cards
                Card charizard = Card.builder()
                                .cardSet(sv03)
                                .name("Charizard ex (Special Illustration Rare)")
                                .rarity("Special Illustration Rare")
                                .collectorNumber("223/197")
                                .language("EN")
                                .isFoil(true)
                                .imageUrl("https://images.pokemontcg.io/sv3/223.png")
                                .description("Flame Pokémon. Weakness: Grass, Retreat Cost: 2.")
                                .build();

                Card bewd = Card.builder()
                                .cardSet(lob)
                                .name("Blue-Eyes White Dragon")
                                .rarity("Ultra Rare")
                                .collectorNumber("LOB-001")
                                .language("EN")
                                .isFoil(true)
                                .imageUrl("https://images.ygoprodeck.com/images/cards/89631139.jpg")
                                .description("This legendary dragon is a powerful engine of destruction. Virtually invincible, very few have faced this awesome creature and lived to tell the tale.")
                                .build();

                Card darkMagician = Card.builder()
                                .cardSet(lob)
                                .name("Dark Magician")
                                .rarity("Ultra Rare")
                                .collectorNumber("LOB-005")
                                .language("EN")
                                .isFoil(true)
                                .imageUrl("https://images.ygoprodeck.com/images/cards/46986414.jpg")
                                .description("The ultimate wizard in terms of attack and defense.")
                                .build();

                cardRepository.saveAll(Arrays.asList(charizard, bewd, darkMagician));

                // 5. Seed Price History Records
                LocalDateTime now = LocalDateTime.now();
                List<Card> cards = Arrays.asList(charizard, bewd, darkMagician);
                List<Double> basePrices = Arrays.asList(1500000.0, 3500000.0, 1200000.0);

                for (int i = 0; i < cards.size(); i++) {
                        Card card = cards.get(i);
                        double basePrice = basePrices.get(i);

                        // Generate historical prices for 7 days
                        for (int day = 7; day >= 0; day--) {
                                double variance = (Math.sin(day) * 0.05 + 0.02 * (Math.random() - 0.5));
                                double finalPrice = basePrice * (1.0 + variance);

                                PriceRecord record = PriceRecord.builder()
                                                .card(card)
                                                .recordedAt(now.minusDays(day))
                                                .lowestPrice(finalPrice * 0.9)
                                                .medianPrice(finalPrice)
                                                .marketPrice(finalPrice)
                                                .dataSource("Market Index")
                                                .build();
                                priceRecordRepository.save(record);
                        }
                }

                // 6. Seed Listings
                Listing list1 = Listing.builder()
                                .seller(seller1)
                                .card(charizard)
                                .cardCondition(Listing.CardCondition.NEAR_MINT)
                                .quantity(5)
                                .price(1480000.0)
                                .status(Listing.ListingStatus.ACTIVE)
                                .imageUrls("https://images.pokemontcg.io/sv3/223.png")
                                .description("Pack fresh Near Mint Charizard ex. Sent in a bubble mailer.")
                                .createdAt(now.minusHours(5))
                                .build();

                Listing list2 = Listing.builder()
                                .seller(seller2)
                                .card(charizard)
                                .cardCondition(Listing.CardCondition.LIGHTLY_PLAYED)
                                .quantity(2)
                                .price(1390000.0)
                                .status(Listing.ListingStatus.ACTIVE)
                                .imageUrls("https://images.pokemontcg.io/sv3/223.png")
                                .description("Slight corner wear, otherwise clean back.")
                                .createdAt(now.minusHours(2))
                                .build();

                Listing list3 = Listing.builder()
                                .seller(seller1)
                                .card(bewd)
                                .cardCondition(Listing.CardCondition.MINT)
                                .quantity(1)
                                .price(3800000.0)
                                .status(Listing.ListingStatus.ACTIVE)
                                .imageUrls("https://images.ygoprodeck.com/images/cards/89631139.jpg")
                                .description("PSA grade candidate. Absolutely pristine condition.")
                                .createdAt(now.minusDays(1))
                                .build();

                Listing list4 = Listing.builder()
                                .seller(seller2)
                                .card(darkMagician)
                                .cardCondition(Listing.CardCondition.MODERATELY_PLAYED)
                                .quantity(3)
                                .price(1100000.0)
                                .status(Listing.ListingStatus.ACTIVE)
                                .imageUrls("https://images.ygoprodeck.com/images/cards/46986414.jpg")
                                .description("Playable condition. Small scratch on the back.")
                                .createdAt(now.minusHours(12))
                                .build();

                listingRepository.saveAll(Arrays.asList(list1, list2, list3, list4));
        }
}
