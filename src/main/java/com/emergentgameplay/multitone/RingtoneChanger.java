package com.emergentgameplay.multitone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
//import android.util.Log;

public class RingtoneChanger extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		//Log.d("ToneChanger", "onRecieve, EXTRA_STATE ="+arg1.getStringExtra(  TelephonyManager.EXTRA_STATE));
		//We are only interested in activating if the phone is ringing
		if(arg1.getStringExtra(  TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING))
		{

			//Step 1 get the phone number calling (the phone is ringing so we have access)
			String phoneNumber = arg1.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
			
			//Call a service to do the rest of the work
	        Intent i = new Intent(arg0, RingtoneChangerService.class);
	        i.putExtra(RingtoneChangerService.INCOMING_PHONE_NUMBER, phoneNumber);
			arg0.startService(i);
		}	
	}
}
