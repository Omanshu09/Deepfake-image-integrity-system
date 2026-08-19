const https = require('https');

const RD_KEY = 'rd_536af02107cd7c60_5dce4b257de7b522f0caff41221c0ba5';
const RD_BASE = 'api.prd.realitydefender.xyz';

// Helper to make HTTPS request
function httpReq(options, body) {
  return new Promise((resolve, reject) => {
    const req = https.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try { resolve({ status: res.statusCode, json: JSON.parse(data) }); }
        catch (e) { resolve({ status: res.statusCode, raw: data }); }
      });
    });
    req.on('error', reject);
    if (body) req.write(body);
    req.end();
  });
}

async function testTiming() {
  // Use a known real public image
  const testImageUrl = 'https://upload.wikimedia.org/wikipedia/commons/thumb/a/a7/Camponotus_flavomarginatus_ant.jpg/320px-Camponotus_flavomarginatus_ant.jpg';
  
  console.log('='.repeat(60));
  console.log('Reality Defender API Timing Test');
  console.log('='.repeat(60));
  console.log('Image URL:', testImageUrl);
  console.log('');

  // ── Step 1: Get presigned URL ──────────────────────────────────────
  const t1 = Date.now();
  console.log('[1] Getting presigned S3 URL...');
  const presigned = await httpReq({
    hostname: RD_BASE,
    path: '/api/files/aws-presigned',
    method: 'POST',
    headers: { 'X-API-KEY': RD_KEY, 'Content-Type': 'application/json' }
  }, JSON.stringify({ fileName: 'test.jpg' }));
  const t2 = Date.now();
  console.log(`    ✓ Presigned URL received in ${t2 - t1}ms`);
  
  if (!presigned.json?.response?.signedUrl) {
    console.error('ERROR: No signed URL returned:', JSON.stringify(presigned));
    return;
  }

  const signedUrl = presigned.json.response.signedUrl;
  const requestId = presigned.json.requestId;
  console.log(`    requestId: ${requestId}`);
  console.log('');

  // ── Step 2: Download + upload image ────────────────────────────────
  console.log('[2] Downloading test image...');
  const imgBytes = await new Promise((resolve, reject) => {
    https.get(testImageUrl, (res) => {
      const chunks = [];
      res.on('data', c => chunks.push(c));
      res.on('end', () => resolve(Buffer.concat(chunks)));
      res.on('error', reject);
    });
  });
  console.log(`    ✓ Downloaded ${imgBytes.length} bytes`);

  const t3 = Date.now();
  console.log('[3] Uploading to S3...');
  const s3Url = new URL(signedUrl);
  const uploadPath = s3Url.pathname + s3Url.search;
  await httpReq({
    hostname: s3Url.hostname,
    path: uploadPath,
    method: 'PUT',
    headers: { 'Content-Type': 'image/jpeg', 'Content-Length': imgBytes.length }
  }, imgBytes);
  const t4 = Date.now();
  console.log(`    ✓ Uploaded in ${t4 - t3}ms`);
  console.log('');

  // ── Step 3: Poll for result ─────────────────────────────────────────
  console.log('[4] Polling for results (every 3 seconds)...');
  const tPollStart = Date.now();
  
  for (let i = 0; i < 30; i++) {
    await new Promise(r => setTimeout(r, 3000));
    const elapsed = Date.now() - tPollStart;
    const result = await httpReq({
      hostname: RD_BASE,
      path: `/api/media/users/${requestId}`,
      method: 'GET',
      headers: { 'X-API-KEY': RD_KEY }
    });
    
    const status = result.json?.status;
    const score  = result.json?.score;
    console.log(`    Attempt ${i+1} (${(elapsed/1000).toFixed(1)}s): status=${status}, score=${score}`);
    
    if (status === 'COMPLETE' || status === 'AUTHENTIC' || status === 'MANIPULATED' || status === 'SUSPICIOUS') {
      const totalMs = Date.now() - t1;
      console.log('');
      console.log('='.repeat(60));
      console.log('✅ RESULT RECEIVED!');
      console.log(`   Status:     ${status}`);
      console.log(`   Score:      ${score}`);
      console.log(`   Models:     ${JSON.stringify(result.json?.models?.map(m => m.name) || [])}`);
      console.log('');
      console.log('⏱  TIMING BREAKDOWN:');
      console.log(`   Presigned URL:  ${t2-t1}ms`);
      console.log(`   S3 Upload:      ${t4-t3}ms`);
      console.log(`   Poll wait:      ${elapsed}ms`);
      console.log(`   TOTAL END-TO-END: ${totalMs}ms (~${(totalMs/1000).toFixed(1)}s)`);
      console.log('='.repeat(60));
      console.log('Full result:', JSON.stringify(result.json, null, 2));
      process.exit(0);
    }
    
    if (status === 'ERROR' || status === 'FAILED') {
      console.log('❌ Analysis failed!');
      process.exit(1);
    }
  }
  
  console.log('⏰ Timed out after 90 seconds');
}

testTiming().catch(console.error);
