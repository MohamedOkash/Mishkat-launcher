/**
 * Mishkat Smart TV - Theme & Wallpaper Engine
 * Mirrors Android: ThemeWallpaper.kt, ThemeRepository.kt, AutoWallpaperManager.kt
 * Pure ES6 (Chromium 76 Safe)
 */

(function(window) {
    'use strict';

    function ThemeEngine() {
        this.themes = [];
        this.wallpapers = [
            // Four Seasons 4K
            { id: 'season_spring_4k', name: 'ربيع الطبيعة والزهور 4K', path: 'assets/wallpapers/season_spring_4k.jpg', cat: 'seasons' },
            { id: 'season_summer_4k', name: 'شمس وأمواج الصيف 4K', path: 'assets/wallpapers/season_summer_4k.jpg', cat: 'seasons' },
            { id: 'season_autumn_4k', name: 'خريف الغابات الذهبية 4K', path: 'assets/wallpapers/season_autumn_4k.jpg', cat: 'seasons' },
            { id: 'season_winter_4k', name: 'شتاء وثلوج نقية 4K', path: 'assets/wallpapers/season_winter_4k.jpg', cat: 'seasons' },

            // Nature 4K
            { id: 'nature_mountains_4k', name: 'قمم الجبال الساحرة 4K', path: 'assets/wallpapers/nature_mountains_4k.jpg', cat: 'nature' },
            { id: 'nature_waterfall_4k', name: 'شلالات الطبيعة العذبة 4K', path: 'assets/wallpapers/nature_waterfall_4k.jpg', cat: 'nature' },

            // Dark OLED Luxury 4K
            { id: 'dark_luxury_onyx_4k', name: 'أونيكس أسود فاخر OLED 4K', path: 'assets/wallpapers/dark_luxury_onyx_4k.jpg', cat: 'dark' },
            { id: 'dark_carbon_mesh_4k', name: 'ألياف الكربون المظلمة 4K', path: 'assets/wallpapers/dark_carbon_mesh_4k.jpg', cat: 'dark' },

            // Birds & Wildlife 4K
            { id: 'birds_falcon_eagle_4k', name: 'صقور ونسور الجبال 4K', path: 'assets/wallpapers/birds_falcon_eagle_4k.jpg', cat: 'birds' },
            { id: 'birds_colorful_kingfisher_4k', name: 'طيور ملونة وببغاوات 4K', path: 'assets/wallpapers/birds_colorful_kingfisher_4k.jpg', cat: 'birds' },

            // Classic Islamic & Landmarks
            { id: 'real_makkah_kaaba', name: 'الكعبة المشرفة - مكة المكرمة', path: 'assets/wallpapers/real_makkah_kaaba.jpg', cat: 'islamic' },
            { id: 'real_madinah_mosque', name: 'المسجد النبوي - المدينة المنورة', path: 'assets/wallpapers/real_madinah_mosque.jpg', cat: 'islamic' },
            { id: 'real_islamic_mosque', name: 'عمارة المساجد الإسلامية', path: 'assets/wallpapers/real_islamic_mosque.jpg', cat: 'islamic' },
            { id: 'real_desert_night', name: 'صحراء العرب والنجوم ليلاً', path: 'assets/wallpapers/real_desert_night.jpg', cat: 'nature' },

            // Classic Ambients
            { id: 'real_autumn_forest', name: 'ألوان أوراق الخريف الكلاسيكية', path: 'assets/wallpapers/real_autumn_forest.jpg', cat: 'seasons' },
            { id: 'real_summer_sunset', name: 'شاطئ البحر وغروب الشمس', path: 'assets/wallpapers/real_summer_sunset.jpg', cat: 'seasons' },
            { id: 'real_spring_blossom', name: 'زهور الربيع الخضراء', path: 'assets/wallpapers/real_spring_blossom.jpg', cat: 'seasons' },
            { id: 'real_winter_snow', name: 'ثلوج الشتاء النقية', path: 'assets/wallpapers/real_winter_snow.jpg', cat: 'seasons' },
            { id: 'real_winter_rain', name: 'أمطار الشتاء وأضواء الليل', path: 'assets/wallpapers/real_winter_rain.jpg', cat: 'seasons' },
            { id: 'real_cinema_night', name: 'سينما وأجواء الأفلام', path: 'assets/wallpapers/real_cinema_night.jpg', cat: 'ambient' },
            { id: 'real_sports_stadium', name: 'الملاعب والرياضة الحماسية', path: 'assets/wallpapers/real_sports_stadium.jpg', cat: 'ambient' },
            { id: 'real_storm_lightning', name: 'عواصف وبروق الطبيعة', path: 'assets/wallpapers/real_storm_lightning.jpg', cat: 'nature' }
        ];

        this.currentTheme = null;
        this.currentWallpaperIndex = 0;
        this.autoCycleTimer = null;
        this.cycleIntervalMinutes = 15; // 5, 15, or 60 min
        this.autoCycleEnabled = true;

        this.init();
    }

    ThemeEngine.prototype.init = function() {
        var data = window.MISHKAT_DATA || {};
        this.themes = data.themes || [];

        // Load saved preferences
        var savedThemeId = localStorage.getItem('mishkat_active_theme') || 'kaaba_gold';
        var savedCycle = localStorage.getItem('mishkat_wallpaper_cycle');
        if (savedCycle) {
            this.cycleIntervalMinutes = parseInt(savedCycle, 10) || 15;
        }
        var savedAuto = localStorage.getItem('mishkat_wallpaper_auto');
        if (savedAuto !== null) {
            this.autoCycleEnabled = (savedAuto === 'true');
        }

        this.applyTheme(savedThemeId);
        this.setupAutoCycle();
    };

    ThemeEngine.prototype.applyTheme = function(themeId) {
        var found = null;
        for (var i = 0; i < this.themes.length; i++) {
            if (this.themes[i].id === themeId) {
                found = this.themes[i];
                break;
            }
        }
        if (!found && this.themes.length > 0) {
            found = this.themes[0];
        }
        if (!found) return;

        this.currentTheme = found;
        localStorage.setItem('mishkat_active_theme', found.id);

        // Update Wallpaper
        if (found.wallpaper) {
            this.setWallpaper(found.wallpaper);
        }

        // Apply Theme Tokens to CSS Variables (Primary, glow, surface, glass blur)
        var root = document.documentElement;
        if (root && root.style) {
            var primary = found.primary || '#F59E0B';
            var surface = found.surface || '#070B14';
            root.style.setProperty('--accent-gold', primary);
            root.style.setProperty('--accent-gold-glow', primary + '73');
            root.style.setProperty('--glass-border-focus', primary);
            root.style.setProperty('--theme-surface', surface);
        }

        // Update Top Bar Label
        var labelEl = document.getElementById('currentThemeLabel');
        if (labelEl) {
            labelEl.textContent = 'المظهر: ' + found.name;
        }

        console.log('Applied theme:', found.name, '(' + found.id + ')');
    };

    ThemeEngine.prototype.setWallpaper = function(wallpaperPath) {
        var layer1 = document.getElementById('wallpaperLayer');
        var layer2 = document.getElementById('wallpaperLayerNext');

        if (!layer1) return;

        // If second layer exists, do smooth crossfade
        if (layer2) {
            layer2.style.backgroundImage = 'url(\'' + wallpaperPath + '\')';
            layer2.style.opacity = '1';
            setTimeout(function() {
                layer1.style.backgroundImage = 'url(\'' + wallpaperPath + '\')';
                layer2.style.opacity = '0';
            }, 600);
        } else {
            layer1.style.backgroundImage = 'url(\'' + wallpaperPath + '\')';
        }
    };

    ThemeEngine.prototype.setupAutoCycle = function() {
        var self = this;
        if (this.autoCycleTimer) {
            clearInterval(this.autoCycleTimer);
            this.autoCycleTimer = null;
        }

        if (!this.autoCycleEnabled || this.cycleIntervalMinutes <= 0) return;

        var intervalMs = this.cycleIntervalMinutes * 60 * 1000;
        this.autoCycleTimer = setInterval(function() {
            self.cycleToNextWallpaper();
        }, intervalMs);
    };

    ThemeEngine.prototype.cycleToNextWallpaper = function() {
        if (!this.wallpapers.length) return;
        this.currentWallpaperIndex = (this.currentWallpaperIndex + 1) % this.wallpapers.length;
        var wp = this.wallpapers[this.currentWallpaperIndex];
        this.setWallpaper(wp.path);
        console.log('Auto-cycled wallpaper to:', wp.name);
    };

    ThemeEngine.prototype.setAutoCycleInterval = function(minutes) {
        this.cycleIntervalMinutes = minutes;
        this.autoCycleEnabled = (minutes > 0);
        localStorage.setItem('mishkat_wallpaper_cycle', String(minutes));
        localStorage.setItem('mishkat_wallpaper_auto', String(this.autoCycleEnabled));
        this.setupAutoCycle();
    };

    window.ThemeEngine = new ThemeEngine();

})(window);
