# قائمة فحص وقياس Sprint S0 على جهاز Mi Box S (2nd Gen)

هذه القائمة مطلوبة لإغلاق **Sprint S0** ميدانيًا على الجهاز المرجعي وفق **§6** و **§10** و **R16**.

## 1) التثبيت والتعيين كواجهة (HOME)
```powershell
D:\Android\Sdk\platform-tools\adb.exe connect <MI_BOX_IP>:5555
D:\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```
- [ ] الضغط على زر **Home** في الريموت يظهر نافذة اختيار المشغل ويحتوي على **مشكاة / Mishkat**.
- [ ] الضغط على زر **Back** داخل الشاشة الرئيسية لا يغلق اللانشر.

## 2) مصفوفة أزرار ريموت البلوتوث (Mi Box S 2)
راقب اللوج أثناء الضغط:
```powershell
D:\Android\Sdk\platform-tools\adb.exe logcat -s "[Mishkat Input]:D" "[Mishkat Focus]:D"
```
- [ ] الأسهم الأربعة (فوق/تحت/يمين/يسار) تنقل التركيز بسلاسة بين مبدل الثيمات، رف الأقسام، ورف التطبيقات بدون أي فخ تركيز (Focus Trap).
- [ ] الضغط المطول على السهم اليمين/اليسار داخل رف التطبيقات يمرر الكروت بسرعة.
- [ ] زر **OK / Enter** يفعّل الثيم المختار أو يفتح التطبيق المركز عليه.
- [ ] أزرار الصوت ترفع/تخفض صوت النظام فعليًا مع ظهور الحدث في اللوج.

## 3) اختبار استعادة التركيز (`FocusRestorer`)
- [ ] ركّز على كارت تطبيق معين وانتظر ثانية واحدة.
- [ ] اقتل العملية:
  ```powershell
  D:\Android\Sdk\platform-tools\adb.exe shell am force-stop com.mishkat.launcher
  D:\Android\Sdk\platform-tools\adb.exe shell input keyevent KEYCODE_HOME
  ```
- [ ] يعود التركيز تلقائيًا إلى نفس الكارت.

## 4) اختبار RTL / LTR واللغات
- [ ] بالعربية (الافتراضي): المحاذاة تبدأ من اليمين، والأرفف تبدأ من اليمين.
- [ ] بالإنجليزية: المحاذاة تبدأ من اليسار بدون أي نصوص مقطوعة.

## 5) قياس ميزانيات الأداء (§6) على نسخة `release` / `debug`
- [ ] **حجم الـ APK الأولي ≤ 25MB** (مقاس حاليًا بعد البناء — انظر تقرير البناء).
- [ ] **Cold Start ≤ 1.2s (1200ms)**:
  ```powershell
  D:\Android\Sdk\platform-tools\adb.exe shell am force-stop com.mishkat.launcher
  D:\Android\Sdk\platform-tools\adb.exe shell am start -W -n com.mishkat.launcher/.MainActivity
  ```
  تحقق من `TotalTime` ≤ `1200`.
- [ ] **استهلاك الذاكرة RAM ≤ 120MB**:
  ```powershell
  D:\Android\Sdk\platform-tools\adb.exe shell dumpsys meminfo com.mishkat.launcher | Select-String "TOTAL PSS"
  ```
- [ ] **سلاسة السكرول 60fps (Jank)**:
  ```powershell
  D:\Android\Sdk\platform-tools\adb.exe shell dumpsys gfxinfo com.mishkat.launcher reset
  # مرر يمينًا ويسارًا في رف التطبيقات لمدة 10 ثوانٍ ثم:
  D:\Android\Sdk\platform-tools\adb.exe shell dumpsys gfxinfo com.mishkat.launcher
  ```
- [ ] **صفر شبكة عند أول رسم**: شغّل مع إيقاف الـ Wi-Fi وتأكد من رسم الواجهة بالكامل فورًا.
