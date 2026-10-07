import type { CodegenConfig } from '@graphql-codegen/cli';

// Types come from the server's own schema, so the frontend cannot drift from it unnoticed.
const config: CodegenConfig = {
  schema: '../src/main/resources/graphql/schema.graphqls',
  documents: ['src/**/*.{ts,tsx}', '!src/graphql/generated/**'],
  ignoreNoDocuments: true,
  generates: {
    './src/graphql/generated/': {
      preset: 'client',
      config: {
        // Plain strings: no GraphQL runtime ships to the browser
        documentMode: 'string',
        enumsAsTypes: true,
        useTypeImports: true,
      },
      presetConfig: {
        fragmentMasking: false,
      },
    },
  },
};

export default config;
