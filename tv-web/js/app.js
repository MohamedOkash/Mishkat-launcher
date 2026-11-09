/**
 * Mishkat Smart TV - Main Application Orchestrator
 * Auto-syncing channel catalog, Apple TV styling & TV remote experience
 */

class MishkatApp {
    constructor() {
        this.channels = [];
        this.filteredChannels = [];
        this.stations = [];
        this.currentStationIndex = 0;
        this.activeCategory = 'all';
        this.lastChannelFocused = null;
        
        this.radioAudio = new Audio();
        this.isRadioPlaying = false;
        this.sleepTimerId = null;
        this.sleepMinutes = 0;

        this.GITHUB_CHANNELS_URL = 'https://raw.githubusercontent.com/MohamedOkash/Mishkat-launcher/main/shared/channels.json';
        this.GITHUB_STATIONS_URL = 'https://raw.githubusercontent.com/MohamedOkash/Mishkat-launcher/main/shared/stations.json';

        this.init();
    }

    async init() {
        this.setupClock();
        this.setupTopBar();
        this.setupRadioEvents();
        this.setupAppShortcuts();
        
        await this.loadData();
        this.renderCategories();
        this.renderChannels();

        // Initial focus
        setTimeout(() => {
            const firstCard = document.querySelector('.channels-row .channel-card');
            if (firstCard && window.spatialNav) {
                window.spatialNav.setFocus(firstCard);
            }
        }, 300);
    }

    setupClock() {
        const updateClock = () => {
            const now = new Date();
            let h = now.getHours();
            const m = now.getMinutes().toString().padStart(2, '0');
            const period = h >= 12 ? 'م' : 'ص';
            h = h % 12 || 12;
            const clockEl = document.getElementById('clockTime');
            if (clockEl) {
                clockEl.textContent = `${h.toString().padStart(2, '0')}:${m} ${period}`;
            }
        };
        updateClock();
        setInterval(updateClock, 30000);
    }

    setupTopBar() {
        // Developer Profile Dialog
        const btnDev = document.getElementById('btnDevProfile');
        const modalDev = document.getElementById('devProfileModal');
        const btnCloseDev = document.getElementById('btnCloseDev');

        if (btnDev && modalDev) {
            btnDev.addEventListener('click', () => {
                window.spatialNav.lastHomeFocus = btnDev;
                modalDev.classList.add('active');
                if (btnCloseDev && window.spatialNav) {
                    window.spatialNav.setFocus(btnCloseDev);
                }
            });
        }

        if (btnCloseDev && modalDev) {
            btnCloseDev.addEventListener('click', () => {
                modalDev.classList.remove('active');
                if (window.spatialNav?.lastHomeFocus) {
                    window.spatialNav.setFocus(window.spatialNav.lastHomeFocus);
                }
            });
        }

        // Theme Switcher
        const btnTheme = document.getElementById('btnTheme');
        const themes = [
            { name: 'رمال مذهّب', bg: '#0F172A', accent: '#F59E0B' },
            { name: 'زمرد سائل', bg: '#064E3B', accent: '#10B981' },
            { name: 'داكن هادئ', bg: '#090D16', accent: '#38BDF8' },
            { name: 'أزرق ملكي', bg: '#1E1B4B', accent: '#818CF8' }
        ];
        let currentThemeIdx = 0;

        if (btnTheme) {
            btnTheme.addEventListener('click', () => {
                currentThemeIdx = (currentThemeIdx + 1) % themes.length;
                const t = themes[currentThemeIdx];
                document.documentElement.style.setProperty('--bg-primary', t.bg);
                document.documentElement.style.setProperty('--accent-gold', t.accent);
                document.documentElement.style.setProperty('--glass-border-focus', t.accent);
                btnTheme.querySelector('.pill-text').textContent = t.name;
            });
        }
    }

    async loadData() {
        // 1. Load Channels with Cloud-First Auto-Update architecture
        let rawChannels = null;
        try {
            // Attempt to fetch from GitHub raw first (for automatic updates across platforms)
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 2500);
            const cloudRes = await fetch(this.GITHUB_CHANNELS_URL, { signal: controller.signal });
            clearTimeout(timeoutId);
            if (cloudRes.ok) {
                rawChannels = await cloudRes.json();
                localStorage.setItem('mishkat_channels_cache', JSON.stringify(rawChannels));
                console.log('Loaded channels dynamically from GitHub repository');
            }
        } catch(e) {
            console.log('GitHub fetch skipped/timed out, using local fallback');
        }

        if (!rawChannels) {
            const cached = localStorage.getItem('mishkat_channels_cache');
            if (cached) {
                try { rawChannels = JSON.parse(cached); } catch(e) {}
            }
        }

        if (!rawChannels) {
            try {
                const localRes = await fetch('../shared/channels.json');
                if (localRes.ok) rawChannels = await localRes.json();
            } catch(e) {
                try {
                    const fallbackRes = await fetch('shared/channels.json');
                    if (fallbackRes.ok) rawChannels = await fallbackRes.json();
                } catch(err) {}
            }
        }

        this.channels = (rawChannels || []).map(ch => ({
            id: ch.id,
            name: ch.nameAr || ch.name,
            nameEn: ch.nameEn || '',
            category: ch.category,
            subtitle: ch.subtitleAr || '',
            url: ch.streamUrl || ch.url,
            glyph: ch.glyph || '📺',
            badge: ch.badge || 'HD'
        }));

        this.filteredChannels = [...this.channels];
        if (window.TvPlayer) {
            window.TvPlayer.setChannels(this.channels);
        }

        // 2. Load Radio Stations
        try {
            const stRes = await fetch('../shared/stations.json');
            if (stRes.ok) {
                this.stations = await stRes.json();
            }
        } catch(e) {
            try {
                const stFallback = await fetch('shared/stations.json');
                if (stFallback.ok) this.stations = await stFallback.json();
            } catch(err) {}
        }

        if (this.stations.length > 0) {
            this.updateRadioUI();
        }
    }

    renderCategories() {
        const row = document.getElementById('categoriesRow');
        if (!row) return;
        row.innerHTML = '';

        const catDefinitions = [
            { id: 'all', title: 'كل القنوات' },
            { id: 'islamic_quran', title: 'القرآن والإسلامية' },
            { id: 'kids_education', title: 'التعليم والأطفال' },
            { id: 'drama_series', title: 'المسلسلات والدراما' },
            { id: 'cinema_movies', title: 'السينما والأفلام' },
            { id: 'egypt_general', title: 'القنوات المصرية' },
            { id: 'news_doc', title: 'الأخبار والوثائقيات' },
            { id: 'sports_cooking', title: 'الرياضة والطبخ' }
        ];

        catDefinitions.forEach(cat => {
            const btn = document.createElement('button');
            btn.className = `focusable cat-pill ${this.activeCategory === cat.id ? 'active' : ''}`;
            btn.tabIndex = 0;
            btn.textContent = cat.title;
            btn.dataset.category = cat.id;

            btn.addEventListener('click', () => {
                this.activeCategory = cat.id;
                document.querySelectorAll('.cat-pill').forEach(p => p.classList.remove('active'));
                btn.classList.add('active');
                
                if (cat.id === 'all') {
                    this.filteredChannels = [...this.channels];
                } else {
                    this.filteredChannels = this.channels.filter(c => c.category === cat.id);
                }

                this.renderChannels();
            });

            row.appendChild(btn);
        });
    }

    renderChannels() {
        const row = document.getElementById('channelsRow');
        const badgeEl = document.getElementById('channelCountBadge');
        if (!row) return;
        row.innerHTML = '';

        if (badgeEl) {
            badgeEl.textContent = `${this.filteredChannels.length} قنوات متاحة`;
        }

        this.filteredChannels.forEach((ch, idx) => {
            const card = document.createElement('div');
            card.className = 'focusable channel-card';
            card.tabIndex = 0;
            card.dataset.index = idx;
            card.innerHTML = `
                <div class="card-top">
                    <span class="channel-glyph">${ch.glyph}</span>
                    <span class="card-badge ${ch.badge.includes('LIVE') ? 'badge-live' : ''}">${ch.badge}</span>
                </div>
                <div class="card-bottom">
                    <div class="channel-title">${ch.name}</div>
                    <div class="channel-sub">${ch.subtitle}</div>
                </div>
            `;

            card.addEventListener('click', () => {
                this.lastChannelFocused = card;
                const globalIndex = this.channels.findIndex(c => c.id === ch.id);
                if (window.TvPlayer) {
                    window.TvPlayer.playChannel(ch, globalIndex >= 0 ? globalIndex : idx);
                }
            });

            row.appendChild(card);
        });
    }

    setupRadioEvents() {
        const btnPlay = document.getElementById('btnRadioPlay');
        const btnNext = document.getElementById('btnRadioNext');
        const btnPrev = document.getElementById('btnRadioPrev');
        const btnSleep = document.getElementById('btnSleepTimer');

        if (btnPlay) {
            btnPlay.addEventListener('click', () => this.toggleRadio());
        }

        if (btnNext) {
            btnNext.addEventListener('click', () => {
                if (!this.stations.length) return;
                this.currentStationIndex = (this.currentStationIndex + 1) % this.stations.length;
                this.updateRadioUI();
                if (this.isRadioPlaying) this.startRadio();
            });
        }

        if (btnPrev) {
            btnPrev.addEventListener('click', () => {
                if (!this.stations.length) return;
                this.currentStationIndex = (this.currentStationIndex - 1 + this.stations.length) % this.stations.length;
                this.updateRadioUI();
                if (this.isRadioPlaying) this.startRadio();
            });
        }

        if (btnSleep) {
            btnSleep.addEventListener('click', () => {
                const sleepIntervals = [0, 15, 30, 45, 60];
                const currentIdx = sleepIntervals.indexOf(this.sleepMinutes);
                this.sleepMinutes = sleepIntervals[(currentIdx + 1) % sleepIntervals.length];

                clearTimeout(this.sleepTimerId);
                if (this.sleepMinutes > 0) {
                    btnSleep.textContent = `مؤقت: ${this.sleepMinutes} د`;
                    this.sleepTimerId = setTimeout(() => {
                        this.stopRadio();
                        btnSleep.textContent = 'مؤقت النوم';
                        this.sleepMinutes = 0;
                    }, this.sleepMinutes * 60 * 1000);
                } else {
                    btnSleep.textContent = 'مؤقت النوم';
                }
            });
        }
    }

    updateRadioUI() {
        if (!this.stations.length) return;
        const current = this.stations[this.currentStationIndex];
        const titleEl = document.getElementById('currentStationTitle');
        const subEl = document.getElementById('currentReciterTitle');

        if (titleEl) titleEl.textContent = current.nameAr || current.name;
        if (subEl) subEl.textContent = current.nameEn || 'بث مباشر عالي النقاء';
    }

    toggleRadio() {
        if (this.isRadioPlaying) {
            this.stopRadio();
        } else {
            this.startRadio();
        }
    }

    startRadio() {
        if (!this.stations.length) return;
        const st = this.stations[this.currentStationIndex];
        this.radioAudio.src = st.url;
        this.radioAudio.play().then(() => {
            this.isRadioPlaying = true;
            const btnPlay = document.getElementById('btnRadioPlay');
            if (btnPlay) {
                btnPlay.textContent = 'إيقاف';
                btnPlay.style.background = '#EF4444';
            }
        }).catch(err => {
            console.warn('Radio audio play error:', err);
        });
    }

    stopRadio() {
        this.radioAudio.pause();
        this.isRadioPlaying = false;
        const btnPlay = document.getElementById('btnRadioPlay');
        if (btnPlay) {
            btnPlay.textContent = 'تشغيل';
            btnPlay.style.background = 'var(--accent-gold)';
        }
    }

    setupAppShortcuts() {
        document.querySelectorAll('.app-card').forEach(card => {
            card.addEventListener('click', () => {
                const appId = card.dataset.app;
                this.launchExternalApp(appId);
            });
        });
    }

    launchExternalApp(appId) {
        // Platform specific app launcher
        if (typeof tizen !== 'undefined') {
            const tizenAppMap = {
                youtube: '9Ur5tN0cea.YouTube',
                iptv: 'org.tizen.browser',
                browser: 'org.tizen.browser'
            };
            const pkg = tizenAppMap[appId];
            if (pkg) {
                try {
                    tizen.application.launch(pkg);
                    return;
                } catch(e) {}
            }
        } else if (typeof webOS !== 'undefined' && window.webOS && webOS.service) {
            const webosAppMap = {
                youtube: 'youtube.leanback.v4',
                browser: 'com.webos.app.browser'
            };
            const id = webosAppMap[appId];
            if (id) {
                webOS.service.request('luna://com.webos.applicationManager', {
                    method: 'launch',
                    parameters: { id: id }
                });
                return;
            }
        }

        // Web Fallback
        const urls = {
            youtube: 'https://www.youtube.com/tv',
            downloader: 'https://www.google.com',
            browser: 'https://www.google.com'
        };
        if (urls[appId]) {
            window.open(urls[appId], '_blank');
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.app = new MishkatApp();
});
