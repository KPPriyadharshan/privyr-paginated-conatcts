import type { TurboModule } from "react-native/Libraries/TurboModule/RCTExport";
import { TurboModuleRegistry } from "react-native";

export interface Spec extends TurboModule {
  getAuthorizationStatus: () => Promise<any>;
  requestAccess: (uuid: string) => Promise<any>;
  setNameMatch: (uid: string, nameMatch: string) => Promise<any>;
  contactsCount: (uid: string) => Promise<number>;
  getContactsWithRange: (
    uuid: string,
    offset: number,
    size: number,
    keysToFetch: any[],
  ) => Promise<any>;
  getContactsWithIdentifiers: (
    uuid: string,
    identifiers: any[],
    keysToFetch: any[],
  ) => Promise<any>;
  addContacts: (contact: Object, uuid: string) => Promise<any>;
  dispose: (uuid: string) => void;
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  "ReactNativePagedContacts",
);
