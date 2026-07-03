import React from "react";
import { createStackNavigator } from "@react-navigation/stack";
import { BooksMenuScreen } from "../features/books/BooksMenuScreen";
import { InvoiceListScreen } from "../features/invoices/InvoiceListScreen";
import { InvoiceCreateScreen } from "../features/invoices/InvoiceCreateScreen";
import { PayoutListScreen } from "../features/pay/PayoutListScreen";
import { CommitmentCreateScreen } from "../features/commitments/CommitmentCreateScreen";
import { CommitmentDetailScreen } from "../features/commitments/CommitmentDetailScreen";
import { CommitmentListScreen } from "../features/commitments/CommitmentListScreen";
import { InstallmentCreateScreen } from "../features/installments/InstallmentCreateScreen";
import { InstallmentDetailScreen } from "../features/installments/InstallmentDetailScreen";
import { InstallmentListScreen } from "../features/installments/InstallmentListScreen";
import { ReminderCreateScreen } from "../features/reminders/ReminderCreateScreen";
import { ReminderListScreen } from "../features/reminders/ReminderListScreen";
import { BankOcrCaptureScreen } from "../features/ocr/BankOcrCaptureScreen";
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
      <Stack.Screen name="InvoiceList" component={InvoiceListScreen} options={{ title: "Invoices" }} />
      <Stack.Screen name="Invoices" component={InvoiceCreateScreen} options={{ title: "New invoice" }} />
      <Stack.Screen name="PayoutList" component={PayoutListScreen} options={{ title: "Payouts" }} />
      <Stack.Screen name="CommitmentList" component={CommitmentListScreen} options={{ title: "Commitments" }} />
      <Stack.Screen name="CommitmentCreate" component={CommitmentCreateScreen} options={{ title: "New commitment" }} />
      <Stack.Screen
        name="CommitmentDetail"
        component={CommitmentDetailScreen}
        options={({ route }) => ({ title: route.params.title })}
      />
      <Stack.Screen name="InstallmentList" component={InstallmentListScreen} options={{ title: "Installments" }} />
      <Stack.Screen name="InstallmentCreate" component={InstallmentCreateScreen} options={{ title: "New installment" }} />
      <Stack.Screen
        name="InstallmentDetail"
        component={InstallmentDetailScreen}
        options={({ route }) => ({ title: route.params.title })}
      />
      <Stack.Screen name="ReminderList" component={ReminderListScreen} options={{ title: "Reminders" }} />
      <Stack.Screen name="ReminderCreate" component={ReminderCreateScreen} options={{ title: "New reminder" }} />
      <Stack.Screen name="BankOcrCapture" component={BankOcrCaptureScreen} options={{ title: "OCR bank capture" }} />
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
