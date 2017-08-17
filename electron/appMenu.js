const {app, Menu} = require('electron');
const {killJVM, clearCache} = require("./util");
const log = require('electron-log');
const {autoUpdater} = require("electron-updater");

var mainWindow;
var createWindow;

function updateMenuItem(progress) {
    var label = "Check for Updates";
    const finished = progress === 100;
    if (progress !== undefined) {
        label = "Downloading Update: " + Math.floor(progress) + "%"
    }
    if (progress === -1) {
        label = "No Updates available";
        setTimeout(function () {
            setMenu(mainWindow, createWindow)
        }, 20000)
    }

    if (finished) {
        label = "Install Update"
    }

    return {
        label: label,
        click: function () {
            if (finished) {
                killJVM();
                clearCache();
                autoUpdater.quitAndInstall(false);
            } else {
                autoUpdater.checkForUpdates();
            }
        }
    }
}

function setMenu(mw, cw, progress) {
    mainWindow = mw;
    createWindow = cw;
    const template = [{
        label: "Application",
        submenu: [
            {
                label: "About iWasWhere",
                selector: "orderFrontStandardAboutPanel:"
            }, {
                type: "separator"
            },
            updateMenuItem(progress),
            {
                label: "Quit and Stop Background Process",
                click: function () {
                    killJVM();
                    app.quit();
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
        label: "File",
        submenu: [
            {
                label: "New Entry",
                accelerator: "CmdOrCtrl+N",
                click: function () {
                    mainWindow.webContents.send('cmd', {msg: 'new-entry'});
                }
            }, {
                label: "New Story",
                click: function () {
                    mainWindow.webContents.send('cmd', {msg: 'new-story'});
                }
            }, {
                label: "New Saga",
                click: function () {
                    mainWindow.webContents.send('cmd', {msg: 'new-saga'});
                }
            }, {
                label: "Upload",
                accelerator: "CmdOrCtrl+U",
                click: function () {
                    mainWindow.webContents.send('cmd', {msg: 'upload'});
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
                label: "Spell Check: English",
                click: function () {
                    mainWindow.webContents.send('cmd', {msg: 'spellcheck-en'});
                }
            }, {
                label: "Spell Check: German",
                click: function () {
                    mainWindow.webContents.send('cmd', {msg: 'spellcheck-de'});
                }
            }
        ]
    }, {
        label: "View",
        submenu: [
            {
                label: "New Window",
                accelerator: "Option+Cmd+N",
                click: function () {
                    createWindow();
                }
            }, {
                label: "Dev Tools",
                accelerator: "Option+Cmd+I",
                click: function () {
                    mainWindow.webContents.openDevTools()
                }
            }, {
                label: "Hide Menu",
                click: function () {
                    mainWindow.webContents.send('cmd', {msg: 'hide-menu'});
                }
            }, {
                label: "Main View",
                click: function () {
                    mainWindow.webContents.send('cmd', {msg: 'nav-main'});
                }
            }, {
                label: "Charts",
                click: function () {
                    mainWindow.webContents.send('cmd', {msg: 'nav-charts'});
                }
            }, {
                label: "Dashboards",
                click: function () {
                    mainWindow.webContents.send('cmd', {msg: 'nav-dashboards'});
                }
            }, {
                label: "Clear Cache",
                click: clearCache
            }
        ]
    }
    ];

    Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}


autoUpdater.on('checking-for-update', () => {
    log.info('Checking for update...');
});

autoUpdater.on('update-available', (info) => {
    log.info('Update available.');
});

autoUpdater.on('update-not-available', (info) => {
    log.info('Update not available.');
    setMenu(mainWindow, createWindow, -1);
});

autoUpdater.on('error', (err) => {
    log.info('Error in auto-updater.');
});

autoUpdater.on('download-progress', (progressObj) => {
    let log_message = "Download speed: " + progressObj.bytesPerSecond;
    log_message = log_message + ' - Downloaded ' + progressObj.percent + '%';
    log_message = log_message + ' (' + progressObj.transferred + "/" + progressObj.total + ')';
    log.info(log_message);
    setMenu(mainWindow, createWindow, progressObj.percent);
});

autoUpdater.on('update-downloaded', (info) => {
    log.info('Update can be installed')
});


module.exports.setMenu = setMenu;
