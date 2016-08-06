package com.emergentgameplay.multitone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
//import android.util.Log;

public class NotificationtoneChanger extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		//This broadcast receiver will be called when an SMS message is received
		//Call a service to do the rest of the work
        Intent i = new Intent(arg0, NotificationtoneChangerService.class);
        i.putExtras(arg1.getExtras());
		arg0.startService(i);
	}
}
