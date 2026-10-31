import { StatusBar } from 'expo-status-bar';
import { Colors } from './src/utils/theme';
import RootNavigator from './src/navigation/RootNavigator';

export default function App() {
  return (
    <>
      <StatusBar style="light" />
      <RootNavigator />
    </>
  );
}
