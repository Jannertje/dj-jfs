package com.jfs.funkmachine2000;

public class Midi {
	private long id;
	private byte[] file;
	private String date;
	private String string;
	
	public long getId() {
		return this.id;
	}
	
	public byte[] getFile() {
		return this.file;
	}
	
	public String getDate() {
		return this.date;
	}
	
	public String getString() {
		return this.string;
	}
	
	public Midi setId(long id) {
		this.id = id;
		return this;
	}
	
	public Midi setFile(byte[] file) {
		this.file = file;
		return this;
	}
	
	public Midi setDate(String date) {
		this.date = date;
		return this;
	}
	
	public Midi setString(String string) {
		this.string = string;
		return this;
	}
	
	@Override
	public String toString() {
		return this.date+" ("+this.id+")";
	}
}
