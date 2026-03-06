#import <AddressBook/AddressBook.h>
#import <UIKit/UIKit.h>
#import "RCTPagedContactsModule.h"

#import "WXContactsManager.h"

@implementation RCTPagedContactsModule
{
	dispatch_queue_t _managerDispatchQueue;
	NSMutableDictionary<NSString*, WXContactsManager*>* _managerMapping;
	NSDateFormatter* _jsonDateFormatter;
	CNContactFormatter* _displayNameFormatter;
}

- (instancetype)init
{
	self = [super init];
	
	if(self)
	{
		_managerDispatchQueue = dispatch_queue_create("_managerDispatchQueue", NULL);
		_managerMapping = [NSMutableDictionary new];
		_jsonDateFormatter = [NSDateFormatter new];
		_jsonDateFormatter.dateFormat = @"yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS'Z'";
		_displayNameFormatter = [CNContactFormatter new];
		_displayNameFormatter.style = CNContactFormatterStyleFullName;
	}
	
	return self;
}

RCT_EXPORT_MODULE(ReactNativePagedContacts);

// Constants are now defined in JavaScript for cross-platform consistency

- (WXContactsManager*)_managerForIdentifier:(NSString*)identifier
{
	__block WXContactsManager* manager;
	dispatch_sync(_managerDispatchQueue, ^{
		manager = _managerMapping[identifier];
		if(manager == nil)
		{
			manager = [WXContactsManager new];
			_managerMapping[identifier] = manager;
		}
	});
	
	return manager;
}

- (dispatch_queue_t)methodQueue
{
	return dispatch_queue_create("RCTPagedContactsModule", DISPATCH_QUEUE_SERIAL);
}

RCT_EXPORT_METHOD(getAuthorizationStatus:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
	resolve(@([WXContactsManager authorizationStatus]));
}

RCT_EXPORT_METHOD(requestAccess:(NSString*)uuid resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
	WXContactsManager* manager = [self _managerForIdentifier:uuid];
	
	[manager requestAccessWithCompletionHandler:^(BOOL granted, NSError* error) {
		if(error)
		{
			return reject(@(error.code).stringValue, error.localizedDescription, error);
		}
		
		return resolve(@(granted));
	}];
}

RCT_EXPORT_METHOD(dispose:(NSString*)identifier)
{
	dispatch_sync(_managerDispatchQueue, ^{
		[_managerMapping removeObjectForKey:identifier];
	});
}

RCT_EXPORT_METHOD(setNameMatch:(NSString*)uid nameMatch:(NSString*)nameMatch resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
	WXContactsManager* manager = [self _managerForIdentifier:uid];
	manager.nameMatch = nameMatch;
	resolve(nil);
}

RCT_EXPORT_METHOD(contactsCount:(NSString*)uid resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
	WXContactsManager* manager = [self _managerForIdentifier:uid];
	resolve(@(manager.contactsCount));
}


RCT_EXPORT_METHOD(addContacts:(NSDictionary*)contact uuid:(NSString*)uuid resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {
    WXContactsManager* manager = [self _managerForIdentifier:uuid];

    CNMutableContact * cnContact = [[CNMutableContact alloc] init];
    [self _updateCNContactWithContactData:cnContact withData:contact];

    [manager saveContact:cnContact];
    resolve(nil);
}

- (id)_transformValueToJSValue:(id)value
{
	if([value isKindOfClass:[NSArray class]])
	{
		NSMutableArray* transformed = [NSMutableArray new];
		[(NSArray*)value enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
			[transformed addObject:[self _transformValueToJSValue:obj]];
		}];
		return transformed;
	}
	else if([value isKindOfClass:[CNLabeledValue class]])
	{
		id label = [CNLabeledValue localizedStringForLabel:[(CNLabeledValue*)value label]];
		if (label == nil) label = @"";
		return @{@"label": label, @"value": [self _transformValueToJSValue:[(CNLabeledValue*)value value]]};
	}
	else if([value isKindOfClass:[CNPhoneNumber class]])
	{
		return [(CNPhoneNumber*)value stringValue];
	}
	else if([value isKindOfClass:[CNPostalAddress class]] || [value isKindOfClass:[CNSocialProfile class]] || [value isKindOfClass:[CNInstantMessageAddress class]])
	{
		return [value valueForKey:@"dictionaryRepresentation"];
	}
	else if([value isKindOfClass:[CNContactRelation class]])
	{
		return [(CNContactRelation*)value name];
	}
	else if([value isKindOfClass:[NSData class]])
	{
		return [(NSData*)value base64EncodedStringWithOptions:0];
	}
	else if([value isKindOfClass:[NSDateComponents class]])
	{
		return [[(NSDateComponents*)value calendar] dateFromComponents:value];
	}
	else if([value respondsToSelector:@selector(stringValue)])
	{
		return [value performSelector:@selector(stringValue)];
	}
	
	return value;
}

- (NSArray<NSDictionary*>*)_transformCNContactsToContactDatas:(NSArray<CNContact*>*) contacts keysToFetch:(NSArray<NSString*>*)keysToFetch managerForObscureContacts:(WXContactsManager*)manager
{
	NSMutableArray* rv = [NSMutableArray new];
	
	[contacts enumerateObjectsUsingBlock:^(CNContact* _Nonnull contact, NSUInteger idx, BOOL * _Nonnull stop) {
		NSMutableDictionary<NSString*, id>* rvC = [NSMutableDictionary new];
		
		rvC[@"identifier"] = contact.identifier;
		
		if([keysToFetch containsObject:@"displayName"])
		{
			NSString* displayName = [_displayNameFormatter stringFromContact:contact];
			if(displayName.length == 0)
			{
				if([contact isKeyAvailable:CNContactEmailAddressesKey] && contact.emailAddresses.count > 0)
				{
					displayName = contact.emailAddresses.firstObject.value;
				}
				else if([contact isKeyAvailable:CNContactPhoneNumbersKey] && contact.phoneNumbers.count > 0)
				{
					displayName = contact.phoneNumbers.firstObject.value.stringValue;
				}
				else
				{
					CNContact* fulfilledContact = [manager contactWithIdentifier:contact.identifier keysToFetch:@[CNContactEmailAddressesKey, CNContactPhoneNumbersKey]];
					displayName = fulfilledContact.emailAddresses.count > 0 ? fulfilledContact.emailAddresses.firstObject.value : fulfilledContact.phoneNumbers.firstObject.value.stringValue;
				}
			}
			rvC[@"displayName"] = displayName;
		}
		[keysToFetch enumerateObjectsUsingBlock:^(NSString * _Nonnull key, NSUInteger idx, BOOL * _Nonnull stop) {
			if([key isEqualToString:@"displayName"])
			{
				return;
			}
			
			id value = [self _transformValueToJSValue:[contact valueForKey:key]];
			if(value != nil
			   && ([value respondsToSelector:@selector(length)] == NO || [(NSString*)value length] > 0)
			   && ([value respondsToSelector:@selector(count)]  == NO || [(NSArray*)value count]   > 0))
			{
				rvC[key] = value;
			}
		}];
		
		[rv addObject:rvC];
	}];
	
	return rv;
}

-(void) _updateCNContactWithContactData:(CNMutableContact *)contact withData:(NSDictionary *)contactData
{
    NSString *givenName = [contactData valueForKey:@"givenName"];
    NSString *familyName = [contactData valueForKey:@"familyName"];
    NSString *middleName = [contactData valueForKey:@"middleName"];
    NSString *nickname = [contactData valueForKey:@"nickname"];
    NSString *namePrefix = [contactData valueForKey:@"namePrefix"];
    NSString *nameSuffix = [contactData valueForKey:@"nameSuffix"];
    NSString *departmentName = [contactData valueForKey:@"departmentName"];
    NSString *organizationName = [contactData valueForKey:@"organizationName"];
    NSString *jobTitle = [contactData valueForKey:@"jobTitle"];
    NSString *note = [contactData valueForKey:@"note"];

    contact.givenName = givenName;
    contact.familyName = familyName;
    contact.middleName = middleName;
    contact.namePrefix = namePrefix;
    contact.nameSuffix = nameSuffix;
    contact.nickname = nickname;
    contact.organizationName = organizationName;
    contact.departmentName = departmentName;
    contact.jobTitle = jobTitle;
    contact.note = note;


    NSMutableArray *phoneNumbers = [[NSMutableArray alloc]init];

    for (id phoneData in [contactData valueForKey:@"phoneNumbers"]) {
        NSString *label = [phoneData valueForKey:@"label"];
        NSString *number = [phoneData valueForKey:@"value"];

        CNLabeledValue *phone;
        if ([label isEqual: @"main"]){
            phone = [[CNLabeledValue alloc] initWithLabel:CNLabelPhoneNumberMain value:[[CNPhoneNumber alloc] initWithStringValue:number]];
        }
        else if ([label isEqual: @"mobile"]){
            phone = [[CNLabeledValue alloc] initWithLabel:CNLabelPhoneNumberMobile value:[[CNPhoneNumber alloc] initWithStringValue:number]];
        }
        else if ([label isEqual: @"iPhone"]){
            phone = [[CNLabeledValue alloc] initWithLabel:CNLabelPhoneNumberiPhone value:[[CNPhoneNumber alloc] initWithStringValue:number]];
        }
        else{
            phone = [[CNLabeledValue alloc] initWithLabel:label value:[[CNPhoneNumber alloc] initWithStringValue:number]];
        }

        [phoneNumbers addObject:phone];
    }
    contact.phoneNumbers = phoneNumbers;


    NSMutableArray *urls = [[NSMutableArray alloc]init];

    for (id urlData in [contactData valueForKey:@"urlAddresses"]) {
        NSString *label = [urlData valueForKey:@"label"];
        NSString *url = [urlData valueForKey:@"value"];

        if(label && url) {
            [urls addObject:[[CNLabeledValue alloc] initWithLabel:label value:url]];
        }
    }

    contact.urlAddresses = urls;


    NSMutableArray *emails = [[NSMutableArray alloc]init];

    for (id emailData in [contactData valueForKey:@"emailAddresses"]) {
        NSString *label = [emailData valueForKey:@"label"];
        NSString *email = [emailData valueForKey:@"value"];

        if(label && email) {
            [emails addObject:[[CNLabeledValue alloc] initWithLabel:label value:email]];
        }
    }

    contact.emailAddresses = emails;

    NSMutableArray *postalAddresses = [[NSMutableArray alloc]init];

    for (id addressData in [contactData valueForKey:@"postalAddresses"]) {
        NSString *label = [addressData valueForKey:@"label"];

        NSString* postalAddressData = [addressData valueForKey:@"value"];

        NSString *street = [postalAddressData valueForKey:@"street"];
        NSString *postalCode = [postalAddressData valueForKey:@"postCode"];
        NSString *city = [postalAddressData valueForKey:@"city"];
        NSString *country = [postalAddressData valueForKey:@"country"];
        NSString *state = [postalAddressData valueForKey:@"state"];
        
        if(label && street) {
            CNMutablePostalAddress *postalAddr = [[CNMutablePostalAddress alloc] init];
            postalAddr.street = street;
            postalAddr.postalCode = postalCode;
            postalAddr.city = city;
            postalAddr.country = country;
            postalAddr.state = state;
            [postalAddresses addObject:[[CNLabeledValue alloc] initWithLabel:label value: postalAddr]];
        }
    }

    contact.postalAddresses = postalAddresses;

    NSString *imageUrl = [contactData valueForKey:@"imageUrl"];

    if(imageUrl) {
        contact.imageData = [RCTPagedContactsModule imageDataFromUrl:imageUrl];
    }
}

+ (NSData*) imageDataFromUrl:(NSString*)sourceUrl
{
    NSURL* url = [[NSURL alloc] initWithString:sourceUrl];
    return [NSData dataWithContentsOfURL:url];
}

- (NSArray*)_keysToFetchIncludingManadatoryKeys:(NSArray*)keysToFetch
{
	NSMutableSet* rvSet = [NSMutableSet setWithArray:keysToFetch];
	[rvSet addObject:CNContactIdentifierKey];
	if([keysToFetch containsObject:@"displayName"])
	{
		[rvSet removeObject:@"displayName"];
		[rvSet addObject:[CNContactFormatter descriptorForRequiredKeysForStyle:CNContactFormatterStyleFullName]];
	}
	
	return rvSet.allObjects;
}

RCT_EXPORT_METHOD(getContactsWithRange:(NSString*)uuid offset:(double)offset size:(double)size keysToFetch:(NSArray*)keysToFetch resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
	if(size == 0)
	{
		return resolve(@[]);
	}

	NSArray* realKeysToFetch = [self _keysToFetchIncludingManadatoryKeys:keysToFetch];

	WXContactsManager* manager = [self _managerForIdentifier:uuid];
	NSArray<CNContact*>* contacts = [manager contactsWithRange:NSMakeRange((NSUInteger)offset, (NSUInteger)size) keysToFetch:realKeysToFetch];

	resolve([self _transformCNContactsToContactDatas:contacts keysToFetch:keysToFetch managerForObscureContacts:manager]);
}

RCT_EXPORT_METHOD(getContactsWithIdentifiers:(NSString*)uuid identifiers:(NSArray*)identifiers keysToFetch:(NSArray*)keysToFetch resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
	if(identifiers.count == 0)
	{
		return resolve(@[]);
	}
	
	NSArray* realKeysToFetch = [self _keysToFetchIncludingManadatoryKeys:keysToFetch];

	WXContactsManager* manager = [self _managerForIdentifier:uuid];
	NSArray* contacts = [manager contactsWithIdentifiers:identifiers keysToFetch:realKeysToFetch];
	
	resolve([self _transformCNContactsToContactDatas:contacts keysToFetch:keysToFetch managerForObscureContacts:manager]);
}


+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativePagedContactsSpecJSI>(params);
}

@end
