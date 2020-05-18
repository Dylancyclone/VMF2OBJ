package com.lathrum.VMF2OBJ;

import java.io.File;
import java.io.IOException;


public interface Entry 
{
	public byte[] readData() throws IOException, Exception;
	
	public void extract(File file) throws IOException, Exception;
  
	public String getFileName();
  
	public String getExtension();
  
	public String getFullName();
  
	public String getPath();
  
	public String getFullPath();
}