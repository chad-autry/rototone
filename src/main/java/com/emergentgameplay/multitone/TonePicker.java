/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.emergentgameplay.multitone;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Customized Audio Picker from Ringdroid code
 */
public class TonePicker
    extends ListActivity
    implements TextWatcher
{
    private TextView mFilter;
    private SimpleCursorAdapter mAdapter;
    private Ringtone tone = null;//Use this to play the tones if requested
    private boolean showExternalMedia = true;
    private int toneType;
    
    private static final int DETAILS_ID = Menu.FIRST;
    private static final int PLAY_ID = Menu.FIRST + 1;

    // Result codes
    private static final int REQUEST_CODE_EDIT = 1;
    
    public TonePicker() {
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        //Check if external media can be shown
        checkExternalMedia();
        
        //If there is a saved state reinitialize from there
        if(icicle != null){ 
        	toneType = icicle.getInt(MultiTone.MULTITONE_TYPE,MultiTone.TYPE_UNDETERMINED);
        }
        else{//else get the intent and go from there
	        //Get the tone type to display
	        Bundle extras = getIntent().getExtras();
	        if (extras != null) {
	        	toneType = extras.getInt(MultiTone.MULTITONE_TYPE,MultiTone.TYPE_UNDETERMINED);
	
	        }
        }
        
        
        // Inflate our UI from its XML layout description.
        setContentView(R.layout.media_select);

        
		LinearLayout titleBar = (LinearLayout)findViewById(R.id.title_bar);
		titleBar.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,new int[]{Color.DKGRAY,Color.GRAY}));
		titleBar.setPadding(0, 5, 0, 0);
		
		Button cancelButton = (Button) findViewById(R.id.cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				Intent mIntent = new Intent();

				setResult(RESULT_CANCELED, mIntent);
				finish();
			}

		});
        
        try {
            mAdapter = new SimpleCursorAdapter(
                this,
                // Use a template that displays a text view
                R.layout.media_select_row,
                // Give the cursor to the list adapter
                createCursor(""),
                // Map from database columns...
                new String[] {
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media._ID},
                // To widget ids in the row layout...
                new int[] {
                    R.id.row_title,
                    R.id.row_icon});
            setListAdapter(mAdapter);

            // Normal click - open the editor
            getListView().setOnItemClickListener(new OnItemClickListener() {
                    public void onItemClick(AdapterView parent,
                                            View view,
                                            int position,
                                            long id) {
                    	returnSelectedMedia();
                    }
                });

        } catch (SecurityException e) {
            // No permission to retrieve audio?
            //Log.e("TonePicker", e.toString());

            // todo error 1
        } catch (IllegalArgumentException e) {
            // No permission to retrieve audio?
           // Log.e("TonePicker", e.toString());

            // todo error 2
        }

        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
                public boolean setViewValue(View view,
                                            Cursor cursor,
                                            int columnIndex) {
                	switch(view.getId()){
                	case  R.id.row_icon:
                        setSoundIconFromCursor((ImageView) view, cursor);
                        return true;
                	}
                    return false;
                }
            });

        // Long-press opens a context menu
        registerForContextMenu(getListView());

        mFilter = (TextView) findViewById(R.id.search_filter);
        if (mFilter != null) {
            mFilter.addTextChangedListener(this);
        }
    }

    private void setSoundIconFromCursor(ImageView view, Cursor cursor) {
        if (0 != cursor.getInt(cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_RINGTONE))) {
            view.setImageResource(R.drawable.type_ringtone);
            ((View) view.getParent()).setBackgroundColor(
                getResources().getColor(R.drawable.type_bkgnd_ringtone));
        } else if (0 != cursor.getInt(cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_ALARM))) {
            view.setImageResource(R.drawable.type_alarm);
            ((View) view.getParent()).setBackgroundColor(
                getResources().getColor(R.drawable.type_bkgnd_alarm));
        } else if (0 != cursor.getInt(cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_NOTIFICATION))) {
            view.setImageResource(R.drawable.type_notification);
            ((View) view.getParent()).setBackgroundColor(
                getResources().getColor(R.drawable.type_bkgnd_notification));
        } else if (0 != cursor.getInt(cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_MUSIC))) {
            view.setImageResource(R.drawable.type_music);
            ((View) view.getParent()).setBackgroundColor(
                getResources().getColor(R.drawable.type_bkgnd_music));
        }
    }

    /** Called with an Activity we started with an Intent returns. */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent dataIntent) {
        if (requestCode != REQUEST_CODE_EDIT) {
            return;
        }

        if (resultCode != RESULT_OK) {
            return;
        }

        setResult(RESULT_OK, dataIntent);
        finish();
    }

    private void showFinalAlert(CharSequence message) {
        new AlertDialog.Builder(TonePicker.this)
            .setTitle(getResources().getText(R.string.alert_title_failure))
            .setMessage(message)
            .setPositiveButton(
                R.string.alert_ok_button,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                    	dialog.dismiss();
                    }
                })
            .setCancelable(false)
            .show();
    }
    
    private void showSDUnavailableAlert(CharSequence message) {
        new AlertDialog.Builder(TonePicker.this)
            .setTitle(getResources().getText(R.string.alert_title_warning))
            .setMessage(message)
            .setPositiveButton(
                R.string.alert_ok_button,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                    	dialog.dismiss();
                    }
                })
            .setCancelable(false)
            .show();
    }

    private void returnSelectedMedia() {
//        Cursor c = mAdapter.getCursor();
//        int dataIndex = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
//        String filename = c.getString(dataIndex);
//        try {
//            Intent intent = new Intent(Intent.ACTION_EDIT,
//                                       Uri.parse(filename));
//            intent.putExtra("was_get_content_intent",
//                            mWasGetContentIntent);
//            intent.setClassName(
//                "com.ringdroid",
//                "com.ringdroid.RingdroidEditActivity");
//            startActivityForResult(intent, REQUEST_CODE_EDIT);
//        } catch (Exception e) {
//            Log.e("Ringdroid", "Couldn't start editor");
//        }
    	Cursor c = mAdapter.getCursor();
		int uriIndex = c.getColumnIndex("\""
				+ MediaStore.Audio.Media.INTERNAL_CONTENT_URI + "\"");
		if (uriIndex == -1) {
			uriIndex = c.getColumnIndex("\""
					+ MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "\"");
		}
		if (uriIndex == -1) {
			showFinalAlert(getResources().getText(R.string.select_failed));
			return;
		}
		
		String itemUri = c.getString(uriIndex)
				+ "/"
				+ c.getString(c
						.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
		
		Bundle bundle = new Bundle();
		
		//If this is the first tone to be picked for a list, return what type the tone is
		if(toneType == MultiTone.TYPE_UNDETERMINED){
			int isRingtone = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_RINGTONE));
			if(isRingtone != 0){
				bundle.putInt(MultiTone.MULTITONE_TYPE, MultiTone.TYPE_RINGTONE);
			}
			else{
				bundle.putInt(MultiTone.MULTITONE_TYPE, MultiTone.TYPE_NOTIFICATION_TONE);
			}
			
		}
		bundle.putParcelable(RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
				Uri.parse(itemUri));

		
		
		Intent mIntent = new Intent();
		mIntent.putExtras(bundle);
		setResult(RESULT_OK, mIntent);
		finish();
    }

    private Cursor getInternalAudioCursor(String selection,
                                          String[] selectionArgs) {
        return managedQuery(
            MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
            INTERNAL_COLUMNS,
            selection,
            selectionArgs,
            MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    private Cursor getExternalAudioCursor(String selection,
                                          String[] selectionArgs) {
        return managedQuery(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            EXTERNAL_COLUMNS,
            selection,
            selectionArgs,
            MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    Cursor createCursor(String filter) {
        ArrayList<String> args = new ArrayList<String>();
        String selection;

      //  if (mShowAll) {
            selection = "(_DATA LIKE ? and ALBUM != 'Rototone' and ALBUM != 'RingtonePlaylists' ";
            switch(toneType){
            case MultiTone.TYPE_RINGTONE:
            	selection += "and IS_RINGTONE != 0";
            	break;
            case MultiTone.TYPE_NOTIFICATION_TONE:
            	selection += "and IS_NOTIFICATION != 0";
            	break;
            case MultiTone.TYPE_UNDETERMINED:
            	selection += "and (IS_NOTIFICATION != 0 or IS_RINGTONE != 0)";
            }
            	
            selection += ")";
            args.add("%");
//        } else {
//            selection = "(";
//            for (String extension : CheapSoundFile.getSupportedExtensions()) {
//                args.add("%." + extension);
//                if (selection.length() > 1) {
//                    selection += " OR ";
//                }
//                selection += "(_DATA LIKE ?)";
//            }
//            selection += ")";
//
//            selection = "(" + selection + ") AND (_DATA NOT LIKE ?)";
//            args.add("%espeak-data/scratch%");
//        }

        if (filter != null && filter.length() > 0) {
            filter = "%" + filter + "%";
            selection =
                "(" + selection + " AND " +
                "((TITLE LIKE ?) OR (ARTIST LIKE ?) OR (ALBUM LIKE ?)))";
            args.add(filter);
            args.add(filter);
            args.add(filter);
        }
        String[] argsArray = args.toArray(new String[args.size()]);

        Cursor c = null;
        if(showExternalMedia){ //If we can show external media then show it with the internal
        	c = new MergeCursor(new Cursor[] {
                    getExternalAudioCursor(selection, argsArray),
                    getInternalAudioCursor(selection, argsArray)});
        }else{ //else only show internal media
            c= getInternalAudioCursor(selection, argsArray);
        }
        startManagingCursor(c);
        return c;
    }

    public void beforeTextChanged(CharSequence s, int start,
                                  int count, int after) {
    }

    public void onTextChanged(CharSequence s,
                              int start, int before, int count) {
    }

    public void afterTextChanged(Editable s) {
        refreshListView();
    }

    private void refreshListView() {
    	//Always check if the drive has been mounted when refreshing
    	checkExternalMedia();
        String filterStr = mFilter.getText().toString();
        mAdapter.changeCursor(createCursor(filterStr));
    }
    
    /**
     * Provide a context menu for Play and Details options
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
//        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
//        Cursor c =  (Cursor)getListAdapter().getItem(info.position);
//        int currentToneType = c.getInt(c.getColumnIndex(MultiToneDbAdapter.KEY_COLUMN_MULTITONE_TYPE));
        menu.add(0, DETAILS_ID, 0, R.string.menu_details);
        menu.add(0, PLAY_ID, 0, R.string.menu_play);
     

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	Cursor c = (Cursor)mAdapter.getItem(info.position);
        switch(item.getItemId()) {
            case DETAILS_ID:
            	LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
            	View view = inflater.inflate(R.layout.tone_details, (ViewGroup)findViewById(R.id.details_root));
            	TextView text = (TextView) view.findViewById(R.id.artist);
            	text.setText("Artist: "+c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
            	text = (TextView) view.findViewById(R.id.album);
            	text.setText("Album: "+c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
            	
            	new AlertDialog.Builder(TonePicker.this)
                .setTitle(c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE)))
                .setView(view)
                .setPositiveButton(
                    R.string.alert_ok_button,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                        	dialog.dismiss();
                        }
                    })
                .setCancelable(false)
                .show();

                return true;
            case PLAY_ID:
            	if(tone != null && tone.isPlaying()){
            		tone.stop();
            	}
        		int uriIndex = c.getColumnIndex("\""
        				+ MediaStore.Audio.Media.INTERNAL_CONTENT_URI + "\"");
        		if (uriIndex == -1) {
        			uriIndex = c.getColumnIndex("\""
        					+ MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "\"");
        		}
        		if (uriIndex == -1) {
        			showFinalAlert(getResources().getText(R.string.select_failed));
        			return true;
        		}
        		
        		String itemUri = c.getString(uriIndex)
        				+ "/"
        				+ c.getString(c
        						.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
        		tone = RingtoneManager.getRingtone(this, Uri.parse(itemUri));
        		tone.setStreamType(AudioManager.STREAM_SYSTEM);
        		tone.play();
                return true;
        }
        return super.onContextItemSelected(item);
    }
    
    
    /*
     * Media may have been added or removed, refresh the list.
     * (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
	protected void onResume() {
		super.onResume();
		refreshListView();
	}
    
    

    @Override
	protected void onStop() {
    	if(tone != null && tone.isPlaying()){
    		tone.stop();
    	}
		super.onStop();
	}

	@Override
    //if interupted save the screen's state
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(MultiTone.MULTITONE_TYPE,toneType);
    }

	private static final String[] INTERNAL_COLUMNS = new String[] {
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.IS_RINGTONE,
        MediaStore.Audio.Media.IS_ALARM,
        MediaStore.Audio.Media.IS_NOTIFICATION,
        MediaStore.Audio.Media.IS_MUSIC,
        "\"" + MediaStore.Audio.Media.INTERNAL_CONTENT_URI + "\""
    };

    private static final String[] EXTERNAL_COLUMNS = new String[] {
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.IS_RINGTONE,
        MediaStore.Audio.Media.IS_ALARM,
        MediaStore.Audio.Media.IS_NOTIFICATION,
        MediaStore.Audio.Media.IS_MUSIC,
        "\"" + MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "\""
    };
    
    private void checkExternalMedia(){
    	//If we've already failed this check once don't re-display the message, just return
    	if(!showExternalMedia){
    		return;
    	}
        String status = Environment.getExternalStorageState();
        //Check if the media is shared to a pc
        if (status.equals(Environment.MEDIA_SHARED)) {
        	showSDUnavailableAlert(getResources().getText(R.string.sdcard_shared));
            showExternalMedia=false;
        }
        //else check if the media is not mounted (mounted read only is allowed
        else if (!status.equals(Environment.MEDIA_MOUNTED_READ_ONLY) && !status.equals(Environment.MEDIA_MOUNTED)) {
        	showSDUnavailableAlert(getResources().getText(R.string.no_sdcard));
            showExternalMedia=false;
        }
    }
}
