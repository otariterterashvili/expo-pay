import { requireNativeView } from 'expo';
import * as React from 'react';

import { ExpoPayViewProps } from './ExpoPay.types';

const NativeView: React.ComponentType<ExpoPayViewProps> = requireNativeView('ExpoPay');

export default function ExpoPayView(props: ExpoPayViewProps) {
  return <NativeView {...props} />;
}
