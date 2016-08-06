package com.emergentgameplay.multitone;

import java.io.Serializable;

/**
 * A simple tone bean which contains those values the application needs
 * There is a one to many relationship between a MultiTone instance and a Tone instance
 * @author Chad Autry
 *
 */
public class Tone implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8500155351355834285L;
	private String Uri;
	private long id;
	private int position; //The position in the list
	private String title;
	private long multiToneId;
	private String artist;
	private long duration;
	public String getUri() {
		return Uri;
	}
	public void setUri(String uri) {
		Uri = uri;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public long getMultiToneId() {
		return multiToneId;
	}
	public void setMultiToneId(long multiToneId) {
		this.multiToneId = multiToneId;
	}
	@Override
	public String toString() {
		return getTitle();
	}
	public String getArtist() {
		return artist;
	}
	public void setArtist(String artist) {
		this.artist = artist;
	}
	public long getDuration() {
		return duration;
	}
	public void setDuration(long duration) {
		this.duration = duration;
	}
	
}
