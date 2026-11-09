\# MISHKAT LAUNCHER — PROJECT BRIEF v1.0 (عقد العمل)

> Codename: MishkatLauncher (الاسم التجاري OPEN — يُحسم في S3).

> الأصل: مشروع "رمال TV". EN ABSTRACT: A lightweight, Chinese-launcher-smooth Android TV/Google TV launcher: offline global prayer times + adhkar, built-in Islamic radio (Media3), theme/icon engine over 3 existing themes, RTL-first ar/en, clean-room implementation.



\## 0) تعليمات للوكيل (اقرأها أولاً)

1. هذا الملف عقد عمل: لا تخالف "القواعد" إلا بقرار مسجل في DECISIONS.md بموافقة المستخدم.

2. اعمل Sprint بالسبرينت. بداية كل Sprint: خطة ملفات. نهايته: كود + نتيجة بناء + Checklist اختبار على Mi Box S 2 + إدخال CHANGELOG + DECISIONS.

3\. Clean-room: المستودعات المرجعية للقراءة وفهم الأنماط فقط. ممنوع نسخ كود/موارد منها (R1).

4\. أي مكتبة جديدة: تُضاف في libs.versions.toml + سطر ترخيص في NOTICE.md قبل الاستخدام (R2).

5\. إذا تعثرت مرتين في نفس المشكلة: توقف واسأل المستخدم بخيارات مرقمة، لا ترتجل.



\## 1) القرارات المثبتة (LOCKED)

| البند | القرار |

|---|---|

| Stack | Kotlin + Compose for TV (androidx.tv.material3) + Media3 + DataStore + Room مصغر + Coil3 + kotlinx.serialization. minSdk 24 / target 35 |

| الأجهزة | Mi Box S 2 = الجهاز المرجعي (Baseline). Stretch: بوكس صيني 1GB/Android 9. لاحقًا Google TV وشاشات مدمجة |

| الإدخال | RemoteUnificationLayer: IR + BT + AirMouse عبر Adapters (الكود مرفق §7) |

| اللغات | ar (RTL افتراضي) + en. الإطار جاهز لإضافة لغات لاحقًا |

| المواقيت | Offline بالكامل: adhan-java + جدول دولة→طريقة + حزمة مدن Offline (geonames) + android.icu للهجري |

| الراديو | MVP: تشغيل + مفضلة + دمج Sleep Timer. التسجيل مؤجل لـ S2.5 |

| الثيمات | محرك جديد فوق الثيمات الثلاثة الموجودة (رمال/مصر/ليلة) كـ Built-in Presets + Theme Packs قابلة للتنزيل لاحقًا |

| الجمالية | مدرسة الصينيين: أرفف قابلة للترتيب، Billboard كاروسيل، مدير حزم موارد، ميزانيات أداء §6 |

| DI | يدوي (object graph) — لا Hilt حفاظًا على الخفة |



\## 2) المكتبات: تبعيات مباشرة (Gradle + المصدر)

| المكتبة | الإحداثيات | GitHub |用途 |

|---|---|---|---|

| Compose for TV | androidx.tv:tv-foundation / androidx.tv:tv-material3 (أحدث stable) | maven.google (أنماط من android/compose-samples) | UI |

| Media3 | androidx.media3:media3-exoplayer / media3-session (أحدث stable) | github.com/androidx/media | مشغل الراديو/Bث |

| Adhan | com.batoulapps.Adhan:Adhan:1.2.1 (تحقق من README) | github.com/batoulapps/adhan-java | حساب المواقيت Offline |

| Coil3 | io.coil-kt.coil3:coil-compose | github.com/coil-kt/coil | صور/لوجوهات بكاش |

| Serialization | org.jetbrains.kotlinx:kotlinx-serialization-json | github.com/Kotlin/kotlinx.serialization | JSON ثيمات/محطات |

| DataStore | androidx.datastore:datastore-preferences | androidx | تفضيلات |

| Room | androidx.room:room-runtime + ksp compiler | androidx | محطات/كاش |

| WorkManager | androidx.work:work-runtime-ktx | androidx | جدولة auto-clean |

> ثبّت الإصدارات في libs.versions.toml وقت الإعداد ولا تترك "+" ديناميكيًا.



\## 3) مصادر بيانات (ليست تبعيات كود)

\- محطات إسلامية: https://mp3quran.net/api/v3/radio?language=ar → يُبنى منه assets/stations\_snapshot.json وقت البناء (يعمل Offline أول تشغيل)، وتحديث Online اختياري.

\- احتياطي راديو عالمي: radio-browser API (github.com/radio-browser/radio-browser-api).

\- بث مباشر (لاحقًا): iptv-org (github.com/iptv-org/iptv + /api) — فلترة Religious/أخبار عربي. تحقق من الترخيص ونسب المصدر.

\- مدن Offline: geonames cities1000 (ترخيص CC-BY 4.0 → Attribution إلزامي في About).

\- طقس: Open-Meteo (بدون مفتاح) مع كاش 30د وسقوط Offline.

\- مرجع فقهِي/نمطي للقراءة فقط: github.com/metinkaleoglu/prayer-times-android، github.com/quran/quran\_android، github.com/y20k/transistor، RadioDroid2 (حدده من صفحة F-Droid)، github.com/d4rken-org/sdmaid-se (واقعية التنظيف)، github.com/breezy-weather/breezy-weather (تجريد مزودي الطقس)، github.com/Neamar/KISS و github.com/lawnchairlauncher/lawnchair (Providers/Theming)، github.com/android/tv-samples.



\## 4) المعمارية وحزمة المشروع

Single-module :app package-by-feature (التقسيم لوحدات لاحقًا عند الحاجة):

com.mishkat.launcher → input/ focus/ theme/ home/ radio/ prayer/ weather/ tools/ system/ core/ i18n/

\- input: RemoteUnificationLayer + Adapters (§7).

\- focus: Modifier.mishkatFocus() + FocusRestorer (استعادة آخر عنصر مُركَّز من DataStore بعد crash).

\- theme: ThemeTokens (مصدر وحيد للألوان/الأبعاد/الحركة) + ThemeRepository + ThemeJson schema (§8).

\- home: Shelves قابلة لإعادة الترتيب/الإخفاء + Billboard كاروسيل + Dock + Sidebar (وقت/نت/طقس/حرارة CPU).

\- radio: StationRepository(snapshot+room) + RadioPlaybackService(MediaSessionService, foreground type mediaPlayback) + NowPlayingBar + RadioPanel.

\- prayer: PrayerEngine(adhan) + CountryMethods.json + CitiesPack + AdhkarRepository + AdhanOverlay (فوق أي شاشة).

\- tools: قراءات /proc/meminfo وStatFs وحرارة SoC + تنظيف \*\*صادق\*\* (§9 R24).

\- system: BootReceiver + SleepTimerRepository (مصدر وحيد للمؤقت، يخدم الراديو والشاشة).



\## 5) القواعد (RULES — يُرفض أي MR يخالفها)

R1 clean-room. R2 ترخيص قبل مكتبة. R3 الهوم يرسم Offline بالكامل؛ الشبكة للتحديث فقط مع fallback كاش. R4 لا WebView في النواة. R5 لا حظر على الـ main thread؛ لا runBlocking في مسار الأحداث (استخدم tryEmit). R6 Compose: بيانات stable، مفاتيح LazyRow ثابتة، صفر تخصيص داخل lambdas السكرول. R7 RTL: start/end فقط، واختبار الاتجاهين. R8 كل النصوص في strings.xml (ar/en). R9 Logger موحّد بتاج \[Mishkat \*] معطّل في release؛ لا println. R10 الهوم لا ينهار أبدًا: أي كارت فاشل → ErrorCard؛ crash-loop (3/60ث) → Safe Mode يعرض العودة لللانشر النظامي. R11 صلاحيات دنيا: INTERNET, ACCESS\_NETWORK\_STATE, RECEIVE\_BOOT\_COMPLETED, FOREGROUND\_SERVICE\_MEDIA\_PLAYBACK, POST\_NOTIFICATIONS. R12 Audio focus + ACTION\_AUDIO\_BECOMING\_NOISY + إيقاف عند فقدان التركيز. R13 Sleep Timer مصدر وحيد + fade-out 5ث ثم stop + بقاء بعد موت العملية. R14 لا hex hardcoded داخل Composables. R15 كل تفاعلي بـ mishkatFocus()؛ لا فخاخ focus؛ العودة من أي لوحة تُرجع الـ focus لنقطة الأصل. R16 ميزانيات §6 تُقاس على Mi Box S 2 قبل إغلاق أي Sprint. R17 CHANGELOG + DECISIONS append-only. R18 Conventional Commits مع تاج Sprint. R19 اختبارات وحدة: golden values للمواقيت (القاهرة/مكة/جاكرتا تواريخ معلومة)، منطق المؤقت، جدول mapping لكل Adapter. R20 contentDescription لكل زر أيقوني. R21 تبديل لغة داخل التطبيق بدون إعادة تشغيل جهاز؛ أرقام حسب اللغة؛ 12/24h قابل للإعداد. R22 https للميتاداتا؛ cleartext مسموح فقط لستريمات ميديا عبر network-security-config استثناءً موثقًا. R23 أوقف الكاروسيل/بارالاكس عند عدم الرؤية؛ فيديو خلفية فقط إذا RAM≥2GB. R24 لا ادعاءات كاذبة في أدوات النظام: كل زر يعرض ما فعله فعليًا بالبايت المقاس؛ تنظيف = كاش التطبيق نفسه + تقليم Room/Coil/logs فقط. R25 اللانشر مسجل كـ HOME (CATEGORY\_HOME/DEFAULT) مع Safe Mode أعلاه.



\## 6) ميزانيات الأداء (DoD دائم)

Cold start ≤1.2s • سكرول الأرفف 60fps • RAM العملية ≤120MB • APK أولي ≤25MB • صفر شبكة عند أول رسم • صفر تخصيص أثناء السكرول.



\## 7) الكود المثبت v1.1 — input/RemoteUnificationLayer.kt

(نسخة مصححة: tryEmit بدل runBlocking احترامًا لـ R5)

```kotlin

sealed class MishkatAction {

&#x20;   object NavigateUp, NavigateDown, NavigateLeft, NavigateRight,

&#x20;          Confirm, Back, Home, Menu, PlayPause, Next, Previous,

&#x20;          VolumeUp, VolumeDown, Mute, Search, Settings : MishkatAction()

&#x20;   data class ChannelNumber(val digit: Int) : MishkatAction()

&#x20;   data class Unknown(val keyCode: Int) : MishkatAction()

}

interface RemoteAdapter { val name: String; fun translate(kc: Int, e: KeyEvent): MishkatAction? }

object XiaomiRemoteAdapter : RemoteAdapter { /\* DPAD/ENTER/BACK/MENU/MEDIA\_\*/ / KEYCODE\_TV\_DATA\_SERVICE→Settings \*/ }

object GenericChineseAdapter : RemoteAdapter { /\* + NUMPAD\_ENTER, MEDIA\_PLAY/PAUSE, KEYCODE\_0..9→ChannelNumber, KEYCODE\_SETTINGS→Menu \*/ }

object AirMouseAdapter : RemoteAdapter { /\* BUTTON\_1→Confirm, BUTTON\_2→Back \*/ }

object RemoteUnificationLayer {

&#x20;   private val adapters = listOf(XiaomiRemoteAdapter, GenericChineseAdapter, AirMouseAdapter)

&#x20;   private val \_actions = MutableSharedFlow<MishkatAction>(extraBufferCapacity = 32)

&#x20;   val actions = \_actions.asSharedFlow()

&#x20;   private val volumeKeys = setOf(KeyEvent.KEYCODE\_VOLUME\_UP, KeyEvent.KEYCODE\_VOLUME\_DOWN)

&#x20;   fun onKeyEvent(event: KeyEvent): Boolean {

&#x20;       if (event.action != KeyEvent.ACTION\_DOWN) return false

&#x20;       if (event.repeatCount > 0 \&\& event.keyCode !in volumeKeys) return true

&#x20;       for (a in adapters) a.translate(event.keyCode, event)?.let {

&#x20;           Logger.input("${a.name}: ${event.keyCode} -> ${it::class.simpleName}")

&#x20;           \_actions.tryEmit(it); return true

&#x20;       }

&#x20;       \_actions.tryEmit(MishkatAction.Unknown(event.keyCode)); return false

&#x20;   }

}

// MainActivity: override dispatchKeyEvent(e) = RemoteUnificationLayer.onKeyEvent(e) || super.dispatchKeyEvent(e)

```

قاعدة توسعة: ريموت جديد = Adapter جديد فقط، صفر تعديل UI. سجل Unknown≥5 تكرارات → تقرير "زرار ناقص".



\## 8) Theme JSON Schema v1

```json

{ "id":"rimal","name":"رمال","builtin":true,

&#x20; "palette":{"primary":"#C9A227","surface":"#141A2E","onSurface":"#F5F1E6",

&#x20;            "panelAccents":{"sleep":"#6B4E8E","prayer":"#1E2A4A","tools":"#8E4E6B","weather":"#8E5A4E"}},

&#x20; "wallpaper":{"type":"gradient|image|video","asset":"wp\_rimal","parallax":0.15},

&#x20; "cards":{"radius":20,"glass":0.35,"strokeAlpha":0.18},

&#x20; "icons":{"mask":"squircle\_22","pack":"mishkat\_cute\_v1"},

&#x20; "motion":{"focusScale":1.06,"enterMs":240,"springDamping":0.85} }

```

الثيمات builtin: rimal / misr / layla. أي لون في UI يمر من ThemeTokens فقط (R14).



\## 9) مواصفات موجزة

\- الراديو MVP: قائمة من snapshot + لوجوهات Coil بكاش + مفضلة Room + NowPlayingBar دائم + دمج SleepTimer (R13) + retry/backoff + fallback محطة محلية عند فشل الستريم.

\- المواقيت: adhan-java؛ CountryMethods.json (\~250 صف: EG→Egyptian, SA→UmmAlQura, PK→Karachi, US/CA→ISNA, EU→MWL, TR→Turkey, MA/DZ/TN طرقها...)؛ المدن: حزمة Offline (اسم ar/en, lat, lon, tz)؛ الافتراضي من locale الدولة + اختيار يدوي؛ أذان Overlay + تنبيه اختياري.

\- الأذكار مرتبطة بالمواقيت لا بساعة الجهاز: الصباح \[الفجر→الشروق+15د]، المساء \[العصر→24:00]، تسبيح بعد كل صلاة (+2د) بعدّاد OK، أذكار نوم عند بدء Sleep Timer. النصوص: مختارات حصن المسلم JSON (ar، وحقل en لاحقًا).

\- الطقس: Open-Meteo + كاش 30د + لوحة 7 أيام الحالية بتصميمك.

\- أدوات النظام: عرض صادق + تنظيف ذاتي فقط (R24) + auto-clean كل 6س إذا كاش>500MB عبر WorkManager.

\- كارت المباراة v1: جدول محلي يحرره المستخدم؛ لا scraping.



\## 10) السبرينتات وDoD

S0 النواة (7 أيام): إعداد catalog+build، Input v1.1، Focus System (mishkatFocus + استعادة)، ThemeTokens من الثيمات الثلاثة، Motion spec، Baseline فيديو على Mi Box S 2. DoD: تنقل كامل بريمو BT بدون فقدان focus + ميزانيات §6 مقاسة.

S1 راديو MVP. DoD: تشغيل خلفي مستقر 30د على Android 9/1GB + بقاء بعد مغادرة الهوم + دمج المؤقت.

S2 مواقيت+أذكار Offline + AdhanOverlay. DoD: golden tests خضراء + أذان فوق أي شاشة.

S2.5 تسجيل الراديو (MediaStore، بدون صلاحية تخزين على 30+).

S3 محرك الثيمات/الأيقونات + حسم الاسم التجاري + pack manager skeleton.

S4 i18n en كامل + Drawer/بحث/إخفاء + Boot/SafeMode.

S5 باس الأداء الصيني: profiling، كاش أيقونات DB، مراجعة §6، auto-clean صادق.



\## 11) OPEN QUESTIONS

الاسم التجاري (معايير: يوحي بالمنطقة، 4-6 حروف لاتينية سهلة، بلا تصادم) • مرجع بصري صيني محدد (Dangbei/When/Shafa؟) • ريموتات إضافية لمصفوفة S0 • EPG مصدره (مؤجل).



\## 12) NOTICE/تراخيص (يُملأ مع كل إضافة)

adhan-java (MIT) • geonames (CC-BY 4.0 attribution) • iptv-org (تحقق) • نصوص تراثية (ملك عام) • البايع androidx/Apache-2.0.

