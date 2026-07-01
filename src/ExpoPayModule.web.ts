import { registerWebModule, NativeModule } from 'expo';

import { ExpoPayModuleEvents } from './ExpoPay.types';

// ExpoPayModule is not available on the web platform.
class ExpoPayModule extends NativeModule<ExpoPayModuleEvents> {}

export default registerWebModule(ExpoPayModule, 'ExpoPayModule');
