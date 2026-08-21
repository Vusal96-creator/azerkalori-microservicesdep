package az.azerkalori.catalog.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "openfoodfacts", url = "${openfoodfacts.url}")
public interface OpenFoodFactsClient {

    @GetMapping("/api/v2/product/{barcode}.json")
    Map<String, Object> byBarcode(@PathVariable String barcode);
}
