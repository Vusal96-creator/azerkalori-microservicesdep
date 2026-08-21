package az.azerkalori.catalog.config;

import az.azerkalori.catalog.entity.Product;
import az.azerkalori.catalog.repo.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository products;

    @Override
    public void run(String... args) {
        if (products.count() > 0) return;
        products.saveAll(List.of(
                p("Plov (Azərbaycan)", "Milli", "MAIN", 168, 3.5, 4.2, 29),
                p("Dolma (yarpaq)", "Milli", "MAIN", 145, 5.0, 9.0, 11),
                p("Düşbərə", "Milli", "MAIN", 195, 8.0, 7.5, 24),
                p("Qutab (ət)", "Milli", "MAIN", 210, 8.5, 9.0, 24),
                p("Qutab (göy)", "Milli", "MAIN", 175, 4.5, 6.0, 26),
                p("Kabab (quzu)", "Milli", "MEAT", 250, 22, 18, 0),
                p("Lülə kabab", "Milli", "MEAT", 260, 20, 20, 2),
                p("Piti", "Milli", "MAIN", 180, 9.0, 11, 10),
                p("Bozbaş", "Milli", "MAIN", 160, 8.5, 9.5, 11),
                p("Xəngəl", "Milli", "MAIN", 205, 7.0, 8.0, 26),
                p("Şəkərbura", "Milli", "SWEETS", 405, 7.0, 18, 54),
                p("Paxlava", "Milli", "SWEETS", 430, 8.0, 22, 51),
                p("Şəkər çörəyi", "Milli", "BAKERY", 360, 9.0, 6.0, 66),
                p("Təndir çörəyi", "Milli", "BAKERY", 265, 8.5, 1.2, 53),
                p("Lavaş", "Milli", "BAKERY", 275, 9.0, 1.0, 56),
                p("Motal pendiri", "Milli", "DAIRY", 290, 19, 24, 1.5),
                p("Süzmə", "Milli", "DAIRY", 90, 9.0, 4.0, 4.0),
                p("Qatıq", "Milli", "DAIRY", 60, 3.5, 3.2, 4.5),
                p("Ayran", "Milli", "DRINKS", 38, 1.7, 2.0, 3.0),
                p("Kompot (albalı)", "Milli", "DRINKS", 55, 0.2, 0.1, 14),
                p("Firni", "Milli", "SWEETS", 120, 3.5, 3.0, 20),
                p("Badımcan qızartması", "Milli", "VEGETABLES", 130, 1.5, 10, 9.0),
                p("Pomidor-xiyar salatı", "Milli", "VEGETABLES", 45, 1.0, 2.5, 5.0),
                p("Qovurma (quzu)", "Milli", "MEAT", 240, 18, 18, 1.0),
                p("Şorba (toyuq)", "Milli", "MAIN", 65, 5.0, 2.5, 6.0)
        ));
    }

    private Product p(String name, String brand, String category,
                      double kcal, double protein, double fat, double carbs) {
        return Product.builder()
                .name(name).brand(brand).category(category)
                .calories(kcal).proteinG(protein).fatG(fat).carbsG(carbs)
                .enriched(false)
                .build();
    }
}
