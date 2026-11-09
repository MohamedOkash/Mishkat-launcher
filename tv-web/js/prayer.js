/**
 * Mishkat Smart TV - Prayer & Adhkar Engine
 * Official Egyptian General Authority of Survey (الهيئة المصرية العامة للمساحة)
 */

class PrayerEngine {
    constructor() {
        // Cairo default coordinates
        this.coords = { lat: 30.0444, lng: 31.2357, tz: 2 };
        this.times = {};
        this.adhkarList = [];
        this.adhkarIndex = 0;
        this.tasbeehCount = 0;
        this.tasbeehTarget = 33;
        
        this.init();
    }

    async init() {
        this.calculateTimes();
        this.renderTimes();
        this.startCountdown();
        await this.loadAdhkar();
        this.setupAdhkarClick();
    }

    calculateTimes() {
        const now = new Date();
        const d = now.getDate();
        const m = now.getMonth() + 1;
        const y = now.getFullYear();

        // Julian Date
        const a = Math.floor((14 - m) / 12);
        const y2 = y + 4800 - a;
        const m2 = m + 12 * a - 3;
        const jd = d + Math.floor((153 * m2 + 2) / 5) + 365 * y2 + Math.floor(y2 / 4) - Math.floor(y2 / 100) + Math.floor(y2 / 400) - 32045;

        const d_julian = jd - 2451545.0;
        const g = (357.529 + 0.98560028 * d_julian) % 360;
        const q = (280.459 + 0.98564736 * d_julian) % 360;
        const L = (q + 1.915 * Math.sin(this.deg2rad(g)) + 0.020 * Math.sin(this.deg2rad(2 * g))) % 360;
        const e = 23.439 - 0.00000036 * d_julian;
        const RA = (this.rad2deg(Math.atan2(Math.cos(this.deg2rad(e)) * Math.sin(this.deg2rad(L)), Math.cos(this.deg2rad(L))))) / 15;
        const D = this.rad2deg(Math.asin(Math.sin(this.deg2rad(e)) * Math.sin(this.deg2rad(L))));
        const EqT = q / 15 - (RA < 0 ? RA + 24 : RA);

        // Solar Noon
        const noon = 12 + this.coords.tz - (this.coords.lng / 15) - EqT;

        // Egyptian Survey Method: Fajr 19.5 deg, Isha 17.5 deg
        const fajrDiff = (1 / 15) * this.rad2deg(Math.acos((-Math.sin(this.deg2rad(19.5)) - Math.sin(this.deg2rad(this.coords.lat)) * Math.sin(this.deg2rad(D))) / (Math.cos(this.deg2rad(this.coords.lat)) * Math.cos(this.deg2rad(D)))));
        const sunriseDiff = (1 / 15) * this.rad2deg(Math.acos((-Math.sin(this.deg2rad(0.833)) - Math.sin(this.deg2rad(this.coords.lat)) * Math.sin(this.deg2rad(D))) / (Math.cos(this.deg2rad(this.coords.lat)) * Math.cos(this.deg2rad(D)))));
        const ishaDiff = (1 / 15) * this.rad2deg(Math.acos((-Math.sin(this.deg2rad(17.5)) - Math.sin(this.deg2rad(this.coords.lat)) * Math.sin(this.deg2rad(D))) / (Math.cos(this.deg2rad(this.coords.lat)) * Math.cos(this.deg2rad(D)))));
        
        // Asr (Shafi'i shadow = 1)
        const asrAngle = -this.rad2deg(Math.atan(1 + Math.tan(this.deg2rad(Math.abs(this.coords.lat - D)))));
        const asrDiff = (1 / 15) * this.rad2deg(Math.acos((Math.sin(this.deg2rad(asrAngle)) - Math.sin(this.deg2rad(this.coords.lat)) * Math.sin(this.deg2rad(D))) / (Math.cos(this.deg2rad(this.coords.lat)) * Math.cos(this.deg2rad(D)))));

        this.times = {
            fajr: this.hoursToDate(now, noon - fajrDiff),
            dhuhr: this.hoursToDate(now, noon),
            asr: this.hoursToDate(now, noon + asrDiff),
            maghrib: this.hoursToDate(now, noon + sunriseDiff),
            isha: this.hoursToDate(now, noon + ishaDiff)
        };
    }

    deg2rad(deg) { return (deg * Math.PI) / 180.0; }
    rad2deg(rad) { return (rad * 180.0) / Math.PI; }

    hoursToDate(baseDate, hoursDec) {
        const h = Math.floor(hoursDec);
        const m = Math.floor((hoursDec - h) * 60);
        const s = Math.floor(((hoursDec - h) * 60 - m) * 60);
        const d = new Date(baseDate);
        d.setHours(h, m, s, 0);
        return d;
    }

    formatTime(date) {
        let h = date.getHours();
        const m = date.getMinutes().toString().padStart(2, '0');
        const period = h >= 12 ? 'م' : 'ص';
        h = h % 12 || 12;
        return `${h.toString().padStart(2, '0')}:${m} ${period}`;
    }

    renderTimes() {
        if (document.getElementById('timeFajr')) document.getElementById('timeFajr').textContent = this.formatTime(this.times.fajr);
        if (document.getElementById('timeDhuhr')) document.getElementById('timeDhuhr').textContent = this.formatTime(this.times.dhuhr);
        if (document.getElementById('timeAsr')) document.getElementById('timeAsr').textContent = this.formatTime(this.times.asr);
        if (document.getElementById('timeMaghrib')) document.getElementById('timeMaghrib').textContent = this.formatTime(this.times.maghrib);
        if (document.getElementById('timeIsha')) document.getElementById('timeIsha').textContent = this.formatTime(this.times.isha);
    }

    startCountdown() {
        const update = () => {
            const now = new Date();
            const prayerOrder = [
                { name: 'الفجر', slot: 'slotFajr', time: this.times.fajr },
                { name: 'الظهر', slot: 'slotDhuhr', time: this.times.dhuhr },
                { name: 'العصر', slot: 'slotAsr', time: this.times.asr },
                { name: 'المغرب', slot: 'slotMaghrib', time: this.times.maghrib },
                { name: 'العشاء', slot: 'slotIsha', time: this.times.isha }
            ];

            let nextPrayer = null;
            let activePrayer = null;

            for (let i = 0; i < prayerOrder.length; i++) {
                if (now < prayerOrder[i].time) {
                    nextPrayer = prayerOrder[i];
                    activePrayer = prayerOrder[(i - 1 + prayerOrder.length) % prayerOrder.length];
                    break;
                }
            }

            if (!nextPrayer) {
                // Next is tomorrow's Fajr
                nextPrayer = { name: 'الفجر', slot: 'slotFajr', time: new Date(this.times.fajr.getTime() + 24 * 3600 * 1000) };
                activePrayer = prayerOrder[prayerOrder.length - 1];
            }

            // Highlight active slot
            document.querySelectorAll('.prayer-slot').forEach(s => s.classList.remove('active'));
            if (activePrayer) {
                const el = document.getElementById(activePrayer.slot);
                if (el) el.classList.add('active');
            }

            // Countdown string
            const diffMs = nextPrayer.time - now;
            const diffSec = Math.max(0, Math.floor(diffMs / 1000));
            const hrs = Math.floor(diffSec / 3600).toString().padStart(2, '0');
            const mins = Math.floor((diffSec % 3600) / 60).toString().padStart(2, '0');
            const secs = (diffSec % 60).toString().padStart(2, '0');

            const bannerLabel = document.querySelector('.next-label');
            const countdownEl = document.getElementById('prayerCountdown');

            if (bannerLabel) bannerLabel.textContent = `الصلاة القادمة: ${nextPrayer.name} بعد`;
            if (countdownEl) countdownEl.textContent = `${hrs}:${mins}:${secs}`;
        };

        update();
        setInterval(update, 1000);
    }

    async loadAdhkar() {
        try {
            const res = await fetch('../shared/adhkar.json');
            if (res.ok) {
                const data = await res.json();
                this.adhkarList = data.adhkar || [];
                this.displayCurrentDhikr();
            }
        } catch(e) {
            this.adhkarList = [
                { category: "أذكار الصباح", text: "«أَصْبَحْنَا وَأَصْبَحَ المُلْكُ لِلَّهِ، وَالحَمْدُ لِلَّهِ لَا إِلَهَ إِلَّا اللَّهُ»", repeat: 33 },
                { category: "تسبيح", text: "«سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ العَظِيمِ»", repeat: 33 },
                { category: "استغفار", text: "«أَسْتَغْفِرُ اللَّهَ العَظِيمَ الَّذِي لَا إِلَهَ إِلَّا هُوَ الحَيُّ القَيُّومُ وَأَتُوبُ إِلَيْهِ»", repeat: 33 }
            ];
            this.displayCurrentDhikr();
        }
    }

    displayCurrentDhikr() {
        if (!this.adhkarList.length) return;
        const current = this.adhkarList[this.adhkarIndex % this.adhkarList.length];
        this.tasbeehTarget = current.repeat || 33;
        
        const tag = document.querySelector('.adhkar-tag');
        const text = document.getElementById('adhkarMainText');
        const count = document.getElementById('tasbeehCounter');

        if (tag) tag.textContent = `${current.category || 'أذكار'} • اضغط OK للتسبيح`;
        if (text) text.textContent = current.text;
        if (count) count.textContent = `${this.tasbeehCount} / ${this.tasbeehTarget}`;
    }

    setupAdhkarClick() {
        const btn = document.getElementById('btnAdhkar');
        if (btn) {
            btn.addEventListener('click', () => {
                this.tasbeehCount++;
                if (this.tasbeehCount >= this.tasbeehTarget) {
                    this.tasbeehCount = 0;
                    this.adhkarIndex = (this.adhkarIndex + 1) % this.adhkarList.length;
                }
                this.displayCurrentDhikr();

                // Apple TV spring tactile bounce
                btn.style.transform = 'scale(0.96)';
                setTimeout(() => { btn.style.transform = ''; }, 120);
            });
        }
    }
}

window.prayerEngine = new PrayerEngine();
