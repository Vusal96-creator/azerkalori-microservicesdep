package az.azerkalori.catalog.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "openfoodfacts", url = "${openfoodfacts.url}")
public interface OpenFoodFactsClient {

    @GetMapping("/api/v2/product/{barcode}.json")
    Map<String, Object> byBarcode(@PathVariable String barcode);

    // Ada görə axtarış (klassik OpenFoodFacts axtarış API-si).
    // Nümunə: /cgi/search.pl?search_terms=alma&search_simple=1&action=process&json=1&page_size=10
    @GetMapping("/cgi/search.pl?search_simple=1&action=process&json=1")
    Map<String, Object> searchByName(@RequestParam("search_terms") String name,
                                     @RequestParam("page_size") int pageSize);
}
