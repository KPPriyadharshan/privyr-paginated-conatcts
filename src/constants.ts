/**
 * Contact field keys for iOS and Android
 */
export const ContactFields = {
  identifier: 'identifier',
  displayName: 'displayName',

  namePrefix: 'namePrefix',
  givenName: 'givenName',
  middleName: 'middleName',
  familyName: 'familyName',
  previousFamilyName: 'previousFamilyName',
  nameSuffix: 'nameSuffix',
  nickname: 'nickname',
  organizationName: 'organizationName',
  departmentName: 'departmentName',
  jobTitle: 'jobTitle',
  phoneticGivenName: 'phoneticGivenName',
  phoneticMiddleName: 'phoneticMiddleName',
  phoneticFamilyName: 'phoneticFamilyName',
  phoneticOrganizationName: 'phoneticOrganizationName',
  birthday: 'birthday',
  nonGregorianBirthday: 'nonGregorianBirthday',
  note: 'note',
  imageData: 'imageData',
  thumbnailImageData: 'thumbnailImageData',
  phoneNumbers: 'phoneNumbers',
  emailAddresses: 'emailAddresses',
  postalAddresses: 'postalAddresses',
  dates: 'dates',
  urlAddresses: 'urlAddresses',
  socialProfiles: 'socialProfiles',
  instantMessageAddresses: 'instantMessageAddresses',
  relations: 'relations',
} as const;

/**
 * Authorization status constants
 */
export const AuthorizationStatus = {
  denied: 0,
  notDetermined: 1,
  authorized: 2,
  restricted: 3,
} as const;

export type ContactFieldKey = keyof typeof ContactFields;
export type AuthorizationStatusValue = typeof AuthorizationStatus[keyof typeof AuthorizationStatus];
