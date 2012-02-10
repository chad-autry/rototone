package com.emergentgameplay.multitone;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;

public class ContactsServiceNewApi extends ContactsService{

	@Override
	public Intent getContactPickerIntent() {
		return new Intent(Intent.ACTION_PICK,
                Contacts.CONTENT_URI);
	}

	@Override
	public void updateContactCustomRingtone(Context ctx, Uri contactUri, String ringtoneUri) {
        ContentValues values = new ContentValues();
        if(ringtoneUri == null){
        	values.putNull(Contacts.CUSTOM_RINGTONE);
        }
        else{
        	values.put(Contacts.CUSTOM_RINGTONE, ringtoneUri);
        }
        ctx.getContentResolver().update(contactUri, values, null, null);
	}
	
	@Override
	public String retrieveCustomRingtone(Context cntx, String phoneNumber) {
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
		Cursor cursor = cntx.getContentResolver().query(uri, new String[]{ContactsContract.Contacts.CUSTOM_RINGTONE}, null, null, null);
		String result = null;
		if(cursor.moveToFirst()){
			result =cursor.getString(0);
		}
		cursor.close();
		return result;
	}

	@Override
	public Cursor getContactsCursor(Context cntx, String filter,Collection<String> filterCollection) {
		String selection = "HAS_PHONE_NUMBER = 1";
		String[] values=null;
		if(filterCollection != null){
			Iterator<String> it = filterCollection.iterator();
			if(it.hasNext()){
				values = new String[filterCollection.size()];
				selection = selection + " and (";
	
				for (int i=0;it.hasNext();i++) {
					values[i]=it.next();
					selection = selection +ContactsContract.Contacts.LOOKUP_KEY + " =?";
					if(it.hasNext()){
						selection=selection+" or ";
					}

				}
				selection = selection + ")";
			}

		}
//		if(filter != null && !filter.equals("")){
//			selection = selection +" and "+ContactsContract.Contacts.DISPLAY_NAME+" LIKE ?"; 
//		}
		Cursor cursor = cntx.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, new String[]{ ContactsContract.Contacts._ID,ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.CUSTOM_RINGTONE,ContactsContract.Contacts.LOOKUP_KEY}, selection, values, ContactsContract.Contacts.DISPLAY_NAME);
		return cursor;
	}

	@Override
	public String[] getDataBaseColumns() {
		return new String[]{ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.CUSTOM_RINGTONE, ContactsContract.Contacts.LOOKUP_KEY};
	}

	@Override
	public Uri getContactUri(long id) {
		String contactUri = ContactsContract.Contacts.CONTENT_URI + "/"+id;
		return Uri.parse(contactUri);
	}

	@Override
	public String getIndexedColumn() {
		return ContactsContract.Contacts.DISPLAY_NAME;
	}

	@Override
	public Bitmap getContactPhoto(Cursor c, Context con, int toneType) {
		Uri uri = ContentUris.withAppendedId(
				ContactsContract.Contacts.CONTENT_URI, c.getLong(c.getColumnIndex(ContactsContract.Contacts._ID)));
		InputStream input = ContactsContract.Contacts
				.openContactPhotoInputStream(con.getContentResolver(), uri);
		if (input == null) {
			if(toneType == MultiTone.TYPE_RINGTONE){
				return BitmapFactory.decodeResource(con.getResources(), R.drawable.type_ringtone);
			} else if(toneType == MultiTone.TYPE_NOTIFICATION_TONE){
				return BitmapFactory.decodeResource(con.getResources(), R.drawable.type_notification);
			}
		}
		return BitmapFactory.decodeStream(input);
	}
	
	@Override
	public void removeCustomRingtoneFromContacts(Context ctx, String ringtoneUri) {
        ContentValues values = new ContentValues();
        
        values.putNull(Contacts.CUSTOM_RINGTONE);
        String clause = Contacts.CUSTOM_RINGTONE + "=?";
        ctx.getContentResolver().update(ContactsContract.Contacts.CONTENT_URI, values, clause, new String[]{ringtoneUri});
	}

	@Override
	public String getContactLookupKey(Context cntx,
			String phoneNumber) {
		//first lookup the user
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
		Cursor cursor = cntx.getContentResolver().query(uri, new String[]{ContactsContract.Contacts.LOOKUP_KEY}, null, null, null);
		String result = null;
		if(cursor.moveToFirst()){
			result =cursor.getString(0);
		}
		cursor.close();
		return result;
	}

	@Override
	public Collection<String> getContactLookupKeysForTone(Context ctx,String toneUri) {
		ArrayList<String> result = new ArrayList<String>();
		String clause = Contacts.CUSTOM_RINGTONE + "=?";
		Cursor cursor = ctx.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, new String[]{ContactsContract.Contacts.LOOKUP_KEY}, clause, new String[]{toneUri}, null);
		while(cursor.moveToNext()){
			result.add(cursor.getString(0));
		}
		return result;
	}

	@Override
	public boolean isRingToneAttachedToContact(Context ctx, String toneUri) {
		
		String clause = Contacts.CUSTOM_RINGTONE + "=?";
		Cursor cursor = ctx.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, new String[]{ContactsContract.Contacts.LOOKUP_KEY}, clause, new String[]{toneUri}, null);
		boolean result = cursor.moveToNext();
		return result;
	}

}
