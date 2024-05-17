package com.wix.pagedcontacts.contacts.readers;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import com.wix.pagedcontacts.contacts.Items.Contact;
import com.wix.pagedcontacts.contacts.Items.ContactItemReader;
import com.wix.pagedcontacts.contacts.Items.DisplayName;
import com.wix.pagedcontacts.contacts.Items.Identity;
import com.wix.pagedcontacts.contacts.Items.InvalidCursorTypeException;
import com.wix.pagedcontacts.contacts.query.QueryParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContactCursorReader {
    private Map<String, Contact> contacts;
    private Context context;

    public HashMap<String, Contact> cachedContacts = new HashMap<>();

     public int offset = 0;
     public  int batchSize = 0;

     boolean isSyncInProgress = false;

     String TAG = "CONTACTS";


    private boolean  readWithIdsCursorRunning = false;

    public ContactCursorReader(Context context) {
        this.context = context;
        contacts = new HashMap<>();
    }

    public Contact read(Cursor cursor) {
        Contact contact = new Contact(getId(cursor));
        contact.identity = new Identity(cursor);
        contact.displayName = new DisplayName(cursor);
        return contact;
    }

    public List<Contact> readWithIds(Cursor cursor) {
        Set<String> fetchedContacts = new HashSet<>();
        List<Contact> contacts = new ArrayList<>();
        this.readWithIdsCursorRunning = true;

        if(this.offset == 0 && this.cachedContacts.values().size() == batchSize) {
          Log.d(TAG, "readWithIds: contacts are found in cache");
          contacts = new ArrayList<>(this.cachedContacts.values());
          this.readWithIdsCursorRunning = false;
          Log.d(TAG, "readWithIds: contacts are not found in cache");
          cursor.close();
          return  contacts;
        }
        while (cursor.moveToNext()) {
          String contactIdFromCursor = getId(cursor);
          Contact contact;
          if(cachedContacts.containsKey(contactIdFromCursor)) {
            contact = cachedContacts.get(contactIdFromCursor);
            Log.d("contact service", "readWithIds: " + contactIdFromCursor + " Found in the cache");
          }else {
             contact  = read(cursor, getId(cursor));
            Log.d("contact service", "readWithIds: " + contactIdFromCursor + " Not Found in the cache fetching from cursor");
          }
            String contactId = contact.getContactId();
            if (!fetchedContacts.contains(contactId)) {
                fetchedContacts.add(contactId);
                contacts.add(contact);
            }
        }
        Log.d("contacts", "readWithIds: 3 " + contacts.size());
        this.readWithIdsCursorRunning = false;
        cursor.close();
        return contacts;
    }

    public void  syncCachedContacts(QueryParams params) {
      if(isSyncInProgress) {
        Log.d(TAG, "syncCachedContacts: sync skipped");
        return;
      }
      new Thread(() -> {
        try {
          // data is already there no need to sync
         synchronized (this) {
           isSyncInProgress = true;
         }

          Cursor contactCounts  = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
            params.getProjection(),
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
          );

          int allContacts = contactCounts.getCount();
          contactCounts.close();

          HashMap contactsToStore = new HashMap<String, Contact>();

          Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
            params.getProjection(),
            params.getSelection(),
            params.getSelectionArgs(),
            ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
          );
          Log.d("sync contacts", "syncCachedContacts: started ");
          if(allContacts > 50000) {
              // synchronize the contacts once the contact access is done

              while (cursor.moveToNext()) {
                if(contactsToStore.values().size() < 200) {
                  String contactIdFromCursor = getId(cursor);
                  Contact contact = read(cursor, contactIdFromCursor);
                  contactsToStore.put(contact.getContactId() , contact);
                  Log.d("contact", "syncCachedContacts:  " + contact.getContactId()
                    + contact.displayName.name);
                }else{
                  cursor.close();
                  break;
                }
              }

            synchronized (this) {
              cachedContacts = contactsToStore;
            }
            cursor.close();

          }else {
            cursor.close();
          }
        } catch (Exception e) {
          Log.d("contact sync error", "syncCachedContacts: " + e.getMessage());
        }
      }).start();
      Log.d("sync contacts", "syncCachedContacts: exit ");
      return;
    }


    private Contact read(Cursor cursor, String contactId) {
        Contact contact = getContact(contactId);
        contact.displayName = new DisplayName(cursor);
        readField(cursor, contact);
        return contact;
    }

    private void readField(Cursor cursor, Contact contact) {
        try {
            new ContactItemReader(contact, context).read(cursor);
        } catch (InvalidCursorTypeException e) {
            // Nothing
        }
    }

    private Contact getContact(String contactId) {
        Contact contact = contacts.get(contactId);
        if (contact == null) {
            contact = new Contact(contactId);
            contacts.put(contactId, contact);
        }
        return contact;
    }

    private String getId(Cursor cursor) {
        return String.valueOf(cursor.getInt(cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)));
    }
}
