# مشكاة لانشر | Mishkat TV Launcher 📺✨

> **مِشكاة** — لانشر تلفزيوني ذكي بتصميم زجاجي سائل (Liquid Glass) مستوحى من فلسفة Apple TV، مخصص لأجهزة Android TV و Google TV و TV Boxes.

---

## 🌟 الميزات الرئيسية (Key Features)

- 🧹 **قاتل تطبيقات الخلفية ومُسرّع الرام الذكي (Background Bloatware & Process Killer - v1.7.0)**:
  - فحص العمليات والتطبيقات الدعائية العالقة في الخلفية وإيقافها فورياً (`ActivityManager.killBackgroundProcesses`).
  - أوضاع كنس تلقائي عند إقلاع التلفزيون، وعند العودة للشاشة الرئيسية، وتلقائياً إذا تجاوز استهلاك الذاكرة 75%.
  - إيقاف إجباري عميق (`Deep Force-Stop`) للتطبيقات العنيدة مع شاشة تحكم وإحصائيات بالرام المحرر وعدد العمليات.
  - تطبيق قاعدة **Zero-Regression** الصارمة: الحفاظ الكامل على مؤثرات الزجاج السائل والخلفيات الحية دون مساس.

- 🕌 **محرك صوت الأذان البشري النقي (Real Human Adhan Audio Engine - v1.7.0)**:
  - صوت أذان حقيقي نقي 100% مدمج في ملفات التطبيق ويعمل Offline بدون إنترنت (أذان الحرم المكي الشريف).
  - قائمة اختيار المؤذن (الحرم المكي، الشيخ عبد الباسط عبد الصمد، الشيخ مشاري العفاسي، وإذاعة القرآن الكريم من القاهرة) مع التخزين المحلي التلقائي.
  - رفع تدريجي للصوت لمدة 2.5 ثانية وزر لتجربة وسماع الأذان فورياً داخل إعدادات الصلاة.

- 📺 **دليل القنوات الفضائية والريسيفر الداخلي (Live TV & Internal Tuner - v1.7.0)**:
  - تشغيل قنوات الريسيفر الداخلي للشاشة عبر `TvView` و `TvContract.Channels.CONTENT_URI` مع سقوط آمن ضد أي حظر صلاحيات.
  - دعم 4 أقمار صناعية كبرى (نايل سات 7°W، عرب سات / بدر 26°E، سهيل سات 25.5°E، وياه سات 52.5°E) بإجمالي 42 قناة تلفزيونية.
  - إمكانية القفز الفوري لرقم القناة عبر أرقام الريموت (0..9) ومزامنة سحابية للقنوات وتحديث الترددات.

- 📱 **تطبيق ريموت الموبايل المستقل المتقدم (Mishkat Remote Companion App - v1.7.0)**:
  - تطبيق أندرويد مستقل لهواتف المحمول (`:remote`) بحجم 1 ميجابايت فقط وبتصميم زجاجي سائل (Liquid Glass).
  - اكتشاف فوري وتلقائي لشاشات مشكاة عبر شبكة الـ Wi-Fi (بث منارات UDP المنفذ 8788).
  - لوحة تحكم D-Pad كاملة مع اهتزازات تفاعلية لمسية (Haptic Feedback)، وماوس هوائي تفاعلي (Touchpad)، وإدخال نصوص وإملاء صوتي بالمايكروفون، وزر تسريع التلفزيون عن بُعد (`🚀 تسريع TV`).
  - خادم وب محلي مدمج بديل عبر كود QR يتيح التحكم من أي متصفح هاتف دون تثبيت برامج.

- 📻 **الإذاعة وقراء القرآن الكريم (Media3 Audio Engine)**:
  - إذاعة متواصلة 24/7 مع خدمة تشغيل في الخلفية `MediaSessionService`.
  - قائمة سريعة لأشهر قراء مصر والعالم الإسلامي ومؤقت نوم ذكي قابل للضبط.

- 🎮 **نظام التنقل الذكي وزر الهوم (Accessibility Home Key)**:
  - محرك تنقل فائق السرعة عبر الأسهم (D-Pad) بدون تعليق مع تركيز مرئي واضح.
  - خدمة إمكانية وصول مدمجة (`MishkatAccessibilityService`) تبرمج زر `Home` في الريموت للعودة دائماً لمشكاة وتمنع الخروج للانشر النظام الأصلي.

- ⚡ **أداء خفيف واستهلاك موارد منخفض**:
  - تقليص الحجم عبر ProGuard و R8 بحجم 8.3 ميجابايت فقط للانشر التلفزيون و 1.0 ميجابايت لريموت الموبايل.
  - دعم أنظمة أندرويد من **Android 7.0 (Nougat / API 24)** وحتى **Android 15 (API 35+)**.

---

## 📦 حزم التوزيع المتاحة (Distribution Packages - v1.8.0)

الملفات الجاهزة للتثبيت المباشر في مجلد `dist/`:
- **تلفزيون أندرويد**: [`dist/MishkatLauncher-AndroidTV-v1.8.0.apk`](file:///d:/my%20projict/launcher%20tv/dist/MishkatLauncher-AndroidTV-v1.8.0.apk) (الحجم: ~9.9 MB)
- **ريموت الموبايل**: [`dist/MishkatRemote-Mobile-v1.8.0.apk`](file:///d:/my%20projict/launcher%20tv/dist/MishkatRemote-Mobile-v1.8.0.apk) (الحجم: ~1.0 MB)
- **شاشات سامسونج تايزن**: `dist/MishkatLauncher-Samsung-Tizen.wgt`
- **شاشات إل جي ويب أو إس**: `dist/MishkatLauncher-LG-webOS.zip` & `dist/com.mishkat.launcher_1.8.0_all.ipk`

---

## 🛠️ التقنيات المستخدمة (Tech Stack)

- **Language**: Kotlin 2.1
- **UI Toolkit**: Jetpack Compose for TV (`androidx.tv.material3`) + Liquid Glass Custom Modifiers
- **Media Playback**: AndroidX Media3 (ExoPlayer + HLS Module + Session Service)
- **Background & Audio**: Foreground Service (`mediaPlayback`)
- **System Integration**: Android AccessibilityService for Remote Key Interception (`KEYCODE_HOME`)
- **Local Storage**: Jetpack DataStore Preferences
- **Build System**: Gradle 8.11 with Kotlin DSL & R8 Minification

---

## 👨‍💻 المطور (Developer)

- **Eng. Mohamed Okash**
- **Portfolio**: [mohamed-okash.web.app](https://mohamed-okash.web.app)
- **Email**: mohamed.okash1998@gmail.com
- **Specialization**: IT Infrastructure & HSE Engineering • Advanced Android Systems

---

## 📄 الترخيص (License)

هذا المشروع مطور لأغراض التطوير والاستخدام التلفزيوني الشخصي والمنزلي الذكي.
جميع حقوق البث والعلامات التجارية للقنوات والإذاعات محفوظة لأصحابها والشبكات الأصلية.
