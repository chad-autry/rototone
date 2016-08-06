package com.emergentgameplay.multitone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
//import android.util.Log;

/**
 * Simple notes database access helper class. Defines the basic CRUD operations
 * for the notepad example, and gives the ability to list all notes as well as
 * retrieve or modify a specific note.
 * 
 */
public class MultiToneDbAdapter {

	public static final String KEY_TABLE_MULTITONE = "multitone";
	public static final String KEY_TABLE_TONE = "tone";
	public static final String KEY_TABLE_CONTACT_TONE = "contact_tone";
	public static final String KEY_COLUMN_ID = "_id";
	public static final String KEY_COLUMN_TITLE = "title";
	public static final String KEY_COLUMN_SHUFFLE = "shuffle";
	public static final String KEY_COLUMN_URI = "uri";
	public static final String KEY_COLUMN_POSITION = "position";
	public static final String KEY_COLUMN_CURRENT_POSITION = "current_position";
	public static final String KEY_COLUMN_MULTITONE_ID = "multitone_id";
	public static final String KEY_COLUMN_MULTITONE_TYPE = "multitone_type";
	public static final String KEY_COLUMN_DEFAULT = "default_in";
	public static final String KEY_COLUMN_CONTACT_LOOKUP_KEY = "lookup_key";
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	/**
	 * MULTITONE table creation sql statement
	 */
	private static final String DATABASE_MULTITONE_CREATE = "create table multitone (_id integer primary key autoincrement, "
			+ "title text not null,uri text,shuffle text not null,current_position integer not null,multitone_type integer not null, default_in text not null);";

	/**
	 * TONE table creation sql statement
	 */
	private static final String DATABASE_TONE_CREATE = "create table tone (_id integer primary key autoincrement, "
			+ "title string not null,uri string not null, multitone_id integer not null, position integer not null);";
	
	/**
	 * Create SQL for the table where contact specific SMS notification tone entries are stored
	 */
	private static final String DATABASE_CONTACT_SELECTION_CREATE = "create table contact_tone (_id integer primary key autoincrement, "
		+ "multitone_id integer not null, lookup_key text not null);";
	private static final String DATABASE_NAME = "multitoneschema";
	private static final int DATABASE_VERSION = 3;

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			db.execSQL(DATABASE_MULTITONE_CREATE);
			db.execSQL(DATABASE_TONE_CREATE);
			db.execSQL(DATABASE_CONTACT_SELECTION_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			//Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
			//		+ newVersion + ", which will destroy all old data");
			if(oldVersion < 2){
				db.execSQL("alter table multitone add multitone_type integer default 0");
			}
			if(oldVersion < 3){
				db.execSQL("alter table multitone add default_in text not null default 'N'");
				db.execSQL(DATABASE_CONTACT_SELECTION_CREATE);
			}
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public MultiToneDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	/**
	 * Open the notes database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public MultiToneDbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	public MultiTone fetchMultiTone(long multiToneId){
		MultiTone multiTone = new MultiTone();
		multiTone.setId(multiToneId);
		
		Cursor cursor = mDb.query(KEY_TABLE_MULTITONE, new String[] {KEY_COLUMN_SHUFFLE,KEY_COLUMN_CURRENT_POSITION,KEY_COLUMN_TITLE,KEY_COLUMN_URI,KEY_COLUMN_MULTITONE_TYPE,KEY_COLUMN_DEFAULT}, "_id=?", new String[] { multiToneId+""}, null, null, null);
		cursor.moveToNext();
		multiTone.setCurrentTone(cursor.getInt(cursor.getColumnIndex(KEY_COLUMN_CURRENT_POSITION)));
		multiTone.setShuffle(cursor.getString(cursor.getColumnIndex(KEY_COLUMN_SHUFFLE)));
		multiTone.setUri(cursor.getString(cursor.getColumnIndex(KEY_COLUMN_URI)));
		multiTone.setTitle(cursor.getString(cursor.getColumnIndex(KEY_COLUMN_TITLE)));
		multiTone.setCurrentToneType(cursor.getInt(cursor.getColumnIndex(KEY_COLUMN_MULTITONE_TYPE)));
		multiTone.setDefault(cursor.getString(cursor.getColumnIndex(KEY_COLUMN_DEFAULT)).equalsIgnoreCase("Y"));
		cursor.close();

		cursor = mDb.query(KEY_TABLE_TONE, new String[] {KEY_COLUMN_ID,KEY_COLUMN_URI,KEY_COLUMN_POSITION,KEY_COLUMN_TITLE}, "multitone_id=?", new String[] { multiToneId+""}, null, null, KEY_COLUMN_POSITION);
		while(cursor.moveToNext()){
			Tone tone = new Tone();
			tone.setId(cursor.getLong(cursor.getColumnIndex(KEY_COLUMN_ID)));
			tone.setUri(cursor.getString(cursor.getColumnIndex(KEY_COLUMN_URI)));
			tone.setPosition(cursor.getInt(cursor.getColumnIndex(KEY_COLUMN_POSITION)));
			tone.setTitle(cursor.getString(cursor.getColumnIndex(KEY_COLUMN_TITLE)));
			multiTone.addTone(tone);
		}
		cursor.close();
		
		return multiTone;
	}
	
	/**
	 * Get the number of tones included in a multitone
	 */
	public int getToneCount(long multiToneId){
		String sql = "select count('x') from "+KEY_TABLE_TONE+" where multitone_id=?";
		Cursor cursor = mDb.rawQuery(sql, new String[] { multiToneId+""});
		int result = 1;
		if(cursor.moveToNext()){
			result=cursor.getInt(0);
		}
		cursor.close();
		return result;
	}
	
	/**
	 * Create a multitone witht the title provided defaults to current position. If the multitone is
	 * successfully created return the new rowId for that multitone, otherwise return
	 * a -1 to indicate failure.
	 * 
	 * @param title
	 *            the title of the note
	 * @return thingid or -1 if failed
	 */
	public long createMultitone(MultiTone multiTone) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_COLUMN_TITLE, multiTone.getTitle());
		initialValues.put(KEY_COLUMN_SHUFFLE, multiTone.getShuffle());
		initialValues.put(KEY_COLUMN_CURRENT_POSITION, "0");
		initialValues.put(KEY_COLUMN_DEFAULT, "N");
        switch(multiTone.getCurrentToneType()){
    	case MultiTone.TYPE_RINGTONE:
    		initialValues.put(KEY_COLUMN_URI, multiTone.getUri());
    		break;
    	case MultiTone.TYPE_NOTIFICATION_TONE:
    		initialValues.putNull(KEY_COLUMN_URI);
        }
		initialValues.put(KEY_COLUMN_MULTITONE_TYPE, multiTone.getCurrentToneType());
		long id = mDb.insert(KEY_TABLE_MULTITONE, null, initialValues);
		multiTone.setId(id);
		
		for(Tone tone:multiTone.getToneList()){
			tone.setMultiToneId(id);
			tone.setId(addTone(tone));
		}
		
		return id;
	}

	/**
	 * Return a cursor for the MainList ListActivity to use
	 * 
	 * @return Cursor over all notes
	 */
	public Cursor fetchAllMultitones() {

		
		return mDb.query(KEY_TABLE_MULTITONE, new String[] { KEY_COLUMN_ID, KEY_COLUMN_TITLE, KEY_COLUMN_MULTITONE_TYPE, KEY_COLUMN_URI, KEY_COLUMN_DEFAULT}, null, null, null, null, KEY_COLUMN_TITLE);
	}
	
	/**
	 * Return a Cursor over the list of all tones for the given multitone
	 * 
	 * @return Cursor over all notes
	 */
	public Cursor fetchTones(long multiToneId) {

		return mDb.query(KEY_TABLE_TONE, new String[] { KEY_COLUMN_ID, KEY_COLUMN_TITLE}, KEY_COLUMN_MULTITONE_ID+"=? and uri is not null", new String[] { multiToneId+""}, null, null, null);
	}
	
	/**
	 * Add a Tone to a multitone
	 * 
	 * @param multiToneId
	 * @param Uri
	 * @return
	 */
	public long addTone(Tone tone) {

		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_COLUMN_TITLE, tone.getTitle());
		initialValues.put(KEY_COLUMN_URI, tone.getUri());
		initialValues.put(KEY_COLUMN_POSITION, tone.getPosition()+"");
		initialValues.put(KEY_COLUMN_MULTITONE_ID, tone.getMultiToneId()+"");

		return mDb.insert(KEY_TABLE_TONE, null, initialValues);
	}
	
	/**
	 * Check wether the multitone has a URI or not
	 * @param multiToneId
	 * @return
	 */
	public boolean hasUri(long multiToneId){
		Cursor cursor = mDb.query(KEY_TABLE_MULTITONE, new String[] { KEY_COLUMN_ID}, KEY_COLUMN_MULTITONE_ID+"=? and uri is not null", new String[] { multiToneId+""}, null, null, null);
		boolean result = cursor.moveToNext();
		cursor.close();
		return result;
	}
	
	/**
	 * Set the uri path that a multiTone is found at
	 * @param multiToneId
	 * @param uri
	 * @return
	 */
	public int setUri(long multiToneId, String uri){
		ContentValues updateValues = new ContentValues();
		updateValues.put(KEY_COLUMN_URI, uri);
		return mDb.update(KEY_TABLE_MULTITONE, updateValues, "_id=?", new String[] { multiToneId+""});
	}
	
	/**
	 * Returns the id of the MultiTone at the given URI or -1 if it doesn't exist
	 * @param uriPath
	 * @return
	 */
	public long getMultiToneId(String uriPath){
		Cursor cursor = mDb.query(KEY_TABLE_MULTITONE, new String[] { KEY_COLUMN_ID}, "uri=?", new String[] { uriPath}, null, null, null);
		if(cursor.moveToNext()){
			long id = cursor.getLong(0);
			cursor.close();
			return id;
		}
		cursor.close();
		return -1;
	}
	
	/**
	 * Returns the id of the MultiTone with the given title, -1 if it doesn't exist
	 * @param uriPath
	 * @return
	 */
	public long getTitleExists(String title){
		Cursor cursor = mDb.query(KEY_TABLE_MULTITONE, new String[] { KEY_COLUMN_ID}, "title=?", new String[] { title}, null, null, null);
		if(cursor.moveToNext()){
			long id = cursor.getLong(0);
			cursor.close();
			return id;
		}
		cursor.close();
		return -1;
	}
	
	/**
	 * This method will return the next tone, and set the position into the database. Only used for ringtone lists
	 * @param multiToneId
	 * @return
	 */
	public String getNextTone(long multiToneId){
		MultiTone multiTone = new MultiTone();
		Cursor cursor = mDb.query(KEY_TABLE_MULTITONE, new String[] {KEY_COLUMN_SHUFFLE,KEY_COLUMN_CURRENT_POSITION}, "_id=?", new String[] { multiToneId+""}, null, null, null);
		cursor.moveToNext();
		multiTone.setCurrentTone(cursor.getInt(cursor.getColumnIndex(KEY_COLUMN_CURRENT_POSITION)));
		multiTone.setShuffle(cursor.getString(cursor.getColumnIndex(KEY_COLUMN_SHUFFLE)));
		cursor.close();

		cursor = mDb.query(KEY_TABLE_TONE, new String[] {KEY_COLUMN_URI,KEY_COLUMN_POSITION}, "multitone_id=?", new String[] { multiToneId+""}, null, null, null);
		while(cursor.moveToNext()){
			Tone tone = new Tone();
			tone.setUri(cursor.getString(cursor.getColumnIndex(KEY_COLUMN_URI)));
			tone.setPosition(cursor.getInt(cursor.getColumnIndex(KEY_COLUMN_POSITION)));
			multiTone.addTone(tone);
		}
		cursor.close();
		String result = multiTone.getNextToneUri();
		//Now update the current_position
		ContentValues updateValues = new ContentValues();
		updateValues.put(KEY_COLUMN_CURRENT_POSITION, multiTone.getCurrentTone()+"");
		mDb.update(KEY_TABLE_MULTITONE, updateValues, "_id=?", new String[] { multiToneId+""});
		return result;
	}
	
	public boolean updateTitle(MultiTone multiTone){
		ContentValues updateValues = new ContentValues();
		updateValues.put(KEY_COLUMN_TITLE, multiTone.getTitle());
		mDb.update(KEY_TABLE_MULTITONE, updateValues, "_id=?", new String[] { multiTone.getId()+""});
		return true;
	}
	
	public boolean updateShuffle(MultiTone multiTone){
		ContentValues updateValues = new ContentValues();
		updateValues.put(KEY_COLUMN_SHUFFLE, multiTone.getShuffle());
		mDb.update(KEY_TABLE_MULTITONE, updateValues, "_id=?", new String[] { multiTone.getId()+""});
		return true;
	}
	
	/**
	 * Delete a multitone from the database and remove it from the content resolver
	 * Content resolver work done here since this method will be shared across activities,
	 * the work is not limited to the DB
	 * @param ctx
	 * @param multiToneId
	 * @return
	 */
	public boolean deleteMultiTone(Context ctx, long multiToneId){
		//Get the URI
		Cursor cursor = mDb.query(KEY_TABLE_MULTITONE, new String[] {KEY_COLUMN_URI}, "_id=?", new String[] { multiToneId+""}, null, null, null);
		cursor.moveToNext();
		String uriString=cursor.getString(0);
		
		//Notification tones don't have a URI and aren't saved to users or in the content resolver
		if(uriString != null){
			Uri uri = Uri.parse(uriString);
			ContactsService.getInstance().removeCustomRingtoneFromContacts(ctx, uriString);
			//Log.d("MultiToneDbAdapter", "Deleting uri="+uriString);
			ctx.getContentResolver().delete(uri, null, null);
		}
		
		mDb.delete(KEY_TABLE_MULTITONE, "_id=?", new String[] { multiToneId+""});
		mDb.delete(KEY_TABLE_TONE, "multitone_id=?", new String[] { multiToneId+""});
		mDb.delete(KEY_TABLE_CONTACT_TONE, "multitone_id=?", new String[] { multiToneId+""});
		return true;
	}
	
	/**
	 * Resets the specified order of the tones to be that on the list
	 * @param multiTone
	 * @return
	 */
	public boolean reorderTones(MultiTone multiTone){
		Iterator<Tone> it = multiTone.getToneList().iterator();
		for(int i=0;it.hasNext();i++){
			Tone tone = it.next();
			ContentValues updateValues = new ContentValues();
			updateValues.put(KEY_COLUMN_POSITION, i);
			mDb.update(KEY_TABLE_TONE, updateValues, "_id=?", new String[] { tone.getId()+""});
		}
		ContentValues updateValues = new ContentValues();
		updateValues.put(KEY_COLUMN_CURRENT_POSITION, "0");
		//If we re-order reset the current MultiTone position. This is in case of deletion
		mDb.update(KEY_TABLE_MULTITONE, updateValues, "_id=?", new String[] { multiTone.getId()+""});
		return true;
	}
	
	/**
	 * Deletes the given tone from teh database
	 * @param multiTone
	 * @return
	 */
	public boolean deleteTone(Tone tone){
		mDb.delete(KEY_TABLE_TONE, "_id=?", new String[] { tone.getId()+""});
		return true;
	}
	

	public String getCustomNotificationToneSelection(String lookupKey){
		String sql = "select "+KEY_COLUMN_SHUFFLE+", "+KEY_COLUMN_CURRENT_POSITION+", mt."+KEY_COLUMN_ID+" from "+KEY_TABLE_MULTITONE+" mt, "+KEY_TABLE_CONTACT_TONE+" ct where ct.multitone_id = mt._id and ct.lookup_key=?";
		Cursor cursor = mDb.rawQuery(sql, new String[] { lookupKey});
		String result = null;
		if(cursor.moveToNext()){
			MultiTone multiTone = new MultiTone();
			multiTone.setId(cursor.getInt(cursor.getColumnIndex(KEY_COLUMN_ID)));
			multiTone.setCurrentTone(cursor.getInt(cursor.getColumnIndex(KEY_COLUMN_CURRENT_POSITION)));
			multiTone.setShuffle(cursor.getString(cursor.getColumnIndex(KEY_COLUMN_SHUFFLE)));
			cursor.close();
			
			Cursor inner = mDb.query(KEY_TABLE_TONE, new String[] {KEY_COLUMN_URI,KEY_COLUMN_POSITION}, "multitone_id=?", new String[] { multiTone.getId()+""}, null, null, null);
			while(inner.moveToNext()){
				Tone tone = new Tone();
				tone.setUri(inner.getString(inner.getColumnIndex(KEY_COLUMN_URI)));
				tone.setPosition(inner.getInt(inner.getColumnIndex(KEY_COLUMN_POSITION)));
				multiTone.addTone(tone);
			}
			inner.close();
			result = multiTone.getNextToneUri();
			//Now update the current_position
			ContentValues updateValues = new ContentValues();
			updateValues.put(KEY_COLUMN_CURRENT_POSITION, multiTone.getCurrentTone()+"");
			mDb.update(KEY_TABLE_MULTITONE, updateValues, "_id=?", new String[] { multiTone.getId()+""});
		}
		cursor.close();
		return result;
	}
	
	public String getCustomNotificationToneTitle(String lookupKey){
//		String sql = "select "+KEY_COLUMN_TITLE+" from "+KEY_TABLE_MULTITONE+" mt, "+KEY_TABLE_CONTACT_TONE+" ct where ct.multitone_id = mt._id and ct.lookup_key=?";
//		Cursor cursor = mDb.rawQuery(sql, new String[] { lookupKey});
//		String result = null;
//		if(cursor.moveToNext()){
//			result = cursor.getString(0);
//		}
//		cursor.close();
//		return result;
		
		Cursor cursor = mDb.query(KEY_TABLE_CONTACT_TONE, new String[] {KEY_COLUMN_MULTITONE_ID}, "lookup_key=?",  new String[] { lookupKey}, null, null, null);
		long id = -1;
		if(cursor.moveToNext()){
			id = cursor.getLong(0);
		}
		
		
		cursor.close();
		
		cursor = mDb.query(KEY_TABLE_MULTITONE, new String[] {KEY_COLUMN_TITLE}, "_id=?",  new String[] { id+""}, null, null, null);
		String result = null;
		if(cursor.moveToNext()){
			result = cursor.getString(0);
		}
		
		
		cursor.close();
		return result;
	}
	
	public String getDefaultNotificationToneSelection(){
		Cursor cursor = mDb.query(KEY_TABLE_MULTITONE, new String[] {KEY_COLUMN_ID,KEY_COLUMN_CURRENT_POSITION,KEY_COLUMN_SHUFFLE}, "default_in=?",  new String[] { "Y"}, null, null, null);
		String result = null;
		if(cursor.moveToNext()){
			MultiTone multiTone = new MultiTone();
			multiTone.setId(cursor.getInt(cursor.getColumnIndex(KEY_COLUMN_ID)));
			multiTone.setCurrentTone(cursor.getInt(cursor.getColumnIndex(KEY_COLUMN_CURRENT_POSITION)));
			multiTone.setShuffle(cursor.getString(cursor.getColumnIndex(KEY_COLUMN_SHUFFLE)));
			cursor.close();
			
			Cursor inner = mDb.query(KEY_TABLE_TONE, new String[] {KEY_COLUMN_URI,KEY_COLUMN_POSITION}, "multitone_id=?", new String[] { multiTone.getId()+""}, null, null, null);
			while(inner.moveToNext()){
				Tone tone = new Tone();
				tone.setUri(inner.getString(inner.getColumnIndex(KEY_COLUMN_URI)));
				tone.setPosition(inner.getInt(inner.getColumnIndex(KEY_COLUMN_POSITION)));
				multiTone.addTone(tone);
			}
			inner.close();
			result = multiTone.getNextToneUri();
			//Now update the current_position
			ContentValues updateValues = new ContentValues();
			updateValues.put(KEY_COLUMN_CURRENT_POSITION, multiTone.getCurrentTone()+"");
			mDb.update(KEY_TABLE_MULTITONE, updateValues, "_id=?", new String[] { multiTone.getId()+""});
		}
		cursor.close();
		return result;
	}
	
	public boolean setToneAsDefault(long multiToneId){

		//Update the old default as not
		ContentValues updateValues = new ContentValues();
		updateValues.put(KEY_COLUMN_DEFAULT, "N");
		mDb.update(KEY_TABLE_MULTITONE, updateValues, "default_in=?", new String[] { "Y"});

		//and update the new given tone as default
		updateValues = new ContentValues();
		updateValues.put(KEY_COLUMN_DEFAULT, "Y");
		mDb.update(KEY_TABLE_MULTITONE, updateValues, "_id=?", new String[] { multiToneId+""});
		return true;
	}
	
	public void setNotificationToneToUser(String lookupKey, long multitoneId){
		ContentValues updateValues = new ContentValues();
		updateValues.put(KEY_COLUMN_MULTITONE_ID, multitoneId);
		int updateCount = mDb.update(KEY_TABLE_CONTACT_TONE, updateValues, "lookup_key=?", new String[] { lookupKey});
		if(updateCount < 1){
			ContentValues initialValues = new ContentValues();

			initialValues.put(KEY_COLUMN_CONTACT_LOOKUP_KEY, lookupKey);
			initialValues.put(KEY_COLUMN_MULTITONE_ID, multitoneId+"");

			mDb.insert(KEY_TABLE_CONTACT_TONE, null, initialValues);
		}
	}
	
	//Delete a users custom tone selection
	public void deleteNotificationToneFromUser(String lookupKey){
		mDb.delete(KEY_TABLE_CONTACT_TONE, "lookup_key=?", new String[] { lookupKey});
	}
	
	//Get the user lookup keys based on the Notification Tone URI
	public Collection<String> getContactLookupKeys(long multitoneId){
		Cursor cursor = mDb.query(KEY_TABLE_CONTACT_TONE, new String[] {KEY_COLUMN_CONTACT_LOOKUP_KEY}, "multitone_id=?",  new String[] { multitoneId+""}, null, null, null);

		ArrayList<String> result = new ArrayList<String>();
		while(cursor.moveToNext()){
			result.add(cursor.getString(0));
		}
		cursor.close();
		return result;
	}
	
	//Check if the tone is attached to any contacts
	public boolean isNotificationToneAttachedToContact(long multiToneId){
		Cursor cursor = mDb.query(KEY_TABLE_CONTACT_TONE, new String[] {KEY_COLUMN_CONTACT_LOOKUP_KEY}, "multitone_id=?",  new String[] { multiToneId+""}, null, null, null);

		boolean result = cursor.moveToNext();
		cursor.close();
		return result;
	}
	
	//Remove default status from the given notification tone (ringtones can't be set as default within the app)
	public void removeFromDefault(long multitoneId){
		ContentValues updateValues = new ContentValues();
		updateValues.put(KEY_COLUMN_DEFAULT, "N");
		mDb.update(KEY_TABLE_MULTITONE, updateValues, "_id=?", new String[] { multitoneId+""});
	}
}
