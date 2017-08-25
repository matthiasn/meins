// Node.JS externs: https://github.com/dcodeIO/node.js-closure-compiler-externs

var electron = {};
electron.dialog = function() {};
electron.app = {};
electron.app.quit = function() {};

electron.ipcRenderer = function() {};
electron.ipcMain = function() {};
electron.on = function() {};
electron.send = function() {};
electron.remote = function() {};
electron.require = function() {};
electron.buildFromTemplate = function() {};
electron.popup = function() {};
electron.getCurrentWindow = function() {};
electron.showErrorBox = function() {};
electron.setTitle = function() {};
electron.setRepresentedFilename = function() {};
electron.showMessageBox = function() {};
electron.getPath = function() {};
electron.showSaveDialog = function() {};
electron.showOpenDialog = function() {};

electron.BrowserWindow = function () {};

electron.BrowserWindow.on = function() {};
electron.BrowserWindow.loadURL = function() {};
electron.BrowserWindow.webContents = {};
electron.BrowserWindow.webContents.openDevTools = function() {};

electron.Menu = {};
electron.Menu.buildFromTemplate = function() {};
electron.Menu.setApplicationMenu = function() {};

/**
 * @constructor
 * @extends events.EventEmitter
 */
var process = function() {};

/**
 * @type {string}
 */
process.platform;

/**
 * @return {string}
 * @nosideeffects
 */
process.cwd = function() {};
/**
 * @type {string}
 */
process.platform;
/**
 * @type {string}
 */
process.resourcesPath;


var electronUpdater = {};
electronUpdater.autoUpdater = {};
electronUpdater.autoUpdater.on = function() {};
electronUpdater.autoUpdater.logger = function() {};
electronUpdater.autoUpdater.checkForUpdates = function() {};
electronUpdater.autoUpdater.downloadUpdate = function() {};
electronUpdater.autoUpdater.quitAndInstall = function() {};

var document = {};
document.querySelector = {};
document.querySelector.getWebContents = function() {};
document.querySelector.getWebContents.executeJavaScript = function() {};
