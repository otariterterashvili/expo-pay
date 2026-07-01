import { ExpoPayViewProps } from './ExpoPay.types';

// ExpoPayView is not available on the web platform.
export default function ExpoPayView(_props: ExpoPayViewProps) {
  throw new Error('ExpoPayView is not available on the web platform.');
}
