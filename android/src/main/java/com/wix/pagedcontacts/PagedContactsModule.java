package com.wix.pagedcontacts;

import android.Manifest;
import androidx.core.content.PermissionChecker;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.wix.pagedcontacts.contacts.ContactsProviderFactory;
import com.wix.pagedcontacts.contacts.Field;
import com.wix.pagedcontacts.contacts.permission.ReadContactsPermissionStatus;
import com.wix.pagedcontacts.contacts.query.QueryParams;
import com.wix.pagedcontacts.utils.Collections;
import com.wix.pagedcontacts.NativePagedContactsSpec;

import java.util.HashMap;
import java.util.Map;

public class PagedContactsModule extends NativePagedContactsSpec {
    public static final int READ_CONTACTS_PERMISSION_REQUEST_CODE = 30156;
    private final ContactsProviderFactory contactProvider;
    private Promise requestPermissionPromise;
    // name for android RNPagedContacts
    public static final String NAME = "ReactNativePagedContacts";

    public PagedContactsModule(ReactApplicationContext context) {
        super(context);
        contactProvider = new ContactsProviderFactory(context);
    }

    @Override
    public void getAuthorizationStatus(Promise promise) {
        promise.resolve(ReadContactsPermissionStatus.getAuthorizationStatus(getCurrentActivity()));
    }

     @Override
    public void dispose(String uuid) {}

    @Override
    public void requestAccess(String uuid, Promise promise) {
        if (ReadContactsPermissionStatus.isAuthorized(getCurrentActivity())) {
            promise.resolve(true);
            return;
        }
        ReadContactsPermissionStatus.requestAccess(getCurrentActivity());
        this.requestPermissionPromise = promise;
    }

    @Override
    public void setNameMatch(String uid, String nameMatch, Promise promise) {
        contactProvider.get(uid).setMatchName(nameMatch);
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        Field.exportToJs(constants);
        ReadContactsPermissionStatus.exportToJs(constants);
        return constants;
    }

    @Override
    public void contactsCount(String uuid, Promise promise) {
        final int count = contactProvider.get(uuid).getContactsCount();
        promise.resolve(count);
    }

    @Override
    public void getContactsWithRange(String uuid, double offset, double size, ReadableArray keysToFetch,
            Promise promise) {
        QueryParams params = new QueryParams(Collections.toStringList(keysToFetch), (int) offset, (int) size);
        WritableArray contacts = contactProvider.get(uuid).getContacts(params);
        promise.resolve(contacts);
    }

    @Override
    public void getContactsWithIdentifiers(String uuid, ReadableArray identifiers, ReadableArray keysToFetch,
            Promise promise) {
        QueryParams params = new QueryParams(Collections.toStringList(keysToFetch),
                Collections.toStringList(identifiers));
        WritableArray contacts = contactProvider.get(uuid).getContactsWithIdentifiers(params);
        promise.resolve(contacts);
    }

    @Override
    public void addContacts(ReadableMap contact, String uuid, Promise promise) {
        contactProvider.get(uuid).saveContact(contact, promise);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (isReadContactsPermission(requestCode, permissions)) {
            if (requestPermissionPromise != null)
                requestPermissionPromise.resolve(grantResults[0] == PermissionChecker.PERMISSION_GRANTED);
        }
    }

    private boolean isReadContactsPermission(int requestCode, String[] permissions) {
        return requestCode == READ_CONTACTS_PERMISSION_REQUEST_CODE &&
                Manifest.permission.READ_CONTACTS.equals(permissions[0]);
    }
}