/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        /* Uniquindío-style institutional palette: green primary, warm neutrals */
        primary: {
          DEFAULT: "#0d5c2e",
          light: "#157340",
          dark: "#0a4722",
        },
        uniquindio: {
          green: "#0d5c2e",
          "green-light": "#157340",
          "green-dark": "#0a4722",
          gold: "#c9a227",
          cream: "#f5f0e6",
        },
        surface: {
          DEFAULT: "#f5f0e6",
          card: "#ffffff",
        },
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
      },
    },
  },
  plugins: [],
};
