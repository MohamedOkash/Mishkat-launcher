/**
 * Mishkat Smart TV - Spatial Navigation & D-Pad Remote Controller
 * Mirrors Android: RemoteUnificationLayer.kt, RemoteAdapters.kt, MishkatFocus.kt
 * Pure ES6 (Chromium 76 Safe)
 */

(function(window) {
    'use strict';

    function SpatialNav() {
        this.currentFocus = null;
        this.lastHomeFocus = null;
        this.modalStack = [];

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
            MEDIA_PLAY: 415,
            MEDIA_PAUSE: 19,
            MEDIA_PLAY_PAUSE: 10252,
            MEDIA_FF: 417,
            MEDIA_REW: 412,
            RED: 403,
            GREEN: 404,
            YELLOW: 405,
            BLUE: 406,
            NUM_0: 48,
            NUM_1: 49,
            NUM_2: 50,
            NUM_3: 51,
            NUM_4: 52,
            NUM_5: 53,
            NUM_6: 54,
            NUM_7: 55,
            NUM_8: 56,
            NUM_9: 57
        };

        this.init();
    }

    SpatialNav.prototype.init = function() {
        this.registerTizenKeys();
        this.bindEvents();
    };

    SpatialNav.prototype.registerTizenKeys = function() {
        if (typeof tizen !== 'undefined' && tizen.tvinputdevice) {
            try {
                var keysToRegister = [
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                    'ChannelUp', 'ChannelDown',
                    'MediaPlayPause', 'MediaPlay', 'MediaPause', 'MediaFastForward', 'MediaRewind',
                    'ColorF0Red', 'ColorF1Green', 'ColorF2Yellow', 'ColorF3Blue'
                ];
                if (typeof tizen.tvinputdevice.registerKeyBatch === 'function') {
                    tizen.tvinputdevice.registerKeyBatch(keysToRegister);
                    console.log('Registered Tizen remote key batch successfully.');
                } else if (typeof tizen.tvinputdevice.registerKey === 'function') {
                    for (var i = 0; i < keysToRegister.length; i++) {
                        try {
                            tizen.tvinputdevice.registerKey(keysToRegister[i]);
                        } catch(e) {}
                    }
                }
            } catch (err) {
                console.warn('Tizen registerKeyBatch exception:', err);
            }
        }
    };

    SpatialNav.prototype.bindEvents = function() {
        var self = this;
        window.addEventListener('keydown', function(e) {
            self.handleKeyDown(e);
        });

        // Mouse hover focus support for testing on PC
        document.addEventListener('mouseover', function(e) {
            var target = e.target;
            while (target && target !== document.body) {
                if (target.classList && target.classList.contains('focusable')) {
                    if (target !== self.currentFocus) {
                        self.setFocus(target);
                    }
                    break;
                }
                target = target.parentElement;
            }
        });
    };

    SpatialNav.prototype.handleKeyDown = function(e) {
        var code = e.keyCode || e.which;

        // 1. Direct Video Player Navigation
        if (window.LiveTvPlayer && window.LiveTvPlayer.isActive()) {
            if (window.LiveTvPlayer.handleKey(code, e)) {
                e.preventDefault();
                return;
            }
        }

        // 2. Active Modal / Overlay Trapping
        var activeModal = this.getActiveModal();
        if (activeModal) {
            this.handleModalKeyDown(code, activeModal, e);
            return;
        }

        // 3. Normal Launcher Home Navigation (Pure Geometric Screen Coordinates)
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
                // Physical screen Left is ALWAYS screen Left
                this.moveFocus('left');
                e.preventDefault();
                break;
            case this.KEYS.RIGHT:
                // Physical screen Right is ALWAYS screen Right
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
                this.handleBack();
                e.preventDefault();
                break;
            case this.KEYS.CH_UP:
            case this.KEYS.PAGE_UP:
                if (window.LiveTvPlayer) window.LiveTvPlayer.nextChannel();
                e.preventDefault();
                break;
            case this.KEYS.CH_DOWN:
            case this.KEYS.PAGE_DOWN:
                if (window.LiveTvPlayer) window.LiveTvPlayer.prevChannel();
                e.preventDefault();
                break;
            case this.KEYS.MEDIA_PLAY_PAUSE:
            case this.KEYS.MEDIA_PLAY:
            case this.KEYS.MEDIA_PAUSE:
                if (window.RadioModule) window.RadioModule.togglePlay();
                e.preventDefault();
                break;
            case this.KEYS.RED:
                if (window.ModalsModule) window.ModalsModule.openAppManager();
                e.preventDefault();
                break;
            case this.KEYS.GREEN:
                if (window.ModalsModule) window.ModalsModule.openSports();
                e.preventDefault();
                break;
            case this.KEYS.YELLOW:
                if (window.RadioModule) window.RadioModule.openRecitersModal();
                e.preventDefault();
                break;
            case this.KEYS.BLUE:
                if (window.ModalsModule) window.ModalsModule.openThemes();
                e.preventDefault();
                break;
            default:
                if (code >= this.KEYS.NUM_0 && code <= this.KEYS.NUM_9) {
                    var digit = code - this.KEYS.NUM_0;
                    if (window.LiveTvPlayer) {
                        window.LiveTvPlayer.handleDigit(digit);
                    }
                }
                break;
        }
    };

    SpatialNav.prototype.getActiveModal = function() {
        var modals = document.querySelectorAll('.modal-overlay.active, .drawer-overlay.active, .app-overlay.active');
        if (modals && modals.length > 0) {
            return modals[modals.length - 1];
        }
        return null;
    };

    SpatialNav.prototype.handleModalKeyDown = function(code, modal, e) {
        if (code === this.KEYS.BACK_ESC || code === this.KEYS.BACK_TIZEN || 
            code === this.KEYS.BACK_WEBOS || code === this.KEYS.BACKSPACE) {
            this.closeTopModal();
            e.preventDefault();
            return;
        }

        if (code === this.KEYS.ENTER) {
            if (this.currentFocus && modal.contains(this.currentFocus)) {
                this.currentFocus.click();
            }
            e.preventDefault();
            return;
        }

        switch (code) {
            case this.KEYS.UP:
                this.moveFocusInContainer('up', modal);
                e.preventDefault();
                break;
            case this.KEYS.DOWN:
                this.moveFocusInContainer('down', modal);
                e.preventDefault();
                break;
            case this.KEYS.LEFT:
                this.moveFocusInContainer('left', modal);
                e.preventDefault();
                break;
            case this.KEYS.RIGHT:
                this.moveFocusInContainer('right', modal);
                e.preventDefault();
                break;
        }
    };

    SpatialNav.prototype.openModal = function(modalEl, initialFocusSelector) {
        if (!modalEl) return;
        if (!this.modalStack.length) {
            this.lastHomeFocus = this.currentFocus;
        }
        this.modalStack.push(modalEl);
        modalEl.classList.add('active');

        var focusTarget = null;
        if (initialFocusSelector) {
            focusTarget = modalEl.querySelector(initialFocusSelector);
        }
        if (!focusTarget) {
            focusTarget = modalEl.querySelector('.focusable');
        }
        if (focusTarget) {
            this.setFocus(focusTarget);
        }
    };

    SpatialNav.prototype.closeTopModal = function() {
        if (this.modalStack.length > 0) {
            var modal = this.modalStack.pop();
            modal.classList.remove('active');
        } else {
            var active = this.getActiveModal();
            if (active) active.classList.remove('active');
        }

        if (this.modalStack.length > 0) {
            var topModal = this.modalStack[this.modalStack.length - 1];
            var f = topModal.querySelector('.focusable');
            if (f) this.setFocus(f);
        } else if (this.lastHomeFocus && document.body.contains(this.lastHomeFocus)) {
            this.setFocus(this.lastHomeFocus);
        } else {
            var first = document.querySelector('.horizontal-shelf-row .focusable');
            if (first) this.setFocus(first);
        }
    };

    SpatialNav.prototype.handleBack = function() {
        if (this.modalStack.length > 0 || this.getActiveModal()) {
            this.closeTopModal();
        } else {
            if (window.ModalsModule) {
                window.ModalsModule.openLockScreen();
            }
        }
    };

    SpatialNav.prototype.moveFocus = function(direction) {
        var container = document.getElementById('mainContainer') || document.body;
        this.moveFocusInContainer(direction, container);
    };

    SpatialNav.prototype.moveFocusInContainer = function(direction, container) {
        if (!container) container = document.getElementById('mainContainer') || document.body;
        if (!container) return;

        var rawList = container.querySelectorAll('.focusable');
        var focusables = [];
        for (var idx = 0; idx < rawList.length; idx++) {
            var el = rawList[idx];
            if (!el.disabled && getComputedStyle(el).display !== 'none') {
                focusables.push(el);
            }
        }

        if (!focusables.length) return;

        if (!this.currentFocus || !container.contains(this.currentFocus)) {
            this.setFocus(focusables[0]);
            return;
        }

        var currentRect = this.currentFocus.getBoundingClientRect();
        var curCenterX = currentRect.left + currentRect.width / 2;
        var curCenterY = currentRect.top + currentRect.height / 2;

        var bestCandidate = null;
        var minDistance = Infinity;

        for (var i = 0; i < focusables.length; i++) {
            var candidate = focusables[i];
            if (candidate === this.currentFocus) continue;

            var r = candidate.getBoundingClientRect();
            var candCenterX = r.left + r.width / 2;
            var candCenterY = r.top + r.height / 2;

            var isValid = false;
            var primaryDiff = 0;
            var secondaryDiff = 0;

            if (direction === 'up') {
                // Must be physically higher on screen
                isValid = (candCenterY < curCenterY - 10) || (r.bottom <= currentRect.top + 8);
                if (isValid) {
                    primaryDiff = curCenterY - candCenterY;
                    secondaryDiff = Math.abs(curCenterX - candCenterX);
                }
            } else if (direction === 'down') {
                // Must be physically lower on screen
                isValid = (candCenterY > curCenterY + 10) || (r.top >= currentRect.bottom - 8);
                if (isValid) {
                    primaryDiff = candCenterY - curCenterY;
                    secondaryDiff = Math.abs(curCenterX - candCenterX);
                }
            } else if (direction === 'left') {
                // Physical screen Left: center is to the left of current center
                isValid = (candCenterX < curCenterX - 8) || (r.right <= currentRect.left + 8);
                if (isValid) {
                    primaryDiff = curCenterX - candCenterX;
                    secondaryDiff = Math.abs(curCenterY - candCenterY);
                }
            } else if (direction === 'right') {
                // Physical screen Right: center is to the right of current center
                isValid = (candCenterX > curCenterX + 8) || (r.left >= currentRect.right - 8);
                if (isValid) {
                    primaryDiff = candCenterX - curCenterX;
                    secondaryDiff = Math.abs(curCenterY - candCenterY);
                }
            }

            if (isValid) {
                // Weighted distance formula: penalize perpendicular deviation to prefer elements in same row/column
                var distance = 0;
                if (direction === 'left' || direction === 'right') {
                    distance = primaryDiff * 1.0 + secondaryDiff * 3.0;
                } else {
                    distance = primaryDiff * 1.0 + secondaryDiff * 1.8;
                }

                if (distance < minDistance) {
                    minDistance = distance;
                    bestCandidate = candidate;
                }
            }
        }

        if (bestCandidate) {
            this.setFocus(bestCandidate);
        }
    };

    SpatialNav.prototype.setFocus = function(element) {
        if (!element) return;

        if (this.currentFocus) {
            this.currentFocus.classList.remove('focused');
            this.currentFocus.blur();
        }

        this.currentFocus = element;
        element.classList.add('focused');
        element.focus();

        // 1. Horizontal smooth centering in parent scroll row
        var parentRow = element.closest('.horizontal-shelf-row') || 
                        element.closest('.satellites-tabs-group') || 
                        element.closest('.categories-filter-row');
        if (parentRow && typeof parentRow.scrollBy === 'function' && typeof parentRow.getBoundingClientRect === 'function') {
            try {
                var rowRect = parentRow.getBoundingClientRect();
                var elRect = element.getBoundingClientRect();
                var scrollOffset = (elRect.left + elRect.width / 2) - (rowRect.left + rowRect.width / 2);
                parentRow.scrollBy({ left: scrollOffset, behavior: 'smooth' });
            } catch(e) {}
        }

        // 2. Vertical smooth centering in viewport/modal
        if (typeof element.scrollIntoView === 'function') {
            try {
                element.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });
            } catch(e) {}
        }
    };

    window.SpatialNav = new SpatialNav();

})(window);
