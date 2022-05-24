package com.lathrum.VMF2OBJ.dataStructure;

public class VectorSorter {
	final Vector3 center, normal, pp, qp;

	public VectorSorter(Vector3 normal, Vector3 center) { 
		this.center = center;
		this.normal = normal;
		Vector3 i = normal.cross(new Vector3(1,0,0));
		Vector3 j = normal.cross(new Vector3(0,1,0));
		Vector3 k = normal.cross(new Vector3(0,0,1));
		pp = i.getLonger(j).getLonger(k); // Get longest to reduce floating point imprecision
		qp = normal.cross(pp);
	} 

	public double getOrder(Vector3 vector) {
		Vector3 normalized = vector.subtract(center);
		return Math.atan2(normal.dot(normalized.cross(pp)), normal.dot(normalized.cross(qp)));
	}
}