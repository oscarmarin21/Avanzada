# Frontend design system

The frontend uses **Tailwind CSS** and **[Flowbite](https://flowbite.com/docs/getting-started/introduction/)** for styling. Flowbite provides a library of UI components (buttons, forms, cards, tables, navbars, modals, etc.) built with Tailwind utility classes. There are no component or global CSS files beyond `src/styles.css`; all layout and appearance use Tailwind + Flowbite classes and the theme in `tailwind.config.js`.

Interactive Flowbite components (dropdowns, modals, etc.) are initialised via `initFlowbite()` in `AppComponent` (on load and after each route change).

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

## Reusable patterns (Flowbite + theme)

Use these patterns so the UI stays coherent. Prefer [Flowbite components](https://flowbite.com/docs/components/buttons/) where applicable; override with theme colors (`primary`, `surface`) when needed.

- **Nav**: Institutional bar `flex gap-4 px-6 py-4 bg-primary text-white border-b-2 border-uniquindio-gold shadow`; links `text-white no-underline hover:underline`; user `ml-auto text-gray-300 text-sm`; logout as link-style button.
- **Primary button**: `text-white bg-primary hover:bg-primary-dark focus:ring-4 focus:ring-primary-300 font-medium rounded-lg text-sm px-5 py-2.5`.
- **Secondary button**: `text-gray-900 bg-white border border-gray-300 hover:bg-gray-100 focus:ring-4 focus:ring-gray-200 font-medium rounded-lg text-sm px-5 py-2.5`.
- **Card**: `p-6 bg-white border border-gray-200 rounded-lg shadow` (or `bg-gray-50` for form sections).
- **Form**: Label `block mb-2 text-sm font-medium text-gray-900`; input `bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-primary-500 focus:border-primary-500 block w-full p-2.5`; select same as input.
- **Table**: Wrapper `overflow-hidden rounded-lg border border-gray-200 bg-white shadow`; table `w-full text-sm text-left text-gray-500`; `thead` `text-xs text-gray-700 uppercase bg-gray-50`; `th`/`td` `px-6 py-4`; row `border-b hover:bg-gray-50`.
- **Error / alert**: `p-4 mb-4 text-sm text-red-800 rounded-lg bg-red-50` (Flowbite alert style).
- **Link**: `font-medium text-primary hover:underline`.

## Adding new components

Do not add new `styleUrl` or `.css` files. Use only Tailwind classes in the template (and optionally `@apply` inside a single shared Tailwind layer if we introduce one). Follow the patterns above for nav, buttons, cards, forms, and tables so the app stays consistent.
