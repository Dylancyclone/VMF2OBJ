import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;
import java.util.regex.*;
import com.google.gson.*;
import com.lathrum.VMF2OBJ.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.awt.image.BufferedImage;

public class App {

	public static Gson gson = new Gson();
	public static Process proc;
	public static String VTFLibPath;
	public static String CrowbarLibPath;

	public static void extractLibraries(String dir) throws URISyntaxException {
		//Spooky scary, but I don't want to reinvent the wheel.
		ArrayList<String> files = new ArrayList<String>();
		File tempFolder = new File(dir);
		tempFolder.mkdirs();
		tempFolder.deleteOnExit();

		//VTFLIB
		//http://nemesis.thewavelength.net/index.php?p=40
		//For converting VTF files to TGA files
		files.add("DevIL.dll"); //VTFLib dependency
		files.add("VTFLib.dll"); //VTFLib dependency
		files.add("VTFCmd.exe"); //VTFLib itself
		//Crowbar
		//https://steamcommunity.com/groups/CrowbarTool
		//https://github.com/UltraTechX/Crowbar-Command-Line
		//For converting MDL files to SMD files
		files.add("CrowbarCommandLineDecomp.exe"); //Crowbar itself

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
						case ("CrowbarCommandLineDecomp.exe"):
							CrowbarLibPath = Paths.get(fileURI).toString();
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

	public static boolean deleteRecursive(File path) throws FileNotFoundException {
		if (!path.exists()) throw new FileNotFoundException(path.getAbsolutePath());
		boolean ret = true;
		if (path.isDirectory()){
				for (File f : path.listFiles()){
						ret = ret && deleteRecursive(f);
				}
		}
		return ret && path.delete();
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

					
					String disps = "";
					Pattern dispPattern = Pattern.compile("dispinfo");
					Matcher dispMatcher = dispPattern.matcher(side);
					while (dispMatcher.find()) {
						if (side.charAt(dispMatcher.end()) == '{')
						{
							int dispStartIndex = dispMatcher.end();
							int dispEndIndex = findClosingBracketMatchIndex(side,dispStartIndex);
							String disp = side.substring(dispStartIndex,dispEndIndex+1);
			
							side = side.replace(side.substring(dispMatcher.start(),dispEndIndex+1),""); //snip this section

							
							String normals = "";
							Pattern normalsPattern = Pattern.compile("(?<!offset_)normals"); //Match normals but not offset_normals
							Matcher normalsMatcher = normalsPattern.matcher(disp);
							while (normalsMatcher.find()) {
								if (disp.charAt(normalsMatcher.end()) == '{')
								{
									int normalStartIndex = normalsMatcher.end();
									int normalEndIndex = findClosingBracketMatchIndex(disp,normalStartIndex);
									String normal = disp.substring(normalStartIndex,normalEndIndex+1);
					
									disp = disp.replace(disp.substring(normalsMatcher.start(),normalEndIndex+1),""); //snip this section
									
									Pattern rowsPattern = Pattern.compile("\"row[0-9]+\"\"((?:[0-9.-]+ ?)+)\"");
									Matcher rowsMatcher = rowsPattern.matcher(normal);
									while (rowsMatcher.find()) {
										String vectors = "";
										Pattern vectorPattern = Pattern.compile("([0-9.-]+) ([0-9.-]+) ([0-9.-]+)");
										Matcher vectorMatcher = vectorPattern.matcher(rowsMatcher.group(1));
										while (vectorMatcher.find()) {
											vectors = vectors + "{\"x\":"+Double.parseDouble(vectorMatcher.group(1))+",\"y\":"+Double.parseDouble(vectorMatcher.group(2))+",\"z\":"+Double.parseDouble(vectorMatcher.group(3))+"},";
										}
										normals = normals + "["+vectors+"],";
									}
								}
								normalsMatcher = normalsPattern.matcher(disp);
							}
							
							String distances = "";
							Pattern distancesPattern = Pattern.compile("distances");
							Matcher distancesMatcher = distancesPattern.matcher(disp);
							while (distancesMatcher.find()) {
								if (disp.charAt(distancesMatcher.end()) == '{')
								{
									int distanceStartIndex = distancesMatcher.end();
									int distanceEndIndex = findClosingBracketMatchIndex(disp,distanceStartIndex);
									String distance = disp.substring(distanceStartIndex,distanceEndIndex+1);
					
									disp = disp.replace(disp.substring(distancesMatcher.start(),distanceEndIndex+1),""); //snip this section
									
									Pattern rowsPattern = Pattern.compile("\"row[0-9]+\"\"((?:[0-9.-]+ ?)+)\"");
									Matcher rowsMatcher = rowsPattern.matcher(distance);
									while (rowsMatcher.find()) {
										String vectors = "";
										Pattern vectorPattern = Pattern.compile("((?<!w[0-9]?)[0-9.-]+)");
										Matcher vectorMatcher = vectorPattern.matcher(rowsMatcher.group(1));
										while (vectorMatcher.find()) {
											vectors = vectors + Double.parseDouble(vectorMatcher.group(1))+",";
										}
										distances = distances + "["+vectors+"],";
									}
								}
								distancesMatcher = distancesPattern.matcher(disp);
							}
							
							String alphas = "";
							Pattern alphasPattern = Pattern.compile("alphas");
							Matcher alphasMatcher = alphasPattern.matcher(disp);
							while (alphasMatcher.find()) {
								if (disp.charAt(alphasMatcher.end()) == '{')
								{
									int alphaStartIndex = alphasMatcher.end();
									int alphaEndIndex = findClosingBracketMatchIndex(disp,alphaStartIndex);
									String alpha = disp.substring(alphaStartIndex,alphaEndIndex+1);
					
									disp = disp.replace(disp.substring(alphasMatcher.start(),alphaEndIndex+1),""); //snip this section
									
									Pattern rowsPattern = Pattern.compile("\"row[0-9]+\"\"((?:[0-9.-]+ ?)+)\"");
									Matcher rowsMatcher = rowsPattern.matcher(alpha);
									while (rowsMatcher.find()) {
										String vectors = "";
										Pattern vectorPattern = Pattern.compile("((?<!w[0-9]?)[0-9.-]+)");
										Matcher vectorMatcher = vectorPattern.matcher(rowsMatcher.group(1));
										while (vectorMatcher.find()) {
											vectors = vectors + Double.parseDouble(vectorMatcher.group(1))+",";
										}
										alphas = alphas + "["+vectors+"],";
									}
								}
								alphasMatcher = alphasPattern.matcher(disp);
							}

							disp = disp.replaceAll(objectRegex,",\"$1\":$2");
							disp = disp.replaceAll(keyValueRegex,"$1:$2,");
							disp = disp.replaceAll(",,",",");
							normals = ",\"normals\":["+normals.substring(0,normals.length()-1)+"]";
							disp = splice(disp,normals,disp.length()-1);
							distances = ",\"distances\":["+distances.substring(0,distances.length()-1)+"]";
							disp = splice(disp,distances,disp.length()-1);
							alphas = ",\"alphas\":["+alphas.substring(0,alphas.length()-1)+"]";
							disp = splice(disp,alphas,disp.length()-1);
							disp = disp.replaceAll(cleanUpRegex,"$1"); //remove commas at the end of a list
							disps = disp;
							//System.out.println(disp);
						}
						dispMatcher = dispPattern.matcher(solid);
					}

					side = side.replaceAll(objectRegex,",\"$1\":$2");
					side = side.replaceAll(keyValueRegex,"$1:$2,");
					side = side.replaceAll(",,",",");
					if (disps != "")
					{
						disps = "\"dispinfo\":"+disps.substring(0,disps.length()-1)+"}";
						side = splice(side,disps,side.length()-1);
					}
					side = side.replaceAll(cleanUpRegex,"$1"); //remove commas at the end of a list
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

	public static VMT parseVMT(String text) {

		if (text.substring(1,6).equals("Water")) // If water texture
		{
			//Water is weird. It doesn't really have a displayable texture other than a normal map,
			//which shouldn't really be used anyways in this case. So we'll give it an obvious texture
			//So it can be easily changed
			VMT vmt = gson.fromJson("{\"basetexture\":\"TOOLS/TOOLSDOTTED\"}", VMT.class);
			return vmt;
		}

		String keyValueRegex = "(\"[a-zA-z._0-9 ]+\")(\"[^\"]*\")";
		String cleanUpRegex = ", *([}\\]])";

		//Holy moly do I wish Valve was consistant in their kv files.
		//All the following are just to format the file correctly.

		text = text.replaceAll("\\\\", "/"); // Replace backslashs with forwardslashs
		text = text.replaceAll("//(.*)", ""); // Remove all commented lines
		text = text.replaceAll("\\x1B", ""); // Remove all illegal characters
		text = text.replaceAll("srgb\\?", ""); // Remove all weirdos
		text = text.replaceAll("-dx10", ""); // Remove all dx10 fallback textures
		text = text.replaceAll("[^\"](\\$[^\" \\t]+)", "\"$1\""); // fix unquoted keys
		text = text.replaceAll("(\".+\"[ \\t]+)([^\" \\t\\s].*)", "$1\"$2\""); // fix unquoted values
		text = text.replaceAll("\\$", ""); // Remove all key prefixes
		text = text.replaceAll("\"%.+", ""); // Remove all lines with keys that start with percentage signs
		//text = text.replaceAll("(\".+)[{}](.+\")", "$1$2"); // Remove brackets in quotes
		text = text.replaceAll("[\\t\\r\\n]", ""); // Remove all whitespaces and newlines not in quotes
		text = text.replaceAll("\" +\"", "\"\""); // Remove all whitespaces and newlines not in quotes
		//text = text.replaceAll("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)", ""); // Remove all whitespaces and newlines not in quotes
		
		Pattern bracketPattern = Pattern.compile("\\{");
		Matcher bracketMatcher = bracketPattern.matcher(text);
		if (bracketMatcher.find()) {
			int startIndex = bracketMatcher.end()-1;
			int endIndex = findClosingBracketMatchIndex(text,startIndex);
			if (endIndex == -1) // Invalid vmt
			{
				VMT vmt = gson.fromJson("{\"basetexture\":\"TOOLS/TOOLSDOTTED\"}", VMT.class);
				return vmt;
			}
			text = text.substring(startIndex,endIndex+1);
			
			Pattern proxiesPattern = Pattern.compile("\"proxies\""); //check if the materials has proxies
			Matcher proxiesMatcher = proxiesPattern.matcher(text);
			if (proxiesMatcher.find()) {
				if (text.charAt(proxiesMatcher.end()) == '{')
				{
					int proxiesStartIndex = proxiesMatcher.end();
					int proxiesEndIndex = findClosingBracketMatchIndex(text,proxiesStartIndex);
	
					text = text.replace(text.substring(proxiesMatcher.start(),proxiesEndIndex+1),""); //snip the proxies
				}
			}

			text = text.replaceAll(keyValueRegex,"$1:$2,");
			text = text.replaceAll(cleanUpRegex,"$1"); //remove commas at the end of a list
		}
		text=text.toLowerCase();

		//System.out.println(text);
		VMT vmt = gson.fromJson(text, VMT.class);
		return vmt;
	}

	public static QC parseQC(String text) {

		//Why does valve have to be so inconsistant yet so strict?
		//From https://developer.valvesoftware.com/wiki/$texturegroup
		//Bug: You must add spaces between the {} and the "". Adding a new skin with {"skin0"} will not work, but { "skin0" } will.
		//Whyy

		text = text.replaceAll("\\\\", "/"); // Replace backslashs with forwardslashs
		text = text.replaceAll("//(.*)", ""); // Remove all commented lines
		text = text.replaceAll("\\$staticprop", ""); // Remove all weirdos
		text = text.replaceAll("[^\"](\\$[^\" \\t]+)", "\"$1\""); // fix unquoted keys
		text = text.replaceAll("(\".+\"[ \\t]+)([^\" \\t\\s].*)", "$1\"$2\""); // fix unquoted values
		text = text.replaceAll("\\$", ""); // Remove all key prefixes
		text = text.replaceAll("\\{ (\".+\") +\"\\}\"", "$1"); // Fix texturegroup formatting
		text = text.replaceAll("[\\t\\r\\n]", ""); // Remove all whitespaces and newlines not in quotes
		text = text.replaceAll("\" +\"", "\"\""); // Remove all whitespaces and newlines not in quotes
		//text = text.replaceAll("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)", ""); // Remove all whitespaces and newlines not in quotes
		
		Matcher modelNameMatcher = Pattern.compile("\"modelname\"\"([a-zA-Z0-9._/]+)\"").matcher(text);
		String modelName = "";
		if (modelNameMatcher.find()) {
			modelName = modelNameMatcher.group(1);
		}
		else {
			System.out.println("Failed to find modelName");
		}

		Matcher bodyGroupMatcher = Pattern.compile("(\"bodygroup\"\"[a-zA-Z0-9._/]+\"\\{([a-zA-Z0-9._/\" ]+)\\})").matcher(text);
		Collection<String> bodygroups = new LinkedList<String>();
		while (bodyGroupMatcher.find()) {
			String working = bodyGroupMatcher.group(2);
			
			
			Matcher stringMatcher = Pattern.compile("\"([a-zA-Z0-9._/ ]+)\"").matcher(working);
			while (stringMatcher.find()) {
				bodygroups.add(stringMatcher.group(1));
			}
			
			
			text = text.replace(bodyGroupMatcher.group(1),""); //snip this section
			bodyGroupMatcher = Pattern.compile("(\"bodygroup\"\"[a-zA-Z0-9._/]+\"\\{([a-zA-Z0-9._/\" ]+)\\})").matcher(text);
		}

		Matcher cdMaterialsMatcher = Pattern.compile("\"cdmaterials\"\"([a-zA-Z0-9._/]+)\"").matcher(text);
		Collection<String> cdMaterials = new LinkedList<String>();
		while (cdMaterialsMatcher.find()) {
			cdMaterials.add(cdMaterialsMatcher.group(1));
		}

		return new QC(modelName,bodygroups.toArray(new String[bodygroups.size()]),cdMaterials.toArray(new String[cdMaterials.size()]));
	}

	public static SMDTriangle[] parseSMD(String text) {

		//Aha! Finally a format that should be consistant!

		Matcher triangleMatcher = Pattern.compile("([a-zA-Z0-9_]+)\\R  ([0-9]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+)\\R  ([0-9]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+)\\R  ([0-9]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+) ([0-9.-]+)\\R").matcher(text);
		Collection<SMDTriangle> SMDTriangles = new LinkedList<SMDTriangle>();
		while (triangleMatcher.find()) {
			SMDPoint[] points = new SMDPoint[3];
			for (int i = 0; i <3;i++) {
				points[i] = new SMDPoint(
					new Vector3(Double.parseDouble(triangleMatcher.group(12*i+3)), Double.parseDouble(triangleMatcher.group(12*i+4)), Double.parseDouble(triangleMatcher.group(12*i+5))),
					new Vector3(Double.parseDouble(triangleMatcher.group(12*i+6)), Double.parseDouble(triangleMatcher.group(12*i+7)), Double.parseDouble(triangleMatcher.group(12*i+8))),
					triangleMatcher.group(12*i+9),
					triangleMatcher.group(12*i+10)
				);
			}
			SMDTriangles.add(new SMDTriangle(triangleMatcher.group(1),points));
		}

		return SMDTriangles.toArray(new SMDTriangle[SMDTriangles.size()]);
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
		if (vmf.solids == null) {return vmf;} //There are no brushes in this VMF
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

	public static boolean isDisplacementSolid(Solid solid) {
		for (Side side : solid.sides) {
			if (side.dispinfo != null) {
				return true;
			}
		}
		return false;
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
	public static ArrayList<Integer> getEntryIndiciesByPattern(ArrayList<Entry> object, String pattern) {
		ArrayList<Integer> indicies = new ArrayList<Integer>();
		for (int i = 0; i < object.size(); i++) {
			if (object !=null && object.get(i).getFullPath().contains(pattern)) {
				indicies.add(i);
			}
			//else{System.out.println(object.get(i).getFullPath());}
		}
		return indicies;
	}
	public static int getTextureIndexByName(ArrayList<Texture> object, String name) {
		for (int i = 0; i < object.size(); i++) {
			if (object !=null && object.get(i).name.equalsIgnoreCase(name)) {
				return i;
			}
		}
		return -1;
	}
	public static int getTextureIndexByFileName(ArrayList<Texture> object, String name) {
		for (int i = 0; i < object.size(); i++) {
			if (object !=null && object.get(i).fileName.equalsIgnoreCase(name)) {
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
		

		//Clean working directory
		try {
			deleteRecursive(new File(Paths.get(outPath).getParent().resolve("materials").toString()));
			deleteRecursive(new File(Paths.get(outPath).getParent().resolve("models").toString()));
		}
		catch (Exception e) {
			//System.err.println("Exception: "+e);
		}

		//Extract Libraries
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

		ArrayList<Entry> vpkEntries = new ArrayList<Entry>();
		for (Directory directory : vpk.getDirectories()) {
			for (Entry entry : directory.getEntries()) {
				vpkEntries.add(entry);
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
		ArrayList<Face> faces = new ArrayList<Face>();

		ArrayList<String> materials = new ArrayList<String>();
		ArrayList<Texture> textures = new ArrayList<Texture>();
		int vertexOffset = 1;
		int vertexTextureOffset = 1;
		int vertexNormalOffset = 1;
		System.out.println("[3/?] Writing brushes...");
		
		objFile.println("# Decompiled with VMF2OBJ by Dylancyclone\n");
		objFile.println("mtllib "+matLibName.substring(formatPath(matLibName).lastIndexOf(File.separatorChar)+1, matLibName.length()));

		
		if (vmf.solids != null) { //There are no brushes in this VMF
			for (Solid solid : vmf.solids)
			{
				verticies.clear();
				faces.clear();
				materials.clear();

				for (Side side : solid.sides)
				{
					//if (side.material.contains("TOOLS/")){continue;}
					materials.add(side.material);
					if (side.dispinfo == null) {
						if (isDisplacementSolid(solid)) continue;
						for (Vector3 point : side.points)
						{
							verticies.add(point);
						}
					}
					else
					{
						//Points are defined in this order:
						// 1  4
						// 2  3
						// -or-
						// A  D
						// B  C
						Vector3 ad = side.points[3].subtract(side.points[0]);
						Vector3 ab = side.points[1].subtract(side.points[0]);
						//System.out.println(ad);
						//System.out.println(ab);
						for (int i = 0; i < side.dispinfo.normals.length; i++) //rows
						{
							for (int j = 0; j < side.dispinfo.normals[0].length; j++) //columns
							{
								Vector3 point = side.points[0]
									.add(ad.normalize().multiply(ad.divide(side.dispinfo.normals[0].length-1).abs().multiply(j)))
									.add(ab.normalize().multiply(ab.divide(side.dispinfo.normals.length-1).abs().multiply(i)))
									.add(side.dispinfo.normals[i][j].multiply(side.dispinfo.distances[i][j]));
								verticies.add(point);
							}
						}
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
					el = el.toLowerCase();
					
					// Read File
					String VMTText = "";
					try {
						int index = getEntryIndexByPath(vpkEntries, "materials/"+el+".vmt");
						VMTText = new String(vpkEntries.get(index).readData());
					}catch (IOException e) {
						System.out.println("Exception Occured: " + e.toString());
					}

					VMT vmt = parseVMT(VMTText);
					vmt.name = el;
					//System.out.println(gson.toJson(vmt));
					//System.out.println(vmt.basetexture);
					if (vmt.basetexture.endsWith(".vtf")) {
						vmt.basetexture = vmt.basetexture.substring(0, vmt.basetexture.lastIndexOf('.')); //snip the extension
					}
					int index = getEntryIndexByPath(vpkEntries, "materials/"+vmt.basetexture+".vtf");
					//System.out.println(index);
					if (index != -1){
						File materialOutPath = new File(outPath);
						materialOutPath = new File(formatPath(materialOutPath.getParent()+File.separator+vpkEntries.get(index).getFullPath()));
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
								vpkEntries.get(index).extract(materialOutPath);
								String[] command = new String[] {
									VTFLibPath,
									"-folder", formatPath(materialOutPath.toString()),
									"-output", formatPath(materialOutPath.getParent()),
									"-exportformat", "jpg"};
								
								if (vmt.translucent==1||vmt.alphatest==1) {
									command[6]="tga"; //If the texture is translucent, use the targa format
								}
							
								proc = Runtime.getRuntime().exec(command);
								proc.waitFor();
								//materialOutPath.delete();
								if (vmt.translucent==1||vmt.alphatest==1) {
									materialOutPath = new File(materialOutPath.toString().substring(0, materialOutPath.toString().lastIndexOf('.'))+".tga");
								} else {
									materialOutPath = new File(materialOutPath.toString().substring(0, materialOutPath.toString().lastIndexOf('.'))+".jpg");
								}

								int width = 1;
								int height = 1;
								BufferedImage bimg;
								try {
									if (vmt.translucent==1||vmt.alphatest==1) {
										byte[] fileContent = Files.readAllBytes(materialOutPath.toPath());
										bimg = TargaReader.decode(fileContent);
									} else {
										bimg = ImageIO.read(materialOutPath);
									}
									width = bimg.getWidth();
									height = bimg.getHeight();
								}
								catch (Exception e) {
									System.out.println("Cant read Material: "+ materialOutPath);
									//System.out.println(e);
								}
								//System.out.println("Adding Material: "+ el);
								textures.add(new Texture(el,vmt.basetexture,materialOutPath.toString(),width,height));
							}
							catch (Exception e) {
								System.err.println("Exception on extract: "+e);
							}
							
							if (vmt.bumpmap != null) { //If the material has a bump map associated with it
								if (vmt.bumpmap.endsWith(".vtf")) {
									vmt.bumpmap = vmt.bumpmap.substring(0, vmt.bumpmap.lastIndexOf('.')); //snip the extension
								}
								//System.out.println("Bump found on "+vmt.basetexture+": "+vmt.bumpmap);
								int bumpMapIndex = getEntryIndexByPath(vpkEntries, "materials/"+vmt.bumpmap+".vtf");
								//System.out.println(bumpMapIndex);
								if (bumpMapIndex != -1){
									File bumpMapOutPath = new File(outPath);
									bumpMapOutPath = new File(formatPath(bumpMapOutPath.getParent()+File.separator+vpkEntries.get(bumpMapIndex).getFullPath()));
									if (!bumpMapOutPath.exists()) {
										try {
											File directory = new File(bumpMapOutPath.getParent());
											if (!directory.exists()) {
												directory.mkdirs();
											}
										} catch (Exception e) {
											System.out.println("Exception Occured: " + e.toString());
										}
										try {
											vpkEntries.get(bumpMapIndex).extract(bumpMapOutPath);
											String[] command = new String[] {
												VTFLibPath,
												"-folder", formatPath(bumpMapOutPath.toString()),
												"-output", formatPath(bumpMapOutPath.getParent()),
												"-exportformat", "jpg"};
											proc = Runtime.getRuntime().exec(command);
											proc.waitFor();
										}
										catch (Exception e) {
											System.err.println("Exception on extract: "+e);
										}
									}
								}
							}
							
							materialFile.println("\n" + 
							"newmtl "+el+"\n"+
							"Ka 1.000 1.000 1.000\n"+
							"Kd 1.000 1.000 1.000\n"+
							"Ks 0.000 0.000 0.000\n"+
							"d 1.0\n"+
							"illum 2");
							if (vmt.translucent==1||vmt.alphatest==1) {
								materialFile.println(
								"map_Ka "+"materials/"+vmt.basetexture+".tga"+"\n"+
								"map_Kd "+"materials/"+vmt.basetexture+".tga");
							} else {
								materialFile.println(
								"map_Ka "+"materials/"+vmt.basetexture+".jpg"+"\n"+
								"map_Kd "+"materials/"+vmt.basetexture+".jpg");
							}
							if (vmt.bumpmap != null) { //If the material has a bump map associated with it
								materialFile.println(
								"map_bump "+"materials/"+vmt.bumpmap+".jpg");
							}
							materialFile.println();
						}
						else { //File has already been extracted
							int textureIndex = getTextureIndexByName(textures,el);
							if (textureIndex == -1) //But this is a new material
							{
								textureIndex = getTextureIndexByFileName(textures,vmt.basetexture);
								//System.out.println("Adding Material: "+ el);
								textures.add(new Texture(el,vmt.basetexture,materialOutPath.toString(),textures.get(textureIndex).width,textures.get(textureIndex).height));
								
								materialFile.println("\n" + 
								"newmtl "+el+"\n"+
								"Ka 1.000 1.000 1.000\n"+
								"Kd 1.000 1.000 1.000\n"+
								"Ks 0.000 0.000 0.000\n"+
								"d 1.0\n"+
								"illum 2");
								if (vmt.translucent==1||vmt.alphatest==1) {
									materialFile.println(
									"map_Ka "+"materials/"+vmt.basetexture+".tga"+"\n"+
									"map_Kd "+"materials/"+vmt.basetexture+".tga");
								} else {
									materialFile.println(
									"map_Ka "+"materials/"+vmt.basetexture+".jpg"+"\n"+
									"map_Kd "+"materials/"+vmt.basetexture+".jpg");
								}
								if (vmt.bumpmap != null) { //If the material has a bump map associated with it
									materialFile.println(
									"map_bump "+"materials/"+vmt.bumpmap+".jpg");
								}
								materialFile.println();
							}
						}
					}
					else { //Cant find material
						int textureIndex = getTextureIndexByName(textures,el);
						if (textureIndex == -1) //But this is a new material
						{
							System.out.println("Missing Material: "+ vmt.basetexture);
							textures.add(new Texture(el,vmt.basetexture,"",1,1));
						}
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
					
					if (side.dispinfo == null)
					{
						if (isDisplacementSolid(solid)) continue;
						for (int i = 0; i < side.points.length; i++)
						{
							double u = Vector3.dot(side.points[i], side.uAxisVector) / (texture.width * side.uAxisScale) + side.uAxisTranslation / texture.width;
							double v = Vector3.dot(side.points[i], side.vAxisVector) / (texture.height * side.vAxisScale) + side.vAxisTranslation / texture.height;
							u = -u + texture.width;
							v = -v + texture.height;
							objFile.println("vt "+u+" "+v);
							buffer += (uniqueVerticiesList.indexOf(side.points[i]) + vertexOffset) + "/"+(i+vertexTextureOffset)+" ";
						}
						faces.add(new Face(buffer,side.material.toLowerCase()));
						vertexTextureOffset += side.points.length;
					}
					else
					{
						//Points are defined in this order:
						// 1  4
						// 2  3
						// -or-
						// A  D
						// B  C
						Vector3 ad = side.points[3].subtract(side.points[0]);
						Vector3 ab = side.points[1].subtract(side.points[0]);
						for (int i = 0; i < side.dispinfo.normals.length-1; i++) //all rows but last
						{
							for (int j = 0; j < side.dispinfo.normals[0].length-1; j++) //all columns but last
							{
								buffer = "";
								Vector3 point = side.points[0]
									.add(ad.normalize().multiply(ad.divide(side.dispinfo.normals[0].length-1).abs().multiply(j)))
									.add(ab.normalize().multiply(ab.divide(side.dispinfo.normals.length-1).abs().multiply(i)))
									.add(side.dispinfo.normals[i][j].multiply(side.dispinfo.distances[i][j]));
								double u = Vector3.dot(point, side.uAxisVector) / (texture.width * side.uAxisScale) + side.uAxisTranslation / texture.width;
								double v = Vector3.dot(point, side.vAxisVector) / (texture.height * side.vAxisScale) + side.vAxisTranslation / texture.height;
								u = -u + texture.width;
								v = -v + texture.height;
								objFile.println("vt "+u+" "+v);
								buffer += (uniqueVerticiesList.indexOf(point) + vertexOffset) + "/"+((((side.dispinfo.normals.length-1)*i)+j)*4+vertexTextureOffset)+" ";

								point = side.points[0]
									.add(ad.normalize().multiply(ad.divide(side.dispinfo.normals[0].length-1).abs().multiply(j)))
									.add(ab.normalize().multiply(ab.divide(side.dispinfo.normals.length-1).abs().multiply(i+1)))
									.add(side.dispinfo.normals[i+1][j].multiply(side.dispinfo.distances[i+1][j]));
								u = Vector3.dot(point, side.uAxisVector) / (texture.width * side.uAxisScale) + side.uAxisTranslation / texture.width;
								v = Vector3.dot(point, side.vAxisVector) / (texture.height * side.vAxisScale) + side.vAxisTranslation / texture.height;
								u = -u + texture.width;
								v = -v + texture.height;
								objFile.println("vt "+u+" "+v);
								buffer += (uniqueVerticiesList.indexOf(point) + vertexOffset) + "/"+((((side.dispinfo.normals.length-1)*i)+j)*4+vertexTextureOffset+1)+" ";

								point = side.points[0]
									.add(ad.normalize().multiply(ad.divide(side.dispinfo.normals[0].length-1).abs().multiply(j+1)))
									.add(ab.normalize().multiply(ab.divide(side.dispinfo.normals.length-1).abs().multiply(i+1)))
									.add(side.dispinfo.normals[i+1][j+1].multiply(side.dispinfo.distances[i+1][j+1]));
								u = Vector3.dot(point, side.uAxisVector) / (texture.width * side.uAxisScale) + side.uAxisTranslation / texture.width;
								v = Vector3.dot(point, side.vAxisVector) / (texture.height * side.vAxisScale) + side.vAxisTranslation / texture.height;
								u = -u + texture.width;
								v = -v + texture.height;
								objFile.println("vt "+u+" "+v);
								buffer += (uniqueVerticiesList.indexOf(point) + vertexOffset) + "/"+((((side.dispinfo.normals.length-1)*i)+j)*4+vertexTextureOffset+2)+" ";

								point = side.points[0]
									.add(ad.normalize().multiply(ad.divide(side.dispinfo.normals[0].length-1).abs().multiply(j+1)))
									.add(ab.normalize().multiply(ab.divide(side.dispinfo.normals.length-1).abs().multiply(i)))
									.add(side.dispinfo.normals[i][j+1].multiply(side.dispinfo.distances[i][j+1]));
								u = Vector3.dot(point, side.uAxisVector) / (texture.width * side.uAxisScale) + side.uAxisTranslation / texture.width;
								v = Vector3.dot(point, side.vAxisVector) / (texture.height * side.vAxisScale) + side.vAxisTranslation / texture.height;
								u = -u + texture.width;
								v = -v + texture.height;
								objFile.println("vt "+u+" "+v);
								buffer += (uniqueVerticiesList.indexOf(point) + vertexOffset) + "/"+((((side.dispinfo.normals.length-1)*i)+j)*4+vertexTextureOffset+3)+" ";

								faces.add(new Face(buffer,side.material.toLowerCase()));
							}
						}
					vertexTextureOffset += (side.dispinfo.normals.length-1)*(side.dispinfo.normals[0].length-1)*4;
					}
				}
				objFile.println();
				vertexOffset += uniqueVerticiesList.size();
				
				String lastMaterial = "";
				for (int i = 0; i < faces.size(); i++) {
					if (!faces.get(i).material.equals(lastMaterial))
					{
						objFile.println("usemtl " + faces.get(i).material);
					}
					lastMaterial = faces.get(i).material;

					objFile.println("f " + faces.get(i).text);
				}
			}
		}
		
		System.out.println("[4/?] Processing models...");

		for (Entity entity : vmf.entities)
		{
			if (entity.classname.contains("prop_")) { //If the entity is a prop
				if (entity.model == null) {
					System.out.println("Prop has no model? "+entity.classname);
					continue;
				}
				verticies.clear();
				faces.clear();
				materials.clear();

				ArrayList<Integer> indicies = getEntryIndiciesByPattern(vpkEntries,entity.model.substring(0, entity.model.lastIndexOf('.'))+".");
				indicies.removeAll(getEntryIndiciesByPattern(vpkEntries,entity.model.substring(0, entity.model.lastIndexOf('.'))+".vmt"));
				indicies.removeAll(getEntryIndiciesByPattern(vpkEntries,entity.model.substring(0, entity.model.lastIndexOf('.'))+".vtf"));
				//System.out.println(indicies);
				for (int index : indicies) {
					//System.out.println(vpkEntries.get(index).getFullPath());
					
					if (index != -1){
						File fileOutPath = new File(outPath);
						fileOutPath = new File(formatPath(fileOutPath.getParent()+File.separator+vpkEntries.get(index).getFullPath()));
						if (!fileOutPath.exists()) {
							try {
								File directory = new File(fileOutPath.getParent());
								if (!directory.exists()) {
									directory.mkdirs();
								}
							} catch (Exception e) {
								System.out.println("Exception Occured: " + e.toString());
							}
							try {
								vpkEntries.get(index).extract(fileOutPath);
							}
							catch (Exception e) {
								System.err.println("Exception on extract: "+e);
							}
						}
					}
				}
				
				String[] command = new String[] {
					CrowbarLibPath,
					"-p", formatPath(new File(outPath).getParent()+File.separator+entity.model)};
			
				proc = Runtime.getRuntime().exec(command);
				proc.waitFor();
				
				String qcText = "";
				try {
					qcText = readFile(new File(outPath).getParent()+File.separator+entity.model.substring(0, entity.model.lastIndexOf('.'))+".qc"); // This line may cause errors if the qc file does not have the same name as the mdl file
				}catch (IOException e) {
					System.out.println("Exception Occured: " + e.toString());
				}
				if (qcText.matches(""))
				{
					System.out.println("Error: Could not find QC file for model, skipping: "+entity.model);
					continue;
				}
				//System.out.println(qcText);
				
				QC qc = parseQC(qcText);

				ArrayList<SMDTriangle> SMDTriangles = new ArrayList<SMDTriangle>();

				for (String bodyGroup : qc.BodyGroups) {
					String smdText = readFile(formatPath(new File(outPath).getParent()+File.separator+"models"+File.separator+qc.ModelName.substring(0, qc.ModelName.lastIndexOf('/'))+"/"+bodyGroup));
					
					SMDTriangles.addAll(Arrays.asList(parseSMD(smdText)));
				}

				//Transform model
				String[] angles = entity.angles.split(" ");
				double[] radAngles = new double[3];
				radAngles[0] = Double.parseDouble(angles[0])*Math.PI/180;
				radAngles[1] = (Double.parseDouble(angles[1])+90)*Math.PI/180;
				radAngles[2] = Double.parseDouble(angles[2])*Math.PI/180;
				String[] origin = entity.origin.split(" ");
				Vector3 transform = new Vector3(Double.parseDouble(origin[0]), Double.parseDouble(origin[1]), Double.parseDouble(origin[2]));
				for (int i=0;i<SMDTriangles.size();i++) {
					SMDTriangle temp = SMDTriangles.get(i);
					materials.add(temp.materialName);
					for (int j=0;j<temp.points.length;j++) {
						//VMF stores rotations as: YZX
						//Source stores relative to obj as: YXZ
						//Meaning translated rotations are: YXZ
						//Or what would normally be read as XZY
						temp.points[j].position = temp.points[j].position.rotate3D(radAngles[0], radAngles[2], radAngles[1]);

						temp.points[j].position = temp.points[j].position.add(transform);
						verticies.add(temp.points[j].position);
					}
					SMDTriangles.set(i,temp);
				}
				
				//TODO: Margin of error?
				Set<Vector3> uniqueVerticies = new HashSet<Vector3>(verticies);
				ArrayList<Vector3> uniqueVerticiesList = new ArrayList<Vector3>(uniqueVerticies);

				Set<String> uniqueMaterials = new HashSet<String>(materials);
				ArrayList<String> uniqueMaterialsList = new ArrayList<String>(uniqueMaterials);
				
				
				//Write Faces
				
				objFile.println("\n");
				objFile.println("o "+qc.ModelName.substring(qc.ModelName.lastIndexOf('/')+1, qc.ModelName.lastIndexOf('.'))+"\n");
				
				for (Vector3 e : uniqueVerticiesList) {
					objFile.println("v " + e.x +" "+ e.y +" "+ e.z);
				}
				
				for (String el : uniqueMaterialsList) {
					el = el.toLowerCase();
					
					// Read File
					String VMTText = "";
					for (String cdMaterial : qc.CDMaterials){ //Our material can be in multiple directories, we gotta find it
						if (cdMaterial.endsWith("/")) {
							cdMaterial = cdMaterial.substring(0, cdMaterial.lastIndexOf('/'));
						}
						try {
							int index = getEntryIndexByPath(vpkEntries, "materials/"+cdMaterial+"/"+el+".vmt");
							if (index == -1) {continue;} //Could not find it
							VMTText = new String(vpkEntries.get(index).readData());
						}catch (IOException e) {
							System.out.println("Exception Occured: " + e.toString());
						}
						if (!VMTText.isEmpty()) {break;}
					}
					if (VMTText.isEmpty()) {
						System.out.println("Could not find material: " + el);
					}

					VMT vmt = parseVMT(VMTText);
					vmt.name = el;
					//System.out.println(gson.toJson(vmt));
					//System.out.println(vmt.basetexture);
					if (vmt.basetexture.endsWith(".vtf")) {
						vmt.basetexture = vmt.basetexture.substring(0, vmt.basetexture.lastIndexOf('.')); //snip the extension
					}
					int index = getEntryIndexByPath(vpkEntries, "materials/"+vmt.basetexture+".vtf");
					//System.out.println(index);
					if (index != -1){
						File materialOutPath = new File(outPath);
						materialOutPath = new File(formatPath(materialOutPath.getParent()+File.separator+vpkEntries.get(index).getFullPath()));
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
								vpkEntries.get(index).extract(materialOutPath);
								String[] convertCommand = new String[] {
									VTFLibPath,
									"-folder", formatPath(materialOutPath.toString()),
									"-output", formatPath(materialOutPath.getParent()),
									"-exportformat", "jpg"};

								if (vmt.translucent==1||vmt.alphatest==1) {
									convertCommand[6]="tga"; //If the texture is translucent, use the targa format
								}
							
								proc = Runtime.getRuntime().exec(convertCommand);
								proc.waitFor();
								//materialOutPath.delete();
								if (vmt.translucent==1||vmt.alphatest==1) {
									materialOutPath = new File(materialOutPath.toString().substring(0, materialOutPath.toString().lastIndexOf('.'))+".tga");
								} else {
									materialOutPath = new File(materialOutPath.toString().substring(0, materialOutPath.toString().lastIndexOf('.'))+".jpg");
								}
								
								int width = 1;
								int height = 1;
								BufferedImage bimg;
								try {
									if (vmt.translucent==1||vmt.alphatest==1) {
										byte[] fileContent = Files.readAllBytes(materialOutPath.toPath());
										bimg = TargaReader.decode(fileContent);
									} else {
										bimg = ImageIO.read(materialOutPath);
									}
									width = bimg.getWidth();
									height = bimg.getHeight();
								}
								catch (Exception e) {
									System.out.println("Cant read Material: "+ materialOutPath);
									//System.out.println(e);
								}
								//System.out.println("Adding Material: "+ el);
								textures.add(new Texture(el,vmt.basetexture,materialOutPath.toString(),width,height));
							}
							catch (Exception e) {
								System.err.println("Exception on extract: "+e);
							}
							
							if (vmt.bumpmap != null) { //If the material has a bump map associated with it
								if (vmt.bumpmap.endsWith(".vtf")) {
									vmt.bumpmap = vmt.bumpmap.substring(0, vmt.bumpmap.lastIndexOf('.')); //snip the extension
								}
								//System.out.println("Bump found on "+vmt.basetexture+": "+vmt.bumpmap);
								int bumpMapIndex = getEntryIndexByPath(vpkEntries, "materials/"+vmt.bumpmap+".vtf");
								//System.out.println(bumpMapIndex);
								if (bumpMapIndex != -1){
									File bumpMapOutPath = new File(outPath);
									bumpMapOutPath = new File(formatPath(bumpMapOutPath.getParent()+File.separator+vpkEntries.get(bumpMapIndex).getFullPath()));
									if (!bumpMapOutPath.exists()) {
										try {
											File directory = new File(bumpMapOutPath.getParent());
											if (!directory.exists()) {
												directory.mkdirs();
											}
										} catch (Exception e) {
											System.out.println("Exception Occured: " + e.toString());
										}
										try {
											vpkEntries.get(bumpMapIndex).extract(bumpMapOutPath);
											String[] convertCommand = new String[] {
												VTFLibPath,
												"-folder", formatPath(bumpMapOutPath.toString()),
												"-output", formatPath(bumpMapOutPath.getParent()),
												"-exportformat", "jpg"};
											proc = Runtime.getRuntime().exec(convertCommand);
											proc.waitFor();
										}
										catch (Exception e) {
											System.err.println("Exception on extract: "+e);
										}
									}
								}
							}
							
							materialFile.println("\n" + 
							"newmtl "+el+"\n"+
							"Ka 1.000 1.000 1.000\n"+
							"Kd 1.000 1.000 1.000\n"+
							"Ks 0.000 0.000 0.000\n"+
							"d 1.0\n"+
							"illum 2");
							if (vmt.translucent==1||vmt.alphatest==1) {
								materialFile.println(
								"map_Ka "+"materials/"+vmt.basetexture+".tga"+"\n"+
								"map_Kd "+"materials/"+vmt.basetexture+".tga");
							} else {
								materialFile.println(
								"map_Ka "+"materials/"+vmt.basetexture+".jpg"+"\n"+
								"map_Kd "+"materials/"+vmt.basetexture+".jpg");
							}
							if (vmt.bumpmap != null) { //If the material has a bump map associated with it
								materialFile.println(
								"map_bump "+"materials/"+vmt.bumpmap+".jpg");
							}
							materialFile.println();
						}
						else { //File has already been extracted
							int textureIndex = getTextureIndexByName(textures,el);
							if (textureIndex == -1) //But this is a new material
							{
								textureIndex = getTextureIndexByFileName(textures,vmt.basetexture);
								//System.out.println("Adding Material: "+ el);
								textures.add(new Texture(el,vmt.basetexture,materialOutPath.toString(),textures.get(textureIndex).width,textures.get(textureIndex).height));
								
								materialFile.println("\n" + 
								"newmtl "+el+"\n"+
								"Ka 1.000 1.000 1.000\n"+
								"Kd 1.000 1.000 1.000\n"+
								"Ks 0.000 0.000 0.000\n"+
								"d 1.0\n"+
								"illum 2");
								if (vmt.translucent==1||vmt.alphatest==1) {
									materialFile.println(
									"map_Ka "+"materials/"+vmt.basetexture+".tga"+"\n"+
									"map_Kd "+"materials/"+vmt.basetexture+".tga");
								} else {
									materialFile.println(
									"map_Ka "+"materials/"+vmt.basetexture+".jpg"+"\n"+
									"map_Kd "+"materials/"+vmt.basetexture+".jpg");
								}
								if (vmt.bumpmap != null) { //If the material has a bump map associated with it
									materialFile.println(
									"map_bump "+"materials/"+vmt.bumpmap+".jpg");
								}
								materialFile.println();
							}
						}
					}
					else { //Cant find material
						int textureIndex = getTextureIndexByName(textures,el);
						if (textureIndex == -1) //But this is a new material
						{
							System.out.println("Missing Material: "+ vmt.basetexture);
							textures.add(new Texture(el,vmt.basetexture,"",1,1));
						}
					}
				}
				objFile.println();

				
				for (SMDTriangle SMDTriangle : SMDTriangles)
				{
					//if (side.material.contains("TOOLS/")){continue;}

					String buffer = "";

					for (int i = 0; i < SMDTriangle.points.length; i++)
					{
						double u = Double.parseDouble(SMDTriangle.points[i].uaxis);
						double v = Double.parseDouble(SMDTriangle.points[i].vaxis);
						objFile.println("vt "+u+" "+v);
						objFile.println("vn " + SMDTriangle.points[i].normal.x +" "+ SMDTriangle.points[i].normal.y +" "+ SMDTriangle.points[i].normal.z);
						//buffer += (uniqueVerticiesList.indexOf(SMDTriangle.points[i].position) + vertexOffset) + "/"+(i+vertexTextureOffset)+" ";
						buffer += (uniqueVerticiesList.indexOf(SMDTriangle.points[i].position) + vertexOffset) + "/"+(i+vertexTextureOffset)+"/"+(i+vertexNormalOffset)+" ";
					}
					faces.add(new Face(buffer,SMDTriangle.materialName.toLowerCase()));
					vertexTextureOffset += SMDTriangle.points.length;
					vertexNormalOffset += SMDTriangle.points.length;
				}
				objFile.println();
				vertexOffset += uniqueVerticiesList.size();
				
				String lastMaterial = "";
				for (int i = 0; i < faces.size(); i++) {
					if (!faces.get(i).material.equals(lastMaterial))
					{
						objFile.println("usemtl " + faces.get(i).material);
					}
					lastMaterial = faces.get(i).material;

					objFile.println("f " + faces.get(i).text);
				}
			}
		}


		in.close();
		objFile.close();
		materialFile.close();

	}
}
