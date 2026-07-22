# seed-vendors.ps1
# PowerShell script to seed a vendor and verified bank account in real mode.

$mobile = "9876543210"

Write-Host "1. Requesting OTP..." -ForegroundColor Cyan
$reqBody = @{ mobile = $mobile } | ConvertTo-Json
$reqRes = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/otp/request" -Method Post -Body $reqBody -ContentType "application/json"
$otp = $reqRes.otp

Write-Host "   OTP received: $otp" -ForegroundColor Gray

Write-Host "2. Verifying OTP..." -ForegroundColor Cyan
$verifyBody = @{ 
    mobile = $mobile
    otp = $otp
    device = @{
        fingerprint = "powershell-client"
        platform = "Windows"
        model = "CLI"
    }
} | ConvertTo-Json
$authRes = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/otp/verify" -Method Post -Body $verifyBody -ContentType "application/json"
$accessToken = $authRes.accessToken

Write-Host "   Access Token retrieved successfully." -ForegroundColor Gray

# Setup headers
$headers = @{
    Authorization = "Bearer $accessToken"
}

Write-Host "3. Creating Vendor..." -ForegroundColor Cyan
$vendorBody = @{
    name = "Sri Kanya"
    mobile = $mobile
    email = "srikanya@example.com"
    gstin = "27AAAAA1111A1Z1"
    address = "Visakhapatnam, AP"
} | ConvertTo-Json
$vendorRes = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/vendors" -Method Post -Body $vendorBody -ContentType "application/json" -Headers $headers
$vendorId = $vendorRes.id

Write-Host "   Vendor created: Sri Kanya (ID: $vendorId)" -ForegroundColor Green

Write-Host "4. Adding Bank Account to Vendor..." -ForegroundColor Cyan
$baBody = @{
    accountNumber = "50100123456789"
    ifsc = "HDFC0001234"
    upi = "srikanya@upi"
    bankName = "HDFC Bank"
    holderName = "Sri Kanya Enterprises"
    source = "MANUAL"
} | ConvertTo-Json
$baRes = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/vendors/$vendorId/bank-accounts" -Method Post -Body $baBody -ContentType "application/json" -Headers $headers
$baId = $baRes.id

Write-Host "   Bank Account added (ID: $baId, status: $($baRes.status))" -ForegroundColor Gray

Write-Host "5. Confirming/Verifying Bank Account..." -ForegroundColor Cyan
$confirmRes = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/vendors/$vendorId/bank-accounts/$baId/confirm" -Method Post -Headers $headers
Write-Host "   Bank Account CONFIRMED (status: $($confirmRes.status))" -ForegroundColor Green

Write-Host "`nSuccessfully seeded Vendor 'Sri Kanya' and verified bank account in real database!" -ForegroundColor Green
