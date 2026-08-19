const https = require('https');
const originalRequest = https.request;
https.request = function(options, callback) {
    console.log('HTTPS Request:', options.method, options.hostname, options.path);
    const req = originalRequest.apply(this, arguments);
    const oldWrite = req.write;
    req.write = function(chunk) {
        if (chunk) console.log('BODY:', chunk.toString());
        return oldWrite.apply(this, arguments);
    };
    return req;
};

// Also try fetch, as newer SDKs might use node-fetch or global fetch
if (typeof fetch !== 'undefined') {
    const origFetch = fetch;
    global.fetch = async (...args) => {
        console.log('FETCH:', args[0]);
        return origFetch(...args);
    };
}

const { RealityDefender } = require('@realitydefender/realitydefender');
const fs = require('fs');
fs.writeFileSync('test.jpg', 'fake image data');

const rd = new RealityDefender({ apiKey: 'rd_536af02107cd7c60_5dce4b257de7b522f0caff41221c0ba5' });

rd.detect({ filePath: 'test.jpg' })
  .then(console.log)
  .catch(console.error);
