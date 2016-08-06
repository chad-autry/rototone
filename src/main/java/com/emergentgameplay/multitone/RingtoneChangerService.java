package com.emergentgameplay.multitone;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
//import android.util.Log;

public class RingtoneChangerService extends Service {

	public static String INCOMING_PHONE_NUMBER = "incoming_phone_number";
	private MultiToneDbAdapter mDbHelper = null;
	private int latestStartId;

	@Override
	public IBinder onBind(Intent arg0) {
		// This service is not supposed to be bound to
		return null;
	}

	@Override
	public void onCreate() {
		mDbHelper = new MultiToneDbAdapter(this);
		mDbHelper.open();
		super.onCreate();

	}

	@Override
	public void onDestroy() {
		mDbHelper.close();
		super.onDestroy();
	}

	// I wish my application to be pre Android 5 compatible
	@Override
	public void onStart(Intent intent, int startId) {
		latestStartId=startId;
		// create a thread which does the tone changing work
		final String phoneNumber = intent.getExtras().getString(INCOMING_PHONE_NUMBER);
		final int id = startId;
		Thread thread = new Thread() {
			public void run() {
				// First sleep the thread for 3 seconds to make sure we don't
				// conflict with the ringtone being played.
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				changeTone(phoneNumber, id);
			}
		};
		thread.run();
		super.onStart(intent, startId);
	}

	private void changeTone(String phoneNumber, int startId) {
		//Step 2 Check if the user has a custom ringtone
		String ringingUriPath = null;

		//Get the URI if the ringtone, the rigamarole is for multiple version support
		ringingUriPath=(ContactsService.getInstance().retrieveCustomRingtone(this, phoneNumber));
		//ringingUriPath=CustomRingtoneRetriever.getInstance().retrieveCustomRingtone(this, phoneNumber);
		
		//If no custom ringtone for the user, check if the user belongs to a group with a custom ringtone set
		
		//If no custom ring tone get the default ringtone path
		if(ringingUriPath==null){
			//Need to use getActualDefaultRingtoneUri or it gives a static link string.
			ringingUriPath = RingtoneManager.getActualDefaultRingtoneUri(this,RingtoneManager.TYPE_RINGTONE).toString();
		}
		
		//Log.d("ToneChanger", "ringingUriPath ="+ringingUriPath);
		//Step 3 Determine if the ringing ringtone is a MultiTone
		long multiToneId = mDbHelper.getMultiToneId(ringingUriPath);
		
		//If it is a multitone then update the tone to play within the content provider next time
		if(multiToneId > -1){
			
			//Log.d("ToneChanger", "The path is a multitone");
			 
    		//Retrieve the next uri (and updates the db to record what is playing)
			String nextUri = mDbHelper.getNextTone(multiToneId);
			
			//Retrieve the tone information from the content provider
    		Uri ringToneUri = Uri.parse(nextUri);
    		String[] projection = new String[] {
    				MediaStore.MediaColumns.DATA,
    				MediaStore.MediaColumns.SIZE,
    				MediaStore.MediaColumns.MIME_TYPE,
    				MediaStore.Audio.Media.DURATION
                  };

    		Cursor cursor = getContentResolver().query(ringToneUri,
                 projection, // Which columns to return 
                 null,       // Which rows to return (all rows)
                 null,       // Selection arguments (none)
                 null);
    		if(cursor != null){
	    		if(cursor.moveToNext()){
		            ContentValues values = new ContentValues();
		            values.put(MediaStore.MediaColumns.DATA, cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA)));
		            values.put(MediaStore.MediaColumns.SIZE,  cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)));
		            values.put(MediaStore.MediaColumns.MIME_TYPE, cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)));
		            values.put(MediaStore.Audio.Media.DURATION,  cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));
		            Uri ringingUri = Uri.parse(ringingUriPath);
		           // Log.d("ToneChanger", "attempting to update the ringtone");
		            getContentResolver().update(ringingUri, values,null,null);
	    		}
	            cursor.close();
    		}
		}
		
		//If this is the final thread running in the service stop it
		if(startId>=latestStartId){
			stopSelfResult(startId);
		}
	}

}
