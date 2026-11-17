import type { Config } from "tailwindcss";
export default {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
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
      boxShadow: { panel: "0 4px 24px rgba(20,61,66,.04)" },
    },
  },
  plugins: [],
} satisfies Config;
