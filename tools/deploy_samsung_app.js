const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const PackageSigner = require('C:/Users/mokas/.vscode/extensions/tizensdk.tizentv-1.3.3/node_modules/@tizentv/webide-common-tizentv/lib/packageSigner');

const PROFILE_NAME = 'MishkatSamsungProfile';
const TV_IP = '192.168.1.8';
const SDB_EXE = 'C:/tizentv-tools/sdb/sdb.exe';

const CERT_DIR = 'C:/tizentv-tools/SamsungCertificate/MishkatSamsungProfile';
const AUTHOR_P12 = path.join(CERT_DIR, 'author.p12').replace(/\\/g, '/');
const AUTHOR_PWD = path.join(CERT_DIR, 'author.pwd').replace(/\\/g, '/');
const DIST_P12 = path.join(CERT_DIR, 'distributor.p12').replace(/\\/g, '/');
const DIST_PWD = path.join(CERT_DIR, 'distributor.pwd').replace(/\\/g, '/');

const PROFILES_XML_1 = 'C:/Users/mokas/.vscode/extensions/tizensdk.tizentv-1.3.3/resource/profiles.xml';
const PROFILES_XML_2 = 'C:/Users/mokas/tizen-studio-data/vscode-tizentv/resource/profiles.xml';

async function main() {
    console.log('=== Deploying Mishkat Launcher to Samsung Smart TV ===\n');

    // 1. Update profiles.xml
    console.log('1️⃣ Updating profiles.xml with genuine Samsung certificates...');
    const xmlContent = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<profiles active="${PROFILE_NAME}" version="3.1">
  <profile name="${PROFILE_NAME}">
    <profileitem ca="" distributor="0" key="${AUTHOR_P12}" password="${AUTHOR_PWD}" rootca=""/>
    <profileitem ca="" distributor="1" key="${DIST_P12}" password="${DIST_PWD}" rootca=""/>
    <profileitem ca="" distributor="2" key="" password="" rootca=""/>
  </profile>
</profiles>
`;
    fs.writeFileSync(PROFILES_XML_1, xmlContent);
    try { fs.writeFileSync(PROFILES_XML_2, xmlContent); } catch (e) {}
    console.log('   ✅ profiles.xml written successfully!');

    // 2. Prepare staging directory
    console.log('\n2️⃣ Preparing stage directory...');
    const projectRoot = path.resolve(__dirname, '..');
    const stageDir = path.join(projectRoot, 'tizen', 'stage');
    if (fs.existsSync(stageDir)) {
        fs.rmSync(stageDir, { recursive: true, force: true });
    }
    fs.mkdirSync(stageDir, { recursive: true });

    fs.copyFileSync(path.join(projectRoot, 'tizen', 'config.xml'), path.join(stageDir, 'config.xml'));
    fs.copyFileSync(path.join(projectRoot, 'tizen', 'icon.png'), path.join(stageDir, 'icon.png'));

    function copyDir(src, dest) {
        fs.mkdirSync(dest, { recursive: true });
        for (const item of fs.readdirSync(src)) {
            if (item.startsWith('_')) continue;
            const sPath = path.join(src, item);
            const dPath = path.join(dest, item);
            if (fs.statSync(sPath).isDirectory()) {
                copyDir(sPath, dPath);
            } else {
                fs.copyFileSync(sPath, dPath);
            }
        }
    }
    copyDir(path.join(projectRoot, 'tv-web'), stageDir);

    const sharedStage = path.join(stageDir, 'shared');
    fs.mkdirSync(sharedStage, { recursive: true });
    const sharedDir = path.join(projectRoot, 'shared');
    for (const f of fs.readdirSync(sharedDir)) {
        if (f.endsWith('.json')) {
            fs.copyFileSync(path.join(sharedDir, f), path.join(sharedStage, f));
        }
    }
    console.log('   ✅ Staged all application files (HTML, JS, CSS, JSON resources)');

    // 3. Sign the package
    console.log('\n3️⃣ Signing stage directory with PackageSigner and Samsung Profile...');
    const signer = new PackageSigner();
    await signer.setProfile(PROFILES_XML_1, PROFILE_NAME);
    signer.signPackage(stageDir, []);

    const authorSigPath = path.join(stageDir, 'author-signature.xml');
    const distSigPath = path.join(stageDir, 'signature1.xml');
    console.log('   ✅ author-signature.xml created:', fs.existsSync(authorSigPath), 'size:', fs.statSync(authorSigPath).size);
    console.log('   ✅ signature1.xml created:', fs.existsSync(distSigPath), 'size:', fs.statSync(distSigPath).size);

    // 4. Pack into .wgt
    console.log('\n4️⃣ Packaging .wgt widget archive...');
    const distDir = path.join(projectRoot, 'dist');
    if (!fs.existsSync(distDir)) fs.mkdirSync(distDir, { recursive: true });
    const wgtPath = path.join(distDir, 'MishkatLauncher-Samsung-Tizen.wgt');
    if (fs.existsSync(wgtPath)) fs.unlinkSync(wgtPath);

    const packPy = path.join(__dirname, 'pack_wgt.py');
    execSync(`python "${packPy}" "${stageDir}" "${wgtPath}"`, { stdio: 'inherit' });
    console.log('   ✅ Generated .wgt size:', fs.statSync(wgtPath).size, 'bytes');

    // 5. Connect SDB and Install on Samsung TV
    console.log('\n5️⃣ Connecting and pushing to Samsung TV (' + TV_IP + ':26101)...');
    try {
        const pushCmd = `& "${SDB_EXE}" connect ${TV_IP}:26101 ; Start-Sleep -Milliseconds 800 ; & "${SDB_EXE}" push "${wgtPath}" /tmp/MishkatLauncher.wgt`;
        const pushOut = execSync(pushCmd, { shell: 'powershell.exe' }).toString();
        console.log('   SDB Push Output:\n' + pushOut.trim());
    } catch (e) {
        console.error('   SDB Push Error:', e.message);
        throw e;
    }

    console.log('   Installing native application on Samsung Tizen OS...');
    const installCmd = `& "${SDB_EXE}" connect ${TV_IP}:26101 ; Start-Sleep -Milliseconds 500 ; & "${SDB_EXE}" shell 0 vd_appinstall Mishkat001.Launcher /tmp/MishkatLauncher.wgt`;
    const installOut = execSync(installCmd, { shell: 'powershell.exe' }).toString();
    console.log('   Install Output:\n' + installOut);

    if (installOut.includes('val[ok]') || installOut.includes('val[100]') || installOut.includes('install completed')) {
        console.log('\n🎉🎉🎉 SUCCESS! Mishkat Launcher installed natively on Samsung Smart TV! 🎉🎉🎉');
        console.log('   Launching Mishkat Launcher on screen...');
        try {
            const launchCmd = `& "${SDB_EXE}" connect ${TV_IP}:26101 ; Start-Sleep -Milliseconds 500 ; & "${SDB_EXE}" shell 0 execute Mishkat001.Launcher`;
            const launchOut = execSync(launchCmd, { shell: 'powershell.exe' }).toString();
            console.log('   Launch Output:\n' + launchOut);
        } catch (e) {
            console.log('   Launch Output:', e.message);
        }
    } else {
        console.log('   Install response check above.');
    }
}

main().catch(err => {
    console.error('\n❌ Deployment failed:', err);
    process.exit(1);
});
