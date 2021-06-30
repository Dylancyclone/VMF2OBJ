package com.lathrum.VMF2OBJ.fileStructure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class VPKEntry implements Entry {

	public static final int TERMINATOR = 0x7FFF;

	private VPK archive;
	private short archiveIndex;

	private byte[] preloadData;

	private String filename;
	private String extension;

	private int crc;
	private int offset;
	private int length;
	private short terminator;

	private String path;

	protected VPKEntry(VPK archive, short archiveIndex, byte[] preloadData, String filename, String extension, int crc,
			int offset, int length, short terminator, String path) {
		this.archive = archive;
		this.archiveIndex = archiveIndex;

		this.preloadData = preloadData;

		this.filename = filename.trim();
		this.extension = extension.trim();

		this.crc = crc;
		this.offset = offset;
		this.length = length;
		this.terminator = terminator;

		this.path = path;
	}

	public byte[] readData() throws IOException, Exception {
		byte[] data;
		int dataOffset = 0;
		// check for preload data
		if (this.preloadData != null) {
			if (this.length == 0) {
				return this.preloadData;
			}
			// Allocate enough space for the data
			data = new byte[this.preloadData.length + this.length];
			// Copy in the preloaded data
			System.arraycopy(this.preloadData, 0, data, 0, this.preloadData.length);
			// If there's additional data let it know to insert after the preloaded data
			dataOffset = this.preloadData.length;
		} else {
			data = new byte[this.length];
		}

		// get target archive
		File target = null;

		if (this.archive.isMultiPart())
			target = this.archive.getChildArchive(this.archiveIndex);
		else
			target = this.archive.getFile();

		try (FileInputStream fileInputStream = new FileInputStream(target)) {
			if (this.archiveIndex == VPKEntry.TERMINATOR) {
				// skip tree and header
				fileInputStream.skip(this.archive.getTreeLength());
				fileInputStream.skip(this.archive.getHeaderLength());
			}

			// read data
			fileInputStream.skip(this.offset);
			fileInputStream.read(data, dataOffset, this.length);

			return data;
		}
	}

	public void extract(File file) throws IOException, Exception {
		try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
			// write
			fileOutputStream.write(this.readData());
		}
	}

	public VPK getArchive() {
		return this.archive;
	}

	public int getArchiveIndex() {
		return this.archiveIndex;
	}

	public String getFileName() {
		return this.filename;
	}

	public String getExtension() {
		return this.extension;
	}

	public String getFullName() {
		return (this.filename + "." + this.extension);
	}

	public String getPath() {
		return (this.path);
	}

	public String getFullPath() {
		return (this.path + "/" + this.filename + "." + this.extension);
	}

	public int getCrc() {
		return this.crc;
	}

	public int getOffset() {
		return this.offset;
	}

	public int getLength() {
		return this.length;
	}

	public int getTerminator() {
		return this.terminator;
	}
}