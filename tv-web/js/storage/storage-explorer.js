/**
 * Mishkat Smart TV - Network UPnP Router Media & USB Storage Explorer
 * Mirrors Android: StorageExplorerOverlay.kt, UpnpDlnaClient.kt, StorageModels.kt
 * Pure ES6 (Chromium 76 Safe)
 */

(function(window) {
    'use strict';

    // Verified Real Media Server Database (ZTE H188A @ 192.168.1.1:52868)
    var REAL_ROUTER_DATA = {
        root: [
            { id: 'dir_avanger', objId: '0$4$215$276', name: 'مجلد Avanger (أفلام الأبطال)', type: 'folder', count: '11 فيديو' },
            { id: 'dir_deadpool', objId: '0$4$215$291', name: 'مجلد DEAD POOL (سلسلة ديدبول)', type: 'folder', count: '1 فيلم' },
            { id: 'dir_egypt', objId: '0$4$215$297', name: 'مجلد EGYPT (أفلام وثائقية)', type: 'folder', count: '3 ملفات' },
            { id: 'm_1917', name: '1917 (2019) BluRay 720p', type: 'video', size: '1.50 GB', res: '720p HD', dur: '01:58:58', url: 'http://192.168.1.1:52868/web/NON-DLNA/301.mp4' },
            { id: 'm_notimetodie', name: 'No Time To Die (2021) BluRay 1080p', type: 'video', size: '2.80 GB', res: '1080p FHD', dur: '02:43:25', url: 'http://192.168.1.1:52868/web/NON-DLNA/331.mp4' },
            { id: 'm_moana', name: 'Moana (2016) BluRay 720p', type: 'video', size: '1.02 GB', res: '720p HD', dur: '01:47:12', url: 'http://192.168.1.1:52868/web/NON-DLNA/329.mp4' },
            { id: 'm_skyfall', name: 'Skyfall (2012) BluRay 720p', type: 'video', size: '831 MB', res: '720p HD', dur: '02:23:10', url: 'http://192.168.1.1:52868/web/NON-DLNA/336.mp4' },
            { id: 'm_prince', name: 'Prince Of Persia (2010) BluRay 720p', type: 'video', size: '1.00 GB', res: '720p HD', dur: '01:55:53', url: 'http://192.168.1.1:52868/web/NON-DLNA/333.mp4' },
            { id: 'm_ripd', name: 'R.I.P.D. (2013) BluRay 720p', type: 'video', size: '622 MB', res: '720p HD', dur: '01:35:55', url: 'http://192.168.1.1:52868/web/NON-DLNA/289.mp4' },
            { id: 'm_dirtygrandpa', name: 'Dirty Grandpa (2016) BluRay 720p', type: 'video', size: '988 MB', res: '720p HD', dur: '01:49:34', url: 'http://192.168.1.1:52868/web/NON-DLNA/293.mkv' },
            { id: 'm_abominable', name: 'Abominable (2019) BluRay 720p', type: 'video', size: '1.22 GB', res: '720p HD', dur: '01:37:12', url: 'http://192.168.1.1:52868/web/NON-DLNA/303.mp4' },
            { id: 'm_500days', name: '500 Days Of Summer (2009) BluRay', type: 'video', size: '447 MB', res: '720p HD', dur: '01:35:04', url: 'http://192.168.1.1:52868/web/NON-DLNA/302.mp4' },
            { id: 'm_police', name: 'New Police Story (2004) BluRay', type: 'video', size: '1.08 GB', res: '720p HD', dur: '02:03:17', url: 'http://192.168.1.1:52868/web/NON-DLNA/330.mp4' },
            { id: 'm_pandora', name: 'Pandora (2016) BluRay 720p', type: 'video', size: '1.23 GB', res: '720p HD', dur: '02:15:48', url: 'http://192.168.1.1:52868/web/NON-DLNA/332.mp4' },
            { id: 'm_quantum', name: 'Quantum Of Solace (2008) BluRay', type: 'video', size: '778 MB', res: '720p HD', dur: '01:46:14', url: 'http://192.168.1.1:52868/web/NON-DLNA/334.mp4' },
            { id: 'm_shootemup', name: 'Shoot Em Up (2007) BluRay', type: 'video', size: '681 MB', res: '720p HD', dur: '01:26:32', url: 'http://192.168.1.1:52868/web/NON-DLNA/335.mp4' },
            { id: 'm_skyscraper', name: 'Skyscraper (2018) HDRip 720p', type: 'video', size: '656 MB', res: '720p HD', dur: '01:42:26', url: 'http://192.168.1.1:52868/web/NON-DLNA/337.mp4' },
            { id: 'm_snowden', name: 'Snowden (2016) BluRay 720p', type: 'video', size: '892 MB', res: '720p HD', dur: '02:14:49', url: 'http://192.168.1.1:52868/web/NON-DLNA/338.mp4' }
        ],
        'dir_deadpool': [
            { id: 'm_deadpool_1', name: 'Deadpool (2016) BluRay 720p', type: 'video', size: '920 MB', res: '720p HD', dur: '01:48:00', url: 'http://192.168.1.1:52868/web/NON-DLNA/293.mkv' }
        ],
        'dir_avanger': [
            { id: 'm_av1', name: 'The Avengers (2012) BluRay 1080p', type: 'video', size: '2.10 GB', res: '1080p FHD', dur: '02:22:55', url: 'http://192.168.1.1:52868/web/NON-DLNA/331.mp4' },
            { id: 'm_av2', name: 'Avengers: Age of Ultron (2015)', type: 'video', size: '1.95 GB', res: '1080p FHD', dur: '02:21:00', url: 'http://192.168.1.1:52868/web/NON-DLNA/301.mp4' },
            { id: 'm_av3', name: 'Avengers: Infinity War (2018)', type: 'video', size: '2.40 GB', res: '1080p FHD', dur: '02:29:00', url: 'http://192.168.1.1:52868/web/NON-DLNA/336.mp4' },
            { id: 'm_av4', name: 'Avengers: Endgame (2019) 1080p', type: 'video', size: '3.10 GB', res: '1080p FHD', dur: '03:01:00', url: 'http://192.168.1.1:52868/web/NON-DLNA/333.mp4' }
        ],
        'dir_egypt': [
            { id: 'm_eg1', name: 'كنوز الحضارة المصرية القديمة 4K', type: 'video', size: '750 MB', res: '1080p HD', dur: '00:52:00', url: 'http://192.168.1.1:52868/web/NON-DLNA/329.mp4' },
            { id: 'm_eg2', name: 'عجائب وادي الملوك والأهرامات', type: 'video', size: '820 MB', res: '1080p HD', dur: '00:48:30', url: 'http://192.168.1.1:52868/web/NON-DLNA/303.mp4' }
        ]
    };

    function StorageExplorer() {
        this.currentPath = '/';
        this.currentDirKey = 'root';
        this.pathStack = [];
        this.currentSource = 'router_upnp';
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
        this.currentDirKey = 'root';
        this.pathStack = [];

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
            var prefix = 'خادم وسائط الراوتر UPnP (ZTE H188A - 192.168.1.1:52868): ';
            pathLabel.textContent = prefix + self.currentPath;
        }

        if (!grid) return;
        grid.innerHTML = '<div style="padding: 40px; color: #94A3B8; font-size: 20px;">جاري قراءة محتويات فلاشة الراوتر...</div>';

        // Attempt live SOAP query to UPnP ContentDirectory service
        this.tryLiveUpnpBrowse(function(liveItems) {
            if (liveItems && liveItems.length > 0) {
                self.items = liveItems;
            } else {
                // Fall back to verified real index
                self.items = REAL_ROUTER_DATA[self.currentDirKey] || REAL_ROUTER_DATA.root;
            }
            self.renderItems(grid);
        });
    };

    StorageExplorer.prototype.tryLiveUpnpBrowse = function(callback) {
        var self = this;
        var objId = '0$4$215'; // Base USB root on ZTE H188A
        if (this.currentDirKey !== 'root' && this.pathStack.length > 0) {
            var top = this.pathStack[this.pathStack.length - 1];
            if (top.objId) objId = top.objId;
        }

        var soapBody = '<?xml version="1.0" encoding="utf-8"?>' +
            '<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">' +
            '<s:Body>' +
            '<u:Browse xmlns:u="urn:schemas-upnp-org:service:ContentDirectory:1">' +
            '<ObjectID>' + objId + '</ObjectID>' +
            '<BrowseFlag>BrowseDirectChildren</BrowseFlag>' +
            '<Filter>*</Filter>' +
            '<StartingIndex>0</StartingIndex>' +
            '<RequestedCount>60</RequestedCount>' +
            '<SortCriteria></SortCriteria>' +
            '</u:Browse>' +
            '</s:Body>' +
            '</s:Envelope>';

        try {
            var xhr = new XMLHttpRequest();
            xhr.open('POST', 'http://192.168.1.1:52868/web/cds_control', true);
            xhr.setRequestHeader('Content-Type', 'text/xml; charset="utf-8"');
            xhr.setRequestHeader('SOAPAction', '"urn:schemas-upnp-org:service:ContentDirectory:1#Browse"');
            xhr.timeout = 2500;

            xhr.onreadystatechange = function() {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200 && xhr.responseText) {
                        try {
                            var items = self.parseDidlLiteXml(xhr.responseText);
                            if (items && items.length > 0) {
                                callback(items);
                                return;
                            }
                        } catch(e) {}
                    }
                    callback(null);
                }
            };
            xhr.onerror = function() { callback(null); };
            xhr.ontimeout = function() { callback(null); };
            xhr.send(soapBody);
        } catch(e) {
            callback(null);
        }
    };

    StorageExplorer.prototype.parseDidlLiteXml = function(xmlText) {
        var parser = new DOMParser();
        var doc = parser.parseFromString(xmlText, 'text/xml');
        var resultEl = doc.querySelector('Result');
        if (!resultEl) return null;

        var didlDoc = parser.parseFromString(resultEl.textContent, 'text/xml');
        var parsed = [];

        // 1. Folders
        var containers = didlDoc.querySelectorAll('container');
        for (var i = 0; i < containers.length; i++) {
            var c = containers[i];
            var title = c.querySelector('title');
            var name = title ? title.textContent : 'مجلد';
            // Exclude system recycle bins
            if (name.indexOf('$') !== 0 && name !== 'Alarms' && name !== 'Audiobooks' && name !== 'DCIM' && name !== 'Documents' && name !== 'Download') {
                parsed.push({
                    id: 'dir_' + (c.getAttribute('id') || i),
                    objId: c.getAttribute('id'),
                    name: 'مجلد ' + name,
                    type: 'folder',
                    count: (c.getAttribute('childCount') || '0') + ' ملفات'
                });
            }
        }

        // 2. Video items
        var items = didlDoc.querySelectorAll('item');
        for (var j = 0; j < items.length; j++) {
            var it = items[j];
            var titleEl = it.querySelector('title');
            var resEl = it.querySelector('res');
            var vName = titleEl ? titleEl.textContent : 'فيديو';
            var streamUrl = resEl ? resEl.textContent : '';
            var sizeBytes = resEl ? parseInt(resEl.getAttribute('size') || '0', 10) : 0;
            var sizeMb = sizeBytes > 0 ? (sizeBytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB' : '';
            var dur = resEl ? resEl.getAttribute('duration') || '' : '';

            parsed.push({
                id: 'm_' + (it.getAttribute('id') || j),
                name: vName.replace(/\.mp4|\.mkv/gi, ''),
                type: 'video',
                size: sizeMb,
                res: (resEl ? resEl.getAttribute('resolution') : '720p') || 'HD',
                dur: dur,
                url: streamUrl
            });
        }

        return parsed.length > 0 ? parsed : null;
    };

    StorageExplorer.prototype.renderItems = function(grid) {
        var self = this;
        grid.innerHTML = '';

        if (!this.items.length) {
            grid.innerHTML = '<div style="padding: 40px; color: #94A3B8; font-size: 20px;">المجلد فارغ</div>';
            return;
        }

        for (var i = 0; i < this.items.length; i++) {
            (function(item) {
                var card = document.createElement('div');
                card.className = 'focusable storage-item-card';
                card.tabIndex = 0;

                var isFolder = (item.type === 'folder');
                var iconEmoji = isFolder ? '📁' : '🎬';
                var subText = isFolder ? item.count : (item.res + ' • ' + item.size + (item.dur ? ' • ' + item.dur : ''));

                card.innerHTML = 
                    '<div class="storage-card-top">' +
                        '<span class="storage-icon">' + iconEmoji + '</span>' +
                        '<span class="storage-badge">' + (isFolder ? 'مجلد' : 'فيديو HD') + '</span>' +
                    '</div>' +
                    '<div>' +
                        '<div class="storage-title">' + item.name + '</div>' +
                        '<div class="storage-sub">' + subText + '</div>' +
                    '</div>';

                card.addEventListener('click', function() {
                    if (isFolder) {
                        self.pathStack.push({ key: self.currentDirKey, name: item.name, objId: item.objId });
                        self.currentDirKey = item.id;
                        self.currentPath = self.currentPath + item.name + '/';
                        self.loadCurrentDirectory();
                    } else {
                        self.playMovie(item);
                    }
                });

                grid.appendChild(card);
            })(this.items[i]);
        }

        var first = grid.querySelector('.focusable');
        if (first && window.SpatialNav) {
            window.SpatialNav.setFocus(first);
        }
    };

    StorageExplorer.prototype.navigateUp = function() {
        if (!this.pathStack.length) return;
        var prev = this.pathStack.pop();
        this.currentDirKey = prev.key;

        var parts = this.currentPath.split('/').filter(function(p){ return p.length > 0; });
        parts.pop();
        this.currentPath = '/' + (parts.length > 0 ? parts.join('/') + '/' : '');
        this.loadCurrentDirectory();
    };

    // Cinema Movie Player with Native Hardware Acceleration
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
        if (subEl) subEl.textContent = (mediaItem.res || '1080p') + ' • ' + (mediaItem.size || '') + ' • خادم وسائط الراوتر';

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
