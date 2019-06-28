import java.util.*;
import java.util.regex.*;
import java.io.*;

public class VMF2OBJ {

	public static void main(String args[])
	{
		//Read Geometry
		//Collapse Vertices
		//Write objects
		//Extract Models
		//Extract materials
		//Convert Materials
		//Convert models to SMD
		//Convert models to OBJ
		//Write Models
		//Write Materials

		if(args.length != 2)
		{
			System.err.println("Usage: java vmf2obj <infile> <outpath>");
			return;
		}
		
		Scanner in;
		PrintWriter outfile;
		PrintWriter materialfile;
		String objname = args[1] + ".obj";
		String matlibname = args[1] + ".mtl";
		
		int numberOfSides = 0;
		String currentLine="";

		ArrayList<String> faces= new ArrayList<String>();
		ArrayList<String> faceMaterials= new ArrayList<String>();
		ArrayList<String> verticies= new ArrayList<String>();
		
		try
		{
			in = new Scanner(new File(args[0]));
			outfile = new PrintWriter(new FileOutputStream(objname));
			materialfile = new PrintWriter(new FileOutputStream(matlibname));
		}
		catch(IOException e)
		{
			System.err.println("Error while opening file: "+e.getMessage());
			return;
		}
		
		System.out.println("Reading geometry...");
		while (in.hasNext())
		{
			currentLine = in.nextLine();
			for (String element : currentLine.split(" "))
			{
                if (element.equalsIgnoreCase("		side"))
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
