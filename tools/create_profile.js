const path = require('path');
const { TizenCertManager, ProfileManager, PackageSigner } = require('C:/Users/mokas/.vscode/extensions/tizensdk.tizentv-1.3.3/node_modules/@tizentv/webide-common-tizentv');
const resourcePath = 'C:/Users/mokas/.vscode/extensions/tizensdk.tizentv-1.3.3/resource';
const tizenCertMgr = new TizenCertManager(resourcePath);

async function run() {
    console.log('Initializing TizenCertManager...');
    await tizenCertMgr.init();
    
    const profileName = 'MishkatProfile';
    const keyFileName = 'author';
    const authorName = 'Eng. Mohamed Okash';
    const authorPassword = 'MishkatPassword123!';
    
    console.log('Generating Author Certificate in the name of:', authorName);
    tizenCertMgr.createCert({
        keyFileName: keyFileName,
        authorName: authorName,
        authorPassword: authorPassword,
        countryInfo: 'EG',
        stateInfo: 'Cairo',
        cityInfo: 'Cairo',
        organizationInfo: 'Mishkat',
        departmentInfo: 'Development',
        emailInfo: 'mohamed.okash1998@gmail.com'
    });
    
    const keyFilePath = path.join(resourcePath, 'Author', keyFileName + '.p12');
    console.log('Created key at:', keyFilePath);
    
    const profileMgr = new ProfileManager(resourcePath);
    const authorProfile = {
        authorCA: tizenCertMgr.getTizenDeveloperCA(),
        authorCertPath: keyFilePath,
        authorPassword: authorPassword
    };
    
    const distributorProfile = tizenCertMgr.getTizenDistributorProfile('partner');
    console.log('Distributor profile:', distributorProfile);
    
    await profileMgr.registerProfile(profileName, authorProfile, distributorProfile);
    await profileMgr.setActivateProfile(profileName);
    console.log('Profile registered and activated successfully!');
}

run().catch(e => console.error(e));
