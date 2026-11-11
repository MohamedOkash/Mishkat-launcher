/**
 * Mishkat Smart TV - Home Screen Orchestrator
 * Mirrors Android: HomeScreen.kt, AppsRepository.kt
 * Pure ES6 (Chromium 76 Safe)
 */

(function(window) {
    'use strict';

    function HomeScreen() {
        this.channels = [];
        this.filteredChannels = [];
        this.activeSatellite = 'all';
        this.activeCategory = 'all';

        this.adhkar = [];
        this.currentDhikrIndex = 0;
        this.adhkarCounter = 0;
        this.adhkarTimer = null;

        this.init();
    }

    HomeScreen.prototype.init = function() {
        var data = window.MISHKAT_DATA || {};
        this.channels = data.channels || [];
        this.adhkar = data.adhkar || [];
        this.filteredChannels = this.channels.slice();

        this.setupClockAndDate();
        this.renderSamsungApps();
        this.setupAdhkar();
        this.setupSatellitesAndCategories();
        this.renderChannels();
    };

    HomeScreen.prototype.setupClockAndDate = function() {
        var update = function() {
            var now = new Date();
            var h = now.getHours();
            var m = now.getMinutes();
            var mStr = (m < 10 ? '0' : '') + m;
            var period = h >= 12 ? 'م' : 'ص';
            var h12 = h % 12;
            if (h12 === 0) h12 = 12;
            var hStr = (h12 < 10 ? '0' : '') + h12;

            var clockEl = document.getElementById('clockTime');
            if (clockEl) {
                clockEl.textContent = hStr + ':' + mStr + ' ' + period;
            }
        };
        update();
        setInterval(update, 10000);
    };

    HomeScreen.prototype.renderSamsungApps = function() {
        var self = this;
        var container = document.getElementById('appsRow');
        if (!container) return;
        container.innerHTML = '';

        // 1. Router Media Server UPnP Card (H188A)
        var upnpCard = document.createElement('div');
        upnpCard.className = 'focusable app-card';
        upnpCard.tabIndex = 0;
        upnpCard.innerHTML = 
            '<div class="app-card-top">' +
                '<span class="app-icon-emoji">💽</span>' +
                '<span class="app-badge-pill" style="background: #0284C7;">ROUTER UPnP</span>' +
            '</div>' +
            '<div>' +
                '<div class="app-title">أفلام فلاشة الراوتر (H188A)</div>' +
                '<div class="app-desc">خادم وسائط UPnP • 192.168.1.1</div>' +
            '</div>';

        upnpCard.addEventListener('click', function() {
            if (window.StorageExplorer) {
                window.StorageExplorer.open();
            } else if (typeof tizen !== 'undefined' && tizen.application) {
                tizen.application.launch('org.tizen.browser', function(){}, function(){});
            } else {
                window.open('http://192.168.1.1:52869/', '_blank');
            }
        });
        container.appendChild(upnpCard);

        // 2. Fetch Installed Tizen Apps or Use Guaranteed Smart TV Suite
        if (typeof tizen !== 'undefined' && tizen.application && typeof tizen.application.getAppsInfo === 'function') {
            tizen.application.getAppsInfo(function(apps) {
                var excludeList = ['tts-', 'csft-', 'daemon', 'coba.setting.devicemanager', 'sealayer', 'ad-web', 'aria-engine', 'automation-alert', 'Mishkat001'];
                var validApps = [];

                for (var i = 0; i < apps.length; i++) {
                    var app = apps[i];
                    var idLower = (app.id || '').toLowerCase();
                    var isExcluded = false;
                    for (var j = 0; j < excludeList.length; j++) {
                        if (idLower.indexOf(excludeList[j]) !== -1) {
                            isExcluded = true;
                            break;
                        }
                    }
                    if (!isExcluded) {
                        validApps.push(app);
                    }
                }

                if (validApps.length > 0) {
                    for (var k = 0; k < Math.min(validApps.length, 16); k++) {
                        self.createAppCard(container, validApps[k]);
                    }
                } else {
                    self.renderFallbackApps(container);
                }
            }, function(err) {
                console.warn('Tizen getAppsInfo error, using fallback apps:', err);
                self.renderFallbackApps(container);
            });
        } else {
            this.renderFallbackApps(container);
        }
    };

    HomeScreen.prototype.createAppCard = function(container, app) {
        var card = document.createElement('div');
        card.className = 'focusable app-card';
        card.tabIndex = 0;

        var iconHtml = app.iconPath 
            ? '<img src="' + app.iconPath + '" class="app-icon-img" alt="' + app.name + '" onerror="this.outerHTML=\'<span class=\\\'app-icon-emoji\\\'>📺</span>\'">'
            : '<span class="app-icon-emoji">📺</span>';

        card.innerHTML = 
            '<div class="app-card-top">' +
                iconHtml +
                '<span class="app-badge-pill">TIZEN</span>' +
            '</div>' +
            '<div>' +
                '<div class="app-title">' + app.name + '</div>' +
                '<div class="app-desc">' + app.id + '</div>' +
            '</div>';

        card.addEventListener('click', function() {
            try {
                if (typeof tizen !== 'undefined' && tizen.application) {
                    tizen.application.launch(app.id, 
                        function() { console.log('Launched app:', app.name); },
                        function(e) { console.error('Launch failed:', e); }
                    );
                }
            } catch(e) {
                console.error(e);
            }
        });

        container.appendChild(card);
    };

    HomeScreen.prototype.renderFallbackApps = function(container) {
        var defaultApps = [
            { name: 'YouTube', icon: '▶️', badge: 'VIDEO', id: 'org.tizen.youtube-app' },
            { name: 'Netflix', icon: '🍿', badge: 'CINEMA', id: 'org.tizen.netflix-app' },
            { name: 'Prime Video', icon: '🎬', badge: 'MOVIES', id: 'org.tizen.primevideo' },
            { name: 'Shahid VIP', icon: '⭐', badge: 'ARABIC', id: 'com.mbc.shahid' },
            { name: 'Apple TV', icon: '🍏', badge: 'STREAM', id: 'com.samsung.tv.aria-video' },
            { name: 'متصفح الإنترنت (Browser)', icon: '🌐', badge: 'WEB', id: 'org.tizen.browser' },
            { name: 'الريسيفر الداخلي (Live TV)', icon: '📡', badge: 'DTV', id: 'com.samsung.tv.coba.source' },
            { name: 'معرض الصور (Gallery)', icon: '🖼️', badge: 'MEDIA', id: 'com.samsung.tv.gallery' },
            { name: 'HDMI 1 (جهاز خارجي)', icon: '🔌', badge: 'HDMI', id: 'hdmi1' },
            { name: 'HDMI 2 (eARC ألعاب)', icon: '🎮', badge: 'HDMI', id: 'hdmi2' }
        ];

        for (var i = 0; i < defaultApps.length; i++) {
            var item = defaultApps[i];
            var card = document.createElement('div');
            card.className = 'focusable app-card';
            card.tabIndex = 0;
            card.innerHTML = 
                '<div class="app-card-top">' +
                    '<span class="app-icon-emoji">' + item.icon + '</span>' +
                    '<span class="app-badge-pill">' + item.badge + '</span>' +
                '</div>' +
                '<div>' +
                    '<div class="app-title">' + item.name + '</div>' +
                    '<div class="app-desc">تطبيق سامسونج الذكي</div>' +
                '</div>';

            (function(appItem) {
                card.addEventListener('click', function() {
                    if (typeof tizen !== 'undefined' && tizen.application) {
                        tizen.application.launch(appItem.id, function(){}, function(){});
                    }
                });
            })(item);

            container.appendChild(card);
        }
    };

    HomeScreen.prototype.setupAdhkar = function() {
        var self = this;
        var bodyEl = document.getElementById('adhkarBodyText');
        var catEl = document.getElementById('adhkarCategory');
        var cardEl = document.getElementById('adhkarCard');
        var counterEl = document.getElementById('adhkarCounter');

        var display = function() {
            if (!self.adhkar.length) return;
            var item = self.adhkar[self.currentDhikrIndex];
            if (bodyEl) {
                bodyEl.style.opacity = '0';
                setTimeout(function() {
                    bodyEl.textContent = '«' + item.textAr + '»';
                    if (catEl) {
                        var catText = item.window === 'MORNING' ? 'أذكار الصباح' : (item.window === 'EVENING' ? 'أذكار المساء' : 'أذكار وأدعية مأثورة');
                        catEl.textContent = catText;
                    }
                    bodyEl.style.opacity = '1';
                }, 200);
            }
        };

        display();

        if (this.adhkarTimer) clearInterval(this.adhkarTimer);
        this.adhkarTimer = setInterval(function() {
            self.currentDhikrIndex = (self.currentDhikrIndex + 1) % self.adhkar.length;
            self.adhkarCounter = 0;
            if (counterEl) counterEl.textContent = '٠';
            display();
        }, 20000);

        // Clicking Adhkar Card increments Tasbeeh counter or moves to next dhikr
        if (cardEl) {
            cardEl.addEventListener('click', function() {
                self.adhkarCounter++;
                if (counterEl) {
                    counterEl.textContent = String(self.adhkarCounter);
                    counterEl.classList.add('pulse');
                    setTimeout(function() { counterEl.classList.remove('pulse'); }, 300);
                }
            });
        }
    };

    HomeScreen.prototype.setupSatellitesAndCategories = function() {
        var self = this;
        var satTabs = document.querySelectorAll('.sat-tab');
        for (var i = 0; i < satTabs.length; i++) {
            (function(tab) {
                tab.addEventListener('click', function() {
                    for (var j = 0; j < satTabs.length; j++) {
                        satTabs[j].classList.remove('active');
                    }
                    tab.classList.add('active');
                    self.activeSatellite = tab.getAttribute('data-sat') || 'all';
                    self.filterAndRenderChannels();
                });
            })(satTabs[i]);
        }

        var catRow = document.getElementById('categoriesRow');
        if (!catRow) return;
        catRow.innerHTML = '';

        var categories = [
            { id: 'all', title: 'الكل' },
            { id: 'islamic_quran', title: 'القرآن والإسلامية' },
            { id: 'kids_education', title: 'الأطفال والتعليم' },
            { id: 'drama_series', title: 'المسلسلات والدراما' },
            { id: 'cinema_movies', title: 'السينما والأفلام' },
            { id: 'egypt_general', title: 'القنوات المصرية' },
            { id: 'news_doc', title: 'الأخبار والوثائقيات' },
            { id: 'sports_cooking', title: 'الرياضة والطبخ' }
        ];

        for (var k = 0; k < categories.length; k++) {
            (function(cat) {
                var btn = document.createElement('button');
                btn.className = 'focusable cat-pill' + (self.activeCategory === cat.id ? ' active' : '');
                btn.tabIndex = 0;
                btn.textContent = cat.title;
                btn.setAttribute('data-category', cat.id);
                btn.addEventListener('click', function() {
                    var pills = catRow.querySelectorAll('.cat-pill');
                    for (var p = 0; p < pills.length; p++) {
                        pills[p].classList.remove('active');
                    }
                    btn.classList.add('active');
                    self.activeCategory = cat.id;
                    self.filterAndRenderChannels();
                });
                catRow.appendChild(btn);
            })(categories[k]);
        }
    };

    HomeScreen.prototype.filterAndRenderChannels = function() {
        var self = this;
        this.filteredChannels = [];
        for (var i = 0; i < this.channels.length; i++) {
            var ch = this.channels[i];
            var matchSat = (self.activeSatellite === 'all' || ch.satellite === self.activeSatellite);
            var matchCat = (self.activeCategory === 'all' || ch.category === self.activeCategory);
            if (matchSat && matchCat) {
                self.filteredChannels.push(ch);
            }
        }
        this.renderChannels();
    };

    HomeScreen.prototype.renderChannels = function() {
        var self = this;
        var row = document.getElementById('channelsRow');
        if (!row) return;
        row.innerHTML = '';

        if (!this.filteredChannels.length) {
            row.innerHTML = '<div style="padding: 30px; font-size: 20px; color: #94A3B8;">لا توجد قنوات مطابقة لهذا التصنيف</div>';
            return;
        }

        for (var i = 0; i < this.filteredChannels.length; i++) {
            (function(ch, idx) {
                var card = document.createElement('div');
                card.className = 'focusable channel-card';
                card.tabIndex = 0;

                var freqInfo = ch.frequency ? (ch.frequency + ' ' + (ch.polarization || 'V')) : '1080p Full HD';
                var satLabel = ch.satellite ? ch.satellite.replace(/_/g, ' ') : 'نايل سات';

                card.innerHTML = 
                    '<div class="channel-card-top">' +
                        '<span class="channel-glyph">' + (ch.glyph || '📺') + '</span>' +
                        '<span class="channel-badge">' + (ch.badge || '1080p HD') + '</span>' +
                    '</div>' +
                    '<div>' +
                        '<div class="channel-title">' + (ch.nameAr || ch.name) + '</div>' +
                        '<div class="channel-freq">' + satLabel + ' • ' + freqInfo + '</div>' +
                    '</div>';

                card.addEventListener('click', function() {
                    if (window.LiveTvPlayer) {
                        window.LiveTvPlayer.setChannels(self.filteredChannels);
                        window.LiveTvPlayer.playChannel(ch, idx);
                    }
                });

                row.appendChild(card);
            })(this.filteredChannels[i], i);
        }
    };

    window.HomeScreen = new HomeScreen();

})(window);
