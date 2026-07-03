import React from "react";
import { StyleSheet, Text, View } from "react-native";
import type { StackScreenProps } from "@react-navigation/stack";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { Money } from "../../components/Money";
import { QueryState } from "../../components/QueryState";
import { Screen } from "../../components/Screen";
import type { InvoiceResponse } from "../../shared/types";
import { theme } from "../../theme/theme";
import type { BooksStackParamList } from "../../navigation/types";
import { useInvoices } from "./hooks";

type Props = StackScreenProps<BooksStackParamList, "InvoiceList">;

/** List invoices for the tenant with a shortcut to create a new one. */
export function InvoiceListScreen({ navigation }: Props): React.ReactElement {
  const invoices = useInvoices();
  return (
    <Screen>
      <Button title="＋ New invoice" onPress={() => navigation.navigate("Invoices")} />
      <QueryState query={invoices} emptyWhen={(l) => l.length === 0} emptyText="No invoices yet.">
        {(list: InvoiceResponse[]) => (
          <View style={styles.list}>
            {list.map((inv) => (
              <Card key={inv.id}>
                <View style={styles.row}>
                  <Text style={styles.num}>{inv.invoiceNumber}</Text>
                  <Money minor={inv.totalAmountMinor} size="body" />
                </View>
                <Text style={styles.sub}>
                  {inv.customerName ?? "—"} · {inv.status}
                </Text>
              </Card>
            ))}
          </View>
        )}
      </QueryState>
    </Screen>
  );
}

const styles = StyleSheet.create({
  list: { gap: theme.space(3), marginTop: theme.space(3) },
  row: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  num: { color: theme.color.text, fontSize: theme.font.title, fontWeight: "700" },
  sub: { color: theme.color.textMuted, fontSize: theme.font.caption },
});
