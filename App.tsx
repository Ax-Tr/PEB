import React from "react";
import { StatusBar } from "expo-status-bar";
import { GestureHandlerRootView } from "react-native-gesture-handler";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { NavigationContainer, DarkTheme } from "@react-navigation/native";
import { QueryClientProvider } from "@tanstack/react-query";
import { AuthProvider } from "./src/features/auth/AuthContext";
import { I18nProvider } from "./src/features/i18n/I18nContext";
import { OfflineQueueProvider } from "./src/features/offline/OfflineQueueContext";
import { RootNavigator } from "./src/navigation/RootNavigator";
import { queryClient } from "./src/shared/queryClient";
import { theme } from "./src/theme/theme";

const navTheme = {
  ...DarkTheme,
  colors: {
    ...DarkTheme.colors,
    background: theme.color.bg,
    card: theme.color.surface,
    text: theme.color.text,
    primary: theme.color.primary,
    border: theme.color.border,
  },
};

/** PEB app root — one codebase for Android, iOS, and web (react-native-web). */
export default function App(): React.ReactElement {
  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <QueryClientProvider client={queryClient}>
          <I18nProvider>
            <AuthProvider>
              <OfflineQueueProvider>
                <NavigationContainer theme={navTheme}>
                  <RootNavigator />
                  <StatusBar style="light" />
                </NavigationContainer>
              </OfflineQueueProvider>
            </AuthProvider>
          </I18nProvider>
        </QueryClientProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
