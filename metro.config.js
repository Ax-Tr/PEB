const { getDefaultConfig } = require('expo/metro-config');

const config = getDefaultConfig(__dirname);

// Exclude the backend directory from Metro's watch and resolve paths
const exclusionPattern = /backend\/.*/;

if (!config.resolver.blockList) {
  config.resolver.blockList = [];
}
if (Array.isArray(config.resolver.blockList)) {
  config.resolver.blockList.push(exclusionPattern);
} else {
  config.resolver.blockList = [exclusionPattern];
}

module.exports = config;
