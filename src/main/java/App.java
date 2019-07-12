import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import com.lathrum.VMF2OBJ.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class App {

	public static Gson gson = new Gson();

	static String readFile(String path) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, StandardCharsets.UTF_8);
	}

	public static VMF parseVMF(String text) {

		String objectRegex = "([a-zA-z._0-9]+)([{\\[])";
		String keyValueRegex = "(\"[a-zA-z._0-9]+\")(\"[^\"]*\")";
		String objectCommaRegex = "[}\\]]\"";
		String cleanUpRegex = ",([}\\]])";

		text = text.replaceAll("\\\\", "/"); // Replace backslashs with forwardslashs
		text = text.replaceAll("//(.*)", ""); // Remove all commented lines
		text = text.replaceAll("\\x1B", ""); // Remove all illegal characters
		text = text.replaceAll("[\\t\\r\\n]", ""); // Remove all whitespaces and newlines not in quotes
		text = text.replaceAll("\" \"", "\"\""); // Remove all whitespaces and newlines not in quotes
		//text = text.replaceAll("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)", ""); // Remove all whitespaces and newlines not in quotes
		
		String solids = "";
		Pattern solidPattern = Pattern.compile("solid\\{");
		Matcher solidMatcher = solidPattern.matcher(text);
		while (solidMatcher.find()) {
			int startIndex = solidMatcher.end()-1;
			int endIndex = findClosingBracketMatchIndex(text,startIndex);
			String solid = text.substring(startIndex,endIndex+1);

			text = text.replace(text.substring(solidMatcher.start(),endIndex+1),""); //snip this section

			String sides = "";
			Pattern sidePattern = Pattern.compile("side");
			Matcher sideMatcher = sidePattern.matcher(solid);
			while (sideMatcher.find()) {
				if (solid.charAt(sideMatcher.end()) == '{')
				{
					int sideStartIndex = sideMatcher.end();
					int sideEndIndex = findClosingBracketMatchIndex(solid,sideStartIndex);
					String side = solid.substring(sideStartIndex,sideEndIndex+1);
	
					solid = solid.replace(solid.substring(sideMatcher.start(),sideEndIndex+1),""); //snip this section
					side = side.replaceAll(objectRegex,",\"$1\":$2");
					side = side.replaceAll(keyValueRegex,"$1:$2,");
					side = side.replaceAll(",,",",");
					sides = sides+side+",";
				}
				sideMatcher = sidePattern.matcher(solid);
			}
			solid = solid.replaceAll(objectRegex,"\"$1\":$2");
			solid = solid.replaceAll(keyValueRegex,"$1:$2,");
			solid = solid.replaceAll(objectCommaRegex,"},\"");
			sides = ",\"sides\":["+sides.substring(0,sides.length()-1)+"]";
			solid = splice(solid,sides,solid.length()-1);
			solid = solid.replaceAll(cleanUpRegex,"$1"); //remove commas at the end of a list
			
			solids = solids+solid+",";
			solidMatcher = solidPattern.matcher(text);
		}
		
		String entities = "";
		Pattern entityPattern = Pattern.compile("entity");
		Matcher entityMatcher = entityPattern.matcher(text);
		while (entityMatcher.find()) {
			if (text.charAt(entityMatcher.end()) == '{')
      {
        int startIndex = entityMatcher.end();
        int endIndex = findClosingBracketMatchIndex(text,startIndex);
				String entity = text.substring(startIndex,endIndex+1);
				
        text = text.replace(text.substring(entityMatcher.start(),endIndex+1),""); //snip this section

				entity = entity.replaceAll(objectRegex,"\"$1\":$2");
				entity = entity.replaceAll(keyValueRegex,"$1:$2,");
				entity = entity.replaceAll(objectCommaRegex,"},\"");
				entity = entity.replaceAll(cleanUpRegex,"$1"); //remove commas at the end of a list

				entities = entities+entity+",";
			}
			entityMatcher = entityPattern.matcher(text);
		}
		
		text = text.replaceAll(objectRegex,"\"$1\":$2");
		text = text.replaceAll(keyValueRegex,"$1:$2,");
		text = text.replaceAll(objectCommaRegex,"},\"");
		if (solids != "")
		{
			solids = ",\"solids\":["+solids.substring(0,solids.length()-1)+"]";
		}
		if (entities != "")
		{
			entities = ",\"entities\":["+entities.substring(0,entities.length()-1)+"]";
		}
		text = "{"+text+solids+entities+"}";
		text = text.replaceAll(cleanUpRegex,"$1"); //remove commas at the end of a list

		VMF vmf = gson.fromJson(text, VMF.class);
		return vmf;
	}

	
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
    String begin = original.substring(0,index);
    String end = original.substring(index);
    return begin + insert + end;
	}
	
	public static VMF parseSolids(VMF vmf) {
		String planeRegex = "\\((.+?) (.+?) (.+?)\\) \\((.+?) (.+?) (.+?)\\) \\((.+?) (.+?) (.+?)\\)";
		Pattern planePattern = Pattern.compile(planeRegex);
		Matcher planeMatch;

		String uvRegex = "\\[(.+?) (.+?) (.+?) (.+?)\\] (.+)";
		Pattern uvPattern = Pattern.compile(uvRegex);
		Matcher uvMatch;

		int i = 0;
		for (Solid solid : vmf.solids)
		{
			int j = 0;
			for (Side side : solid.sides)
			{
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
						Double.parseDouble(uvMatch.group(3)),
						Double.parseDouble(uvMatch.group(2)));
						vmf.solids[i].sides[j].uAxisTranslation = Double.parseDouble(uvMatch.group(4));
						vmf.solids[i].sides[j].uAxisScale = Double.parseDouble(uvMatch.group(5));
				}
				uvMatch = uvPattern.matcher(side.vaxis);
				if (uvMatch.find())
				{
					vmf.solids[i].sides[j].vAxisVector = new Vector3(
						Double.parseDouble(uvMatch.group(1)),
						Double.parseDouble(uvMatch.group(3)),
						Double.parseDouble(uvMatch.group(2)));
						vmf.solids[i].sides[j].vAxisTranslation = Double.parseDouble(uvMatch.group(4));
						vmf.solids[i].sides[j].vAxisScale = Double.parseDouble(uvMatch.group(5));
				}
				j++;
			}
			
			j = 0;
			Solid solidProxy = gson.fromJson(gson.toJson(solid, Solid.class), Solid.class);
			for (Side side : solidProxy.sides)
			{
				Side newSide = completeSide(side, solidProxy);
				if (newSide != null)
				{
					vmf.solids[i].sides[j] = newSide;
				}
				else
				{
					//System.arraycopy(vmf.solids[i].sides, j + 1, vmf.solids[i].sides, j, vmf.solids[i].sides.length - 1 - j); //Remove invalid side
				}
				j++;
			}
			i++;
		}

		return vmf;
	}

	
	public static Side completeSide(Side side, Solid solid) {

		Collection<Vector3> intersections = new LinkedList<Vector3>();

		for(Side side2 : solid.sides)
		{
			for(Side side3 : solid.sides)
			{
				Vector3 intersection = GetPlaneIntersectionPoint(side.points, side2.points, side3.points);

				if (intersection == null)
				{
					continue;
				}

				if (intersections.contains(intersection))
				{
					continue;
				}
				
				if (!pointInHull(intersection, solid.sides))
				{
					continue;
				}
				intersections.add((intersection));
			}
		}

		// TODO: Convex check?
		// Theoretically source only allows convex shapes, and fixes any problems upon saving...

		if (intersections.size() < 3)
		{
			System.out.println("Malformed side "+side.id+", only "+intersections.size()+" points");
			return null;
		}

		Vector3 sum = new Vector3();
		for (Vector3 point : intersections)
		{
			sum = sum.add(point);
		}
		final Vector3 center = sum.divide(intersections.size());
		final Vector3 normal = new Plane(side).normal().normalize();
		
		List<Vector3> IntersectionsList = new ArrayList<Vector3>(intersections);
		Collections.sort(IntersectionsList, new Comparator<Vector3>() {
			@Override
			public int compare(Vector3 o1, Vector3 o2) {
				double det = Vector3.dot(normal, Vector3.cross(o1.subtract(center), o2.subtract(center)));
				if (det < 0){
					return -1;
				}
				if (det > 0){
					return 1;
				}

				// If 0, then they are colinear, just select which point is further from the center
				double d1 = o1.subtract(center).magnitude();
				double d2 = o2.subtract(center).magnitude();
				if (d1<d2) {
					return -1;
				}
				else {
					return 1;
				}
			}
		});

		Side newSide = gson.fromJson(gson.toJson(side, Side.class), Side.class);

		newSide.points = IntersectionsList.toArray(new Vector3[IntersectionsList.size()]);

		return newSide;
	}

	public static Vector3 GetPlaneIntersectionPoint(Vector3[] side1, Vector3[] side2, Vector3[] side3)
	{
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

			if ((determinant <= 0.01 && determinant >= -0.01) || Double.isNaN(determinant))
			{
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

		for (Side side : sides)
		{
			Plane plane = new Plane(side);
			Vector3 facing = point.subtract(plane.center()).normalize();

			if (Vector3.dot(facing, plane.normal().normalize()) < -0.01) {
				return false;
			}
		}

    return true;
}

	public static void main(String args[]) {
		// Read Geometry
		// Collapse Vertices
		// Write objects
		// Extract Models
		// Extract materials
		// Convert Materials
		// Convert models to SMD
		// Convert models to OBJ
		// Write Models
		// Write Materials

		if (args.length != 2) {
			System.err.println("Usage: java vmf2obj <infile> <outpath>");
			return;
		}

		Scanner in;
		PrintWriter outfile;
		PrintWriter materialfile;
		String objname = args[1] + ".obj";
		String matlibname = args[1] + ".mtl";

		// Open file
		File workingFile = new File(args[0]);
		if (!workingFile.exists()) {
			try {
				File directory = new File(workingFile.getParent());
				if (!directory.exists()) {
					directory.mkdirs();
				}
				workingFile.createNewFile();
			} catch (IOException e) {
				System.out.println("Excepton Occured: " + e.toString());
			}
		}

		// Read File
		String text = "";
		try {
			text = readFile(args[0]);
		}catch (IOException e) {
			System.out.println("Excepton Occured: " + e.toString());
		}
		//System.out.println(text);

		try
		{
			File directory = new File(new File(args[1]).getParent());
			if (!directory.exists()) {
					directory.mkdirs();
			}
			
			in = new Scanner(new File(args[0]));
			outfile = new PrintWriter(new FileOutputStream(objname));
			materialfile = new PrintWriter(new FileOutputStream(matlibname));
		}
		catch(IOException e)
		{
			System.err.println("Error while opening file: "+e.getMessage());
			return;
		}

		//
		// Read Geometry
		//

		System.out.println("[1/?] Reading geometry...");

		VMF vmf = parseVMF(text);
		vmf = parseSolids(vmf);
		//System.out.println(gson.toJson(vmf));

		ArrayList<Vector3> verticies = new ArrayList<Vector3>();
		ArrayList<String> faces = new ArrayList<String>();
		int vertexOffset = 1;
		System.out.println("[2/?] Writing faces...");
		
		outfile.println("# Decompiled with VMF2OBJ by Dylancyclone\n");
		outfile.println("mtllib "+matlibname);

		for (Solid solid : vmf.solids)
		{
			verticies.clear();
			faces.clear();
			for (Side side : solid.sides)
			{
				//if (side.material.contains("TOOLS/")){continue;}
				for (Vector3 point : side.points)
				{
					//System.out.println(point);
					verticies.add(point);
				}
			}
			
			//TODO: Margin of error?
			Set<Vector3> uniqueVerticies = new HashSet<Vector3>(verticies);
			ArrayList<Vector3> uniqueVerticiesList = new ArrayList<Vector3>(uniqueVerticies);

			for (Side side : solid.sides)
			{
				//if (side.material.contains("TOOLS/")){continue;}
				String buffer = "";
				for (Vector3 point : side.points)
				{
					buffer += (uniqueVerticiesList.indexOf(point) + vertexOffset) + " ";
				}
				faces.add(buffer);
			}
			vertexOffset += uniqueVerticiesList.size();
			
			//Write Faces
			
			outfile.println("\n");
			outfile.println("o "+solid.id+"\n");
			
			for (Vector3 e : uniqueVerticiesList) {
				outfile.println("v " + e.x +" "+ e.y +" "+ e.z);
			}
			
			//outfile.println("\n" + 
			//		"#64x64@0.25\n" + 
			//		"vt 0.000000 1.000000\n" + 
			//		"vt 36.000000 1.000000\n" + 
			//		"vt 36.000000 -35.000000\n" + 
			//		"vt 0.000000 -35.000000\n" + 
			//		"\n" + 
			//		"vn 0.000000 1.000000 0.000000\n" +
			//		"s off\n");
			outfile.println();
			

			for (String element : faces) {
				outfile.println("f " + element);
			}
		}
		
		in.close();
		outfile.close();
		materialfile.close();

	}
}
