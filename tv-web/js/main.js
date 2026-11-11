/**
 * Mishkat Smart TV - Main Application Orchestrator
 * High-performance 4K TV Dashboard with Tizen API, Offline-First Embedded Data & 20 Themes
 * Pure ES6 (Chromium 76 Safe)
 */

(function(window) {
    'use strict';

    function initMishkatLauncher() {
        console.log('=== Initializing Mishkat Launcher v2.0.0 (Tizen Chromium 76 Engine) ===');

        try {
            // Give DOM 150ms to settle then set focus on the first app
            setTimeout(function() {
                var firstApp = document.querySelector('.horizontal-shelf-row .app-card');
                if (!firstApp) {
                    firstApp = document.querySelector('.horizontal-shelf-row .focusable');
                }
                if (!firstApp) {
                    firstApp = document.querySelector('.focusable');
                }
                if (firstApp && window.SpatialNav) {
                    window.SpatialNav.setFocus(firstApp);
                    console.log('Initial spatial focus set on:', firstApp);
                }
            }, 300);

            // Register global shortcut to open Settings with Blue key or remote
            document.addEventListener('keydown', function(e) {
                if (e.keyCode === 406 && window.ModalsModule) { // Blue key
                    window.ModalsModule.openThemes();
                }
            });

            console.log('Mishkat Launcher initialized successfully.');
        } catch (err) {
            console.error('Fatal initialization error caught safely:', err);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initMishkatLauncher);
    } else {
        initMishkatLauncher();
    }

})(window);
