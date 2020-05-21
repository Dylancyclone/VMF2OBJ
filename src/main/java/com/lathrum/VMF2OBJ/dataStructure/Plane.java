package com.lathrum.VMF2OBJ.dataStructure;

import com.lathrum.VMF2OBJ.dataStructure.map.Side;

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
    if (points.length < 3) {
      throw new IllegalArgumentException("Plane must have 3 points");
    }
    this.a = points[0];
    this.b = points[1];
    this.c = points[2];
  }

  public Plane(Side side) {
    if (side.points.length < 3) {
      throw new IllegalArgumentException("Plane must have 3 points");
    }
    this.a = side.points[0];
    this.b = side.points[1];
    this.c = side.points[2];
  }

  public Vector3 normal() {
    Vector3 ab = this.b.subtract(a);
    Vector3 ac = this.c.subtract(a);

    return ab.cross(ac);
  }

  public Vector3 center() {
    return this.a.add(b).add(c).divide(3);
  }

  public double distance() {
    Vector3 normal = this.normal();

    return ((this.a.x * normal.x) + (this.a.y * normal.y) + (this.a.z * normal.z))
        / Math.sqrt(Math.pow(normal.x, 2) + Math.pow(normal.y, 2) + Math.pow(normal.z, 2));
  }

  public String toString() {
    return "(" + a + "," + b + "," + c + ")";
  }

}