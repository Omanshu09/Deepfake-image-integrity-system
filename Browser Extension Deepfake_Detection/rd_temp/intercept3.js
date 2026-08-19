const https = require('https');
const originalRequest = https.request;

https.request = function(options, callback) {
    const req = originalRequest.call(this, options, function(res) {
        if (options.hostname.includes('realitydefender')) {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                console.log('RESPONSE from', options.path, ':', data);
                if (data.includes('"status":"COMPLETE"')) {
                    process.exit(0);
                }
            });
        }
        if (callback) callback(res);
    });
    return req;
};

const { RealityDefender } = require('@realitydefender/realitydefender');
const fs = require('fs');
fs.writeFileSync('test.jpg', Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=', 'base64'));

const rd = new RealityDefender({ apiKey: 'rd_536af02107cd7c60_5dce4b257de7b522f0caff41221c0ba5' });

rd.detect({ filePath: 'test.jpg' })
  .then(console.log)
  .catch(console.error);
