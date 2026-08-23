package az.azerkalori.catalog.client;

import az.azerkalori.catalog.entity.Product;
import az.azerkalori.catalog.repo.ProductRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ada görə qida axtarışı.
 * Strategiya: ƏVVƏLCƏ lokal kataloq (sürətli, əsas Azərbaycan qidaları).
 * Lokalda tapılmasa -> OpenFoodFacts-dan ad üzrə gətir, DB-yə saxla və qaytar.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FoodSearchService {

    private static final int EXTERNAL_LIMIT = 10;

    private final OpenFoodFactsClient client;
    private final ProductRepository products;

    public List<Product> search(String name) {
        // 1) Lokal kataloq
        List<Product> local = products.findByNameContainingIgnoreCase(name);
        if (!local.isEmpty()) {
            return local;
        }
        // 2) Lokalda yoxdursa -> xarici API
        return searchExternal(name);
    }

    @CircuitBreaker(name = "openfoodfacts", fallbackMethod = "externalFallback")
    @SuppressWarnings("unchecked")
    public List<Product> searchExternal(String name) {
        Map<String, Object> resp = client.searchByName(name, EXTERNAL_LIMIT);
        List<Map<String, Object>> found = (List<Map<String, Object>>) resp.get("products");

        List<Product> result = new ArrayList<>();
        if (found == null) {
            return result;
        }

        for (Map<String, Object> pr : found) {
            String productName = str(pr.get("product_name"));
            if (productName == null || productName.isBlank()) continue;

            Map<String, Object> n = (Map<String, Object>) pr.get("nutriments");
            if (n == null) continue;

            Double kcal = num(n.get("energy-kcal_100g"));
            if (kcal == null) continue; // kalorisi olmayanı atla

            result.add(Product.builder()
                    .name(productName)
                    .brand(str(pr.get("brands")))
                    .category("EXTERNAL")
                    .barcode(str(pr.get("code")))
                    .calories(kcal)
                    .proteinG(num(n.get("proteins_100g")))
                    .fatG(num(n.get("fat_100g")))
                    .carbsG(num(n.get("carbohydrates_100g")))
                    .enriched(true)
                    .build());
        }

        // Növbəti dəfə lokaldan gəlsin deyə saxlayırıq.
        return products.saveAll(result);
    }

    // API əlçatan olmayanda (circuit breaker) boş nəticə qaytar — app dağılmasın.
    public List<Product> externalFallback(String name, Throwable t) {
        log.warn("OpenFoodFacts search unavailable ({}) for '{}'",
                t.getClass().getSimpleName(), name);
        return List.of();
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    private Double num(Object o) {
        try {
            return o == null ? null : Double.valueOf(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
