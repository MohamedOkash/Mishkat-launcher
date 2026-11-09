# سجل القرارات المعمارية — DECISIONS.md

> هذا الملف append-only وفق القاعدة **R17**. أي تعديل على القواعد أو الكود المثبت في `PROJECT_BRIEF.md` يُسجل هنا بموافقة المستخدم.

---

## D-001 (2026-11-08) — تصحيح `RemoteUnificationLayer` (§7) لتمرير أحداث التنقل والصوت
- **السياق**: الكود المرفق في §7 كان يحتوي على:
  1. صيغة Kotlin غير صالحة (`object NavigateUp, NavigateDown, ... : MishkatAction()`).
  2. إرجاع `true` من `onKeyEvent` لكل الأزرار المترجمة (بما فيها الأسهم `DPAD_*` و `CONFIRM` و `BACK` و `VOLUME_*`)، مما يجعل `Activity.dispatchKeyEvent` يستهلك الحدث ويمنع محرك التركيز في Compose والنظام من استلامه.
  3. منع التكرار `repeatCount > 0` للأسهم، مما يعطّل السكرول السريع بالضغط المطول.
- **القرار (بموافقة المستخدم)**:
  - فصل كل `data object` في `MishkatAction`.
  - تقسيم الأفعال إلى **Pass-Through** (`Navigate*`, `Confirm`, `Back`, `Home`, `Volume*`, `Mute`) و **App-Consumed** (`Menu`, `PlayPause`, `Next`, `Previous`, `Search`, `Settings`, `ChannelNumber`).
  - الأفعال Pass-Through تُبث في `actions` عبر `tryEmit` (احترامًا لـ R5) ثم ترجع `PASS` إذا كان الزر قياسيًا، أو تعيد توجيه الكود القياسي المكافئ إذا جاءت من ريموت غير قياسي (مثل `AirMouse BUTTON_1 -> KEYCODE_DPAD_CENTER`).
  - السماح بـ `repeatCount > 0` لأزرار الاتجاهات والصوت فقط.

## D-002 (2026-11-08) — الاعتماد على `compose.foundation` بدل `androidx.tv:tv-foundation`
- **السياق**: قوائم `TvLazyRow` / `TvLazyColumn` في `tv-foundation` تم إيقافها (deprecated) في الإصدارات الحديثة لصالح `LazyRow` / `LazyColumn` القياسية في `androidx.compose.foundation` مع الاحتفاظ بـ `androidx.tv:tv-material:1.1.0`.
- **القرار**: استخدام `androidx.compose.foundation` مع `androidx.tv:tv-material:1.1.0` لتفادي تحذيرات الإيقاف وتقليل حجم الـ APK.

## D-003 (2026-11-08) — إعدادات SDK والتوافق المستقبلي مع الشاشات
- **السياق**: المكتبات الحديثة (Compose BOM 2026.09 / Core 1.19 / Activity 1.13 / Lifecycle 2.11) تتطلب `compileSdk = 37.2`، بينما البريف يثبت `minSdk = 24` و `targetSdk = 35`. كما طلب المستخدم مراعاة قابلية التوسع لاحقًا لجميع الشاشات.
- **القرار**: ضبط `compileSdk = 37.2` (`release(37) { minorApiLevel = 2 }`), `minSdk = 24`, `targetSdk = 35`. هذا يغطي كل أجهزة Android TV / Google TV وبوكسات الصيني من Android 7.0 حتى Android 15/16+. (ملاحظة: شاشات Samsung Tizen و LG webOS غير المبنية على أندرويد تتطلب لاحقًا عميل Web/KMP مستقل خارج نطاق APK الأندرويد، لذا تم الحفاظ على منطق البيانات والنماذج بصيغ Kotlin/JSON قياسية يسهل مشاركتها مستقبلاً).

## D-004 (2026-11-08) — الثيمات الثلاثة المدمجة وتوسعة اختيارية لـ Theme JSON Schema v1
- **السياق**: المستخدم أوضح أن المشروع جديد بالكامل ويركز على واجهة إسلامية نظيفة خالية من الإعلانات والبرامج المثقلة للجهاز.
- **القرار**:
  - اعتماد 3 ثيمات مدمجة بهوية إسلامية هادئة: **رمال (`rimal`)** بالقيم المذكورة في §8، **نور (`noor`)** (أخضر زمردي وذهبي مستوحى من المسجد النبوي)، و **ليلة (`layla`)** (كحلي ليلي هادئ للغرف المظلمة).
  - إضافة حقول اختيارية متوافقة رجعيًا في `ThemeSpec` (`nameEn`, `wallpaper.gradient`, `wallpaper.pattern`) لرسم زخرفة النجمة الثمانية الهندسية كـ Vector Path مخبأ (`drawWithCache`) بدون صور نقطية ثقيلة وبدون أي تخصيص أثناء السكرول.

## D-005 (2026-11-08) — تخزين عداد الانهيارات (`CrashGuard`) في `SharedPreferences`
- **السياق**: القاعدة R10 تشترط كشف 3 انهيارات خلال 60 ثانية لتفعيل Safe Mode. عند حدوث `UncaughtException` تموت العملية فورًا فلا يضمن `DataStore` غير المتزامن كتابة الطابع الزمني قبل موت العملية.
- **القرار**: استخدام `SharedPreferences.edit().commit()` داخل `UncaughtExceptionHandler` فقط لحفظ طوابع الانهيار كاستثناء موثق ومحدود لضمان عمل Safe Mode بموثوقية 100%.

## D-006 (2026-11-08) — تصحيح مسار API إذاعات القرآن (`mp3quran.net/api/v3/radios`)
- **السياق**: البريف في §3 يذكر الرابط `https://mp3quran.net/api/v3/radio?language=ar`، وعند فحصه فعليًا يرجع `404 Not Found`، بينما المسار الرسمي الفعال في API v3 هو `https://mp3quran.net/api/v3/radios?language=ar` (و`language=eng` للأسماء الإنجليزية) ويرجع 177 إذاعة قرآنية بروابط `https://backup.qurango.net/radio/*`.
- **القرار**: استخدام المسار الصحيح `/api/v3/radios` لبناء `assets/stations_snapshot.json` (177 محطة باللغتين العربية والإنجليزية) وللتحديث الاختياري عبر الشبكة، مع عدم الحاجة لأي استثناء `cleartext` لأن جميع الروابط تعمل عبر `HTTPS` (R22).

## D-007 (2026-11-08) — إلغاء تسجيل الراديو (S2.5) والاكتفاء بالبث المباشر لجميع القراء
- **السياق**: طلب المستخدم صراحةً عدم إضافة ميزة تسجيل المقاطع الصوتية والاكتفاء بالبث المباشر (Live Streaming) مع توفير جميع القراء وتصنيفهم للوصول السريع.
- **القرار**: إلغاء Sprint S2.5 (تسجيل الراديو) نهائيًا حفاظًا على خفة اللانشر ومساحة الجهاز، وتوجيه الجهود لتوسعة تجربة القراء المباشرة (تصنيف القراء + بحث سريع بالأحرف) ضمن Sprint S3.
