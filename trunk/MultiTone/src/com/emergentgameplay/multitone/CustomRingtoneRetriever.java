package com.emergentgameplay.multitone;

import android.content.Context;
import android.os.Build;

public abstract class CustomRingtoneRetriever {
	
	public abstract String retrieveCustomRingtone(Context cntx, String phoneNumber);
	
    private static CustomRingtoneRetriever sInstance;

    public static CustomRingtoneRetriever getInstance() {
        if (sInstance == null) {
            String className;
            int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
            if (sdkVersion < Build.VERSION_CODES.ECLAIR) {
                className = "OldRingtoneRetriever";
            } else {
                className = "NewRingtoneRetriever";
            }
            try {
                Class<? extends CustomRingtoneRetriever> clazz =
                        Class.forName(CustomRingtoneRetriever.class.getPackage() + "." + className)
                                .asSubclass(CustomRingtoneRetriever.class);
                sInstance = clazz.newInstance();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return sInstance;
    }

}
