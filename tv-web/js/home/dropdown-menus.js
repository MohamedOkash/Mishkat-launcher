/**
 * Mishkat Smart TV - Top Bar Luxury Dropdown Menus
 * Mirrors Android: TopBarDropdownMenus.kt
 * Pure ES6 (Chromium 76 Safe)
 */

(function(window) {
    'use strict';

    function DropdownMenus() {
        this.init();
    }

    DropdownMenus.prototype.init = function() {
        this.bindNavPills();
    };

    DropdownMenus.prototype.bindNavPills = function() {
        var btnInputs = document.getElementById('btnNavInputs');
        var btnTools = document.getElementById('btnNavTools');
        var btnThemes = document.getElementById('btnNavThemes');

        if (btnInputs) {
            btnInputs.addEventListener('click', function() {
                var modal = document.getElementById('inputsModal');
                if (modal && window.SpatialNav) {
                    window.SpatialNav.openModal(modal, '#btnInputTv');
                }
            });
        }

        if (btnTools) {
            btnTools.addEventListener('click', function() {
                var modal = document.getElementById('toolsModal');
                if (modal && window.SpatialNav) {
                    window.SpatialNav.openModal(modal, '#btnToolSports');
                }
            });
        }

        if (btnThemes) {
            btnThemes.addEventListener('click', function() {
                var modal = document.getElementById('appearanceModal');
                if (modal && window.SpatialNav) {
                    window.SpatialNav.openModal(modal, '#btnOpenThemes');
                }
            });
        }

        // Close buttons for modals
        var closeButtons = document.querySelectorAll('.modal-close-btn');
        for (var i = 0; i < closeButtons.length; i++) {
            closeButtons[i].addEventListener('click', function() {
                if (window.SpatialNav) {
                    window.SpatialNav.closeTopModal();
                }
            });
        }
    };

    window.DropdownMenus = new DropdownMenus();

})(window);
