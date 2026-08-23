package az.azerkalori.catalog.repo;

import az.azerkalori.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {
    Optional<Product> findByBarcode(String barcode);

    // Ada görə (hərf böyük/kiçik fərqi olmadan) lokal axtarış.
    List<Product> findByNameContainingIgnoreCase(String name);
}
