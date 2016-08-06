package com.emergentgameplay.multitone;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.telephony.SmsMessage;
//import android.util.Log;

public class NotificationtoneChangerService extends Service {

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
		//Set a variable to know if the service can be shut down once done with the method
		latestStartId=startId;
		
		//Get the SMS message and determine the address
		Bundle bundle = intent.getExtras();        
        SmsMessage firstMsgPortion = null;        
        if (bundle != null)
        {
        	String ringingUriPath = null;
        	
        	
            //Get the SMS message and extract the address
            Object[] pdus = (Object[]) bundle.get("pdus");       
            firstMsgPortion = SmsMessage.createFromPdu((byte[])pdus[0]);  
            String address = firstMsgPortion.getOriginatingAddress();
            
            //TODO Email based SMS
            
            //Look up if the SMS came from a contact
            String lookupKey = null;
            lookupKey = ContactsService.getInstance().getContactLookupKey(this, address);
            
            
            //lookup if that contact has a custom tone selected (retrieves the next tone to play)
            if(lookupKey != null){
            	ringingUriPath=mDbHelper.getCustomNotificationToneSelection(lookupKey);
            }
            
            //Else check if there is a default notification tone list (retrieves the next tone to play)
            if(ringingUriPath == null){
            	ringingUriPath=mDbHelper.getDefaultNotificationToneSelection();
            }
        	
            
            //If we have got a notification tone, then play the tone
            if(ringingUriPath != null){
            	
            	//play the tone
        		Ringtone notificationTone = RingtoneManager.getRingtone(this, Uri.parse(ringingUriPath));
        		if(notificationTone == null){
        			//If the notification tone is null it is because the tone has been deleted, or because the sdcard is mounted
        			//TODO send some sort of alert if the tone is missing?
//                	NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//                    Notification notification = new Notification(R.drawable.icon,"Temp Notification",System.currentTimeMillis());
//                    CharSequence contentTitle = "Temp Notification";
//                    CharSequence contentText = "This notification should be automatically cleared immediately and only played because a tone was missing";
//                    Intent notificationIntent = new Intent(this, MainList.class);
//                    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
//
//                    notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
//                    //set the default sound
//                    notification.defaults |= Notification.DEFAULT_SOUND;
//                    manager.notify(1,notification);   
//                    
//                    manager.cancelAll();
        		}
        		else{
	        		notificationTone.setStreamType(AudioManager.STREAM_NOTIFICATION);
	        		notificationTone.play();
        		}
            }
 
        }
        else{
        	NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification notification = new Notification(R.drawable.icon,"Bundle Null",System.currentTimeMillis());
            CharSequence contentTitle = "Bundle Null";
            CharSequence contentText = "Bundle Null";
            Intent notificationIntent = new Intent(this, MainList.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
            manager.notify(1,notification);   
        }
		super.onStart(intent, startId);
		if(startId>=latestStartId){
			stopSelfResult(startId);
		}
	}
}
