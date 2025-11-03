Param(
    [string]$BaseUrl = "https://rastreador-whatsapp-server.onrender.com",
    [string]$Phone = "+525512345678",
    [int]$TimeoutSec = 30
)

Write-Host "Debugging subscription endpoint..." -ForegroundColor Cyan
Write-Host "Base URL: $BaseUrl" -ForegroundColor Gray

$headers = @{ 'Content-Type' = 'application/json' }
$body = @{ phone = $Phone } | ConvertTo-Json

try {
    # Use Invoke-WebRequest to capture body even on non-2xx
    $resp = Invoke-WebRequest -Method POST -Uri "$BaseUrl/create-subscription" -Headers $headers -Body $body -TimeoutSec $TimeoutSec
    Write-Host "HTTP Status: $($resp.StatusCode)" -ForegroundColor Green
    Write-Host "Response Body:" -ForegroundColor Gray
    Write-Host $resp.Content
} catch {
    $ex = $_.Exception
    Write-Host "HTTP request failed" -ForegroundColor Red
    Write-Host "Error: $($ex.Message)" -ForegroundColor Red

    # Try to read body from the response stream
    if ($ex.Response) {
        try {
            $stream = $ex.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $content = $reader.ReadToEnd()
            Write-Host "Captured Error Response Body:" -ForegroundColor Yellow
            Write-Host $content
            
            # Attempt to parse JSON for clarity
            try {
                $json = $content | ConvertFrom-Json
                if ($json.error) {
                    Write-Host "Server Error Message: $($json.error)" -ForegroundColor Yellow
                }
            } catch { }
        } catch { }
    }
    exit 1
}

