package com.lathrum.VMF2OBJ.dataStructure.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import com.lathrum.VMF2OBJ.VMF2OBJ;
import com.lathrum.VMF2OBJ.dataStructure.Plane;
import com.lathrum.VMF2OBJ.dataStructure.Vector3;
import com.lathrum.VMF2OBJ.dataStructure.VectorSorter;

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

		// Theoretically source only allows convex shapes, and fixes any problems upon
		// saving...

		if (intersections.size() < 3) {
			VMF2OBJ.logger.log(Level.WARNING, "Malformed side " + side.id + ", only " + intersections.size() + " points");
			return null;
		}

		Vector3 sum = new Vector3();
		for (Vector3 point : intersections) {
			sum = sum.add(point);
		}
		final Vector3 center = sum.divide(intersections.size());
		final Vector3 normal = new Plane(side).normal().normalize();

		List<Vector3> IntersectionsList = new ArrayList<Vector3>(intersections);
		VectorSorter sorter = new VectorSorter(normal, center);
		Collections.sort(IntersectionsList, new Comparator<Vector3>() {
			@Override
			public int compare(Vector3 o1, Vector3 o2) {
				return ((Double) sorter.getOrder(o1)).compareTo((Double) sorter.getOrder(o2));
			}
		});

		Side newSide = VMF2OBJ.gson.fromJson(VMF2OBJ.gson.toJson(side, Side.class), Side.class);

		newSide.points = IntersectionsList.toArray(new Vector3[IntersectionsList.size()]);

		return newSide;
	}

}
