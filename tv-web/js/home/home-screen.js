/**
 * Mishkat Smart TV - Home Screen Orchestrator
 * Mirrors Android: HomeScreen.kt, AppsRepository.kt
 * Pure ES6 (Chromium 76 Safe)
 */

(function(window) {
    'use strict';

    // Luxury Brand Vector Icons (Pure Inline SVG)
    var APP_ICONS = {
        router_upnp: '<svg viewBox="0 0 64 64" fill="none" class="app-svg-icon"><circle cx="32" cy="32" r="30" fill="url(#gradUpnp)"/><path d="M18 24C18 20.6863 20.6863 18 24 18H40C43.3137 18 46 20.6863 46 24V40C46 43.3137 43.3137 46 40 46H24C20.6863 46 18 43.3137 18 40V24Z" fill="#0F172A" stroke="#38BDF8" stroke-width="2.5"/><path d="M24 38H40M24 30H40" stroke="#38BDF8" stroke-width="2" stroke-linecap="round"/><circle cx="25" cy="24" r="2" fill="#38BDF8"/><circle cx="31" cy="24" r="2" fill="#38BDF8"/><path d="M32 12C26.4772 12 22 16.4772 22 22M32 8C24.268 8 18 14.268 18 22" stroke="#38BDF8" stroke-width="2.5" stroke-linecap="round"/><defs><linearGradient id="gradUpnp" x1="0" y1="0" x2="64" y2="64"><stop stop-color="#0284C7"/><stop offset="1" stop-color="#0369A1"/></linearGradient></defs></svg>',
        youtube: '<svg viewBox="0 0 64 64" fill="none" class="app-svg-icon"><rect width="64" height="64" rx="18" fill="#FF0000"/><path d="M47.7 22.8C47.2 20.8 45.6 19.3 43.7 18.8C40.1 17.8 32 17.8 32 17.8C32 17.8 23.9 17.8 20.3 18.8C18.4 19.3 16.8 20.8 16.3 22.8C15.3 26.4 15.3 33.9 15.3 33.9C15.3 33.9 15.3 41.4 16.3 45C16.8 47 18.4 48.5 20.3 49C23.9 50 32 50 32 50C32 50 40.1 50 43.7 49C45.6 48.5 47.2 47 47.7 45C48.7 41.4 48.7 33.9 48.7 33.9C48.7 33.9 48.7 26.4 47.7 22.8ZM28.6 40.7V27.1L40.5 33.9L28.6 40.7Z" fill="white"/></svg>',
        netflix: '<svg viewBox="0 0 64 64" fill="none" class="app-svg-icon"><rect width="64" height="64" rx="18" fill="#141414"/><path d="M21 16H27V48H21V16Z" fill="#E50914"/><path d="M37 16H43V48H37V16Z" fill="#E50914"/><path d="M21 16L43 48H37L21 24.5V16Z" fill="#B20710"/><path d="M21 16H27L43 48H37L21 16Z" fill="#E50914"/></svg>',
        primevideo: '<svg viewBox="0 0 64 64" fill="none" class="app-svg-icon"><rect width="64" height="64" rx="18" fill="#00A8E1"/><path d="M18 28C18 22.4772 22.4772 18 28 18H36C41.5228 18 46 22.4772 46 28V36C46 41.5228 41.5228 46 36 46H28C22.4772 46 18 41.5228 18 36V28Z" fill="#00050D"/><path d="M22 36C28 41 36 41 42 36" stroke="#00A8E1" stroke-width="3" stroke-linecap="round"/><path d="M40 33L43 36.5L39 38" stroke="#00A8E1" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/></svg>',
        disney: '<svg viewBox="0 0 64 64" fill="none" class="app-svg-icon"><rect width="64" height="64" rx="18" fill="#113CCF"/><path d="M20 32C20 22 28 16 38 16C40 16 42 16.5 44 17.5C40 21 34 26 28 35C24 41 22 47 22 51C20.7 45 20 38 20 32Z" fill="white"/><path d="M38 28C38 34 35 40 31 46H45V40H38V36H44V31H38V28Z" fill="white"/><path d="M48 24V28H52V30H48V34H46V30H42V28H46V24H48Z" fill="#38BDF8"/></svg>',
        appletv: '<svg viewBox="0 0 64 64" fill="none" class="app-svg-icon"><rect width="64" height="64" rx="18" fill="#000000" stroke="#334155" stroke-width="1.5"/><path d="M32 23C34 23 35.5 21.8 36.5 20C35 19.8 33 21 32 23ZM36.5 27.2C35 27.2 33.6 26.2 32.2 26.2C30.7 26.2 29.2 27.2 27.8 27.2C25.5 27.2 23 25.2 23 21.5C23 17 26.8 14.5 29.5 14.5C31 14.5 32.2 15.5 33.2 15.5C34.2 15.5 35.6 14.5 37.3 14.5C38.3 14.5 40.8 15 42 16.7C39.2 18.2 39.5 22 42.5 23.2C41.8 25.2 40.2 27.2 38.5 28.8L36.5 27.2Z" fill="white" transform="matrix(1.4 0 0 1.4 -12 4)"/><text x="35" y="46" font-family="sans-serif" font-weight="900" font-size="16" fill="white">tv</text></svg>',
        hulu: '<svg viewBox="0 0 64 64" fill="none" class="app-svg-icon"><rect width="64" height="64" rx="18" fill="#1CE783"/><path d="M19 18V46H26V34H38V46H45V18H38V28H26V18H19Z" fill="#0B131E"/></svg>',
        shahid: '<svg viewBox="0 0 64 64" fill="none" class="app-svg-icon"><rect width="64" height="64" rx="18" fill="url(#gradShahid)"/><path d="M32 16L36 26H46L38 32L41 42L32 36L23 42L26 32L18 26H28L32 16Z" fill="#F59E0B"/><defs><linearGradient id="gradShahid" x1="0" y1="0" x2="64" y2="64"><stop stop-color="#1E293B"/><stop offset="1" stop-color="#0F172A"/></linearGradient></defs></svg>',
        browser: '<svg viewBox="0 0 64 64" fill="none" class="app-svg-icon"><rect width="64" height="64" rx="18" fill="url(#gradBrowser)"/><circle cx="32" cy="32" r="16" stroke="white" stroke-width="2.5"/><ellipse cx="32" cy="32" rx="7" ry="16" stroke="white" stroke-width="2.2"/><path d="M17 32H47" stroke="white" stroke-width="2.2"/><defs><linearGradient id="gradBrowser" x1="0" y1="0" x2="64" y2="64"><stop stop-color="#2563EB"/><stop offset="1" stop-color="#1D4ED8"/></linearGradient></defs></svg>',
        livetv: '<svg viewBox="0 0 64 64" fill="none" class="app-svg-icon"><rect width="64" height="64" rx="18" fill="url(#gradLiveTv)"/><circle cx="32" cy="32" r="14" stroke="#F59E0B" stroke-width="2.5"/><path d="M26 32C26 28.6863 28.6863 26 32 26C35.3137 26 38 28.6863 38 32" stroke="#F59E0B" stroke-width="2.5" stroke-linecap="round"/><circle cx="32" cy="32" r="3" fill="#F59E0B"/><path d="M40 20L46 14M24 20L18 14" stroke="#F59E0B" stroke-width="2.5" stroke-linecap="round"/><defs><linearGradient id="gradLiveTv" x1="0" y1="0" x2="64" y2="64"><stop stop-color="#0F172A"/><stop offset="1" stop-color="#1E293B"/></linearGradient></defs></svg>',
        tubi: '<svg viewBox="0 0 64 64" fill="none" class="app-svg-icon"><rect width="64" height="64" rx="18" fill="#FA4616"/><text x="32" y="39" font-family="sans-serif" font-weight="900" font-size="20" fill="white" text-anchor="middle">tubi</text></svg>',
        xbox: '<svg viewBox="0 0 64 64" fill="none" class="app-svg-icon"><circle cx="32" cy="32" r="30" fill="#107C10"/><path d="M20 20C24 24 28 32 32 40C36 32 40 24 44 20C38 15 26 15 20 20Z" fill="white"/><path d="M16 25C17 33 21 42 27 46C23 43 19 36 18 30C17 28 16.5 26.5 16 25Z" fill="white"/><path d="M48 25C47 33 43 42 37 46C41 43 45 36 46 30C47 28 47.5 26.5 48 25Z" fill="white"/></svg>',
        health: '<svg viewBox="0 0 64 64" fill="none" class="app-svg-icon"><rect width="64" height="64" rx="18" fill="#0077F8"/><path d="M32 46C32 46 18 35 18 25C18 20 22 17 26 17C29 17 31 19 32 21C33 19 35 17 38 17C42 17 46 20 46 25C46 35 32 46 32 46Z" fill="white"/></svg>',
        gallery: '<svg viewBox="0 0 64 64" fill="none" class="app-svg-icon"><rect width="64" height="64" rx="18" fill="url(#gradGallery)"/><circle cx="25" cy="25" r="4" fill="#F59E0B"/><path d="M18 44L28 32L34 38L42 28L48 44H18Z" fill="white"/><defs><linearGradient id="gradGallery" x1="0" y1="0" x2="64" y2="64"><stop stop-color="#E11D48"/><stop offset="1" stop-color="#BE123C"/></linearGradient></defs></svg>',
        hdmi: '<svg viewBox="0 0 64 64" fill="none" class="app-svg-icon"><rect width="64" height="64" rx="18" fill="#1E293B" stroke="#475569" stroke-width="1.5"/><path d="M22 24H42V36C42 38.2 40.2 40 38 40H26C23.8 40 22 38.2 22 36V24Z" fill="#334155" stroke="#94A3B8" stroke-width="2"/><path d="M26 30H38M28 34H36" stroke="#F59E0B" stroke-width="2" stroke-linecap="round"/></svg>'
    };

    // Database of Known Verified User Apps
    var KNOWN_APPS = {
        '9Ur5IzDKqV.TizenYouTube': { name: 'يوتيوب (YouTube)', badge: 'STREAM', iconKey: 'youtube', desc: 'بث مقاطع وفيديوهات 4K' },
        '111299001912': { name: 'يوتيوب (YouTube)', badge: 'STREAM', iconKey: 'youtube', desc: 'بث مقاطع وفيديوهات 4K' },
        'org.tizen.netflix-app': { name: 'نتفلكس (Netflix)', badge: 'CINEMA', iconKey: 'netflix', desc: 'أفلام ومسلسلات عالمية 4K' },
        '3201907018807': { name: 'نتفلكس (Netflix)', badge: 'CINEMA', iconKey: 'netflix', desc: 'أفلام ومسلسلات عالمية 4K' },
        'org.tizen.primevideo': { name: 'برايم فيديو (Prime)', badge: 'MOVIES', iconKey: 'primevideo', desc: 'سينما وأعمال أمازون الحصرية' },
        '3201910019365': { name: 'برايم فيديو (Prime)', badge: 'MOVIES', iconKey: 'primevideo', desc: 'سينما وأعمال أمازون الحصرية' },
        'MCmYXNxgcu.DisneyPlus': { name: 'ديزني بلس (Disney+)', badge: 'FAMILY', iconKey: 'disney', desc: 'عالم مارفل وديزني ووثائقيات' },
        '3201901017640': { name: 'ديزني بلس (Disney+)', badge: 'FAMILY', iconKey: 'disney', desc: 'عالم مارفل وديزني ووثائقيات' },
        'com.samsung.tv.aria-video': { name: 'آبل تي في (Apple TV)', badge: 'STREAM', iconKey: 'appletv', desc: 'أفلام ومسلسلات Apple TV+' },
        '3201807016597': { name: 'آبل تي في (Apple TV)', badge: 'STREAM', iconKey: 'appletv', desc: 'أفلام ومسلسلات Apple TV+' },
        'LBUAQX1exg.Hulu': { name: 'هولو (Hulu)', badge: 'SERIES', iconKey: 'hulu', desc: 'أحدث المسلسلات الأمريكية الحصرية' },
        '3201601007625': { name: 'هولو (Hulu)', badge: 'SERIES', iconKey: 'hulu', desc: 'أحدث المسلسلات الأمريكية الحصرية' },
        'com.mbc.shahid': { name: 'شاهد VIP (Shahid)', badge: 'ARABIC', iconKey: 'shahid', desc: 'أضخم إنتاجات الدراما العربية' },
        'org.tizen.browser': { name: 'متصفح الإنترنت (Web)', badge: 'WEB', iconKey: 'browser', desc: 'تصفح مواقع الإنترنت على التلفاز' },
        '3202010022079': { name: 'متصفح الإنترنت (Web)', badge: 'WEB', iconKey: 'browser', desc: 'تصفح مواقع الإنترنت على التلفاز' },
        '3KA0pm7a7V.TubiTV': { name: 'توبي (Tubi TV)', badge: 'FREE', iconKey: 'tubi', desc: 'أفلام وعروض تلفزيونية مجانية' },
        '3201504001965': { name: 'توبي (Tubi TV)', badge: 'FREE', iconKey: 'tubi', desc: 'أفلام وعروض تلفزيونية مجانية' },
        'com.samsung.tv.coba.source': { name: 'الريسيفر الداخلي (Live TV)', badge: 'DTV', iconKey: 'livetv', desc: 'البث الفضائي والأرضي المباشر' },
        'GHI3a0zMSx.XboxGamePass': { name: 'إكس بوكس (Xbox)', badge: 'GAMING', iconKey: 'xbox', desc: 'ألعاب سحابية بدون كونسول' },
        '3202203026799': { name: 'إكس بوكس (Xbox)', badge: 'GAMING', iconKey: 'xbox', desc: 'ألعاب سحابية بدون كونسول' },
        'com.samsung.tv.samsung-health': { name: 'سامسونج هيلث (Health)', badge: 'FITNESS', iconKey: 'health', desc: 'تمارين رياضية وصحة بدنية' },
        '3201909019211': { name: 'سامسونج هيلث (Health)', badge: 'FITNESS', iconKey: 'health', desc: 'تمارين رياضية وصحة بدنية' },
        'com.samsung.tv.gallery': { name: 'معرض الصور (Gallery)', badge: 'MEDIA', iconKey: 'gallery', desc: 'استعراض الصور والفيديوهات الشخصية' },
        '3201710015037': { name: 'معرض الصور (Gallery)', badge: 'MEDIA', iconKey: 'gallery', desc: 'استعراض الصور والفيديوهات الشخصية' }
    };

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

        // 1. Router Media Server UPnP Card (H188A) - Position #1
        var upnpCard = document.createElement('div');
        upnpCard.className = 'focusable app-card app-card-featured';
        upnpCard.tabIndex = 0;
        upnpCard.innerHTML = 
            '<div class="app-card-top">' +
                '<div class="app-icon-wrap">' + APP_ICONS.router_upnp + '</div>' +
                '<span class="app-badge-pill badge-cyan">ROUTER UPnP</span>' +
            '</div>' +
            '<div>' +
                '<div class="app-title">أفلام فلاشة الراوتر (UPnP)</div>' +
                '<div class="app-desc">خادم وسائط UPnP • 192.168.1.1:52868</div>' +
            '</div>';

        upnpCard.addEventListener('click', function() {
            if (window.StorageExplorer) {
                window.StorageExplorer.open('router_upnp');
            }
        });
        container.appendChild(upnpCard);

        // 2. Fetch Installed Tizen Apps or Use Guaranteed Smart TV Suite
        if (typeof tizen !== 'undefined' && tizen.application && typeof tizen.application.getAppsInfo === 'function') {
            tizen.application.getAppsInfo(function(apps) {
                // Strict Blacklist for internal daemons, test runners, providers, and clones
                var systemBlacklist = [
                    'cloning', 'csfs', 'coba.setting', 'provider', 'ftp', 'promotion', 'smartthings', 
                    'daemon', 'factory', 'test', 'security', 'emanual', 'gamebreaks', 'glance', 
                    'mishkat001', 'tts-', 'csft-', 'sealayer', 'ad-web', 'aria-engine', 'automation-alert'
                ];

                var userApps = [];
                for (var i = 0; i < apps.length; i++) {
                    var app = apps[i];
                    var idLower = (app.id || '').toLowerCase();
                    var nameLower = (app.name || '').toLowerCase();

                    var isBlacklisted = false;
                    for (var j = 0; j < systemBlacklist.length; j++) {
                        if (idLower.indexOf(systemBlacklist[j]) !== -1 || nameLower.indexOf(systemBlacklist[j]) !== -1) {
                            isBlacklisted = true;
                            break;
                        }
                    }
                    if (!isBlacklisted) {
                        userApps.push(app);
                    }
                }

                if (userApps.length > 0) {
                    for (var k = 0; k < userApps.length; k++) {
                        self.createAppCard(container, userApps[k]);
                    }
                }
                // Append standard apps if not already present
                self.ensureCoreApps(container, userApps);
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

        var known = KNOWN_APPS[app.id] || KNOWN_APPS[app.name] || null;
        var appName = known ? known.name : app.name;
        var appBadge = known ? known.badge : 'APP';
        var appDesc = known ? known.desc : 'تطبيق سامسونج سمارت';
        var iconHtml = (known && APP_ICONS[known.iconKey]) ? APP_ICONS[known.iconKey] : APP_ICONS.browser;

        card.innerHTML = 
            '<div class="app-card-top">' +
                '<div class="app-icon-wrap">' + iconHtml + '</div>' +
                '<span class="app-badge-pill">' + appBadge + '</span>' +
            '</div>' +
            '<div>' +
                '<div class="app-title">' + appName + '</div>' +
                '<div class="app-desc">' + appDesc + '</div>' +
            '</div>';

        card.addEventListener('click', function() {
            try {
                if (typeof tizen !== 'undefined' && tizen.application) {
                    tizen.application.launch(app.id, 
                        function() { console.log('Launched app:', appName); },
                        function(e) { console.error('Launch failed:', e); }
                    );
                }
            } catch(e) {
                console.error(e);
            }
        });

        container.appendChild(card);
    };

    HomeScreen.prototype.ensureCoreApps = function(container, existingApps) {
        var existingIds = [];
        for (var i = 0; i < existingApps.length; i++) {
            existingIds.push(existingApps[i].id);
        }

        var coreList = [
            { id: 'com.mbc.shahid', name: 'شاهد VIP (Shahid)', badge: 'ARABIC', iconKey: 'shahid', desc: 'أضخم إنتاجات الدراما العربية' },
            { id: 'com.samsung.tv.coba.source', name: 'الريسيفر الداخلي (Live TV)', badge: 'DTV', iconKey: 'livetv', desc: 'البث الفضائي والأرضي المباشر' },
            { id: 'hdmi1', name: 'مدخل HDMI 1', badge: 'INPUT', iconKey: 'hdmi', desc: 'جهاز الاستقبال الخارجي' },
            { id: 'hdmi2', name: 'مدخل HDMI 2 (eARC)', badge: 'INPUT', iconKey: 'hdmi', desc: 'منصة الألعاب والمسرح المنزلي' }
        ];

        for (var j = 0; j < coreList.length; j++) {
            var item = coreList[j];
            if (existingIds.indexOf(item.id) === -1) {
                this.renderSingleCard(container, item);
            }
        }
    };

    HomeScreen.prototype.renderSingleCard = function(container, item) {
        var card = document.createElement('div');
        card.className = 'focusable app-card';
        card.tabIndex = 0;

        var iconHtml = APP_ICONS[item.iconKey] || APP_ICONS.browser;

        card.innerHTML = 
            '<div class="app-card-top">' +
                '<div class="app-icon-wrap">' + iconHtml + '</div>' +
                '<span class="app-badge-pill">' + item.badge + '</span>' +
            '</div>' +
            '<div>' +
                '<div class="app-title">' + item.name + '</div>' +
                '<div class="app-desc">' + item.desc + '</div>' +
            '</div>';

        (function(targetItem) {
            card.addEventListener('click', function() {
                if (typeof tizen !== 'undefined' && tizen.application) {
                    tizen.application.launch(targetItem.id, function(){}, function(){});
                }
            });
        })(item);

        container.appendChild(card);
    };

    HomeScreen.prototype.renderFallbackApps = function(container) {
        var defaultApps = [
            { id: '9Ur5IzDKqV.TizenYouTube', name: 'يوتيوب (YouTube)', badge: 'STREAM', iconKey: 'youtube', desc: 'بث مقاطع وفيديوهات 4K' },
            { id: 'org.tizen.netflix-app', name: 'نتفلكس (Netflix)', badge: 'CINEMA', iconKey: 'netflix', desc: 'أفلام ومسلسلات عالمية 4K' },
            { id: 'org.tizen.primevideo', name: 'برايم فيديو (Prime)', badge: 'MOVIES', iconKey: 'primevideo', desc: 'سينما وأعمال أمازون الحصرية' },
            { id: 'MCmYXNxgcu.DisneyPlus', name: 'ديزني بلس (Disney+)', badge: 'FAMILY', iconKey: 'disney', desc: 'عالم مارفل وديزني ووثائقيات' },
            { id: 'com.samsung.tv.aria-video', name: 'آبل تي في (Apple TV)', badge: 'STREAM', iconKey: 'appletv', desc: 'أفلام ومسلسلات Apple TV+' },
            { id: 'com.mbc.shahid', name: 'شاهد VIP (Shahid)', badge: 'ARABIC', iconKey: 'shahid', desc: 'أضخم إنتاجات الدراما العربية' },
            { id: 'LBUAQX1exg.Hulu', name: 'هولو (Hulu)', badge: 'SERIES', iconKey: 'hulu', desc: 'أحدث المسلسلات الأمريكية الحصرية' },
            { id: 'org.tizen.browser', name: 'متصفح الإنترنت (Web)', badge: 'WEB', iconKey: 'browser', desc: 'تصفح مواقع الإنترنت على التلفاز' },
            { id: '3KA0pm7a7V.TubiTV', name: 'توبي (Tubi TV)', badge: 'FREE', iconKey: 'tubi', desc: 'أفلام وعروض تلفزيونية مجانية' },
            { id: 'com.samsung.tv.coba.source', name: 'الريسيفر الداخلي (Live TV)', badge: 'DTV', iconKey: 'livetv', desc: 'البث الفضائي والأرضي المباشر' },
            { id: 'GHI3a0zMSx.XboxGamePass', name: 'إكس بوكس (Xbox)', badge: 'GAMING', iconKey: 'xbox', desc: 'ألعاب سحابية بدون كونسول' },
            { id: 'com.samsung.tv.gallery', name: 'معرض الصور (Gallery)', badge: 'MEDIA', iconKey: 'gallery', desc: 'استعراض الصور والفيديوهات الشخصية' },
            { id: 'hdmi1', name: 'مدخل HDMI 1', badge: 'INPUT', iconKey: 'hdmi', desc: 'جهاز الاستقبال الخارجي' },
            { id: 'hdmi2', name: 'مدخل HDMI 2 (eARC)', badge: 'INPUT', iconKey: 'hdmi', desc: 'منصة الألعاب والمسرح المنزلي' }
        ];

        for (var i = 0; i < defaultApps.length; i++) {
            this.renderSingleCard(container, defaultApps[i]);
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

        // Clicking Adhkar Card increments Tasbeeh counter
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
