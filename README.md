# مشكاة لانشر 2.0 | Mishkat TV Launcher 2.0 📺✨

> **مِشكاة 2.0** — لانشر تلفزيوني ذكي فائق السلاسة بتصميم زجاجي سائل (Liquid Glass) مستوحى من فلسفة وهندسة **Apple TV tvOS**، مصمم خصيصاً لأجهزة **Android TV** و **Google TV** وشاشات التلفزيون الذكية والـ TV Boxes.

[![Release](https://img.shields.io/badge/Release-v2.0.0-gold?style=for-the-badge&logo=android)](https://github.com/MohamedOkash/Mishkat-launcher/releases/latest)
[![Android](https://img.shields.io/badge/Android-7.0%20to%2015%20(API%2024--35)-brightgreen?style=for-the-badge&logo=android)](https://github.com/MohamedOkash/Mishkat-launcher)
[![Design](https://img.shields.io/badge/UI%2FUX-Apple%20tvOS%20Liquid%20Glass-blue?style=for-the-badge&logo=apple)](https://github.com/MohamedOkash/Mishkat-launcher)
[![License](https://img.shields.io/badge/License-Proprietary%20%2F%20Personal-purple?style=for-the-badge)](https://github.com/MohamedOkash/Mishkat-launcher)

---

## 🌟 ميزات الإصدار 2.0.0 الجديد (Version 2.0.0 Highlights)

### 1. 🍏 واجهة تحكم فائقة السلاسة بنمط Apple TV (tvOS Precision UI/UX)
- **شريط علوي أفقي موزع (Spaced TopBar)**: تصميم متباعد يمنع أي تداخل بين الأزرار، مع خطوط واضحة دون أي التفاف رأسي للكلمات (`maxLines = 1`).
- **كبسولة الضبط المباشرة (`⚙ الضبط`)**: وصول فوري إلى مركز التحكم بجوار شعار `✦ مشكاة` مع تمييز لوني ذهبي واضح.
- **استجابة فورية 0ms**: انتقال فوري بين العناصر عبر أزرار الريموت (D-Pad) مع إطارات بلورية زجاجية ولمعان تفاعلي عند التركيز.

### 2. ⚙ مركز إعدادات موحد ومقسّم (Apple TV Split-View Settings)
- **تصميم الشاشة المنقسمة**: قائمة جانبية منظمة تضم 6 تصنيفات رئيسية، وبجانبها بطاقات التحكم الزجاجية التفاعلية مع شارات كبسولية للقيم الحالية وأسهم توجيه `›`.
- **قوائم منسدلة فورية (Modal Dropdown Sheets)**:
  - **قائمة المدن 📍**: اختيار مدينتك لحساب مواقيت الصلاة الفلكية بنقرة واحدة من قائمة كاملة بالمدن المصرية والعربية، دون الحاجة للتقليب الفردي المرهق.
  - **قائمة المؤذنين 🕌**: اختيار صوت المؤذن من بين 10 أصوات حقيقية لكبار المشايخ (الحرم المكي، المسجد النبوي، الشيخ عبد الباسط، المنشاوي، مصطفى إسماعيل، محمد رفعت، إلخ) مع زر استماع ومعاينة صوتية فورية.
  - **حجم الخطوط 🔤**: اختيار مباشر بين (مدمج 90%، قياسي متوازن 100%، كبير واضح 115%).

### 3. 🎬 مشغل فيديو سينمائي متطور بنمط VLC (VLC-Style Flex Scrubber)
- **تشغيل محلي وفوري عبر الهاردوير**: دعم التشغيل المباشر لأفلام ومقاطع الـ 4K و 1080p BluRay من الذاكرة الداخلية وUSB عبر `FileDataSource` دون أي تأخير أو وسيط شبكي.
- **تمرير مرن وسريع (Dynamic Scrubbing)**:
  - قفزات ديناميكية بنسب مئوية (1%، 2%، 5%، 10% من إجمالي مدة الفيلم) للوصول لربع أو نصف أو نهاية الفيديو في ضغطات معدودة.
  - دعم أزرار أرقام الريموت للقفز الفوري (`1` = 10%، `5` = 50% نصف الفيلم، `9` = 90%، `0` = البداية).
  - مؤشر تمرير عائم يوضح الدقيقة والنسبة المئوية: `⏩ 01:15:30 / 02:30:00 (50%)`.
- **زر الطوارئ للتشغيل الخارجي**: في حال وجود ترميز صوتي أو مرئي نادر غير مدعوم في الشاشة، تظهر بطاقة واضحة مع زر تشغيل فوري عبر مشغلات خارجية مثل **VLC** أو **MX Player** بنقرة واحدة.

### 4. 📱 إعادة ترتيب حقيقية وديناميكية لصفوف الشاشة (Live Shelf Reordering)
- تحكم كامل في ترتيب أقسام الشاشة الرئيسية (التطبيقات، البث المباشر للقنوات، الصلاة والأذكار، المباريات والرياضة، الراديو، المداخل).
- إعادة الترتيب تتم فورياً وتنعكس على الشاشة الرئيسية لحظة حفظ الترتيب عبر DataStore.

### 5. ⚽ مركز المباريات الحية الحقيقية 100% (Real Live Sports Center)
- **اتصال حي بـ ESPN API**: عرض مواعيد ونتائج المباريات الحقيقية للدوريات الكبرى:
  - الدوري السعودي (دوري روشن)
  - الدوري الإنجليزي الممتاز (Premier League)
  - الدوري الإسباني (LaLiga)
  - الدوري الإيطالي (Serie A)
  - الدوري الألماني (Bundesliga)
  - دوري أبطال أوروبا (UEFA Champions League)
- **بيانات صادقة 100%**: إظهار المباريات المباشرة الحالية مع دقيقة المباراة، والمباريات المنتهية بنتيجتها الفعلية، والمباريات القادمة بموعدها الصحيح، دون أي نتائج وهمية أو مصطنعة.

### 6. 🎨 24 ثيم وخلفية 4K حقيقية مع مؤثرات حية تفاعلية
- **الفصول الأربعة الحية**:
  - ❄ **شتاء وغابة الثلوج**: تساقط ثلوج ثلاثي الأبعاد.
  - 🌧 **أمطار الشتاء**: قطرات المطر وسيول الزجاج المتحركة.
  - ⚡ **العاصفة الرعدية**: ومضات البرق والسحب الداكنة.
  - 🌸 **ربيع الأزهار**: أشجار الساكورا وبتلات الكرز المتطايرة.
  - 🌊 **أمواج وغروب الصيف**: تموجات وانعكاسات مياه البحر الذهبية.
  - 🍂 **خريف الغابات**: تساقط أوراق الشجر الذهبية.
- **المقدسات الإسلامية بدقة 4K Ultra HD**:
  - 🕋 الحرم المكي والكعبة المشرفة.
  - 🕌 المسجد النبوي الشريف بالمدينة المنورة.
  - 🕌 المسجد الكبير والقباب والعمارة الإسلامية.
- **شاشات OLED الداكنة الفاخرة**:
  - أونيكس أسود فاخر (Deep Onyx Luxury).
  - ألياف الكربون التكنولوجية (Carbon Dark Mesh).
- **انسجام الزجاج السائل**: تلوين الزجاج السائل والأزرار تلقائياً بألوان الثيم والخلفية النشطة.

### 7. 🕌 الصلاة والأذان التلقائي والقرآن الكريم
- حساب فلكي دقيق لمواقيت الصلاة حسب إحداثيات مدينتك.
- تنبيه أذان منبثق تلقائي وقت حلول الصلاة مع شاشة أذكار ودعاء ما بعد الأذان.
- إذاعة القرآن الكريم من القاهرة ومكة المكرمة تعمل 24/7 مع مشغل صوتي في الخلفية ومؤقت نوم ذكي.

### 8. 🛠️ منظومة صيانة الرام والذاكرة الحقيقية
- كنس وتفريغ حقيقي للعمليات الدعائية العالقة في الخلفية عبر نواة النظام (`killBackgroundProcesses`).
- فحص وتفريغ حقيقي للذاكرة المؤقتة (Cache Cleaner).
- مراقبة حية لنسبة استهلاك الرام ودرجة حرارة المعالج (SoC Temperature).

### 9. 🔄 نظام التحديث الهوائي التلقائي (Direct GitHub OTA Updates)
- فحص دوري للتحديثات مباشرة من مستودع GitHub عبر ملف `version.json`.
- إشعار فوري عند صدور أي إصدار جديد مع إمكانية التحديث المباشر من داخل الشاشة.

---

## 📦 حزم التوزيع والتحميل (Downloads & Installation)

| الحزمة | المنصة | الرابط المباشر |
| :--- | :--- | :--- |
| **Mishkat Launcher v2.0.0 (APK)** | Android TV / Google TV / Boxes | [تحميل APK الأحدث](https://github.com/MohamedOkash/Mishkat-launcher/releases/latest/download/MishkatLauncher-Android-Latest.apk) |
| **Mishkat Remote Companion (APK)** | هواتف أندرويد (ريموت تحكم) | [تحميل APK الريموت](https://github.com/MohamedOkash/Mishkat-launcher/releases/latest/download/MishkatRemote-AndroidMobile-v1.9.0.apk) |

### التثبيت السريع عبر الـ USB أو ADB:
```bash
# التثبيت عبر ADB على التلفزيون:
adb connect <IP_ADDRESS>:5555
adb install -r MishkatLauncher-Android-Latest.apk

# تعيين مشكاة كـ Launcher افتراضي:
adb shell cmd package set-home-activity com.mishkat.launcher/.MainActivity
```

---

## 🛠️ التقنيات المستخدمة (Tech Stack)

- **Language**: Kotlin 2.1 (Strict Typing & Coroutines)
- **UI Framework**: Jetpack Compose for TV (`androidx.tv.material3`) + Apple tvOS Liquid Glass Modifiers
- **Media Engine**: AndroidX Media3 1.5 (ExoPlayer + Direct FileDataSource + Session Service)
- **Live Sports API**: ESPN Scoreboard REST API (Real-time live scores and schedules)
- **Persistence**: Jetpack DataStore Preferences
- **Architecture**: MVI / Unidirectional Data Flow with StateFlow
- **Compatibility**: Android 7.0 (Nougat / API 24) up to Android 15 (API 35+)

---

## 👨‍💻 المطور (Developer)

- **م. محمد عكاش (Eng. Mohamed Okash)**
- **الموقع الشخصي**: [mohamed-okash.web.app](https://mohamed-okash.web.app)
- **البريد الإلكتروني**: mohamed.okash1998@gmail.com
- **التخصص**: أنظمة أندرويد التلفزيونية المتقدمة • هندسة البنية التحتية والسلامة الصناعية

---

## 📄 الترخيص (License)

جميع حقوق التطوير والتصميم محفوظة © 2026 م. محمد عكاش.
جميع حقوق أسماء القنوات وبث الإذاعات والشعارات الرياضية محفوظة لشبكاتها الأصلية.
