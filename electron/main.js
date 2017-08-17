const {app, BrowserWindow, Menu, ipcMain} = require('electron');
const {setMenu} = require("./appMenu");
const fetch = require('electron-fetch');
const path = require('path');
const url = require('url');
const {spawn, fork} = require('child_process');
const log = require('electron-log');
const fs = require('fs');
const {session} = require('electron');
const spotify = require('./spotify');
const {autoUpdater} = require("electron-updater");

const userData = app.getPath("userData");
const binPath = app.getPath("exe");
const resourcePath = process.resourcesPath;
const PORT = 7777;
const UPLOAD_PORT = 3002;

let appPath;
if (fs.existsSync(resourcePath + "/app/main.js")) {
    appPath = path.normalize(resourcePath + "/app");
} else {
    appPath = process.cwd();
}

const geocoder = fork("geocoder.js", [], {cwd: appPath});

log.transports.console.level = 'info';
log.transports.console.format = '{h}:{i}:{s}:{ms} {text}';
log.transports.file.level = 'debug';
log.transports.file.format = '{h}:{i}:{s}:{ms} {text}';

if (process.platform !== "win32") {
    log.transports.file.file = '/tmp/iWasWhereUI.log';
}

autoUpdater.logger = log;

log.info("platform", process.platform);
log.info("binPath", binPath);
log.info("appPath", appPath);
log.info("process.execPath", process.execPath);
log.info("process.cwd", process.cwd());
log.info("resources path", resourcePath);

process.env.GOOGLE_API_KEY = 'AIzaSyD78NTnhgt--LCGBdIGPEg8GtBYzQl0gKU';

let started = false;
let jvmService;

// Keep a global reference of the window object, if you don't, the window will
// be closed automatically when the JavaScript object is garbage collected.
let mainWindow;

function createWindow() {
    log.info("creating main window");
    // Create the browser window.
    mainWindow = new BrowserWindow(
        {
            width: 1200,
            height: 800,
            webPreferences: {
                experimentalFeatures: true
            }
        }
    );

    // and load the index.html of the app.
    mainWindow.loadURL(url.format({
        pathname: path.join(__dirname, 'index.html'),
        protocol: 'file:',
        slashes: true
    }));

    // Emitted when the window is closed.
    mainWindow.on('closed', function () {
        // Dereference the window object, usually you would store windows
        // in an array if your app supports multi windows, this is the time
        // when you should delete the corresponding element.
        mainWindow = null
    })
}

function waitUntilUp() {
    fetch("http://localhost:" + PORT)
        .then(
            function (response) {
                if (response.status !== 200) {
                    log.info('Looks like there was a problem. Status Code: ' +
                        response.status);
                    return;
                }

                // Examine the text in the response
                response.text().then(function (data) {
                    createWindow();
                });
            }
        )
        .catch(function (err) {
            if (!started) {
                var javaPath = "/usr/bin/java";
                if (process.platform === 'darwin') {
                    javaPath = appPath + "/bin/zulu8.23.0.3-jdk8.0.144-mac_x64/bin/java";
                }
                if (process.platform === 'win32') {
                    javaPath = path.normalize(appPath + "/bin/zulu8.23.0.3-jdk8.0.144-win_x64/bin/java.exe");
                }

                const jarPath = path.normalize(appPath + "/bin/iwaswhere.jar");
                const blinkPath = path.normalize(appPath + "/bin/blink1-mac-cli");
                const dataPath = path.normalize(userData + "/data");

                log.info("Starting backend service...");
                log.info('User data in:', dataPath);

                jvmService = spawn(javaPath, ["-Dapple.awt.UIElement=true", "-jar", jarPath], {
                    detached: false,
                    //shell: "/bin/bash",
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

                started = true;
            }
            log.info("Loading? Retry in 1000ms...");
            setTimeout(waitUntilUp, 1000);
        });
}

function start() {
    waitUntilUp();
    setMenu(waitUntilUp, autoUpdater, mainWindow, app);
}

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.on('ready', start);

// Quit when all windows are closed.
app.on('window-all-closed', function () {
    // On OS X it is common for applications and their menu bar
    // to stay active until the user quits explicitly with Cmd + Q
    if (process.platform !== 'darwin') {
        app.quit()
    }
});

app.on('activate', function () {
    // On OS X it's common to re-create a window in the app when the
    // dock icon is clicked and there are no other windows open.
    if (mainWindow === null) {
        createWindow();
    }
});

autoUpdater.on('checking-for-update', () => {
    log.info('Checking for update...');
});
autoUpdater.on('update-available', (info) => {
    log.info('Update available.');
});
autoUpdater.on('update-not-available', (info) => {
    log.info('Update not available.');
});
autoUpdater.on('error', (err) => {
    log.info('Error in auto-updater.');
});
autoUpdater.on('download-progress', (progressObj) => {
    let log_message = "Download speed: " + progressObj.bytesPerSecond;
    log_message = log_message + ' - Downloaded ' + progressObj.percent + '%';
    log_message = log_message + ' (' + progressObj.transferred + "/" + progressObj.total + ')';
    log.info(log_message);
});
autoUpdater.on('update-downloaded', (info) => {
    log.info('Update can be installed')
});
