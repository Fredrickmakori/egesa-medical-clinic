# Web Platform Quick Start Guide

This guide will help you build, deploy, and run the Egesa Medical Clinic application on the web platform using WebAssembly (WASM).

## Quick Start (5 minutes)

### Prerequisites
- Windows with PowerShell
- Java 21+ (for Gradle)
- Python 3.x OR Node.js (for local testing)

### Build & Run Locally

1. **Build the WASM bundle:**
   ```powershell
   .\build-web.ps1
   ```
   
   The script will:
   - Compile Kotlin code to WebAssembly
   - Generate JavaScript bundles
   - Copy files to `web-dist/` directory
   - Optionally start a local web server

2. **Access the app:**
   ```
   http://localhost:8000
   ```

---

## Detailed Setup

### 1. Project Structure

```
egesa-medical-clinic-mobile-app/
├── shared/
│   ├── src/wasmJsMain/
│   │   ├── kotlin/
│   │   │   ├── Main.kt                    # Entry point for web
│   │   │   └── db/DatabaseDriverFactory.kt # WebWorkerDriver setup
│   │   └── resources/
│   │       ├── index.html                 # HTML entry point
│   │       └── sql-worker.js              # SQLite Web Worker
│   ├── build.gradle.kts                   # WASM build config
│   └── ...
├── build-web.ps1                          # Build script
├── web.config                             # IIS configuration
├── nginx.conf                             # Nginx configuration
├── .htaccess                              # Apache configuration
└── docs/WEB_PLATFORM_SETUP.md            # Detailed guide
```

### 2. Build Process

#### Manual Build (without script)

```powershell
# Build WASM bundle
.\gradlew.bat :shared:wasmJsProductionWebpack

# Copy output
mkdir web-dist -Force
Copy-Item -Path "shared/build/js/packages/egesa-medical-clinic-shared/*" -Destination "web-dist" -Recurse
```

#### Output Files

Build produces in `shared/build/js/packages/egesa-medical-clinic-shared/`:
- **shared.js** (1.68 MiB) - Main Kotlin/WASM bundle
- **kotlin_skiko_mjs.js** (1.21 MiB) - Graphics library
- **egesa-medical-clinic-shared.wasm** (18.4 MiB) - Core WASM code
- **skiko.wasm** (7.91 MiB) - Rendering engine
- **index.html** - Web page template
- **sql-worker.js** - Database worker

**Total Size: ~30 MiB (uncompressed), ~8-10 MiB (gzip)**

### 3. Local Testing

#### Using Python

```powershell
cd web-dist
python -m http.server 8000
# Open: http://localhost:8000
```

#### Using Node.js

```powershell
cd web-dist
npx http-server -p 8000 -o
```

#### Using Built-in Script

```powershell
.\build-web.ps1
# Choose 'y' when prompted to start server
```

### 4. Deployment Options

#### Option A: GitHub Pages (Free)

1. **Enable GitHub Pages:**
   - Go to repository Settings → Pages
   - Set source to `main` branch → `/docs` folder

2. **Build for GitHub:**
   ```powershell
   .\gradlew.bat :shared:wasmJsProductionWebpack
   mkdir -Force docs
   Copy-Item "shared/build/js/packages/egesa-medical-clinic-shared/*" -Destination "docs" -Recurse
   git add docs/
   git commit -m "Deploy web platform to GitHub Pages"
   git push
   ```

3. **Access:**
   ```
   https://username.github.io/egesa-medical-clinic-mobile-app/
   ```

#### Option B: Netlify (Recommended)

1. **Connect repository:**
   - Go to [Netlify](https://netlify.com)
   - Click "New service" → "Connect to Git"
   - Select your repository

2. **Configure build:**
   ```
   Build command: ./gradlew.bat :shared:wasmJsProductionWebpack
   Publish directory: shared/build/js/packages/egesa-medical-clinic-shared
   ```

3. **Deploy:**
   - Netlify auto-deploys on push
   - Custom domain configuration available

#### Option C: Traditional Web Server

##### IIS (Windows)

```powershell
# 1. Copy web.config to deployment directory
Copy-Item "web.config" -Destination "C:\inetpub\wwwroot\egesa-clinic"

# 2. Copy WASM bundle
Copy-Item "web-dist\*" -Destination "C:\inetpub\wwwroot\egesa-clinic" -Recurse

# 3. In IIS Manager:
#    - Add application pointing to directory
#    - Ensure MIME type .wasm → application/wasm
```

##### Nginx (Linux)

```bash
# 1. Copy nginx.conf settings to your server block
sudo cp nginx.conf /etc/nginx/sites-available/egesa-clinic

# 2. Deploy WASM bundle
sudo cp -r web-dist/* /var/www/egesa-clinic

# 3. Test and reload
sudo nginx -t
sudo systemctl reload nginx
```

##### Apache (Linux/Windows)

```bash
# 1. Copy .htaccess to web root
cp .htaccess /var/www/html/egesa-clinic/

# 2. Deploy WASM bundle
cp -r web-dist/* /var/www/html/egesa-clinic/

# 3. Enable mod_rewrite
sudo a2enmod rewrite
sudo systemctl reload apache2
```

#### Option D: Docker

```dockerfile
# Dockerfile
FROM nginx:latest

# Copy nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy WASM bundle
COPY web-dist/ /usr/share/nginx/html/

EXPOSE 80
```

```bash
# Build and run
docker build -t egesa-clinic:web .
docker run -p 8080:80 egesa-clinic:web
```

### 5. Performance Optimization

#### Image Optimization
```powershell
# Install ImageMagick
choco install imagemagick

# Optimize images
magick convert input.png -resize 1200x700 -quality 85 output.webp
```

#### Asset Compression
```powershell
# Using 7-Zip
7z a -ttar archive.tar.gz web-dist/
```

#### Build Optimization
```powershell
# Production build with optimizations
.\gradlew.bat :shared:wasmJsProductionWebpack -Poptimize=true
```

### 6. Security Considerations

#### HTTPS Configuration

```powershell
# Generate self-signed cert for testing
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes
```

#### CORS Headers

If calling external APIs, ensure proper CORS configuration:

```powershell
# Add header to responses
Header set "Access-Control-Allow-Origin" "https://yourdomain.com"
```

#### Content Security Policy

```html
<!-- Add to index.html <head> -->
<meta http-equiv="Content-Security-Policy" 
      content="default-src 'self'; script-src 'self' 'wasm-unsafe-eval'; 
               connect-src 'self' https://api.yourserver.com;">
```

### 7. Monitoring & Analytics

#### Browser Console Debugging
```javascript
// Check IndexedDB (database storage)
indexedDB.databases()

// Check localStorage
console.log(localStorage)

// Check Service Workers
navigator.serviceWorker.getRegistrations()
```

#### Performance Monitoring
- Use Chrome DevTools → Performance tab
- Monitor Network tab for WASM file loading
- Check Application → Storage for database usage

### 8. Troubleshooting

#### Issue: "Failed to fetch shared.js"
- **Cause:** Missing or incorrect MIME types
- **Fix:** Ensure `.js` files served as `application/javascript`

#### Issue: "WASM module failed to load"
- **Cause:** MIME type for `.wasm` not set correctly
- **Fix:** Configure `.wasm` → `application/wasm`

#### Issue: Database operations slow
- **Cause:** Large queries blocking worker thread
- **Fix:** Paginate queries, use pagination in UI

#### Issue: Page works offline, then breaks online
- **Cause:** Service Worker caching issue
- **Fix:** Clear browser cache, disable aggressive caching

### 9. Advanced Topics

#### Service Workers (Offline Support)

```javascript
// Create service-worker.js
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/service-worker.js');
}
```

#### Progressive Web App (PWA)

Add to `index.html` `<head>`:
```html
<meta name="theme-color" content="#2196F3">
<link rel="manifest" href="/manifest.json">
<link rel="icon" href="/icon-192x192.png">
```

#### Real-time Updates with WebSockets

```kotlin
// In Kotlin code
val webSocket = WebSocket("wss://api.your-server.com/updates")
webSocket.onmessage = { event ->
    // Update UI with new data
}
```

### 10. Support & Resources

#### Official Documentation
- [Kotlin Multiplatform Web](https://kotl.in/wasm-web)
- [SQLDelight](https://cashapp.github.io/sqldelight/)
- [WebAssembly](https://webassembly.org/)

#### Tools
- [Chrome DevTools](https://developer.chrome.com/docs/devtools/)
- [WebAssembly Explorer](https://mbebenita.github.io/WasmExplorer/)
- [Lighthouse](https://developers.google.com/web/tools/lighthouse)

#### Community
- [Kotlin Slack](https://slack.kotlinlang.org/)
- [SQLDelight GitHub](https://github.com/cashapp/sqldelight)

---

## Next Steps

1. ✅ Build the WASM bundle locally
2. ✅ Test on `localhost:8000`
3. ✅ Deploy to Netlify or GitHub Pages
4. ✅ Set up custom domain
5. ✅ Configure SSL/TLS
6. ✅ Monitor performance metrics

Good luck! 🚀

