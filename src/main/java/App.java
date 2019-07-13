import java.util.*;
import java.util.zip.*;
import java.util.regex.*;
import com.google.gson.*;
import com.lathrum.VMF2OBJ.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.awt.image.BufferedImage;
import javax.imageio.*;

public class App {

	public static Gson gson = new Gson();
	public static Process proc;
	public static String VTFLibPath;

	public static void extractLibraries(String dir) throws URISyntaxException {
		//Spooky scary, but I don't want to reinvent the wheel.
		ArrayList<String> files = new ArrayList<String>();
		File tempFolder = new File(dir);
		tempFolder.mkdirs();
		tempFolder.deleteOnExit();

		//VTFLIB
		//http://nemesis.thewavelength.net/index.php?p=40
		//For converting VTF file to TGA files
		files.add("DevIL.dll"); //VTFLib dependency
		files.add("VTFLib.dll"); //VTFLib dependency
		files.add("VTFCmd.exe"); //VTFLib itself

		URI uri = new URI("");
		URI fileURI;

		try {
			uri = App.class.getProtectionDomain().getCodeSource().getLocation().toURI();
		}
		catch (Exception e) {
			System.err.println("Exception: "+e);
		}

		for (String el : files)
		{
			ZipFile zipFile;

			try {
				zipFile = new ZipFile(new File(uri));

				try
				{
					fileURI = extractFile(zipFile, el, dir);

					switch (el){
						case ("VTFCmd.exe"):
							VTFLibPath = Paths.get(fileURI).toString();
							break;
					}
				}
				finally
				{
					zipFile.close();
				}
			}
			catch (Exception e) {
				System.err.println("Exception: "+e);
			}
		}
	}

	public static URI extractFile(ZipFile zipFile, String fileName, String dir) throws IOException {
			File tempFile;
			ZipEntry entry;
			InputStream zipStream;
			OutputStream fileStream;

			tempFile = new File(dir+File.separator+fileName);
			tempFile.createNewFile();
			tempFile.deleteOnExit();
			entry = zipFile.getEntry(fileName);

			if(entry == null)
			{
				throw new FileNotFoundException("cannot find file: " + fileName + " in archive: " + zipFile.getName());
			}

			zipStream  = zipFile.getInputStream(entry);
			fileStream = null;

			try
			{
				final byte[] buf;
				int i;

				fileStream = new FileOutputStream(tempFile);
				buf = new byte[1024];
				i = 0;

				while((i = zipStream.read(buf)) != -1)
				{
					fileStream.write(buf, 0, i);
				}
			}
			finally
			{
				zipStream.close();
				fileStream.close();
			}

			return (tempFile.toURI());
	}

	static String readFile(String path) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, StandardCharsets.UTF_8);
	}
	
	public static String formatPath(String res) {
		if (res==null) return null;
		if (File.separatorChar=='\\') {
				// From Windows to Linux/Mac
				return res.replace('/', File.separatorChar);
		} else {
				// From Linux/Mac to Windows
				return res.replace('\\', File.separatorChar);
		}
	}

	public static VMF parseVMF(String text) {

		String objectRegex = "([a-zA-z._0-9]+)([{\\[])";
		String keyValueRegex = "(\"[a-zA-z._0-9]+\")(\"[^\"]*\")";
		String objectCommaRegex = "[}\\]]\"";
		String cleanUpRegex = ",([}\\]])";

		text = text.replaceAll("\\\\", "/"); // Replace backslashs with forwardslashs
		text = text.replaceAll("//(.*)", ""); // Remove all commented lines
		text = text.replaceAll("\\x1B", ""); // Remove all illegal characters
		text = text.replaceAll("(\".+)[{}](.+\")", "$1$2"); // Remove brackets in quotes
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

	public static int getEntryIndexByPath(ArrayList<Entry> object, String path) {
		for (int i = 0; i < object.size(); i++) {
				if (object !=null && object.get(i).getFullPath().equalsIgnoreCase(path)) {
						return i;
				}
				//else{System.out.println(object.get(i).getFullPath());}
		}
		return -1;
	}
	public static int getTextureIndexByName(ArrayList<Texture> object, String name) {
		for (int i = 0; i < object.size(); i++) {
				if (object !=null && object.get(i).name.equalsIgnoreCase(name)) {
						return i;
				}
		}
		return -1;
	}


	public static void main(String args[]) throws Exception{
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

		if (args.length != 3) {
			System.err.println("Usage: java vmf2obj <infile> <outpath> <vpkpath>");
			return;
		}

		Scanner in;
		PrintWriter objFile;
		PrintWriter materialFile;
		String outPath = args[1];
		String objName = outPath + ".obj";
		String matLibName = outPath + ".mtl";
		
		try{
			extractLibraries(Paths.get(outPath).getParent().resolve("temp").toString());
		}
		catch (Exception e) {
			System.err.println("Exception: "+e);
		}
		

		// Open vpk file
		System.out.println("[1/?] Reading VPK...");
		File vpkFile = new File(args[2]);
		VPK vpk = new VPK(vpkFile);
		try {
			vpk.load();
		}
		catch(Exception e)
		{
			System.err.println("Error while loading vpk file: "+e.getMessage());
			return;
		}

		ArrayList<Entry> vpkMaterials = new ArrayList<Entry>();
		for (Directory directory : vpk.getDirectories()) {
			for (Entry entry : directory.getEntries()) {
				vpkMaterials.add(entry);
			}
		}

		// Open infile
		File workingFile = new File(args[0]);
		if (!workingFile.exists()) {
			try {
				File directory = new File(workingFile.getParent());
				if (!directory.exists()) {
					directory.mkdirs();
				}
				workingFile.createNewFile();
			} catch (IOException e) {
				System.out.println("Exception Occured: " + e.toString());
			}
		}

		// Read File
		String text = "";
		try {
			text = readFile(args[0]);
		}catch (IOException e) {
			System.out.println("Exception Occured: " + e.toString());
		}
		//System.out.println(text);

		try
		{
			File directory = new File(new File(outPath).getParent());
			if (!directory.exists()) {
					directory.mkdirs();
			}
			
			in = new Scanner(new File(args[0]));
			objFile = new PrintWriter(new FileOutputStream(objName));
			materialFile = new PrintWriter(new FileOutputStream(matLibName));
		}
		catch(IOException e)
		{
			System.err.println("Error while opening file: "+e.getMessage());
			return;
		}

		//
		// Read Geometry
		//

		System.out.println("[2/?] Reading geometry...");

		VMF vmf = parseVMF(text);
		vmf = parseSolids(vmf);
		//System.out.println(gson.toJson(vmf));

		ArrayList<Vector3> verticies = new ArrayList<Vector3>();
		ArrayList<String> faces = new ArrayList<String>();

		ArrayList<String> materials = new ArrayList<String>();
		ArrayList<Texture> textures = new ArrayList<Texture>();
		int vertexOffset = 1;
		int vertexTextureOffset = 1;
		System.out.println("[3/?] Writing faces...");
		
		objFile.println("# Decompiled with VMF2OBJ by Dylancyclone\n");
		objFile.println("mtllib "+matLibName);

		for (Solid solid : vmf.solids)
		{
			verticies.clear();
			faces.clear();
			materials.clear();

			for (Side side : solid.sides)
			{
				//if (side.material.contains("TOOLS/")){continue;}
				materials.add(side.material);
				for (Vector3 point : side.points)
				{
					//System.out.println(point);
					verticies.add(point);
				}
			}
			
			//TODO: Margin of error?
			Set<Vector3> uniqueVerticies = new HashSet<Vector3>(verticies);
			ArrayList<Vector3> uniqueVerticiesList = new ArrayList<Vector3>(uniqueVerticies);

			Set<String> uniqueMaterials = new HashSet<String>(materials);
			ArrayList<String> uniqueMaterialsList = new ArrayList<String>(uniqueMaterials);
			
			
			//Write Faces
			
			objFile.println("\n");
			objFile.println("o "+solid.id+"\n");
			
			for (Vector3 e : uniqueVerticiesList) {
				objFile.println("v " + e.x +" "+ e.y +" "+ e.z);
			}

			
			for (String el : uniqueMaterialsList) {
				//System.out.println(el);
				int index = getEntryIndexByPath(vpkMaterials, "materials/"+el+".vtf"); //TODO: Only basic vtf
				//System.out.println(index);
				if (index != -1){
					File materialOutPath = new File(outPath);
					materialOutPath = new File(formatPath(materialOutPath.getParent()+File.separator+vpkMaterials.get(index).getFullPath()));
					if (!materialOutPath.exists()) {
						try {
							File directory = new File(materialOutPath.getParent());
							if (!directory.exists()) {
								directory.mkdirs();
							}
						} catch (Exception e) {
							System.out.println("Exception Occured: " + e.toString());
						}
						try {
							vpkMaterials.get(index).extract(materialOutPath);
							String[] command = new String[] {
								VTFLibPath,
								"-folder", formatPath(materialOutPath.toString()),
								"-output", formatPath(materialOutPath.getParent()),
								"-exportformat", "tga"};
						
								proc = Runtime.getRuntime().exec(command);
								proc.waitFor();
								//materialOutPath.delete();
								materialOutPath = new File(materialOutPath.toString().substring(0, materialOutPath.toString().lastIndexOf('.'))+".tga");
								
								int width = 1;
								int height = 1;
								try {
									byte[] fileContent = Files.readAllBytes(materialOutPath.toPath());
									BufferedImage bimg = TargaReader.decode(fileContent);
									width = bimg.getWidth();
									height = bimg.getHeight();
								}
								catch (Exception e) {
									System.out.println("Cant read Material: "+ materialOutPath);
									//System.out.println(e);
								}
								//System.out.println("Adding Material:"+ el);
								textures.add(new Texture(el,materialOutPath.toString(),width,height));
						}
						catch (Exception e) {
							System.err.println("Exception on extract: "+e);
						}
						materialFile.println("\n" + 
						"newmtl "+el+"\n"+
						"Ka 1.000 1.000 1.000\n"+
						"Kd 1.000 1.000 1.000\n"+
						"Ks 0.000 0.000 0.000\n"+
						"d 1.0\n"+
						"illum 2\n"+
						"map_Ka "+"materials/"+el+".tga"+"\n"+
						"map_Kd "+"materials/"+el+".tga"+"\n"+
						"map_Ks "+"materials/"+el+".tga"+"\n");
						materialFile.println();
					}
				}
				else {
					System.out.println("Missing Material: "+ el);
					textures.add(new Texture(el,"",1,1));
				}
			}
			objFile.println();
			
			for (Side side : solid.sides)
			{
				//if (side.material.contains("TOOLS/")){continue;}
				Texture texture = textures.get(getTextureIndexByName(textures,side.material));

				side.uAxisTranslation = side.uAxisTranslation % texture.width;
				side.vAxisTranslation = side.vAxisTranslation % texture.height;

				if (side.uAxisTranslation < -texture.width / 2)
				{
					side.uAxisTranslation += texture.width;
				}

				if (side.vAxisTranslation < -texture.height / 2)
				{
					side.vAxisTranslation += texture.height;
				}

				String buffer = "";
				for (int i = 0; i < side.points.length; i++)
				{
					double u = Vector3.dot(side.points[i], side.uAxisVector) / (texture.width * side.uAxisScale) + side.uAxisTranslation / texture.width;
					double v = Vector3.dot(side.points[i], side.vAxisVector) / (texture.height * side.vAxisScale) + side.vAxisTranslation / texture.height;
					objFile.println("vt "+u+" "+v);
					buffer += (uniqueVerticiesList.indexOf(side.points[i]) + vertexOffset) + "/"+(i+vertexTextureOffset)+" ";
				}
				faces.add(buffer);
				vertexTextureOffset += side.points.length;
			}
			objFile.println();
			vertexOffset += uniqueVerticiesList.size();
			

			for (int i = 0; i < faces.size(); i++) {
				objFile.println("usemtl " + solid.sides[i].material);
				objFile.println("f " + faces.get(i));
			}
		}
		
		in.close();
		objFile.close();
		materialFile.close();

	}
}
