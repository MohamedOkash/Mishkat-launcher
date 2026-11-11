/**
 * Mishkat Smart TV - Astronomical Prayer Calculator & Adhan Player
 * Mirrors Android: PrayerEngine.kt, PrayerUi.kt, AdhanSoundPlayer.kt
 * Pure ES6 (Chromium 76 Safe)
 */

(function(window) {
    'use strict';

    function PrayerModule() {
        this.cities = [];
        this.currentCity = null;
        this.todayPrayers = null;
        this.nextPrayer = null;

        this.adhanAudio = new Audio();
        this.adhanVoices = [
            { id: 'makkah', name: 'أذان الحرم المكي الشريف', path: 'assets/audio/adhan_makkah.mp3' },
            { id: 'abdulbasit', name: 'الشيخ عبد الباسط عبد الصمد', path: 'assets/audio/adhan_makkah.mp3' },
            { id: 'afasy', name: 'الشيخ مشاري راشد العفاسي', path: 'assets/audio/adhan_makkah.mp3' },
            { id: 'cairo', name: 'إذاعة القرآن الكريم - القاهرة', path: 'assets/audio/adhan_makkah.mp3' }
        ];
        this.activeVoiceId = 'makkah';

        this.countdownTimer = null;

        this.init();
    }

    PrayerModule.prototype.init = function() {
        var data = window.MISHKAT_DATA || {};
        this.cities = data.cities || [];

        // Load saved city
        var savedCityId = localStorage.getItem('mishkat_prayer_city') || 'cairo';
        this.currentCity = this.findCity(savedCityId) || this.cities[0] || { nameAr: 'القاهرة', lat: 30.0444, lng: 31.2357, timezone: 2 };

        var savedVoice = localStorage.getItem('mishkat_muadhin_voice');
        if (savedVoice) this.activeVoiceId = savedVoice;

        this.calculateTodayPrayers();
        this.setupCityModal();
        this.setupMuadhinModal();
        this.startCountdownLoop();
    };

    PrayerModule.prototype.findCity = function(cityId) {
        for (var i = 0; i < this.cities.length; i++) {
            if (this.cities[i].id === cityId) return this.cities[i];
        }
        return null;
    };

    PrayerModule.prototype.calculateTodayPrayers = function() {
        var now = new Date();
        var lat = this.currentCity.lat || 30.0444;
        var lng = this.currentCity.lng || 31.2357;
        var tz = this.currentCity.timezone !== undefined ? this.currentCity.timezone : 2;

        // Standard Egyptian General Authority of Survey calculation
        var d = now.getDate();
        var m = now.getMonth() + 1;
        var y = now.getFullYear();

        // Julian date
        var jd = Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d - 1524.5;
        var dDays = jd - 2451545.0;

        var g = (357.529 + 0.98560028 * dDays) % 360;
        var q = (280.459 + 0.98564736 * dDays) % 360;
        var L = (q + 1.915 * Math.sin(g * Math.PI / 180) + 0.020 * Math.sin(2 * g * Math.PI / 180)) % 360;
        var e = 23.439 - 0.00000036 * dDays;
        var ra = Math.atan2(Math.cos(e * Math.PI / 180) * Math.sin(L * Math.PI / 180), Math.cos(L * Math.PI / 180)) * 180 / Math.PI / 15;
        ra = (ra + 24) % 24;

        var dSun = Math.asin(Math.sin(e * Math.PI / 180) * Math.sin(L * Math.PI / 180)) * 180 / Math.PI;
        var noon = (12 + tz - lng / 15 - (q / 15 - ra) + 24) % 24;

        // Fajr (19.5 deg) & Isha (17.5 deg)
        var fajrAngle = -19.5;
        var ishaAngle = -17.5;
        var sunRadius = -0.833;

        var fajrDiff = this.hourAngle(fajrAngle, lat, dSun) / 15;
        var sunriseDiff = this.hourAngle(sunRadius, lat, dSun) / 15;
        var sunsetDiff = this.hourAngle(sunRadius, lat, dSun) / 15;
        var ishaDiff = this.hourAngle(ishaAngle, lat, dSun) / 15;

        // Asr (Shafi'i shadow = 1)
        var asrAngle = -Math.atan(1 + Math.tan(Math.abs(lat - dSun) * Math.PI / 180)) * 180 / Math.PI;
        var asrDiff = this.hourAngle(asrAngle, lat, dSun) / 15;

        var fajr = noon - fajrDiff;
        var dhuhr = noon;
        var asr = noon + asrDiff;
        var maghrib = noon + sunsetDiff;
        var isha = noon + ishaDiff;

        this.todayPrayers = {
            fajr: this.hoursToDate(now, fajr),
            dhuhr: this.hoursToDate(now, dhuhr),
            asr: this.hoursToDate(now, asr),
            maghrib: this.hoursToDate(now, maghrib),
            isha: this.hoursToDate(now, isha)
        };

        this.updatePrayersUI();
    };

    PrayerModule.prototype.hourAngle = function(alpha, lat, dec) {
        var cosHA = (Math.sin(alpha * Math.PI / 180) - Math.sin(lat * Math.PI / 180) * Math.sin(dec * Math.PI / 180)) /
                    (Math.cos(lat * Math.PI / 180) * Math.cos(dec * Math.PI / 180));
        cosHA = Math.max(-1, Math.min(1, cosHA));
        return Math.acos(cosHA) * 180 / Math.PI;
    };

    PrayerModule.prototype.hoursToDate = function(refDate, hours) {
        var h = Math.floor(hours);
        var m = Math.floor((hours - h) * 60);
        var s = Math.floor(((hours - h) * 60 - m) * 60);
        var res = new Date(refDate.getTime());
        res.setHours(h, m, s, 0);
        return res;
    };

    PrayerModule.prototype.formatTime = function(date) {
        var h = date.getHours();
        var m = date.getMinutes();
        var period = h >= 12 ? 'م' : 'ص';
        var h12 = h % 12;
        if (h12 === 0) h12 = 12;
        return (h12 < 10 ? '0' : '') + h12 + ':' + (m < 10 ? '0' : '') + m + ' ' + period;
    };

    PrayerModule.prototype.updatePrayersUI = function() {
        if (!this.todayPrayers) return;
        var tF = document.getElementById('timeFajr');
        var tD = document.getElementById('timeDhuhr');
        var tA = document.getElementById('timeAsr');
        var tM = document.getElementById('timeMaghrib');
        var tI = document.getElementById('timeIsha');

        if (tF) tF.textContent = this.formatTime(this.todayPrayers.fajr);
        if (tD) tD.textContent = this.formatTime(this.todayPrayers.dhuhr);
        if (tA) tA.textContent = this.formatTime(this.todayPrayers.asr);
        if (tM) tM.textContent = this.formatTime(this.todayPrayers.maghrib);
        if (tI) tI.textContent = this.formatTime(this.todayPrayers.isha);

        var cityLabel = document.getElementById('prayerCityLabel');
        if (cityLabel) cityLabel.textContent = this.currentCity.nameAr || 'القاهرة';
    };

    PrayerModule.prototype.startCountdownLoop = function() {
        var self = this;
        var countdownEl = document.getElementById('prayerCountdown');

        var tick = function() {
            var now = new Date();
            var prayers = self.todayPrayers;
            if (!prayers) return;

            var slots = [
                { name: 'الفجر', date: prayers.fajr, id: 'slotFajr' },
                { name: 'الظهر', date: prayers.dhuhr, id: 'slotDhuhr' },
                { name: 'العصر', date: prayers.asr, id: 'slotAsr' },
                { name: 'المغرب', date: prayers.maghrib, id: 'slotMaghrib' },
                { name: 'العشاء', date: prayers.isha, id: 'slotIsha' }
            ];

            // Find next prayer
            var next = null;
            var activeSlotId = 'slotIsha';

            for (var i = 0; i < slots.length; i++) {
                if (now < slots[i].date) {
                    next = slots[i];
                    activeSlotId = (i > 0) ? slots[i - 1].id : 'slotIsha';
                    break;
                }
            }

            if (!next) {
                // Next is tomorrow's Fajr
                var tomorrowFajr = new Date(prayers.fajr.getTime() + 24 * 3600 * 1000);
                next = { name: 'الفجر', date: tomorrowFajr, id: 'slotFajr' };
                activeSlotId = 'slotIsha';
            }

            // Update slot active class
            for (var j = 0; j < slots.length; j++) {
                var el = document.getElementById(slots[j].id);
                if (el) {
                    if (slots[j].id === activeSlotId) el.classList.add('active');
                    else el.classList.remove('active');
                }
            }

            // Update countdown digits
            var diffSec = Math.floor((next.date.getTime() - now.getTime()) / 1000);
            if (diffSec <= 0) {
                self.triggerAdhanOverlay(next.name);
                self.calculateTodayPrayers();
                diffSec = 1;
            }

            var hh = Math.floor(diffSec / 3600);
            var mm = Math.floor((diffSec % 3600) / 60);
            var ss = diffSec % 60;

            var hhStr = (hh < 10 ? '0' : '') + hh;
            var mmStr = (mm < 10 ? '0' : '') + mm;
            var ssStr = (ss < 10 ? '0' : '') + ss;

            if (countdownEl) {
                countdownEl.textContent = hhStr + ':' + mmStr + ':' + ssStr;
            }
        };

        tick();
        if (this.countdownTimer) clearInterval(this.countdownTimer);
        this.countdownTimer = setInterval(tick, 1000);
    };

    PrayerModule.prototype.triggerAdhanOverlay = function(prayerName) {
        var overlay = document.getElementById('adhanOverlay');
        var nameEl = document.getElementById('adhanPrayerName');
        if (nameEl) nameEl.textContent = 'حان الآن موعد أذان ' + prayerName;

        if (overlay && window.SpatialNav) {
            window.SpatialNav.openModal(overlay, '#btnDismissAdhan');
        }

        // Play adhan audio
        var voice = this.adhanVoices[0];
        for (var i = 0; i < this.adhanVoices.length; i++) {
            if (this.adhanVoices[i].id === this.activeVoiceId) {
                voice = this.adhanVoices[i];
                break;
            }
        }
        this.adhanAudio.src = voice.path;
        this.adhanAudio.play().catch(function(){});
    };

    PrayerModule.prototype.setupCityModal = function() {
        var self = this;
        var grid = document.getElementById('citiesGrid');
        if (!grid) return;
        grid.innerHTML = '';

        for (var i = 0; i < this.cities.length; i++) {
            (function(c) {
                var card = document.createElement('div');
                card.className = 'focusable city-card' + (c.id === self.currentCity.id ? ' active' : '');
                card.tabIndex = 0;
                card.innerHTML = 
                    '<div class="city-name">' + c.nameAr + '</div>' +
                    '<div class="city-country">' + (c.countryAr || 'العالم العربي') + '</div>';

                card.addEventListener('click', function() {
                    self.currentCity = c;
                    localStorage.setItem('mishkat_prayer_city', c.id);
                    self.calculateTodayPrayers();
                    if (window.SpatialNav) window.SpatialNav.closeTopModal();
                });

                grid.appendChild(card);
            })(this.cities[i]);
        }
    };

    PrayerModule.prototype.setupMuadhinModal = function() {
        var self = this;
        var container = document.getElementById('muadhinList');
        if (!container) return;
        container.innerHTML = '';

        for (var i = 0; i < this.adhanVoices.length; i++) {
            (function(v) {
                var item = document.createElement('div');
                item.className = 'focusable muadhin-item' + (v.id === self.activeVoiceId ? ' active' : '');
                item.tabIndex = 0;
                item.innerHTML = 
                    '<div class="muadhin-details">' +
                        '<span class="muadhin-icon">📢</span>' +
                        '<span class="muadhin-name">' + v.name + '</span>' +
                    '</div>' +
                    '<button class="focusable action-btn preview-btn" tabindex="0">▶ استماع وتجربة</button>';

                var previewBtn = item.querySelector('.preview-btn');
                if (previewBtn) {
                    previewBtn.addEventListener('click', function(e) {
                        e.stopPropagation();
                        self.adhanAudio.src = v.path;
                        self.adhanAudio.play().catch(function(){});
                    });
                }

                item.addEventListener('click', function() {
                    self.activeVoiceId = v.id;
                    localStorage.setItem('mishkat_muadhin_voice', v.id);
                    if (window.SpatialNav) window.SpatialNav.closeTopModal();
                });

                container.appendChild(item);
            })(this.adhanVoices[i]);
        }
    };

    window.PrayerModule = new PrayerModule();

})(window);
