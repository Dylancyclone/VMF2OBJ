package com.lathrum.VMF2OBJ;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;

public class Side {
	public String id;

	public String plane;
	public Vector3[] points;

	public String material;

	public String uaxis;
	public Vector3 uAxisVector;
	public double uAxisTranslation;
	public double uAxisScale;

	public String vaxis;
	public Vector3 vAxisVector;
	public double vAxisTranslation;
	public double vAxisScale;

	public Displacement dispinfo;

	public static Side completeSide(Side side, Solid solid) {
		Gson gson = new Gson();
		Collection<Vector3> intersections = new LinkedList<Vector3>();

		for (Side side2 : solid.sides) {
			for (Side side3 : solid.sides) {
				Vector3 intersection = Vector3.GetPlaneIntersectionPoint(side.points, side2.points, side3.points);

				if (intersection == null) {
					continue;
				}

				if (intersections.contains(intersection)) {
					continue;
				}

				if (!Vector3.pointInHull(intersection, solid.sides)) {
					continue;
				}
				intersections.add((intersection));
			}
		}

		// Theoretically source only allows convex shapes, and fixes any problems upon saving...

		if (intersections.size() < 3) {
			System.out.println("Malformed side " + side.id + ", only " + intersections.size() + " points");
			return null;
		}

		Vector3 sum = new Vector3();
		for (Vector3 point : intersections) {
			sum = sum.add(point);
		}
		final Vector3 center = sum.divide(intersections.size());
		final Vector3 normal = new Plane(side).normal().normalize();

		List<Vector3> IntersectionsList = new ArrayList<Vector3>(intersections);
		Collections.sort(IntersectionsList, new Comparator<Vector3>() {
			@Override
			public int compare(Vector3 o1, Vector3 o2) {
				double det = Vector3.dot(normal, Vector3.cross(o1.subtract(center), o2.subtract(center)));
				if (det < 0) {
					return -1;
				}
				if (det > 0) {
					return 1;
				}

				// If 0, then they are colinear, just select which point is further from the
				// center
				double d1 = o1.subtract(center).magnitude();
				double d2 = o2.subtract(center).magnitude();
				if (d1 < d2) {
					return -1;
				} else {
					return 1;
				}
			}
		});

		Side newSide = gson.fromJson(gson.toJson(side, Side.class), Side.class);

		newSide.points = IntersectionsList.toArray(new Vector3[IntersectionsList.size()]);

		return newSide;
	}

}
