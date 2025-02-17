package com.wix.pagedcontacts.contacts;

import android.content.Context;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;

import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;

import com.wix.pagedcontacts.contacts.Items.Contact;
import com.wix.pagedcontacts.contacts.query.QueryParams;
import com.wix.pagedcontacts.contacts.readers.ContactCursorReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContactsProvider {
    public static final int MAX_ARGS = 990;
    private Context context;
    public List<String> contactIds;
    private String matchName;

    private  ContactCursorReader contactCursorReader;

    public void setMatchName(String matchName) {
        this.matchName = matchName;
        contactIds.clear();
    }

    ContactsProvider(Context context) {
        this.context = context;
        contactIds = new ArrayList<>();
        this.contactCursorReader = new ContactCursorReader(context);
    }

    public int getContactsCount() {
        ensureContactIds();
        return contactIds.size();
    }

    private List<String> getAllContacts() {
        List<String> contactIds = new ArrayList<>();
        Set<String> dedupSet = new HashSet<>();
        Cursor cursor = queryContacts(new QueryParams(matchName));
        ContactCursorReader reader = new ContactCursorReader(context);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                Contact contact = reader.read(cursor);
                if (dedupSet.add(contact.getContactId())) {
                    contactIds.add(contact.getContactId());
                }
            }
            cursor.close();
        }
        return contactIds;
    }

    public WritableArray getContacts(QueryParams params) {
        if (matchName != null) {
            return getContactsWithNameFilter(params);
        } else {
            List<Contact> contactsWithRange = getContactsWithRange(params);
            return toWritableArray(params, new List[]{contactsWithRange});
        }
    }

    private WritableArray getContactsWithNameFilter(QueryParams params) {
        ensureContactIds();
        int size = contactIds.size();
        int offset = 0;
        List<List<Contact>> contactLists = new ArrayList<>();
        while (size > 0) {
            params.size = size > MAX_ARGS ? MAX_ARGS : size;
            params.offset = offset;
            contactLists.add(getContactsWithRange(params));

            offset += params.size;
            size = size - MAX_ARGS;
        }
        List[] lists = contactLists.toArray(new List[contactLists.size()]);
        return toWritableArray(params, lists);
    }

    private List<Contact> getContactsWithRange(QueryParams params) {
        ensureContactIds();
        List<String> contactsToFetch = getContactsToFetch(params);
        if (contactsToFetch.isEmpty()) {
            return Collections.emptyList();
        }
        this.contactCursorReader.batchSize = params.size;
        this.contactCursorReader.offset = params.offset;
        if(params.offset == 0 && this.contactCursorReader.cachedContacts.size() == params.size) {
          this.contactCursorReader.syncCachedContacts(params);
          params.getProjection(); // this called just to populate all the keys to query param
          List<Contact> contacts = new ArrayList<>(this.contactCursorReader.cachedContacts.values());
          return  contacts;
        }
        params.setContactsToFetch(contactsToFetch);
        Cursor cursor = queryContacts(params);
        if(params.offset == 0 ) {
          // cache the first offset contacts
          this.contactCursorReader.syncCachedContacts(params);
        }
        return this.contactCursorReader.readWithIds(cursor);
    }

    public WritableArray getContactsWithIdentifiers(QueryParams params) {
        ensureContactIds();
        params.setContactsToFetch(getContactsToFetch(params));
        Cursor cursor = queryContacts(params);
        List<Contact> contacts = new ContactCursorReader(context).readWithIds(cursor);
        return toWritableArray(params, new List[]{contacts});
    }

    private WritableArray toWritableArray(QueryParams params, List<Contact>[] contactsLists) {
        WritableArray result = Arguments.createArray();
        for (List<Contact> contactList : contactsLists) {
            for (Contact contact : contactList) {
                result.pushMap(contact.toMap(params));
            }
        }
        return result;
    }

    private void ensureContactIds() {
        if (contactIds.isEmpty()) {
            sync();
        }
    }

    private void sync() {
        contactIds = getAllContacts();
    }

    private Cursor queryContacts(QueryParams params) {
        return context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                params.getProjection(),
                params.getSelection(),
                params.getSelectionArgs(),
                ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
        );
    }

    private List<String> getContactsToFetch(QueryParams params) {
        if (!params.getIdentifiers().isEmpty()) {
            return params.getIdentifiers();
        }
        List<String> ids = new ArrayList<>();
        int offset = params.offset <= getContactsCount() ? params.offset : getContactsCount() - 1;
        int size = offset + params.size < getContactsCount() ? params.size : getContactsCount() - offset;
        for (int i = offset; i < offset + size; i++) {
            ids.add(contactIds.get(i));
        }
        return ids;
    }

    public void saveContact(ReadableMap contact, Promise promise) {
        try {

            ArrayList<ContentProviderOperation> ops = (new Contact(contact)).createOps();


            ContentResolver cr = this.context.getContentResolver();
            ContentProviderResult[] result = cr.applyBatch(ContactsContract.AUTHORITY, ops);

            promise.resolve(null);

        } catch (Exception e) {
            promise.reject(e);
        }
    }


}
