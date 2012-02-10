package com.emergentgameplay.multitone;

import java.util.Collection;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.People;

/**
 * Class for accessing and modify contacts pre-Eclair
 * @author Chad Autry
 *
 */
public class ContactsSerivceOldApi extends ContactsService{

	@Override
	public Intent getContactPickerIntent() {
		return new Intent(Intent.ACTION_PICK,
                People.CONTENT_URI);
	}

	@Override
	public String retrieveCustomRingtone(Context cntx, String phoneNumber) {
		Cursor cursor = cntx.getContentResolver().query(Contacts.Phones.CONTENT_URI, new String[]{Contacts.Phones.CUSTOM_RINGTONE}, Contacts.Phones.NUMBER+"=?",  new String[]{phoneNumber}, null);
		String result = null;
		if(cursor.moveToFirst()){
			result =cursor.getString(0);
		}
		else{
			return null;
		}
		cursor.close();
		return result;
	}

	@Override
	public void updateContactCustomRingtone(Context ctx, Uri contactUri, String ringtoneUri) {
        ContentValues values = new ContentValues();
        values.put(Contacts.Phones.CUSTOM_RINGTONE, ringtoneUri);
        ctx.getContentResolver().update(contactUri, values, null, null);
		
	}

	@Override
	public Cursor getContactsCursor(Context cntx, String filter,Collection<String> filterCollection) {
		String selection = Contacts.ContactMethods.KIND + " = " + Contacts.KIND_PHONE;
//		if(filter != null && !filter.equals("")){
//			selection = selection +" and "+ContactsContract.Contacts.DISPLAY_NAME+" LIKE ?"; 
//		}
		Cursor cursor = cntx.getContentResolver().query(Contacts.CONTENT_URI, new String[]{ Contacts.People._ID,Contacts.People.DISPLAY_NAME, Contacts.Phones.CUSTOM_RINGTONE}, null, null, Contacts.People.DISPLAY_NAME);
		return cursor;
	}

	@Override
	public String[] getDataBaseColumns() {
		return new String[]{Contacts.People.DISPLAY_NAME, Contacts.Phones.CUSTOM_RINGTONE};
	}

	@Override
	public Uri getContactUri(long id) {
		String contactUri = Contacts.CONTENT_URI + "/"+id;
		return Uri.parse(contactUri);
	}

	@Override
	public String getIndexedColumn() {
		return Contacts.People.DISPLAY_NAME;
	}

	@Override
	public Bitmap getContactPhoto(Cursor c, Context con, int toneType) {
		
		Uri uri = ContentUris.withAppendedId(People.CONTENT_URI, c.getLong(c.getColumnIndex(Contacts.People._ID)));
		Bitmap bitmap = People.loadContactPhoto(con, uri, R.drawable.type_ringtone, null);
		return bitmap;
	}

	@Override
	public void removeCustomRingtoneFromContacts(Context ctx, String ringtoneUri) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getContactLookupKey(Context cntx,
			String phoneNumber) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getContactLookupKeysForTone(Context ctx,String toneUri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isRingToneAttachedToContact(Context ctx,String toneUri) {
		// TODO Auto-generated method stub
		return false;
	}

}
