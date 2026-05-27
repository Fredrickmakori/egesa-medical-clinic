#!/usr/bin/env powershell
# Build and run Egesa Medical Clinic web platform locally

# Color output functions
function Write-Success {
    param([string]$Message)
    Write-Host $Message -ForegroundColor Green
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host $Message -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host $Message -ForegroundColor Cyan
}

# Script parameters
$buildDir = "build\js\packages\egesa-medical-clinic-shared"
$distDir = "web-dist"
$port = 8000

Write-Info "=========================================="
Write-Info "Egesa Medical Clinic - Web Platform Build"
Write-Info "=========================================="
Write-Info ""

# Step 1: Build WASM
Write-Info "1. Building WASM bundle..."
try {
    & .\gradlew.bat :shared:wasmJsProductionWebpack --quiet
    Write-Success "✓ WASM build completed"
} catch {
    Write-Error-Custom "✗ WASM build failed"
    exit 1
}

# Step 2: Create distribution directory
Write-Info ""
Write-Info "2. Preparing distribution files..."
if (Test-Path $distDir) {
    Remove-Item -Path $distDir -Recurse -Force
}
New-Item -ItemType Directory -Path $distDir | Out-Null
Write-Success "✓ Distribution directory created"

# Step 3: Copy files
Write-Info ""
Write-Info "3. Copying compiled files..."
try {
    Copy-Item -Path "$buildDir\*" -Destination $distDir -Recurse -Force
    Write-Success "✓ Files copied to web-dist/"
} catch {
    Write-Error-Custom "✗ Failed to copy files"
    exit 1
}

# Step 4: Display build results
Write-Info ""
Write-Info "4. Build Summary:"
Write-Host "   Location: $distDir/"
Get-ChildItem -Path $distDir -Recurse -File | ForEach-Object {
    $size = [math]::Round($_.Length / 1MB, 2)
    Write-Host "   - $($_.Name) ($size MB)"
}

# Step 5: Ask about running server
Write-Info ""
Write-Host "Do you want to start a local web server? (y/n): " -ForegroundColor Yellow -NoNewline
$response = Read-Host

if ($response -eq 'y' -or $response -eq 'Y') {
    Write-Info ""
    Write-Info "5. Starting web server..."
    Write-Success "✓ Server starting on http://localhost:$port"
    Write-Info ""
    Write-Info "Press Ctrl+C to stop the server"
    Write-Info ""

    # Try to use Python if available
    $pythonAvailable = try {
        python --version 2>&1 | Out-Null
        $true
    } catch {
        $false
    }

    if ($pythonAvailable) {
        Write-Info "Using Python HTTP server..."
        Set-Location $distDir
        python -m http.server $port
        Set-Location ..
    } else {
        # Try Node.js http-server
        $nodeAvailable = try {
            npx --version 2>&1 | Out-Null
            $true
        } catch {
            $false
        }

        if ($nodeAvailable) {
            Write-Info "Using Node.js http-server..."
            npx http-server $distDir -p $port -o
        } else {
            Write-Error-Custom "✗ No HTTP server available"
            Write-Info "Please install Python or Node.js to run the server"
            Write-Info ""
            Write-Info "Manual steps:"
            Write-Info "1. cd web-dist"
            Write-Info "2. python -m http.server 8000"
            Write-Info "3. Open http://localhost:8000 in your browser"
            exit 1
        }
    }
} else {
    Write-Info ""
    Write-Info "To start the server manually:"
    Write-Host "  cd $distDir"
    Write-Host "  python -m http.server $port"
    Write-Host ""
    Write-Host "Then open: http://localhost:$port"
}

