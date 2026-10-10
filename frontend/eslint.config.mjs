import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

// Raw HTML controls each have a shared primitive in src/components/ui.
// Feature and page code must use the primitives; add a primitive rather than disabling this rule.
const rawElementPrimitives = {
  button: "Button, IconButton, LinkButton, SegmentedControl or FilterChips",
  input: "Input, SearchInput or Checkbox (inside FormField)",
  select: "Select (inside FormField)",
  textarea: "Textarea (inside FormField)",
  table: "DataTable or the Table primitives",
};

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  {
    files: ["src/features/**/*.tsx", "src/app/**/*.tsx"],
    rules: {
      "no-restricted-syntax": [
        "error",
        ...Object.entries(rawElementPrimitives).map(([element, primitive]) => ({
          selector: `JSXOpeningElement[name.name='${element}']`,
          message: `Use ${primitive} from '@/components/ui' instead of a raw <${element}>.`,
        })),
      ],
    },
  },
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
  ]),
]);

export default eslintConfig;
