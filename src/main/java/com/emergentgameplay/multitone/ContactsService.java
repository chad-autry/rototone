package com.emergentgameplay.multitone;

import java.util.Collection;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;

public abstract class ContactsService {

	private static ContactsService sInstance;

	public static ContactsService getInstance() {
        return new ContactsServiceApi();
    }
	
	public abstract Intent getContactPickerIntent();
	public abstract void updateContactCustomRingtone(Context ctx, Uri contactUri, String ringtoneUri);
	public abstract String retrieveCustomRingtone(Context cntx, String phoneNumber);
	public abstract String getContactLookupKey(Context cntx, String phoneNumber);
	public abstract Cursor getContactsCursor(Context cntx,String filter,Collection<String> filterCollection);
	public abstract String[] getDataBaseColumns();
	public abstract String getIndexedColumn();
	public abstract Uri getContactUri(long id);
	public abstract Bitmap getContactPhoto(Cursor c, Context con, int toneType);
	public abstract void removeCustomRingtoneFromContacts(Context ctx, String ringtoneUri);
	public abstract Collection<String> getContactLookupKeysForTone(Context ctx,String toneUri);
	public abstract boolean isRingToneAttachedToContact(Context ctx,String toneUri);
}
