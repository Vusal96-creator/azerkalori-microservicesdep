package az.azerkalori.catalog.web;

import az.azerkalori.catalog.client.EnrichmentService;
import az.azerkalori.catalog.client.FoodSearchService;
import az.azerkalori.catalog.entity.Product;
import az.azerkalori.catalog.repo.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository products;
    private final EnrichmentService enrichment;
    private final FoodSearchService foodSearch;

    @GetMapping
    public List<Product> all() {
        return products.findAll();
    }

    // Ada görə axtarış: əvvəlcə lokal kataloq, tapılmasa OpenFoodFacts-dan gətirir.
    // Nümunə: GET /api/products/search?name=alma
    @GetMapping("/search")
    public List<Product> search(@RequestParam String name) {
        return foodSearch.search(name);
    }

    @GetMapping("/{id}")
    @Cacheable(value = "products", key = "#id")
    public Product byId(@PathVariable Long id) {
        return products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Product create(@RequestHeader("X-User-Role") String role,
                          @RequestBody Product product) {
        requireAdmin(role);
        return products.save(product);
    }

    @PostMapping("/{id}/enrich")
    public Product enrich(@RequestHeader("X-User-Role") String role,
                          @PathVariable Long id) {
        requireAdmin(role);
        Product product = byId(id);
        return enrichment.enrich(product);
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN only");
        }
    }
}
