package com.emergentgameplay.multitone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

/**
 * This is the activity for editing the title of a MultiTone I didn't like how
 * if it was editable from the main screen the keyboard would squish things and
 * not loose focus
 * 
 * @author Chad Autry
 * 
 */
public class TitleEdit extends Activity {

	private EditText mTitleText;
	AlertDialog.Builder alt_bld;
	private MultiToneDbAdapter mDbHelper;
	private long multiToneId;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.title_edit);
		mTitleText = (EditText) findViewById(R.id.title);
		mTitleText.requestFocus();
        mDbHelper = new MultiToneDbAdapter(this);
        mDbHelper.open();
		// Set the pre-existing title if given
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
            String title = extras.getString(MultiToneDbAdapter.KEY_COLUMN_TITLE);
            if (title != null) {
                mTitleText.setText(title);
            }
            Long multiToneId = extras.getLong(MultiToneDbAdapter.KEY_COLUMN_ID);
            if (multiToneId != null) {
            	this.multiToneId=multiToneId;
            }
		}
		
		alt_bld = new AlertDialog.Builder(this);
		alt_bld.setMessage("Duplicate title exists")
		.setCancelable(false);
		alt_bld.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
			//  Action for 'NO' Button
			dialog.cancel();
			}
			});

		final AlertDialog alert = alt_bld.create();

		

		
		RelativeLayout titleBar = (RelativeLayout)findViewById(R.id.title_bar);
		titleBar.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,new int[]{Color.DKGRAY,Color.GRAY}));
		titleBar.setPadding(0, 5, 0, 0);

		Button confirmButton = (Button) findViewById(R.id.confirm);
		confirmButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				
				//Check if this title already exists (and not for this playlist)
				long titleId=mDbHelper.getTitleExists(mTitleText.getText().toString());
				//If so display an alert and don't return to the screen
				if(titleId==-1 || titleId ==multiToneId){
					Bundle bundle = new Bundle();
					
					bundle.putString(MultiToneDbAdapter.KEY_COLUMN_TITLE,
							mTitleText.getText().toString());
	
					Intent mIntent = new Intent();
					mIntent.putExtras(bundle);
					setResult(RESULT_OK, mIntent);
					finish();
				}
				else{
					alert.show();
				}
			}

		});

		Button cancelButton = (Button) findViewById(R.id.cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				Intent mIntent = new Intent();

				setResult(RESULT_CANCELED, mIntent);
				finish();
			}

		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Close out the mDbHelper. There is no state that needs to be saved
		mDbHelper.close();
	}
}
