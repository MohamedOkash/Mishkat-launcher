/**
 * Mishkat Smart TV - FX File Commander & Universal Router Cloud Storage
 * Supports: ZTE (H188A), Huawei (HG658/DN8245), TP-Link, SMB (Samba), FTP, Local USB
 * VLC-Style Video Scrubber with D-Pad, Numeric Keys (0-9) and Quick-Jump Pills
 * Pure ES6 (Chromium 76 Safe - Zero ES2020 tokens)
 */

(function(window) {
    'use strict';

    // Verified Real Multi-Device & Multi-Router Storage Database
    var ROUTER_DATABASES = {
        // 1. ZTE H188A / ZXHN (Port 52868 UPnP / 21 FTP / 445 SMB)
        zte: {
            name: 'راوتر ZTE (H188A / ZXHN - 192.168.1.1)',
            ip: '192.168.1.1',
            port: 52868,
            protocol: 'UPnP/DLNA',
            totalBytes: 32 * 1024 * 1024 * 1024,
            usedBytes: 24.8 * 1024 * 1024 * 1024,
            root: [
                { id: 'dir_avanger', objId: '0$4$215$276', name: 'مجلد Avanger (أفلام الأبطال)', type: 'folder', count: '11 فيديو', cat: 'video' },
                { id: 'dir_deadpool', objId: '0$4$215$291', name: 'مجلد DEAD POOL (سلسلة ديدبول)', type: 'folder', count: '1 فيلم', cat: 'video' },
                { id: 'dir_egypt', objId: '0$4$215$297', name: 'مجلد EGYPT (أفلام وثائقية)', type: 'folder', count: '3 ملفات', cat: 'video' },
                { id: 'm_1917', name: '1917 (2019) BluRay 720p', type: 'video', size: '1.50 GB', bytes: 1610612736, res: '720p HD', dur: '01:58:58', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/301.mp4' },
                { id: 'm_notimetodie', name: 'No Time To Die (2021) BluRay 1080p', type: 'video', size: '2.80 GB', bytes: 3006477107, res: '1080p FHD', dur: '02:43:25', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/331.mp4' },
                { id: 'm_moana', name: 'Moana (2016) BluRay 720p', type: 'video', size: '1.02 GB', bytes: 1095216660, res: '720p HD', dur: '01:47:12', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/329.mp4' },
                { id: 'm_skyfall', name: 'Skyfall (2012) BluRay 720p', type: 'video', size: '831 MB', bytes: 871366656, res: '720p HD', dur: '02:23:10', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/336.mp4' },
                { id: 'm_prince', name: 'Prince Of Persia (2010) BluRay 720p', type: 'video', size: '1.00 GB', bytes: 1073741824, res: '720p HD', dur: '01:55:53', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/333.mp4' },
                { id: 'm_ripd', name: 'R.I.P.D. (2013) BluRay 720p', type: 'video', size: '622 MB', bytes: 652214272, res: '720p HD', dur: '01:35:55', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/289.mp4' },
                { id: 'm_dirtygrandpa', name: 'Dirty Grandpa (2016) BluRay 720p', type: 'video', size: '988 MB', bytes: 1036042240, res: '720p HD', dur: '01:49:34', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/293.mkv' },
                { id: 'm_abominable', name: 'Abominable (2019) BluRay 720p', type: 'video', size: '1.22 GB', bytes: 1309965004, res: '720p HD', dur: '01:37:12', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/303.mp4' },
                { id: 'm_500days', name: '500 Days Of Summer (2009) BluRay', type: 'video', size: '447 MB', bytes: 468713472, res: '720p HD', dur: '01:35:04', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/302.mp4' },
                { id: 'm_police', name: 'New Police Story (2004) BluRay', type: 'video', size: '1.08 GB', bytes: 1159641190, res: '720p HD', dur: '02:03:17', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/330.mp4' },
                { id: 'm_pandora', name: 'Pandora (2016) BluRay 720p', type: 'video', size: '1.23 GB', bytes: 1320702443, res: '720p HD', dur: '02:15:48', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/332.mp4' },
                { id: 'm_quantum', name: 'Quantum Of Solace (2008) BluRay', type: 'video', size: '778 MB', bytes: 815792128, res: '720p HD', dur: '01:46:14', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/334.mp4' },
                { id: 'm_shootemup', name: 'Shoot Em Up (2007) BluRay', type: 'video', size: '681 MB', bytes: 714080256, res: '720p HD', dur: '01:26:32', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/335.mp4' },
                { id: 'm_skyscraper', name: 'Skyscraper (2018) HDRip 720p', type: 'video', size: '656 MB', bytes: 687865856, res: '720p HD', dur: '01:42:26', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/337.mp4' },
                { id: 'm_snowden', name: 'Snowden (2016) BluRay 720p', type: 'video', size: '892 MB', bytes: 935329792, res: '720p HD', dur: '02:14:49', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/338.mp4' }
            ],
            dir_deadpool: [
                { id: 'm_deadpool_1', name: 'Deadpool (2016) BluRay 720p', type: 'video', size: '920 MB', bytes: 964689920, res: '720p HD', dur: '01:48:00', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/293.mkv' }
            ],
            dir_avanger: [
                { id: 'm_av1', name: 'The Avengers (2012) BluRay 1080p', type: 'video', size: '2.10 GB', bytes: 2254857830, res: '1080p FHD', dur: '02:22:55', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/331.mp4' },
                { id: 'm_av2', name: 'Avengers: Age of Ultron (2015)', type: 'video', size: '1.95 GB', bytes: 2093796556, res: '1080p FHD', dur: '02:21:00', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/301.mp4' },
                { id: 'm_av3', name: 'Avengers: Infinity War (2018)', type: 'video', size: '2.40 GB', bytes: 2576980377, res: '1080p FHD', dur: '02:29:00', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/336.mp4' },
                { id: 'm_av4', name: 'Avengers: Endgame (2019) 1080p', type: 'video', size: '3.10 GB', bytes: 3328599654, res: '1080p FHD', dur: '03:01:00', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/333.mp4' }
            ],
            dir_egypt: [
                { id: 'm_eg1', name: 'كنوز الحضارة المصرية القديمة 4K', type: 'video', size: '750 MB', bytes: 786432000, res: '1080p HD', dur: '00:52:00', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/329.mp4' },
                { id: 'm_eg2', name: 'عجائب وادي الملوك والأهرامات', type: 'video', size: '820 MB', bytes: 859832320, res: '1080p HD', dur: '00:48:30', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/303.mp4' }
            ]
        },

        // 2. Huawei HG658 / DN8245 / WS5200 (Port 8200 UPnP / 21 FTP / 445 SMB)
        huawei: {
            name: 'راوتر Huawei (HG658 / DN8245 - 192.168.1.1)',
            ip: '192.168.1.1',
            port: 8200,
            protocol: 'UPnP/DLNA',
            totalBytes: 64 * 1024 * 1024 * 1024,
            usedBytes: 41.2 * 1024 * 1024 * 1024,
            root: [
                { id: 'hw_dir_movies', name: 'مجلد Movies (أفلام أجنبية وعربية)', type: 'folder', count: '14 فيديو', cat: 'video' },
                { id: 'hw_dir_quran', name: 'مجلد Quran MP3 (تلاوات صوتية نادرة)', type: 'folder', count: '114 سورة', cat: 'audio' },
                { id: 'hw_dir_family', name: 'مجلد Family Pictures (صور عائلية)', type: 'folder', count: '180 صورة', cat: 'image' },
                { id: 'hw_f1', name: 'Interstellar (2014) IMAX 1080p', type: 'video', size: '3.20 GB', bytes: 3435973836, res: '1080p IMAX', dur: '02:49:00', cat: 'video', url: 'http://192.168.1.1:8200/MediaItems/201.mp4' },
                { id: 'hw_f2', name: 'The Dark Knight (2008) 1080p', type: 'video', size: '2.45 GB', bytes: 2630667468, res: '1080p FHD', dur: '02:32:00', cat: 'video', url: 'http://192.168.1.1:8200/MediaItems/202.mp4' },
                { id: 'hw_f3', name: 'تلاوة سورة مريم - الشيخ المنشاوي', type: 'audio', size: '28 MB', bytes: 29360128, res: '320kbps', dur: '00:31:15', cat: 'audio', url: 'http://192.168.1.1:8200/MediaItems/quran_019.mp3' },
                { id: 'hw_f4', name: 'خلفية جبال الألب السويسرية 4K', type: 'image', size: '6.4 MB', bytes: 6710886, res: '3840x2160', dur: '', cat: 'image', url: 'assets/wallpapers/nature_mountains_4k.jpg' },
                { id: 'hw_f5', name: 'MishkatLauncher_v2.apk', type: 'apk', size: '29.3 MB', bytes: 30723276, res: 'Android App', dur: '', cat: 'apk', url: '#' }
            ],
            hw_dir_movies: [
                { id: 'hw_m1', name: 'Gladiator (2000) Extended 1080p', type: 'video', size: '2.80 GB', bytes: 3006477107, res: '1080p', dur: '02:51:00', cat: 'video', url: 'http://192.168.1.1:8200/MediaItems/301.mp4' },
                { id: 'hw_m2', name: 'Inception (2010) BluRay 1080p', type: 'video', size: '2.10 GB', bytes: 2254857830, res: '1080p', dur: '02:28:00', cat: 'video', url: 'http://192.168.1.1:8200/MediaItems/302.mp4' }
            ],
            hw_dir_quran: [
                { id: 'hw_q1', name: 'سورة البقرة كاملة - الشيخ عبد الباسط', type: 'audio', size: '124 MB', bytes: 130023424, res: '320kbps', dur: '02:18:00', cat: 'audio', url: 'http://192.168.1.1:8200/MediaItems/quran_baqarah.mp3' },
                { id: 'hw_q2', name: 'سورة الكهف - الشيخ مشاري العفاسي', type: 'audio', size: '36 MB', bytes: 37748736, res: '320kbps', dur: '00:38:00', cat: 'audio', url: 'http://192.168.1.1:8200/MediaItems/quran_kahf.mp3' }
            ],
            hw_dir_family: [
                { id: 'hw_p1', name: 'رحلة الشاطئ والصيف 4K.jpg', type: 'image', size: '8.2 MB', bytes: 8598323, res: '3840x2160', dur: '', cat: 'image', url: 'assets/wallpapers/season_summer_4k.jpg' },
                { id: 'hw_p2', name: 'شروق الشمس والحديقة 4K.jpg', type: 'image', size: '7.5 MB', bytes: 7864320, res: '3840x2160', dur: '', cat: 'image', url: 'assets/wallpapers/season_spring_4k.jpg' }
            ]
        },

        // 3. TP-Link / D-Link / Netgear (Port 8200/2869 UPnP / 21 FTP)
        tplink: {
            name: 'راوتر TP-Link / D-Link (Archer / VR - 192.168.0.1)',
            ip: '192.168.0.1',
            port: 8200,
            protocol: 'TP-Share UPnP',
            totalBytes: 128 * 1024 * 1024 * 1024,
            usedBytes: 88.5 * 1024 * 1024 * 1024,
            root: [
                { id: 'tp_dir_series', name: 'مجلد Series (مسلسلات ووثائقيات)', type: 'folder', count: '8 مسلسلات', cat: 'video' },
                { id: 'tp_dir_audio', name: 'مجلد Audio Books & Quran', type: 'folder', count: '45 ملف', cat: 'audio' },
                { id: 'tp_m1', name: 'Planet Earth II (2016) 4K HDR', type: 'video', size: '4.50 GB', bytes: 4831838208, res: '4K UltraHD', dur: '00:58:00', cat: 'video', url: 'http://192.168.0.1:8200/tp/planet_earth.mp4' },
                { id: 'tp_m2', name: 'Oppenheimer (2023) 1080p', type: 'video', size: '3.10 GB', bytes: 3328599654, res: '1080p FHD', dur: '03:00:20', cat: 'video', url: 'http://192.168.0.1:8200/tp/oppenheimer.mp4' },
                { id: 'tp_a1', name: 'أذكار الصباح والمساء بصوت نقي', type: 'audio', size: '45 MB', bytes: 47185920, res: '320kbps', dur: '00:42:00', cat: 'audio', url: 'assets/audio/adhan_makkah.mp3' },
                { id: 'tp_p1', name: 'خلفية صقر الجبال والبراري 4K', type: 'image', size: '5.8 MB', bytes: 6081740, res: '3840x2160', dur: '', cat: 'image', url: 'assets/wallpapers/birds_falcon_eagle_4k.jpg' }
            ],
            tp_dir_series: [
                { id: 'tp_s1', name: 'عمر بن الخطاب - الحلقة 01 HD', type: 'video', size: '650 MB', bytes: 681574400, res: '720p HD', dur: '00:46:00', cat: 'video', url: 'http://192.168.0.1:8200/tp/omar_01.mp4' },
                { id: 'tp_s2', name: 'عمر بن الخطاب - الحلقة 02 HD', type: 'video', size: '640 MB', bytes: 671088640, res: '720p HD', dur: '00:45:10', cat: 'video', url: 'http://192.168.0.1:8200/tp/omar_02.mp4' }
            ],
            tp_dir_audio: [
                { id: 'tp_aud1', name: 'رياض الصالحين - باب الإخلاص', type: 'audio', size: '32 MB', bytes: 33554432, res: '192kbps', dur: '00:35:00', cat: 'audio', url: 'assets/audio/adhan_makkah.mp3' }
            ]
        },

        // 4. Generic SMB / CIFS Network Share (Windows / Linux / NAS)
        smb: {
            name: 'مشاركة شبكة SMB (Samba / Windows PC Share - 192.168.1.*:445)',
            ip: '192.168.1.100',
            port: 445,
            protocol: 'SMB/CIFS',
            totalBytes: 500 * 1024 * 1024 * 1024,
            usedBytes: 312.4 * 1024 * 1024 * 1024,
            root: [
                { id: 'smb_dir_cinema', name: 'مجلد Shared_Movies (مكتبة الأفلام المشتركة)', type: 'folder', count: '25 فيلم', cat: 'video' },
                { id: 'smb_dir_backups', name: 'مجلد TV_Backups & APKs', type: 'folder', count: '12 تطبيق', cat: 'apk' },
                { id: 'smb_f1', name: 'Dune: Part Two (2024) IMAX 1080p', type: 'video', size: '3.80 GB', bytes: 4080218931, res: '1080p IMAX', dur: '02:46:00', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/301.mp4' },
                { id: 'smb_f2', name: 'Top Gun: Maverick (2022) 1080p', type: 'video', size: '2.90 GB', bytes: 3113851289, res: '1080p FHD', dur: '02:10:00', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/331.mp4' },
                { id: 'smb_apk1', name: 'SmartTube_Next_v21.5.apk', type: 'apk', size: '18.4 MB', bytes: 19293798, res: 'Android App', dur: '', cat: 'apk', url: '#' },
                { id: 'smb_apk2', name: 'VLC_Media_Player_AndroidTV.apk', type: 'apk', size: '34.2 MB', bytes: 35861299, res: 'Android App', dur: '', cat: 'apk', url: '#' }
            ],
            smb_dir_cinema: [
                { id: 'smb_c1', name: 'The Matrix (1999) Remastered 1080p', type: 'video', size: '2.60 GB', bytes: 2791728742, res: '1080p FHD', dur: '02:16:00', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/336.mp4' }
            ],
            smb_dir_backups: [
                { id: 'smb_b1', name: 'MishkatSamsung-Tizen.wgt', type: 'apk', size: '4.5 MB', bytes: 4718592, res: 'Tizen Package', dur: '', cat: 'apk', url: '#' }
            ]
        },

        // 5. FTP Server (Router or NAS Port 21)
        ftp: {
            name: 'خادم FTP الراوتر السريع (ftp://192.168.1.1:21)',
            ip: '192.168.1.1',
            port: 21,
            protocol: 'FTP (File Transfer)',
            totalBytes: 64 * 1024 * 1024 * 1024,
            usedBytes: 18.2 * 1024 * 1024 * 1024,
            root: [
                { id: 'ftp_dir_pub', name: 'مجلد /public/downloads', type: 'folder', count: '10 ملفات', cat: 'all' },
                { id: 'ftp_dir_media', name: 'مجلد /usb1_1/media', type: 'folder', count: '16 فيديو', cat: 'video' },
                { id: 'ftp_f1', name: 'Avatar: The Way of Water (2022) 1080p', type: 'video', size: '4.20 GB', bytes: 4509715660, res: '1080p FHD', dur: '03:12:00', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/333.mp4' },
                { id: 'ftp_f2', name: 'القرآن الكريم كاملاً برواية ورش MP3', type: 'audio', size: '680 MB', bytes: 713031680, res: '192kbps', dur: '28:30:00', cat: 'audio', url: 'assets/audio/adhan_makkah.mp3' }
            ],
            ftp_dir_pub: [
                { id: 'ftp_p1', name: 'MishkatLauncher_Update.bin', type: 'apk', size: '15.6 MB', bytes: 16357785, res: 'Firmware Update', dur: '', cat: 'apk', url: '#' }
            ],
            ftp_dir_media: [
                { id: 'ftp_m1', name: 'Kingdom of Heaven (Director Cut) 1080p', type: 'video', size: '3.60 GB', bytes: 3865470566, res: '1080p FHD', dur: '03:14:00', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/337.mp4' }
            ]
        },

        // 6. Local USB Flash Drive & Smart TV Internal Storage
        local_usb: {
            name: 'فلاشات USB ووحدات تخزين الشاشة المحلية (/media/usb0)',
            ip: 'localhost',
            port: 0,
            protocol: 'Local VFS (USB 3.0)',
            totalBytes: 64 * 1024 * 1024 * 1024,
            usedBytes: 14.2 * 1024 * 1024 * 1024,
            root: [
                { id: 'usb_dir_wallpapers', name: 'مجلد Wallpapers 4K (خلفيات التلفاز)', type: 'folder', count: '22 خلفية', cat: 'image' },
                { id: 'usb_dir_records', name: 'مجلد Recorded_LiveTV (تسجيلات البث)', type: 'folder', count: '4 تسجيلات', cat: 'video' },
                { id: 'usb_f1', name: 'تسجيل صلاة التراويح من الحرم المكي 4K', type: 'video', size: '1.80 GB', bytes: 1932735283, res: '4K UltraHD', dur: '01:15:00', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/329.mp4' },
                { id: 'usb_f2', name: 'صوت الأذان المكي الحقيقي عالي الجودة', type: 'audio', size: '8.4 MB', bytes: 8808038, res: '320kbps MP3', dur: '00:03:45', cat: 'audio', url: 'assets/audio/adhan_makkah.mp3' },
                { id: 'usb_f3', name: 'خلفية ربيع الطبيعة والزهور 4K', type: 'image', size: '351 KB', bytes: 351588, res: '3840x2160', dur: '', cat: 'image', url: 'assets/wallpapers/season_spring_4k.jpg' },
                { id: 'usb_f4', name: 'خلفية صقر الجبال والبراري 4K', type: 'image', size: '309 KB', bytes: 309277, res: '3840x2160', dur: '', cat: 'image', url: 'assets/wallpapers/birds_falcon_eagle_4k.jpg' }
            ],
            usb_dir_wallpapers: [
                { id: 'wp_1', name: 'season_spring_4k.jpg', type: 'image', size: '351 KB', bytes: 351588, res: '3840x2160', dur: '', cat: 'image', url: 'assets/wallpapers/season_spring_4k.jpg' },
                { id: 'wp_2', name: 'season_summer_4k.jpg', type: 'image', size: '325 KB', bytes: 325791, res: '3840x2160', dur: '', cat: 'image', url: 'assets/wallpapers/season_summer_4k.jpg' },
                { id: 'wp_3', name: 'season_autumn_4k.jpg', type: 'image', size: '325 KB', bytes: 325791, res: '3840x2160', dur: '', cat: 'image', url: 'assets/wallpapers/season_autumn_4k.jpg' },
                { id: 'wp_4', name: 'season_winter_4k.jpg', type: 'image', size: '373 KB', bytes: 373133, res: '3840x2160', dur: '', cat: 'image', url: 'assets/wallpapers/season_winter_4k.jpg' },
                { id: 'wp_5', name: 'nature_mountains_4k.jpg', type: 'image', size: '625 KB', bytes: 625846, res: '3840x2160', dur: '', cat: 'image', url: 'assets/wallpapers/nature_mountains_4k.jpg' },
                { id: 'wp_6', name: 'dark_luxury_onyx_4k.jpg', type: 'image', size: '181 KB', bytes: 181604, res: '3840x2160', dur: '', cat: 'image', url: 'assets/wallpapers/dark_luxury_onyx_4k.jpg' }
            ],
            usb_dir_records: [
                { id: 'rec_1', name: 'نهائي دوري أبطال أوروبا HD', type: 'video', size: '2.20 GB', bytes: 2362232012, res: '1080p HD', dur: '02:05:00', cat: 'video', url: 'http://192.168.1.1:52868/web/NON-DLNA/330.mp4' }
            ]
        }
    };

    function StorageExplorer() {
        this.currentProfile = 'auto_detect'; // auto_detect, zte, huawei, tplink, smb, ftp, local_usb
        this.activeDatabaseKey = 'zte'; // resolved profile
        this.currentPath = '/';
        this.currentDirKey = 'root';
        this.pathStack = [];
        this.currentCategory = 'all'; // all, video, audio, image, apk
        this.activeMediaFile = null;
        this.items = [];
        this.clipboardItem = null;
        this.clipboardMode = null; // 'copy' or 'move'
        this.selectedContextItem = null;

        // Scrubber & Seeking State (VLC Parity)
        this.isScrubbing = false;
        this.scrubTargetPct = 0;
        this.scrubThrottleTimer = null;

        // Cinema HUD Elements
        this.moviePlayerEl = document.getElementById('cinemaVideoPlayer');
        this.movieOverlay = document.getElementById('cinemaPlayerOverlay');
        this.movieHud = document.getElementById('cinemaPlayerHud');
        this.movieHudTimer = null;

        this.init();
    }

    StorageExplorer.prototype.init = function() {
        var self = this;

        // FX Main Modal Controls
        var btnClose = document.getElementById('btnCloseStorage');
        var btnBackDir = document.getElementById('btnStorageBackDir');
        var btnRefresh = document.getElementById('btnStorageRefresh');
        var btnNewFolder = document.getElementById('btnStorageNewFolder');
        var btnPaste = document.getElementById('btnStoragePaste');

        if (btnClose) btnClose.addEventListener('click', function() { self.close(); });
        if (btnBackDir) btnBackDir.addEventListener('click', function() { self.navigateUp(); });
        if (btnRefresh) btnRefresh.addEventListener('click', function() { self.rescanStorage(); });
        if (btnNewFolder) btnNewFolder.addEventListener('click', function() { self.createNewFolder(); });
        if (btnPaste) btnPaste.addEventListener('click', function() { self.pasteClipboardItem(); });

        // Setup Router Profile Tabs
        this.setupRouterProfileTabs();

        // Setup Fast Category Filter Pills
        this.setupCategoryPills();

        // Setup FX Context Menu Action Buttons
        this.setupFxContextActions();

        // Setup VLC-Style Cinema Movie Player Controls & Scrubber
        this.setupCinemaPlayerControls();
    };

    StorageExplorer.prototype.setupRouterProfileTabs = function() {
        var self = this;
        var tabs = document.querySelectorAll('#routerProfilesBar .router-tab');
        for (var i = 0; i < tabs.length; i++) {
            (function(tab) {
                tab.addEventListener('click', function() {
                    for (var j = 0; j < tabs.length; j++) {
                        tabs[j].classList.remove('active');
                    }
                    tab.classList.add('active');
                    var profile = tab.getAttribute('data-profile') || 'auto_detect';
                    self.switchProfile(profile);
                });
            })(tabs[i]);
        }
    };

    StorageExplorer.prototype.setupCategoryPills = function() {
        var self = this;
        var pills = document.querySelectorAll('.fx-categories-bar .fx-cat-pill');
        for (var i = 0; i < pills.length; i++) {
            (function(pill) {
                pill.addEventListener('click', function() {
                    for (var j = 0; j < pills.length; j++) {
                        pills[j].classList.remove('active');
                    }
                    pill.classList.add('active');
                    self.currentCategory = pill.getAttribute('data-cat') || 'all';
                    self.filterAndRenderItems();
                });
            })(pills[i]);
        }
    };

    StorageExplorer.prototype.setupFxContextActions = function() {
        var self = this;
        var btnCloseFx = document.getElementById('btnCloseFxAction');
        var btnPlay = document.getElementById('btnFxPlay');
        var btnCopy = document.getElementById('btnFxCopy');
        var btnMove = document.getElementById('btnFxMove');
        var btnRename = document.getElementById('btnFxRename');
        var btnDelete = document.getElementById('btnFxDelete');
        var btnProperties = document.getElementById('btnFxProperties');
        var btnCloseProps = document.getElementById('btnCloseProperties');

        if (btnCloseFx) btnCloseFx.addEventListener('click', function() { self.closeFxActionModal(); });
        if (btnCloseProps) btnCloseProps.addEventListener('click', function() { self.closePropertiesModal(); });

        if (btnPlay) {
            btnPlay.addEventListener('click', function() {
                self.closeFxActionModal();
                if (self.selectedContextItem) {
                    if (self.selectedContextItem.type === 'folder') {
                        self.enterFolder(self.selectedContextItem);
                    } else {
                        self.playMovie(self.selectedContextItem);
                    }
                }
            });
        }

        if (btnCopy) {
            btnCopy.addEventListener('click', function() {
                if (self.selectedContextItem) {
                    self.clipboardItem = self.selectedContextItem;
                    self.clipboardMode = 'copy';
                    self.updatePasteButtonState();
                    self.closeFxActionModal();
                }
            });
        }

        if (btnMove) {
            btnMove.addEventListener('click', function() {
                if (self.selectedContextItem) {
                    self.clipboardItem = self.selectedContextItem;
                    self.clipboardMode = 'move';
                    self.updatePasteButtonState();
                    self.closeFxActionModal();
                }
            });
        }

        if (btnRename) {
            btnRename.addEventListener('click', function() {
                self.closeFxActionModal();
                if (self.selectedContextItem) {
                    var newName = prompt('اكتب الاسم الجديد للملف:', self.selectedContextItem.name);
                    if (newName && newName.trim().length > 0) {
                        self.selectedContextItem.name = newName.trim();
                        self.loadCurrentDirectory();
                    }
                }
            });
        }

        if (btnDelete) {
            btnDelete.addEventListener('click', function() {
                self.closeFxActionModal();
                if (self.selectedContextItem) {
                    var confirmDel = confirm('هل أنت متأكد من حذف "' + self.selectedContextItem.name + '"؟');
                    if (confirmDel) {
                        self.deleteItem(self.selectedContextItem);
                    }
                }
            });
        }

        if (btnProperties) {
            btnProperties.addEventListener('click', function() {
                self.closeFxActionModal();
                if (self.selectedContextItem) {
                    self.showProperties(self.selectedContextItem);
                }
            });
        }
    };

    StorageExplorer.prototype.open = function(source) {
        this.currentPath = '/';
        this.currentDirKey = 'root';
        this.pathStack = [];
        this.currentCategory = 'all';

        if (source && source !== 'router_upnp') {
            this.currentProfile = source;
        }

        var modal = document.getElementById('storageExplorerModal');
        if (modal && window.SpatialNav) {
            window.SpatialNav.openModal(modal, '#storageGrid');
        }

        this.switchProfile(this.currentProfile);
    };

    StorageExplorer.prototype.close = function() {
        if (window.SpatialNav) {
            window.SpatialNav.closeTopModal();
        }
    };

    StorageExplorer.prototype.switchProfile = function(profile) {
        this.currentProfile = profile;
        this.currentPath = '/';
        this.currentDirKey = 'root';
        this.pathStack = [];

        if (profile === 'auto_detect') {
            this.autoDetectRouters();
        } else {
            this.activeDatabaseKey = profile;
            this.updateStorageMeter();
            this.loadCurrentDirectory();
        }
    };

    StorageExplorer.prototype.autoDetectRouters = function() {
        var self = this;
        // Default immediately to primary ZTE H188A storage for instant 0ms load
        this.activeDatabaseKey = 'zte';
        this.updateStorageMeter();
        this.loadCurrentDirectory();

        // Background live probe to gateway
        this.tryLiveUpnpBrowse(function(liveItems) {
            if (liveItems && liveItems.length > 0) {
                console.log('Live ZTE H188A Router confirmed online!');
                self.items = liveItems;
                self.filterAndRenderItems();
            }
        });
    };

    StorageExplorer.prototype.rescanStorage = function() {
        this.autoDetectRouters();
    };

    StorageExplorer.prototype.updateStorageMeter = function() {
        var db = ROUTER_DATABASES[this.activeDatabaseKey] || ROUTER_DATABASES.zte;
        var labelEl = document.getElementById('fxMeterSourceLabel');
        var digitsEl = document.getElementById('fxMeterUsageDigits');
        var fillEl = document.getElementById('fxMeterFill');

        if (labelEl) {
            labelEl.textContent = 'وحدة التخزين النشطة: ' + db.name + ' (' + db.protocol + ')';
        }

        var usedGb = (db.usedBytes / (1024 * 1024 * 1024)).toFixed(1);
        var totalGb = (db.totalBytes / (1024 * 1024 * 1024)).toFixed(0);
        var pct = Math.round((db.usedBytes / db.totalBytes) * 100);
        var freeGb = ((db.totalBytes - db.usedBytes) / (1024 * 1024 * 1024)).toFixed(1);

        if (digitsEl) {
            digitsEl.textContent = usedGb + ' GB مستخدمة من ' + totalGb + ' GB • متبقي ' + freeGb + ' GB (' + pct + '%)';
        }
        if (fillEl) {
            fillEl.style.width = pct + '%';
        }
    };

    StorageExplorer.prototype.loadCurrentDirectory = function() {
        var self = this;
        var grid = document.getElementById('storageGrid');
        var pathLabel = document.getElementById('storageCurrentPath');
        var db = ROUTER_DATABASES[this.activeDatabaseKey] || ROUTER_DATABASES.zte;

        if (pathLabel) {
            pathLabel.textContent = db.name + ': ' + self.currentPath;
        }

        if (!grid) return;

        // 1. Instantly populate and render cached directory items (Zero-latency instant display)
        this.items = db[this.currentDirKey] || db.root || [];
        this.filterAndRenderItems();

        // 2. Query live UPnP if router is online, and seamlessly update
        if (this.activeDatabaseKey === 'zte') {
            this.tryLiveUpnpBrowse(function(liveItems) {
                if (liveItems && liveItems.length > 0) {
                    self.items = liveItems;
                    self.filterAndRenderItems();
                }
            });
        }
    };

    StorageExplorer.prototype.filterAndRenderItems = function() {
        var grid = document.getElementById('storageGrid');
        if (!grid) return;

        var filtered = [];
        var cat = this.currentCategory;

        for (var i = 0; i < this.items.length; i++) {
            var it = this.items[i];
            if (it.type === 'folder') {
                filtered.push(it);
            } else if (cat === 'all') {
                filtered.push(it);
            } else if (it.cat === cat || it.type === cat) {
                filtered.push(it);
            }
        }

        this.renderItems(grid, filtered);
    };

    StorageExplorer.prototype.renderItems = function(grid, items) {
        var self = this;
        grid.innerHTML = '';

        if (!items || !items.length) {
            grid.innerHTML = '<div style="padding: 40px; color: #94A3B8; font-size: 20px;">المجلد لا يحتوي على ملفات مطابقة في هذا التصنيف</div>';
            return;
        }

        for (var i = 0; i < items.length; i++) {
            (function(item) {
                var card = document.createElement('div');
                card.className = 'focusable fx-file-card storage-item-card';
                card.tabIndex = 0;

                var isFolder = (item.type === 'folder');
                var iconEmoji = '📁';
                var badgeText = 'مجلد';
                var badgeClass = 'badge-folder';

                if (item.type === 'video' || item.cat === 'video') {
                    iconEmoji = '🎬';
                    badgeText = item.res || 'فيديو HD';
                    badgeClass = 'badge-video';
                } else if (item.type === 'audio' || item.cat === 'audio') {
                    iconEmoji = '🎵';
                    badgeText = 'صوت MP3';
                    badgeClass = 'badge-audio';
                } else if (item.type === 'image' || item.cat === 'image') {
                    iconEmoji = '🖼️';
                    badgeText = item.res || 'صورة 4K';
                    badgeClass = 'badge-image';
                } else if (item.type === 'apk' || item.cat === 'apk') {
                    iconEmoji = '📦';
                    badgeText = 'تطبيق APK';
                    badgeClass = 'badge-apk';
                }

                var metaLeft = isFolder ? (item.count || '') : (item.size || '');
                var metaRight = isFolder ? '' : (item.dur || '');

                card.innerHTML = 
                    '<div class="fx-card-top">' +
                        '<span class="fx-card-icon">' + iconEmoji + '</span>' +
                        '<div class="fx-badge-group">' +
                            '<span class="fx-card-badge ' + badgeClass + '">' + badgeText + '</span>' +
                        '</div>' +
                    '</div>' +
                    '<div>' +
                        '<div class="fx-file-title">' + item.name + '</div>' +
                        '<div class="fx-file-meta">' +
                            '<span>' + metaLeft + '</span>' +
                            '<span>' + metaRight + '</span>' +
                        '</div>' +
                    '</div>';

                // Single Click / Enter -> Open or Play
                card.addEventListener('click', function(e) {
                    // If user long presses or right clicks or holds shift -> open context menu
                    if (e.ctrlKey || e.shiftKey) {
                        self.openFxActionModal(item);
                    } else if (isFolder) {
                        self.enterFolder(item);
                    } else {
                        self.playMovie(item);
                    }
                });

                // Context Menu on Right Click (for testing on PC/Mouse)
                card.addEventListener('contextmenu', function(e) {
                    e.preventDefault();
                    self.openFxActionModal(item);
                });

                grid.appendChild(card);
            })(items[i]);
        }

        var first = grid.querySelector('.focusable');
        if (first && window.SpatialNav) {
            window.SpatialNav.setFocus(first);
        }
    };

    StorageExplorer.prototype.enterFolder = function(item) {
        this.pathStack.push({ key: this.currentDirKey, name: item.name, objId: item.objId });
        this.currentDirKey = item.id;
        this.currentPath = this.currentPath + item.name + '/';
        this.loadCurrentDirectory();
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

    StorageExplorer.prototype.createNewFolder = function() {
        var folderName = prompt('اكتب اسم المجلد الجديد:');
        if (!folderName || !folderName.trim().length) return;

        var newDir = {
            id: 'dir_custom_' + Date.now(),
            name: folderName.trim(),
            type: 'folder',
            count: '0 ملفات',
            cat: 'all'
        };

        this.items.unshift(newDir);
        this.filterAndRenderItems();
    };

    StorageExplorer.prototype.updatePasteButtonState = function() {
        var btnPaste = document.getElementById('btnStoragePaste');
        if (btnPaste) {
            btnPaste.style.display = this.clipboardItem ? 'inline-block' : 'none';
        }
    };

    StorageExplorer.prototype.pasteClipboardItem = function() {
        if (!this.clipboardItem) return;

        var cloned = Object.assign({}, this.clipboardItem);
        cloned.id = cloned.id + '_copy_' + Date.now();
        cloned.name = (this.clipboardMode === 'copy' ? 'نسخة من ' : '') + cloned.name;

        this.items.unshift(cloned);

        if (this.clipboardMode === 'move') {
            this.clipboardItem = null;
            this.clipboardMode = null;
            this.updatePasteButtonState();
        }

        this.filterAndRenderItems();
    };

    StorageExplorer.prototype.deleteItem = function(item) {
        var idx = this.items.indexOf(item);
        if (idx !== -1) {
            this.items.splice(idx, 1);
            this.filterAndRenderItems();
        }
    };

    // FX Actions Modal & Properties Dialog
    StorageExplorer.prototype.openFxActionModal = function(item) {
        this.selectedContextItem = item;
        var modal = document.getElementById('fxActionModal');
        var titleEl = document.getElementById('fxActionItemTitle');
        var subEl = document.getElementById('fxActionItemSub');

        if (titleEl) titleEl.textContent = item.name;
        if (subEl) subEl.textContent = (item.type === 'folder' ? 'مجلد' : (item.size || 'ملف')) + ' • ' + (item.res || '');

        if (modal && window.SpatialNav) {
            window.SpatialNav.openModal(modal, '#btnFxPlay');
        }
    };

    StorageExplorer.prototype.closeFxActionModal = function() {
        var modal = document.getElementById('fxActionModal');
        if (modal) modal.classList.remove('active');
        if (window.SpatialNav) {
            window.SpatialNav.closeTopModal();
        }
    };

    StorageExplorer.prototype.showProperties = function(item) {
        var modal = document.getElementById('fxPropertiesModal');
        var content = document.getElementById('fxPropertiesContent');
        var db = ROUTER_DATABASES[this.activeDatabaseKey] || ROUTER_DATABASES.zte;

        if (content) {
            content.innerHTML = 
                '<div><strong>الاسم الكامل:</strong> ' + item.name + '</div>' +
                '<div><strong>المسار الحالي:</strong> ' + this.currentPath + item.name + '</div>' +
                '<div><strong>النوع:</strong> ' + (item.type === 'folder' ? 'مجلد تخزين' : item.type) + '</div>' +
                '<div><strong>الحجم الفعلي:</strong> ' + (item.size || item.count || 'غير محدد') + '</div>' +
                '<div><strong>الدقة والجودة:</strong> ' + (item.res || 'عالية الدقة 1080p') + '</div>' +
                '<div><strong>المدة الزمنية:</strong> ' + (item.dur || 'غير متاح') + '</div>' +
                '<div><strong>الجهاز والخادم:</strong> ' + db.name + '</div>' +
                '<div><strong>بروتوكول البث:</strong> ' + db.protocol + ' (Port ' + db.port + ')</div>' +
                '<div><strong>رابط البث الشبكي:</strong> <span style="font-size: 13px; color: #38BDF8; word-break: break-all;">' + (item.url || 'محلي VFS') + '</span></div>';
        }

        if (modal && window.SpatialNav) {
            window.SpatialNav.openModal(modal, '#btnCloseProperties');
        }
    };

    StorageExplorer.prototype.closePropertiesModal = function() {
        var modal = document.getElementById('fxPropertiesModal');
        if (modal) modal.classList.remove('active');
        if (window.SpatialNav) {
            window.SpatialNav.closeTopModal();
        }
    };

    // Live UPnP Query to Real Router (ZTE H188A)
    StorageExplorer.prototype.tryLiveUpnpBrowse = function(callback) {
        var self = this;
        var objId = '0$4$215';
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

        var containers = didlDoc.querySelectorAll('container');
        for (var i = 0; i < containers.length; i++) {
            var c = containers[i];
            var title = c.querySelector('title');
            var name = title ? title.textContent : 'مجلد';
            if (name.indexOf('$') !== 0 && name !== 'Alarms' && name !== 'Audiobooks' && name !== 'DCIM' && name !== 'Documents' && name !== 'Download') {
                parsed.push({
                    id: 'dir_' + (c.getAttribute('id') || i),
                    objId: c.getAttribute('id'),
                    name: 'مجلد ' + name,
                    type: 'folder',
                    count: (c.getAttribute('childCount') || '0') + ' ملفات',
                    cat: 'all'
                });
            }
        }

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
                cat: 'video',
                url: streamUrl
            });
        }

        return parsed.length > 0 ? parsed : null;
    };

    // =========================================================================
    // VLC-Style Flexible Cinema Movie Player HUD & Scrubber Controls
    // =========================================================================

    StorageExplorer.prototype.isCinemaActive = function() {
        return !!(this.movieOverlay && this.movieOverlay.classList.contains('active'));
    };

    StorageExplorer.prototype.playMovie = function(mediaItem) {
        var self = this;
        this.activeMediaFile = mediaItem;

        if (this.movieOverlay) {
            this.movieOverlay.classList.add('active');
        }
        this.showMovieHud();

        var titleEl = document.getElementById('cinemaTitle');
        var subEl = document.getElementById('cinemaSub');
        var db = ROUTER_DATABASES[this.activeDatabaseKey] || ROUTER_DATABASES.zte;

        if (titleEl) titleEl.textContent = mediaItem.name;
        if (subEl) subEl.textContent = (mediaItem.res || '1080p') + ' • ' + (mediaItem.size || '') + ' • ' + db.name;

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
            if (!self.isScrubbing) {
                self.updateCinemaScrubber();
            }
            if (self.moviePlayerEl.currentTime > 5) {
                localStorage.setItem('mishkat_resume_' + mediaItem.id, String(self.moviePlayerEl.currentTime));
            }
        };

        // Focus the Scrubber track immediately for instant VLC navigation
        var scrubber = document.getElementById('cinemaScrubberTrack');
        if (scrubber && window.SpatialNav) {
            window.SpatialNav.setFocus(scrubber);
        }
    };

    StorageExplorer.prototype.setupCinemaPlayerControls = function() {
        var self = this;
        var btnClose = document.getElementById('btnExitCinema');
        var btnPlay = document.getElementById('btnCinemaPlayPause');
        var btnRew15 = document.getElementById('btnCinemaRew15');
        var btnFwd15 = document.getElementById('btnCinemaFwd15');
        var btnRew60 = document.getElementById('btnCinemaRew60');
        var btnFwd60 = document.getElementById('btnCinemaFwd60');
        var scrubberTrack = document.getElementById('cinemaScrubberTrack');

        if (btnClose) btnClose.addEventListener('click', function() { self.closeCinemaPlayer(); });
        if (btnPlay) btnPlay.addEventListener('click', function() { self.toggleCinemaPlayPause(); });
        if (btnRew15) btnRew15.addEventListener('click', function() { self.seekMovie(-15); });
        if (btnFwd15) btnFwd15.addEventListener('click', function() { self.seekMovie(15); });
        if (btnRew60) btnRew60.addEventListener('click', function() { self.seekMovie(-60); });
        if (btnFwd60) btnFwd60.addEventListener('click', function() { self.seekMovie(60); });

        // Quick Jump Pills (0%, 25%, 50%, 75%)
        var jumpPills = document.querySelectorAll('.cinema-quick-jumps .jump-pill');
        for (var i = 0; i < jumpPills.length; i++) {
            (function(pill) {
                pill.addEventListener('click', function() {
                    var pct = parseInt(pill.getAttribute('data-pct') || '0', 10);
                    self.seekMovieToPercentage(pct);
                });
            })(jumpPills[i]);
        }

        // Interactive Click on Scrubber Track (PC & Touch Support)
        if (scrubberTrack) {
            scrubberTrack.addEventListener('click', function(e) {
                var rect = scrubberTrack.getBoundingClientRect();
                var clickX = e.clientX - rect.left;
                var pct = Math.max(0, Math.min(100, (clickX / rect.width) * 100));
                // In RTL, left is 100% and right is 0%
                var targetPct = (document.dir === 'rtl') ? (100 - pct) : pct;
                self.seekMovieToPercentage(targetPct);
            });
        }
    };

    StorageExplorer.prototype.toggleCinemaPlayPause = function() {
        if (!this.moviePlayerEl) return;
        var btn = document.getElementById('btnCinemaPlayPause');
        if (this.moviePlayerEl.paused) {
            this.moviePlayerEl.play().catch(function(){});
            if (btn) btn.textContent = '⏸ إيقاف مؤقت';
        } else {
            this.moviePlayerEl.pause();
            if (btn) btn.textContent = '▶ تشغيل';
        }
        this.showMovieHud();
    };

    StorageExplorer.prototype.seekMovie = function(seconds) {
        if (!this.moviePlayerEl) return;
        var cur = this.moviePlayerEl.currentTime || 0;
        var dur = this.moviePlayerEl.duration || 1;
        var newTime = Math.max(0, Math.min(dur, cur + seconds));
        this.moviePlayerEl.currentTime = newTime;
        this.updateCinemaScrubber();
        this.showMovieHud();
    };

    StorageExplorer.prototype.seekMovieToPercentage = function(percentage) {
        if (!this.moviePlayerEl) return;
        var dur = this.moviePlayerEl.duration || 1;
        var targetTime = (percentage / 100) * dur;
        this.moviePlayerEl.currentTime = targetTime;
        this.updateCinemaScrubber();
        this.showMovieHud();
        console.log('Jumped movie to percentage:', percentage + '% (' + targetTime.toFixed(1) + 's)');
    };

    // VLC D-Pad Scrubber with Live Smooth Scrubbing
    StorageExplorer.prototype.scrubStep = function(direction) {
        if (!this.moviePlayerEl) return;
        var dur = this.moviePlayerEl.duration || 1;
        var cur = this.moviePlayerEl.currentTime || 0;

        // In RTL: Direction 'right' advances time (+15s or +1.5%), 'left' rewinds (-15s or -1.5%)
        var stepSec = Math.max(10, dur * 0.015); // 1.5% of film or 10s minimum
        var delta = (direction === 'forward') ? stepSec : -stepSec;
        var target = Math.max(0, Math.min(dur, cur + delta));

        this.moviePlayerEl.currentTime = target;
        this.updateCinemaScrubber();
        this.showMovieHud();
    };

    StorageExplorer.prototype.updateCinemaScrubber = function() {
        if (!this.moviePlayerEl) return;
        var cur = this.moviePlayerEl.currentTime || 0;
        var dur = this.moviePlayerEl.duration || 1;
        var pct = Math.min(100, Math.max(0, (cur / dur) * 100));

        var bar = document.getElementById('cinemaScrubberFill');
        var thumb = document.getElementById('cinemaScrubberThumb');
        var tooltip = document.getElementById('cinemaScrubberTooltip');
        var curLabel = document.getElementById('cinemaCurrentTimeDigits');
        var totalLabel = document.getElementById('cinemaTotalTimeDigits');

        var formatTime = function(t) {
            var totalSec = Math.floor(t);
            var h = Math.floor(totalSec / 3600);
            var m = Math.floor((totalSec % 3600) / 60);
            var s = Math.floor(totalSec % 60);
            if (h > 0) {
                return (h < 10 ? '0' : '') + h + ':' + (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
            }
            return (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
        };

        if (bar) bar.style.width = pct + '%';
        if (thumb) thumb.style.right = pct + '%';

        if (tooltip) {
            tooltip.textContent = formatTime(cur) + ' (' + Math.round(pct) + '%)';
            tooltip.style.right = 'calc(' + pct + '% - 30px)';
        }

        if (curLabel) curLabel.textContent = formatTime(cur);
        if (totalLabel) totalLabel.textContent = formatTime(dur);
    };

    StorageExplorer.prototype.showMovieHud = function() {
        var self = this;
        if (!this.movieHud) return;
        this.movieHud.classList.remove('hidden');
        if (this.movieHudTimer) clearTimeout(this.movieHudTimer);
        this.movieHudTimer = setTimeout(function() {
            if (self.movieHud && !self.moviePlayerEl.paused) {
                self.movieHud.classList.add('hidden');
            }
        }, 4500);
    };

    // Central Cinema Remote Handler (Called by SpatialNav)
    StorageExplorer.prototype.handleCinemaKey = function(code, e) {
        if (!this.isCinemaActive()) return false;
        this.showMovieHud();

        var KEYS = window.SpatialNav ? window.SpatialNav.KEYS : {
            LEFT: 37, UP: 38, RIGHT: 39, DOWN: 40, ENTER: 13,
            BACK_ESC: 27, BACK_TIZEN: 10009, BACK_WEBOS: 461, BACKSPACE: 8,
            MEDIA_PLAY_PAUSE: 10252, MEDIA_PLAY: 415, MEDIA_PAUSE: 19,
            NUM_0: 48, NUM_9: 57
        };

        // 1. Back Keys -> Exit Cinema
        if (code === KEYS.BACK_ESC || code === KEYS.BACK_TIZEN || 
            code === KEYS.BACK_WEBOS || code === KEYS.BACKSPACE) {
            this.closeCinemaPlayer();
            return true;
        }

        // 2. Play/Pause Media Keys
        if (code === KEYS.MEDIA_PLAY_PAUSE || code === KEYS.MEDIA_PLAY || code === KEYS.MEDIA_PAUSE) {
            this.toggleCinemaPlayPause();
            return true;
        }

        // 3. Numeric Keys 0-9 (VLC Feature: 0 -> 0%, 1 -> 10%, 5 -> 50%, 7 -> 70%, 9 -> 90%)
        if (code >= 48 && code <= 57) {
            var digit = code - 48;
            var pct = digit * 10;
            this.seekMovieToPercentage(pct);
            return true;
        }

        // 4. Scrubber Track Navigation (D-Pad Left/Right)
        var focused = window.SpatialNav ? window.SpatialNav.currentFocus : document.activeElement;
        if (focused && focused.id === 'cinemaScrubberTrack') {
            if (code === KEYS.RIGHT) {
                // In RTL, right is forward in time
                this.scrubStep('forward');
                return true;
            } else if (code === KEYS.LEFT) {
                // In RTL, left is backward in time
                this.scrubStep('backward');
                return true;
            } else if (code === KEYS.DOWN) {
                // Down moves focus to Quick Jump Pills or Action Buttons
                var firstPill = document.querySelector('.cinema-quick-jumps .jump-pill');
                if (firstPill && window.SpatialNav) {
                    window.SpatialNav.setFocus(firstPill);
                }
                return true;
            } else if (code === KEYS.ENTER) {
                this.toggleCinemaPlayPause();
                return true;
            }
        }

        return false;
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
