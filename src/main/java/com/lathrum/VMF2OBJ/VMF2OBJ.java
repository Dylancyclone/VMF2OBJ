package com.lathrum.VMF2OBJ;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.*;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.URI;
import java.net.URISyntaxException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import me.tongfei.progressbar.DelegatingProgressBarConsumer;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

import com.lathrum.VMF2OBJ.fileStructure.*;
import com.lathrum.VMF2OBJ.dataStructure.*;
import com.lathrum.VMF2OBJ.dataStructure.map.*;
import com.lathrum.VMF2OBJ.dataStructure.model.*;
import com.lathrum.VMF2OBJ.dataStructure.texture.*;

public class VMF2OBJ {

	public static Gson gson = new Gson();
	public static Process proc;
	public static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	public static String VTFLibPath;
	public static String CrowbarLibPath;
	public static boolean quietMode = false;
	public static boolean ignoreTools = false;
	public static boolean flipDisplacements = false;
	public static String appVersion;

	/**
	 * Extract Libraries to temporary directory.
	 * @param dir Directory to extra to
	 */
	public static void extractLibraries(String dir) throws URISyntaxException {
		// Spooky scary, but I don't want to reinvent the wheel.
		ArrayList<String> files = new ArrayList<String>();
		File tempFolder = new File(dir);
		tempFolder.mkdirs();
		tempFolder.deleteOnExit();

		// VTFLIB
		// http://nemesis.thewavelength.net/index.php?p=40
		// For converting VTF files to TGA files
		files.add("DevIL.dll"); // VTFLib dependency
		files.add("VTFLib.dll"); // VTFLib dependency
		files.add("VTFCmd.exe"); // VTFLib itself
		// Crowbar
		// https://steamcommunity.com/groups/CrowbarTool
		// https://github.com/UltraTechX/Crowbar-Command-Line
		// For converting MDL files to SMD files
		files.add("CrowbarCommandLineDecomp.exe"); // Crowbar itself

		URI uri = new URI("");
		URI fileURI;

		try {
			uri = VMF2OBJ.class.getProtectionDomain().getCodeSource().getLocation().toURI();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to get executable's location, do you have permissions?");
			logger.log(Level.SEVERE, e.toString());
		}

		try {
			ZipFile zipFile = new ZipFile(new File(uri));
			for (String el : files) {
				fileURI = extractFile(zipFile, el, dir);
				switch (el) {
					case ("VTFCmd.exe"):
						VTFLibPath = Paths.get(fileURI).toString();
						break;
					case ("CrowbarCommandLineDecomp.exe"):
						CrowbarLibPath = Paths.get(fileURI).toString();
						break;
					default:
						break;
				}
			}
			zipFile.close();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to extract tools, do you have permissions?");
			logger.log(Level.SEVERE, e.toString());
		}
	}

	/**
	 * Extract a particular file from a zip to a particular location.
	 * @param zipFile The zip file to extract from
	 * @param fileName the file to extract from the zip
	 * @param dir the destination of the extracted file
	 * @return URI of extracted file
	 */
	public static URI extractFile(ZipFile zipFile, String fileName, String dir) throws IOException {
		File tempFile;
		ZipEntry entry;
		InputStream zipStream;
		OutputStream fileStream;

		tempFile = new File(dir + File.separator + fileName);
		tempFile.createNewFile();
		tempFile.deleteOnExit();
		entry = zipFile.getEntry(fileName);

		if (entry == null) {
			throw new FileNotFoundException("Cannot find file: " + fileName + " in archive: " + zipFile.getName());
		}

		zipStream = zipFile.getInputStream(entry);
		fileStream = null;

		try {
			final byte[] buf;
			int i;

			fileStream = new FileOutputStream(tempFile);
			buf = new byte[1024];
			i = 0;

			while ((i = zipStream.read(buf)) != -1) {
				fileStream.write(buf, 0, i);
			}
		} finally {
			zipStream.close();
			fileStream.close();
		}

		return (tempFile.toURI());
	}

	/**
	 * Gets the extension of a file.
	 * @param file File to get extension of
	 * @return Extension of file
	 */
	public static String getFileExtension(File file) {
		String fileName = file.getName();
		int dotIndex = fileName.lastIndexOf('.');
		return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
	}

	/**
	 * Deletes all files and folders starting at a directory.
	 * @param path Folder to delete
	 * @return If the delete was successful
	 */
	public static boolean deleteRecursive(File path) throws FileNotFoundException {
		// if (!path.exists())
		// 	throw new FileNotFoundException(path.getAbsolutePath());
		boolean ret = true;
		if (path.isDirectory()) {
			for (File f : path.listFiles()) {
				ret = ret && deleteRecursive(f);
			}
		}
		return ret && path.delete();
	}


	/**
	 * Deletes all files with a certain extension recursively starting at a directory.
	 * @param path Root folder to delete from
	 * @param ext Extension of files to delete
	 * @return If the delete was successful
	 */
	public static boolean deleteRecursiveByExtension(File path, String ext) throws FileNotFoundException {
		// if (!path.exists())
		// 	throw new FileNotFoundException(path.getAbsolutePath());
		boolean ret = true;
		if (path.isDirectory()) {
			for (File f : path.listFiles()) {
				ret = ret && deleteRecursiveByExtension(f, ext);
			}
		} else {
			ret = ret && (getFileExtension(path).matches(ext)) ? path.delete() : true;
		}
		return ret;
	}

	/**
	 * Get contents of a file.
	 * @param path location of the file
	 * @return content of the file
	 */
	static String readFile(String path) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, StandardCharsets.UTF_8);
	}

	/**
	 * Format a path to use correct file separators.
	 * @param res Path to format
	 * @return Formatted path
	 */
	public static String formatPath(String res) {
		if (res == null) {
			return null;
		}
		if (File.separatorChar == '\\') {
			// From Windows to Linux/Mac
			return res.replace('/', File.separatorChar);
		} else {
			// From Linux/Mac to Windows
			return res.replace('\\', File.separatorChar);
		}
	}

	/**
	 * Get index of an entry in an ArrayList by it's path.
	 * @param object ArrayList of Entries to search
	 * @param path Path to find
	 * @return index of entry
	 */
	public static int getEntryIndexByPath(ArrayList<Entry> object, String path) {
		for (int i = 0; i < object.size(); i++) {
			if (object != null && object.get(i).getFullPath().equalsIgnoreCase(path)) {
				return i;
			}
			// else{logger.log(Level.FINE, object.get(i).getFullPath());}
		}
		return -1;
	}

	/**
	 * Get index of an entry in an ArrayList by an arbitrary pattern.
	 * @param object ArrayList of Entries to search
	 * @param pattern Path to find
	 * @return index of entry
	 */
	public static ArrayList<Integer> getEntryIndiciesByPattern(ArrayList<Entry> object, String pattern) {
		ArrayList<Integer> indicies = new ArrayList<Integer>();
		for (int i = 0; i < object.size(); i++) {
			if (object != null && object.get(i).getFullPath().toLowerCase().contains(pattern.toLowerCase())) {
				indicies.add(i);
			}
			// else{logger.log(Level.FINE, object.get(i).getFullPath());}
		}
		return indicies;
	}

	/**
	 * Get index of an Texture in an ArrayList by it's name.
	 * @param object ArrayList of Textures to search
	 * @param name Name to find
	 * @return index of entry
	 */
	public static int getTextureIndexByName(ArrayList<Texture> object, String name) {
		for (int i = 0; i < object.size(); i++) {
			if (object != null && object.get(i).name.equalsIgnoreCase(name)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Get index of an Texture in an ArrayList by it's file name.
	 * @param object ArrayList of Textures to search
	 * @param name File name to find
	 * @return index of entry
	 */
	public static int getTextureIndexByFileName(ArrayList<Texture> object, String name) {
		for (int i = 0; i < object.size(); i++) {
			if (object != null && object.get(i).fileName.equalsIgnoreCase(name)) {
				return i;
			}
		}
		return -1;
	}


	/**
	 * Prepare external files to be used as resources.
	 * @param start Root path to start pulling from
	 * @param dir Current directory
	 * @return ArrayList of new Entries
	 */
	public static ArrayList<Entry> addExtraFiles(String start, File dir) {
		ArrayList<Entry> entries = new ArrayList<Entry>();

		try {
			File[] files = dir.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					String path = file.getCanonicalPath().substring(start.length());
					if (path.charAt(0) == File.separatorChar) {
						path = path.substring(1);
					}
					// logger.log(Level.FINE, "directory: " + path);
					entries.addAll(addExtraFiles(start, file));
				} else {
					String path = file.getCanonicalPath().substring(start.length());
					if (path.charAt(0) == File.separatorChar) {
						path = path.substring(1);
					}
					path = path.replaceAll("\\\\", "/");
					// logger.log(Level.FINE, "file: " + path);
					if (path.lastIndexOf("/") == -1) {
						entries.add(new FileEntry(file.getName().substring(0, file.getName().lastIndexOf('.')),
								getFileExtension(file), "", file.toString()));
					} else {
						entries.add(new FileEntry(file.getName().substring(0, file.getName().lastIndexOf('.')),
								getFileExtension(file), path.substring(0, path.lastIndexOf("/")), file.toString()));
					}
				}
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to load external resources");
			logger.log(Level.SEVERE, e.toString());
		}
		return entries;
	}

	/**
	 * Print line to console without messing up the progress bar.
	 * @param text line to print
	 */
	public static void printProgressBar(String text) {
		if (quietMode) {
			return; // Supress warnings
		}
		Terminal terminal;
		int consoleWidth = 80;
		try {
			// Issue #42
			// Defaulting to a dumb terminal when a supported terminal can not be correctly
			// created
			// see https://github.com/jline/jline3/issues/291
			terminal = TerminalBuilder.builder().dumb(true).build();
			if (terminal.getWidth() >= 10)  { // Workaround for issue #23 under IntelliJ
				consoleWidth = terminal.getWidth();
			}
		} catch (IOException ignored) { /* */ }
		String pad = new String(new char[Math.max(consoleWidth - text.length(), 0)]).replace("\0", " ");

		logger.log(Level.INFO, "\r" + text + pad);
	}

	/**
	 * Convert Source .vmf files to generic .obj
	 * @param job Job object that represents the configuration for the conversion
	 * @throws Exception Unhandled exception
	 */
	public static void main(Job job) throws Exception {
		// The General outline of the program is as follows:

		// Read Geometry
		// Collapse Vertices
		// Write Objects
		// Extract Models
		// Extract Materials
		// Convert Materials
		// Convert Models to SMD
		// Convert Models to OBJ
		// Write Models
		// Write Materials
		// Clean Up

		// Load app version
		final Properties properties = new Properties();
		properties.load(VMF2OBJ.class.getClassLoader().getResourceAsStream("project.properties"));
		appVersion = properties.getProperty("version");

		Scanner in;
		ProgressBarBuilder pbb;
		ArrayList<Entry> vpkEntries = new ArrayList<Entry>();
		HashMap<String, VMT> materialCache = new HashMap<String, VMT>();
		HashMap<String, QC> modelCache = new HashMap<String, QC>();
		PrintWriter objFile;
		PrintWriter materialFile;
		String outPath = job.file.outPath;
		String objName = job.file.objFile.toString();
		String matLibName = job.file.mtlFile.toString();
		quietMode = job.SuppressWarnings;
		ignoreTools = job.skipTools;
		flipDisplacements = job.flipDisplacements;
		final File tempDir;

		// Clean working directory
		try {
			deleteRecursiveByExtension(new File(Paths.get(outPath).getParent().resolve("materials").toString()), "vtf"); // Delete unconverted textures
		} catch (Exception ignored) {}
		tempDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "vmf2objtemp");
		tempDir.mkdirs();
		// When the program shuts down, delete temporary directory
		Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        try {
					deleteRecursive(tempDir);
				} catch (Exception ignored) { ignored.printStackTrace();}
      }
    });

		// Extract Libraries
		try {
			extractLibraries(tempDir.getAbsolutePath());
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to extract tools, do you have permissions?");
			logger.log(Level.SEVERE, e.toString());
		}

		logger.log(Level.INFO, "Starting VMF2OBJ conversion v" + appVersion);

		//
		// Read VPK
		//

		// Load resources
		logger.log(Level.INFO, "[1/5] Reading VPK file(s) and custom content...");
		for (Path path : job.resourcePaths) {
			if (Files.isDirectory(path)) {
				vpkEntries.addAll(addExtraFiles(path.toString(), path.toFile()));
			} else {
				VPK vpk = new VPK(path.toFile());
				try {
					vpk.load();
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Error while loading vpk file: " + e.getMessage());
					return;
				}

				for (Directory directory : vpk.getDirectories()) {
					for (Entry entry : directory.getEntries()) {
						vpkEntries.add(entry);
					}
				}
			}
		}

		// Read input file
		String text = "";
		try {
			text = readFile(job.file.vmfFile.toString());
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to read file: " + job.file.vmfFile + ", does file exist?");
			logger.log(Level.SEVERE, e.toString());
		}
		// logger.log(Level.FINE, text);

		try {
			File directory = new File(new File(outPath).getParent());
			if (!directory.exists()) {
				directory.mkdirs();
			}

			in = new Scanner(job.file.vmfFile);
			objFile = new PrintWriter(new FileOutputStream(objName));
			materialFile = new PrintWriter(new FileOutputStream(matLibName));
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error while opening file: " + e.getMessage());
			return;
		}

		//
		// Read Geometry
		//

		logger.log(Level.INFO, "[2/5] Reading geometry...");

		VMF vmf = VMF.parseVMF(text);
		vmf = VMF.parseSolids(vmf);

		//
		// Write brushes
		//

		ArrayList<Vector3> verticies = new ArrayList<Vector3>();
		ArrayList<Face> faces = new ArrayList<Face>();

		ArrayList<String> materials = new ArrayList<String>();
		ArrayList<Texture> textures = new ArrayList<Texture>();
		int vertexOffset = 1;
		int vertexTextureOffset = 1;
		int vertexNormalOffset = 1;
		logger.log(Level.INFO, "[3/5] Writing brushes...");

		objFile.println("# Decompiled with VMF2OBJ v" + appVersion + " by Dylancyclone\n");
		materialFile.println("# Decompiled with VMF2OBJ v" + appVersion + " by Dylancyclone\n");
		objFile.println("mtllib "
				+ matLibName.substring(formatPath(matLibName).lastIndexOf(File.separatorChar) + 1, matLibName.length()));

		if (vmf.solids != null) { // There are no brushes in this VMF
			pbb = new ProgressBarBuilder().setStyle(ProgressBarStyle.ASCII).setTaskName("Writing Brushes...").setConsumer(new DelegatingProgressBarConsumer(logger::info)).showSpeed();
			for (Solid solid : ProgressBar.wrap(Arrays.asList(vmf.solids), pbb)) {
				verticies.clear();
				faces.clear();
				materials.clear();

				for (Side side : solid.sides) {
					if (ignoreTools && side.material.toLowerCase().contains("tools/")) {
						continue;
					}
					materials.add(side.material.trim());
					if (side.dispinfo == null) {
						if (Solid.isDisplacementSolid(solid)) {
							continue;
						}
						for (Vector3 point : side.points) {
							verticies.add(point);
						}
					} else {
						// Points are defined in this order:
						// 3 0
						// 2 1
						// -or-
						// D A
						// C B
						int startIndex = side.dispinfo.startposition.closestIndex(side.points);

						// Some maps have the displacements seemingly inverted.
						// Not sure what causes this, but it seems to be consistent within a map.
						// Meaning, if the user notices the displacements are off, they can
						// enable this setting to attempt to fix them across the entire map.
						// See: https://github.com/Dylancyclone/VMF2OBJ/issues/30#issuecomment-1059908043
						if (flipDisplacements) startIndex = Math.floorMod((startIndex - 2), 4);

						//Get adjacent points by going around counter-clockwise
						Vector3 a = side.points[Math.floorMod((startIndex - 2), 4)];
						Vector3 b = side.points[Math.floorMod((startIndex - 1), 4)];
						Vector3 c = side.points[startIndex];
						Vector3 d = side.points[Math.floorMod((startIndex + 1), 4)];
						Vector3 cd = d.subtract(c);
						Vector3 cb = b.subtract(c);
						Vector3 ba = a.subtract(b);
						// logger.log(Level.FINE, cd);
						// logger.log(Level.FINE, cb);
						for (int i = 0; i < side.dispinfo.normals.length; i++) { // rows
							for (int j = 0; j < side.dispinfo.normals[0].length; j++) { // columns
								double rowProgress = (double)j / (double)(side.dispinfo.normals[0].length - 1);
								double colProgress = (double)i / (double)(side.dispinfo.normals.length - 1);
								Vector3 point = side.points[startIndex]
										.add(
											cd.multiply(colProgress).multiply(1 - rowProgress).add(
											ba.multiply(colProgress).multiply(rowProgress))
										)
										.add(cb.multiply(rowProgress))
										.add(side.dispinfo.normals[i][j].multiply(side.dispinfo.distances[i][j]));
								verticies.add(point);
							}
						}
					}
				}

				Set<Vector3> uniqueVerticies = new HashSet<Vector3>(verticies);
				ArrayList<Vector3> uniqueVerticiesList = new ArrayList<Vector3>(uniqueVerticies);

				Set<String> uniqueMaterials = new HashSet<String>(materials);
				ArrayList<String> uniqueMaterialsList = new ArrayList<String>(uniqueMaterials);

				// Write Faces

				objFile.println("\n");
				objFile.println("o " + solid.id + "\n");

				for (Vector3 e : uniqueVerticiesList) {
					objFile.println("v " + e.x + " " + e.y + " " + e.z);
				}

				for (String el : uniqueMaterialsList) {
					el = el.toLowerCase();

					VMT vmt = materialCache.get(el); // Check if material has been processed before
					if (vmt != null) {
						vmt = gson.fromJson(gson.toJson(vmt, VMT.class), VMT.class); // Deep copy so the cache isn't modified
					} else { // If it has not yet been processed, process it
						// Read File
						String VMTText = "";
						try {
							int index = getEntryIndexByPath(vpkEntries, "materials/" + el + ".vmt");
							if (index == -1) {
								printProgressBar("Missing Material: " + el);
								continue;
							}
							VMTText = new String(vpkEntries.get(index).readData());
						} catch (IOException e) {
							logger.log(Level.SEVERE, "Failed to read material: " + el);
							logger.log(Level.SEVERE, e.toString());
						}

						try {
							vmt = VMT.parseVMT(VMTText);
						} catch (Exception ex) {
							printProgressBar("Failed to parse Material: " + el);
							continue;
						}
						vmt.name = el;

						if (vmt.basetexture == null || vmt.basetexture.isEmpty()) {
							printProgressBar("Material has no texture: " + el);
							continue;
						}
						if (vmt.basetexture.endsWith(".vtf")) {
							vmt.basetexture = vmt.basetexture.substring(0, vmt.basetexture.lastIndexOf('.')); // snip the extension
						}
						materialCache.put(el, vmt);
						vmt = gson.fromJson(gson.toJson(vmt, VMT.class), VMT.class); // Deep copy so the cache isn't modified
					}
					
					int index = getEntryIndexByPath(vpkEntries, "materials/" + vmt.basetexture + ".vtf");
					// logger.log(Level.FINE, index);
					if (index != -1) {
						File materialOutPath = new File(outPath);
						materialOutPath = new File(
								formatPath(materialOutPath.getParent() + File.separator + vpkEntries.get(index).getFullPath()));
						if (!materialOutPath.exists()) {
							try {
								File directory = new File(materialOutPath.getParent());
								if (!directory.exists()) {
									directory.mkdirs();
								}
							} catch (Exception e) {
								logger.log(Level.SEVERE, "Failed to create directory: " + materialOutPath.getParent());
								logger.log(Level.SEVERE, e.toString());
							}
							try {
								vpkEntries.get(index).extract(materialOutPath);
								String[] command = new String[] {
									VTFLibPath,
									"-folder", formatPath(materialOutPath.toString()),
									"-output", formatPath(materialOutPath.getParent()),
									"-exportformat", "tga",
									"-format", "BGR888"};

								if (vmt.translucent == 1 || vmt.alphatest == 1) {
									command[8] = "BGRA8888"; // Only include alpha channel if it's a transparent texture
								}

								proc = Runtime.getRuntime().exec(command);
								BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
								while ((reader.readLine()) != null) {}
								proc.waitFor();
								// materialOutPath.delete();
								materialOutPath = new File(
									materialOutPath.toString().substring(0, materialOutPath.toString().lastIndexOf('.')) + ".tga");

								int width = 1;
								int height = 1;
								try {
									byte[] fileContent = Files.readAllBytes(materialOutPath.toPath());
									width = TargaReader.getWidth(fileContent); 
									height = TargaReader.getHeight(fileContent);
								} catch (Exception e) {
									logger.log(Level.WARNING, "Cant read Material: " + materialOutPath);
									// logger.log(Level.SEVERE, e);
								}
								// logger.log(Level.FINE, "Adding Material: "+ el);
								textures.add(new Texture(el, vmt.basetexture, materialOutPath.toString(), width, height));
							} catch (Exception e) {
								logger.log(Level.SEVERE, "Failed to extract material: " + vmt.basetexture);
								logger.log(Level.SEVERE, e.toString());
							}

							if (vmt.bumpmap != null) { // If the material has a bump map associated with it
								if (vmt.bumpmap.endsWith(".vtf")) {
									vmt.bumpmap = vmt.bumpmap.substring(0, vmt.bumpmap.lastIndexOf('.')); // snip the extension
								}
								// logger.log(Level.FINE, "Bump found on "+vmt.basetexture+": "+vmt.bumpmap);
								int bumpMapIndex = getEntryIndexByPath(vpkEntries, "materials/" + vmt.bumpmap + ".vtf");
								// logger.log(Level.FINE, bumpMapIndex);
								if (bumpMapIndex != -1) {
									File bumpMapOutPath = new File(outPath);
									bumpMapOutPath = new File(formatPath(
											bumpMapOutPath.getParent() + File.separator + vpkEntries.get(bumpMapIndex).getFullPath()));
									if (!bumpMapOutPath.exists()) {
										try {
											File directory = new File(bumpMapOutPath.getParent());
											if (!directory.exists()) {
												directory.mkdirs();
											}
										} catch (Exception e) {
											logger.log(Level.SEVERE, "Failed to create directory: " + materialOutPath.getParent());
											logger.log(Level.SEVERE, e.toString());
										}
										try {
											vpkEntries.get(bumpMapIndex).extract(bumpMapOutPath);
											String[] command = new String[] {
												VTFLibPath,
												"-folder", formatPath(bumpMapOutPath.toString()),
												"-output", formatPath(bumpMapOutPath.getParent()),
												"-exportformat", "tga",
												"-format", "BGR888"};
											proc = Runtime.getRuntime().exec(command);
											BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
											while ((reader.readLine()) != null) {}
											proc.waitFor();
										} catch (Exception e) {
											logger.log(Level.SEVERE, "Failed to extract bump material: " + vmt.bumpmap);
											logger.log(Level.SEVERE, e.toString());
										}
									}
								}
							}

							materialFile.println("\n" +
							"newmtl " + el + "\n" +
							"Ka 1.000 1.000 1.000\n" +
							"Kd 1.000 1.000 1.000\n" +
							"Ks 0.000 0.000 0.000\n" +
							"map_Ka " + "materials/" + vmt.basetexture + ".tga\n" +
							"map_Kd " + "materials/" + vmt.basetexture + ".tga");
							if (vmt.bumpmap != null) { //If the material has a bump map associated with it
								materialFile.println(
								"map_bump " + "materials/" + vmt.bumpmap + ".tga");
							}
							if (vmt.translucent == 1 || vmt.alphatest == 1) { // If the material has transparency
								materialFile.println(
								// "map_d -imfchan m" + "materials/" + vmt.basetexture + ".tga\n" + // Many programs fail to import the alpha channel of the tga file and instead import the color channel
								"illum 4");
							}
							materialFile.println();
						} else { // File has already been extracted
							int textureIndex = getTextureIndexByName(textures, el);
							if (textureIndex == -1) { // But this is a new material
								textureIndex = getTextureIndexByFileName(textures, vmt.basetexture);
								// logger.log(Level.FINE, "Adding Material: "+ el);
								textures.add(new Texture(el, vmt.basetexture, materialOutPath.toString(),
										textures.get(textureIndex).width, textures.get(textureIndex).height));

								materialFile.println("\n" +
								"newmtl " + el + "\n" +
								"Ka 1.000 1.000 1.000\n" +
								"Kd 1.000 1.000 1.000\n" +
								"Ks 0.000 0.000 0.000\n" +
								"map_Ka " + "materials/" + vmt.basetexture + ".tga\n" +
								"map_Kd " + "materials/" + vmt.basetexture + ".tga");
								if (vmt.bumpmap != null) { //If the material has a bump map associated with it
									materialFile.println(
									"map_bump " + "materials/" + vmt.bumpmap + ".tga");
								}
								if (vmt.translucent == 1 || vmt.alphatest == 1) { // If the material has transparency
									materialFile.println(
									// "map_d -imfchan m" + "materials/" + vmt.basetexture + ".tga\n" + // Many programs fail to import the alpha channel of the tga file and instead import the color channel
									"illum 4");
								}
								materialFile.println();
							}
						}
					} else { // Cant find material
						int textureIndex = getTextureIndexByName(textures, el);
						if (textureIndex == -1) { // But this is a new material
							printProgressBar("Missing Material: " + vmt.basetexture);
							textures.add(new Texture(el, vmt.basetexture, "", 1, 1));
						}
					}
				}
				objFile.println();

				for (Side side : solid.sides) {
					if (ignoreTools && side.material.toLowerCase().contains("tools/")) {
						continue;
					}
					int index = getTextureIndexByName(textures, side.material.trim());
					if (index == -1) {
						continue;
					}
					Texture texture = textures.get(index);

					side.uAxisTranslation = side.uAxisTranslation % texture.width;
					side.vAxisTranslation = side.vAxisTranslation % texture.height;

					if (side.uAxisTranslation < -texture.width / 2) {
						side.uAxisTranslation += texture.width;
					}

					if (side.vAxisTranslation < -texture.height / 2) {
						side.vAxisTranslation += texture.height;
					}

					String buffer = "";

					if (side.dispinfo == null) {
						if (Solid.isDisplacementSolid(solid)) {
							continue;
						}
						for (int i = 0; i < side.points.length; i++) {
							double u = Vector3.dot(side.points[i], side.uAxisVector) / (texture.width * side.uAxisScale)
									+ side.uAxisTranslation / texture.width;
							double v = Vector3.dot(side.points[i], side.vAxisVector) / (texture.height * side.vAxisScale)
									+ side.vAxisTranslation / texture.height;
							u = -u + texture.width;
							v = -v + texture.height;
							objFile.println("vt " + u + " " + v);
							buffer += (uniqueVerticiesList.indexOf(side.points[i]) + vertexOffset) + "/" + (i + vertexTextureOffset)
									+ " ";
						}
						faces.add(new Face(buffer, side.material.trim().toLowerCase()));
						vertexTextureOffset += side.points.length;
					} else {
						// Points are defined in this order:
						// 3 0
						// 2 1
						// -or-
						// D A
						// C B
						int startIndex = side.dispinfo.startposition.closestIndex(side.points);

						// Some maps have the displacements seemingly inverted.
						// Not sure what causes this, but it seems to be consistent within a map.
						// Meaning, if the user notices the displacements are off, they can
						// enable this setting to attempt to fix them across the entire map.
						// See: https://github.com/Dylancyclone/VMF2OBJ/issues/30#issuecomment-1059908043
						if (flipDisplacements) startIndex = Math.floorMod((startIndex - 2), 4);

						//Get adjacent points by going around counter-clockwise
						Vector3 a = side.points[Math.floorMod((startIndex - 2), 4)];
						Vector3 b = side.points[Math.floorMod((startIndex - 1), 4)];
						Vector3 c = side.points[startIndex];
						Vector3 d = side.points[Math.floorMod((startIndex + 1), 4)];
						Vector3 cd = d.subtract(c);
						Vector3 cb = b.subtract(c);
						Vector3 ba = a.subtract(b);
						// logger.log(Level.FINE, cd);
						// logger.log(Level.FINE, cb);
						for (int i = 0; i < side.dispinfo.normals.length - 1; i++) { // all rows but last
							for (int j = 0; j < side.dispinfo.normals[0].length - 1; j++) { // all columns but last
								buffer = "";
								double rowProgress, colProgress, u, v;

								rowProgress = (double)j / (double)(side.dispinfo.normals[0].length - 1);
								colProgress = (double)i / (double)(side.dispinfo.normals.length - 1);
								Vector3 point = side.points[startIndex]
										.add(
											cd.multiply(colProgress).multiply(1 - rowProgress).add(
											ba.multiply(colProgress).multiply(rowProgress))
										)
										.add(cb.multiply(rowProgress))
										.add(side.dispinfo.normals[i][j].multiply(side.dispinfo.distances[i][j]));
								u = Vector3.dot(point, side.uAxisVector) / (texture.width * side.uAxisScale)
										+ side.uAxisTranslation / texture.width;
								v = Vector3.dot(point, side.vAxisVector) / (texture.height * side.vAxisScale)
										+ side.vAxisTranslation / texture.height;
								v = -v + texture.height;
								objFile.println("vt " + u + " " + v);
								buffer += (uniqueVerticiesList.indexOf(point) + vertexOffset) + "/"
										+ ((((side.dispinfo.normals.length - 1) * i) + j) * 4 + vertexTextureOffset) + " ";

								rowProgress = (double)j / (double)(side.dispinfo.normals[0].length - 1);
								colProgress = (double)(i + 1) / (double)(side.dispinfo.normals.length - 1);
								point = side.points[startIndex]
										.add(
											cd.multiply(colProgress).multiply(1 - rowProgress).add(
											ba.multiply(colProgress).multiply(rowProgress))
										)
										.add(cb.multiply(rowProgress))
										.add(side.dispinfo.normals[i + 1][j].multiply(side.dispinfo.distances[i + 1][j]));
								u = Vector3.dot(point, side.uAxisVector) / (texture.width * side.uAxisScale)
										+ side.uAxisTranslation / texture.width;
								v = Vector3.dot(point, side.vAxisVector) / (texture.height * side.vAxisScale)
										+ side.vAxisTranslation / texture.height;
								v = -v + texture.height;
								objFile.println("vt " + u + " " + v);
								buffer += (uniqueVerticiesList.indexOf(point) + vertexOffset) + "/"
										+ ((((side.dispinfo.normals.length - 1) * i) + j) * 4 + vertexTextureOffset + 1) + " ";

								rowProgress = (double)(j + 1) / (double)(side.dispinfo.normals[0].length - 1);
								colProgress = (double)(i + 1) / (double)(side.dispinfo.normals.length - 1);
								point = side.points[startIndex]
										.add(
											cd.multiply(colProgress).multiply(1 - rowProgress).add(
											ba.multiply(colProgress).multiply(rowProgress))
										)
										.add(cb.multiply(rowProgress))
										.add(side.dispinfo.normals[i + 1][j + 1].multiply(side.dispinfo.distances[i + 1][j + 1]));
								u = Vector3.dot(point, side.uAxisVector) / (texture.width * side.uAxisScale)
										+ side.uAxisTranslation / texture.width;
								v = Vector3.dot(point, side.vAxisVector) / (texture.height * side.vAxisScale)
										+ side.vAxisTranslation / texture.height;
								v = -v + texture.height;
								objFile.println("vt " + u + " " + v);
								buffer += (uniqueVerticiesList.indexOf(point) + vertexOffset) + "/"
										+ ((((side.dispinfo.normals.length - 1) * i) + j) * 4 + vertexTextureOffset + 2) + " ";

								rowProgress = (double)(j + 1) / (double)(side.dispinfo.normals[0].length - 1);
								colProgress = (double)i / (double)(side.dispinfo.normals.length - 1);
								point = side.points[startIndex]
										.add(
											cd.multiply(colProgress).multiply(1 - rowProgress).add(
											ba.multiply(colProgress).multiply(rowProgress))
										)
										.add(cb.multiply(rowProgress))
										.add(side.dispinfo.normals[i][j + 1].multiply(side.dispinfo.distances[i][j + 1]));
								u = Vector3.dot(point, side.uAxisVector) / (texture.width * side.uAxisScale)
										+ side.uAxisTranslation / texture.width;
								v = Vector3.dot(point, side.vAxisVector) / (texture.height * side.vAxisScale)
										+ side.vAxisTranslation / texture.height;
								v = -v + texture.height;
								objFile.println("vt " + u + " " + v);
								buffer += (uniqueVerticiesList.indexOf(point) + vertexOffset) + "/"
										+ ((((side.dispinfo.normals.length - 1) * i) + j) * 4 + vertexTextureOffset + 3) + " ";

								faces.add(new Face(buffer, side.material.trim().toLowerCase()));
							}
						}
						vertexTextureOffset += (side.dispinfo.normals.length - 1) * (side.dispinfo.normals[0].length - 1) * 4;
					}
				}
				objFile.println();
				vertexOffset += uniqueVerticiesList.size();

				String lastMaterial = "";
				for (int i = 0; i < faces.size(); i++) {
					if (!faces.get(i).material.equals(lastMaterial)) {
						objFile.println("usemtl " + faces.get(i).material);
					}
					lastMaterial = faces.get(i).material;

					objFile.println("f " + faces.get(i).text);
				}
			}
		}

		//
		// Process Entities
		//

		logger.log(Level.INFO, ""); // Newline to make sure progressbar stays visible
		logger.log(Level.INFO, "[4/5] Processing entities...");

		if (vmf.entities != null) { // There are no entities in this VMF
			pbb = new ProgressBarBuilder().setStyle(ProgressBarStyle.ASCII).setTaskName("Processing entities...").setConsumer(new DelegatingProgressBarConsumer(logger::info)).showSpeed();
			for (Entity entity : ProgressBar.wrap(Arrays.asList(vmf.entities), pbb)) {
				if (entity.classname.contains("prop_")) { // If the entity is a prop
					if (entity.model == null) {
						printProgressBar("Prop has no model? " + entity.classname);
						continue;
					}
					verticies.clear();
					faces.clear();
					materials.clear();

					QC qc = modelCache.get(entity.model); // Check if model has been processed before
					if (qc != null) {
						qc = gson.fromJson(gson.toJson(qc, QC.class), QC.class); // Deep copy so the cache isn't modified
					} else { // If it has not yet been processed, process it

						// Crowbar has a setting that puts all decompiled models in a subfolder
						// called `DecompileFolderForEachModelIsChecked`. This is false by default,
						// but must be handled otherwise all models will fail to convert
						Boolean crowbarSubfolderSetting = false;
	
						String modelWithoutExtension = entity.model.substring(0, entity.model.lastIndexOf('.'));
						entity.modelName = modelWithoutExtension.substring(modelWithoutExtension.lastIndexOf("/"));
						ArrayList<Integer> indicies = getEntryIndiciesByPattern(vpkEntries, modelWithoutExtension + ".");
	
						indicies.removeAll(
								getEntryIndiciesByPattern(vpkEntries, modelWithoutExtension + ".vmt"));
						indicies.removeAll(
								getEntryIndiciesByPattern(vpkEntries, modelWithoutExtension + ".vtf"));
						for (int index : indicies) {
	
							if (index != -1) {
								File fileOutPath = tempDir;
								fileOutPath = new File(
										formatPath(fileOutPath + File.separator + vpkEntries.get(index).getFullPath()));
								if (!fileOutPath.exists()) {
									try {
										File directory = new File(fileOutPath.getParent());
										if (!directory.exists()) {
											directory.mkdirs();
										}
									} catch (Exception e) {
										logger.log(Level.SEVERE, "Failed to create directory: " + fileOutPath.getParent());
										logger.log(Level.SEVERE, e.toString());
									}
									try {
										vpkEntries.get(index).extract(fileOutPath);
									} catch (Exception e) {
										logger.log(Level.SEVERE, "Failed to extract: " + fileOutPath);
										logger.log(Level.SEVERE, e.toString());
									}
								}
							}
						}
	
						String[] command = new String[] {
							CrowbarLibPath,
							"-p", formatPath(tempDir + File.separator + entity.model)};
					
						proc = Runtime.getRuntime().exec(command);
						// BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
						// String line = "";
						// while ((line = reader.readLine()) != null) {
						// 	logger.log(Level.FINE, line);
						// }
						BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
						while ((reader.readLine()) != null) {}
						proc.waitFor();
	
						String qcText = "";
						try {
							// This line may cause errors if the qc file does not have the same name as the mdl file
							qcText = readFile(tempDir + File.separator + modelWithoutExtension + ".qc");
						} catch (IOException e) {
							//This will be caught by detecting a blank string
							// logger.log(Level.SEVERE, "Exception Occured: " + e.toString());
						}
						if (qcText.matches("")) {
							try {
								crowbarSubfolderSetting = true;
								// This line may cause errors if the qc file does not have the same name as the mdl file
								qcText = readFile(tempDir + File.separator + modelWithoutExtension + File.separator + entity.modelName + ".qc");
							} catch (IOException e) {
								//This will be caught by detecting a blank string
								// logger.log(Level.SEVERE, "Exception Occured: " + e.toString());
							}
							if (qcText.matches("")) {
								printProgressBar("Error: Could not find QC file for model, skipping: " + entity.model);
								continue;
							}
						}
	
						qc = QC.parseQC(qcText);
	
						ArrayList<SMDTriangle> SMDTriangles = new ArrayList<SMDTriangle>();
	
						for (String bodyGroup : qc.BodyGroups) {
							String path = "/";
							if (qc.ModelName.contains("/")) {
								path += qc.ModelName.substring(0, qc.ModelName.lastIndexOf('/'));
							}
							if (crowbarSubfolderSetting) {
								path += File.separator + entity.modelName;
							}
							String smdText = readFile(formatPath(
									tempDir + File.separator + "models" + path + File.separator + bodyGroup));
	
							SMDTriangles.addAll(Arrays.asList(SMDTriangle.parseSMD(smdText)));
						}
						qc.triangles = SMDTriangles.toArray(new SMDTriangle[] {});
						modelCache.put(entity.model, qc);
						qc = gson.fromJson(gson.toJson(qc, QC.class), QC.class); // Deep copy so the cache isn't modified
					}
	
					// Transform model
					String[] angles = entity.angles.split(" ");
					double[] radAngles = new double[3];
					radAngles[0] = Double.parseDouble(angles[0]) * Math.PI / 180;
					radAngles[1] = (Double.parseDouble(angles[1]) + 90) * Math.PI / 180;
					radAngles[2] = Double.parseDouble(angles[2]) * Math.PI / 180;
					double scale = entity.uniformscale != null ? Double.parseDouble(entity.uniformscale) : 1.0;
					String[] origin = entity.origin.split(" ");
					Vector3 transform = new Vector3(Double.parseDouble(origin[0]), Double.parseDouble(origin[1]),
							Double.parseDouble(origin[2]));
					for (int i = 0; i < qc.triangles.length; i++) {
						SMDTriangle temp = qc.triangles[i];
						materials.add(temp.materialName);
						for (int j = 0; j < temp.points.length; j++) {
							// VMF stores rotations as: YZX
							// Source stores relative to obj as: YXZ
							// Meaning translated rotations are: YXZ
							// Or what would normally be read as XZY
							temp.points[j].position = temp.points[j].position.rotate3D(radAngles[0], radAngles[2], radAngles[1]);

							temp.points[j].position = temp.points[j].position.multiply(scale);
							temp.points[j].position = temp.points[j].position.add(transform);
							verticies.add(temp.points[j].position);
						}
						qc.triangles[i] = temp;
					}

					Set<Vector3> uniqueVerticies = new HashSet<Vector3>(verticies);
					ArrayList<Vector3> uniqueVerticiesList = new ArrayList<Vector3>(uniqueVerticies);

					Set<String> uniqueMaterials = new HashSet<String>(materials);
					ArrayList<String> uniqueMaterialsList = new ArrayList<String>(uniqueMaterials);

					// Write Faces

					objFile.println("\n");
					objFile.println(
							"o " + qc.ModelName.substring(qc.ModelName.lastIndexOf('/') + 1, qc.ModelName.lastIndexOf('.')) + "\n");

					for (Vector3 e : uniqueVerticiesList) {
						objFile.println("v " + e.x + " " + e.y + " " + e.z);
					}

					for (String el : uniqueMaterialsList) {
						el = el.toLowerCase();

						VMT vmt = materialCache.get(el); // Check if material has been processed before
						if (vmt != null) {
							vmt = gson.fromJson(gson.toJson(vmt, VMT.class), VMT.class); // Deep copy so the cache isn't modified
						} else { // If it has not yet been processed, process it
							// Read File
							String VMTText = "";
							for (String cdMaterial : qc.CDMaterials) { // Our material can be in multiple directories, we gotta find it
								if (cdMaterial.endsWith("/")) {
									cdMaterial = cdMaterial.substring(0, cdMaterial.lastIndexOf('/'));
								}
								try {
									int index = getEntryIndexByPath(vpkEntries, "materials/" + cdMaterial + "/" + el + ".vmt");
									if (index == -1) {
										continue;
									} // Could not find it
									VMTText = new String(vpkEntries.get(index).readData());
								} catch (IOException e) {
									logger.log(Level.SEVERE, "Failed to read material: " + el);
									logger.log(Level.SEVERE, e.toString());
								}
								if (!VMTText.isEmpty()) {
									break;
								}
							}
							if (VMTText.isEmpty()) {
								printProgressBar("Could not find material: " + el);
								continue;
							}

							try {
								vmt = VMT.parseVMT(VMTText);
							} catch (Exception ex) {
								printProgressBar("Failed to parse Material: " + el);
								continue;
							}
							vmt.name = el;
							if (vmt.basetexture == null || vmt.basetexture.isEmpty()) {
								printProgressBar("Material has no texture: " + el);
								continue;
							}
							if (vmt.basetexture.endsWith(".vtf")) {
								vmt.basetexture = vmt.basetexture.substring(0, vmt.basetexture.lastIndexOf('.')); // snip the extension
							}
							materialCache.put(el, vmt);
							vmt = gson.fromJson(gson.toJson(vmt, VMT.class), VMT.class); // Deep copy so the cache isn't modified
						}

						int index = getEntryIndexByPath(vpkEntries, "materials/" + vmt.basetexture + ".vtf");
						// logger.log(Level.FINE, index);
						if (index != -1) {
							File materialOutPath = new File(outPath);
							materialOutPath = new File(
									formatPath(materialOutPath.getParent() + File.separator + vpkEntries.get(index).getFullPath()));
							if (!materialOutPath.exists()) {
								try {
									File directory = new File(materialOutPath.getParent());
									if (!directory.exists()) {
										directory.mkdirs();
									}
								} catch (Exception e) {
									logger.log(Level.SEVERE, "Exception Occured: " + e.toString());
								}
								try {
									vpkEntries.get(index).extract(materialOutPath);
									String[] convertCommand = new String[] {
										VTFLibPath,
										"-folder", formatPath(materialOutPath.toString()),
										"-output", formatPath(materialOutPath.getParent()),
										"-exportformat", "tga",
										"-format", "BGR888"};
	
									if (vmt.translucent == 1 || vmt.alphatest == 1) {
										convertCommand[8] = "BGRA8888"; // Only include alpha channel if it's a transparent texture
									}

									proc = Runtime.getRuntime().exec(convertCommand);
									BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
									while ((reader.readLine()) != null) {}
									proc.waitFor();
									// materialOutPath.delete();
									materialOutPath = new File(
										materialOutPath.toString().substring(0, materialOutPath.toString().lastIndexOf('.')) + ".tga");

									int width = 1;
									int height = 1;
									try {
										byte[] fileContent = Files.readAllBytes(materialOutPath.toPath());
										width = TargaReader.getWidth(fileContent); 
										height = TargaReader.getHeight(fileContent);
									} catch (Exception e) {
										logger.log(Level.WARNING, "Cant read Material: " + materialOutPath);
										// logger.log(Level.WARNING, e);
									}
									// logger.log(Level.FINE, "Adding Material: "+ el);
									textures.add(new Texture(el, vmt.basetexture, materialOutPath.toString(), width, height));
								} catch (Exception e) {
									logger.log(Level.SEVERE, "Failed to extract material: " + vmt.basetexture);
									logger.log(Level.SEVERE, e.toString());
								}

								if (vmt.bumpmap != null) { // If the material has a bump map associated with it
									if (vmt.bumpmap.endsWith(".vtf")) {
										vmt.bumpmap = vmt.bumpmap.substring(0, vmt.bumpmap.lastIndexOf('.')); // snip the extension
									}
									// logger.log(Level.FINE, "Bump found on "+vmt.basetexture+": "+vmt.bumpmap);
									int bumpMapIndex = getEntryIndexByPath(vpkEntries, "materials/" + vmt.bumpmap + ".vtf");
									// logger.log(Level.FINE, bumpMapIndex);
									if (bumpMapIndex != -1) {
										File bumpMapOutPath = new File(outPath);
										bumpMapOutPath = new File(formatPath(
												bumpMapOutPath.getParent() + File.separator + vpkEntries.get(bumpMapIndex).getFullPath()));
										if (!bumpMapOutPath.exists()) {
											try {
												File directory = new File(bumpMapOutPath.getParent());
												if (!directory.exists()) {
													directory.mkdirs();
												}
											} catch (Exception e) {
												logger.log(Level.SEVERE, "Failed to create directory: " + bumpMapOutPath.getParent());
												logger.log(Level.SEVERE, e.toString());
											}
											try {
												vpkEntries.get(bumpMapIndex).extract(bumpMapOutPath);
												String[] convertCommand = new String[] {
													VTFLibPath,
													"-folder", formatPath(bumpMapOutPath.toString()),
													"-output", formatPath(bumpMapOutPath.getParent()),
													"-exportformat", "tga",
													"-format", "BGR888"};
												proc = Runtime.getRuntime().exec(convertCommand);
												BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
												while ((reader.readLine()) != null) {}
												proc.waitFor();
											} catch (Exception e) {
												logger.log(Level.SEVERE, "Failed to extract bump material: " + vmt.bumpmap);
												logger.log(Level.SEVERE, e.toString());
											}
										}
									}
								}

								materialFile.println("\n" + 
								"newmtl " + el + "\n" +
								"Ka 1.000 1.000 1.000\n" +
								"Kd 1.000 1.000 1.000\n" +
								"Ks 0.000 0.000 0.000\n" +
								"map_Ka " + "materials/" + vmt.basetexture + ".tga\n" +
								"map_Kd " + "materials/" + vmt.basetexture + ".tga");
								if (vmt.bumpmap != null) { // If the material has a bump map associated with it
									materialFile.println("map_bump " + "materials/" + vmt.bumpmap + ".tga");
								}
								if (vmt.translucent == 1 || vmt.alphatest == 1) { // If the material has transparency
									materialFile.println(
									// "map_d -imfchan m" + "materials/" + vmt.basetexture + ".tga\n" + // Many programs fail to import the alpha channel of the tga file and instead import the color channel
									"illum 4");
								}
								materialFile.println();
							} else { // File has already been extracted
								int textureIndex = getTextureIndexByName(textures, el);
								if (textureIndex == -1) { // But this is a new material
									textureIndex = getTextureIndexByFileName(textures, vmt.basetexture);
									// logger.log(Level.FINE, "Adding Material: "+ el);
									textures.add(new Texture(el, vmt.basetexture, materialOutPath.toString(),
											textures.get(textureIndex).width, textures.get(textureIndex).height));

									materialFile.println("\n" + 
									"newmtl " + el + "\n" +
									"Ka 1.000 1.000 1.000\n" +
									"Kd 1.000 1.000 1.000\n" +
									"Ks 0.000 0.000 0.000\n" +
									"map_Ka " + "materials/" + vmt.basetexture + ".tga\n" +
									"map_Kd " + "materials/" + vmt.basetexture + ".tga");
									if (vmt.bumpmap != null) { // If the material has a bump map associated with it
										materialFile.println("map_bump " + "materials/" + vmt.bumpmap + ".tga");
									}
									if (vmt.translucent == 1 || vmt.alphatest == 1) { // If the material has transparency
										materialFile.println(
										// "map_d -imfchan m" + "materials/" + vmt.basetexture + ".tga\n" + // Many programs fail to import the alpha channel of the tga file and instead import the color channel
										"illum 4");
									}
									materialFile.println();
								}
							}
						} else { // Cant find material
							int textureIndex = getTextureIndexByName(textures, el);
							if (textureIndex == -1) { // But this is a new material
								printProgressBar("Missing Material: " + vmt.basetexture);
								textures.add(new Texture(el, vmt.basetexture, "", 1, 1));
							}
						}
					}
					objFile.println();

					for (SMDTriangle SMDTriangle : qc.triangles) {
						String buffer = "";

						for (int i = 0; i < SMDTriangle.points.length; i++) {
							double u = Double.parseDouble(SMDTriangle.points[i].uaxis);
							double v = Double.parseDouble(SMDTriangle.points[i].vaxis);
							objFile.println("vt " + u + " " + v);
							objFile.println("vn " + SMDTriangle.points[i].normal.x + " " + SMDTriangle.points[i].normal.y + " "
									+ SMDTriangle.points[i].normal.z);
							// buffer += (uniqueVerticiesList.indexOf(SMDTriangle.points[i].position) +
							// vertexOffset) + "/"+(i+vertexTextureOffset)+" ";
							buffer += (uniqueVerticiesList.indexOf(SMDTriangle.points[i].position) + vertexOffset) + "/"
									+ (i + vertexTextureOffset) + "/" + (i + vertexNormalOffset) + " ";
						}
						faces.add(new Face(buffer, SMDTriangle.materialName.toLowerCase()));
						vertexTextureOffset += SMDTriangle.points.length;
						vertexNormalOffset += SMDTriangle.points.length;
					}
					objFile.println();
					vertexOffset += uniqueVerticiesList.size();
					String lastMaterial = "";
					for (int i = 0; i < faces.size(); i++) {
						if (!faces.get(i).material.equals(lastMaterial)) {
							objFile.println("usemtl " + faces.get(i).material);
						}
						lastMaterial = faces.get(i).material;

						objFile.println("f " + faces.get(i).text);
					}
				}
			}
		}

		//
		// Clean up
		//

		logger.log(Level.INFO, ""); // Newline to make sure progressbar stays visible
		logger.log(Level.INFO, "[5/5] Cleaning up...");

		if (vmf.entities != null) { //There are no entities in this VMF
			deleteRecursive(new File(Paths.get(outPath).getParent().resolve("models").toString())); // Delete models. Everything is now in the OBJ file
		}
		deleteRecursiveByExtension(new File(Paths.get(outPath).getParent().resolve("materials").toString()), "vtf"); // Delete unconverted textures

		in.close();
		objFile.close();
		materialFile.close();

		logger.log(Level.INFO, "Conversion complete! Output can be found at: " + Paths.get(outPath).getParent());
	}
}
