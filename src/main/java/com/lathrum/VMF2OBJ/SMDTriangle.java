package com.lathrum.VMF2OBJ;

public class SMDTriangle {
	public String materialName;

	public SMDPoint[] points;

  public SMDTriangle(String materialName, SMDPoint[] points) {
		this.materialName = materialName;
		this.points = points;
	}
}
