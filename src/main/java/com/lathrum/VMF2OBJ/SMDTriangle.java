package com.lathrum.VMF2OBJ;

import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SMDTriangle {
	public String materialName;

	public SMDPoint[] points;

  public SMDTriangle(String materialName, SMDPoint[] points) {
		this.materialName = materialName;
		this.points = points;
	}

	public static SMDTriangle[] parseSMD(String text) {

		// Aha! Finally a format that should be consistant!

		Matcher triangleMatcher = Pattern.compile(
				"([a-zA-Z0-9_]+)\\R  ([0-9]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+)\\R  ([0-9]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+)\\R  ([0-9]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+)\\R")
				.matcher(text);
		Collection<SMDTriangle> SMDTriangles = new LinkedList<SMDTriangle>();
		while (triangleMatcher.find()) {
			SMDPoint[] points = new SMDPoint[3];
			for (int i = 0; i < 3; i++) {
				points[i] = new SMDPoint(
					new Vector3(Double.parseDouble(triangleMatcher.group(12*i+3)), Double.parseDouble(triangleMatcher.group(12*i+4)), Double.parseDouble(triangleMatcher.group(12*i+5))),
					new Vector3(Double.parseDouble(triangleMatcher.group(12*i+6)), Double.parseDouble(triangleMatcher.group(12*i+7)), Double.parseDouble(triangleMatcher.group(12*i+8))),
					triangleMatcher.group(12*i+9),
					triangleMatcher.group(12*i+10)
				);
			}
			SMDTriangles.add(new SMDTriangle(triangleMatcher.group(1), points));
		}

		return SMDTriangles.toArray(new SMDTriangle[SMDTriangles.size()]);
	}

}
