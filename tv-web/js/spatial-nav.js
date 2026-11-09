/**
 * Mishkat Smart TV - Spatial Navigation Engine
 * Cross-platform D-Pad navigation for Samsung Tizen, LG webOS & Android TV
 */

class SpatialNav {
    constructor() {
        this.currentFocus = null;
        this.isRTL = document.documentElement.dir === 'rtl';
        this.initKeyCodes();
        this.initEvents();
    }

    initKeyCodes() {
        this.KEYS = {
            LEFT: 37,
            UP: 38,
            RIGHT: 39,
            DOWN: 40,
            ENTER: 13,
            BACK_ESC: 27,
            BACK_TIZEN: 10009,
            BACK_WEBOS: 461,
            BACKSPACE: 8,
            CH_UP: 427,
            CH_DOWN: 428,
            PAGE_UP: 33,
            PAGE_DOWN: 34,
            PLAY: 415,
            PAUSE: 19,
            PLAY_PAUSE: 10252,
            RED: 403,
            GREEN: 404,
            YELLOW: 405,
            BLUE: 406
        };
    }

    initEvents() {
        window.addEventListener('keydown', (e) => this.handleKeyDown(e));
        
        // Also support click / mouse focus for testing & hybrid devices
        document.addEventListener('mouseover', (e) => {
            const focusable = e.target.closest('.focusable');
            if (focusable && focusable !== this.currentFocus) {
                this.setFocus(focusable);
            }
        });
    }

    handleKeyDown(e) {
        const code = e.keyCode || e.which;

        // 1. Check if Fullscreen Player is active
        if (window.TvPlayer && window.TvPlayer.isActive()) {
            if (window.TvPlayer.handleKey(code, e)) {
                e.preventDefault();
                return;
            }
        }

        // 2. Check if Developer / Settings Modal is open
        const activeModal = document.querySelector('.modal-overlay.active');
        if (activeModal) {
            this.handleModalNav(code, activeModal, e);
            return;
        }

        // 3. Normal Launcher Home Navigation
        switch (code) {
            case this.KEYS.UP:
                this.moveFocus('up');
                e.preventDefault();
                break;
            case this.KEYS.DOWN:
                this.moveFocus('down');
                e.preventDefault();
                break;
            case this.KEYS.LEFT:
                this.moveFocus('left');
                e.preventDefault();
                break;
            case this.KEYS.RIGHT:
                this.moveFocus('right');
                e.preventDefault();
                break;
            case this.KEYS.ENTER:
                if (this.currentFocus) {
                    this.currentFocus.click();
                }
                e.preventDefault();
                break;
            case this.KEYS.BACK_ESC:
            case this.KEYS.BACK_TIZEN:
            case this.KEYS.BACK_WEBOS:
            case this.KEYS.BACKSPACE:
                this.handleBackKey();
                e.preventDefault();
                break;
        }
    }

    handleModalNav(code, modal, e) {
        if (code === this.KEYS.BACK_ESC || code === this.KEYS.BACK_TIZEN || 
            code === this.KEYS.BACK_WEBOS || code === this.KEYS.BACKSPACE) {
            modal.classList.remove('active');
            if (this.lastHomeFocus) {
                this.setFocus(this.lastHomeFocus);
            }
            e.preventDefault();
            return;
        }

        const focusables = Array.from(modal.querySelectorAll('.focusable'));
        if (!focusables.length) return;

        const currentIndex = focusables.indexOf(this.currentFocus);

        if (code === this.KEYS.UP || code === this.KEYS.LEFT) {
            const prev = (currentIndex - 1 + focusables.length) % focusables.length;
            this.setFocus(focusables[prev]);
            e.preventDefault();
        } else if (code === this.KEYS.DOWN || code === this.KEYS.RIGHT) {
            const next = (currentIndex + 1) % focusables.length;
            this.setFocus(focusables[next]);
            e.preventDefault();
        } else if (code === this.KEYS.ENTER) {
            if (this.currentFocus) this.currentFocus.click();
            e.preventDefault();
        }
    }

    handleBackKey() {
        // Exit or prompt on TV
        if (typeof tizen !== 'undefined') {
            try { tizen.application.getCurrentApplication().exit(); } catch(e) {}
        } else if (typeof webOS !== 'undefined') {
            try { window.close(); } catch(e) {}
        }
    }

    setFocus(el) {
        if (!el) return;
        if (this.currentFocus) {
            this.currentFocus.classList.remove('focused');
        }
        this.currentFocus = el;
        el.classList.add('focused');
        el.focus({ preventScroll: true });

        // Scroll parent container horizontally or vertically into view
        const scrollParent = el.closest('.scroll-container');
        if (scrollParent) {
            el.scrollIntoView({
                behavior: 'smooth',
                inline: 'center',
                block: 'nearest'
            });
        }
    }

    moveFocus(direction) {
        const allFocusable = Array.from(document.querySelectorAll('.tv-container .focusable:not([disabled])'))
            .filter(el => {
                const rect = el.getBoundingClientRect();
                return rect.width > 0 && rect.height > 0 && el.offsetParent !== null;
            });

        if (!allFocusable.length) return;

        if (!this.currentFocus || !allFocusable.includes(this.currentFocus)) {
            this.setFocus(allFocusable[0]);
            return;
        }

        const currentRect = this.currentFocus.getBoundingClientRect();
        const currentCenter = {
            x: currentRect.left + currentRect.width / 2,
            y: currentRect.top + currentRect.height / 2
        };

        // Determine candidate elements in the desired direction
        let bestCandidate = null;
        let minDistance = Infinity;

        // Try direct row sibling first if moving horizontal within a scroll container
        const scrollContainer = this.currentFocus.closest('.scroll-container');
        if (scrollContainer && (direction === 'left' || direction === 'right')) {
            const siblings = Array.from(scrollContainer.querySelectorAll('.focusable'));
            const idx = siblings.indexOf(this.currentFocus);
            
            // RTL handling: 'right' means physically to the right (previous item in RTL list)
            const forward = this.isRTL ? (direction === 'left') : (direction === 'right');
            const targetIdx = forward ? idx + 1 : idx - 1;

            if (targetIdx >= 0 && targetIdx < siblings.length) {
                this.setFocus(siblings[targetIdx]);
                return;
            }
        }

        // Spatial geometry calculation
        for (const candidate of allFocusable) {
            if (candidate === this.currentFocus) continue;

            const rect = candidate.getBoundingClientRect();
            const center = {
                x: rect.left + rect.width / 2,
                y: rect.top + rect.height / 2
            };

            const dx = center.x - currentCenter.x;
            const dy = center.y - currentCenter.y;

            let isInDirection = false;
            switch (direction) {
                case 'up':
                    isInDirection = dy < -8 && Math.abs(dx) < Math.abs(dy) * 2.2;
                    break;
                case 'down':
                    isInDirection = dy > 8 && Math.abs(dx) < Math.abs(dy) * 2.2;
                    break;
                case 'left':
                    isInDirection = dx < -8 && Math.abs(dy) < Math.abs(dx) * 1.5;
                    break;
                case 'right':
                    isInDirection = dx > 8 && Math.abs(dy) < Math.abs(dx) * 1.5;
                    break;
            }

            if (isInDirection) {
                // Priority distance: penalize orthogonal movement
                const dist = Math.sqrt(dx * dx + dy * dy);
                const penalty = (direction === 'up' || direction === 'down') 
                    ? Math.abs(dx) * 1.5 
                    : Math.abs(dy) * 1.5;
                const totalScore = dist + penalty;

                if (totalScore < minDistance) {
                    minDistance = totalScore;
                    bestCandidate = candidate;
                }
            }
        }

        if (bestCandidate) {
            this.setFocus(bestCandidate);
        }
    }
}

window.spatialNav = new SpatialNav();
