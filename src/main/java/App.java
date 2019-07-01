import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import com.lathrum.VMF2OBJ.VMF;
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
		String keyValueRegex = "(\"[a-zA-z._0-9]+\")(\"[-a-zA-z.,_0-9/)(\\]\\[ ]+\")";
		String objectCommaRegex = "[}\\]]\"";
		String cleanUpRegex = ",([}\\]])";

		text = text.replaceAll("\\\\", "/"); // Replace backslashs with forwardslashs
		text = text.replaceAll("//(.*)", ""); // Remove all commented lines
		text = text.replaceAll("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)", ""); // Remove all whitespaces and newlines not in quotes
		
		String solids = "";
		Pattern solidPattern = Pattern.compile("solid");
		Matcher solidMatcher = solidPattern.matcher(text);
		while (solidMatcher.find()) {
			if (text.charAt(solidMatcher.end()) == '{')
      {
        int startIndex = solidMatcher.end();
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
						side = side.replaceAll(keyValueRegex,"$1:$2,");
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
			}
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
		solids = ",\"solids\":["+solids.substring(0,solids.length()-1)+"]";
		entities = ",\"entities\":["+entities.substring(0,entities.length()-1)+"]";
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

		ArrayList<String> faces = new ArrayList<String>();
		ArrayList<String> faceMaterials = new ArrayList<String>();
		ArrayList<String> verticies = new ArrayList<String>();

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

		VMF vmf = parseVMF(text);
		System.out.println(gson.toJson(vmf));
		System.exit(0);
		
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
