const {app, BrowserWindow, Menu} = require('electron');
const fetch = require('electron-fetch');
const shell = require('electron').shell;
const child_process = require('child_process');
const path = require('path')
const url = require('url')

process.env.GOOGLE_API_KEY = 'AIzaSyD78NTnhgt--LCGBdIGPEg8GtBYzQl0gKU'

// require('electron-context-menu')({
//     prepend: (params, browserWindow) => [{
//         label: 'Rainbow',
//         // Only show it when right-clicking images
//         //visible: true,
//         showInspectElement: true
//     }]
// });

// Keep a global reference of the window object, if you don't, the window will
// be closed automatically when the JavaScript object is garbage collected.
let mainWindow

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
    // mainWindow.loadURL(url.format({
    //     pathname: path.join(__dirname, 'index.html'),
    //     protocol: 'file:',
    //     slashes: true
    // }))

    mainWindow.loadURL("http://localhost:7777/")

    // Open the DevTools.
    mainWindow.webContents.openDevTools()

    // Emitted when the window is closed.
    mainWindow.on('closed', function () {
        // Dereference the window object, usually you would store windows
        // in an array if your app supports multi windows, this is the time
        // when you should delete the corresponding element.
        mainWindow = null
    })
}

function delayed() {
    console.log("delayed");
    setTimeout(createWindow, 10000);
}

function waitUntilUp() {
    fetch("http://localhost:7777")
        .then(
            function (response) {
                if (response.status !== 200) {
                    console.log('Looks like there was a problem. Status Code: ' +
                        response.status);
                    return;
                }

                // Examine the text in the response
                response.text().then(function (data) {
                    console.log("up");
                    createWindow();
                });
            }
        )
        .catch(function (err) {
            //console.log('Fetch Error :-S', err);
            console.log("retry");
            setTimeout(waitUntilUp, 2000);
        });
}

function start() {
    // // exec: spawns a shell.
    // child_process.exec('./run-packaged.sh', function (error, stdout, stderr) {
    //     console.log(stdout);
    // });
    // waitUntilUp();
    createWindow();

    // Create the Application's main menu
    var template = [{
        label: "Application",
        submenu: [
            { label: "About Application", selector: "orderFrontStandardAboutPanel:" },
            { type: "separator" },
            { label: "Quit", accelerator: "Command+Q", click: function() { app.quit(); }}
        ]}, {
        label: "Edit",
        submenu: [
            { label: "Undo", accelerator: "CmdOrCtrl+Z", selector: "undo:" },
            { label: "Redo", accelerator: "Shift+CmdOrCtrl+Z", selector: "redo:" },
            { type: "separator" },
            { label: "Cut", accelerator: "CmdOrCtrl+X", selector: "cut:" },
            { label: "Copy", accelerator: "CmdOrCtrl+C", selector: "copy:" },
            { label: "Paste", accelerator: "CmdOrCtrl+V", selector: "paste:" },
            { label: "Select All", accelerator: "CmdOrCtrl+A", selector: "selectAll:" }
        ]}
    ];

    Menu.setApplicationMenu(Menu.buildFromTemplate(template));

}

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.on('ready', start)

// Quit when all windows are closed.
app.on('window-all-closed', function () {
    // On OS X it is common for applications and their menu bar
    // to stay active until the user quits explicitly with Cmd + Q
    if (process.platform !== 'darwin') {
        app.quit()
    }
})

app.on('activate', function () {
    // On OS X it's common to re-create a window in the app when the
    // dock icon is clicked and there are no other windows open.
    if (mainWindow === null) {
        createWindow()
    }
})

// In this file you can include the rest of your app's specific main process
// code. You can also put them in separate files and require them here.
