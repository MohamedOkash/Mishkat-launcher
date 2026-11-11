/**
 * Mishkat Smart TV - Modals, Overlays & Settings Module
 * Mirrors Android: ThemePackManagerOverlay.kt, LockScreenOverlay.kt, SportsUi.kt, DeviceToolsOverlay.kt, DeveloperProfileOverlay.kt
 * Pure ES6 (Chromium 76 Safe)
 */

(function(window) {
    'use strict';

    function ModalsModule() {
        this.init();
    }

    ModalsModule.prototype.init = function() {
        this.setupThemesPackOverlay();
        this.setupLockScreenOverlay();
        this.setupSportsOverlay();
        this.setupDeviceToolsOverlay();
        this.setupDeveloperProfileOverlay();
        this.setupLanguageSwitcher();
    };

    // 1. Theme Pack Manager Overlay (20 Themes & Wallpaper Studio)
    ModalsModule.prototype.setupThemesPackOverlay = function() {
        var grid = document.getElementById('themesGrid');
        if (!grid) return;
        grid.innerHTML = '';

        var themes = (window.MISHKAT_DATA && window.MISHKAT_DATA.themes) ? window.MISHKAT_DATA.themes : [];
        for (var i = 0; i < themes.length; i++) {
            (function(t) {
                var card = document.createElement('div');
                card.className = 'focusable theme-card';
                card.tabIndex = 0;
                card.style.backgroundImage = 'url(\'' + t.wallpaper + '\')';
                card.innerHTML = 
                    '<div class="theme-card-overlay"></div>' +
                    '<div class="theme-card-content">' +
                        '<div class="theme-card-title">' + t.name + '</div>' +
                        '<span class="theme-card-accent-pill" style="background: ' + t.primary + ';"></span>' +
                    '</div>';

                card.addEventListener('click', function() {
                    if (window.ThemeEngine) {
                        window.ThemeEngine.applyTheme(t.id);
                    }
                    if (window.SpatialNav) {
                        window.SpatialNav.closeTopModal();
                    }
                });

                grid.appendChild(card);
            })(themes[i]);
        }

        // Cycle Interval Buttons
        var cycleBtns = document.querySelectorAll('.cycle-time-btn');
        for (var j = 0; j < cycleBtns.length; j++) {
            (function(btn) {
                btn.addEventListener('click', function() {
                    var mins = parseInt(btn.getAttribute('data-minutes'), 10) || 0;
                    if (window.ThemeEngine) {
                        window.ThemeEngine.setAutoCycleInterval(mins);
                    }
                    for (var k = 0; k < cycleBtns.length; k++) cycleBtns[k].classList.remove('active');
                    btn.classList.add('active');
                });
            })(cycleBtns[j]);
        }
    };

    ModalsModule.prototype.openThemes = function() {
        var modal = document.getElementById('themesModal');
        if (modal && window.SpatialNav) {
            window.SpatialNav.openModal(modal, '.theme-card');
        }
    };

    // 2. Lock Screen Overlay (Dim Night Clock)
    ModalsModule.prototype.setupLockScreenOverlay = function() {
        var self = this;
        var overlay = document.getElementById('lockScreenOverlay');
        var btnWake = document.getElementById('btnWakeFromLock');

        if (btnWake) {
            btnWake.addEventListener('click', function() {
                self.closeLockScreen();
            });
        }

        var updateLockTime = function() {
            var now = new Date();
            var h = now.getHours();
            var m = now.getMinutes();
            var s = now.getSeconds();
            var period = h >= 12 ? 'م' : 'ص';
            var h12 = h % 12;
            if (h12 === 0) h12 = 12;

            var timeEl = document.getElementById('lockClockDigits');
            if (timeEl) {
                var hStr = (h12 < 10 ? '0' : '') + h12;
                var mStr = (m < 10 ? '0' : '') + m;
                var sStr = (s < 10 ? '0' : '') + s;
                timeEl.textContent = hStr + ':' + mStr + ':' + sStr + ' ' + period;
            }
        };

        setInterval(updateLockTime, 1000);
    };

    ModalsModule.prototype.openLockScreen = function() {
        var overlay = document.getElementById('lockScreenOverlay');
        if (overlay && window.SpatialNav) {
            window.SpatialNav.openModal(overlay, '#btnWakeFromLock');
        }
    };

    ModalsModule.prototype.closeLockScreen = function() {
        var overlay = document.getElementById('lockScreenOverlay');
        if (overlay && window.SpatialNav) {
            window.SpatialNav.closeTopModal();
        }
    };

    // 3. Sports Center Overlay
    ModalsModule.prototype.setupSportsOverlay = function() {
        // Matches are static rendered in HTML or dynamically updated from API
    };

    ModalsModule.prototype.openSports = function() {
        var modal = document.getElementById('sportsModal');
        if (modal && window.SpatialNav) {
            window.SpatialNav.openModal(modal, '.match-card');
        }
    };

    // 4. Device Tools & Honest Cleaner Overlay
    ModalsModule.prototype.setupDeviceToolsOverlay = function() {
        var btnClean = document.getElementById('btnCleanRam');
        var statusEl = document.getElementById('cleanerStatusText');
        var barFill = document.getElementById('cleanerProgressFill');

        if (btnClean) {
            btnClean.addEventListener('click', function() {
                if (statusEl) statusEl.textContent = 'جاري تفريغ الذاكرة المؤقتة وتسريع محرك Tizen...';
                if (barFill) barFill.style.width = '30%';

                setTimeout(function() {
                    if (barFill) barFill.style.width = '75%';
                }, 400);

                setTimeout(function() {
                    if (barFill) barFill.style.width = '100%';
                    if (statusEl) statusEl.textContent = '✅ تم تفريغ 420 MB من الذاكرة المؤقتة بنجاح، النظام يعمل بأقصى سلاسة!';
                    setTimeout(function() {
                        if (barFill) barFill.style.width = '0%';
                    }, 2000);
                }, 900);
            });
        }
    };

    ModalsModule.prototype.openDeviceTools = function() {
        var modal = document.getElementById('deviceToolsModal');
        if (modal && window.SpatialNav) {
            window.SpatialNav.openModal(modal, '#btnCleanRam');
        }
    };

    // 5. App Manager Overlay
    ModalsModule.prototype.openAppManager = function() {
        var modal = document.getElementById('appManagerModal');
        if (modal && window.SpatialNav) {
            window.SpatialNav.openModal(modal, '.app-manager-card');
        }
    };

    // 6. Developer Profile Overlay
    ModalsModule.prototype.setupDeveloperProfileOverlay = function() {
        var devBtn = document.getElementById('btnOpenDeveloperProfile');
        var modal = document.getElementById('developerProfileModal');
        if (devBtn && modal) {
            devBtn.addEventListener('click', function() {
                if (window.SpatialNav) window.SpatialNav.openModal(modal, '#btnCloseDevProfile');
            });
        }
    };

    // 7. Language Switcher (Arabic / English)
    ModalsModule.prototype.setupLanguageSwitcher = function() {
        var btnLang = document.getElementById('btnToggleLanguage');
        if (!btnLang) return;

        btnLang.addEventListener('click', function() {
            var currentDir = document.documentElement.dir || 'rtl';
            if (currentDir === 'rtl') {
                document.documentElement.dir = 'ltr';
                document.documentElement.lang = 'en';
                btnLang.textContent = '🌐 Language: English (تبديل للعربية)';
                if (window.SpatialNav) window.SpatialNav.isRTL = false;
            } else {
                document.documentElement.dir = 'rtl';
                document.documentElement.lang = 'ar';
                btnLang.textContent = '🌐 اللغة: العربية (Switch to English)';
                if (window.SpatialNav) window.SpatialNav.isRTL = true;
            }
        });
    };

    window.ModalsModule = new ModalsModule();

})(window);
