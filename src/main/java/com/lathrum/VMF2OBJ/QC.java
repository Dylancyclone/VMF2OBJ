package com.lathrum.VMF2OBJ;

public class QC {
	public String ModelName;
	
	public String[] BodyGroup;

	public String[] CDMaterials;

	//public String TextureGroup; //oof

	
  public QC(String ModelName, String[] BodyGroup, String[] CDMaterials) {
		this.ModelName = ModelName;
		this.BodyGroup = BodyGroup;
		this.CDMaterials = CDMaterials;
	}
}
