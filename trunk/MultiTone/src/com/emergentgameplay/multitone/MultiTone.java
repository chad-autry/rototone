package com.emergentgameplay.multitone;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A class representing the MultiTone itself
 * Has various bean attributes plus several helper methods such as for determining the next tone
 * @author Chad Autry
 *
 */
public class MultiTone implements Serializable {
	/**
	 * 
	 */
	public static String MULTITONE_TYPE="multitone_type";
	public static final int TYPE_RINGTONE=0;
	public static final int TYPE_NOTIFICATION_TONE=1;
	public static final int TYPE_UNDETERMINED = 2;
	
	private static final long serialVersionUID = -272788321377252917L;
	private List<Tone> toneList;
	private boolean shuffle = false;
	private int currentTone;
	private String title;
	private String uri;
	private long id;
	private int currentToneType = TYPE_UNDETERMINED;
	private boolean isDefault;
	
	public void addTone(Tone tone){
		if(toneList == null){
			toneList = new ArrayList<Tone>();
		}
		tone.setMultiToneId(id);
		tone.setPosition(toneList.size());
		toneList.add(tone.getPosition(), tone);
	}
	
	public int getSize(){
		if (toneList==null){
			return 0;
		}
		return toneList.size();
	}
	
	public int getCurrentToneType() {
		return currentToneType;
	}

	public void setCurrentToneType(int currentToneType) {
		this.currentToneType = currentToneType;
	}

	/**
	 * This method is used the first time a MultiTone is created 
	 * Returns the toneUri to rip data from when publishing in the content store
	 * @return
	 */
	public String getCurrentToneUri(){
		return toneList.get(currentTone).getUri();
	}
	
	/**
	 * Determine which tone this MultiTone should play next and return the uri string
	 * @return
	 */
	public String getNextToneUri(){
		if(shuffle){
			//I don't want any back to back tone playing shennanigans. Don't count the current tone when randomizing
			int position = (int) Math.floor(Math.random()*(toneList.size()-1));
			
			//we randomed one less than the size of the tonelist. If we get the currently playing tone select the final one instead.
			if(position == currentTone){
				currentTone = toneList.size()-1;
			}
			else{
				currentTone = position;
			}
		}
		else{
			if((toneList.size()-1)==currentTone){
				currentTone = -1;
			}
			currentTone++;	
		}
		return toneList.get(currentTone).getUri();
	}

	public int getCurrentTone() {
		return currentTone;
	}

	public void setCurrentTone(int currentTone) {
		this.currentTone = currentTone;
	}
	
	public void setShuffle(String shuffle){
		if(shuffle.equalsIgnoreCase("Y")){
			this.shuffle=true;
		}
		else{
			this.shuffle=false;
		}
	}
	
	public String getShuffle(){
		if(shuffle){
			return "Y";
		}
		else{
			return "N";
		}
	}
	
	public void setShuffle(boolean shuffle){
		this.shuffle=shuffle;
	}
	
	public boolean isShuffle(){
		return shuffle;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<Tone> getToneList() {
		return toneList;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public boolean isDefault() {
		return isDefault;
	}
	
}
