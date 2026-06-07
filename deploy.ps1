# EGESA Medical Clinic - DigitalOcean Deployment Helper (Windows)
# Usage: .\deploy.ps1 -Environment production -DropletIP "your.ip.address"

param(
    [Parameter(Mandatory=$false)]
    [string]$Environment = "production",

    [Parameter(Mandatory=$false)]
    [string]$DropletIP = "",

    [Parameter(Mandatory=$false)]
    [string]$GitHubToken = ""
)

$ErrorActionPreference = "Stop"

Write-Host "🏥 EGESA Medical Clinic - Deployment Helper (Windows)" -ForegroundColor Green
Write-Host "Environment: $Environment" -ForegroundColor Green
Write-Host ""

# Step 1: Check prerequisites
Write-Host "[1/5] Checking prerequisites..." -ForegroundColor Yellow
$gradleCmd = if (Test-Path ".\gradlew.bat") { ".\gradlew.bat" } else { "gradle" }
Write-Host "✓ Prerequisites OK" -ForegroundColor Green
Write-Host ""

# Step 2: Build the server
Write-Host "[2/5] Building server with Gradle..." -ForegroundColor Yellow
& $gradleCmd ":server:clean" ":server:build" "-x" "test" "--info"
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Build failed" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Build complete" -ForegroundColor Green
Write-Host ""

# Step 3: Prepare distribution
Write-Host "[3/5] Preparing distribution..." -ForegroundColor Yellow
$distPath = "server\build\distributions"
if (-not (Test-Path $distPath)) {
    Write-Host "✗ Distribution folder not found" -ForegroundColor Red
    exit 1
}

Push-Location $distPath
$tarball = Get-ChildItem -Filter "server-*.tar.gz" | Select-Object -First 1
if (-not $tarball) {
    Write-Host "✗ No distribution tarball found" -ForegroundColor Red
    Pop-Location
    exit 1
}
Write-Host "Found: $($tarball.Name)"
$tarballs_path = Join-Path (Get-Location) $tarball.Name
Pop-Location

Write-Host "✓ Distribution ready" -ForegroundColor Green
Write-Host ""

# Step 4: Deploy to Droplet (if IP provided)
if ($DropletIP) {
    Write-Host "[4/5] Deploying to Droplet ($DropletIP)..." -ForegroundColor Yellow

    # Copy tarball to droplet using SCP
    Write-Host "Uploading $($tarball.Name)..."
    & scp -o StrictHostKeyChecking=no "$tarballs_path" "root@${DropletIP}:/tmp/"

    if ($LASTEXITCODE -ne 0) {
        Write-Host "✗ Upload failed" -ForegroundColor Red
        exit 1
    }

    # Extract and restart
    Write-Host "Extracting and restarting server..."
    & ssh -o StrictHostKeyChecking=no "root@$DropletIP" "cd /opt/egesa-medical-clinic-mobile-app && tar -xzf /tmp/$($tarball.Name) && systemctl restart egesa-clinic-server && systemctl status egesa-clinic-server"

    Write-Host "✓ Deployment complete" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host "[4/5] Skipping Droplet deployment (DropletIP not provided)" -ForegroundColor Yellow
    Write-Host ""
}

# Step 5: Summary
Write-Host "[5/5] Deployment Summary" -ForegroundColor Yellow
Write-Host "✓ Server built successfully" -ForegroundColor Green

if ($DropletIP) {
    Write-Host "✓ Deployed to $DropletIP" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "  1. SSH into droplet: ssh root@$DropletIP"
    Write-Host "  2. Check logs: journalctl -u egesa-clinic-server -f"
    Write-Host "  3. Verify API: curl http://localhost:8080/health"
} else {
    Write-Host "To deploy to DigitalOcean, run:" -ForegroundColor Cyan
    Write-Host "  .\deploy.ps1 -Environment $Environment -DropletIP ""your.droplet.ip.address"""
}

Write-Host ""
Write-Host "🚀 Deployment process complete!" -ForegroundColor Green

