# Figma Annotation Standards: Accessibility, Safety, and Localization

This document defines baseline UX rules to include in Figma annotations for all core clinical workflows.

## 1) Accessibility Rules (Required in Figma Annotations)

### 1.1 Minimum Contrast
- **Body text and essential icons:** WCAG AA contrast ratio of **4.5:1** minimum against background.
- **Large text (>= 18 pt regular or >= 14 pt bold):** **3:1** minimum.
- **UI boundaries (inputs, cards, separators) that convey state:** **3:1** minimum where needed for perception.
- Annotate contrast in component specs with token names (for example: `text/primary on surface/default`).

### 1.2 Keyboard Focus States
- Every interactive control must have a visible focus treatment in annotations:
  - **Focus ring thickness:** 2 px minimum.
  - **Focus offset:** 2 px outside component edge where possible.
  - **Focus contrast:** ring must remain visible on all supported backgrounds.
- Define focus order in complex forms (triage, diagnosis, discharge) and mark any intentional focus traps (for modals only).

### 1.3 Touch Targets
- Minimum target size for all tappable controls: **44 x 44 px**.
- Maintain minimum spacing of **8 px** between adjacent interactive targets.
- If visual icon appears smaller, keep hit area expanded and annotate invisible hitbox.

### 1.4 Readable Typography
- Base body size: **16 px** minimum for mobile, 14–16 px for desktop dense tables with zoom support.
- Line-height target: **1.4–1.6** for paragraph text and instructions.
- Avoid all-caps for long labels or clinical instructions.
- Keep paragraph width around **45–90 characters** for readability.

---

## 2) Color-Independent Status Cues

Never rely on color alone for status communication.

### Required Pattern
Each status must include:
1. **Color token** (visual scan),
2. **Icon** (shape cue),
3. **Text label** (explicit meaning).

### Example Status Mapping
- **Critical:** red + warning/alert icon + label `Critical`
- **Stable:** green + check/heart-beat icon + label `Stable`
- **Pending:** amber + clock icon + label `Pending`
- **Unknown/Not recorded:** neutral gray + question icon + label `Unknown`

### Annotation Rule
- In tables, chips, and timeline rows, annotate status component as `Status = [Color + Icon + Label]`.
- Include screen-reader label text in specs (for implementation parity).

---

## 3) Confirmation Patterns for Risky Actions

Use one standard confirmation system for high-risk operations:
- **Admit/Discharge**
- **Delete record/entry**
- **Finalize diagnosis**

### Confirmation Dialog Anatomy
- **Title:** action-first and explicit (for example, `Finalize diagnosis?`).
- **Impact summary:** one sentence describing irreversible or downstream effects.
- **Entity context:** patient name + MRN/ID to prevent wrong-patient actions.
- **Primary CTA:** verb-specific (`Finalize diagnosis`, `Discharge patient`, `Delete note`).
- **Secondary CTA:** `Cancel`.
- **Optional safety step for destructive actions:** typed confirmation for delete/finalize in admin contexts.

### Behavior Rules
- Default keyboard focus starts on **Cancel** for destructive actions.
- Require explicit confirmation (no single-click destructive actions).
- Show post-action toast/banner with undo only where technically safe and auditable.

---

## 4) Interruption-Safe UX

Clinical workflows must tolerate connectivity issues, tab switches, and accidental closures.

### 4.1 Autosave Drafts
- Autosave draft fields every **5–10 seconds** after changes and on blur.
- Show last saved timestamp (`Saved at 14:32`).
- Provide clear states: `Saving…`, `Saved`, `Save failed`.

### 4.2 Session Recovery
- On re-open or crash recovery, prompt: `Restore unsaved draft?` with `Restore` / `Discard`.
- Keep draft snapshots per patient + workflow step (notes, diagnosis, orders).
- Preserve cursor/section position when practical.

### 4.3 Offline Indicator
- Persistent offline banner/chip when connection is unavailable.
- Queue write operations locally and surface sync status (`3 changes pending sync`).
- Resolve conflicts with explicit compare-and-choose UI when remote edits exist.

---

## 5) Localization-Ready Text and Date/Time Patterns

### 5.1 Text Containers
- Do not hardcode fixed-width text boxes for labels/buttons.
- In Figma, use auto-layout with expansion room for longer translations (target **30–50%** growth).
- Avoid embedding text into images/icons.
- Externalize all user-facing strings with stable keys.

### 5.2 Language and Grammar
- Prefer plain, translatable phrasing (avoid idioms and culture-specific shorthand).
- Use sentence case labels and consistent terminology (`Discharge summary`, not mixed variants).

### 5.3 Date/Time Formatting Patterns
- Store canonical values in ISO-like formats at data layer.
- Display by locale and user preference in UI:
  - Date: locale-native order (for example, `MM/DD/YYYY` vs `DD/MM/YYYY`).
  - Time: 12h/24h user preference with timezone where relevant.
- For audit trails and clinical events, include absolute date + time + timezone where ambiguity matters.

### Annotation Rule
- Mark all date/time components with a format token placeholder, for example:
  - `{{date_short}}`, `{{date_long}}`, `{{time_short}}`, `{{datetime_with_tz}}`.

---

## Figma Annotation Checklist (Quick Use)
- [ ] Contrast ratios annotated and pass minimums.
- [ ] Focus states defined for all interactive elements.
- [ ] Touch targets meet 44 x 44 px minimum.
- [ ] Typography sizes and line-height match readability rules.
- [ ] Statuses include color + icon + label.
- [ ] Risky actions use standardized confirmation pattern.
- [ ] Autosave, recovery, and offline states are represented.
- [ ] Text containers are localization-ready.
- [ ] Date/time fields are tokenized and locale-aware.
