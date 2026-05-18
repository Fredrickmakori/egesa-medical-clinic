## Web Platform (WASM/WebAssembly) Setup Guide

### Overview
The Egesa Medical Clinic supports deployment to web browsers via Kotlin Multiplatform's WebAssembly (WASM) compilation. The web platform runs Kotlin code compiled to WASM in the browser with full access to the Compose Multiplatform UI framework.

### Architecture

#### Key Components

1. **WASM Bundle** (`shared.js` + WASM files)
   - Compiled Kotlin code to WebAssembly and JavaScript
   - Includes Compose UI framework for browser rendering
   - Generated in `build/js/` directory on build

2. **Web Worker** (`sql-worker.js`)
   - Runs SQLite database operations off the main thread
   - Prevents UI freezing during database operations
   - Uses IndexedDB for persistent storage
   - Location: `shared/src/wasmJsMain/resources/sql-worker.js`

3. **HTML Entry Point** (`index.html`)
   - Serves the WASM bundle and initializes the app
   - Location: `shared/src/wasmJsMain/resources/index.html`

4. **Main Application** (`Main.kt`)
   - Entry point for web platform
   - Initializes DatabaseDriverFactory with WebWorkerDriver
   - Launches ClinicApp with Compose UI
   - Location: `shared/src/wasmJsMain/kotlin/com/egesa/clinic/shared/ui/Main.kt`

### How It Works

#### Database Initialization Flow

```
Browser loads index.html
    ↓
index.html loads shared.js (WASM bundle)
    ↓
Kotlin main() function executes
    ↓
DatabaseDriverFactory.createDriver() called
    ↓
WebWorkerDriver created with Worker("sql-worker.js")
    ↓
Web Worker spawned
    ↓
sql-worker.js loads shared.js in worker context
    ↓
SQLDelight queries routed to worker via message passing
    ↓
Worker executes queries, returns results to main thread
    ↓
ClinicApp renders UI with data
```

#### Data Storage

- **In-Memory**: Default, no persistence across page reloads
- **IndexedDB**: Available via WebWorkerDriver, persists in browser storage
- **Server API**: Recommended for production, provides cloud sync

### Implementation Details

#### WebWorkerDriver Configuration

File: `shared/src/wasmJsMain/kotlin/com/egesa/clinic/shared/db/DatabaseDriverFactory.kt`

```kotlin
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return WebWorkerDriver(
            worker = window.Worker("sql-worker.js")
        )
    }
}
```

**Key Points:**
- `window.Worker("sql-worker.js")` creates a new Web Worker
- The path is relative to the HTML file location
- Worker must be in the same directory as `index.html`

#### Worker Message Protocol

The web worker communicates with the main thread using a message-passing protocol:

**Main Thread → Worker:**
```javascript
{
  type: 'execute' | 'query' | 'init',
  id: <unique_message_id>,
  sql: <sql_string>,
  parameters: <parameter_array>
}
```

**Worker → Main Thread:**
```javascript
{
  id: <matching_message_id>,
  type: <operation_type>,
  result: <query_result>,
  success: true | false,
  error: <error_message_if_failed>
}
```

### Building for Web

#### Build WASM Bundle

```bash
# From project root, build wasmJs target
./gradlew.bat :shared:wasmJsProductionWebpack

# Output location:
# build/js/packages/egesa-medical-clinic-shared/kotlin/
#   - shared.js (1.68 MiB)
#   - skiko.mjs + .wasm files
```

#### Output Files

Located in `build/js/packages/egesa-medical-clinic-shared/`:

| File | Size | Purpose |
|------|------|---------|
| shared.js | 1.68 MiB | Main Kotlin/WASM bundle |
| kotlin_skiko_mjs.js | 1.21 MiB | Graphics rendering |
| egesa-medical-clinic-shared.wasm | 18.4 MiB | Core WASM code |
| skiko.wasm | 7.91 MiB | Skiko rendering WASM |

### Deployment

#### Local Testing

1. Run the build command above
2. Copy generated files to a web server:
   ```bash
   cp build/js/packages/egesa-medical-clinic-shared/* ./web-dist/
   ```
3. Serve from local web server:
   ```bash
   python -m http.server 8000
   # or
   npx http-server
   ```
4. Open browser: `http://localhost:8000`

#### Production Deployment

1. Build with optimization flags:
   ```bash
   ./gradlew.bat :shared:wasmJsProductionWebpack --release
   ```

2. Deploy to CDN or web server:
   - Upload all files from `build/js/packages/egesa-medical-clinic-shared/`
   - Ensure MIME types are correct (especially `.wasm` → `application/wasm`)

3. Configure CORS headers if calling server APIs

4. Set up appropriate caching headers for versioning

### Limitations & Considerations

#### Browser Support
- Requires WebAssembly support (Chrome 57+, Firefox 52+, Safari 11+, Edge 15+)
- IndexedDB support for persistent storage
- Web Workers for background processing

#### Performance
- Initial load: ~30 MiB (WASM + assets)
- Use service workers for offline caching
- Gzip compression reduces size to ~8-10 MiB

#### Data Synchronization
- Local changes can be synced to server API
- Use polling or WebSockets for real-time updates
- Row-level security enforced on server

#### Limitations
- Cannot directly access device hardware (camera, NFC, etc.)
- IndexedDB has per-origin storage limits (~50 MiB)
- Cross-tab communication via `BroadcastChannel` API

### Troubleshooting

#### Issue: Worker fails to load
**Error:** `TypeError: Failed to construct 'Worker'`

**Solution:**
- Ensure `sql-worker.js` is in same directory as `index.html`
- Check build output includes the worker file
- Verify web server is serving `.js` files with correct MIME type

#### Issue: Database operations timeout
**Error:** `Timeout waiting for worker response`

**Solution:**
- Worker may be overloaded with large queries
- Paginate large result sets
- Profile worker CPU usage in DevTools

#### Issue: WASM module fails to load
**Error:** `Failed to instantiate WASM module`

**Solution:**
- Check browser console for detailed error
- Ensure `.wasm` files are served with `Content-Type: application/wasm`
- Try in different browser to isolate issue
- Clear browser cache

### Testing

#### Browser DevTools
1. **Performance Tab**: Monitor main thread blocking
2. **Network Tab**: Check WASM file loading
3. **Storage → IndexedDB**: View persisted data
4. **Dedicated Worker**: Monitor worker thread

#### Lighthouse Audit
Run Lighthouse in Chrome DevTools to check:
- Performance
- Accessibility
- Best Practices
- SEO

### Future Enhancements

1. **Service Workers**: Offline support and caching
2. **Progressive Web App (PWA)**: Install as app
3. **WebGL/Canvas**: Advanced graphics rendering
4. **Geolocation API**: Map-based features
5. **Bluetooth API**: Wearable device integration

### References

- [Kotlin Multiplatform Web Target](https://kotl.in/wasm-web)
- [SQLDelight Web Worker Driver](https://cashapp.github.io/sqldelight/)
- [MDN Web Docs: WebAssembly](https://developer.mozilla.org/en-US/docs/WebAssembly)
- [Web Workers API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Workers_API)

