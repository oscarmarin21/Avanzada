# Frontend design system

The frontend uses **Tailwind CSS only** for styling. There are no component or global CSS files; all layout and appearance are achieved with Tailwind utility classes and the theme defined in `tailwind.config.js`.

The palette and style are inspired by the [Universidad del Quindío](https://www.uniquindio.edu.co/) institutional site: green as primary, warm cream background, and an optional gold accent for a clean, accessible academic look.

## Theme (tailwind.config.js)

- **Colors**
  - `primary`: institutional green `#0d5c2e` (default), `primary-light` `#157340`, `primary-dark` `#0a4722` for nav, primary buttons, links.
  - `uniquindio`: `green`, `green-light`, `green-dark`, `gold` `#c9a227`, `cream` `#f5f0e6` for accents (e.g. nav border).
  - `surface`: background `#f5f0e6` (cream); cards use `bg-white` or `surface-card`.
  - Grays: Tailwind default `gray-*` for text, borders, backgrounds.
  - Semantic: `text-red-600` for errors.
- **Typography**: Default sans (Inter when available). Headings: `text-2xl font-semibold text-gray-900` (h1), `text-lg font-semibold` (h2). Body/labels: `text-sm font-medium text-gray-700` for labels, `text-gray-900` or `text-gray-600` for content.
- **Spacing**: Tailwind scale (`p-4`, `gap-4`, `mb-4`, etc.).

## Reusable patterns

Use these patterns consistently so the UI stays coherent:

- **Nav**: `flex gap-4 px-6 py-4 bg-primary text-white border-b-2 border-uniquindio-gold shadow-md`; links `text-white no-underline hover:underline`; user info `ml-auto text-gray-300 text-sm`; logout button same as link style.
- **Primary button**: `px-4 py-2 rounded bg-primary text-white font-medium hover:bg-primary-dark`.
- **Secondary button**: `px-4 py-2 rounded border border-gray-300 bg-white font-medium hover:bg-gray-50`.
- **Card**: `p-4 rounded-lg border border-gray-200 bg-white shadow-sm` (or `bg-gray-50` for form cards).
- **Form label**: `flex flex-col gap-1` with inner `text-sm font-medium text-gray-700`; inputs `w-full rounded border border-gray-300 px-3 py-2 text-sm`.
- **Table**: Container `rounded-lg border border-gray-200 bg-white shadow-sm overflow-hidden`; table `w-full border-collapse`; `thead` row `bg-gray-50`; `th`/`td` `px-3 py-2 text-left text-sm`; `th` `font-medium text-gray-700 border-b border-gray-200`; `td` `text-gray-900` or `text-gray-600`; row hover `hover:bg-gray-50`.
- **Error message**: `text-red-600` (and optional `mb-2`).
- **Link**: `text-primary font-medium hover:underline`.

## Adding new components

Do not add new `styleUrl` or `.css` files. Use only Tailwind classes in the template (and optionally `@apply` inside a single shared Tailwind layer if we introduce one). Follow the patterns above for nav, buttons, cards, forms, and tables so the app stays consistent.
