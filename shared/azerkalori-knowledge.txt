# AzərKalori — Qida və Kalori Bilik Bazası

Bu sənəd AzərKalori tətbiqinin RAG chatbot-u üçün bilik bazasıdır. Məlumatlar
100 qram məhsul üçündür (əks halda ayrıca qeyd olunur).

## Əsas anlayışlar

**Kalori (kcal)** — qidanın verdiyi enerjidir. Gündəlik ehtiyac insanın yaşına,
cinsinə, çəkisinə, boyuna və fəallıq səviyyəsinə görə dəyişir.

**Makronutriyentlər:**
- **Protein (zülal):** 1 qram = 4 kcal. Əzələ və toxumaların qurulması üçün.
- **Karbohidrat:** 1 qram = 4 kcal. Əsas enerji mənbəyi.
- **Yağ:** 1 qram = 9 kcal. Enerji ehtiyatı və hormonlar üçün.

**BMR (əsas mübadilə):** İnsanın tam istirahətdə yandırdığı kaloridir.
Mifflin-St Jeor düsturu:
- Kişi: BMR = 10 × çəki(kq) + 6.25 × boy(sm) − 5 × yaş + 5
- Qadın: BMR = 10 × çəki(kq) + 6.25 × boy(sm) − 5 × yaş − 161

**TDEE (gündəlik ümumi enerji):** BMR × fəallıq əmsalı:
- Hərəkətsiz: × 1.2
- Yüngül fəal (həftədə 1-3 gün idman): × 1.375
- Orta fəal (3-5 gün): × 1.55
- Çox fəal (6-7 gün): × 1.725

**Arıqlamaq üçün:** TDEE − 500 kcal (həftədə təxminən 0.5 kq).
**Kökəlmək üçün:** TDEE + 300–500 kcal.

## Gündəlik norma nümunələri

- Orta fəal 30 yaşlı kişi (75 kq, 178 sm): ~2500 kcal
- Orta fəal 30 yaşlı qadın (62 kq, 165 sm): ~2000 kcal
- Protein norması: çəkinin hər kq-na 1.2–2.0 qram
- Su norması: gündə 30–35 ml × çəki(kq)

## Azərbaycan milli yeməkləri (100q)

- Plov (Azərbaycan): 168 kcal, protein 3.5q, yağ 4.2q, karbohidrat 29q
- Dolma (yarpaq): 145 kcal, protein 5.0q, yağ 9.0q, karbohidrat 11q
- Düşbərə: 195 kcal, protein 8.0q, yağ 7.5q, karbohidrat 24q
- Qutab (ət): 210 kcal, protein 8.5q, yağ 9.0q, karbohidrat 24q
- Kabab (quzu): 250 kcal, protein 22q, yağ 18q, karbohidrat 0q
- Lülə kabab: 260 kcal, protein 20q, yağ 20q, karbohidrat 2q
- Piti: 180 kcal, protein 9.0q, yağ 11q, karbohidrat 10q
- Bozbaş: 160 kcal, protein 8.5q, yağ 9.5q, karbohidrat 11q
- Xəngəl: 205 kcal, protein 7.0q, yağ 8.0q, karbohidrat 26q
- Paxlava: 430 kcal, protein 8.0q, yağ 22q, karbohidrat 51q
- Şəkərbura: 405 kcal, protein 7.0q, yağ 18q, karbohidrat 54q
- Təndir çörəyi: 265 kcal, protein 8.5q, yağ 1.2q, karbohidrat 53q
- Motal pendiri: 290 kcal, protein 19q, yağ 24q, karbohidrat 1.5q
- Qatıq: 60 kcal, protein 3.5q, yağ 3.2q, karbohidrat 4.5q
- Ayran: 38 kcal, protein 1.7q, yağ 2.0q, karbohidrat 3.0q

## Meyvələr (100q)

- Alma: 52 kcal, protein 0.3q, yağ 0.2q, karbohidrat 14q
- Banan: 89 kcal, protein 1.1q, yağ 0.3q, karbohidrat 23q
- Portağal: 47 kcal, protein 0.9q, yağ 0.1q, karbohidrat 12q
- Nar: 83 kcal, protein 1.7q, yağ 1.2q, karbohidrat 19q
- Üzüm: 69 kcal, protein 0.7q, yağ 0.2q, karbohidrat 18q
- Xurma: 70 kcal, protein 0.6q, yağ 0.2q, karbohidrat 18q
- Əncir: 74 kcal, protein 0.8q, yağ 0.3q, karbohidrat 19q
- Qarpız: 30 kcal, protein 0.6q, yağ 0.2q, karbohidrat 8q
- Avokado: 160 kcal, protein 2.0q, yağ 15q, karbohidrat 9q
- Kivi: 61 kcal, protein 1.1q, yağ 0.5q, karbohidrat 15q

## Giləmeyvələr (100q)

- Çiyələk: 32 kcal, protein 0.7q, yağ 0.3q, karbohidrat 8q
- Albalı: 50 kcal, protein 1.0q, yağ 0.3q, karbohidrat 12q
- Moruq: 52 kcal, protein 1.2q, yağ 0.7q, karbohidrat 12q
- Qaragilə: 57 kcal, protein 0.7q, yağ 0.3q, karbohidrat 14q

## Tərəvəzlər (100q)

- Pomidor: 18 kcal, protein 0.9q, yağ 0.2q, karbohidrat 3.9q
- Xiyar: 15 kcal, protein 0.7q, yağ 0.1q, karbohidrat 3.6q
- Kartof: 77 kcal, protein 2.0q, yağ 0.1q, karbohidrat 17q
- Yerkökü: 41 kcal, protein 0.9q, yağ 0.2q, karbohidrat 10q
- Brokoli: 34 kcal, protein 2.8q, yağ 0.4q, karbohidrat 7q
- Badımcan: 25 kcal, protein 1.0q, yağ 0.2q, karbohidrat 6q
- Kələm: 25 kcal, protein 1.3q, yağ 0.1q, karbohidrat 6q
- Sarımsaq: 149 kcal, protein 6.4q, yağ 0.5q, karbohidrat 33q

## Göyərti (100q)

- İspanaq: 23 kcal, protein 2.9q, yağ 0.4q, karbohidrat 3.6q
- Cəfəri: 36 kcal, protein 3.0q, yağ 0.8q, karbohidrat 6q
- Şüyüd: 43 kcal, protein 3.5q, yağ 1.1q, karbohidrat 7q
- Reyhan: 23 kcal, protein 3.2q, yağ 0.6q, karbohidrat 2.7q

## Qoz-fındıq (100q)

- Qoz: 654 kcal, protein 15q, yağ 65q, karbohidrat 14q
- Fındıq: 628 kcal, protein 15q, yağ 61q, karbohidrat 17q
- Badam: 579 kcal, protein 21q, yağ 50q, karbohidrat 22q
- Püstə: 560 kcal, protein 20q, yağ 45q, karbohidrat 28q

## Paxlalı və dənli (bişmiş, 100q)

- Mərci: 116 kcal, protein 9.0q, yağ 0.4q, karbohidrat 20q
- Noxud: 164 kcal, protein 8.9q, yağ 2.6q, karbohidrat 27q
- Düyü: 130 kcal, protein 2.7q, yağ 0.3q, karbohidrat 28q
- Yulaf: 68 kcal, protein 2.4q, yağ 1.4q, karbohidrat 12q

## Pəhriz məsləhətləri

- Arıqlamaq üçün gündəlik kalorini TDEE-dən 300–500 kcal az saxla, protein
  normasını qoru ki, əzələ itkisi olmasın.
- Protein doyma hissini artırır; hər yeməkdə protein mənbəyi olsun (yumurta,
  toyuq, balıq, mərci, süzmə).
- Lifli qidalar (tərəvəz, göyərti, paxlalı) həzmi yaxşılaşdırır və uzun müddət
  toxluq verir.
- Şəkərli içkilər və şirniyyatı azalt; onlar çox kalori, az doyma verir.
- Su iç: bəzən susuzluq aclıq kimi hiss olunur.
- Qoz-fındıq faydalıdır, amma kalorisi yüksəkdir — ölçüyə diqqət et (gündə
  bir ovuc).

## AzərKalori tətbiqi haqqında

AzərKalori — kalori və qidalanma izləmə tətbiqidir. Əsas funksiyalar:
- **Qida kataloqu:** Azərbaycan yeməkləri, meyvə, tərəvəz və orqanik qidaların
  hazır bazası. Kataloqda olmayan qida OpenFoodFacts API-sindən avtomatik
  gətirilir.
- **Gündəlik izləmə:** İstifadəçi yediyi qidanı qeyd edir, tətbiq günün cəmi
  kalorisini və makrolarını hesablayır.
- **Hədəf və xəbərdarlıq:** Gündəlik kalori hədəfi təyin olunur. İstifadəçi
  hədəfin 80%-nə çatanda "WARN", 100%-ni keçəndə "LIMIT" bildirişi alır.
- **Həkim nəzarəti:** İstifadəçiyə həkim təyin olunubsa və hədəf aşılırsa,
  həkimə avtomatik xəbərdarlıq (alert) göndərilir.
- **Canlı yeniləmə:** Kalori cəmi WebSocket vasitəsilə real vaxtda yenilənir.
