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
    return -1;    // No matching closing parenthesis
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

		//Add the original points
		//intersections.add(side.points[0]);
		//intersections.add(side.points[1]);
		//intersections.add(side.points[2]);

		for(Side side2 : solid.sides)
		{
			for(Side side3 : solid.sides)
			{
				//System.out.println(side.id);
				//System.out.println(side2.id);
				//System.out.println(side3.id);
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
					//return null;
				}
				//System.out.println(intersection);
				intersections.add((intersection));
			}
		}

		//TODO: Convex check?

		//Remove Dupes
		//Set<Vector3> set = new LinkedHashSet<Vector3>(intersections);
		//set.addAll(intersections);
		//intersections = new LinkedList<Vector3>();
		//intersections.addAll(set);

		//System.out.println();
		//System.out.println();
		//System.out.println(side.id);
		//for (Vector3 v : intersections)
		//{
		//	System.out.println(v);
		//}

		if (intersections.size() < 3)
		{
			System.out.println("Malformed side "+side.id+", only "+intersections.size()+" points");
			return null;
		}

		Side newSide = gson.fromJson(gson.toJson(side, Side.class), Side.class);
		newSide.points = intersections.toArray(new Vector3[intersections.size()]);

		if (newSide.points.length > 3) // Reorder last two vertecies
		{
			Vector3 temp = newSide.points[newSide.points.length-2];
			newSide.points[newSide.points.length-2] = newSide.points[newSide.points.length-1];
			newSide.points[newSide.points.length-1] = temp;
		}

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

			//In case of emergency break open comment
			/*
			System.out.println();
			System.out.println();
			System.out.println(plane1);
			System.out.println(plane1Normal);
			System.out.println(plane1.distance());
			System.out.println(plane2);
			System.out.println(plane2Normal);
			System.out.println(plane2.distance());
			System.out.println(plane3);
			System.out.println(plane3Normal);
			System.out.println(plane3.distance());
			System.out.println(determinant);
			System.out.println(point);
			System.out.println();
			System.out.println(Vector3.cross(plane2Normal, plane3Normal));
			System.out.println(Vector3.cross(plane2Normal, plane3Normal).multiply(plane1.distance()));
			System.out.println(Vector3.cross(plane3Normal, plane1Normal));
			System.out.println(Vector3.cross(plane3Normal, plane1Normal).multiply(plane2.distance()));
			System.out.println(Vector3.cross(plane1Normal, plane2Normal));
			System.out.println(Vector3.cross(plane1Normal, plane2Normal).multiply(plane3.distance()));
			*/

			if (point.magnitude() > 16384)
			{
				//OOB?
				//return null;
			}

			return point;
	}

	public static boolean pointInHull(Vector3 point, Side[] sides) {
		
		Vector3 sum = new Vector3();
		for (Side side : sides)
		{
			Plane plane = new Plane(side);
			sum = sum.add(plane.center());
		}
		Vector3 center = sum.divide(sides.length);

		for (Side side : sides)
		{
			Plane plane = new Plane(side);

			Vector3 direction = plane.center().subtract(center).normalize();

			if (Vector3.dot(plane.normal().normalize(), direction) < 0)
			{
				if ((point.subtract(plane.center()).dot(plane.normal().normalize().multiply(-1))) > 0.01) {
					System.out.println("Point: "+point+", "+(point.subtract(plane.center()).dot(plane.normal().normalize().multiply(-1)))+", from: "+plane);
					System.out.println(center);
					System.out.println(direction);
					System.out.println(Vector3.dot(plane.normal().normalize(), direction)+" REV");
					return false;
				};
			}
			else
			{
				if ((point.subtract(plane.center()).dot(plane.normal().normalize())) > 0.01) {
					System.out.println("Point: "+point+", "+(point.subtract(plane.center()).dot(plane.normal().normalize()))+", from: "+plane);
					System.out.println(center);
					System.out.println(direction);
					System.out.println(Vector3.dot(plane.normal().normalize(), direction));
					return false;
				};
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

		int numberOfSides = 0;
		String currentLine = "";

		//ArrayList<String> faces = new ArrayList<String>();
		//ArrayList<String> faceMaterials = new ArrayList<String>();
		//ArrayList<String> verticies = new ArrayList<String>();

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
		int i = 0;
		int vertexOffset = 1;
		System.out.println("[2/?] Writing faces...");
		
		outfile.println("# Decompiled with VMF2OBJ by Dylancyclone\n");
		outfile.println("mtllib "+matlibname);

		for (Solid solid : vmf.solids)
		{
			verticies.clear();
			faces.clear();
			int j = 0;
			for (Side side : solid.sides)
			{
				//if (side.material.contains("TOOLS/")){continue;}
				int k = 0;
				for (Vector3 point : side.points)
				{
					//System.out.println(point);
					verticies.add(point);
				}
			}
			
			Set<Vector3> uniqueVerticies = new HashSet<Vector3>(verticies);
			ArrayList<Vector3> uniqueVerticiesList = new ArrayList(uniqueVerticies);


			j = 0;
			for (Side side : solid.sides)
			{
				//if (side.material.contains("TOOLS/")){continue;}
				int k = 0;
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
		
		//
		//
		//
	/*
		System.out.println("Reading geometry...");

		while (in.hasNext())
		{
			currentLine = in.nextLine();
			for (String element : currentLine.split(" "))
			{
                if (element.equalsIgnoreCase("\t\tside"))
                {
                	numberOfSides++;
                	
                	in.nextLine();
                	in.nextLine();
                	
                	Pattern pattern = Pattern.compile("([-0-9. ()]+)");
                	Matcher matcher = pattern.matcher(in.nextLine().split("\"")[3].replaceAll("([0-9]+)", "$1.000000").replaceAll("(\\) \\()", ")("));
                    while(matcher.find()) {
                    	faces.add(matcher.group());
                    	//System.out.println(matcher.group());
                    	
                    	Pattern pattern2 = Pattern.compile("([-0-9. ]+)");
                    	Matcher matcher2 = pattern2.matcher(matcher.group());
                    	
                    	while(matcher2.find()) {
                        	verticies.add(matcher2.group());
                        	//System.out.println(matcher2.group());
                    	}
                    }
                    faceMaterials.add(in.nextLine().split("\"")[3]);
                }
            }
		}
		Set<String> uniqueFaceMaterials = new HashSet<String>(faceMaterials);
		ArrayList<String> uniqueFaceMaterialsList = new ArrayList(uniqueFaceMaterials);


		
		//Collapse Vertices
		System.out.println("Collapsing Verticies...");
		
		
		Set<String> uniqueVerticies = new HashSet<String>(verticies);
		ArrayList<String> uniqueVerticiesList = new ArrayList(uniqueVerticies);
		
		int counter = 0;
		for (String element : faces) {
			//System.out.println(element);
			
			Pattern pattern = Pattern.compile("([-0-9. ]+)");
        	Matcher matcher = pattern.matcher(element);
        	
        	String buffer = "";
        	while(matcher.find()) {
            	//verticies.add(matcher.group());
            	//System.out.println(matcher.group());
            	//System.out.println(uniqueVerticiesList.indexOf(matcher.group()));
            	buffer += uniqueVerticiesList.indexOf(matcher.group()) + 1 + " ";
        	}
        	//System.out.println(buffer);
        	faces.set(counter, buffer);
        	counter++;
		}
		//System.out.println(uniqueVerticiesList.indexOf(verticies.get(0)));
		//System.out.println(verticies.get(0));
		//System.out.println(uniqueVerticiesList.toString());
		System.out.println(faces.toString());
		System.out.println(faceMaterials.toString());
		
		
		//Write Faces
		System.out.println("Writing faces...");
		
		outfile.println("# Decompiled with VMF2OBJ by Dylancyclone\n");
		outfile.println("mtllib "+matlibname);
		outfile.println("o converted\n");
		
		for (String element : uniqueVerticiesList) {
			outfile.println("v " + element);
		}
		
		outfile.println("\n" + 
				"#64x64@0.25\n" + 
				"vt 0.000000 1.000000\n" + 
				"vt 36.000000 1.000000\n" + 
				"vt 36.000000 -35.000000\n" + 
				"vt 0.000000 -35.000000\n" + 
				"\n" + 
				"vn 0.000000 1.000000 0.000000\n" +
				"s off\n");
		

		for (String element : faces) {
			outfile.println("f " + element);
		}

		*/
		

		//Extract Models
		//Extract materials
		//Convert Materials
		//Convert models to SMD
		//Convert models to OBJ
		//Write Models
		//Write Materials
		
		
		
		
		in.close();
		outfile.close();
		materialfile.close();

	}
}
