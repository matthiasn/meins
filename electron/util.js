const {app} = require('electron');
const path = require('path');
const log = require('electron-log');
const fs = require('fs');
const {session} = require('electron');
const {spawn} = require('child_process');

const userData = app.getPath("userData");

function killJVM() {
    const pidFile = path.normalize(userData + "/iwaswhere.pid");
    const pid = fs.readFileSync(pidFile, "utf8");
    log.warn("shutting down", pid);
    if (process.platform === 'win32') {
        spawn('TaskKill', ["-F", "/PID," +pid], {});
    } else {
        spawn('/bin/kill', ["-KILL", +pid], {});
    }
}

function clearCache() {
    const ses = session.defaultSession;
    ses.clearCache(() => {
        log.info("cleared cache");
    });
}

module.exports.killJVM = killJVM;
module.exports.clearCache = clearCache;
