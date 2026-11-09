/**
 * Mishkat Smart TV - Video Player Engine
 * Unified HLS / Tizen AVPlay / webOS Player with TV Remote Zapping & HUD
 */

class TvPlayer {
    constructor() {
        this.videoEl = document.getElementById('tvVideoPlayer');
        this.overlay = document.getElementById('playerOverlay');
        this.hud = document.getElementById('playerHud');
        this.quickMenu = document.getElementById('playerQuickMenu');
        this.hls = null;
        
        this.channels = [];
        this.currentIndex = 0;
        this.hudTimer = null;

        this.init();
    }

    init() {
        document.getElementById('btnExitPlayer')?.addEventListener('click', () => this.close());
    }

    isActive() {
        return this.overlay && this.overlay.classList.contains('active');
    }

    setChannels(channelList) {
        this.channels = channelList;
        this.renderQuickMenu();
    }

    playChannel(channel, index) {
        if (!channel) return;
        this.currentIndex = (typeof index === 'number') ? index : this.channels.findIndex(c => c.id === channel.id);
        if (this.currentIndex < 0) this.currentIndex = 0;

        // Open Overlay
        this.overlay.classList.add('active');
        this.showHud();

        // Update HUD
        const titleEl = document.getElementById('hudChannelTitle');
        const subEl = document.getElementById('hudChannelSub');
        if (titleEl) titleEl.textContent = channel.name;
        if (subEl) subEl.textContent = `${channel.category} • ${channel.quality || 'HD 1080p'}`;

        // Stream URL playback
        const streamUrl = channel.url || channel.streamUrl;

        if (window.Hls && Hls.isSupported()) {
            if (this.hls) {
                this.hls.destroy();
            }
            this.hls = new Hls({
                enableWorker: true,
                lowLatencyMode: true,
                backBufferLength: 60
            });
            this.hls.loadSource(streamUrl);
            this.hls.attachMedia(this.videoEl);
            this.hls.on(Hls.Events.MANIFEST_PARSED, () => {
                this.videoEl.play().catch(() => {});
            });
            this.hls.on(Hls.Events.ERROR, (event, data) => {
                if (data.fatal) {
                    console.warn('HLS stream error:', data);
                    this.hls.recoverMediaError();
                }
            });
        } else if (this.videoEl.canPlayType('application/vnd.apple.mpegurl')) {
            // Safari or native webOS / Tizen HLS support
            this.videoEl.src = streamUrl;
            this.videoEl.play().catch(() => {});
        } else {
            this.videoEl.src = streamUrl;
            this.videoEl.play().catch(() => {});
        }

        this.updateQuickMenuActive();
    }

    nextChannel() {
        if (!this.channels.length) return;
        this.currentIndex = (this.currentIndex + 1) % this.channels.length;
        this.playChannel(this.channels[this.currentIndex], this.currentIndex);
    }

    prevChannel() {
        if (!this.channels.length) return;
        this.currentIndex = (this.currentIndex - 1 + this.channels.length) % this.channels.length;
        this.playChannel(this.channels[this.currentIndex], this.currentIndex);
    }

    toggleQuickMenu() {
        if (!this.quickMenu) return;
        const isOpen = this.quickMenu.classList.contains('open');
        if (isOpen) {
            this.quickMenu.classList.remove('open');
        } else {
            this.quickMenu.classList.add('open');
            this.showHud();
            // Focus active item in quick menu
            const activeItem = this.quickMenu.querySelector('.quick-channel-item.active') || this.quickMenu.querySelector('.quick-channel-item');
            if (activeItem && window.spatialNav) {
                window.spatialNav.setFocus(activeItem);
            }
        }
    }

    showHud() {
        if (!this.hud) return;
        this.hud.classList.remove('fade-out');
        clearTimeout(this.hudTimer);
        this.hudTimer = setTimeout(() => {
            if (!this.quickMenu?.classList.contains('open')) {
                this.hud.classList.add('fade-out');
            }
        }, 5000);
    }

    close() {
        if (this.hls) {
            this.hls.destroy();
            this.hls = null;
        }
        if (this.videoEl) {
            this.videoEl.pause();
            this.videoEl.removeAttribute('src');
            this.videoEl.load();
        }
        if (this.quickMenu) this.quickMenu.classList.remove('open');
        this.overlay.classList.remove('active');

        // Restore focus to last active channel card in home screen
        if (window.app && window.app.lastChannelFocused && window.spatialNav) {
            window.spatialNav.setFocus(window.app.lastChannelFocused);
        }
    }

    handleKey(code, e) {
        this.showHud();
        const KEYS = window.spatialNav.KEYS;

        // If Quick Menu is Open
        if (this.quickMenu?.classList.contains('open')) {
            if (code === KEYS.BACK_ESC || code === KEYS.BACK_TIZEN || code === KEYS.BACK_WEBOS || code === KEYS.BACKSPACE || code === KEYS.LEFT) {
                this.quickMenu.classList.remove('open');
                return true;
            }
            if (code === KEYS.UP) {
                window.spatialNav.moveFocus('up');
                return true;
            }
            if (code === KEYS.DOWN) {
                window.spatialNav.moveFocus('down');
                return true;
            }
            if (code === KEYS.ENTER) {
                if (window.spatialNav.currentFocus) {
                    window.spatialNav.currentFocus.click();
                }
                return true;
            }
            return false;
        }

        // Quick Menu is NOT Open: Direct remote zapping
        switch (code) {
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
                this.toggleQuickMenu();
                return true;

            case KEYS.BACK_ESC:
            case KEYS.BACK_TIZEN:
            case KEYS.BACK_WEBOS:
            case KEYS.BACKSPACE:
                this.close();
                return true;
        }

        return false;
    }

    renderQuickMenu() {
        const listEl = document.getElementById('quickMenuList');
        if (!listEl) return;
        listEl.innerHTML = '';

        this.channels.forEach((ch, idx) => {
            const item = document.createElement('div');
            item.className = 'focusable quick-channel-item';
            item.tabIndex = 0;
            item.dataset.index = idx;
            item.innerHTML = `
                <div class="quick-ch-title">${ch.name}</div>
                <div class="quick-ch-badge">${ch.quality || 'HD'}</div>
            `;
            item.addEventListener('click', () => {
                this.playChannel(ch, idx);
                this.quickMenu.classList.remove('open');
            });
            listEl.appendChild(item);
        });
    }

    updateQuickMenuActive() {
        const items = document.querySelectorAll('.quick-channel-item');
        items.forEach((it, i) => {
            if (i === this.currentIndex) it.classList.add('active');
            else it.classList.remove('active');
        });
    }
}

window.TvPlayer = new TvPlayer();
