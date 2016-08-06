package com.emergentgameplay.multitone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AlphabetIndexer;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/*
 * A contact picker class, filters out contacts without a phone number
 */
public class ContactPicker extends ListActivity     implements TextWatcher
{
    private SimpleCursorAdapter mAdapter;
    private int toneType = -1; //used to indicate if we are showing NotificationTones and RingTones
    private MultiToneDbAdapter mDbHelper;
    private String toneUri = null;
    private long multitoneId = -1;//If this is provided we are trying to remove a tone from a user and should only show those users with the tone

    public ContactPicker() {
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDbHelper = new MultiToneDbAdapter(this);
        mDbHelper.open();
        // Inflate our UI from its XML layout description.
        setContentView(R.layout.contact_select);
        
        if(savedInstanceState != null){ 
        	toneType = savedInstanceState.getInt(MultiTone.MULTITONE_TYPE);
        	multitoneId = savedInstanceState.getLong(MultiToneDbAdapter.KEY_COLUMN_ID);
        	toneUri=savedInstanceState.getString(MultiToneDbAdapter.KEY_COLUMN_URI);

        }
        else{//else get the intent and go from there
	        //Initialize the MultiTone using the Id passed (may be a new multiTone)
	        Bundle extras = getIntent().getExtras();
	        toneType = extras.getInt(MultiTone.MULTITONE_TYPE);
	        multitoneId = extras.getLong(MultiToneDbAdapter.KEY_COLUMN_ID);
	        toneUri=extras.getString(MultiToneDbAdapter.KEY_COLUMN_URI);

        }
        
        //If given a uri to filter to find the list of ids witch this uri
        Collection<String> filterIds = null;
        if(toneUri != null){
        	switch(toneType){
	        case MultiTone.TYPE_RINGTONE:
	        	filterIds=ContactsService.getInstance().getContactLookupKeysForTone(this,toneUri);
		        break;
	        case MultiTone.TYPE_NOTIFICATION_TONE:
	        	filterIds = mDbHelper.getContactLookupKeys(multitoneId);
	        	break;
	        }
        }

		//Get the contacts cursor in the api specific fashion
		Cursor c = ContactsService.getInstance().getContactsCursor(this, "",filterIds);
		startManagingCursor(c);
        mAdapter = new MyCursorAdapter(
            this,
            // Use a template that displays a text view
            R.layout.contact_select_row,
            // Give the cursor to the list adapter
            c,
            // Map from database columns...
            ContactsService.getInstance().getDataBaseColumns(),
            // To widget ids in the row layout...
            new int[] {
                R.id.row_contact_name});
        setListAdapter(mAdapter);
        getListView().setFastScrollEnabled(true);
        // Normal click - open the editor
        getListView().setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView parent,
                                        View view,
                                        int position,
                                        long id) {
                	onListItemClick(id);
                }
            });
        

        // Long-press opens a context menu
        registerForContextMenu(getListView());

//        mFilter = (TextView) findViewById(R.id.search_filter);
//        if (mFilter != null) {
//            mFilter.addTextChangedListener(this);
//        }
        
       
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
        //If given a uri to filter to find the list of ids witch this uri
        Collection<String> filterIds = null;
        if(toneUri != null){
        	switch(toneType){
	        case MultiTone.TYPE_RINGTONE:
	        	filterIds=ContactsService.getInstance().getContactLookupKeysForTone(this,toneUri);
		        break;
	        case MultiTone.TYPE_NOTIFICATION_TONE:
	        	filterIds = mDbHelper.getContactLookupKeys(multitoneId);
	        	break;
	        }
        }
       // String filterStr = mFilter.getText().toString();
		Cursor c = ContactsService.getInstance().getContactsCursor(this, "",filterIds);
		startManagingCursor(c);
        mAdapter.changeCursor(c);
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

    protected void onListItemClick(long id) {
    	Intent mIntent = new Intent();
    	switch(toneType){
	        case MultiTone.TYPE_RINGTONE:
	        	Uri contactUri = ContactsService.getInstance().getContactUri(id);
	        	mIntent.setData(contactUri);
		        break;
	        case MultiTone.TYPE_NOTIFICATION_TONE:
	        	Cursor cursor = mAdapter.getCursor();
	        	String contactLookupKey = cursor.getString(cursor.getColumnIndex(ContactsService.getInstance().getDataBaseColumns()[2]));
	        	mIntent.putExtra(MultiToneDbAdapter.KEY_COLUMN_CONTACT_LOOKUP_KEY, contactLookupKey);

	        }
    	

		

		setResult(RESULT_OK, mIntent);
		finish();
    }
    
    //Shift positions and indexes to take into account the headers used
    class HeaderSectionIndexer extends AlphabetIndexer {

    	Object[] sections;
    	Map<Integer,Integer> usedSectionToOriginalPositionMap=new HashMap<Integer,Integer>();
		public HeaderSectionIndexer(Cursor cursor, int sortedColumnIndex,
				CharSequence alphabet) {
			super(cursor, sortedColumnIndex, alphabet);
			List<Object> usedList = new ArrayList<Object>();
			Object[] allSections = super.getSections();
			
			//go through each section and check if it is used
			for(int i = 0; allSections!= null && i < allSections.length;i++ ){
				int position = super.getPositionForSection(i);
				try {
					if( i == super.getSectionForPosition(position)){
						usedList.add(allSections[i]);
						usedSectionToOriginalPositionMap.put(usedList.size()-1, position);
					}
				} catch (IndexOutOfBoundsException e) {
					//getPositionForSection can return the size of the list if there are no elements for the selection
					// catch and ignore index out of bounds exceptions coming from getSectionForPosition
				}
			}
			sections = new Object[usedList.size()];
			for(int i = 0; i < sections.length; i++){
				sections[i] = usedList.get(i);
			}
		}
		
		@Override
		public Object[] getSections() {
			return sections;
		}

		//Our position takes account of the fact that the preceding headers are using space
		//And that we are missing some headers
		@Override
		public int getPositionForSection(int sectionIndex) {

			return usedSectionToOriginalPositionMap.get(sectionIndex) + sectionIndex;
		}

		//return neg 1 if the position is before the position of any sections
		//Else return the first used section this is before
		@Override
		public int getSectionForPosition(int position) {
			int sectionId = 0;
			while(sectionId < sections.length && position >= getPositionForSection((sectionId))){
				sectionId++;
			}
			return sectionId -1;
		}
    	
		
		
    }
    
	class MyCursorAdapter extends SimpleCursorAdapter implements SectionIndexer {
	    private static final int TYPE_HEADER = 1;
	    private static final int TYPE_NORMAL = 0;
	 
	    private HeaderSectionIndexer indexer;

	    public MyCursorAdapter(Context context, int layout, Cursor c,
	            String[] from, int[] to) {
	        super(context, layout, c, from, to);



	        indexer = new HeaderSectionIndexer(c, c.getColumnIndexOrThrow(ContactsService.getInstance().getIndexedColumn()), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
	        
	    }
	 
	    @Override
	    public void bindView(View view, Context context, Cursor cursor) {
	    	ContactsService cService = ContactsService.getInstance();
	    	//Get the users Image (or default) and set it to the image view
	        ImageView imageView = (ImageView) view.findViewById(R.id.row_icon);	        
	        
	        //Get the name of their current ringtone or notification tone and set it to the view
	        TextView toneTitleView = (TextView) view.findViewById(R.id.row_title);
	        switch(toneType){
	        case MultiTone.TYPE_RINGTONE:
	        	imageView.setImageBitmap(cService.getContactPhoto(cursor, context,toneType));
		        String customRingtoneUriPath = cursor.getString(cursor.getColumnIndex(cService.getDataBaseColumns()[1]));
		        if (customRingtoneUriPath != null){
		        	Uri ringToneUri = Uri.parse(customRingtoneUriPath);
		        	Cursor c = context.getContentResolver().query(ringToneUri, new String[] {
		    				MediaStore.MediaColumns.TITLE}, null, null, null);
		    		if(c!= null && c.moveToFirst()){
		    			toneTitleView.setText(c.getString(0));
		    		}
		    		else{
		    			toneTitleView.setText("Default");
		    		}
		    		if(c!= null){
		    			c.close();
		    		}	
		        }
		        else{
		        	toneTitleView.setText("Default");
		        }
		        break;
	        case MultiTone.TYPE_NOTIFICATION_TONE:
	        	imageView.setImageBitmap(cService.getContactPhoto(cursor, context,toneType));
	        	String contactLookupKey = cursor.getString(cursor.getColumnIndex(cService.getDataBaseColumns()[2]));
	        	String title = mDbHelper.getCustomNotificationToneTitle(contactLookupKey);
	        	if(title == null){
	        		title = "Default";
	        	}
	        	toneTitleView.setText(title);
	        }
	      //Call super so that the users name gets set to the appropriate text view automatically
	        super.bindView(view, context, cursor);
	    }

	    public int getCount() {
	        if (super.getCount() != 0){
	            return super.getCount() + indexer.getSections().length;
	        }
	 
	        return 0;
	    }
	 

	    public Object getItem(int position) {
	        if (getItemViewType(position) == TYPE_NORMAL){//we define this function later
	            return super.getItem(position - getSectionForPosition(position) - 1);
	        }
	 
	        return null;
	    }
	 
	    @Override
	    public long getItemId(int position){

	    	return super.getItemId(position - getSectionForPosition(position) - 1);
	    }
	
	    public int getPositionForSection(int section) {
	        return indexer.getPositionForSection(section);
	    }
	 

	    public int getSectionForPosition(int position) {
	    	return indexer.getSectionForPosition(position);
	    }
	 
	
	    public Object[] getSections() {
	        return indexer.getSections();
	    }
	 
	    //nothing much to this: headers have positions that the sectionIndexer manages.

	    public int getItemViewType(int position) {
	        if (position == getPositionForSection(getSectionForPosition(position))){
	            return TYPE_HEADER;
	        } 
	        return TYPE_NORMAL;
	    }
	 
	    //return the header view, if it's in a section header position

	    public View getView(int position, View convertView, ViewGroup parent) {
	    	  final int type = getItemViewType(position);
	       if (type == TYPE_HEADER){
	            if (convertView == null){
	                convertView = getLayoutInflater().inflate(R.layout.contact_select_sectionbreak, parent, false);
	                convertView.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,new int[]{Color.DKGRAY,Color.GRAY}));
	            }
	            TextView tview =((TextView)convertView.findViewById(R.id.header));
	            if(tview == null){
	            	convertView = getLayoutInflater().inflate(R.layout.contact_select_sectionbreak, parent, false);
	                convertView.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,new int[]{Color.DKGRAY,Color.GRAY}));
	                tview =((TextView)convertView.findViewById(R.id.header));
	            }

	            tview.setText((String)getSections()[getSectionForPosition(position)]);
	            return convertView;
	        }
	       else{
	    	   if(convertView != null){
		    	   ImageView imageView = (ImageView) convertView.findViewById(R.id.row_icon);	
		    	   if(imageView == null){
		    		   convertView = null;
		    	   }
	    	   }
	       }
	      
	        return super.getView(position - getSectionForPosition(position) - 1, convertView, parent);
	        
	    }
	 
	    //these two methods just disable the headers
	    public boolean areAllItemsEnabled() {
	        return false;
	    }
	 
	    public boolean isEnabled(int position) {
	        if (getItemViewType(position) == TYPE_HEADER){
	            return false;
	        }
	        return true;
	    }


	}
     
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Close out the mDbHelper.
		mDbHelper.close();
	}
	
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(MultiTone.MULTITONE_TYPE, toneType);
        outState.putLong(MultiToneDbAdapter.KEY_COLUMN_ID, multitoneId);
        outState.putString(MultiToneDbAdapter.KEY_COLUMN_URI,toneUri );
    }
}
