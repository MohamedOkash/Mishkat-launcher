const http = require('http');
const https = require('https');
const url = require('url');
const fs = require('fs');
const path = require('path');
const { execSync, spawn } = require('child_process');

const forge = require('C:/Users/mokas/.vscode/extensions/tizensdk.tizentv-1.3.3/node_modules/@tizentv/webide-common-tizentv/node_modules/node-forge');
const PackageSigner = require('C:/Users/mokas/.vscode/extensions/tizensdk.tizentv-1.3.3/node_modules/@tizentv/webide-common-tizentv/lib/packageSigner');

const TV_IP = '192.168.1.8';
const TV_DUID = '43CB2NS2KWIGC';
const AUTHOR_NAME = 'Eng. Mohamed Okash';
const PASSWORD = 'MishkatPassword123!';
const PROFILE_NAME = 'MishkatSamsungProfile';

const SDB_EXE = 'C:/tizentv-tools/sdb/sdb.exe';
const RESOURCE_PATH = 'C:/Users/mokas/.vscode/extensions/tizensdk.tizentv-1.3.3/resource';
const PROFILES_XML_1 = path.join(RESOURCE_PATH, 'profiles.xml');
const PROFILES_XML_2 = 'C:/Users/mokas/tizen-studio-data/vscode-tizentv/resource/profiles.xml';

const CERTS_DIR = 'C:/tizentv-tools/samsung-certificate';
const OUT_DIR = path.join('C:/tizentv-tools/SamsungCertificate', PROFILE_NAME);

const SERVICE_ID = 'v285zxnl3h';
const CALLBACK_PORT = 4794;
const LOGIN_URL = `https://account.samsung.com/mobile/account/check.do?serviceID=${SERVICE_ID}&actionID=StartOAuth2&accessToken=Y&redirect_uri=http://localhost:${CALLBACK_PORT}/signin/callback`;

if (!fs.existsSync(OUT_DIR)) {
    fs.mkdirSync(OUT_DIR, { recursive: true });
}

function makePostRequest(targetUrl, fields, csrName, csrPem) {
    return new Promise((resolve, reject) => {
        let parts = [];
        for (const f of fields) {
            parts.push(
                `--*****\r\n` +
                `Content-Type: text/plain; charset=utf-8\r\n` +
                `Content-Disposition: form-data; name=${f.name}\r\n\r\n` +
                `${f.value}\r\n`
            );
        }
        parts.push(
            `--*****\r\n` +
            `Content-Disposition: form-data; name=csr; filename=${csrName}; filename*=utf-8''${csrName}\r\n\r\n` +
            `${csrPem}\r\n` +
            `--*****--\r\n`
        );
        const bodyBuf = Buffer.from(parts.join(''), 'utf-8');

        const parsed = url.parse(targetUrl);
        const req = https.request({
            hostname: parsed.hostname,
            port: parsed.port || 443,
            path: parsed.path,
            method: 'POST',
            headers: {
                'content-type': 'multipart/form-data; boundary="*****"',
                'Content-Length': bodyBuf.length
            },
            timeout: 45000
        }, (res) => {
            let data = [];
            res.on('data', chunk => data.push(chunk));
            res.on('end', () => {
                const respText = Buffer.concat(data).toString('utf-8');
                if (res.statusCode >= 200 && res.statusCode < 300) {
                    resolve(respText);
                } else {
                    reject(new Error(`Server returned ${res.statusCode} ${res.statusMessage}: ${respText}`));
                }
            });
        });

        req.on('error', err => reject(err));
        req.on('timeout', () => {
            req.destroy();
            reject(new Error('Request timed out'));
        });
        req.write(bodyBuf);
        req.end();
    });
}

function createP12(privateKey, certPem, caCertPem, p12Path, password) {
    const cert = forge.pki.certificateFromPem(certPem);
    const caCert = forge.pki.certificateFromPem(caCertPem);
    const p12Asn1 = forge.pkcs12.toPkcs12Asn1(
        privateKey,
        [cert, caCert],
        password,
        { generateLocalKeyId: true, friendlyName: 'UserCertificate' }
    );
    const p12Der = forge.asn1.toDer(p12Asn1).getBytes();
    fs.writeFileSync(p12Path, p12Der, { encoding: 'binary' });
}

async function handleToken(session) {
    console.log('\n======================================================');
    console.log('✅ Samsung OAuth Session Received:');
    console.log('   User ID:', session.userId);
    console.log('   Email  :', session.email);
    console.log('   Token  :', session.accessToken.substring(0, 15) + '...');
    console.log('======================================================\n');

    // 1. Author Certificate
    console.log('1️⃣ Generating Author Key Pair and CSR for:', AUTHOR_NAME);
    const authorKeys = forge.pki.rsa.generateKeyPair(2048);
    const authorCsr = forge.pki.createCertificationRequest();
    authorCsr.publicKey = authorKeys.publicKey;
    authorCsr.setSubject([
        { name: 'commonName', value: AUTHOR_NAME },
        { shortName: 'OU', value: 'Mishkat Development' },
        { name: 'organizationName', value: 'Mishkat' },
        { name: 'localityName', value: 'Cairo' },
        { shortName: 'ST', value: 'Cairo' },
        { name: 'countryName', value: 'EG' }
    ]);
    authorCsr.sign(authorKeys.privateKey, forge.md.sha512.create());
    const authorCsrPem = forge.pki.certificationRequestToPem(authorCsr);
    fs.writeFileSync(path.join(OUT_DIR, 'author.csr'), authorCsrPem);

    console.log('   Requesting Author CRT from Samsung CA (https://svdca.samsungqbe.com/apis/v3/authors)...');
    const authorCrtPem = await makePostRequest(
        'https://svdca.samsungqbe.com/apis/v3/authors',
        [
            { name: 'access_token', value: session.accessToken },
            { name: 'user_id', value: session.userId },
            { name: 'platform', value: 'VD' }
        ],
        'author.csr',
        authorCsrPem
    );
    fs.writeFileSync(path.join(OUT_DIR, 'author.crt'), authorCrtPem);
    console.log('   ✅ Author Certificate Received! Size:', authorCrtPem.length);

    const authorCaPem = fs.readFileSync(path.join(CERTS_DIR, 'vd_tizen_dev_author_ca.cer'), 'utf-8');
    const authorP12Path = path.join(OUT_DIR, 'author.p12');
    createP12(authorKeys.privateKey, authorCrtPem, authorCaPem, authorP12Path, PASSWORD);
    fs.writeFileSync(path.join(OUT_DIR, 'author.pwd'), PASSWORD);
    console.log('   ✅ author.p12 generated successfully at:', authorP12Path);

    // 2. Distributor Certificate
    console.log('\n2️⃣ Generating Distributor Key Pair and CSR for TV DUID:', TV_DUID);
    const distKeys = forge.pki.rsa.generateKeyPair(2048);
    const distCsr = forge.pki.createCertificationRequest();
    distCsr.publicKey = distKeys.publicKey;
    distCsr.setSubject([
        { name: 'commonName', value: 'TizenSDK' },
        { name: 'emailAddress', value: session.email || 'developer@samsung.com' }
    ]);
    distCsr.setAttributes([{
        name: 'extensionRequest',
        extensions: [{
            name: 'subjectAltName',
            altNames: [
                { type: 6, value: 'URN:tizen:packageid=' },
                { type: 6, value: 'URN:tizen:deviceid=' + TV_DUID }
            ]
        }]
    }]);
    distCsr.sign(distKeys.privateKey, forge.md.sha512.create());
    const distCsrPem = forge.pki.certificationRequestToPem(distCsr);
    fs.writeFileSync(path.join(OUT_DIR, 'distributor.csr'), distCsrPem);

    const distFields = [
        { name: 'access_token', value: session.accessToken },
        { name: 'user_id', value: session.userId },
        { name: 'privilege_level', value: 'Public' },
        { name: 'developer_type', value: 'Individual' },
        { name: 'platform', value: 'VD' }
    ];

    console.log('   Requesting device-profile.xml from Samsung CA (v1)...');
    try {
        const deviceProfile = await makePostRequest(
            'https://svdca.samsungqbe.com/apis/v1/distributors',
            distFields,
            'distributor.csr',
            distCsrPem
        );
        fs.writeFileSync(path.join(OUT_DIR, 'device-profile.xml'), deviceProfile);
        console.log('   ✅ device-profile.xml saved!');
    } catch (e) {
        console.log('   ⚠️ Warning on v1 distributors:', e.message);
    }

    console.log('   Requesting Distributor CRT from Samsung CA (v3)...');
    const distCrtPem = await makePostRequest(
        'https://svdca.samsungqbe.com/apis/v3/distributors',
        distFields,
        'distributor.csr',
        distCsrPem
    );
    fs.writeFileSync(path.join(OUT_DIR, 'distributor.crt'), distCrtPem);
    console.log('   ✅ Distributor Certificate Received! Size:', distCrtPem.length);

    const distCaPem = fs.readFileSync(path.join(CERTS_DIR, 'vd_tizen_dev_public2.crt'), 'utf-8');
    const distP12Path = path.join(OUT_DIR, 'distributor.p12');
    createP12(distKeys.privateKey, distCrtPem, distCaPem, distP12Path, PASSWORD);
    fs.writeFileSync(path.join(OUT_DIR, 'distributor.pwd'), PASSWORD);
    console.log('   ✅ distributor.p12 generated successfully at:', distP12Path);

    // 3. Register Profile in profiles.xml
    console.log('\n3️⃣ Registering profile in profiles.xml...');
    const xmlContent = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<profiles active="${PROFILE_NAME}" version="3.1">
  <profile name="${PROFILE_NAME}">
    <profileitem ca="" distributor="0" key="${authorP12Path.replace(/\\/g, '/')}" password="${PASSWORD}" rootca=""/>
    <profileitem ca="" distributor="1" key="${distP12Path.replace(/\\/g, '/')}" password="${PASSWORD}" rootca=""/>
    <profileitem ca="" distributor="2" key="" password="" rootca=""/>
  </profile>
</profiles>
`;
    fs.writeFileSync(PROFILES_XML_1, xmlContent);
    try { fs.writeFileSync(PROFILES_XML_2, xmlContent); } catch (e) {}
    console.log('   ✅ profiles.xml updated!');

    // 4. Sign and Build Package
    console.log('\n4️⃣ Signing Mishkat Launcher with official Samsung Profile...');
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

    const signer = new PackageSigner();
    await signer.setProfile(PROFILES_XML_1, PROFILE_NAME);
    signer.signPackage(stageDir, []);

    const authorSigPath = path.join(stageDir, 'author-signature.xml');
    const distSigPath = path.join(stageDir, 'signature1.xml');
    console.log('   ✅ author-signature.xml created:', fs.existsSync(authorSigPath));
    console.log('   ✅ signature1.xml created (tied to TV DUID):', fs.existsSync(distSigPath));

    // Zip stage into .wgt
    const wgtPath = path.join(projectRoot, 'dist', 'MishkatLauncher-Samsung-Tizen.wgt');
    if (fs.existsSync(wgtPath)) {
        fs.unlinkSync(wgtPath);
    }
    console.log('   Packaging .wgt archive with python zipfile...');
    execSync(`python -c "
import zipfile, os
stage = r'${stageDir}'
wgt = r'${wgtPath}'
with zipfile.ZipFile(wgt, 'w', zipfile.ZIP_DEFLATED) as z:
    for root, dirs, files in os.walk(stage):
        for f in files:
            p = os.path.join(root, f)
            arc = os.path.relpath(p, stage).replace('\\\\', '/')
            z.write(p, arc)
"`);
    console.log('   ✅ Generated .wgt size:', fs.statSync(wgtPath).size, 'bytes');

    // 5. Install on Samsung TV via SDB
    console.log('\n5️⃣ Deploying directly to Samsung TV (192.168.1.8)...');
    execSync(`& "${SDB_EXE}" connect ${TV_IP}:26101`, { shell: 'powershell.exe' });
    console.log('   Pushing .wgt to TV /tmp/MishkatLauncher.wgt...');
    execSync(`& "${SDB_EXE}" push "${wgtPath}" /tmp/MishkatLauncher.wgt`, { shell: 'powershell.exe' });
    console.log('   Executing native installation on Samsung Tizen OS...');
    const installOut = execSync(`& "${SDB_EXE}" shell 0 vd_appinstall Mishkat001.Launcher /tmp/MishkatLauncher.wgt`, { shell: 'powershell.exe' }).toString();
    console.log('   Install Output:\n', installOut);

    if (installOut.includes('val[ok]') || installOut.includes('val[100]')) {
        console.log('\n🎉🎉🎉 SUCCESS! Mishkat Launcher installed natively on Samsung Smart TV! 🎉🎉🎉');
        console.log('   Launching Mishkat Launcher on screen...');
        try {
            const launchOut = execSync(`& "${SDB_EXE}" shell 0 execute Mishkat001.Launcher`, { shell: 'powershell.exe' }).toString();
            console.log('   Launch Output:\n', launchOut);
        } catch (e) {
            console.log('   Launch attempt note:', e.message);
        }
    } else {
        console.log('   ⚠️ Installation output note: check output above.');
    }
    console.log('\n=== TASK FINISHED! ===');
    process.exit(0);
}

const server = http.createServer((req, res) => {
    const parsed = url.parse(req.url, true);
    console.log(`[HTTP] ${req.method} ${parsed.pathname}`);

    // Set CORS and Private Network headers for all responses
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', '*');
    res.setHeader('Access-Control-Allow-Private-Network', 'true');

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        res.end();
        return;
    }

    if (parsed.pathname === '/signin/callback') {
        let body = [];
        req.on('data', chunk => body.push(chunk));
        req.on('end', async () => {
            const rawBody = Buffer.concat(body).toString('utf-8');
            let code = parsed.query.code;
            if (!code && rawBody) {
                // Try JSON first
                try {
                    const parsedJson = JSON.parse(rawBody);
                    if (parsedJson.code) code = parsedJson.code;
                    else if (parsedJson.access_token) {
                        // Directly passed session!
                        await handleToken({
                            accessToken: parsedJson.access_token,
                            userId: parsedJson.userId,
                            email: parsedJson.inputEmailID || parsedJson.email
                        });
                        res.writeHead(200, { 'Content-Type': 'application/json' });
                        res.end(JSON.stringify({ ok: true }));
                        return;
                    }
                } catch (e) {
                    const params = new URLSearchParams(rawBody);
                    code = params.get('code');
                }
            }

            if (!code) {
                res.writeHead(400, { 'Content-Type': 'text/html; charset=utf-8' });
                res.end('<h3>❌ Error: No code received in callback</h3>');
                return;
            }

            try {
                const sessionObj = JSON.parse(code);
                res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
                res.end(`
                    <!DOCTYPE html>
                    <html dir="rtl" lang="ar">
                    <head>
                        <meta charset="utf-8">
                        <title>تم تسجيل الدخول بنجاح</title>
                        <style>
                            body { font-family: 'Segoe UI', Tahoma, sans-serif; background: #0f172a; color: #f8fafc; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }
                            .card { background: #1e293b; border: 1px solid #334155; border-radius: 20px; padding: 40px; text-align: center; max-width: 500px; box-shadow: 0 20px 40px rgba(0,0,0,0.5); }
                            h1 { color: #38bdf8; margin-bottom: 12px; }
                            p { font-size: 16px; color: #94a3b8; line-height: 1.6; }
                            .badge { display: inline-block; background: #0369a1; color: white; padding: 6px 16px; border-radius: 999px; margin-top: 15px; font-weight: bold; }
                        </style>
                    </head>
                    <body>
                        <div class="card">
                            <h1>✅ تم تسجيل الدخول بنجاح!</h1>
                            <p>أهلاً بك يا <strong>${AUTHOR_NAME}</strong>.<br>تم استلام جلسة سامسونج وجاري الآن فوراً إصدار الشهادات المعتمدة من خوادم سامسونج وتثبيت مشكاة لانشر مباشرة على شاشتك الـ 65 بوصة...</p>
                            <div class="badge">يمكنك إغلاق هذه الصفحة الآن</div>
                        </div>
                    </body>
                    </html>
                `);

                await handleToken({
                    accessToken: sessionObj.access_token,
                    userId: sessionObj.userId,
                    email: sessionObj.inputEmailID
                });
            } catch (err) {
                console.error('Callback processing error:', err);
                res.writeHead(500, { 'Content-Type': 'text/html; charset=utf-8' });
                res.end(`<h3>❌ Processing Error: ${err.message}</h3>`);
            }
        });
        return;
    }

    if (parsed.pathname === '/login') {
        res.writeHead(302, { Location: LOGIN_URL });
        res.end();
        return;
    }

    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(`
        <h2>Mishkat Launcher - Samsung TV Certificate Auth Server</h2>
        <p>Click below to sign in to your Samsung Account:</p>
        <a href="${LOGIN_URL}">👉 تسجيل الدخول بحساب سامسونج</a>
    `);
});

server.listen(CALLBACK_PORT, '127.0.0.1', () => {
    console.log(`\n=================================================================`);
    console.log(`🚀 Samsung OAuth Listener is running on: http://127.0.0.1:${CALLBACK_PORT}`);
    console.log(`🔗 Sign-in URL:`);
    console.log(`   ${LOGIN_URL}`);
    console.log(`=================================================================\n`);
});
