package com.lathrum.VMF2OBJ.dataStructure.map;

import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lathrum.VMF2OBJ.VMF2OBJ;
import com.lathrum.VMF2OBJ.dataStructure.Vector3;

public class VMF {
	public Solid[] solids;
	public Entity[] entities;

	public static int findClosingBracketMatchIndex(String str, int pos) {
		if (str.charAt(pos) != '{') {
			throw new Error("No '{' at index " + pos);
		}
		int depth = 1;
		for (int i = pos + 1; i < str.length(); i++) {
			switch (str.charAt(i)) {
				case '{':
					depth++;
					break;
				case '}':
					if (--depth == 0) {
						return i;
					}
					break;
			}
		}
		return -1; // No matching closing parenthesis
	}

	public static String splice(String original, String insert, int index) {
		String begin = original.substring(0, index);
		String end = original.substring(index);
		return begin + insert + end;
	}

	public static VMF parseVMF(String text) {
		String objectRegex = "([a-zA-z._0-9]+)([{\\[])";
		String keyValueRegex = "(\"[a-zA-z._0-9]+\")(\"[^\"]*\")";
		String objectCommaRegex = "[}\\]]\"";
		String cleanUpRegex = ",([}\\]])";

		text = text.replaceAll("\\\\", "/"); // Replace backslashs with forwardslashs
		text = text.replaceAll("(?m)^\\s*//(.*)", ""); // Remove all commented lines
		text = text.replaceAll("\\x1B|#", ""); // Remove all illegal characters
		text = text.replaceAll("(\".+)[{}](.+\")", "$1$2"); // Remove brackets in quotes
		text = text.replaceAll("\"Code\"(.*)", ""); // Remove gmod Lua code
		text = text.replaceAll("[\\t\\r\\n]", ""); // Remove all whitespaces and newlines not in quotes
		text = text.replaceAll("\" \"", "\"\""); // Remove all whitespaces and newlines not in quotes
		// text = text.replaceAll("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)", ""); // Remove all whitespaces and newlines not in quotes

		String solids = "";
		Pattern solidPattern = Pattern.compile("solid\\{");
		Matcher solidMatcher = solidPattern.matcher(text);
		while (solidMatcher.find()) {
			int startIndex = solidMatcher.end() - 1;
			int endIndex = findClosingBracketMatchIndex(text, startIndex);
			String solid = text.substring(startIndex, endIndex + 1);

			text = text.replace(text.substring(solidMatcher.start(), endIndex + 1), ""); // snip this section

			String sides = "";
			Pattern sidePattern = Pattern.compile("side");
			Matcher sideMatcher = sidePattern.matcher(solid);
			while (sideMatcher.find()) {
				if (solid.charAt(sideMatcher.end()) == '{') {
					int sideStartIndex = sideMatcher.end();
					int sideEndIndex = findClosingBracketMatchIndex(solid, sideStartIndex);
					String side = solid.substring(sideStartIndex, sideEndIndex + 1);

					solid = solid.replace(solid.substring(sideMatcher.start(), sideEndIndex + 1), ""); // snip this section

					String disps = "";
					Pattern dispPattern = Pattern.compile("dispinfo");
					Matcher dispMatcher = dispPattern.matcher(side);
					while (dispMatcher.find()) {
						if (side.charAt(dispMatcher.end()) == '{') {
							int dispStartIndex = dispMatcher.end();
							int dispEndIndex = findClosingBracketMatchIndex(side, dispStartIndex);
							String disp = side.substring(dispStartIndex, dispEndIndex + 1);

							side = side.replace(side.substring(dispMatcher.start(), dispEndIndex + 1), ""); // snip this section

							String normals = "";
							Pattern normalsPattern = Pattern.compile("(?<!offset_)normals"); // Match normals but not offset_normals
							Matcher normalsMatcher = normalsPattern.matcher(disp);
							while (normalsMatcher.find()) {
								if (disp.charAt(normalsMatcher.end()) == '{') {
									int normalStartIndex = normalsMatcher.end();
									int normalEndIndex = findClosingBracketMatchIndex(disp, normalStartIndex);
									String normal = disp.substring(normalStartIndex, normalEndIndex + 1);

									disp = disp.replace(disp.substring(normalsMatcher.start(), normalEndIndex + 1), ""); // snip this section

									Pattern rowsPattern = Pattern.compile("\"row[0-9]+\"\"((?:[0-9.Ee-]+ ?)+)\"");
									Matcher rowsMatcher = rowsPattern.matcher(normal);
									while (rowsMatcher.find()) {
										String vectors = "";
										Pattern vectorPattern = Pattern.compile("([0-9.Ee-]+) ([0-9.Ee-]+) ([0-9.Ee-]+)");
										Matcher vectorMatcher = vectorPattern.matcher(rowsMatcher.group(1));
										while (vectorMatcher.find()) {
											vectors = vectors + "{\"x\":" + Double.parseDouble(vectorMatcher.group(1)) + ",\"y\":"
													+ Double.parseDouble(vectorMatcher.group(2)) + ",\"z\":"
													+ Double.parseDouble(vectorMatcher.group(3)) + "},";
										}
										normals = normals + "[" + vectors + "],";
									}
								}
								normalsMatcher = normalsPattern.matcher(disp);
							}

							String distances = "";
							Pattern distancesPattern = Pattern.compile("distances");
							Matcher distancesMatcher = distancesPattern.matcher(disp);
							while (distancesMatcher.find()) {
								if (disp.charAt(distancesMatcher.end()) == '{') {
									int distanceStartIndex = distancesMatcher.end();
									int distanceEndIndex = findClosingBracketMatchIndex(disp, distanceStartIndex);
									String distance = disp.substring(distanceStartIndex, distanceEndIndex + 1);

									disp = disp.replace(disp.substring(distancesMatcher.start(), distanceEndIndex + 1), ""); // snip this section

									Pattern rowsPattern = Pattern.compile("\"row[0-9]+\"\"((?:[0-9.Ee-]+ ?)+)\"");
									Matcher rowsMatcher = rowsPattern.matcher(distance);
									while (rowsMatcher.find()) {
										String vectors = "";
										Pattern vectorPattern = Pattern.compile("((?<!w[0-9]?)[0-9.Ee-]+)");
										Matcher vectorMatcher = vectorPattern.matcher(rowsMatcher.group(1));
										while (vectorMatcher.find()) {
											vectors = vectors + Double.parseDouble(vectorMatcher.group(1)) + ",";
										}
										distances = distances + "[" + vectors + "],";
									}
								}
								distancesMatcher = distancesPattern.matcher(disp);
							}

							String alphas = "";
							Pattern alphasPattern = Pattern.compile("alphas");
							Matcher alphasMatcher = alphasPattern.matcher(disp);
							while (alphasMatcher.find()) {
								if (disp.charAt(alphasMatcher.end()) == '{') {
									int alphaStartIndex = alphasMatcher.end();
									int alphaEndIndex = findClosingBracketMatchIndex(disp, alphaStartIndex);
									String alpha = disp.substring(alphaStartIndex, alphaEndIndex + 1);

									disp = disp.replace(disp.substring(alphasMatcher.start(), alphaEndIndex + 1), ""); // snip this section

									Pattern rowsPattern = Pattern.compile("\"row[0-9]+\"\"((?:[0-9.Ee-]+ ?)+)\"");
									Matcher rowsMatcher = rowsPattern.matcher(alpha);
									while (rowsMatcher.find()) {
										String vectors = "";
										Pattern vectorPattern = Pattern.compile("((?<!w[0-9]?)[0-9.Ee-]+)");
										Matcher vectorMatcher = vectorPattern.matcher(rowsMatcher.group(1));
										while (vectorMatcher.find()) {
											vectors = vectors + Double.parseDouble(vectorMatcher.group(1)) + ",";
										}
										alphas = alphas + "[" + vectors + "],";
									}
								}
								alphasMatcher = alphasPattern.matcher(disp);
							}

							disp = disp.replaceAll(objectRegex, ",\"$1\":$2");
							disp = disp.replaceAll(keyValueRegex, "$1:$2,");
							disp = disp.replaceAll(",,", ",");
							disp = disp.replaceAll("\"startposition\":\"\\[(.+?) (.+?) (.+?)\\]\"", "\"startposition\":{\"x\":$1,\"y\":$2,\"z\":$3}"); // Format start position
							normals = ",\"normals\":[" + normals.substring(0, normals.length() - 1) + "]";
							disp = splice(disp, normals, disp.length() - 1);
							distances = ",\"distances\":[" + distances.substring(0, distances.length() - 1) + "]";
							disp = splice(disp, distances, disp.length() - 1);
							alphas = ",\"alphas\":[" + alphas.substring(0, alphas.length() - 1) + "]";
							disp = splice(disp, alphas, disp.length() - 1);
							disp = disp.replaceAll(cleanUpRegex, "$1"); // remove commas at the end of a list
							disps = disp;
							// VMF2OBJ.logger.log(Level.FINE, disp);
						}
						dispMatcher = dispPattern.matcher(side);
					}

					side = side.replaceAll(objectRegex, ",\"$1\":$2");
					side = side.replaceAll(keyValueRegex, "$1:$2,");
					side = side.replaceAll(",,", ",");
					if (disps != "") {
						disps = "\"dispinfo\":" + disps.substring(0, disps.length() - 1) + "}";
						side = splice(side, disps, side.length() - 1);
					}
					side = side.replaceAll(cleanUpRegex, "$1"); // remove commas at the end of a list
					sides = sides + side + ",";
				}
				sideMatcher = sidePattern.matcher(solid);
			}
			solid = solid.replaceAll(objectRegex, "\"$1\":$2");
			solid = solid.replaceAll(keyValueRegex, "$1:$2,");
			solid = solid.replaceAll(objectCommaRegex, "},\"");
			sides = ",\"sides\":[" + sides.substring(0, sides.length() - 1) + "]";
			solid = splice(solid, sides, solid.length() - 1);
			solid = solid.replaceAll(cleanUpRegex, "$1"); // remove commas at the end of a list

			solids = solids + solid + ",";
			solidMatcher = solidPattern.matcher(text);
		}

		String entities = "";
		Pattern entityPattern = Pattern.compile("entity");
		Matcher entityMatcher = entityPattern.matcher(text);
		while (entityMatcher.find()) {
			if (text.charAt(entityMatcher.end()) == '{') {
				int startIndex = entityMatcher.end();
				int endIndex = findClosingBracketMatchIndex(text, startIndex);
				String entity = text.substring(startIndex, endIndex + 1);

				text = text.replace(text.substring(entityMatcher.start(), endIndex + 1), ""); // snip this section

				entity = entity.replaceAll(objectRegex, "\"$1\":$2");
				entity = entity.replaceAll(keyValueRegex, "$1:$2,");
				entity = entity.replaceAll(objectCommaRegex, "},\"");
				entity = entity.replaceAll(cleanUpRegex, "$1"); // remove commas at the end of a list

				entities = entities + entity + ",";
			}
			entityMatcher = entityPattern.matcher(text);
		}

		text = text.replaceAll(objectRegex, "\"$1\":$2");
		text = text.replaceAll(keyValueRegex, "$1:$2,");
		text = text.replaceAll(objectCommaRegex, "},\"");
		if (solids != "") {
			solids = ",\"solids\":[" + solids.substring(0, solids.length() - 1) + "]";
		}
		if (entities != "") {
			entities = ",\"entities\":[" + entities.substring(0, entities.length() - 1) + "]";
		}
		text = "{" + text + solids + entities + "}";
		text = text.replaceAll(cleanUpRegex, "$1"); // remove commas at the end of a list
		text = text.replaceAll(",,", ",");

		// VMF2OBJ.logger.log(Level.FINE, text);
		VMF vmf = VMF2OBJ.gson.fromJson(text, VMF.class);
		return vmf;
	}

	public static VMF parseSolids(VMF vmf) {
		if (vmf.solids == null) {
			return vmf;
		} // There are no brushes in this VMF
		String planeRegex = "\\((.+?) (.+?) (.+?)\\) \\((.+?) (.+?) (.+?)\\) \\((.+?) (.+?) (.+?)\\)";
		Pattern planePattern = Pattern.compile(planeRegex);
		Matcher planeMatch;

		String uvRegex = "\\[(.+?) (.+?) (.+?) (.+?)\\] (.+)";
		Pattern uvPattern = Pattern.compile(uvRegex);
		Matcher uvMatch;

		int i = 0;
		for (Solid solid : vmf.solids) {
			int j = 0;
			for (Side side : solid.sides) {
				planeMatch = planePattern.matcher(side.plane);
				Collection<Vector3> vectors = new LinkedList<Vector3>();
				if (planeMatch.find())
				{
					vectors.add(new Vector3(
						Double.parseDouble(planeMatch.group(1)),
						Double.parseDouble(planeMatch.group(2)),
						Double.parseDouble(planeMatch.group(3))
					));
					vectors.add(new Vector3(
						Double.parseDouble(planeMatch.group(4)),
						Double.parseDouble(planeMatch.group(5)),
						Double.parseDouble(planeMatch.group(6))
					));
					vectors.add(new Vector3(
						Double.parseDouble(planeMatch.group(7)),
						Double.parseDouble(planeMatch.group(8)),
						Double.parseDouble(planeMatch.group(9))
					));
				}
				vmf.solids[i].sides[j].points = vectors.toArray(new Vector3[vectors.size()]);

				uvMatch = uvPattern.matcher(side.uaxis);
				if (uvMatch.find())
				{
					vmf.solids[i].sides[j].uAxisVector = new Vector3(
						Double.parseDouble(uvMatch.group(1)),
						Double.parseDouble(uvMatch.group(2)),
						Double.parseDouble(uvMatch.group(3)));
						vmf.solids[i].sides[j].uAxisTranslation = Double.parseDouble(uvMatch.group(4));
						vmf.solids[i].sides[j].uAxisScale = Double.parseDouble(uvMatch.group(5));
				}
				uvMatch = uvPattern.matcher(side.vaxis);
				if (uvMatch.find())
				{
					vmf.solids[i].sides[j].vAxisVector = new Vector3(
						Double.parseDouble(uvMatch.group(1)),
						Double.parseDouble(uvMatch.group(2)),
						Double.parseDouble(uvMatch.group(3)));
						vmf.solids[i].sides[j].vAxisTranslation = Double.parseDouble(uvMatch.group(4));
						vmf.solids[i].sides[j].vAxisScale = Double.parseDouble(uvMatch.group(5));
				}
				j++;
			}

			j = 0;
			Solid solidProxy = VMF2OBJ.gson.fromJson(VMF2OBJ.gson.toJson(solid, Solid.class), Solid.class);
			for (Side side : solidProxy.sides) {
				Side newSide = Side.completeSide(side, solidProxy);
				if (newSide != null) {
					vmf.solids[i].sides[j] = newSide;
				} else {
					// System.arraycopy(vmf.solids[i].sides, j + 1, vmf.solids[i].sides, j,
					// vmf.solids[i].sides.length - 1 - j); //Remove invalid side
				}
				j++;
			}
			i++;
		}

		return vmf;
	}


}
