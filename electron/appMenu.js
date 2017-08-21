const {app, Menu} = require('electron');
const {killJVM, clearCache, clearIwwCache} = require("./util");
const log = require('electron-log');
const {autoUpdater} = require("electron-updater");

function updateMenuItem(progress, currWindow, createWindow) {
    var label = "Check for Updates";
    const finished = progress === 100;
    if (progress !== undefined) {
        label = "Downloading Update: " + Math.floor(progress) + "%"
    }
    if (progress === -1) {
        label = "No Updates available";
        setTimeout(() => setMenu(currWindow, createWindow), 20000)
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
                clearIwwCache();
                autoUpdater.quitAndInstall(false);
            } else {
                autoUpdater.checkForUpdates();
            }
        }
    }
}

function sendToWindow(currentWindow, msg) {
    return (ev) => {
        try {
            currentWindow.webContents.send('cmd', {msg: msg});
        } catch (err) {
            log.error("MENU error:", err)
        }
    }
}

var currWindowCache;
var createWindowCache;

function setMenu(currWindow, createWindow, progress) {
    currWindowCache = currWindow;
    createWindowCache = createWindow;

    const template = [{
        label: "Application",
        submenu: [
            {
                label: "About iWasWhere",
                selector: "orderFrontStandardAboutPanel:"
            }, {
                type: "separator"
            },
            updateMenuItem(progress, currWindow, createWindow),
            {
                label: "Quit and Stop Background Process",
                click: function () {
                    killJVM();
                    app.quit();
                }
            }, {
                label: "Close Window",
                accelerator: "Cmd+W",
                click: () => currWindow.close()
            }, {
                label: "Quit",
                accelerator: "Cmd+Q",
                click: () => app.quit()
            }
        ]
    }, {
        label: "File",
        submenu: [
            {
                label: "New Entry",
                accelerator: "CmdOrCtrl+N",
                click: sendToWindow(currWindow, 'new-entry')
            }, {
                label: "New Story",
                click: sendToWindow(currWindow, 'new-story')
            }, {
                label: "New Saga",
                click: sendToWindow(currWindow, 'new-saga')
            }, {
                label: "Upload",
                accelerator: "CmdOrCtrl+U",
                click: sendToWindow(currWindow, 'upload')
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
                label: "Spelling",
                submenu: [
                    {
                        label: "English",
                        click: sendToWindow(currWindow, 'spellcheck-en')
                    }, {
                        label: "French",
                        click: sendToWindow(currWindow, 'spellcheck-fr')
                    }, {
                        label: "German",
                        click: sendToWindow(currWindow, 'spellcheck-de')
                    }, {
                        label: "Italian",
                        click: sendToWindow(currWindow, 'spellcheck-it')
                    }, {
                        label: "Spanish",
                        click: sendToWindow(currWindow, 'spellcheck-es')
                    }, {
                        type: "separator"
                    }, {
                        label: "None",
                        click: sendToWindow(currWindow, 'spellcheck-none')
                    }
                ]
            }
        ]
    }, {
        label: "View",
        submenu: [
            {
                label: "New Window",
                accelerator: "Option+Cmd+N",
                click: createWindow
            }, {
                label: "Dev Tools",
                accelerator: "Option+Cmd+I",
                click: () => currWindow.webContents.openDevTools()

            }, {
                label: "Hide Menu",
                click: sendToWindow(currWindow, 'hide-menu')
            }, {
                label: "Charts",
                click: sendToWindow(currWindow, 'nav-charts')
            }, {
                label: "Dashboards",
                click: sendToWindow(currWindow, 'nav-dashboards')
            }, {
                label: "Clear Cache",
                click: clearCache
            }, {
                label: "Clear Cache #2",
                click: clearIwwCache
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
    setMenu(currWindowCache, createWindowCache, -1);
});

autoUpdater.on('error', (err) => {
    log.info('Error in auto-updater.');
});

autoUpdater.on('download-progress', (progressObj) => {
    let log_message = "Download speed: " + progressObj.bytesPerSecond;
    log_message = log_message + ' - Downloaded ' + progressObj.percent + '%';
    log_message = log_message + ' (' + progressObj.transferred + "/" + progressObj.total + ')';
    log.info(log_message);
    setMenu(currWindowCache, createWindowCache, progressObj.percent);
});

autoUpdater.on('update-downloaded', (info) => {
    log.info('Update can be installed')
});


module.exports.setMenu = setMenu;
