# Responsive UI Design System

## Overview

The Egesa Medical Clinic now supports **responsive design** that automatically adapts to different screen sizes and orientations across all platforms:

- **COMPACT** (< 600 dp): Mobile phones - single column, bottom navigation
- **MEDIUM** (600-839 dp): Small tablets, foldables - collapsible sidebar, 2-column grids
- **EXPANDED** (>= 840 dp): Large tablets, desktops - full sidebar, 3-column grids

---

## Architecture

### Responsive Utilities (`ResponsiveUtils.kt`)

Core system providing screen size detection and responsive values:

```kotlin
// Detect current screen size
val windowSize = currentWindowSizeClass()
println(windowSize.widthSize)  // COMPACT, MEDIUM, or EXPANDED

// Get responsive values
val padding = responsiveHorizontalPadding()  // 16, 20, or 24 dp
val spacing = responsiveItemSpacing()         // 8, 12, or 16 dp
val columns = responsiveGridColumns()         // 1, 2, or 3

// Check conditions
if (isWideLayout()) { ... }          // true on MEDIUM+ screens
if (shouldUseCompactUI()) { ... }     // true on COMPACT screens
if (shouldShowSidebar()) { ... }      // true on MEDIUM+ screens
```

### Screen Size Classes

```
COMPACT (Phone)
├─ Width: < 600 dp
├─ Navigation: Bottom nav bar
├─ Sidebar: Hidden
├─ Columns: 1
└─ Padding: 16 dp

MEDIUM (Tablet)
├─ Width: 600-839 dp
├─ Navigation: Collapsible sidebar
├─ Sidebar: Visible, can toggle
├─ Columns: 2
└─ Padding: 20 dp

EXPANDED (Desktop)
├─ Width: >= 840 dp
├─ Navigation: Full sidebar
├─ Sidebar: Always visible
├─ Columns: 3
└─ Padding: 24 dp
```

---

## Responsive Shell

The new `ResponsiveShell` automatically adapts to screen size:

**COMPACT Layout (Mobile)**
```
┌─────────────────────┐
│  Header / Top Bar   │
├─────────────────────┤
│                     │
│     Content Area    │
│                     │
├─────────────────────┤
│  Bottom Navigation  │
└─────────────────────┘
```

**MEDIUM Layout (Tablet)**
```
┌──────────────────────────────────┐
│        Sidebar | Header / Top Bar │
├──────┬────────────────────────────┤
│      │                            │
│ Nav  │   Content Area             │
│      │                            │
└──────┴────────────────────────────┘
```

**EXPANDED Layout (Desktop)**
```
┌──────────────┬──────────────────────────────────┐
│              │  Header / Top Bar                 │
│   Sidebar    ├──────────────────────────────────┤
│   (240 dp)   │                                  │
│              │   Content Area                   │
│              │   (3-column grid)                │
│              │                                  │
└──────────────┴──────────────────────────────────┘
```

---

## Usage Examples

### 1. Responsive Column

```kotlin
ResponsiveColumn {
    Text("This content adapts to screen width")
    Button(onClick = {}) { Text("Click me") }
}
```

### 2. Responsive Row (Switches to Column on Mobile)

```kotlin
ResponsiveRow {
    Box(Modifier.weight(1f)) {
        Card { Text("Left side") }
    }
    Box(Modifier.weight(1f)) {
        Card { Text("Right side") }
    }
}
```

### 3. Responsive Padding

```kotlin
Column(
    Modifier
        .fillMaxWidth()
        .padding(horizontal = responsiveHorizontalPadding().dp)
) {
    Text("Padded based on screen size")
}
```

### 4. Responsive Grid

```kotlin
// 1 column on mobile, 2 on tablet, 3 on desktop
val columns = responsiveGridColumns()

LazyColumn {
    items(items.chunked(columns)) { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { item ->
                Card(Modifier.weight(1f)) {
                    ItemContent(item)
                }
            }
        }
    }
}
```

### 5. Conditional Layout

```kotlin
if (isWideLayout()) {
    // Tablet/Desktop: Side-by-side layout
    Row {
        Panel1(Modifier.weight(1f))
        Panel2(Modifier.weight(1f))
    }
} else {
    // Mobile: Stacked layout
    Column {
        Panel1()
        Panel2()
    }
}
```

### 6. Sidebar Visibility

```kotlin
if (shouldShowSidebar()) {
    Sidebar()  // Visible on MEDIUM+
}

if (shouldUseCompactUI()) {
    MobileBottomNav()  // Visible on COMPACT
}
```

---

## Breakpoint Reference

### Width Breakpoints
| Size | Range | Use Case | Navigation |
|------|-------|----------|-----------|
| COMPACT | < 600 dp | Phones (portrait) | Bottom Nav |
| MEDIUM | 600-839 dp | Tablets (portrait), Foldables | Sidebar |
| EXPANDED | >= 840 dp | Tablets (landscape), Desktops | Sidebar |

### Height Breakpoints
| Size | Range | Use Case |
|------|-------|----------|
| COMPACT | < 480 dp | Portrait phones |
| MEDIUM | 480-899 dp | Most phones/tabs |
| EXPANDED | >= 900 dp | Landscape, large screens |

---

## Responsive Values

### Padding & Margins
```
                COMPACT  MEDIUM  EXPANDED
Horizontal       16 dp    20 dp    24 dp
Vertical          8 dp    12 dp    16 dp
Item Spacing      8 dp    12 dp    16 dp
```

### Layout
```
                COMPACT  MEDIUM  EXPANDED
Sidebar Width       0 dp   240 dp   280 dp
Grid Columns        1        2        3
Max Content Width   ∞      ∞       1200 dp
Top Bar Height     56 dp   56 dp    64 dp
Bottom Nav Height  80 dp    0 dp     0 dp
```

### Typography
```
Font Scale      0.9x     1.0x     1.1x
```

---

## Best Practices

### 1. **Always Use Responsive Utilities**
```kotlin
// ✅ Good
padding = responsiveHorizontalPadding().dp

// ❌ Avoid hardcoding
padding = 16.dp
```

### 2. **Test on Multiple Screen Sizes**
- Use Android emulator with different phone/tablet configs
- Use browser dev tools for web
- Test landscape and portrait orientations

### 3. **Use Weight for Flexible Layouts**
```kotlin
// ✅ Content expands to fill available space
Row {
    Box(Modifier.weight(1f)) { ... }
    Box(Modifier.weight(1f)) { ... }
}
```

### 4. **Constrain Content Width on Large Screens**
```kotlin
// ✅ Prevent content sprawl on very wide screens
Box(Modifier.widthIn(max = 1200.dp)) {
    content()
}
```

### 5. **Handle Touch Targets Appropriately**
- Mobile: 48 dp minimum touch target
- Desktop: Can use smaller targets (32 dp)
- Use `Modifier.minimumInteractiveComponentSize()`

---

## Common Patterns

### Pattern 1: Dashboard Grid

```kotlin
val columns = responsiveGridColumns()

LazyColumn {
    items(metrics.chunked(columns)) { row ->
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(responsiveItemSpacing().dp)
        ) {
            row.forEach { metric ->
                MetricCard(
                    metric,
                    Modifier.weight(1f)
                )
            }
        }
    }
}
```

### Pattern 2: List with Sidebar

```kotlin
if (shouldShowSidebar()) {
    Row {
        Sidebar(Modifier.width(240.dp))
        MainContent(Modifier.weight(1f))
    }
} else {
    Column {
        TopBar()
        MainContent(Modifier.weight(1f))
        BottomNav()
    }
}
```

### Pattern 3: Two-Panel Layout

```kotlin
if (isWideLayout()) {
    Row(Modifier.fillMaxSize()) {
        LeftPanel(Modifier.weight(1f))
        RightPanel(Modifier.weight(1f))
    }
} else {
    LazyColumn(Modifier.fillMaxSize()) {
        item { LeftPanel() }
        item { RightPanel() }
    }
}
```

---

## Troubleshooting

### Issue: Content not adapting to screen size
**Solution:** Make sure you're using `currentWindowSizeClass()` in a `@Composable` context and use `Modifier.fillMaxWidth()`.

### Issue: Bottom nav not showing on mobile
**Solution:** Verify screen width is < 600 dp. Check `shouldUseCompactUI()` returns true.

### Issue: Sidebar doesn't collapse
**Solution:** Ensure `shouldShowSidebar()` is being called inside composable scope.

### Issue: Different layouts on different devices
**Solution:** This is intentional! Test on actual target devices:
- Phone: 400-500 dp width (COMPACT)
- Tablet Portrait: 600-800 dp (MEDIUM)
- Tablet Landscape: 800+ dp (EXPANDED)
- Desktop: 1200+ dp (EXPANDED)

---

## Migration Guide

### From old Desktop/Tablet approach

**Before:**
```kotlin
when (platform) {
    ClientPlatform.Desktop -> DesktopShell(...)
    ClientPlatform.Tablet -> TabletShell(...)
}
```

**After:**
```kotlin
// Automatically detects screen size and adapts!
ResponsiveShell(session, localRepository, onLogout)
```

---

## Performance Considerations

1. **Recomposition**: `currentWindowSizeClass()` only recomposes when window size actually changes
2. **Memory**: Responsive utilities are zero-overhead
3. **Cache**: Use `remember` for expensive calculations based on window size

```kotlin
val columns = remember { responsiveGridColumns() }
```

---

## Future Enhancements

- [ ] Foldable device support
- [ ] Multi-window support (desktop)
- [ ] Device orientation callbacks
- [ ] Dynamic type/accessibility scaling
- [ ] Safe area insets (notches/cutouts)

---

## Resources

- Material Design 3: https://m3.material.io/
- Responsive Design: https://developer.android.com/jetpack/compose/layouts/adaptive
- Window size class docs: https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes


