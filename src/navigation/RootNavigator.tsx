/**
 * Root navigator – defines all 5 screens and the bottom-tab navigation.
 */

import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { Colors } from '../utils/theme';

import MainMenuScreen from '../screens/MainMenuScreen';
import VirtualCameraScreen from '../screens/VirtualCameraScreen';
import DeepfakeScreen from '../screens/DeepfakeScreen';
import CreateDocScreen from '../screens/CreateDocScreen';
import OBSConnectScreen from '../screens/OBSConnectScreen';

const Stack = createNativeStackNavigator();

const SCREEN_OPTIONS = {
  headerShown: false,
};

const STACK_SCREEN_OPTIONS = {
  headerShown: true,
  headerStyle: {
    backgroundColor: Colors.background,
  },
  headerTintColor: Colors.text,
  headerTitleStyle: {
    fontWeight: '700' as const,
    fontSize: 18,
  },
  cardStyle: {
    backgroundColor: Colors.background,
  },
};

export default function RootNavigator() {
  return (
    <NavigationContainer
      theme={{
        colors: {
          primary: Colors.primary,
          background: Colors.background,
          card: Colors.card,
          text: Colors.text,
          border: Colors.cardBorder,
          notification: Colors.danger,
        },
        dark: false,
        fonts: {
          regular: {
            fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
            fontWeight: '400' as const,
          },
          medium: {
            fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
            fontWeight: '500' as const,
          },
          bold: {
            fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
            fontWeight: '700' as const,
          },
          heavy: {
            fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
            fontWeight: '800' as const,
          },
        },
      }}
    >
      <Stack.Navigator initialRouteName="MainMenu" screenOptions={SCREEN_OPTIONS}>
        <Stack.Screen name="MainMenu" component={MainMenuScreen} />
        <Stack.Screen name="VirtualCamera" component={VirtualCameraScreen} options={STACK_SCREEN_OPTIONS} />
        <Stack.Screen name="Deepfake" component={DeepfakeScreen} options={STACK_SCREEN_OPTIONS} />
        <Stack.Screen name="CreateDoc" component={CreateDocScreen} options={STACK_SCREEN_OPTIONS} />
        <Stack.Screen name="OBSConnect" component={OBSConnectScreen} options={STACK_SCREEN_OPTIONS} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
