package com.lathrum.VMF2OBJ.dataStructure.texture;

public class Texture {
	public String name;
	public String fileName;
	public String path;

	public int width;
	public int height;

	public Texture(String name, String fileName, String path, int width, int height) {
		this.name = name;
		this.fileName = fileName;
		this.path = path;
		this.width = width;
		this.height = height;
	}

	public String toString() {
		return "name: " + name + ", filename: " + fileName + ", path: " + path + ", width: " + width + ", height: " + height;
	}
}
