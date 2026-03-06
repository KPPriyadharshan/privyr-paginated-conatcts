import { Platform, PermissionsAndroid } from "react-native";

import NativePageContacts from "./src/NativePagedContacts";
import { ContactFields, AuthorizationStatus } from "./src/constants";

const PagedContactsModule = NativePageContacts;

/**
 * Generates a globally unique identifier.
 *
 * @returns {String} A globally unique identifier.
 */
function guid() {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
    let r = (Math.random() * 16) | 0,
      v = c == "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

/**
 * The `PagedContacts` class is a class that can fetch native contacts in pages.
 *
 * @export
 * @class PagedContacts
 */
export class PagedContacts {
  /**
   * Creates an instance of PagedContacts.
   *
   * @param {String} nameMatch The contact name to be matched
   *
   * @memberOf PagedContacts
   */
  constructor(nameMatch) {
    this._uuid = guid();
    this.setNameMatch(nameMatch);
  }

  /**
   * Return the authorization status.
   *
   * @returns {String} The authorization status
   *
   * @memberOf PagedContacts
   */
  async getAuthorizationStatus() {
    return PagedContactsModule.getAuthorizationStatus();
  }

  /**
   * Requests contact access from the operating system.
   *
   * @returns {Boolean} `true` if access was granted or `false` otherwise
   *
   * @memberOf PagedContacts
   */
  async requestAccess() {
    return PagedContactsModule.requestAccess(this._uuid);
  }

  /**
   * Set the contact name to be matched.
   *
   * @param {String} str The contact name to be matched
   *
   * @memberOf PagedContacts
   */
  setNameMatch(str) {
    this._nameMatch = str;
    PagedContactsModule.setNameMatch(this._uuid, str);
  }

  /**
   * Return the total number of contacts.
   *
   * @returns {Number} The total number of contacts
   *
   * @memberOf PagedContacts
   */
  async getContactsCount() {
    return PagedContactsModule.contactsCount(this._uuid);
  }

  /**
   * Fetches `batchSize` contacts, starting from `offset`, returning the provided keys.
   *
   * @param {Number} offset The fetch offset
   * @param {Number} batchSize The fetch size
   * @param {String[]} keysToFetch The keys to fetch
   * @returns {Object[]} The fetched contacts
   *
   * @memberOf PagedContacts
   */
  async getContactsWithRange(offset, batchSize, keysToFetch) {
    return PagedContactsModule.getContactsWithRange(
      this._uuid,
      offset,
      batchSize,
      keysToFetch,
    );
  }

  /**
   * Fetches contacts matching the provided identifiers, returning the provided keys.
   *
   * @param {String[]} identifiers The contact identifiers to match
   * @param {String[]} keysToFetch The keys to fetch
   * @returns {Object[]} The fetched contacts
   *
   * @memberOf PagedContacts
   */
  async getContactsWithIdentifiers(identifiers, keysToFetch) {
    return PagedContactsModule.getContactsWithIdentifiers(
      this._uuid,
      identifiers,
      keysToFetch,
    );
  }

  /**
   * Disposes the underlying native component, freeing resources.
   * You must call this when the `PagedContacts` object is no longer needed.
   *
   * @memberOf PagedContacts
   */
  dispose() {
    if (Platform.OS === "ios" && PagedContactsModule.dispose) {
      PagedContactsModule.dispose(this._uuid);
    }
  }

  /**
   * Creates a Contact on the device.
   * @param {Object} contact the contact to be added to the device
   *
   * @memberOf PagedContacts
   */
  async addContact(contact) {
    if (Platform.OS === "android") {
      await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.WRITE_CONTACTS,
        {
          title: "Contacts",
          message: "This App would like to write contacts",
        },
      );
    }

    await PagedContactsModule.addContacts(contact, this._uuid);
  }
}

// Contact field constants
PagedContacts.identifier = ContactFields.identifier;
PagedContacts.displayName = ContactFields.displayName;
PagedContacts.namePrefix = ContactFields.namePrefix;
PagedContacts.givenName = ContactFields.givenName;
PagedContacts.middleName = ContactFields.middleName;
PagedContacts.familyName = ContactFields.familyName;
PagedContacts.previousFamilyName = ContactFields.previousFamilyName;
PagedContacts.nameSuffix = ContactFields.nameSuffix;
PagedContacts.nickname = ContactFields.nickname;
PagedContacts.organizationName = ContactFields.organizationName;
PagedContacts.departmentName = ContactFields.departmentName;
PagedContacts.jobTitle = ContactFields.jobTitle;
PagedContacts.phoneticGivenName = ContactFields.phoneticGivenName;
PagedContacts.phoneticMiddleName = ContactFields.phoneticMiddleName;
PagedContacts.phoneticFamilyName = ContactFields.phoneticFamilyName;
PagedContacts.phoneticOrganizationName = ContactFields.phoneticOrganizationName;
PagedContacts.birthday = ContactFields.birthday;
PagedContacts.nonGregorianBirthday = ContactFields.nonGregorianBirthday;
PagedContacts.note = ContactFields.note;
PagedContacts.imageData = ContactFields.imageData;
PagedContacts.thumbnailImageData = ContactFields.thumbnailImageData;
PagedContacts.phoneNumbers = ContactFields.phoneNumbers;
PagedContacts.emailAddresses = ContactFields.emailAddresses;
PagedContacts.postalAddresses = ContactFields.postalAddresses;
PagedContacts.dates = ContactFields.dates;
PagedContacts.urlAddresses = ContactFields.urlAddresses;
PagedContacts.socialProfiles = ContactFields.socialProfiles;
PagedContacts.relations = ContactFields.relations;
PagedContacts.instantMessageAddresses = ContactFields.instantMessageAddresses;

// Authorization status constants
PagedContacts.denied = AuthorizationStatus.denied;
PagedContacts.notDetermined = AuthorizationStatus.notDetermined;
PagedContacts.authorized = AuthorizationStatus.authorized;
PagedContacts.restricted = AuthorizationStatus.restricted;
