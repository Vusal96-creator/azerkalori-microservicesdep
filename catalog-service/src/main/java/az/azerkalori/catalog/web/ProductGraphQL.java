package az.azerkalori.catalog.web;

import az.azerkalori.catalog.entity.Product;
import az.azerkalori.catalog.repo.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductGraphQL {

    private final ProductRepository products;

    @QueryMapping
    public List<Product> searchProducts(@Argument String name,
                                        @Argument String category,
                                        @Argument Double minCalories,
                                        @Argument Double maxCalories,
                                        @Argument Double minProtein) {
        Specification<Product> spec = Specification.where(null);
        if (name != null)        spec = spec.and((r, q, cb) -> cb.like(cb.lower(r.get("name")), "%" + name.toLowerCase() + "%"));
        if (category != null)    spec = spec.and((r, q, cb) -> cb.equal(r.get("category"), category));
        if (minCalories != null) spec = spec.and((r, q, cb) -> cb.ge(r.get("calories"), minCalories));
        if (maxCalories != null) spec = spec.and((r, q, cb) -> cb.le(r.get("calories"), maxCalories));
        if (minProtein != null)  spec = spec.and((r, q, cb) -> cb.ge(r.get("proteinG"), minProtein));
        return products.findAll(spec);
    }
}
