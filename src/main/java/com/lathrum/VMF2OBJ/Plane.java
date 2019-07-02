package com.lathrum.VMF2OBJ;

public class Plane {

	public Vector3 a;
	public Vector3 b;
	public Vector3 c;

    public Plane(Vector3 a, Vector3 b, Vector3 c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public Plane(Vector3[] points) {
        if (points.length < 3)
        {
            throw new IllegalArgumentException("Plane must have 3 points");
        }
        this.a = points[0];
        this.b = points[1];
        this.c = points[2];
    }
    
    public Vector3 normal() {
        Vector3 ab = this.b.subtract(a);
        Vector3 ac = this.c.subtract(a);

        return ab.cross(ac);
    }

	public String toString() {
		return "("+a+","+b+","+c+")";
	}

}