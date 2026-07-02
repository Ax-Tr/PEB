/**
 * Jest config for the frontend's pure TypeScript logic (money, http client, auth flow). React Native
 * component tests need the RN preset + a device runtime; the CI unit gate here covers the framework-
 * free data layer, which is where the financial-correctness risk lives.
 */
module.exports = {
  preset: "ts-jest",
  testEnvironment: "node",
  roots: ["<rootDir>/src"],
  testMatch: ["**/*.test.ts"],
};
