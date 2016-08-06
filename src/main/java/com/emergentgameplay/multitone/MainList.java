package com.emergentgameplay.multitone;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class MainList extends ListActivity{
	private static final int ACTIVITY_EDIT=0;
	private static final int ACTIVITY_CREATE=1;
	private static final int CONTACT_PICKER_RESULT=2; 
	private static final int CONTACT_PICKER_REMOVE_RESULT=3; 
	private static final String preferencesString ="com.emergentgameplay.multitone";
	private static final String showInfo="showInfo";

	private static final int INSERT_ID = Menu.FIRST;
	private static final int DELETE_ID = Menu.FIRST + 1;
	private static final int EDIT_ID = Menu.FIRST + 2;
	private static final int CANCEL_ID = Menu.FIRST + 3;
	private static final int ADD_TO_USER_ID = Menu.FIRST + 4;
	private static final int MAKE_DEFAULT_ID = Menu.FIRST + 5;
	private static final int REMOVE_FROM_USER = Menu.FIRST + 6;
	private static final int REMOVE_FROM_DEFAULT = Menu.FIRST + 7;
	private static final int INFO_ID = Menu.FIRST+8;
	
	private MultiToneDbAdapter mDbHelper;
	private long customToneId=-1;
	
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_list);
        mDbHelper = new MultiToneDbAdapter(this);
        mDbHelper.open();
        
		RelativeLayout titleBar = (RelativeLayout)findViewById(R.id.title_bar);
		titleBar.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,new int[]{Color.DKGRAY,Color.GRAY}));
		titleBar.setPadding(0, 5, 0, 0);

		Button confirmButton = (Button) findViewById(R.id.button_exit);
		confirmButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				finish();
			}

		});

		Button cancelButton = (Button) findViewById(R.id.button_new);
		cancelButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				createMultitone();
			}

		});
        
        fillData();
        registerForContextMenu(getListView());
        
        //Check if the saved instanceState is null before showing the info screen (or else it pops up on orientation change)
        if(savedInstanceState == null){
            SharedPreferences settings = getSharedPreferences(preferencesString, MODE_PRIVATE);
            if(settings.getBoolean(showInfo, true)){
            	showInfo();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	boolean result = super.onCreateOptionsMenu(menu);
        menu.add(0, INSERT_ID, 0, R.string.menu_insert_multitone).setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, INFO_ID, 0, R.string.menu_info).setIcon(android.R.drawable.ic_menu_info_details);
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
        case INSERT_ID:
        	createMultitone();
            return true;
        case INFO_ID:
        	showInfo();
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void createMultitone() {
    	
        Intent i = new Intent(this, MultiToneEdit.class);
        i.putExtra(MultiToneDbAdapter.KEY_COLUMN_ID, -1L);
        startActivityForResult(i, ACTIVITY_CREATE);
    }
    
    private void fillData() {
        // Get all of the notes from the database and create the item list
        Cursor c = mDbHelper.fetchAllMultitones();
        startManagingCursor(c);

        String[] from = new String[] { MultiToneDbAdapter.KEY_COLUMN_TITLE,MultiToneDbAdapter.KEY_COLUMN_ID,MultiToneDbAdapter.KEY_COLUMN_ID,MultiToneDbAdapter.KEY_COLUMN_ID };
        int[] to = new int[] { R.id.text1,R.id.row_icon, R.id.count, R.id.default_in };
        
        // Now create an array adapter and set it to display using our row
        SimpleCursorAdapter multitones =
            new SimpleCursorAdapter(this, R.layout.main_row, c, from, to);
        
        multitones.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View view,
                                        Cursor cursor,
                                        int columnIndex) {
            	switch (view.getId()) {
            	case R.id.row_icon: //If it is the icon set the icon to the appropriate one based on type
                    switch(cursor.getInt(cursor.getColumnIndex(MultiToneDbAdapter.KEY_COLUMN_MULTITONE_TYPE))){
                	case MultiTone.TYPE_RINGTONE:
                		((ImageView)view).setImageResource(R.drawable.type_ringtone);
        	        	break;
                	case MultiTone.TYPE_NOTIFICATION_TONE:
                		((ImageView)view).setImageResource(R.drawable.type_notification);
                		break;
                    }
                    return true;
                    
            	case R.id.count: //if it is the count then lookup the count and set it
            		((TextView)view).setText(" ("+mDbHelper.getToneCount(cursor.getLong(cursor.getColumnIndex(MultiToneDbAdapter.KEY_COLUMN_ID)))+")");
            		return true;
            		
            	case R.id.default_in: //if it is the default in lookup whether this one is default or not
            		//This program handles notification tones differently from ringtones so need to determine which to know how to check default
                    switch(cursor.getInt(cursor.getColumnIndex(MultiToneDbAdapter.KEY_COLUMN_MULTITONE_TYPE))){
                	case MultiTone.TYPE_RINGTONE:
                		String defaultUriPath = RingtoneManager.getActualDefaultRingtoneUri(view.getContext(),RingtoneManager.TYPE_RINGTONE).toString();
                		String multittoneUriPath = cursor.getString(cursor.getColumnIndex(MultiToneDbAdapter.KEY_COLUMN_URI));
                		if(defaultUriPath != null && defaultUriPath.equalsIgnoreCase(multittoneUriPath)){
                			((TextView)view).setText("Default");
                			view.setVisibility(View.VISIBLE);
                		}
                		else{
                			view.setVisibility(View.GONE);
                		}
        	        	break;
                	case MultiTone.TYPE_NOTIFICATION_TONE:
                		String defaultIn = cursor.getString(cursor.getColumnIndex(MultiToneDbAdapter.KEY_COLUMN_DEFAULT));
                		if(defaultIn.equalsIgnoreCase("Y")){
                			((TextView)view).setText("Default");
                			view.setVisibility(View.VISIBLE);
                		}
                		else{
                			view.setVisibility(View.GONE);
                		}
                		break;
                    }
            		return true;
            		
                }
                return false;
            }
        });
        setListAdapter(multitones);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
    	MultiTone customContactRingtone = mDbHelper.fetchMultiTone(info.id);
    	
    	menu.add(0, ADD_TO_USER_ID, 0, R.string.menu_assign_to_contact);
    	
    	boolean isDefault = false;
    	switch(customContactRingtone.getCurrentToneType()){
    	case MultiTone.TYPE_RINGTONE:
    		String defaultUriPath = RingtoneManager.getActualDefaultRingtoneUri(this,RingtoneManager.TYPE_RINGTONE).toString();
    		String multittoneUriPath = customContactRingtone.getUri();
    		isDefault = (defaultUriPath != null && defaultUriPath.equalsIgnoreCase(multittoneUriPath));
        	break;
    	case MultiTone.TYPE_NOTIFICATION_TONE:
    		isDefault=customContactRingtone.isDefault();
    		break;
        }
    	if(isDefault && customContactRingtone.getCurrentToneType() == MultiTone.TYPE_NOTIFICATION_TONE){
    		//Can only blindly remove notification tones from being default. A ringtone needs something to replace it
    		menu.add(0, REMOVE_FROM_DEFAULT, 0, R.string.menu_remove_default);
    	}
    	else if(!isDefault){
    		menu.add(0, MAKE_DEFAULT_ID, 0, R.string.menu_make_default);
    	}
    	
    	
    	//Check if the item selected is attached to users.

    	boolean attachedToContacts = true;
        switch(customContactRingtone.getCurrentToneType()){
    	case MultiTone.TYPE_RINGTONE:
    		attachedToContacts = ContactsService.getInstance().isRingToneAttachedToContact(this,customContactRingtone.getUri());
        	break;
    	case MultiTone.TYPE_NOTIFICATION_TONE:
    		attachedToContacts = mDbHelper.isNotificationToneAttachedToContact(info.id);
        }
    	if(attachedToContacts){
    		menu.add(0, REMOVE_FROM_USER, 0, R.string.menu_remove_from_contact);
    	}
        menu.add(0, EDIT_ID, 0, R.string.menu_edit);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete_multitone);        
    }

	@Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
             
            case DELETE_ID:
            	final long id = info.id;
            	new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                //.setTitle(R.string.alert_are_you_sure)
                .setMessage(R.string.alert_are_you_sure)
                .setPositiveButton(R.string.alert_confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    	deleteMultiTone(id);
                    }

                })
                .setNegativeButton(R.string.alert_cancel, null)
                .show();


                return true;
            case EDIT_ID:
                Intent i = new Intent(this, MultiToneEdit.class);
                i.putExtra(MultiToneDbAdapter.KEY_COLUMN_ID, info.id);
                startActivityForResult(i, ACTIVITY_EDIT);
                return true;
            case CANCEL_ID:
                return true;
            case ADD_TO_USER_ID:
                Intent contactPickerIntent = new Intent(this, ContactPicker.class);
                MultiTone customContactRingtone = mDbHelper.fetchMultiTone(info.id);
                contactPickerIntent.putExtra(MultiTone.MULTITONE_TYPE, customContactRingtone.getCurrentToneType());
                customToneId=info.id;
                startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
                return true;
            case MAKE_DEFAULT_ID:
            	MultiTone newDefaultTone = mDbHelper.fetchMultiTone(info.id);
                switch(newDefaultTone.getCurrentToneType()){
            	case MultiTone.TYPE_RINGTONE:
                	RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE,Uri.parse(newDefaultTone.getUri()));
    	        	break;
            	case MultiTone.TYPE_NOTIFICATION_TONE:
            		mDbHelper.setToneAsDefault(info.id);
                }
                fillData(); //call fill data so the default indicator is updated
            	return true;
            case REMOVE_FROM_USER:
                contactPickerIntent = new Intent(this, ContactPicker.class);
                customContactRingtone = mDbHelper.fetchMultiTone(info.id);
                contactPickerIntent.putExtra(MultiTone.MULTITONE_TYPE, customContactRingtone.getCurrentToneType());
                contactPickerIntent.putExtra(MultiToneDbAdapter.KEY_COLUMN_ID, info.id);
                if( customContactRingtone.getUri() == null){
                	contactPickerIntent.putExtra(MultiToneDbAdapter.KEY_COLUMN_URI, "wonder twin powers activate!");
                }
                else{
                	contactPickerIntent.putExtra(MultiToneDbAdapter.KEY_COLUMN_URI, customContactRingtone.getUri());
                }
                customToneId=info.id;
                startActivityForResult(contactPickerIntent, CONTACT_PICKER_REMOVE_RESULT);
            	return true;
            case REMOVE_FROM_DEFAULT:
            	mDbHelper.removeFromDefault(info.id);
            	fillData();
            	return true;
        }
        return super.onContextItemSelected(item);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, MultiToneEdit.class);
        i.putExtra(MultiToneDbAdapter.KEY_COLUMN_ID, id);
        startActivityForResult(i, ACTIVITY_EDIT);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);
    	if(resultCode == RESULT_OK){
	    	switch(requestCode){
		    	case ACTIVITY_EDIT:
		    	case ACTIVITY_CREATE:
			        fillData();
			        return;
			        
		    	case CONTACT_PICKER_RESULT:
		    		
		    		MultiTone customContactRingtone = mDbHelper.fetchMultiTone(customToneId);
	                switch(customContactRingtone.getCurrentToneType()){
	            	case MultiTone.TYPE_RINGTONE:
	            		Uri contactURI = intent.getData();
			    		if(contactURI != null){
			    			ContactsService.getInstance().updateContactCustomRingtone(this, contactURI,customContactRingtone.getUri());
			    		}
			    		break;
	            	case MultiTone.TYPE_NOTIFICATION_TONE:
	            		String contactLookupKey = intent.getStringExtra(MultiToneDbAdapter.KEY_COLUMN_CONTACT_LOOKUP_KEY);
	            		mDbHelper.setNotificationToneToUser(contactLookupKey,customToneId);
	            		break;
	                }
	                return;
		    	case CONTACT_PICKER_REMOVE_RESULT:
		    		customContactRingtone = mDbHelper.fetchMultiTone(customToneId);
	                switch(customContactRingtone.getCurrentToneType()){
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
	                return;
	    	}
    	}
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Close out the mDbHelper. There is no state that needs to be saved
		mDbHelper.close();
	}
	
	//Method to delete a multiTone
	private void deleteMultiTone(long id){
        mDbHelper.deleteMultiTone(this,id);
        fillData();  
	}
    
    private void showInfo(){
    	

    	LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
    	View view = inflater.inflate(R.layout.app_info, (ViewGroup)findViewById(R.id.app_info_root));
    	
        CheckBox shuffleBox = (CheckBox)view.findViewById(R.id.show_cb);
        //Log.d("MultiToneEdit", "Setting shuffleBox to "+mMultiTone.isShuffle());
        SharedPreferences settings = getSharedPreferences(preferencesString, MODE_PRIVATE);
        shuffleBox.setChecked(!settings.getBoolean(showInfo, true));
        shuffleBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				SharedPreferences settings = getSharedPreferences(preferencesString, MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean(showInfo, !arg1);
				editor.commit();
			}
		});
    	
    	TextView text = (TextView) view.findViewById(R.id.thank_you);
    	text.setText("Thank you for using Rototone!");
    	
    	text = (TextView) view.findViewById(R.id.notification_info);
    	text.setText("If using Rototone for text message tones, please set the Messaging application's own tone to silent (under settings). " +
    			"Then don't forget to set a default tone within Rototone along with any contact specific tones. ");
    	
    	text = (TextView) view.findViewById(R.id.notificationtone_text);
    	text.setText("represents text message tones");
    	
    	text = (TextView) view.findViewById(R.id.ringtone_text);
    	text.setText("represents ringtones");
    	
    	ImageView image = (ImageView)view.findViewById(R.id.notificationtone_image);
    	image.setImageResource(R.drawable.type_notification);
    	
    	image = (ImageView)view.findViewById(R.id.ringtone_image);
    	image.setImageResource(R.drawable.type_ringtone);
    	
    	text = (TextView) view.findViewById(R.id.contact_info);
    	text.setText("Further information and support can be found at www.rototone.com");

    	
    	new AlertDialog.Builder(this)
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
}
