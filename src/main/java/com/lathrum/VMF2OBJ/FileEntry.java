package com.lathrum.VMF2OBJ;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class FileEntry implements Entry {

	private String filename;
	private String extension;

	private String path;
	private String realPath;

	public FileEntry(String filename, String extension, String path, String realPath) {

		this.filename = filename.trim();
		this.extension = extension.trim();

		this.path = path;
		this.realPath = realPath;
	}

	public byte[] readData() throws IOException, Exception {
		return Files.readAllBytes(Paths.get(this.realPath));
	}
	
	public void extract(File file) throws IOException, Exception
	{
		try (FileOutputStream fileOutputStream = new FileOutputStream(file))
		{
			//write
			fileOutputStream.write(this.readData());
		}
	}
  
	public String getFileName()
	{
		return this.filename;
  }
  
	public String getExtension()
	{
		return this.extension;
  }
  
	public String getFullName()
	{
		return (this.filename + "." + this.extension);
  }
  
	public String getPath()
	{
		return (this.path);
  }
  
	public String getFullPath()
	{
		return (this.path+"/"+this.filename + "." + this.extension);
  }
}