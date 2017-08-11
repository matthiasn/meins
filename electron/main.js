const {app, BrowserWindow, Menu} = require('electron');
const fetch = require('electron-fetch');
const shell = require('electron').shell;
const child_process = require('child_process');
const path = require('path');
const url = require('url');
const {spawn} = require('child_process');
const log = require('electron-log');
const fs = require('fs');

const userData = app.getPath("userData");

log.transports.console.level = 'info';
log.transports.console.format = '{h}:{i}:{s}:{ms} {text}';
log.transports.file.level = 'info';
log.transports.file.format = '{h}:{i}:{s}:{ms} {text}';
log.transports.file.file = '/tmp/iWasWhereUI.log';

process.env.GOOGLE_API_KEY = 'AIzaSyD78NTnhgt--LCGBdIGPEg8GtBYzQl0gKU';

let started = false;
let jvmService;

// Keep a global reference of the window object, if you don't, the window will
// be closed automatically when the JavaScript object is garbage collected.
let mainWindow;

function createWindow() {
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
    fetch("http://localhost:7778")
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
                log.info("Starting backend service...");

                const jarPath = "'" + userData + "/bin/iwaswhere.jar'";
                const dataPath = userData + "/data";
                log.info('User data in:', dataPath);

                jvmService = spawn('/usr/bin/java', ["-jar", jarPath], {
                    detached: false,
                    shell: "/bin/bash",
                    cwd: userData,
                    env: {
                        PORT: 7778,
                        UPLOAD_PORT: 3233,
                        DATA_PATH: dataPath
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

    // Create the Application's main menu
    const template = [{
        label: "Application",
        submenu: [
            {
                label: "About iWasWhere",
                selector: "orderFrontStandardAboutPanel:"
            }, {
                type: "separator"
            }, {
                label: "Start Background Process",
                click: function () {
                    waitUntilUp();
                }
            }, {
                label: "Stop Background Process",
                accelerator: "Shift+Cmd+Q",
                click: function () {
                    //jvmService.kill('SIGHUP');
                    const pidFile = userData + "/iwaswhere.pid";
                    const pid = fs.readFileSync(pidFile, "utf8");
                    log.warn("shutting down", pid);
                    started = false;
                    spawn('/bin/kill', ["-KILL", + pid], {});
                }
            }, {
                label: "Quit",
                accelerator: "Cmd+Q",
                click: function () {
                    app.quit();
                }
            }
        ]
    }, {
            label: "Edit",
            submenu: [
                {label: "Undo", accelerator: "CmdOrCtrl+Z", selector: "undo:"},
                {
                    label: "Redo",
                    accelerator: "Shift+CmdOrCtrl+Z",
                    selector: "redo:"
                }, {
                type: "separator"
                }, {
                    label: "Cut",
                    accelerator: "CmdOrCtrl+X",
                    selector: "cut:"
                }, {
                    label: "Copy",
                    accelerator: "CmdOrCtrl+C",
                    selector: "copy:"
                }, {
                    label: "Paste",
                    accelerator: "CmdOrCtrl+V",
                    selector: "paste:"
                }, {
                    label: "Select All",
                    accelerator: "CmdOrCtrl+A",
                    selector: "selectAll:"
                }, {
                    label: "Dev Tools",
                    accelerator: "Option+Cmd+I",
                    click: function () {
                        mainWindow.webContents.openDevTools()
                    }
                }
            ]
        }
    ];

    Menu.setApplicationMenu(Menu.buildFromTemplate(template));

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
        createWindow()
    }
});
