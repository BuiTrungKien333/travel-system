import type { Config } from "tailwindcss";
export default {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    borderRadius: {
      none: "0px",
      sm: "0.125rem",
      DEFAULT: "0.25rem",
      md: "0.25rem",
      lg: "0.375rem",
      xl: "0.5rem",
      "2xl": "0.625rem",
      "3xl": "0.75rem",
      full: "9999px",
    },
    extend: {
      colors: {
        ink: "#143D42",
        muted: "#647C80",
        teal: {
          50: "#EEF9F8",
          100: "#D7F1EE",
          500: "#21B5C3",
          600: "#117B83",
          700: "#105E64",
        },
        canvas: "#F5F8F8",
        line: "#DFE8E7",
      },
      boxShadow: { panel: "none" },
    },
  },
  plugins: [],
} satisfies Config;
