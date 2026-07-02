/**
 * Detox configuration for on-device e2e. Detox drives a real build on an emulator/simulator, so it
 * runs in CI (with device tooling) or locally — not in this repo sandbox. `npm run e2e:build` then
 * `npm run e2e:test`.
 */
module.exports = {
  testRunner: {
    args: { $0: "jest", config: "e2e/jest.config.js" },
    jest: { setupTimeout: 120000 },
  },
  apps: {
    "android.debug": {
      type: "android.apk",
      binaryPath: "android/app/build/outputs/apk/debug/app-debug.apk",
      build:
        "cd android && ./gradlew assembleDebug assembleAndroidTest -DtestBuildType=debug && cd ..",
    },
    "ios.debug": {
      type: "ios.app",
      binaryPath: "ios/build/Build/Products/Debug-iphonesimulator/PEB.app",
      build:
        "xcodebuild -workspace ios/PEB.xcworkspace -scheme PEB -configuration Debug -sdk iphonesimulator -derivedDataPath ios/build",
    },
  },
  devices: {
    emulator: { type: "android.emulator", device: { avdName: "Pixel_6_API_34" } },
    simulator: { type: "ios.simulator", device: { type: "iPhone 15" } },
  },
  configurations: {
    "android.emu.debug": { device: "emulator", app: "android.debug" },
    "ios.sim.debug": { device: "simulator", app: "ios.debug" },
  },
};
