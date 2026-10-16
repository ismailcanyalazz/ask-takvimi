# Aşk Takvimi ❤️

Sevgiline özel, tamamen Türkçe, romantik tasarımlı bir regl (adet) takip uygulaması.
Kotlin + Jetpack Compose ile yazılmış, eksiksiz bir Android Studio projesidir.

- 📴 **İnternetsiz çalışır.** Uygulamanın `INTERNET` izni bile yoktur.
- 🔒 **Veriler sadece telefonda.** Reklam, analitik, takip SDK'sı, sunucu yok.
- 🔔 **Gerçek bildirimler.** Regle 3, 2, 1 gün kala ve tahmini başlangıç günü,
  uygulama kapalıyken bile telefonun bildirim paneline düşer.
- 🩷 **Romantik ama saygılı.** Tüm mesajlar ayarlardan değiştirilebilir veya kapatılabilir.

---

## 1. APK nasıl alınır?

Üç yol var. **En kolayı A seçeneği** — bilgisayarına hiçbir program kurman gerekmez.

### A) GitHub otomatik derlesin (önerilen, program kurmadan)

1. [github.com](https://github.com) üzerinde ücretsiz bir hesap aç.
2. Yeni bir depo (repository) oluştur — "Private" seçebilirsin.
3. Bu klasördeki tüm dosyaları depoya yükle
   (web arayüzünden "Add file → Upload files" ile sürükle bırak yeterlidir;
   `.github` klasörünün de yüklendiğinden emin ol).
4. Depodaki **Actions** sekmesine gir. "APK derle" iş akışı kendiliğinden başlar;
   başlamazsa sağdaki **Run workflow** düğmesine bas.
5. 3–5 dakika sonra iş yeşile döner. İşin sayfasını aç, en altta
   **Artifacts → AskTakvimi-APK** dosyasını indir.
6. İnen ZIP'in içinden `app-debug.apk` çıkar. Bu dosyayı telefona gönder.

### B) Android Studio ile (bilgisayarında)

1. [Android Studio](https://developer.android.com/studio) kur (ücretsiz).
2. **File → Open** ile bu klasörü aç. İlk açılışta Gradle gerekli her şeyi indirir.
3. Üst menüden **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. İşlem bitince çıkan bildirimde **locate** bağlantısına tıkla.
   APK şuradadır: `app/build/outputs/apk/debug/app-debug.apk`

### C) Komut satırı ile

Java 17 ve Android SDK kurulu olmalı (`ANDROID_HOME` tanımlı olmalı):

```bash
chmod +x gradlew
./gradlew testDebugUnitTest   # önce testler
./gradlew assembleDebug       # APK üret
```

Sonuç: `app/build/outputs/apk/debug/app-debug.apk`

---

## 2. APK telefona nasıl kurulur?

1. APK dosyasını telefona gönder (WhatsApp, e-posta, kablo, Drive — fark etmez).
2. Dosyaya dokun. Telefon "bilinmeyen kaynaklardan yükleme" uyarısı verirse,
   çıkan ekranda ilgili uygulamaya (örn. Dosyalar veya Chrome) izin ver.
3. **Yükle** de ve aç.

> Bu bir "debug" imzalı APK'dır: kişisel kullanım ve yan yükleme (sideload) için
> uygundur, Play Store'a yüklenemez. Play Store'a yüklemek istersen kendi imza
> anahtarını oluşturup `assembleRelease` ile derlemen gerekir.

---

## 3. İlk açılışta yapılması gerekenler

Bildirimlerin gerçekten gelmesi için üç şey önemli:

| Ne | Nerede | Neden |
|---|---|---|
| **Bildirim izni** | Uygulama ilk açıldığında sorar | Android 13+ sürümlerinde zorunludur |
| **Kesin alarm izni** | Ayarlar → Bildirim ayarları → "Zamanlama ayarını aç" | Bildirimin seçilen saatte gelmesi için |
| **Pil optimizasyonu** | Telefon ayarları → Uygulamalar → Aşk Takvimi → Pil → "Kısıtlama yok" | Üreticiler (Xiaomi, Samsung, Huawei, Oppo…) arka plan alarmlarını agresif şekilde kısabilir |

İzinler verilmezse uygulama **çökmez**; bildirimler ya gecikir ya da gösterilmez ve
bunu ekranda açıkça anlatır.

---

## 4. Ekranlar

1. **Açılış** — "Hoş geldin aşkım ❤️" karşılaması
2. **İlk kurulum** — 3 adımlık sihirbaz (hiçbir alan zorunlu değil)
3. **Ana sayfa** — kalan gün, döngü günü, yumurtlama, doğurganlık dönemi, yaklaşan tarihler
4. **Aylık takvim** — gerçek/tahmini günler renkli, geçmiş ve gelecek aylara geçiş
5. **Regl kaydı** — başlangıç/bitiş kaydetme, düzenleme, silme
6. **Günlük belirtiler** — kanama, ağrılar, ruh hali, uyku, iştah, not
7. **Geçmiş** — tüm dönemler ve günlük kayıtlar
8. **İstatistikler** — ortalamalar, oynama payı, en sık belirtiler, ruh hali dağılımı
9. **Bildirim ayarları** — saat, günler, metin kişiselleştirme, deneme bildirimi
10. **Uygulama ayarları** — tema, isim, romantik mesajlar, döngü varsayımları
11. **PIN / biyometrik kilit**
12. **Yedekleme, geri yükleme ve tüm verileri silme**

Alt menü: **Ana Sayfa · Takvim · Geçmiş · Ayarlar**

---

## 5. Mimari

```
app/src/main/java/com/asktakvimi/app/
├── AskTakvimiApp.kt          Uygulama başlangıcı: veri yükleme + alarm bağlama
├── MainActivity.kt           Tek Activity: açılış → kilit → kurulum → ana akış
├── data/
│   ├── Models.kt             Period, DayLog, AppSettings, enum'lar
│   ├── AppStorage.kt         JSON olarak diske yazma/okuma (filesDir)
│   ├── AppRepository.kt      Tek veri kaynağı (StateFlow) + değişiklik tetikleyicisi
│   └── PinUtil.kt            PIN için tuzlu SHA-256 özeti
├── domain/
│   ├── CycleEngine.kt        Tüm tarih/tahmin matematiği (Android'den bağımsız)
│   └── RomanticMessages.kt   Varsayılan metinler ve mesaj koleksiyonu
├── notifications/
│   ├── Notifier.kt           Kanallar, izin kontrolü, bildirim oluşturma
│   ├── ReminderScheduler.kt  AlarmManager ile planlama/iptal
│   ├── ReminderReceiver.kt   Alarm geldiğinde bildirimi gösterir (tekrarı engeller)
│   └── BootReceiver.kt       Yeniden başlatma/saat değişiminde alarmları kurar
└── ui/
    ├── AppNav.kt             Alt menü ve ekran yönlendirmesi
    ├── theme/                Renkler, tipografi, açık/koyu tema
    ├── components/           Kart, tarih/saat seçici, Türkçe tarih biçimleri
    └── screens/              12 ekran
```

### Veri akışı

`AppRepository` tek kaynaktır. Her değişiklik üç şeyi birden yapar:
diske yazar → ekranlara yayar (`StateFlow`) → `ReminderScheduler.rescheduleAll` çağırır.
Bu yüzden **regl tarihi değiştiği anda eski bildirimler iptal edilip yenileri kurulur.**

### Tahmin mantığı (`CycleEngine`)

- Döngü uzunluğu, ardışık regl başlangıçları arasındaki farkların **son 6 kaydının
  ortalamasıdır**; 15–60 gün dışındaki hatalı girişler hesaba katılmaz.
- Yeterli kayıt yoksa kullanıcının girdiği varsayım kullanılır ve ekranda
  tahminin **yaklaşık** olduğu açıkça yazar.
- Yumurtlama = bir sonraki tahmini başlangıç − 14 gün. Doğurganlık penceresi
  yumurtlamadan 5 gün önce başlar, 1 gün sonra biter.
- Tahmini tarih geçerse 15 gün boyunca "gecikti" olarak gösterilir, hemen bir
  sonraki döngüye atlanmaz.
- Döngüler arasındaki oynama 8 günden fazlaysa "düzensiz" uyarısı gösterilir.

> ⚠️ Yumurtlama ve doğurganlık tahminleri **gebelikten korunma yöntemi değildir**
> ve tıbbi tavsiye yerine geçmez. Bu uyarı uygulama içinde de birden çok yerde gösterilir.

---

## 6. Testler

Tarih hesaplamaları Android'den bağımsız yazıldığı için doğrudan test edilebilir:

```bash
./gradlew testDebugUnitTest
```

`app/src/test/java/com/asktakvimi/app/CycleEngineTest.kt` içinde 20'ye yakın test var:
ay/yıl geçişleri, artık yıl (2028 Şubat), gecikme durumu, devam eden dönem,
ortalama hesapları, düzensizlik tespiti ve takvim renklendirmesi.

---

## 7. Gizlilik

- Veriler `filesDir` içinde tek bir JSON dosyasında tutulur; bu klasöre başka
  uygulamalar erişemez.
- `android:allowBackup="false"` ve `data_extraction_rules.xml` ile buluta
  yedekleme ve cihaz aktarımı kapatılmıştır.
- Bildirimlerde **gizlilik modu**: açıkken bildirimde regl bilgisi geçmez,
  yalnızca "Sana küçük bir hatırlatma ❤️" türü nötr bir metin görünür ve
  bildirim kilit ekranında gizlenir.
- PIN düz metin olarak değil, rastgele tuzla SHA-256 özeti olarak saklanır.
- Dışa aktarılan yedek dosyası şifrelenmez — uygulama bunu ekranda belirtir.
- Tüm verileri tek dokunuşla kalıcı silme seçeneği vardır.

---

## 8. Bilinen sınırlar

- **Android kesin zamanlama garantisi vermez.** Pil tasarrufu nedeniyle bildirim
  birkaç dakika gecikebilir; uygulama bunu kullanıcıya açıkça söyler.
- Kesin alarm izni verilmezse esnek (yaklaşık) alarm kullanılır — bildirim gelir,
  saati biraz kayabilir.
- `minSdk 26` (Android 8.0 ve üzeri).
- APK "debug" imzalıdır; Play Store yayını için kendi imza anahtarın gerekir.
