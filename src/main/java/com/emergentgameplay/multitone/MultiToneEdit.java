package com.emergentgameplay.multitone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class MultiToneEdit extends ListActivity{
	
	private MultiToneDbAdapter mDbHelper;
	private static final int ADD_TONE_ID = Menu.FIRST;
	private static final int ADD_TO_USER_ID = Menu.FIRST+2;
	private static final int MAKE_DEFAULT_ID = Menu.FIRST+3;
	private static final int REMOVE_FROM_USER = Menu.FIRST + 4;
	private static final int REMOVE_FROM_DEFAULT = Menu.FIRST + 5;
	private static final int ACTIVITY_ADD_TONE = Menu.FIRST+1;
	private static final int CONTACT_PICKER_RESULT = Menu.FIRST+2;
	private static final int CONTACT_PICKER_REMOVE_RESULT=Menu.FIRST+3; 
	private static final int MOVE_ID = Menu.FIRST;
	private static final int DELETE_ID = Menu.FIRST+1;
    private static final int DETAILS_ID = Menu.FIRST+2;
    private static final int PLAY_ID = Menu.FIRST + 3;
	private static final int DISABLEABLE_OPTIONS_GROUP_ID = 1;
	private TextView mTitleText;
	private MultiTone mMultiTone;
	private static final int ACTIVITY_EDIT_TITLE = 1;
	private String SAVED_STATE_MULTITONEID="multiToneId";
	private String SAVED_STATE_MULTITONE="multiTone";
	private boolean initialSavePassed = false;
	private int mMovePosition =-1;//If this is -1 we aren't moving anything. Else it is the id of the element which is moving.
	private Ringtone tone = null;//Use this to play the tones if requested
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tone_list);

        //Create our database helper

        mDbHelper = new MultiToneDbAdapter(this);
        mDbHelper.open();
        
        //Setup the title bar
		RelativeLayout baseBar = (RelativeLayout)findViewById(R.id.bottom_bar);
		baseBar.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,new int[]{Color.DKGRAY,Color.GRAY}));
		baseBar.setPadding(0, 5, 0, 0);
        
        //Setup the title bar
		RelativeLayout titleBar = (RelativeLayout)findViewById(R.id.title_bar);
		titleBar.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,new int[]{Color.DKGRAY,Color.GRAY}));
		titleBar.setPadding(0, 5, 0, 0);

		Button confirmButton = (Button) findViewById(R.id.add);
		confirmButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				addTone();
			}

		});

		Button cancelButton = (Button) findViewById(R.id.done);
		cancelButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				finish();
			}

		});
		
        Long multiToneId = null;
        


        //If there is a saved state reinitialize from there
        if(savedInstanceState != null){ 
        	multiToneId = (Long) savedInstanceState.getSerializable(SAVED_STATE_MULTITONEID);
        	if(multiToneId==null){
        		initialSavePassed=false;
        		mMultiTone=(MultiTone) savedInstanceState.getSerializable(SAVED_STATE_MULTITONE);
        	}
        	else{
        		initialSavePassed=true;
        		mMultiTone=mDbHelper.fetchMultiTone(multiToneId);
        	}
        }
        else{//else get the intent and go from there
	        //Initialize the MultiTone using the Id passed (may be a new multiTone)
	        Bundle extras = getIntent().getExtras();
	        if (multiToneId==null && extras != null) {
	        	multiToneId = extras.getLong(MultiToneDbAdapter.KEY_COLUMN_ID);
	            //Log.d("MultiToneEdit", "Edit called with id"+multiToneId);
	
	        }
	        if(multiToneId<0){
	        	initialSavePassed=false; //The id was set to -1 if create was requested
	            mMultiTone = new MultiTone(); //create a new multiTone, we won't actually save till it has a title and a tone
	        }
	        else{
	        	initialSavePassed=true;
	        	//Retrieve the multitone from the database
	        	mMultiTone=mDbHelper.fetchMultiTone(multiToneId);
	        }
        }
        //Set up the titleText display and click listener
        mTitleText = (TextView) findViewById(R.id.title);

        if(initialSavePassed){ //Set the title if editing
        	mTitleText.setTextColor(Color.BLACK);
        	 mTitleText.setText(mMultiTone.getTitle());
        }
        else{//Or a temp display if creating
        	mTitleText.setTextColor(Color.LTGRAY);
       	 mTitleText.setText("Title");
        }

        mTitleText.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
		        Intent i = new Intent(view.getContext(), TitleEdit.class);
		        if(mMultiTone.getTitle()!=null){
		        	i.putExtra(MultiToneDbAdapter.KEY_COLUMN_TITLE, mMultiTone.getTitle());
		        }
		        if(initialSavePassed){//don't put an id on the intent unless we actually have one from saving
		        	i.putExtra(MultiToneDbAdapter.KEY_COLUMN_ID, mMultiTone.getId());
		        }
		        startActivityForResult(i, ACTIVITY_EDIT_TITLE);
			}
		});
        
        //Set the shuffle checkbox and the shuffleClickListener
        CheckBox shuffleBox = (CheckBox)findViewById(R.id.shuffle_cb);
        //Log.d("MultiToneEdit", "Setting shuffleBox to "+mMultiTone.isShuffle());
        shuffleBox.setChecked(mMultiTone.isShuffle());
        shuffleBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				//Log.d("MultiToneEdit", "Checked changed, setting db to "+arg1);
				mMultiTone.setShuffle(arg1);
				if(initialSavePassed){
					mDbHelper.updateShuffle(mMultiTone);
				}
			}
		});
        registerForContextMenu(getListView());
        //lastly fill the data of tones
        fillData();
    }

    private void fillData() {
    	if(mMultiTone.getToneList()==null){
    		ArrayAdapter<Tone> multiTones = new ArrayAdapter<Tone>(this, R.layout.media_select_row, new Tone[]{});
            setListAdapter(multiTones);
    	}
    	else{
    		           
    		//Get all the tones from the DB
			String[] from = new String[] { "title" ,"image_icon"};
			int[] to = new int[] { R.id.row_title, R.id.row_icon};
			List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
			HashMap<String, String> rowMap;
			for (Tone tone : mMultiTone.getToneList()) {
				rowMap = new HashMap<String, String>();
				rowMap.put("title", tone.getTitle());
				fillMaps.add(rowMap);
			}

			SimpleAdapter adapter = new SimpleAdapter(this, fillMaps,
					R.layout.media_select_row, from, to);
			
			//Handle setting the icon depending on if it is a list of notification tones or ringtones
			adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
				public boolean setViewValue(View view, Object arg1, String arg2) {
					switch(view.getId()){
					case R.id.row_icon:
	                    switch(mMultiTone.getCurrentToneType()){
	                	case MultiTone.TYPE_RINGTONE:
	                		((ImageView)view).setImageResource(R.drawable.type_ringtone);
	        	        	break;
	                	case MultiTone.TYPE_NOTIFICATION_TONE:
	                		((ImageView)view).setImageResource(R.drawable.type_notification);
	                		break;
	                    }
						return true;
					}
					return false;
				}
			});
	       setListAdapter(adapter);
    	}

    }  
    

    @Override //use onprepare options menu because it is desired to determine each time what the menu options are
	public boolean onPrepareOptionsMenu(Menu menu) {
    	menu.clear();
		boolean result = super.onPrepareOptionsMenu(menu);
        menu.add(0, ADD_TONE_ID, 0, R.string.menu_add_tone).setIcon(android.R.drawable.ic_menu_add);
        
        menu.add(DISABLEABLE_OPTIONS_GROUP_ID, ADD_TO_USER_ID, 0, R.string.menu_assign_to_contact).setIcon(R.drawable.ic_add_to_user);
        
        if(initialSavePassed){
        	
        	boolean isDefault = false;
        	switch(mMultiTone.getCurrentToneType()){
        	case MultiTone.TYPE_RINGTONE:
        		String defaultUriPath = RingtoneManager.getActualDefaultRingtoneUri(this,RingtoneManager.TYPE_RINGTONE).toString();
        		String multittoneUriPath = mMultiTone.getUri();
        		isDefault = (defaultUriPath != null && defaultUriPath.equalsIgnoreCase(multittoneUriPath));
            	break;
        	case MultiTone.TYPE_NOTIFICATION_TONE:
        		isDefault=mMultiTone.isDefault();
        		break;
            }
        	if(isDefault && mMultiTone.getCurrentToneType() == MultiTone.TYPE_NOTIFICATION_TONE){
        		//Can only blindly remove notification tones from being default. A ringtone needs something to replace it
        		menu.add(0, REMOVE_FROM_DEFAULT, 0, R.string.menu_remove_default).setIcon(R.drawable.ic_remove_default);
        	}
        	else if(!isDefault){
        		menu.add(0, MAKE_DEFAULT_ID, 0, R.string.menu_make_default).setIcon(R.drawable.ic_make_default);
        	}
        	
        	boolean attachedToContacts = true;
            switch(mMultiTone.getCurrentToneType()){
        	case MultiTone.TYPE_RINGTONE:
        		attachedToContacts = ContactsService.getInstance().isRingToneAttachedToContact(this,mMultiTone.getUri());
            	break;
        	case MultiTone.TYPE_NOTIFICATION_TONE:
        		attachedToContacts = mDbHelper.isNotificationToneAttachedToContact(mMultiTone.getId());
            }
        	if(attachedToContacts){
        		menu.add(DISABLEABLE_OPTIONS_GROUP_ID, REMOVE_FROM_USER, 0, R.string.menu_remove_from_contact).setIcon(R.drawable.ic_remove_from_user);
        	}
        }

	    if(!initialSavePassed){
	    	menu.setGroupEnabled(DISABLEABLE_OPTIONS_GROUP_ID, false);
        }
        return result;
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
        case ADD_TONE_ID:
        	addTone();
            return true;
        case ADD_TO_USER_ID:
            Intent contactPickerIntent = new Intent(this, ContactPicker.class);
            contactPickerIntent.putExtra(MultiTone.MULTITONE_TYPE, mMultiTone.getCurrentToneType());
            startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
            return true;
        case MAKE_DEFAULT_ID:
            switch( mMultiTone.getCurrentToneType()){
        	case MultiTone.TYPE_RINGTONE:
            	RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE,Uri.parse( mMultiTone.getUri()));
	        	break;
        	case MultiTone.TYPE_NOTIFICATION_TONE:
        		mDbHelper.setToneAsDefault( mMultiTone.getId());
        		mMultiTone.setDefault(true);
            }
        	return true;
        	
        case REMOVE_FROM_USER:
            contactPickerIntent = new Intent(this, ContactPicker.class);
            contactPickerIntent.putExtra(MultiTone.MULTITONE_TYPE, mMultiTone.getCurrentToneType());
            contactPickerIntent.putExtra(MultiToneDbAdapter.KEY_COLUMN_ID, mMultiTone.getId());
            if( mMultiTone.getUri() == null){
            	contactPickerIntent.putExtra(MultiToneDbAdapter.KEY_COLUMN_URI, "wonder twin powers activate!");
            }
            else{
            	contactPickerIntent.putExtra(MultiToneDbAdapter.KEY_COLUMN_URI, mMultiTone.getUri());
            }
            startActivityForResult(contactPickerIntent, CONTACT_PICKER_REMOVE_RESULT);
            return true;
        case REMOVE_FROM_DEFAULT:
        	mDbHelper.removeFromDefault(mMultiTone.getId());
        	mMultiTone.setDefault(false);
        	fillData();
        	return true;
        }
    	
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        
        if(resultCode==RESULT_OK){
	        //getting a null pointer exception off of the intent when cancel is hit from the tonepicker
	        if(intent == null){
	        	return;
	        }
	        Bundle extras = intent.getExtras();
	        switch(requestCode) {
	            case ACTIVITY_ADD_TONE:
	            	if(resultCode == RESULT_CANCELED){
	            		break;
	            	}
	            	Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
	            	if (uri != null) {
	            		String ringTonePath = uri.toString();
	
	            		//Retrieve the values to copy from the ringtone
	            		Uri ringToneUri = Uri.parse(ringTonePath);
	            		String[] projection = new String[] {
	            				MediaStore.MediaColumns.TITLE
	                          };
	
	            		Cursor managedCursor = managedQuery(ringToneUri,
	                         projection, // Which columns to return 
	                         null,       // Which rows to return (all rows)
	                         null,       // Selection arguments (none)
	                         null);
	            		managedCursor.moveToNext();
	            		
	            		Tone tone = new Tone();
	            		tone.setUri(ringTonePath);
	            		tone.setTitle(managedCursor.getString(managedCursor.getColumnIndex(MediaStore.MediaColumns.TITLE)));
	            		//multiToneId and position are set onto the tone when added to the MultiTone
	            		mMultiTone.addTone(tone);
	            		
	            		if(initialSavePassed){//Either editing add it to the DB
	            			tone.setId(mDbHelper.addTone(tone));
	            		}
	            		else{//or editing see if we can now create
	            			
	            			//If we haven't created this multitone yet retrieve teh type from the tone picker
	            			mMultiTone.setCurrentToneType(intent.getIntExtra(MultiTone.MULTITONE_TYPE, mMultiTone.getCurrentToneType()));

	            			createMultiTone();
	            		}
	            	}
	                fillData();
	                break;
	                
	            case ACTIVITY_EDIT_TITLE:
	            	if(resultCode == RESULT_CANCELED){
	            		break;
	            	}
	            	else{
	            		if (extras != null) {
	                        String title = extras.getString(MultiToneDbAdapter.KEY_COLUMN_TITLE);
	                        if (title != null && !title.equalsIgnoreCase(mMultiTone.getTitle())) {
	                        	mMultiTone.setTitle(title);
	                            mTitleText.setText(title);
	                            mTitleText.setTextColor(Color.BLACK);
	                    		if(initialSavePassed){//Either editing add it to the DB and republish the title
	                    			titleChanged();
	                    		}
	                    		else{//or editing see if we can now create
	                    			createMultiTone();
	                    		}
	                        }
	            		}
	            	}
	            	break;
		    	case CONTACT_PICKER_RESULT:
	                switch(mMultiTone.getCurrentToneType()){
	            	case MultiTone.TYPE_RINGTONE:
	            		Uri contactURI = intent.getData();
			    		if(contactURI != null){
			    			ContactsService.getInstance().updateContactCustomRingtone(this, contactURI,mMultiTone.getUri());
			    		}
			    		break;
	            	case MultiTone.TYPE_NOTIFICATION_TONE:
	            		String contactLookupKey = intent.getStringExtra(MultiToneDbAdapter.KEY_COLUMN_CONTACT_LOOKUP_KEY);
	            		mDbHelper.setNotificationToneToUser(contactLookupKey,mMultiTone.getId());
	            		break;
	                }
	                break;
		    	case CONTACT_PICKER_REMOVE_RESULT:
	                switch(mMultiTone.getCurrentToneType()){
	            	case MultiTone.TYPE_RINGTONE:
	            		Uri contactURI = intent.getData();
			    		if(contactURI != null){
			    			ContactsService.getInstance().updateContactCustomRingtone(this,contactURI, null);
			    		}
			    		break;
	            	case MultiTone.TYPE_NOTIFICATION_TONE:
	            		String contactLookupKey = intent.getStringExtra(MultiToneDbAdapter.KEY_COLUMN_CONTACT_LOOKUP_KEY);
	            		mDbHelper.deleteNotificationToneFromUser(contactLookupKey);
	            		break;
	                }
	                break;
	        }
        }
    }
    
    private void addTone(){
//    	Intent intent = new Intent( RingtoneManager.ACTION_RINGTONE_PICKER);
//    	intent.putExtra( RingtoneManager.EXTRA_RINGTONE_TYPE,
//    	RingtoneManager.TYPE_RINGTONE);
//    	intent.putExtra( RingtoneManager.EXTRA_RINGTONE_TITLE, R.string.select_tone);
//    	intent.putExtra( RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,false);
//    	intent.putExtra( RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,false);
    	
    	Intent i = new Intent(getBaseContext(), TonePicker.class);
   
    	i.putExtra(MultiTone.MULTITONE_TYPE, mMultiTone.getCurrentToneType());
    	startActivityForResult(  i, ACTIVITY_ADD_TONE);
    }
    
    /**
     * This method will update a title within the database and republish to the contentstore if it is a ringtone
     */
    private void titleChanged(){
    	
    	mDbHelper.updateTitle(mMultiTone);
        if(mMultiTone.getCurrentToneType() == MultiTone.TYPE_RINGTONE){
	        ContentValues values = new ContentValues();
	        values.put(MediaStore.MediaColumns.TITLE, mMultiTone.getTitle());
	        Uri ringingUri = Uri.parse(mMultiTone.getUri());
	        getContentResolver().update(ringingUri, values,null,null);
        }
    }
    
    /**
     * This method will check if the MultiTone has a title and at least one tone
     * If so it will save them all to the DB
     * And publish to the content store
     * Non-unique titles are allowed, but confusing
     */
    private void createMultiTone(){
    	//This method is only called after checking initialSavePassed==false
    	//Check the conditions
    	if(mMultiTone.getTitle() != null && mMultiTone.getSize() > 0){
    		
    		//If it is a ringtone create it in the content provider
    		if(mMultiTone.getCurrentToneType() == MultiTone.TYPE_RINGTONE){
	        	//Create a new RingTone in the media store
	    		Uri ringToneUri = Uri.parse(mMultiTone.getCurrentToneUri());
	    		String[] projection = new String[] {
	    				MediaStore.MediaColumns.DATA,
	    				MediaStore.MediaColumns.SIZE,
	    				MediaStore.MediaColumns.MIME_TYPE,
	    				MediaStore.Audio.Media.DURATION
	                  };
	
	    		Cursor managedCursor = managedQuery(ringToneUri,
	                 projection, // Which columns to return 
	                 null,       // Which rows to return (all rows)
	                 null,       // Selection arguments (none)
	                 null);
	    		managedCursor.moveToNext();
	    		
	            ContentValues values = new ContentValues();
	            values.put(MediaStore.MediaColumns.DATA, managedCursor.getString(managedCursor.getColumnIndex(MediaStore.MediaColumns.DATA)));
	            values.put(MediaStore.MediaColumns.TITLE, mMultiTone.getTitle());
	            values.put(MediaStore.MediaColumns.DISPLAY_NAME, mMultiTone.getTitle());
	            values.put(MediaStore.MediaColumns.SIZE,  managedCursor.getLong(managedCursor.getColumnIndex(MediaStore.MediaColumns.SIZE)));
	            values.put(MediaStore.MediaColumns.MIME_TYPE, managedCursor.getString(managedCursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)));
	
	            values.put(MediaStore.Audio.Media.DURATION,  managedCursor.getLong(managedCursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));
	            values.put(MediaStore.Audio.Media.ALBUM,"Rototone");
	            values.put(MediaStore.Audio.Media.IS_RINGTONE,true);	            
	
	            final Uri newUri = getContentResolver().insert(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, values);
	            mMultiTone.setUri(newUri.toString());
    		}
    		//save to the DB for both ringtones and notification tones
    		mDbHelper.createMultitone(mMultiTone);
            
        	initialSavePassed=true;
    	}
    	
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(initialSavePassed){//If we're persisting to the db we can retrieve it all using the id
        	outState.putSerializable(SAVED_STATE_MULTITONEID, new Long(mMultiTone.getId()));
        }
        else{//else we need to store the whole object
        	outState.putSerializable(SAVED_STATE_MULTITONE, mMultiTone);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, MOVE_ID, 0, R.string.menu_move);
        MenuItem deleteItem = menu.add(0, DELETE_ID, 0, R.string.menu_delete_tone);
        if(mMultiTone.getSize() < 2){
        	deleteItem.setEnabled(false);
        	
        }
        menu.add(0, DETAILS_ID, 0, R.string.menu_details);
        menu.add(0, PLAY_ID, 0, R.string.menu_play);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
	        case MOVE_ID: //when the mMovePosition is set it is possible to move the selected item to the position of another by clicking on it
            	new AlertDialog.Builder(MultiToneEdit.this)//TODO Externalize text
                .setTitle("Move Tone")
                .setMessage("Select a new position to move the tone to.")
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
	        	mMovePosition=info.position;
	            return true;
	        case DELETE_ID:
	        	mDbHelper.deleteTone(mMultiTone.getToneList().get(info.position));

	        	mMultiTone.getToneList().remove(info.position);
	        	mDbHelper.reorderTones(mMultiTone);
	        	fillData();
	        	
	        	//If we deleted the current tone that was ready to play and this is a ringtone reset the content provider
	        	if (mMultiTone.getCurrentTone() == info.position && mMultiTone.getCurrentToneType() == MultiTone.TYPE_RINGTONE){
	        		//Retrieve the next uri (and updates the db to record what is playing)
	    			String nextUri = mDbHelper.getNextTone(mMultiTone.getId());
	    			
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
	        		if(cursor.moveToNext()){//If the next tone is valid move to it
		                ContentValues values = new ContentValues();
		                values.put(MediaStore.MediaColumns.DATA, cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA)));
		                values.put(MediaStore.MediaColumns.SIZE,  cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)));
		                values.put(MediaStore.MediaColumns.MIME_TYPE, cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)));
		                values.put(MediaStore.Audio.Media.DURATION,  cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));
		                Uri ringingUri = Uri.parse(mMultiTone.getUri());
		                //Log.d("ToneChanger", "attempting to update the ringtone");
		                getContentResolver().update(ringingUri, values,null,null);
	        		}
	        		//TODO Add an else clause to the if
	                cursor.close();
	        	}
	            return true;
            case DETAILS_ID:
            	Uri uri = Uri.parse(mMultiTone.getToneList().get(info.position).getUri());
            	Cursor c = managedQuery(uri, new String[]{MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.ALBUM}, null, null, null);
            	if(c.moveToNext()){ //If we find the tone show the info
	            	LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
	            	View view = inflater.inflate(R.layout.tone_details, (ViewGroup)findViewById(R.id.details_root));
	            	TextView text = (TextView) view.findViewById(R.id.artist);
	            	text.setText("Artist: "+c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
	            	text = (TextView) view.findViewById(R.id.album);
	            	text.setText("Album: "+c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
	            	
	            	new AlertDialog.Builder(MultiToneEdit.this)
	                .setTitle(mMultiTone.getToneList().get(info.position).getTitle())
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
            	}
            	else{//Else pop an alert saying the tone is not available
        			new AlertDialog.Builder(MultiToneEdit.this) //TODO Externalize the strings and check if the SD card really is unavailable
                    .setTitle("Tone Unavailable")
                    .setMessage("The tone is unavailable. It has either been deleted or the SD card is mounted")
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
                return true;
            case PLAY_ID:
            	if(tone != null && tone.isPlaying()){
            		tone.stop();
            	}
        		
        		String itemUri = mMultiTone.getToneList().get(info.position).getUri();
        		tone = RingtoneManager.getRingtone(this, Uri.parse(itemUri));
        		if(tone != null){ //If the tone isn't null play it
	        		tone.setStreamType(AudioManager.STREAM_SYSTEM);
	        		tone.play();
        		}
        		else{ //else pop and alert saying the tone is unavailable
        			new AlertDialog.Builder(MultiToneEdit.this)//TODO Externalize the strings and check if the SD card really is unavailable
                    .setTitle("Tone Unavailable")
                    .setMessage("The tone is unavailable. It has either been deleted or the SD card is mounted")
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
                return true;
        }
        return super.onContextItemSelected(item);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if(mMovePosition>-1 && mMovePosition != position){//We're moving an item
        	Tone tone = mMultiTone.getToneList().get(mMovePosition);
        	mMultiTone.getToneList().remove(mMovePosition);
        	mMultiTone.getToneList().add(position, tone);
        	mDbHelper.reorderTones(mMultiTone);
        	fillData();
        }
        mMovePosition=-1;//reset move id, we are no longer moving an item
    }
    
    @Override
	protected void onStop() {
    	if(tone != null && tone.isPlaying()){
    		tone.stop();
    	}
		super.onStop();
	}
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Close out the mDbHelper.
		mDbHelper.close();
	}
}
