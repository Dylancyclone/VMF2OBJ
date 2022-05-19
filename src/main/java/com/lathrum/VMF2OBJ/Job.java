package com.lathrum.VMF2OBJ;

import java.nio.file.Path;
import java.util.ArrayList;
import com.lathrum.VMF2OBJ.fileStructure.VMFFileEntry;

public class Job {

	public VMFFileEntry file;
	public ArrayList<Path> resourcePaths = new ArrayList<Path>();
	public boolean SuppressWarnings;
	public boolean skipTools;
	public boolean flipDisplacements;
}