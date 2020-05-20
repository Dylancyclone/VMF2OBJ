package com.lathrum.VMF2OBJ;

public class SMDPoint {
	public Vector3 position;
	public Vector3 normal;

	public String uaxis;
	public String vaxis;

	public SMDPoint(Vector3 position, Vector3 normal, String uaxis, String vaxis) {
		this.position = position;
		this.normal = normal;
		this.uaxis = uaxis;
		this.vaxis = vaxis;
	}
}
