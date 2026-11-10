# CHANGELOG

> هذا الملف append-only وفق القاعدة **R17**.

## [1.8.0] — 2026-11-10 (الترقية الكبرى: المعالجة البصرية الشاملة + فلاشة الراوتر Samba SMB و FTP والهارد والـ USB + استيراد M3U وتطوير ريموت الموبايل)
### Added
- **المرحلة الأولى — المعالجة البصرية الشاملة (UI/UX Visual Polish)**:
  - `theme/ThemeWallpaper.kt`: طبقة تعتيم متدرجة داكنة ذكية (`Dark Vignette & Scrim Overlay` من `Color(0x88040914)` إلى `Color(0xB8060D1A)`) تمنع طغيان الإضاءة القوية في الخلفية على النصوص والكروت.
  - `theme/ThemeWallpaper.kt`: محرك الزجاج السائل المطور على نمط Apple iOS (Liquid Glass Materials) يدعم 3 مستويات شفافية (`Deep High-Contrast` ألفا 0.92، `Standard` ألفا 0.85، `Crystal Clear` ألفا 0.70).
  - `focus/MishkatFocus.kt`: إطار تركيز مضيء ساطع (`2.5.dp`) مع هالة ضوئية خارجية ناعمة (`Apple TV focus glow`) تمنح الكروت عمقاً بارزاً ووضوحاً تاماً عند الوقوف عليها بالريموت.
  - `theme/SectionTitlePill.kt`: كبسولة زجاجية داكنة موحدة لجميع عناوين الأقسام مع شريط جانبي ملون (`Accent Pill Indicator`) وظل نصي عميق (`blurRadius = 14f`).
  - `home/HomeScreen.kt`: كبسولة مخصصة لشعار مشكاة والساعة في الشريط العلوي، وتنظيم الشريط العلوي في مجموعتين متوازنتين ومريحتين من مسافة 3 أمتار، وحواف أمان تلفزيونية (`contentPadding = PaddingValues(horizontal = 24.dp..36.dp)`) في كافة الصفوف لانزلاق سلس للكروت.
  - `sports/SportsUi.kt`: إعادة تصميم كارت المباراة بسطر مستقل لكل فريق مع نتيجته، لمنع تداخل اللغات واقتصاص الأسماء نهائياً.
- **المرحلة الثانية — فلاشة الراوتر (Samba SMB & FTP) والهارد الخارجي والـ USB ومستعرض الملفات**:
  - إضافة مكتبات `commons-net:3.11.1` و `jcifs-ng:2.1.10` لدعم بروتوكولات FTP و SMBv1 / SMBv2 / SMBv3 متوافقة مع راوترات المنازل.
  - `storage/NetworkAndUsbStorageRepository.kt`: كاشف تلقائي للهارد الخارجي ووحدات USB، مع فحص تلقائي لعنوان بوابة الراوتر (`Gateway IP`) والمنافذ 445 و 139 و 21 في الخلفية لاكتشاف فلاشة الراوتر تلقائياً مع دعم التخصيص اليدوي.
  - `storage/LocalStreamProxy.kt`: خادم بث محلي خفيف على `127.0.0.1` لبث الفيديو والصوتيات من فلاشة الراوتر (SMB / FTP) مباشرة إلى `ExoPlayer` دون تحميل مسبق، مع دعم كامل للتقديم والترجيع (`HTTP Range`).
  - `storage/StorageExplorerOverlay.kt`: مستعرض ملفات ومشغل وسائط متكامل بتنقل D-Pad كامل وتثبيت ملفات APK.
  - `tv/TvInputsUi.kt`: كروت مباشرة لـ Samba الراوتر، و FTP الراوتر، ووحدة USB في صف المداخل بالشاشة الرئيسية.
- **المرحلة الثالثة — تطوير ريموت الموبايل المستقل وقوائم M3U**:
  - `livetv/M3uParser.kt`: محلل قوائم IPTV M3U المدمج.
  - `remote/CompanionRemoteServer.kt`: نقاط نهاية `POST /api/m3u` و `POST /api/storage` وتوسيع `/api/info` ليشمل قائمة القنوات والتطبيقات الحية.
  - `remote/ui/RemoteScreen.kt`: تبويب "📺 القنوات" وتبويب "📱 التطبيقات" مع تشغيل فوري على التلفزيون، وزر "➕ استيراد قائمة قنوات M3U للشاشة"، وزر فتح مستعرض التخزين.
- **المرحلة الرابعة — مزامنة منصات الويب (Samsung Tizen & LG webOS)**:
  - تحديث ملفات `tv-web/` بتعتيم الزجاج السائل الجديد وعناوين الأقمار الأربعة، وتجميع حزم `.wgt` و `.ipk` في `dist/`.

## [1.7.0] — 2026-11-10 (الترقية الكبرى: قاتل البرامج المثقلة + الأذان البشري النقي + مشغل الريسيفر الداخلي والأقمار + ريموت الموبايل المستقل Mishkat Remote)
### Added
- **المستوى الأول — قاتل تطبيقات الخلفية ومُسرّع الرام الذكي (Background Bloatware & Process Killer)**:
  - `tools/BackgroundProcessKiller.kt`: فحص شامل للعمليات والتطبيقات العالقة في الخلفية وإيقافها فورياً عبر `ActivityManager.killBackgroundProcesses` لتحرير الرام مع قائمة استثناء آمنة (`Whitelist`) تحمي خدمات النظام والصوت والمدخلات.
  - أوضاع الكنس التلقائي الذكي (`Auto Background Sweep`): يعمل عند إقلاع التلفزيون (`BootReceiver`)، وعند العودة للشاشة الرئيسية بعد إغلاق التطبيقات الثقيلة (`onHomeResumed`)، وتلقائياً إذا تجاوز استهلاك الرام 75%.
  - `accessibility/MishkatAccessibilityService.kt`: إضافة ميزة **الإيقاف الإجباري العميق (Deep Force-Stop)** للتطبيقات الدعائية العنيدة (PatchWall، توصيات الشاشة، تطبيقات الخلفية المعلقة).
  - `tools/DeviceToolsOverlay.kt`: واجهة إحصائيات حية تعرض عدد العمليات التي أُوقفت وحجم الرام الفعلي المحرر بالميجابايت مع زر للكنس اليدوي الفوري وتفعيل/تعطيل الحارس التلقائي.
  - تطبيق قاعدة **Zero-Regression** الصارمة: الحفاظ الكامل على كافة مؤثرات الزجاج السائل والخلفيات الحية دون أدنى تخفيض في الجودة.
- **المستوى الثاني — محرك صوت الأذان الحقيقي النقي (Real Human Adhan Audio Engine)**:
  - `res/raw/adhan_makkah.mp3`: تضمين ملف صوتي بشري حقيقي نقي 100% لأذان الحرم المكي الشريف يعمل بدون إنترنت (Offline).
  - `prayer/AdhanSoundPlayer.kt`: الترقية إلى مشغل وسائط حقيقي (`MediaPlayer`) مع رفع تدريجي للصوت خلال 2.5 ثانية لمنع المفاجأة، والتخزين المحلي التلقائي في `filesDir/adhan_cache/`.
  - `prayer/PrayerModels.kt` + `prayer/PrayerRepository.kt`: إضافة قائمة اختيار المؤذن (الحرم المكي، الشيخ عبد الباسط عبد الصمد، الشيخ مشاري العفاسي، وإذاعة القرآن الكريم من القاهرة).
  - `prayer/PrayerUi.kt`: شريط اختيار المؤذن وزر تجربة وسماع الأذان فورياً داخل إعدادات الصلاة.
- **المستوى الثالث — مشغل القنوات الفضائية والريسيفر الداخلي الاحترافي (Live TV & Internal Tuner)**:
  - `livetv/InternalTunerManager.kt`: قراءة قنوات التيونر الداخلي للشاشة عبر `TvContract.Channels.CONTENT_URI` مع حماية استثنائية `try-catch (SecurityException)` وسقوط سلس للبث المباشر المفتوح عبر `TvContract.buildChannelUriForPassthroughInput`.
  - `livetv/LiveTvUi.kt`: مشغل نافذة التيونر `TvView` المدمج في واجهة مشكاة، ودعم تقليب القنوات بالأرقام المباشرة (0..9) عبر الريموت، وعرض الترددات والترميز والاستقطاب.
  - `shared/channels.json` + `livetv/LiveTvModels.kt`: دعم 4 أقمار صناعية كبرى (نايل سات 7° غرب، عرب سات / بدر 26° شرق، سهيل سات 25.5° شرق، والياه سات 52.5° شرق) بإجمالي 42 قناة تلفزيونية، ومزامنة القنوات السحابية بنقرة واحدة.
- **المستوى الرابع — موديول ريموت الموبايل المستقل وتطوير الخادم (Mishkat Remote Mobile Module)**:
  - `remote/`: موديول تطبيقي مستقل متكامل لهواتف أندرويد بحجم خفيف جداً (~1 MB) وواجهة زجاجية سائلة Liquid Glass متناسقة مع تلفزيون مشكاة.
  - `remote/discovery/TvDiscoveryClient.kt` + `remote/CompanionRemoteServer.kt`: اكتشاف تلقائي فوري لشاشات مشكاة عبر شبكة الـ Wi-Fi باستخدام بث منارات UDP على المنفذ 8788 دون أي ضبط يدوي، مع إمكانية التحديد اليدوي لـ IP.
  - `remote/ui/RemoteScreen.kt`: تحكم شامل يشمل D-Pad لمسي مع اهتزاز تفاعلي (Haptic Feedback)، وماوس هوائي تفاعلي (Touchpad/Air-Mouse)، وإدخال نصوص وإملاء صوتي بالمايكروفون، واختصارات سريعة للتطبيقات، وزر "🚀 تسريع TV" لتحرير رام التلفزيون بضغطة زر من الموبايل.
  - حزم التوزيع الرسمية المجمعة:
    - `dist/MishkatLauncher-AndroidTV-v1.7.0.apk` (8.3 MB)
    - `dist/MishkatRemote-Mobile-v1.7.0.apk` (1.0 MB)

## [1.4.0-s9] — 2026-11-08 (Sprint S8 + S9 + تحسينات الواقعية والتنقل: خلفيات فوتوغرافية حقيقية + عزل التركيز في القوائم + شاشة قفل ليلية خافتة جدًا + تحريك وترتيب التطبيقات + بوابة الريسيفر والمداخل + ريموت الموبايل QR)
### Added
- `assets/wallpapers/*.jpg` + `theme/ThemeWallpaper.kt`: استبدال الخلفيات الرقمية المرسومة بـ **12 خلفية فوتوغرافية حقيقية عالية الجودة** من الواقع والطبيعة والمعالم الإسلامية (الـ 4 صور المرفوعة للثلوج والمطر والبرق والربيع + صور حقيقية للكعبة المشرفة، المسجد النبوي، العمارة الإسلامية، صحراء الغسق، شاطئ الصيف، غابة الخريف، ملعب كرة قدم ليلي، وقاعة سينما) مع كاش فوري في الذاكرة (`LruCache`).
- `home/HomeScreen.kt` + جميع النوافذ المنبثقة (`*Overlay.kt`): إصلاح جذري لمشكلة تحرك كروت الخلفية أثناء فتح القوائم عبر عزل التركيز (`Modifier.focusProperties { canFocus = !anyOverlayOpen }`) والانتظار لدورة الرسم (`withFrameNanos`) قبل نقل التركيز للنافذة المفتوحة.
- `home/LockScreenOverlay.kt`: خفض إضاءة شاشة القفل إلى أقصى درجة خفوت ليلية (`screenBrightness = 0.03f..0.07f` + طبقة تعتيم عميقة 88%–94%) مع زر للتبديل بين 3 درجات خفوت (`خافتة جدًا 10%`، `ليلي معتم 5%`، `هادئة 20%`).
- `home/AppsRepository.kt` + `HomeScreen.kt` + `AppManagerOverlay.kt`: إضافة **تحريك وترتيب التطبيقات (`► تحريك لليمين` / `◄ تحريك لليسار`)** وتثبيت المفضلة (`★`) والإخفاء (`👁`) مباشرة من الشاشة الرئيسية (زر `⇄ تحريك ومفضلة وإخفاء` أو الضغط المطول على أي تطبيق) وداخل مدير التطبيقات مع حفظ الترتيب المخصص في `DataStore`.
- `tv/TvInputsRepository.kt` + `tv/TvInputsUi.kt` + `input/RemoteAdapters.kt`: بوابة الريسيفر الفضائي (`DVB-S2`) والأرضي (`DTV`) ومداخل `HDMI 1..3` و `AV` وكاشف فلاشة `USB` التلقائي، مع دعم أزرار الريموت المخصصة (`TV`, `INPUT`, `CH+/CH-`, والأزرار الملونة الأربعة `🔴 🟢 🟡 🔵`) وتوافق أندرويد 7.0 (`network_security_config.xml`).
- `prayer/AdhanSoundPlayer.kt` + `PrayerEngine.kt`: تنبيه صوتي لتكبيرات الأذان بمقام الحجاز (`AudioTrack` Offline) وعدّاد المناسبات الإسلامية الهجرية القادمة (`رمضان`، `يوم عرفة`، `العيدين`، `الأيام البيض`).
- `remote/CompanionRemoteServer.kt` + `CompanionRemoteUi.kt`: خادم ريموت الموبايل المحلي عبر الواي فاي (`http://<tv-ip>:8787`) مع مولد كود QR قياسي (`ISO/IEC 18004 Version 3-L`) يتيح لأي موبايل التحكم الكامل في اللانشر والإذاعات والثيمات والخلفيات والريسيفر وقفل الشاشة.

## [1.2.0-s7] — 2026-11-08 (Sprint S7: ثيمات العالم المتعدد + الخلفيات المتحركة للمواسم الأربعة والطقس + مركز المباريات والفرق)
### Added
- `assets/themes/{sports_arena,cinema_action,winter_snow,spring_blossom,summer_sunset}.json`: 5 ثيمات جديدة كليًا (ملاعب وأبطال ⚽، سينما وأكشن 🎬، ثلوج وأمطار الشتاء ❄️، نسيم وأزهار الربيع 🌸، صيف وغروب ذهبي ☀️) ليصبح الإجمالي 10 ثيمات مدمجة.
- `theme/ThemeWallpaper.kt` + `ThemePackManagerOverlay.kt`: محرك الخلفيات الحية للمواسم الأربعة (`winter_snow` ثلوج متساقطة، `winter_rain` أمطار وبرق، `spring_petals` بتلات أزهار، `summer_waves` أمواج وشمس صيفية، `autumn_leaves` أوراق خريف، `weather_sync` مزامنة ذكية مع حالة الطقس والموسم الحالي، `sports_stadium` تخطيط ملعب وأضواء كاشفة، `cinema_imax` شرائط سينمائية) مع التوقف التلقائي عند فتح أي نافذة فرعية (R23).
- `sports/SportsModels.kt` + `SportsRepository.kt` + `SportsUi.kt` + `assets/sports/matches_snapshot.json`: مركز المباريات والفرق المحلية والدولية (دوري أبطال أفريقيا، روشن السعودي، أبطال أوروبا، الدوري الإنجليزي، الإسباني، الإيطالي، الألماني) مع تحديث مباشر عبر ESPN HTTPS، اختيار الفريق المفضل، شارة الفائز (`🏆`)، وشريحة مخصصة في البانر الرئيسي.

## [1.1.0-s6] — 2026-11-08 (Sprint S6: تصميم الزجاج السائل Liquid Glass + تصغير وتوازن الكروت + التطبيقات في الأعلى + إذاعة القاهرة + معرض الخلفيات + شاشة القفل + إعدادات اللانشر)
### Added
- `theme/ThemeWallpaper.kt` + `ThemeTokens.kt` + `ThemePackManagerOverlay.kt`: محرك **الزجاج السائل (`Modifier.liquidGlass`)** متعدد الطبقات مع انعكاس ضوئي قطري وإطار زجاجي مضيء، و5 ثيمات زجاجية (`رمال مذهّب`، `نور النبوة`، `ليلة زجاجية`، `زمرد سائل`، `كسوة وذهب`)، و6 لوحات وخلفيات فنية إسلامية (`شمسة أرابيسك مذهّبة`، `محراب المسجد والمشكاة`، `ليالي البادية والهلال`، `نقوش الكسوة والذهب`، `أمواج الزجاج السائل`، `النجمة الإسلامية الثمانية`).
- `home/LockScreenOverlay.kt`: شاشة القفل والتوقف الإسلامية الزجاجية (ساعة كبيرة، التاريخ الهجري، العد التنازلي للصلاة القادمة، آيات قرآنية متجددة، وتحكم مباشر بإذاعة القرآن الكريم أثناء القفل) مع تفعيل يدوي بزر `🔒` أو تلقائي بعد عدم النشاط.
- `assets/stations_snapshot.json` + `radio/StationRepository.kt`: إضافة **إذاعة القرآن الكريم - القاهرة (مباشر)** (`https://stream.radiojar.com/8s5u5tpdtwzuv`) في صدارة المحطات مع إذاعة السعودية وكبار القراء (عبدالباسط مجود/مرتل/ورش، المنشاوي مجود/مرتل، الحصري مجود/ورش، مصطفى إسماعيل، الطبلاوي، البنا، العفاسي، المعيقلي).
- `home/HomeScreen.kt` + `AppManagerOverlay.kt` + `LocaleRepository.kt`: تصغير وتنسيق أحجام البارات والكروت (`Compact Layout`)، رفع صف التطبيقات للأعلى مباشرة لسهولة الوصول، وتوسيع لوحة إعدادات اللانشر للتحكم الكامل في ترتيب الصفوف، حجم الكروت، قوة الزجاج السائل، الخلفية، ومؤقت شاشة القفل.

## [1.0.0-s5] — 2026-11-08 (Sprint S5: الأداء وأدوات الجهاز الصادقة + التنظيف التلقائي + كاش الأيقونات)
### Added
- `tools/SystemStatsReader.kt`: قراءة حقيقية من النواة ونظام الملفات (`/proc/meminfo`، `StatFs`، `/sys/class/thermal/thermal_zone*/temp`، و `Debug.getPss()`) بدون أي أرقام وهمية أو عشوائية (R24).
- `tools/HonestCleaner.kt` + `AutoCleanWorker`: منظف كاش صادق يقيس الحجم الفعلي بالبايت قبل وبعد الحذف، مع جدولة تنظيف تلقائي كل 6 ساعات عبر `WorkManager` عند تجاوز الكاش 500 MB (§9).
- `tools/DeviceToolsOverlay.kt`: لوحة أدوات الجهاز وفحص الأداء الصادق (RAM، التخزين، حرارة المعالج، استهلاك اللانشر مقارنة بميزانية 120 MB، وتنظيف الكاش الفوري).
- `home/AppsRepository.kt`: تخزين أيقونات وبانرات التطبيقات كملفات PNG على القرص (`filesDir/icon_cache/`) مرتبطة بـ `lastUpdateTime` لتسريع الإقلاع البارد وتقليل استهلاك الذاكرة (§7).
- `SystemStatsAndCleanerTest.kt` + `docs/S5_MIBOX_CHECKLIST.md`.

## [0.5.0-s4] — 2026-11-08 (Sprint S4: إدارة التطبيقات وإزالة البرامج المثقلة + تبديل اللغة الفوري + BootReceiver)
### Added
- `i18n/LocaleRepository.kt`: تبديل فوري للغة (`العربية RTL` / `English LTR`) ونظام الوقت (`12h` / `24h`) من داخل التطبيق بدون إعادة تشغيل الجهاز، مع تعريب الأرقام تلقائيًا (R21).
- `home/AppManagerOverlay.kt` + تحديث `AppsRepository.kt`: مدير التطبيقات الشامل لتثبيت التطبيقات المفضلة في المقدمة (`★`)، إخفاء التطبيقات الإعلانية أو غير المستخدمة، واختصار مباشر لإيقاف البرامج المثقلة إجباريًا (`Force Stop`) أو تعطيلها أو إلغاء تثبيتها، بالإضافة إلى لوحة الإعدادات (`SettingsOverlay`).
- `system/BootReceiver.kt`: مستقبل إقلاع النظام لفتح اللانشر فور تشغيل الشاشة وتجاوز واجهة الإعلانات الأصلية.
- `I18nAndAppFilterTest.kt`.

## [0.4.0-s3] — 2026-11-08 (Sprint S3: البانر الإسلامي Billboard + الطقس 7 أيام + محرك الثيمات وتصنيف القراء)
### Added
- `home/BillboardCarousel.kt`: بانر كاروسيل تفاعلي يتنقل بين البث القرآني المباشر، الصلاة القادمة والذكر، والطقس والتاريخ الهجري، ويتوقف تلقائيًا عند عدم الرؤية (R23).
- `weather/`: `WeatherModels.kt` + `WeatherRepository.kt` (جلب توقعات 7 أيام من Open-Meteo مع كاش 30 دقيقة في `DataStore` وسقوط Offline) + `WeatherUi.kt` (`WeatherPanelOverlay`).
- `theme/IconMask.kt` + `ThemePackManagerOverlay.kt`: محرك أقنعة الأيقونات (`squircle_22`) ولوحة إدارة الثيمات وحزم الأيقونات.
- `radio/RadioUi.kt`: تصنيف الـ 177 محطة إلى (الكل / المفضلة / القراء / تفسير وأذكار) مع شريط الحروف الأبجدية للقفز السريع لأي قارئ بالريموت.
- `WeatherAndCategoryTest.kt`.

## [0.3.0-s2] — 2026-11-08 (Sprint S2: مواقيت الصلاة Offline + الأذكار + AdhanOverlay)
### Added
- `assets/prayer/`: `country_methods.json` و `cities_pack.json` (39 مدينة عربية وعالمية Offline مع المناطق الزمنية) و `adhkar.json` (مختارات حصن المسلم للنوافذ الأربع).
- `prayer/PrayerEngine.kt`: حساب المواقيت الخمسة + الشروق Offline عبر `adhan-java`، حساب الصلاة القادمة والعد التنازلي، تحديد نافذة الأذكار المرتبطة بالمواقيت ومؤقت النوم، وحساب التاريخ الهجري عبر `android.icu.util.IslamicCalendar`.
- `prayer/PrayerRepository.kt`: إدارة المدينة وطريقة الحساب، تحديث العد التنازلي كل ثانية، عدّاد التسبيح التفاعلي بزر OK، وإطلاق تنبيه الأذان عند حلول وقت الصلاة.
- `prayer/PrayerUi.kt`: `PrayerAndAdhkarRow` في الشاشة الرئيسية، `CityPickerOverlay` (مع نسب GeoNames CC-BY 4.0)، `AdhkarPanelOverlay`، و`AdhanOverlay` فوق أي شاشة.
- `PrayerEngineGoldenTest.kt` (اختبارات Golden Values للقاهرة ومكة وجاكرتا + نوافذ الأذكار) + `docs/S2_MIBOX_CHECKLIST.md`.

## [0.2.0-s1] — 2026-11-08 (Sprint S1: الإذاعة الإسلامية والقراء MVP + مؤقت النوم)
### Added
- `assets/stations_snapshot.json`: لقطة Offline مدمجة تضم 177 إذاعة قرآنية وقارئًا باللغتين العربية والإنجليزية من `mp3quran.net/api/v3/radios`.
- `radio/RadioDatabase.kt` + `StationRepository.kt`: قاعدة بيانات Room للمحطات والمفضلة وسجل آخر استماع مع البذر التلقائي Offline والتحديث الاختياري عبر HTTPS.
- `radio/RadioPlaybackService.kt`: خدمة بث خلفي `MediaSessionService` (`foregroundServiceType="mediaPlayback"`) مع إدارة Audio Focus و`ACTION_AUDIO_BECOMING_NOISY` (R12).
- `radio/RadioController.kt`: التحكم بالمشغل، دعم أزرار الميديا والأرقام السريعة (1..9) من الريموت، إعادة المحاولة التلقائية مع Backoff، والتحويل التلقائي لمحطة بديلة عند تعطل البث.
- `system/SleepTimerRepository.kt`: مؤقت نوم موحد محفوظ في `DataStore` مع خفض تدريجي للصوت لمدة 5 ثوانٍ (`Fade-out`) قبل الإيقاف (R13).
- `radio/RadioUi.kt`: شريط `NowPlayingBar` الدائم، رف `RecitersQuickShelf` في الشاشة الرئيسية، ولوحة `RadioPanelOverlay` الكاملة مع فلتر المفضلة واستعادة التركيز (R15).
- `RadioAndSleepTimerTest.kt` + `docs/S1_MIBOX_CHECKLIST.md`.

## [0.1.0-s0] — 2026-11-08 (Sprint S0: النواة)
### Added
- إعداد مشروع Gradle أحادي الوحدة (`:app`) مع Version Catalog مثبت الإصدارات (`gradle/libs.versions.toml`).
- `core/Logger.kt`: مسجل موحد بتاج `[Mishkat *]` معطل تلقائيًا في بناء `release` وبدون تخصيص نصوص عند التعطيل.
- `input/`: طبقة توحيد الريموت `RemoteUnificationLayer` مع `XiaomiRemoteAdapter` و `GenericChineseAdapter` و `AirMouseAdapter` وتقرير الأزرار غير المعروفة (≥5 ضغطات).
- `focus/`: `Modifier.mishkatFocus()` المعتمد على `Modifier.Node` (رسم في مرحلة Draw فقط بدون إعادة تركيب) + `FocusRestorer` لحفظ واستعادة آخر عنصر مركز عليه عبر `DataStore`.
- `theme/`: `ThemeSpec` (kotlinx.serialization) + `ThemeTokens` + `ThemeRepository` + `Motion` + `ThemeWallpaper` (تدرج لوني + زخرفة نجمة إسلامية ثمانية مخبأة عبر `drawWithCache`)، مع 3 ثيمات مدمجة (`rimal`, `noor`, `layla`).
- `home/`: هيكل الشاشة الرئيسية (`HomeScreen`) يعمل Offline بالكامل، يعرض الشريط العلوي (الوقت، حالة الشبكة، مبدل الثيمات)، رف الأقسام، ورف التطبيقات الحقيقية المثبتة من `PackageManager`/`LauncherApps` مع `ErrorCard` عند الفشل.
- `system/CrashGuard.kt`: كاشف حلقة الانهيار (3 مرات خلال 60 ثانية) مع شاشة `SafeModeScreen` للعودة للواجهة الأصلية للجهاز.
- اختبارات الوحدة (`RemoteAdaptersTest`, `ThemeAndCrashPolicyTest`).
- ملفات الحوكمة والتوثيق: `NOTICE.md`, `DECISIONS.md`, `CHANGELOG.md`, `docs/S0_MIBOX_CHECKLIST.md`.
