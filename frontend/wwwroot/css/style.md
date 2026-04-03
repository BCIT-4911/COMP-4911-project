# Design System Strategy: The Organic Editorial

## 1. Overview & Creative North Star
The Creative North Star for this design system is **"Botanical Precision."** We are moving away from the rigid, sterile grids of traditional SaaS platforms toward a high-end editorial experience that feels curated, breathing, and intentional. 

This system rejects the "standard" UI look. We achieve a premium feel through **intentional asymmetry**, where white space is treated as a foundational element rather than a gap to be filled. By utilizing the Manrope typeface and a monochromatic-adjacent green palette, we create a sense of "Organic Brutalism"—softened edges and natural tones met with bold, authoritative typography scales.

## 2. Colors & Tonal Architecture
The palette is a sophisticated study in Sage, Forest, and Moss. It is designed to be experienced as a cohesive landscape rather than a set of disparate buttons.

### Surface Hierarchy & Nesting
To achieve a high-end feel, we move away from flat layouts.
- **The "No-Line" Rule:** We do not use 1px solid borders to separate sections. Boundaries are defined strictly through background shifts. For example, a `surface_container` card sits on a `surface` background to define its shape.
- **Tonal Layering:** Use the `surface_container` tiers (Lowest to Highest) to create physical depth. Treat the UI as stacked sheets of fine, heavy-weight paper. 
    - `surface_container_lowest`: Use for the primary content area to provide a crisp focus.
    - `surface_container_high`: Use for secondary navigation or side panels to create a subtle "lift."

### The "Glass & Signature" Rule
- **Glassmorphism:** For floating elements (like navigation bars or hovering cards), use `surface` colors at 80% opacity with a `24px` backdrop blur. This allows the botanical tones of the background to bleed through, softening the interface.
- **Signature Textures:** Apply subtle linear gradients to primary CTAs. Transition from `primary` (#4b644f) to `primary_dim` (#405843) at a 135-degree angle to add a "satin" finish that flat colors cannot replicate.

## 3. Typography: The Editorial Voice
We use **Manrope** across all scales. Its geometric but warm structure provides the "Precision" in our North Star.

- **Display & Headline:** Use `display-lg` (3.5rem) and `headline-lg` (2rem) for high-impact editorial moments. These should have generous leading (1.1–1.2) to feel authoritative and spacious.
- **Body & Labels:** `body-lg` (1rem) is our workhorse. For metadata and utility, `label-md` (0.75rem) provides a clean, technical counterpoint to the large headlines.
- **The Hierarchy Strategy:** Use significant scale jumps between headlines and body text to create a clear "narrative" on the page.

## 4. Elevation & Depth
Depth is a tactile experience in this design system. We avoid the "floating box" look of 2010-era design.

- **The Layering Principle:** Place a `surface_container_lowest` element on a `surface_container_low` background to create a "recessed" or "elevated" effect through color alone.
- **Ambient Shadows:** Shadows are rare. When used for high-level modals, they must be extra-diffused: `box-shadow: 0 20px 40px rgba(38, 55, 36, 0.06)`. The shadow color is a tint of `on_surface`, not pure black.
- **The "Ghost Border":** If accessibility requires a border, use `outline_variant` at 15% opacity. It should be felt, not seen.

## 5. Components

### Buttons & Interaction
- **Primary:** High-pill shape (`9999px`). Background: `primary`. Text: `on_primary`. Use the signature "satin" gradient for premium states.
- **Secondary:** Surface-only. Background: `secondary_container`. No border.
- **Tertiary/Ghost:** No background. Use `primary` for text. Interaction is shown through a subtle shift to `surface_container_low` on hover.

### Form Elements
- **Input Fields:** Use `surface_container` with a `md` (0.75rem) corner radius. The label should be `label-md` sitting just above the field. Avoid "boxed" inputs; prefer a "filled" look to maintain the tonal weight of the page.
- **Search Bars:** Always `full` rounded. Use `surface_container` to make the search feel like it is "carved" into the interface.

### Cards & Navigation
- **Cards:** Forbid dividers. Use `xl` (1.5rem) corner radius for main cards. Separation of content is achieved through vertical padding (using the Spacing Scale) or subtle background shifts.
- **Floating Nav:** Use the Glassmorphism rule with a `lg` (1rem) or `full` corner radius to create a "pebble" effect.

## 6. Do’s and Don'ts

### Do:
- **Embrace Asymmetry:** Offset text blocks from center-aligned images to create an editorial feel.
- **Use Tonal Contrast:** Use `on_surface_variant` for secondary text to create a soft, readable hierarchy against the Sage background.
- **Prioritize Breathing Room:** When in doubt, increase the margin. This system thrives on "expensive" white space.

### Don’t:
- **No Hard Borders:** Never use 100% opaque, 1px borders. It breaks the organic flow.
- **No Generic Shadows:** Avoid the standard "Drop Shadow" presets in design tools. Use the Ambient Shadow rules.
- **No Pure Grays:** Never use `#888888` or pure black/white. Every "neutral" in this system must be tinted with the Moss/Sage palette to maintain the "Botanical" identity.