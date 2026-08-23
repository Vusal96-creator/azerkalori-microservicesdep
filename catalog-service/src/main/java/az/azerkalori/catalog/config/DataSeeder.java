package az.azerkalori.catalog.config;

import az.azerkalori.catalog.entity.Product;
import az.azerkalori.catalog.repo.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * İlk işə düşəndə (products cədvəli boşdursa) kataloqu hazır qida datası ilə doldurur.
 * Dəyərlər 100 qram üçündür: kalori (kcal), protein (q), yağ (q), karbohidrat (q).
 * Qeyd: meyvə/tərəvəz üçün xam (çiy), dənli/paxlalı üçün bişmiş dəyərlər götürülüb.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository products;

    @Override
    public void run(String... args) {
        if (products.count() > 0) return;

        List<Product> seed = new ArrayList<>();

        // ---------- Milli yeməklər ----------
        seed.add(p("Plov (Azərbaycan)", "Milli", "MAIN", 168, 3.5, 4.2, 29));
        seed.add(p("Dolma (yarpaq)", "Milli", "MAIN", 145, 5.0, 9.0, 11));
        seed.add(p("Düşbərə", "Milli", "MAIN", 195, 8.0, 7.5, 24));
        seed.add(p("Qutab (ət)", "Milli", "MAIN", 210, 8.5, 9.0, 24));
        seed.add(p("Qutab (göy)", "Milli", "MAIN", 175, 4.5, 6.0, 26));
        seed.add(p("Kabab (quzu)", "Milli", "MEAT", 250, 22, 18, 0));
        seed.add(p("Lülə kabab", "Milli", "MEAT", 260, 20, 20, 2));
        seed.add(p("Piti", "Milli", "MAIN", 180, 9.0, 11, 10));
        seed.add(p("Bozbaş", "Milli", "MAIN", 160, 8.5, 9.5, 11));
        seed.add(p("Xəngəl", "Milli", "MAIN", 205, 7.0, 8.0, 26));
        seed.add(p("Şəkərbura", "Milli", "SWEETS", 405, 7.0, 18, 54));
        seed.add(p("Paxlava", "Milli", "SWEETS", 430, 8.0, 22, 51));
        seed.add(p("Şəkər çörəyi", "Milli", "BAKERY", 360, 9.0, 6.0, 66));
        seed.add(p("Təndir çörəyi", "Milli", "BAKERY", 265, 8.5, 1.2, 53));
        seed.add(p("Lavaş", "Milli", "BAKERY", 275, 9.0, 1.0, 56));
        seed.add(p("Motal pendiri", "Milli", "DAIRY", 290, 19, 24, 1.5));
        seed.add(p("Süzmə", "Milli", "DAIRY", 90, 9.0, 4.0, 4.0));
        seed.add(p("Qatıq", "Milli", "DAIRY", 60, 3.5, 3.2, 4.5));
        seed.add(p("Ayran", "Milli", "DRINKS", 38, 1.7, 2.0, 3.0));
        seed.add(p("Kompot (albalı)", "Milli", "DRINKS", 55, 0.2, 0.1, 14));
        seed.add(p("Firni", "Milli", "SWEETS", 120, 3.5, 3.0, 20));
        seed.add(p("Badımcan qızartması", "Milli", "MAIN", 130, 1.5, 10, 9.0));
        seed.add(p("Pomidor-xiyar salatı", "Milli", "MAIN", 45, 1.0, 2.5, 5.0));
        seed.add(p("Qovurma (quzu)", "Milli", "MEAT", 240, 18, 18, 1.0));
        seed.add(p("Şorba (toyuq)", "Milli", "MAIN", 65, 5.0, 2.5, 6.0));

        // ---------- Meyvələr ----------
        seed.add(p("Alma", "Təbii", "FRUITS", 52, 0.3, 0.2, 14));
        seed.add(p("Armud", "Təbii", "FRUITS", 57, 0.4, 0.1, 15));
        seed.add(p("Banan", "Təbii", "FRUITS", 89, 1.1, 0.3, 23));
        seed.add(p("Portağal", "Təbii", "FRUITS", 47, 0.9, 0.1, 12));
        seed.add(p("Naringi", "Təbii", "FRUITS", 53, 0.8, 0.3, 13));
        seed.add(p("Limon", "Təbii", "FRUITS", 29, 1.1, 0.3, 9));
        seed.add(p("Üzüm", "Təbii", "FRUITS", 69, 0.7, 0.2, 18));
        seed.add(p("Şaftalı", "Təbii", "FRUITS", 39, 0.9, 0.3, 10));
        seed.add(p("Ərik", "Təbii", "FRUITS", 48, 1.4, 0.4, 11));
        seed.add(p("Gavalı", "Təbii", "FRUITS", 46, 0.7, 0.3, 11));
        seed.add(p("Nar", "Təbii", "FRUITS", 83, 1.7, 1.2, 19));
        seed.add(p("Xurma", "Təbii", "FRUITS", 70, 0.6, 0.2, 18));
        seed.add(p("Əncir", "Təbii", "FRUITS", 74, 0.8, 0.3, 19));
        seed.add(p("Qarpız", "Təbii", "FRUITS", 30, 0.6, 0.2, 8));
        seed.add(p("Yemiş", "Təbii", "FRUITS", 34, 0.8, 0.2, 8));
        seed.add(p("Kivi", "Təbii", "FRUITS", 61, 1.1, 0.5, 15));
        seed.add(p("Ananas", "Təbii", "FRUITS", 50, 0.5, 0.1, 13));
        seed.add(p("Feyxoa", "Təbii", "FRUITS", 55, 0.7, 0.4, 13));
        seed.add(p("Heyva", "Təbii", "FRUITS", 57, 0.4, 0.1, 15));
        seed.add(p("Alça", "Təbii", "FRUITS", 27, 0.4, 0.1, 6.5));
        seed.add(p("Avokado", "Təbii", "FRUITS", 160, 2.0, 15, 9));

        // ---------- Giləmeyvələr ----------
        seed.add(p("Çiyələk", "Təbii", "BERRIES", 32, 0.7, 0.3, 8));
        seed.add(p("Albalı", "Təbii", "BERRIES", 50, 1.0, 0.3, 12));
        seed.add(p("Gilas", "Təbii", "BERRIES", 63, 1.1, 0.2, 16));
        seed.add(p("Moruq", "Təbii", "BERRIES", 52, 1.2, 0.7, 12));
        seed.add(p("Böyürtkən", "Təbii", "BERRIES", 43, 1.4, 0.5, 10));
        seed.add(p("Qaragilə", "Təbii", "BERRIES", 57, 0.7, 0.3, 14));
        seed.add(p("Firəngüzümü", "Təbii", "BERRIES", 44, 0.9, 0.6, 10));
        seed.add(p("Zoğal", "Təbii", "BERRIES", 44, 1.0, 0.1, 10));

        // ---------- Tərəvəzlər ----------
        seed.add(p("Pomidor", "Təbii", "VEGETABLES", 18, 0.9, 0.2, 3.9));
        seed.add(p("Xiyar", "Təbii", "VEGETABLES", 15, 0.7, 0.1, 3.6));
        seed.add(p("Kartof", "Təbii", "VEGETABLES", 77, 2.0, 0.1, 17));
        seed.add(p("Soğan", "Təbii", "VEGETABLES", 40, 1.1, 0.1, 9));
        seed.add(p("Sarımsaq", "Təbii", "VEGETABLES", 149, 6.4, 0.5, 33));
        seed.add(p("Yerkökü", "Təbii", "VEGETABLES", 41, 0.9, 0.2, 10));
        seed.add(p("Bibər (şirin)", "Təbii", "VEGETABLES", 26, 1.0, 0.3, 6));
        seed.add(p("Badımcan", "Təbii", "VEGETABLES", 25, 1.0, 0.2, 6));
        seed.add(p("Kələm", "Təbii", "VEGETABLES", 25, 1.3, 0.1, 6));
        seed.add(p("Gül kələm", "Təbii", "VEGETABLES", 25, 1.9, 0.3, 5));
        seed.add(p("Brokoli", "Təbii", "VEGETABLES", 34, 2.8, 0.4, 7));
        seed.add(p("Çuğundur", "Təbii", "VEGETABLES", 43, 1.6, 0.2, 10));
        seed.add(p("Balqabaq", "Təbii", "VEGETABLES", 26, 1.0, 0.1, 7));
        seed.add(p("Kabaçkı", "Təbii", "VEGETABLES", 17, 1.2, 0.3, 3));
        seed.add(p("Turp", "Təbii", "VEGETABLES", 16, 0.7, 0.1, 3.4));
        seed.add(p("Şalğam", "Təbii", "VEGETABLES", 28, 0.9, 0.1, 6));
        seed.add(p("Qarğıdalı", "Təbii", "VEGETABLES", 86, 3.2, 1.2, 19));
        seed.add(p("Yaşıl noxud", "Təbii", "VEGETABLES", 81, 5.4, 0.4, 14));
        seed.add(p("Lobya (yaşıl)", "Təbii", "VEGETABLES", 31, 1.8, 0.1, 7));
        seed.add(p("Göbələk", "Təbii", "VEGETABLES", 22, 3.1, 0.3, 3.3));
        seed.add(p("Zeytun", "Təbii", "VEGETABLES", 115, 0.8, 11, 6));

        // ---------- Göyərti ----------
        seed.add(p("İspanaq", "Təbii", "GREENS", 23, 2.9, 0.4, 3.6));
        seed.add(p("Kahı", "Təbii", "GREENS", 15, 1.4, 0.2, 2.9));
        seed.add(p("Cəfəri", "Təbii", "GREENS", 36, 3.0, 0.8, 6));
        seed.add(p("Şüyüd", "Təbii", "GREENS", 43, 3.5, 1.1, 7));
        seed.add(p("Keşniş", "Təbii", "GREENS", 23, 2.1, 0.5, 3.7));
        seed.add(p("Reyhan", "Təbii", "GREENS", 23, 3.2, 0.6, 2.7));
        seed.add(p("Nanə", "Təbii", "GREENS", 44, 3.3, 0.7, 8));
        seed.add(p("Kərəviz", "Təbii", "GREENS", 16, 0.7, 0.2, 3));
        seed.add(p("Turşəng", "Təbii", "GREENS", 22, 2.0, 0.7, 3));
        seed.add(p("Yaşıl soğan", "Təbii", "GREENS", 32, 1.8, 0.2, 7));

        // ---------- Qurumeyvə ----------
        seed.add(p("Kişmiş", "Təbii", "DRIED", 299, 3.1, 0.5, 79));
        seed.add(p("Qax (qurudulmuş ərik)", "Təbii", "DRIED", 241, 3.4, 0.5, 63));
        seed.add(p("Qara gavalı (qurudulmuş)", "Təbii", "DRIED", 240, 2.2, 0.4, 64));
        seed.add(p("Xurma (qurudulmuş)", "Təbii", "DRIED", 282, 2.5, 0.4, 75));
        seed.add(p("Quru əncir", "Təbii", "DRIED", 249, 3.3, 0.9, 64));

        // ---------- Qoz-fındıq ----------
        seed.add(p("Qoz", "Təbii", "NUTS", 654, 15, 65, 14));
        seed.add(p("Fındıq", "Təbii", "NUTS", 628, 15, 61, 17));
        seed.add(p("Badam", "Təbii", "NUTS", 579, 21, 50, 22));
        seed.add(p("Püstə", "Təbii", "NUTS", 560, 20, 45, 28));
        seed.add(p("Yer fındığı", "Təbii", "NUTS", 567, 26, 49, 16));
        seed.add(p("Şabalıd", "Təbii", "NUTS", 213, 2.4, 2.3, 45));

        // ---------- Paxlalı və dənli (bişmiş) ----------
        seed.add(p("Mərci (bişmiş)", "Təbii", "LEGUMES", 116, 9.0, 0.4, 20));
        seed.add(p("Noxud (bişmiş)", "Təbii", "LEGUMES", 164, 8.9, 2.6, 27));
        seed.add(p("Lobya (bişmiş)", "Təbii", "LEGUMES", 127, 8.7, 0.5, 23));
        seed.add(p("Düyü (bişmiş)", "Təbii", "GRAINS", 130, 2.7, 0.3, 28));
        seed.add(p("Qarabaşaq (bişmiş)", "Təbii", "GRAINS", 92, 3.4, 0.6, 20));
        seed.add(p("Bulqur (bişmiş)", "Təbii", "GRAINS", 83, 3.1, 0.2, 19));
        seed.add(p("Yulaf (bişmiş)", "Təbii", "GRAINS", 68, 2.4, 1.4, 12));

        // ---------- Digər əsas qidalar ----------
        seed.add(p("Yumurta", "Təbii", "OTHER", 155, 13, 11, 1.1));
        seed.add(p("Bal", "Təbii", "OTHER", 304, 0.3, 0.0, 82));
        seed.add(p("Zeytun yağı", "Təbii", "OTHER", 884, 0.0, 100, 0.0));

        products.saveAll(seed);
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
