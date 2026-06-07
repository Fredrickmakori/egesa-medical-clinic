# Egesa Medical Clinic — Foundations & Component Spec

This document defines the Figma setup and token/component inventory for building the requested `Foundations` page and reusable UI system.

## 1) Figma Page Structure

Create a page named **`Foundations`** with these top-level frames (auto-layout vertical, 64 spacing):

1. `Color Tokens`
2. `Typography Tokens`
3. `Spacing Tokens`
4. `Elevation Tokens`
5. `Radius Tokens`
6. `Primitives`
7. `Components`
8. `States & Sizes Matrix`

---

## 2) Color Tokens

Use semantic-first naming, then map to base palette.

### Base Palette

- `base/blue/600` `#2563EB`
- `base/blue/700` `#1D4ED8`
- `base/green/600` `#16A34A`
- `base/amber/600` `#D97706`
- `base/purple/600` `#7C3AED`
- `base/cyan/600` `#0891B2`
- `base/teal/600` `#0F766E`
- `base/red/600` `#DC2626`
- `base/red/700` `#B91C1C`
- `base/gray/50` `#F9FAFB`
- `base/gray/100` `#F3F4F6`
- `base/gray/200` `#E5E7EB`
- `base/gray/300` `#D1D5DB`
- `base/gray/500` `#6B7280`
- `base/gray/700` `#374151`
- `base/gray/900` `#111827`
- `base/white` `#FFFFFF`

### Core Semantic Tokens

- `bg/default` → `base/white`
- `bg/subtle` → `base/gray/50`
- `surface/default` → `base/white`
- `surface/muted` → `base/gray/100`
- `border/default` → `base/gray/200`
- `border/strong` → `base/gray/300`
- `text/primary` → `base/gray/900`
- `text/secondary` → `base/gray/700`
- `text/muted` → `base/gray/500`
- `action/primary` → `base/blue/600`
- `action/primary-hover` → `base/blue/700`
- `action/destructive` → `base/red/600`
- `action/destructive-hover` → `base/red/700`

### Workflow Status Colors (Required)

- `status/waiting` → `#D97706` (amber)
- `status/in-consultation` → `#2563EB` (blue)
- `status/diagnosis` → `#7C3AED` (purple)
- `status/admitted` → `#0891B2` (cyan)
- `status/discharged` → `#16A34A` (green)
- `status/critical` → `#DC2626` (red)

Also create companions for each status:

- `status/<name>/bg` (10–15% tint)
- `status/<name>/text` (main color)
- `status/<name>/border` (30–40% tint)

---

## 3) Typography Tokens (Desktop + Mobile)

Font family: **Inter** (fallback: system sans).

### Desktop

- `desktop/title` — 32/40, 700
- `desktop/section-heading` — 24/32, 600
- `desktop/body` — 16/24, 400
- `desktop/caption` — 12/16, 400
- `desktop/data-value` — 28/32, 700, tabular nums

### Mobile

- `mobile/title` — 24/32, 700
- `mobile/section-heading` — 20/28, 600
- `mobile/body` — 14/20, 400
- `mobile/caption` — 12/16, 400
- `mobile/data-value` — 22/28, 700, tabular nums

---

## 4) Spacing, Elevation, Radius

### Spacing Tokens

- `space/0` = 0
- `space/1` = 4
- `space/2` = 8
- `space/3` = 12
- `space/4` = 16
- `space/5` = 20
- `space/6` = 24
- `space/8` = 32
- `space/10` = 40
- `space/12` = 48

### Elevation Tokens

- `elevation/0` = none
- `elevation/1` = `0 1 2 0 rgba(17,24,39,0.06)`
- `elevation/2` = `0 4 10 -2 rgba(17,24,39,0.12)`
- `elevation/3` = `0 10 24 -6 rgba(17,24,39,0.18)`

### Radius Tokens

- `radius/none` = 0
- `radius/sm` = 4
- `radius/md` = 8
- `radius/lg` = 12
- `radius/xl` = 16
- `radius/full` = 999

---

## 5) Auto-layout Primitives

Create these primitives as components in `Primitives`:

1. `Container/Card`
   - Padding: `space/4`
   - Gap: `space/3`
   - Radius: `radius/lg`
   - Fill: `surface/default`
   - Stroke: `border/default`
   - Shadow: `elevation/1`

2. `Section Header`
   - Horizontal auto-layout, space-between
   - Left: title + optional subtitle
   - Right: action slot

3. `Divider`
   - 1px height (or width vertical)
   - Color: `border/default`

4. `Icon + Label`
   - Horizontal layout, gap `space/2`
   - 16/20 icon variants

5. `Badge`
   - Horizontal layout, padding `4 8`
   - Radius `radius/full`
   - Semantic color variants including all `status/*`

---

## 6) Reusable Interactive Components

Build component sets with variants and interactive states:

### Button
- Variants: `primary`, `secondary`, `destructive`, `text`
- Sizes: `sm`, `md`, `lg`
- States: `default`, `hover`, `focused`, `disabled`, `error`
- Optional leading/trailing icon boolean props

### Input
Types as variants:
- `text`, `search`, `date`, `dropdown`, `textarea`

Shared variants:
- `state`: `default`, `hover`, `focused`, `disabled`, `error`
- `size`: `sm`, `md`, `lg`
- `has-label`, `has-helper`, `has-prefix-icon`, `has-suffix-icon`

### Tabs
- Variants: `underline`, `pill`
- States per tab item: `default`, `hover`, `focused`, `disabled`, `active`
- Size: `sm`, `md`, `lg`

### Table
- Subcomponents: header row, body row, cell, sortable header, selection cell
- Row states: `default`, `hover`, `selected`, `critical`
- Density/size: `sm`, `md`, `lg`

### Modal/Drawer
- `Modal` sizes: `sm`, `md`, `lg`
- `Drawer` sides: `right`, `bottom`
- States: `default`, `focused`, `error` (for contained forms)

### Toast/Snackbar
- Types: `info`, `success`, `warning`, `error`, `critical`
- Optional action button + dismiss

### Empty / Error / Loading
- `Empty State`: icon, title, body, primary action
- `Error State`: icon, title, body, retry action
- `Loading Skeleton`: text-line, avatar-line, card-skeleton, table-row-skeleton

---

## 7) Variant Matrix Requirement

For each interactive component set, include at least:

- `state`: `default | hover | focused | disabled | error`
- `size`: `sm | md | lg`

Suggested variant properties in Figma:

- `Type` (e.g., primary/secondary)
- `State`
- `Size`
- `Icon` (none/leading/trailing/both)
- `Label` (true/false where relevant)

---

## 8) Naming Conventions

- Tokens: `category/subcategory/name`
- Components: `Component/VariantGroup`
- Instances in layouts: semantic names (e.g., `Patient Status Badge` not `Badge 14`)

---

## 9) Publish Checklist

- All colors are variables and semantically mapped.
- Desktop and mobile text styles are published.
- Primitives are locked and documented as building blocks.
- Components have complete size/state variants.
- Interactive prototype links for hover/focus/disabled/error examples.
- Library published as `Egesa Design System v1`.
