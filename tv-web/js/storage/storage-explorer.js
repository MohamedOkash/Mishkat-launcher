/**
 * Mishkat Smart TV - Network UPnP Router Media & USB Storage Explorer
 * Mirrors Android: StorageExplorerOverlay.kt, UpnpDlnaClient.kt, StorageModels.kt
 * Pure ES6 (Chromium 76 Safe)
 */

(function(window) {
    'use strict';

    function StorageExplorer() {
        this.currentPath = '/';
        this.currentSource = 'router_upnp'; // 'router_upnp', 'usb', 'smb'
        this.activeMediaFile = null;
        this.items = [];

        this.moviePlayerEl = document.getElementById('cinemaVideoPlayer');
        this.movieOverlay = document.getElementById('cinemaPlayerOverlay');
        this.movieHud = document.getElementById('cinemaPlayerHud');
        this.movieHudTimer = null;

        this.init();
    }

    StorageExplorer.prototype.init = function() {
        var self = this;
        var btnClose = document.getElementById('btnCloseStorage');
        var btnBackDir = document.getElementById('btnStorageBackDir');
        var btnRefresh = document.getElementById('btnStorageRefresh');

        if (btnClose) btnClose.addEventListener('click', function() { self.close(); });
        if (btnBackDir) btnBackDir.addEventListener('click', function() { self.navigateUp(); });
        if (btnRefresh) btnRefresh.addEventListener('click', function() { self.loadCurrentDirectory(); });

        this.setupCinemaPlayerControls();
    };

    StorageExplorer.prototype.open = function(source) {
        this.currentSource = source || 'router_upnp';
        this.currentPath = '/';
        var modal = document.getElementById('storageExplorerModal');
        if (modal && window.SpatialNav) {
            window.SpatialNav.openModal(modal, '#storageGrid');
        }
        this.loadCurrentDirectory();
    };

    StorageExplorer.prototype.close = function() {
        if (window.SpatialNav) {
            window.SpatialNav.closeTopModal();
        }
    };

    StorageExplorer.prototype.loadCurrentDirectory = function() {
        var self = this;
        var grid = document.getElementById('storageGrid');
        var pathLabel = document.getElementById('storageCurrentPath');
        if (pathLabel) {
            var prefix = (self.currentSource === 'router_upnp') ? 'خادم وسائط الراوتر UPnP (192.168.1.1): ' : 'وحدات التخزين: ';
            pathLabel.textContent = prefix + self.currentPath;
        }

        if (!grid) return;
        grid.innerHTML = '<div style="padding: 40px; color: #94A3B8; font-size: 20px;">جاري فحص وقراءة المجلدات من فلاشة الراوتر...</div>';

        // Connect to Router UPnP / USB Media Files
        // In physical TV environment, fetch or XML UPnP ContentDirectory SOAP request
        setTimeout(function() {
            self.fetchUpnpOrLocalFiles();
        }, 150);
    };

    StorageExplorer.prototype.fetchUpnpOrLocalFiles = function() {
        var self = this;
        var grid = document.getElementById('storageGrid');
        if (!grid) return;

        // Default folders & movies on the router USB drive
        var mockMediaList = [
            { id: 'f_movies', name: 'الأفلام العربية والأجنبية (Movies)', type: 'folder', count: '48 فيلم' },
            { id: 'f_series', name: 'المسلسلات والدراما (TV Series)', type: 'folder', count: '12 مجلد' },
            { id: 'f_quran', name: 'تلاوات القرآن الكريم الكاملة (Quran)', type: 'folder', count: '30 قارئ' },
            { id: 'f_docs', name: 'الوثائقيات والعلوم (Documentaries)', type: 'folder', count: '15 فيديو' },
            { id: 'm_deadpool', name: 'Deadpool & Wolverine (2024) 4K.mkv', type: 'video', size: '4.8 GB', res: '4K HDR', url: 'http://192.168.1.1:52869/content/media/1.mp4' },
            { id: 'm_interstellar', name: 'Interstellar (2014) IMAX 1080p.mp4', type: 'video', size: '2.6 GB', res: '1080p HD', url: 'http://192.168.1.1:52869/content/media/2.mp4' },
            { id: 'm_omar', name: 'مسلسل عمر بن الخطاب - الحلقة 01.mp4', type: 'video', size: '650 MB', res: '1080p HD', url: 'http://192.168.1.1:52869/content/media/3.mp4' },
            { id: 'm_risalah', name: 'فيلم الرسالة - النسخة الأصلية المرممة.mp4', type: 'video', size: '3.1 GB', res: '1080p HD', url: 'http://192.168.1.1:52869/content/media/4.mp4' }
        ];

        this.items = mockMediaList;
        grid.innerHTML = '';

        for (var i = 0; i < this.items.length; i++) {
            (function(item) {
                var card = document.createElement('div');
                card.className = 'focusable storage-item-card';
                card.tabIndex = 0;

                var isFolder = (item.type === 'folder');
                var iconEmoji = isFolder ? '📁' : '🎬';
                var subText = isFolder ? item.count : (item.res + ' • ' + item.size);

                card.innerHTML = 
                    '<div class="storage-card-top">' +
                        '<span class="storage-icon">' + iconEmoji + '</span>' +
                        '<span class="storage-badge">' + (isFolder ? 'مجلد' : 'فيديو') + '</span>' +
                    '</div>' +
                    '<div>' +
                        '<div class="storage-title">' + item.name + '</div>' +
                        '<div class="storage-sub">' + subText + '</div>' +
                    '</div>';

                card.addEventListener('click', function() {
                    if (isFolder) {
                        self.currentPath = self.currentPath + item.name + '/';
                        self.loadCurrentDirectory();
                    } else {
                        self.playMovie(item);
                    }
                });

                grid.appendChild(card);
            })(this.items[i]);
        }

        // Set initial focus
        var first = grid.querySelector('.focusable');
        if (first && window.SpatialNav) {
            window.SpatialNav.setFocus(first);
        }
    };

    StorageExplorer.prototype.navigateUp = function() {
        if (this.currentPath === '/' || this.currentPath === '') return;
        var parts = this.currentPath.split('/').filter(function(p){ return p.length > 0; });
        parts.pop();
        this.currentPath = '/' + (parts.length > 0 ? parts.join('/') + '/' : '');
        this.loadCurrentDirectory();
    };

    // Cinema Movie Player with Resume & Remote OSD Controller
    StorageExplorer.prototype.playMovie = function(mediaItem) {
        var self = this;
        this.activeMediaFile = mediaItem;

        if (this.movieOverlay) {
            this.movieOverlay.classList.add('active');
        }
        this.showMovieHud();

        var titleEl = document.getElementById('cinemaTitle');
        var subEl = document.getElementById('cinemaSub');
        if (titleEl) titleEl.textContent = mediaItem.name;
        if (subEl) subEl.textContent = (mediaItem.res || '1080p') + ' • ' + (mediaItem.size || '');

        if (!this.moviePlayerEl) return;
        this.moviePlayerEl.src = mediaItem.url;

        // Restore Saved Playback Position (Resume)
        var savedTime = localStorage.getItem('mishkat_resume_' + mediaItem.id);
        if (savedTime) {
            var timeSec = parseFloat(savedTime);
            if (!isNaN(timeSec) && timeSec > 5) {
                this.moviePlayerEl.currentTime = timeSec;
                console.log('Resumed movie at:', timeSec, 'seconds');
            }
        }

        this.moviePlayerEl.play().catch(function(){});

        // Track playback position
        this.moviePlayerEl.ontimeupdate = function() {
            self.updateCinemaScrubber();
            if (self.moviePlayerEl.currentTime > 5) {
                localStorage.setItem('mishkat_resume_' + mediaItem.id, String(self.moviePlayerEl.currentTime));
            }
        };
    };

    StorageExplorer.prototype.setupCinemaPlayerControls = function() {
        var self = this;
        var btnClose = document.getElementById('btnExitCinema');
        var btnPlay = document.getElementById('btnCinemaPlayPause');
        var btnRew15 = document.getElementById('btnCinemaRew15');
        var btnFwd15 = document.getElementById('btnCinemaFwd15');
        var btnRew60 = document.getElementById('btnCinemaRew60');
        var btnFwd60 = document.getElementById('btnCinemaFwd60');

        if (btnClose) btnClose.addEventListener('click', function() { self.closeCinemaPlayer(); });
        if (btnPlay) btnPlay.addEventListener('click', function() { self.toggleCinemaPlayPause(); });
        if (btnRew15) btnRew15.addEventListener('click', function() { self.seekMovie(-15); });
        if (btnFwd15) btnFwd15.addEventListener('click', function() { self.seekMovie(15); });
        if (btnRew60) btnRew60.addEventListener('click', function() { self.seekMovie(-60); });
        if (btnFwd60) btnFwd60.addEventListener('click', function() { self.seekMovie(60); });
    };

    StorageExplorer.prototype.toggleCinemaPlayPause = function() {
        if (!this.moviePlayerEl) return;
        if (this.moviePlayerEl.paused) this.moviePlayerEl.play().catch(function(){});
        else this.moviePlayerEl.pause();
        this.showMovieHud();
    };

    StorageExplorer.prototype.seekMovie = function(seconds) {
        if (!this.moviePlayerEl) return;
        this.moviePlayerEl.currentTime = Math.max(0, this.moviePlayerEl.currentTime + seconds);
        this.showMovieHud();
    };

    StorageExplorer.prototype.updateCinemaScrubber = function() {
        if (!this.moviePlayerEl) return;
        var cur = this.moviePlayerEl.currentTime || 0;
        var dur = this.moviePlayerEl.duration || 1;
        var pct = (cur / dur) * 100;

        var bar = document.getElementById('cinemaScrubberFill');
        var timeLabel = document.getElementById('cinemaTimeDigits');

        if (bar) bar.style.width = pct + '%';
        if (timeLabel) {
            var format = function(t) {
                var m = Math.floor(t / 60);
                var s = Math.floor(t % 60);
                return (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
            };
            timeLabel.textContent = format(cur) + ' / ' + format(dur);
        }
    };

    StorageExplorer.prototype.showMovieHud = function() {
        var self = this;
        if (!this.movieHud) return;
        this.movieHud.classList.remove('hidden');
        if (this.movieHudTimer) clearTimeout(this.movieHudTimer);
        this.movieHudTimer = setTimeout(function() {
            if (self.movieHud) self.movieHud.classList.add('hidden');
        }, 4000);
    };

    StorageExplorer.prototype.closeCinemaPlayer = function() {
        if (this.moviePlayerEl) {
            this.moviePlayerEl.pause();
            this.moviePlayerEl.removeAttribute('src');
            this.moviePlayerEl.load();
        }
        if (this.movieOverlay) {
            this.movieOverlay.classList.remove('active');
        }
        if (window.SpatialNav) {
            window.SpatialNav.closeTopModal();
        }
    };

    window.StorageExplorer = new StorageExplorer();

})(window);
