import { NativeModule, requireNativeModule } from 'expo';

import { ExpoPayModuleEvents } from './ExpoPay.types';

declare class ExpoPayModule extends NativeModule<ExpoPayModuleEvents> {
  PI: number;
  hello(): string;
  setValueAsync(value: string): Promise<void>;
}

export default requireNativeModule<ExpoPayModule>('ExpoPay');
