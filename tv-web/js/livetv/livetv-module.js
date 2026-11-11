/**
 * Mishkat Smart TV - Live Satellite TV Player & Channel Manager
 * Mirrors Android: LiveTvUi.kt, ChannelAndTransponderManager.kt, M3uParser.kt
 * Pure ES6 (Chromium 76 Safe)
 */

(function(window) {
    'use strict';

    function LiveTvPlayer() {
        this.videoEl = document.getElementById('tvVideoPlayer');
        this.overlay = document.getElementById('playerOverlay');
        this.hud = document.getElementById('playerHud');
        this.sideList = document.getElementById('playerSideList');
        this.hls = null;

        this.channels = [];
        this.currentIndex = 0;
        this.hudTimer = null;
        this.aspectModes = ['contain', 'cover', 'fill'];
        this.currentAspectIdx = 0;

        this.digitBuffer = '';
        this.digitTimer = null;

        this.init();
    }

    LiveTvPlayer.prototype.init = function() {
        var self = this;
        var btnExit = document.getElementById('btnExitPlayer');
        var btnRew = document.getElementById('btnHudRewind');
        var btnFwd = document.getElementById('btnHudForward');
        var btnPlay = document.getElementById('btnHudPlayPause');
        var btnPrev = document.getElementById('btnHudPrevChannel');
        var btnNext = document.getElementById('btnHudNextChannel');
        var btnAspect = document.getElementById('btnHudAspect');
        var btnSideList = document.getElementById('btnHudChannelList');

        if (btnExit) btnExit.addEventListener('click', function() { self.close(); });
        if (btnRew) btnRew.addEventListener('click', function() { self.seek(-15); });
        if (btnFwd) btnFwd.addEventListener('click', function() { self.seek(15); });
        if (btnPlay) btnPlay.addEventListener('click', function() { self.togglePlayPause(); });
        if (btnPrev) btnPrev.addEventListener('click', function() { self.prevChannel(); });
        if (btnNext) btnNext.addEventListener('click', function() { self.nextChannel(); });
        if (btnAspect) btnAspect.addEventListener('click', function() { self.toggleAspect(); });
        if (btnSideList) btnSideList.addEventListener('click', function() { self.toggleSideList(); });

        if (this.videoEl) {
            this.videoEl.addEventListener('play', function() { self.updatePlayBtnText('⏸ إيقاف مؤقت'); });
            this.videoEl.addEventListener('pause', function() { self.updatePlayBtnText('▶ استئناف'); });
        }

        // Load custom imported M3U channels from localStorage
        this.loadCustomChannels();
    };

    LiveTvPlayer.prototype.isActive = function() {
        return this.overlay && this.overlay.classList.contains('active');
    };

    LiveTvPlayer.prototype.setChannels = function(channelList) {
        this.channels = channelList || [];
    };

    LiveTvPlayer.prototype.playChannel = function(channel, index) {
        if (!channel) return;
        var self = this;

        if (typeof index === 'number') {
            this.currentIndex = index;
        } else {
            var idx = -1;
            for (var i = 0; i < this.channels.length; i++) {
                if (this.channels[i].id === channel.id) {
                    idx = i;
                    break;
                }
            }
            this.currentIndex = (idx !== -1) ? idx : 0;
        }

        if (this.overlay) {
            this.overlay.classList.add('active');
        }
        this.showHud();

        var titleEl = document.getElementById('hudChannelTitle');
        var subEl = document.getElementById('hudChannelSub');
        var badgeEl = document.getElementById('hudBadge');
        var numEl = document.getElementById('hudChannelNum');

        if (titleEl) titleEl.textContent = channel.nameAr || channel.name;
        if (subEl) {
            var sat = channel.satellite ? channel.satellite.replace(/_/g, ' ') : 'نايل سات';
            var freq = channel.frequency ? (' • ' + channel.frequency + ' ' + (channel.polarization || 'V')) : '';
            subEl.textContent = sat + freq;
        }
        if (badgeEl) badgeEl.textContent = (channel.badge || '1080p HD') + ' • LIVE';
        if (numEl) numEl.textContent = 'CH ' + (this.currentIndex + 1);

        var streamUrl = channel.streamUrl || channel.url;
        console.log('Playing live stream:', streamUrl);

        if (!this.videoEl) return;

        // On Samsung Tizen Smart TVs, native HTML5 video element decodes HLS via hardware GStreamer
        var canPlayNativeHls = this.videoEl.canPlayType('application/vnd.apple.mpegurl') || 
                               this.videoEl.canPlayType('application/x-mpegURL');

        if (canPlayNativeHls) {
            console.log('Using Tizen native hardware HLS player for:', streamUrl);
            this.videoEl.src = streamUrl;
            this.videoEl.play().catch(function(e){ console.warn('Native play error:', e); });
        } else if (window.Hls && typeof window.Hls.isSupported === 'function' && window.Hls.isSupported()) {
            if (this.hls) {
                this.hls.destroy();
                this.hls = null;
            }
            try {
                this.hls = new window.Hls({ enableWorker: true, lowLatencyMode: true });
                this.hls.loadSource(streamUrl);
                this.hls.attachMedia(this.videoEl);
                this.hls.on(window.Hls.Events.MANIFEST_PARSED, function() {
                    self.videoEl.play().catch(function(){});
                });
                this.hls.on(window.Hls.Events.ERROR, function(event, data) {
                    if (data && data.fatal) {
                        self.hls.recoverMediaError();
                    }
                });
            } catch(e) {
                console.warn('Hls.js failed, using native video element fallback:', e);
                this.videoEl.src = streamUrl;
                this.videoEl.play().catch(function(){});
            }
        } else {
            this.videoEl.src = streamUrl;
            this.videoEl.play().catch(function(){});
        }
    };

    LiveTvPlayer.prototype.seek = function(seconds) {
        if (!this.videoEl) return;
        this.videoEl.currentTime = Math.max(0, this.videoEl.currentTime + seconds);
        this.showHud();
    };

    LiveTvPlayer.prototype.togglePlayPause = function() {
        if (!this.videoEl) return;
        if (this.videoEl.paused) {
            this.videoEl.play().catch(function(){});
        } else {
            this.videoEl.pause();
        }
        this.showHud();
    };

    LiveTvPlayer.prototype.updatePlayBtnText = function(text) {
        var btn = document.getElementById('btnHudPlayPause');
        if (btn) btn.textContent = text;
    };

    LiveTvPlayer.prototype.nextChannel = function() {
        if (!this.channels.length) return;
        this.currentIndex = (this.currentIndex + 1) % this.channels.length;
        this.playChannel(this.channels[this.currentIndex], this.currentIndex);
    };

    LiveTvPlayer.prototype.prevChannel = function() {
        if (!this.channels.length) return;
        this.currentIndex = (this.currentIndex - 1 + this.channels.length) % this.channels.length;
        this.playChannel(this.channels[this.currentIndex], this.currentIndex);
    };

    LiveTvPlayer.prototype.toggleAspect = function() {
        this.currentAspectIdx = (this.currentAspectIdx + 1) % this.aspectModes.length;
        var mode = this.aspectModes[this.currentAspectIdx];
        if (this.videoEl) this.videoEl.style.objectFit = mode;
        var btn = document.getElementById('btnHudAspect');
        if (btn) {
            var labels = { contain: 'أبعاد (16:9)', cover: 'ملء الشاشة (Zoom)', fill: 'تمدد (Stretch)' };
            btn.textContent = '🔲 ' + (labels[mode] || mode);
        }
        this.showHud();
    };

    LiveTvPlayer.prototype.toggleSideList = function() {
        if (!this.sideList) return;
        if (this.sideList.classList.contains('active')) {
            this.sideList.classList.remove('active');
        } else {
            this.renderSideList();
            this.sideList.classList.add('active');
            var firstItem = this.sideList.querySelector('.focusable');
            if (firstItem && window.SpatialNav) {
                window.SpatialNav.setFocus(firstItem);
            }
        }
    };

    LiveTvPlayer.prototype.renderSideList = function() {
        var self = this;
        var container = document.getElementById('playerSideListContent');
        if (!container) return;
        container.innerHTML = '';

        for (var i = 0; i < this.channels.length; i++) {
            (function(ch, idx) {
                var item = document.createElement('div');
                item.className = 'focusable side-channel-item' + (idx === self.currentIndex ? ' active' : '');
                item.tabIndex = 0;
                item.innerHTML = 
                    '<span class="side-channel-num">' + (idx + 1) + '</span>' +
                    '<span class="side-channel-name">' + (ch.nameAr || ch.name) + '</span>';
                item.addEventListener('click', function() {
                    self.playChannel(ch, idx);
                    if (self.sideList) self.sideList.classList.remove('active');
                });
                container.appendChild(item);
            })(this.channels[i], i);
        }
    };

    LiveTvPlayer.prototype.handleDigit = function(digit) {
        var self = this;
        this.digitBuffer += String(digit);
        var indicator = document.getElementById('hudChannelNumIndicator');
        if (indicator) {
            indicator.textContent = 'قناة: ' + this.digitBuffer;
            indicator.style.display = 'block';
        }

        if (this.digitTimer) clearTimeout(this.digitTimer);
        this.digitTimer = setTimeout(function() {
            var targetNum = parseInt(self.digitBuffer, 10);
            self.digitBuffer = '';
            if (indicator) indicator.style.display = 'none';

            if (!isNaN(targetNum) && targetNum >= 1 && targetNum <= self.channels.length) {
                var targetIdx = targetNum - 1;
                self.playChannel(self.channels[targetIdx], targetIdx);
            }
        }, 1500);
    };

    LiveTvPlayer.prototype.showHud = function() {
        var self = this;
        if (!this.hud) return;
        this.hud.classList.remove('hidden');
        if (this.hudTimer) clearTimeout(this.hudTimer);
        this.hudTimer = setTimeout(function() {
            if (self.hud && (!self.sideList || !self.sideList.classList.contains('active'))) {
                self.hud.classList.add('hidden');
            }
        }, 4000);
    };

    LiveTvPlayer.prototype.close = function() {
        if (this.hls) {
            this.hls.destroy();
            this.hls = null;
        }
        if (this.videoEl) {
            this.videoEl.pause();
            this.videoEl.removeAttribute('src');
            this.videoEl.load();
        }
        if (this.overlay) {
            this.overlay.classList.remove('active');
        }
        if (this.sideList) {
            this.sideList.classList.remove('active');
        }

        if (window.SpatialNav) {
            window.SpatialNav.closeTopModal();
        }
    };

    LiveTvPlayer.prototype.handleKey = function(code, e) {
        this.showHud();
        var KEYS = window.SpatialNav ? window.SpatialNav.KEYS : {};

        switch (code) {
            case KEYS.LEFT:
                this.seek(-15);
                return true;
            case KEYS.RIGHT:
                this.seek(15);
                return true;
            case KEYS.UP:
            case KEYS.CH_UP:
            case KEYS.PAGE_UP:
                this.nextChannel();
                return true;
            case KEYS.DOWN:
            case KEYS.CH_DOWN:
            case KEYS.PAGE_DOWN:
                this.prevChannel();
                return true;
            case KEYS.ENTER:
                if (this.sideList && this.sideList.classList.contains('active')) {
                    // Let SpatialNav handle clicking within side list
                    return false;
                }
                this.toggleSideList();
                return true;
            case KEYS.MEDIA_PLAY:
                if (this.videoEl) this.videoEl.play();
                return true;
            case KEYS.MEDIA_PAUSE:
                if (this.videoEl) this.videoEl.pause();
                return true;
            case KEYS.MEDIA_PLAY_PAUSE:
                this.togglePlayPause();
                return true;
            case KEYS.MEDIA_FF:
                this.seek(15);
                return true;
            case KEYS.MEDIA_REW:
                this.seek(-15);
                return true;
            case KEYS.BACK_ESC:
            case KEYS.BACK_TIZEN:
            case KEYS.BACK_WEBOS:
            case KEYS.BACKSPACE:
                if (this.sideList && this.sideList.classList.contains('active')) {
                    this.sideList.classList.remove('active');
                    return true;
                }
                this.close();
                return true;
        }

        // Numeric keys
        if (code >= KEYS.NUM_0 && code <= KEYS.NUM_9) {
            this.handleDigit(code - KEYS.NUM_0);
            return true;
        }

        return false;
    };

    LiveTvPlayer.prototype.loadCustomChannels = function() {
        try {
            var raw = localStorage.getItem('mishkat_custom_channels');
            if (raw) {
                var list = JSON.parse(raw);
                if (Array.isArray(list) && list.length > 0) {
                    var data = window.MISHKAT_DATA || {};
                    data.channels = (data.channels || []).concat(list);
                }
            }
        } catch(e) {
            console.warn('Error loading custom channels from storage:', e);
        }
    };

    window.LiveTvPlayer = new LiveTvPlayer();

})(window);
