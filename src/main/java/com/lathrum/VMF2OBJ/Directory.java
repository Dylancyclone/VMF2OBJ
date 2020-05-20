package com.lathrum.VMF2OBJ;

import java.util.ArrayList;

public class Directory {

	public static final String SEPARATOR = "/";

	private String path;
	private ArrayList<Entry> entries;

	protected Directory(String path) {
		this.path = path.trim();
		this.entries = new ArrayList<Entry>();
	}

	public String getPath() {
		return this.path;
	}

	public String getPathFor(Entry entry) {
		return (this.path + Directory.SEPARATOR + entry.getFullName());
	}

	public void addEntry(Entry entry) {
		this.entries.add(entry);
	}

	public void removeEntry(Entry entry) {
		this.entries.remove(entry);
	}

	public ArrayList<Entry> getEntries() {
		return this.entries;
	}
}