const fs = require('fs');
const path = require('path');
const PackageSigner = require('C:/Users/mokas/.vscode/extensions/tizensdk.tizentv-1.3.3/node_modules/@tizentv/webide-common-tizentv/lib/packageSigner');
const resourcePath = 'C:/Users/mokas/.vscode/extensions/tizensdk.tizentv-1.3.3/resource';
const profilesPath = path.join(resourcePath, 'profiles.xml');

async function main() {
    console.log('=== Signing Mishkat Launcher with MishkatProfile ===');
    
    // 1. Prepare stage directory
    const projectRoot = path.resolve(__dirname, '..');
    const stageDir = path.join(projectRoot, 'tizen', 'stage');
    
    if (fs.existsSync(stageDir)) {
        fs.rmSync(stageDir, { recursive: true, force: true });
    }
    fs.mkdirSync(stageDir, { recursive: true });
    
    // 2. Copy config.xml and icon.png
    fs.copyFileSync(path.join(projectRoot, 'tizen', 'config.xml'), path.join(stageDir, 'config.xml'));
    fs.copyFileSync(path.join(projectRoot, 'tizen', 'icon.png'), path.join(stageDir, 'icon.png'));
    
    // 3. Copy tv-web files
    const webDir = path.join(projectRoot, 'tv-web');
    function copyDir(src, dest) {
        fs.mkdirSync(dest, { recursive: true });
        for (const item of fs.readdirSync(src)) {
            const sPath = path.join(src, item);
            const dPath = path.join(dest, item);
            if (fs.statSync(sPath).isDirectory()) {
                copyDir(sPath, dPath);
            } else {
                fs.copyFileSync(sPath, dPath);
            }
        }
    }
    copyDir(webDir, stageDir);
    
    // 4. Copy shared json files
    const sharedStage = path.join(stageDir, 'shared');
    fs.mkdirSync(sharedStage, { recursive: true });
    const sharedDir = path.join(projectRoot, 'shared');
    for (const f of fs.readdirSync(sharedDir)) {
        if (f.endsWith('.json')) {
            fs.copyFileSync(path.join(sharedDir, f), path.join(sharedStage, f));
        }
    }
    
    // 5. Sign the package
    console.log('Signing stage directory with PackageSigner...');
    const signer = new PackageSigner();
    await signer.setProfile(profilesPath, 'MishkatProfile');
    signer.signPackage(stageDir, []);
    
    const authorSigPath = path.join(stageDir, 'author-signature.xml');
    const distributorSigPath = path.join(stageDir, 'signature1.xml');
    console.log('author-signature.xml created:', fs.existsSync(authorSigPath), 'size:', fs.statSync(authorSigPath).size);
    console.log('signature1.xml created:', fs.existsSync(distributorSigPath), 'size:', fs.statSync(distributorSigPath).size);
    
    console.log('Signing completed successfully!');
}

main().catch(err => console.error('Sign error:', err));
