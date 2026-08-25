package az.azerkalori.catalog.config;

import az.azerkalori.catalog.entity.Product;
import az.azerkalori.catalog.repo.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * İlk işə düşəndə (products cədvəli boşdursa) kataloqu
 * resources/seed/products.csv faylından doldurur.
 *
 * CSV format (başlıq sətri var):
 *   name,brand,category,calories,proteinG,fatG,carbsG
 * Dəyərlər 100 qram üçündür.
 *
 * Yeni məhsul əlavə etmək = CSV-yə sətir əlavə etmək (kod dəyişmir).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final String CSV_PATH = "seed/products.csv";

    private final ProductRepository products;

    @Override
    public void run(String... args) {
        if (products.count() > 0) {
            return;
        }
        List<Product> seed = load();
        if (seed.isEmpty()) {
            log.warn("Seed CSV boşdur və ya tapılmadı: {}", CSV_PATH);
            return;
        }
        products.saveAll(seed);
        log.info("Kataloq {} məhsulla dolduruldu ({})", seed.size(), CSV_PATH);
    }

    private List<Product> load() {
        List<Product> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ClassPathResource(CSV_PATH).getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean header = true;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (header) { header = false; continue; }          // başlığı atla
                if (line.isBlank() || line.startsWith("#")) continue; // boş/şərh sətri

                String[] c = line.split(",", -1);
                if (c.length < 7) {
                    log.warn("Səhv CSV sətri #{} (7 sütun gözlənilir): {}", lineNo, line);
                    continue;
                }
                try {
                    list.add(Product.builder()
                            .name(c[0].trim())
                            .brand(blankToNull(c[1]))
                            .category(c[2].trim())
                            .calories(parse(c[3]))
                            .proteinG(parse(c[4]))
                            .fatG(parse(c[5]))
                            .carbsG(parse(c[6]))
                            .enriched(false)
                            .build());
                } catch (NumberFormatException e) {
                    log.warn("Rəqəm oxunmadı, sətir #{} atlandı: {}", lineNo, line);
                }
            }
        } catch (Exception e) {
            log.error("Seed CSV oxunmadı ({}): {}", CSV_PATH, e.getMessage());
        }
        return list;
    }

    private static Double parse(String s) {
        String t = s.trim();
        return t.isEmpty() ? null : Double.valueOf(t.replace(',', '.'));
    }

    private static String blankToNull(String s) {
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
