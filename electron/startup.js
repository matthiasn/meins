const {app} = require('electron');
const path = require('path');
const log = require('electron-log');
const fs = require('fs');
const {spawn} = require('child_process');

const userData = app.getPath("userData");
const resourcePath = process.resourcesPath;
const platform = process.platform;

let jvmService;

let appPath;
if (fs.existsSync(resourcePath + "/app/main.js")) {
    appPath = path.normalize(resourcePath + "/app");
} else {
    appPath = process.cwd();
}

function startJVM (PORT, UPLOAD_PORT) {
    log.info("Starting JVM");
    var javaPath = "/usr/bin/java";
    if (platform === 'darwin') {
        javaPath = appPath + "/bin/zulu8.23.0.3-jdk8.0.144-mac_x64/bin/java";
    }
    if (platform === 'win32') {
        javaPath = path.normalize(appPath + "/bin/zulu8.23.0.3-jdk8.0.144-win_x64/bin/java.exe");
    }

    const jarPath = path.normalize(appPath + "/bin/iwaswhere.jar");
    const blinkPath = path.normalize(appPath + "/bin/blink1-mac-cli");
    const dataPath = path.normalize(userData + "/data");

    log.info("Starting backend service...");
    log.info('User data in:', dataPath);

    jvmService = spawn(javaPath, ["-Dapple.awt.UIElement=true", "-jar", jarPath], {
        detached: false,
        cwd: userData,
        env: {
            PORT: PORT,
            UPLOAD_PORT: UPLOAD_PORT,
            DATA_PATH: dataPath,
            BLINK_PATH: blinkPath
        }
    });

    jvmService.stdout.on('data', (data) => {
        log.info(`- ${data}`);
    });

    jvmService.stderr.on('data', (data) => {
        log.error(`- ${data}`);
    });
}

module.exports.startJVM = startJVM;
