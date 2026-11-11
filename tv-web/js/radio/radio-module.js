/**
 * Mishkat Smart TV - Quran Radio & Reciters Encyclopedia
 * Mirrors Android: RadioUi.kt, StationRepository.kt, SleepTimerRepository.kt
 * Pure ES6 (Chromium 76 Safe)
 */

(function(window) {
    'use strict';

    function RadioModule() {
        this.stations = [];
        this.filteredStations = [];
        this.currentIndex = 0;
        this.audio = new Audio();
        this.isPlaying = false;

        this.sleepTimer = null;
        this.sleepTimerMinutes = 0;
        this.sleepInterval = null;

        this.searchQuery = '';
        this.activeLetter = 'all';

        this.init();
    }

    RadioModule.prototype.init = function() {
        var self = this;
        var data = window.MISHKAT_DATA || {};
        this.stations = data.stations || [];
        this.filteredStations = this.stations.slice();

        this.bindHomeScreenControls();
        this.setupRecitersModal();
        this.setupSleepTimerModal();

        this.audio.addEventListener('play', function() {
            self.isPlaying = true;
            self.updatePlayStateUI();
        });

        this.audio.addEventListener('pause', function() {
            self.isPlaying = false;
            self.updatePlayStateUI();
        });

        this.audio.addEventListener('error', function(err) {
            console.warn('Radio audio error:', err);
            self.isPlaying = false;
            self.updatePlayStateUI();
        });

        this.updateStationInfoUI();
    };

    RadioModule.prototype.bindHomeScreenControls = function() {
        var self = this;
        var playBtn = document.getElementById('btnRadioPlay');
        var prevBtn = document.getElementById('btnRadioPrev');
        var nextBtn = document.getElementById('btnRadioNext');
        var chooseBtn = document.getElementById('btnChooseReciter');

        if (playBtn) playBtn.addEventListener('click', function() { self.togglePlay(); });
        if (prevBtn) prevBtn.addEventListener('click', function() { self.prev(); });
        if (nextBtn) nextBtn.addEventListener('click', function() { self.next(); });
        if (chooseBtn) chooseBtn.addEventListener('click', function() { self.openRecitersModal(); });
    };

    RadioModule.prototype.togglePlay = function() {
        if (!this.stations.length) return;
        if (this.isPlaying) {
            this.audio.pause();
        } else {
            this.playStation(this.currentIndex);
        }
    };

    RadioModule.prototype.playStation = function(index) {
        if (!this.stations.length) return;
        this.currentIndex = index;
        var st = this.stations[index];
        this.audio.src = st.url;
        this.audio.play().catch(function(e) {
            console.warn('Radio play exception:', e);
        });
        this.updateStationInfoUI();
    };

    RadioModule.prototype.next = function() {
        if (!this.stations.length) return;
        this.currentIndex = (this.currentIndex + 1) % this.stations.length;
        if (this.isPlaying) {
            this.playStation(this.currentIndex);
        } else {
            this.updateStationInfoUI();
        }
    };

    RadioModule.prototype.prev = function() {
        if (!this.stations.length) return;
        this.currentIndex = (this.currentIndex - 1 + this.stations.length) % this.stations.length;
        if (this.isPlaying) {
            this.playStation(this.currentIndex);
        } else {
            this.updateStationInfoUI();
        }
    };

    RadioModule.prototype.updateStationInfoUI = function() {
        if (!this.stations.length) return;
        var st = this.stations[this.currentIndex];
        var titleEl = document.getElementById('currentStationTitle');
        var reciterEl = document.getElementById('currentReciterTitle');
        if (titleEl) titleEl.textContent = st.nameAr || st.name;
        if (reciterEl) reciterEl.textContent = st.reciterAr || st.nameEn || 'إذاعة القرآن الكريم المباشرة';
    };

    RadioModule.prototype.updatePlayStateUI = function() {
        var playIcon = document.getElementById('radioPlayIcon');
        if (playIcon) {
            playIcon.textContent = this.isPlaying ? '⏸ إيقاف' : '▶ تشغيل';
        }
    };

    RadioModule.prototype.openRecitersModal = function() {
        var modal = document.getElementById('recitersModal');
        if (modal && window.SpatialNav) {
            window.SpatialNav.openModal(modal, '#recitersSearchInput');
        }
    };

    RadioModule.prototype.setupRecitersModal = function() {
        var self = this;
        var searchInput = document.getElementById('recitersSearchInput');
        var alphabetContainer = document.getElementById('recitersAlphabetBar');

        if (searchInput) {
            searchInput.addEventListener('input', function(e) {
                self.searchQuery = e.target.value.trim().toLowerCase();
                self.filterStations();
            });
        }

        if (alphabetContainer) {
            alphabetContainer.innerHTML = '';
            var letters = ['الكل', 'أ', 'ب', 'ت', 'ث', 'ج', 'ح', 'خ', 'د', 'ذ', 'ر', 'ز', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ع', 'غ', 'ف', 'ق', 'ك', 'ل', 'م', 'ن', 'هـ', 'و', 'ي'];
            for (var i = 0; i < letters.length; i++) {
                (function(l) {
                    var btn = document.createElement('button');
                    btn.className = 'focusable alphabet-pill' + (self.activeLetter === l ? ' active' : '');
                    btn.tabIndex = 0;
                    btn.textContent = l;
                    btn.addEventListener('click', function() {
                        var pills = alphabetContainer.querySelectorAll('.alphabet-pill');
                        for (var p = 0; p < pills.length; p++) pills[p].classList.remove('active');
                        btn.classList.add('active');
                        self.activeLetter = (l === 'الكل') ? 'all' : l;
                        self.filterStations();
                    });
                    alphabetContainer.appendChild(btn);
                })(letters[i]);
            }
        }

        this.renderRecitersGrid();
    };

    RadioModule.prototype.filterStations = function() {
        var self = this;
        this.filteredStations = [];

        for (var i = 0; i < this.stations.length; i++) {
            var st = this.stations[i];
            var name = (st.nameAr || st.name || '').toLowerCase();
            var reciter = (st.reciterAr || st.nameEn || '').toLowerCase();

            var matchSearch = !self.searchQuery || name.indexOf(self.searchQuery) !== -1 || reciter.indexOf(self.searchQuery) !== -1;
            var matchLetter = (self.activeLetter === 'all') || (name.indexOf(self.activeLetter) === 0);

            if (matchSearch && matchLetter) {
                self.filteredStations.push(st);
            }
        }

        this.renderRecitersGrid();
    };

    RadioModule.prototype.renderRecitersGrid = function() {
        var self = this;
        var grid = document.getElementById('recitersGrid');
        if (!grid) return;
        grid.innerHTML = '';

        for (var i = 0; i < this.filteredStations.length; i++) {
            (function(st, idx) {
                var card = document.createElement('div');
                card.className = 'focusable reciter-card';
                card.tabIndex = 0;
                card.innerHTML = 
                    '<span class="reciter-card-icon">📖</span>' +
                    '<div>' +
                        '<div class="reciter-card-name">' + (st.nameAr || st.name) + '</div>' +
                        '<div style="font-size: 13px; color: #94A3B8;">' + (st.reciterAr || st.nameEn || 'إذاعة مباشرة') + '</div>' +
                    '</div>';

                card.addEventListener('click', function() {
                    // Find actual index in this.stations
                    var originalIdx = 0;
                    for (var j = 0; j < self.stations.length; j++) {
                        if (self.stations[j].url === st.url) {
                            originalIdx = j;
                            break;
                        }
                    }
                    self.playStation(originalIdx);
                    if (window.SpatialNav) {
                        window.SpatialNav.closeTopModal();
                    }
                });

                grid.appendChild(card);
            })(this.filteredStations[i], i);
        }
    };

    RadioModule.prototype.setupSleepTimerModal = function() {
        var self = this;
        var sleepButtons = document.querySelectorAll('.sleep-timer-btn');
        for (var i = 0; i < sleepButtons.length; i++) {
            (function(btn) {
                btn.addEventListener('click', function() {
                    var minutes = parseInt(btn.getAttribute('data-minutes'), 10) || 0;
                    self.setSleepTimer(minutes);
                    if (window.SpatialNav) window.SpatialNav.closeTopModal();
                });
            })(sleepButtons[i]);
        }
    };

    RadioModule.prototype.setSleepTimer = function(minutes) {
        var self = this;
        if (this.sleepTimer) clearTimeout(this.sleepTimer);
        if (this.sleepInterval) clearInterval(this.sleepInterval);

        this.sleepTimerMinutes = minutes;
        var indicator = document.getElementById('sleepTimerIndicator');

        if (minutes <= 0) {
            if (indicator) indicator.textContent = '';
            console.log('Sleep timer disabled');
            return;
        }

        var totalSeconds = minutes * 60;
        this.sleepInterval = setInterval(function() {
            totalSeconds--;
            if (totalSeconds <= 0) {
                clearInterval(self.sleepInterval);
                self.audio.pause();
                if (indicator) indicator.textContent = '';
            } else {
                var m = Math.floor(totalSeconds / 60);
                var s = totalSeconds % 60;
                if (indicator) indicator.textContent = '⏱ مؤقت النوم: ' + m + ':' + (s < 10 ? '0' : '') + s;

                // Fade volume in last 2 minutes
                if (totalSeconds <= 120) {
                    self.audio.volume = Math.max(0.05, totalSeconds / 120);
                }
            }
        }, 1000);

        console.log('Sleep timer set to', minutes, 'minutes');
    };

    window.RadioModule = new RadioModule();

})(window);
