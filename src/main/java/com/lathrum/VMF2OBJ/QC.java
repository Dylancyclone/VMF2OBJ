package com.lathrum.VMF2OBJ;

public class QC {
	public String ModelName;

	public String[] BodyGroups;

	public String[] CDMaterials;

	// public String TextureGroup; //oof

	public QC(String ModelName, String[] BodyGroups, String[] CDMaterials) {
		this.ModelName = ModelName;
		this.BodyGroups = BodyGroups;
		this.CDMaterials = CDMaterials;
	}
}
