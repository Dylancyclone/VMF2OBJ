package com.lathrum.VMF2OBJ.fileStructure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class VPK {
	// https://developer.valvesoftware.com/wiki/VPK
	public static final int SIGNATURE = 0x55AA1234;
	public static final char NULL_TERMINATOR = 0x0;

	public static final int MINIMUM_VERSION = 1;
	public static final int MAXIMUM_VERSION = 2;

	public static final int VERSION_ONE = 1;
	public static final int VERSION_TWO = 2;
	public static final int VERSION_ONE_HEADER_SIZE = 12;
	public static final int VERSION_TWO_HEADER_SIZE = 28;

	private File file;
	private boolean multiPart;

	private int signature;
	private int version;
	private int treeLength;
	private int headerLength;

	private ArrayList<Directory> directories;

	public VPK(File file) {
		this.file = file;
		this.multiPart = false;

		this.signature = 0;
		this.version = 0;
		this.treeLength = 0;
		this.headerLength = 0;

		this.directories = new ArrayList<Directory>();
	}

	public void load() throws IOException, Exception {
		try (FileInputStream fileInputStream = new FileInputStream(this.file)) {
			// check for multiple child archives
			this.multiPart = this.file.getName().contains("_dir");

			// read header
			this.signature = this.readUnsignedInt(fileInputStream);
			this.version = this.readUnsignedInt(fileInputStream);
			this.treeLength = this.readUnsignedInt(fileInputStream);

			// check signature and version
			if (this.signature != VPK.SIGNATURE)
				throw new Exception("Invalid signature");
			if (this.version < VPK.MINIMUM_VERSION || this.version > VPK.MAXIMUM_VERSION)
				throw new Exception("Unsupported version");

			// version handling
			switch (this.version) {
				case VPK.VERSION_ONE: {
					this.headerLength = VPK.VERSION_ONE_HEADER_SIZE;

					break;
				}
				case VPK.VERSION_TWO: {
					this.headerLength = VPK.VERSION_TWO_HEADER_SIZE;

					// read extra data
					// serves no purpose right now
					this.readUnsignedInt(fileInputStream);
					this.readUnsignedInt(fileInputStream);
					this.readUnsignedInt(fileInputStream);
					this.readUnsignedInt(fileInputStream);
				}
			}

			while (fileInputStream.available() != 0) {
				// get extension
				String extension = this.readString(fileInputStream);
				if (extension.isEmpty())
					break;

				while (true) {
					// get path
					String path = this.readString(fileInputStream);
					if (path.isEmpty())
						break;

					// directory
					Directory directory = new Directory(path);
					this.directories.add(directory);

					while (true) {
						// get filename
						String filename = this.readString(fileInputStream);
						if (filename.isEmpty())
							break;

						// read data
						int crc = this.readUnsignedInt(fileInputStream);
						short preloadSize = this.readUnsignedShort(fileInputStream);
						short archiveIndex = this.readUnsignedShort(fileInputStream);
						int entryOffset = this.readUnsignedInt(fileInputStream);
						int entryLength = this.readUnsignedInt(fileInputStream);
						short terminator = this.readUnsignedShort(fileInputStream);
						byte[] preloadData = null;

						if (preloadSize > 0) {
							// read preload data
							preloadData = new byte[preloadSize];
							fileInputStream.read(preloadData);
						}

						// create entry
						VPKEntry entry = new VPKEntry(this, archiveIndex, preloadData, filename, extension, crc, entryOffset,
								entryLength, terminator, path);
						directory.addEntry(entry);
					}
				}
			}
		}
	}

	public File getChildArchive(int index) throws Exception {
		// check
		if (!this.multiPart)
			throw new Exception("Archive is not multi-part");

		// get parent
		File parent = this.file.getParentFile();
		if (parent == null)
			throw new Exception("Archive has no parent");

		// get child name
		String fileName = this.file.getName();
		String rootName = fileName.substring(0, fileName.length() - 8);
		String childName = String.format("%s_%03d.vpk", rootName, index);

		return new File(parent, childName);
	}

	private String readString(FileInputStream fileInputStream) throws IOException {
		// builder
		StringBuilder stringBuilder = new StringBuilder();

		// read
		int character = 0;
		while ((character = fileInputStream.read()) != VPK.NULL_TERMINATOR)
			stringBuilder.append((char) character);

		return stringBuilder.toString();
	}

	private int readUnsignedInt(FileInputStream fileInputStream) throws IOException {
		return this.readBytes(fileInputStream, 4).getInt();
	}

	private short readUnsignedShort(FileInputStream fileInputStream) throws IOException {
		return this.readBytes(fileInputStream, 2).getShort();
	}

	private ByteBuffer readBytes(FileInputStream fileInputStream, int size) throws IOException {
		// byte array
		byte[] buffer = new byte[size];
		fileInputStream.read(buffer);

		// byte buffer
		ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		return byteBuffer;
	}

	public File getFile() {
		return this.file;
	}

	public boolean isMultiPart() {
		return this.multiPart;
	}

	public int getSignature() {
		return this.signature;
	}

	public int getVersion() {
		return this.version;
	}

	public int getTreeLength() {
		return this.treeLength;
	}

	public int getHeaderLength() {
		return this.headerLength;
	}

	public ArrayList<Directory> getDirectories() {
		return this.directories;
	}
}