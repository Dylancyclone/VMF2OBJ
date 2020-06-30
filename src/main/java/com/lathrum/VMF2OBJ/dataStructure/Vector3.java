package com.lathrum.VMF2OBJ.dataStructure;

import com.lathrum.VMF2OBJ.dataStructure.map.Side;

public class Vector3 {

	public double x;
	public double y;
	public double z;

	public Vector3() {
		this.x = 0.0;
		this.y = 0.0;
		this.z = 0.0;
	}
  public Vector3(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	//No Operator Overloading :(
	public static Vector3 add(Vector3 tip, Vector3 tail) {
		return new Vector3(
			tip.x+tail.x,
			tip.y+tail.y,
			tip.z+tail.z);
	}
	public Vector3 add(Vector3 vector) {
		return new Vector3(
			this.x+vector.x,
			this.y+vector.y,
			this.z+vector.z);
	}
	public Vector3 add(double num) {
		return new Vector3(
			this.x+num,
			this.y+num,
			this.z+num);
	}
	public static Vector3 subtract(Vector3 tip, Vector3 tail) {
		return new Vector3(
			tip.x-tail.x,
			tip.y-tail.y,
			tip.z-tail.z);
	}
	public Vector3 subtract(Vector3 vector) {
		return new Vector3(
			this.x-vector.x,
			this.y-vector.y,
			this.z-vector.z);
	}
	public Vector3 subtract(double num) {
		return new Vector3(
			this.x-num,
			this.y-num,
			this.z-num);
	}
	public static Vector3 multiply(Vector3 vectorA, Vector3 vectorB) {
		return new Vector3(
			vectorA.x*vectorB.x,
			vectorA.y*vectorB.y,
			vectorA.z*vectorB.z);
	}
	public Vector3 multiply(Vector3 vector) {
		return new Vector3(
			this.x*vector.x,
			this.y*vector.y,
			this.z*vector.z);
	}
	public Vector3 multiply(double num) {
		return new Vector3(
			this.x*num,
			this.y*num,
			this.z*num);
	}
	public static Vector3 divide(Vector3 vectorA, Vector3 vectorB) {
		return new Vector3(
			vectorA.x/vectorB.x,
			vectorA.y/vectorB.y,
			vectorA.z/vectorB.z);
	}
	public Vector3 divide(Vector3 vector) {
		return new Vector3(
			this.x/vector.x,
			this.y/vector.y,
			this.z/vector.z);
	}
	public Vector3 divide(double num) {
		return new Vector3(
			this.x/num,
			this.y/num,
			this.z/num);
	}

	
	public static double magnitude(Vector3 vector) {
		return Math.sqrt(Math.pow(vector.x,2)+Math.pow(vector.y,2)+Math.pow(vector.z,2));
	}
	public double magnitude() {
		return Math.sqrt(Math.pow(this.x,2)+Math.pow(this.y,2)+Math.pow(this.z,2));
	}

	
	public static Vector3 abs(Vector3 vector) {
		if (vector.x < 0)
			vector.x = -vector.x;
		if (vector.y < 0)
			vector.y = -vector.y;
		if (vector.z < 0)
			vector.z = -vector.z;
		return vector;
	}
	public Vector3 abs() {
		if (this.x < 0)
			this.x = -this.x;
		if (this.y < 0)
			this.y = -this.y;
		if (this.z < 0)
			this.z = -this.z;
		return this;
	}

	
	public static Vector3 cross(Vector3 vectorA, Vector3 vectorB) {
		return new Vector3(
			vectorA.y * vectorB.z - vectorA.z * vectorB.y,
			vectorA.z * vectorB.x - vectorA.x * vectorB.z,
			vectorA.x * vectorB.y - vectorA.y * vectorB.x);
	}
	public Vector3 cross(Vector3 vector) {
		return new Vector3(
			this.y * vector.z - this.z * vector.y,
			this.z * vector.x - this.x * vector.z,
			this.x * vector.y - this.y * vector.x);
	}

	public static double dot(Vector3 vectorA, Vector3 vectorB) {
		return (vectorA.x * vectorB.x) + (vectorA.y * vectorB.y) + (vectorA.z * vectorB.z);
	}
	public double dot(Vector3 vector) {
		return (this.x * vector.x) + (this.y * vector.y) + (this.z * vector.z);
	}

	
	public static Vector3 rotateX(Vector3 vector, double rad) {
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		return new Vector3(
			vector.x,
			vector.y*cos-vector.z*sin,
			vector.z*cos+vector.y*sin);
	}
	public Vector3 rotateX(double rad) {
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		return new Vector3(
			this.x,
			this.y*cos-this.z*sin,
			this.z*cos+this.y*sin);
	}
	
	public static Vector3 rotateY(Vector3 vector, double rad) {
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		return new Vector3(
			vector.x*cos-vector.z*sin,
			vector.y,
			vector.z*cos+vector.x*sin);
	}
	public Vector3 rotateY(double rad) {
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		return new Vector3(
			this.x*cos-this.z*sin,
			this.y,
			this.z*cos+this.x*sin);
	}
	
	public static Vector3 rotateZ(Vector3 vector, double rad) {
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		return new Vector3(
			vector.x*cos-vector.y*sin,
			vector.y*cos+vector.x*sin,
			vector.z);
	}
	public Vector3 rotateZ(double rad) {
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		return new Vector3(
			this.x*cos-this.y*sin,
			this.y*cos+this.x*sin,
			this.z);
	}
	
	public static Vector3 rotate3D(Vector3 vector, double radX, double radY, double radZ) {
		return vector.rotateX(radX).rotateY(radY).rotateZ(radZ);
	}
	public Vector3 rotate3D(double radX, double radY, double radZ) {
		return this.rotateX(radX).rotateY(radY).rotateZ(radZ);
	}

	public static Vector3 normalize(Vector3 vector) {
		double length = vector.magnitude();
		return vector.divide(new Vector3(length,length,length));
	}
	public Vector3 normalize() {
		double length = this.magnitude();
		return this.divide(new Vector3(length,length,length));
	}

	public static double distance(Vector3 vectorA, Vector3 vectorB) {
		return Math.sqrt(
				Math.pow(vectorA.x - vectorB.x, 2) + Math.pow(vectorA.y - vectorB.y, 2) + Math.pow(vectorA.z - vectorB.z, 2));
	}

	public double distance(Vector3 vector) {
		return Math.sqrt(Math.pow(this.x - vector.x, 2) + Math.pow(this.y - vector.y, 2) + Math.pow(this.z - vector.z, 2));
	}

	public int closestIndex(Vector3[] vectors) {
		if (vectors.length == 0) {
			return -1;
		} else if (vectors.length == 1) {
			return 0;
		} else {
			int index = 0;
			double distance = this.distance(vectors[0]);
			for (int i = 1; i < vectors.length; i++) {
				double thisDistance = this.distance(vectors[i]);
				if (thisDistance < distance) {
					index = i;
					distance = thisDistance;
				}
			}
			return index;
		}
	}

	public static Vector3 GetPlaneIntersectionPoint(Vector3[] side1, Vector3[] side2, Vector3[] side3) {
		Plane plane1 = new Plane(side1);
		Vector3 plane1Normal = plane1.normal().normalize();
		Plane plane2 = new Plane(side2);
		Vector3 plane2Normal = plane2.normal().normalize();
		Plane plane3 = new Plane(side3);
		Vector3 plane3Normal = plane3.normal().normalize();
		double determinant =
			(
				(
					plane1Normal.x * plane2Normal.y * plane3Normal.z +
					plane1Normal.y * plane2Normal.z * plane3Normal.x +
					plane1Normal.z * plane2Normal.x * plane3Normal.y
				)
				-
				(
					plane1Normal.z * plane2Normal.y * plane3Normal.x +
					plane1Normal.y * plane2Normal.x * plane3Normal.z +
					plane1Normal.x * plane2Normal.z * plane3Normal.y
				)
			);

		// Can't intersect parallel planes.

		if ((determinant <= 0.01 && determinant >= -0.01) || Double.isNaN(determinant)) {
			return null;
		}

		Vector3 point =
		(
			Vector3.cross(plane2Normal, plane3Normal).multiply(plane1.distance()).add(
			Vector3.cross(plane3Normal, plane1Normal).multiply(plane2.distance())).add(
			Vector3.cross(plane1Normal, plane2Normal).multiply(plane3.distance()))
		)
		.divide(determinant);

		return point;
	}

	public static boolean pointInHull(Vector3 point, Side[] sides) {

		for (Side side : sides) {
			Plane plane = new Plane(side);
			Vector3 facing = point.subtract(plane.center()).normalize();

			if (Vector3.dot(facing, plane.normal().normalize()) < -0.01) {
				return false;
			}
		}

		return true;
	}

	public String toString() {
		return "("+x+","+y+","+z+")";
	}

	public boolean equals(Object obj) {
		if (obj instanceof Vector3) {
			return ((this.x*31 + this.y)*31 + this.z) == ((((Vector3) obj).x*31 + ((Vector3) obj).y)*31 + ((Vector3) obj).z);
		}
		return false;
	}

	public int hashCode() {
		return Math.round(Math.round((x*31 + y)*31 + z));
	}

}