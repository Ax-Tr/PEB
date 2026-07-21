const fs = require('fs');

async function testAll() {
  const mobile = "9876" + Math.floor(100000 + Math.random() * 900000);
  console.log(`1. Requesting OTP for mobile: ${mobile}...`);
  const otpRes = await fetch('http://localhost:8080/api/v1/auth/otp/request', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ mobile })
  });
  const otpData = await otpRes.json();
  console.log("OTP result:", otpData);

  if (!otpData.otp) {
    console.error("No OTP in response:", otpData);
    return;
  }

  console.log("\n2. Verifying OTP...");
  const verifyRes = await fetch('http://localhost:8080/api/v1/auth/otp/verify', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ mobile, otp: otpData.otp })
  });
  const authData = await verifyRes.json();
  console.log("Auth response:", authData.accessToken ? "SUCCESS (Token Received)" : authData);

  if (!authData.accessToken) {
    console.error("Failed to authenticate");
    return;
  }

  const token = authData.accessToken;
  const headers = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  };

  const endpoints = [
    '/api/v1/analytics/pnl?year=2026&month=7',
    '/api/v1/analytics/receivables-aging',
    '/api/v1/analytics/payables-aging',
    '/api/v1/analytics/cashflow',
    '/api/v1/analytics/product-profitability?year=2026&month=7',
    '/api/v1/analytics/commitments-summary',
    '/api/v1/analytics/collection-efficiency',
    '/api/v1/analytics/freshness',
    '/api/v1/invoices?page=0&size=10',
    '/api/v1/purchase-bills?page=0&size=10',
    '/api/v1/payment-requests?page=0&size=10',
    '/api/v1/payouts?page=0&size=10'
  ];

  console.log("\n3. Testing Dashboard Endpoints:");
  for (const path of endpoints) {
    const res = await fetch(`http://localhost:8080${path}`, { headers });
    const text = await res.text();
    const isOk = res.status === 200;
    console.log(`${isOk ? '✅' : '❌'} [HTTP ${res.status}] ${path}`);
    if (!isOk) {
      console.log(`   Error: ${text.substring(0, 150)}`);
    }
  }
}

testAll().catch(console.error);
