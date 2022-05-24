package com.lathrum.VMF2OBJ.fileStructure;

import java.io.File;

public class VMFFileEntry {

	public File vmfFile;
	public String outPath;
	public File objFile;
	public File mtlFile;

	public VMFFileEntry(File vmfFile) {
		this.vmfFile = vmfFile;
		outPath = replaceExtension(vmfFile, "").toString();
		objFile = replaceExtension(vmfFile, ".obj");
		mtlFile = replaceExtension(vmfFile, ".mtl");
	}

	public VMFFileEntry(File vmfFile, String outputString) {
		this.vmfFile = vmfFile;
		outPath = outputString;
		objFile = new File(outputString + ".obj");
		mtlFile = new File(outputString + ".mtl");
	}

	private static File replaceExtension(File file, String newExt) {
		String fileName = file.getName();
		String base = fileName.substring(0, fileName.lastIndexOf('.'));
		File parentFile = file.getAbsoluteFile().getParentFile();

		return new File(parentFile, base + newExt);
	}
	public void setOutpath(String outPath) {
		this.outPath = outPath;
		this.objFile = new File(outPath + ".obj");
		this.mtlFile = new File(outPath + ".mtl");
	}
}