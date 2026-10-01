# Public website design

The `/`, `/about`, `/frames` and `/contact` routes use the existing Thymeleaf renderer, shared public navigation and business profile. Shared styling is scoped to `.public-page`, with cream (`#fff9ed`) backgrounds, logo blue (`#3d8fcb`, sampled from the existing logo) panels and dark blue (`#174c70`) text. No framework runtime or external fonts are required. A small local script enhances the shared mobile navigation; links remain available with JavaScript disabled. Frame links use the existing `FRAME` catalogue filter; examination and visit links lead to the contact page.

## Hero asset

`src/main/resources/static/images/eyewear-hero.webp` is an illustrative hero image, not a catalogue item or a statement of stock availability. Generated with the built-in imagegen tool, then encoded as WebP at quality 88 (about 46 KB). The existing Nethcare logo is retained.

Generation prompt:

> Create a photorealistic luxury eyewear product photograph for a minimal optician website hero, landscape 1536x1024. A single pair of black dark midnight navy acetate prescription eyeglasses, elegant rounded rectangular panto shape, clear transparent lenses, subtle silver hinge pin detailing, straight-on front view slightly above eye level with temples receding behind. Glasses centered occupying 82 percent of width, complete glasses visible, plenty of margin around subject. Clean warm ivory seamless studio background color #f4f2ed, very soft realistic grounded shadow directly below glasses. Beautiful meticulous premium product photography, crisp polished acetate and realistic reflections, soft diffuse studio lighting. No text, no lettering, no logo, no other objects, no people. This is a standalone website asset, not a screenshot or page design.

## Verification

Checked in Chromium against Spring Boot using an isolated H2 database. Verified desktop, tablet and mobile layouts at widths 1440, 768, 390 and 320, image loading, horizontal overflow, the keyboard skip link, filtered frame navigation, and HTTP success for About, Contact and Login. Visuals reviewed at desktop and mobile sizes. This check does not validate production stock data or appointment booking; the landing page links to existing services.

The About page covers the shop and six services. The catalogue preserves category selection during search and retains the search query when changing categories, with separate empty collection and no-match states. Contact renders real phone/email links when supplied and helpful pending states otherwise, plus keyboard-accessible native disclosure FAQs.

Follow-up validation: all four pages passed Chromium checks at widths 1440, 768, 390 and 320 with no overflow, missing images or browser errors. Category/search/reset and FAQ keyboard interactions passed. `mvn -q -Dtest=PublicPagesTest test`: 3 tests passed, covering public rendering, populated catalogue output, preserved filters, empty states and missing contact details.

## Transparent eyewear and responsive navigation

Home and Frames now use `src/main/resources/static/images/eyewear-cutout.webp` (about 104 KB). The built-in imagegen tool removed the original background with actual alpha transparency, including inside the lenses. Exported to WebP with alpha preserved. The old source asset is retained. CSS no longer uses blending or masking to hide a rectangular background.

Final edit prompt:

> Use case: background-extraction. Edit the supplied eyewear image: remove the entire ivory studio background and floor completely to actual transparent alpha, including all background visible through both clear lenses and between the temples. Preserve this exact black acetate eyeglass frame, its shape, angle, silver hinge details, temples and sharp reflective edges. The lenses must be transparent so a website's blue or cream background shows through naturally, retaining only subtle transparent optical reflections. No rectangular backdrop, no opaque lens fills, no checkerboard pixels, no floor or cast shadow. Keep the original landscape 1536x1024 canvas and placement, with the full glasses centered and uncropped. Output a clean transparent product cutout.

The floating header becomes a collapsible menu at 800px. Its button exposes expanded state, supports Escape with restored focus, closes on outside clicks, and resets when crossing the desktop breakpoint. Mobile layouts use larger body text, stacked hero buttons and touch targets of at least 44px for the navigation, filters and primary actions.

Final checks: all four pages passed overflow, image loading and semantic heading/navigation checks at 320, 375, 390, 430, 768, 800, 820, 1024 and 1440 pixels. After hiding decorative menu arrows from assistive technology, desktop/mobile checks and menu interaction checks passed, including Escape/focus restoration, outside click, breakpoint reset, route navigation and the JavaScript-disabled fallback. Catalogue search/filter/reset and FAQ keyboard checks also passed. The three Spring rendering tests passed after adding the new header and cutout.

## Editorial homepage revision

The homepage now uses `home-editorial.css`: oversized serif typography, a larger angled transparent eyewear hero, cream-led sections and a single blue visit panel. The hero has a short entrance animation disabled by `prefers-reduced-motion`. The repeated service cards have been replaced with an image and linked service rows.

`PublicController.index` supplies up to three listed frames from the existing gallery service. Home and Frames share the same product-card fragment, including real prices, availability and missing-photo states. An empty catalogue shows an in-store invitation. No sample products or fabricated prices are inserted into the database.

`src/main/resources/static/images/frame-adjustment.webp` is an illustrative editorial image created with the built-in imagegen tool, not a photograph of Neth Opticians. It is labelled as illustrative in the page caption. Final prompt:

> Editorial eyewear craftsmanship photograph, portrait 1024x1536. Tight close-up of an optician's hands carefully adjusting the small silver hinge on dark tortoiseshell acetate eyeglasses using a tiny precision screwdriver, on a warm cream work surface. Natural medium brown skin, realistic hand anatomy, accurate optical tools. Hands and eyewear in lower middle of frame, soft cream linen shirt partially visible, face entirely out of frame. Soft directional window light, tactile materials, subtle film grain, warm desaturated palette, high-end independent eyewear magazine photography. Clean composition with intimate human detail, shallow depth of field, no text, no logos, no store signage. This is an illustrative optical-care editorial asset, no specific shop or person.

Validation: four Spring rendering tests pass, including the three-product limit and the empty homepage catalogue. Browser checks passed at 1440, 1024, 768, 390 and 320px after containing the rotated image on mobile. Shared navigation, filters, FAQ keyboard behavior and JavaScript-disabled navigation passed. The isolated H2 preview runs on port 8086 and has no catalogue entries; real product imagery still needs to be supplied through the existing catalogue.

## Glass buttons

`glass-buttons.css` replaces the clay treatment on public actions, Login, the mobile toggle, catalogue filters/search and the active navigation pill. Translucent cream and logo-blue gradients, backdrop blur, fine borders and a single inset highlight create the glass effect. Hover and pressed feedback stay subtle. Keyboard focus, reduced motion, forced-color borders and an opaque fallback for browsers without backdrop filtering are retained.
