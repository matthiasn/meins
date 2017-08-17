const {Menu} = require('electron');
const {killJVM, clearCache} = require("./util");

function setMenu (waitUntilUp, autoUpdater, mainWindow, app) {
    const template = [{
        label: "Application",
        submenu: [
            {
                label: "About iWasWhere",
                selector: "orderFrontStandardAboutPanel:"
            }, {
                type: "separator"
            }, {
                label: "Check for Updates",
                click: function () {
                    autoUpdater.checkForUpdates();
                }
            }, {
                label: "Install Updates",
                click: function () {
                    killJVM();
                    clearCache();
                    autoUpdater.quitAndInstall(false);
                }
            }, {
                label: "Quit and Stop Background Process",
                accelerator: "Cmd+Q",
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

module.exports.setMenu = setMenu;
