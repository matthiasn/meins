const express = require('express');
const svc = express();
const os = require('os');
const fs = require('fs');
const geocoder = require('local-reverse-geocoder');
const log = require('electron-log');
const tcpPortUsed = require('tcp-port-used');
const npid = require('npid');
const win32 = process.platform === "win32";

const PORT = Number(process.env.GEOPORT || 3003);

log.transports.file.level = 'info';
log.transports.file.format = '{h}:{i}:{s}:{ms} {text}';

if (!win32) {
    log.transports.file.file = '/tmp/geocoder.log';
}

console.log = function (d) {
    log.info("GEOCODER:", d);
};

svc.get(/geocode/, function (req, res) {
    const lat = req.query.latitude || false;
    const lon = req.query.longitude || false;
    const maxResults = req.query.maxResults || 1;
    if (!lat || !lon) {
        return res.status(400).send('Bad Request');
    }
    const points = [];
    if (Array.isArray(lat) && Array.isArray(lon)) {
        if (lat.length !== lon.length) {
            return res.status(400).send('Bad Request');
        }
        for (var i = 0, lenI = lat.length; i < lenI; i++) {
            points[i] = {latitude: lat[i], longitude: lon[i]};
        }
    } else {
        points[0] = {latitude: lat, longitude: lon};
    }
    geocoder.lookUp(points, maxResults, function (err, addresses) {
        if (err) {
            return res.status(500).send(err);
        }
        log.info("GEOCODER: found:", lat, lon);
        return res.send(addresses);
    });
});

const pid = process.pid;

function initGeocoderSvc() {
    const tmpDir = win32 ? os.tmpdir() : "/tmp";
    const dumpDir = tmpDir + "/geonames";
    fs.mkdirSync(dumpDir);
    const pidFile = dumpDir + '/geocoder.pid';
    log.info("GEOCODER: starting on port", PORT, "PID", pid);
    npid.remove(pidFile);

    try {
        const pid = npid.create(pidFile);
        pid.removeOnExit();
    } catch (err) {
        log.info(err);
        process.exit(1);
    }

    geocoder.init({
        dumpDirectory: dumpDir,
        load: {
            admin1: true,
            admin2: true,
            admin3And4: false,
            alternateNames: false
        }
    }, function () {
        svc.listen(PORT, 'localhost', function () {
            log.info('GEOCODER: listening on port ' + PORT);
        });
    });
}

log.info("GEOCODER: check PORT", PORT, "PID", pid);

tcpPortUsed.check(PORT)
    .then(function (inUse) {
        log.info("GEOCODER: in use", inUse);
        if (inUse) {
            log.info("GEOCODER: Port already in use:", PORT);
            process.exit(1);
        } else {
            initGeocoderSvc();
        }
    }, function (err) {
        log.info('GEOCODER: Error on check:', err.message);
        process.exit(1);
    });
