package az.azerkalori.catalog.client;

import az.azerkalori.catalog.entity.Product;
import az.azerkalori.catalog.repo.ProductRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrichmentService {

    private final OpenFoodFactsClient client;
    private final ProductRepository products;

    @CircuitBreaker(name = "openfoodfacts", fallbackMethod = "fallback")
    @SuppressWarnings("unchecked")
    public Product enrich(Product product) {
        Map<String, Object> resp = client.byBarcode(product.getBarcode());
        Map<String, Object> p = (Map<String, Object>) resp.get("product");
        if (p != null) {
            Map<String, Object> n = (Map<String, Object>) p.get("nutriments");
            if (n != null) {
                product.setCalories(num(n.get("energy-kcal_100g")));
                product.setProteinG(num(n.get("proteins_100g")));
                product.setFatG(num(n.get("fat_100g")));
                product.setCarbsG(num(n.get("carbohydrates_100g")));
                product.setEnriched(true);
            }
        }
        return products.save(product);
    }

    public Product fallback(Product product, Throwable t) {
        log.warn("OpenFoodFacts unavailable ({}) — using manual data for barcode {}",
                t.getClass().getSimpleName(), product.getBarcode());
        product.setEnriched(false);
        return products.save(product);
    }

    private Double num(Object o) {
        return o == null ? null : Double.valueOf(o.toString());
    }
}
