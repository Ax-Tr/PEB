import React from "react";
import { createStackNavigator } from "@react-navigation/stack";
import { MoreMenuScreen } from "../features/more/MoreMenuScreen";
import { InsightsScreen } from "../features/dashboard/InsightsScreen";
import { DataRightsScreen } from "../features/privacy/DataRightsScreen";
import { DataRightDetailScreen } from "../features/privacy/DataRightDetailScreen";
import { AssistantScreen } from "../features/ai/AssistantScreen";
import { VoiceDraftReviewScreen } from "../features/ai/VoiceDraftReviewScreen";
import { theme } from "../theme/theme";
import type { MoreStackParamList } from "./types";

const Stack = createStackNavigator<MoreStackParamList>();

export function MoreNavigator(): React.ReactElement {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: theme.color.surface },
        headerTitleStyle: { color: theme.color.text },
        headerTintColor: theme.color.primary,
        cardStyle: { backgroundColor: theme.color.bg },
      }}
    >
      <Stack.Screen name="MoreMenu" component={MoreMenuScreen} options={{ title: "More" }} />
      <Stack.Screen name="Insights" component={InsightsScreen} options={{ title: "Insights" }} />
      <Stack.Screen name="Assistant" component={AssistantScreen} options={{ title: "AI assistant" }} />
      <Stack.Screen name="VoiceDraftReview" component={VoiceDraftReviewScreen} options={{ title: "Voice drafts" }} />
      <Stack.Screen name="DataRights" component={DataRightsScreen} options={{ title: "Data rights" }} />
      <Stack.Screen name="DataRightDetail" component={DataRightDetailScreen} options={{ title: "Request" }} />
    </Stack.Navigator>
  );
}
