import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { View, StyleSheet } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { AppProvider } from './src/context/AppContext';
import HomeScreen from './src/screens/HomeScreen';
import ReceiveFlowScreen from './src/screens/ReceiveFlowScreen';
import PayFlowScreen from './src/screens/PayFlowScreen';
import AnalyticsScreen from './src/screens/AnalyticsScreen';
import InstalmentScreen from './src/screens/InstalmentScreen';

const Stack = createStackNavigator();
const Tab = createBottomTabNavigator();

const COLORS = {
  background: '#0A0A1A',
  surface: '#12122A',
  primary: '#6C63FF',
  textSecondary: '#6B6B8D',
  cardBorder: 'rgba(108, 99, 255, 0.2)',
};

function HomeTabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarStyle: {
          backgroundColor: COLORS.surface,
          borderTopColor: COLORS.cardBorder,
          borderTopWidth: 1,
          height: 80,
          paddingBottom: 20,
          paddingTop: 10,
        },
        tabBarActiveTintColor: COLORS.primary,
        tabBarInactiveTintColor: COLORS.textSecondary,
        tabBarLabelStyle: { fontSize: 11, fontWeight: '600' },
        tabBarIcon: ({ color, size }) => {
          let iconName;
          if (route.name === 'Home') iconName = 'home';
          else if (route.name === 'Analytics') iconName = 'bar-chart-2';
          else if (route.name === 'Instalments') iconName = 'calendar';
          return <Feather name={iconName} size={22} color={color} />;
        },
      })}
    >
      <Tab.Screen name="Home" component={HomeScreen} />
      <Tab.Screen name="Analytics" component={AnalyticsScreen} />
      <Tab.Screen name="Instalments" component={InstalmentScreen} />
    </Tab.Navigator>
  );
}

export default function App() {
  return (
    <AppProvider>
      <NavigationContainer>
        <Stack.Navigator screenOptions={{ headerShown: false, cardStyle: { backgroundColor: COLORS.background } }}>
          <Stack.Screen name="MainTabs" component={HomeTabs} />
          <Stack.Screen name="ReceiveFlow" component={ReceiveFlowScreen} options={{ presentation: 'modal' }} />
          <Stack.Screen name="PayFlow" component={PayFlowScreen} options={{ presentation: 'modal' }} />
        </Stack.Navigator>
      </NavigationContainer>
    </AppProvider>
  );
}
