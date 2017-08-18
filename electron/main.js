const {app, BrowserWindow} = require('electron');
const os = require('os');
const {setMenu} = require("./appMenu");
const {startJVM} = require("./startup");
const fetch = require('electron-fetch');
const path = require('path');
const url = require('url');
const {fork} = require('child_process');
const log = require('electron-log');
const fs = require('fs');
const spotify = require('./spotify');
const {autoUpdater} = require("electron-updater");

process.env.GOOGLE_API_KEY = 'AIzaSyD78NTnhgt--LCGBdIGPEg8GtBYzQl0gKU';
const platform = process.platform;
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

log.info("platform", platform);
log.info("temp", os.tmpdir());
log.info("appPath", appPath);
log.info("process.execPath", process.execPath);
log.info("process.cwd", process.cwd());
log.info("resources path", resourcePath);

let started = false;
var mainWindow;

function createWindow() {
    log.info("creating main window");
    let window = new BrowserWindow(
        {
            width: 1200,
            height: 800,
            webPreferences: {
                experimentalFeatures: true
            }
        }
    );
    window.loadURL(url.format({
        pathname: path.join(__dirname, 'index.html'),
        protocol: 'file:',
        slashes: true
    }));

    mainWindow = window;
    setMenu(window, createWindow);

    window.on('focus', function () {
        log.info("focused");
        mainWindow = window;
        setMenu(window, createWindow);
    });
    window.on('closed', function () {
        window = null;
        mainWindow = null
    })
}

function waitUntilUp() {
    fetch("http://localhost:" + PORT)
        .then(function (response) {
                if (response.status !== 200) {
                    log.info('Looks like there was a problem. Status Code: ' +
                        response.status);
                    return;
                }
                response.text().then(createWindow);
            }
        )
        .catch(function (err) {
            log.error(err);
            if (!started) {
                startJVM(PORT, UPLOAD_PORT);
                started = true;
            }
            log.info("Loading? Retry in 1000ms...");
            setTimeout(waitUntilUp, 1000);
        });
}

app.on('ready', function () {
    waitUntilUp();
});

app.on('window-all-closed', function () {
    if (platform !== 'darwin') {
        app.quit()
    }
});

app.on('activate', function () {
    if (mainWindow === null) {
        createWindow()
    }
});
