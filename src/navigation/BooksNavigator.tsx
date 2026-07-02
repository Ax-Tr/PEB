import React from "react";
import { createStackNavigator } from "@react-navigation/stack";
import { BooksMenuScreen } from "../features/books/BooksMenuScreen";
import { InvoiceCreateScreen } from "../features/invoices/InvoiceCreateScreen";
import { ReconciliationScreen } from "../features/reconciliation/ReconciliationScreen";
import { ComplianceListScreen } from "../features/compliance/ComplianceListScreen";
import { ComplianceDetailScreen } from "../features/compliance/ComplianceDetailScreen";
import { theme } from "../theme/theme";
import type { BooksStackParamList } from "./types";

const Stack = createStackNavigator<BooksStackParamList>();

export function BooksNavigator(): React.ReactElement {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: theme.color.surface },
        headerTitleStyle: { color: theme.color.text },
        headerTintColor: theme.color.primary,
        cardStyle: { backgroundColor: theme.color.bg },
      }}
    >
      <Stack.Screen name="BooksMenu" component={BooksMenuScreen} options={{ title: "Books" }} />
      <Stack.Screen name="Invoices" component={InvoiceCreateScreen} options={{ title: "New invoice" }} />
      <Stack.Screen name="Reconciliation" component={ReconciliationScreen} options={{ title: "Reconciliation" }} />
      <Stack.Screen name="Compliance" component={ComplianceListScreen} options={{ title: "Compliance" }} />
      <Stack.Screen
        name="ComplianceDetail"
        component={ComplianceDetailScreen}
        options={({ route }) => ({ title: route.params.title })}
      />
    </Stack.Navigator>
  );
}
