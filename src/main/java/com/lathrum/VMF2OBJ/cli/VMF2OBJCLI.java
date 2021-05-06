package com.lathrum.VMF2OBJ.cli;

import org.apache.commons.cli.*;
import java.io.*;
import java.nio.file.*;
import com.lathrum.VMF2OBJ.Job;
import com.lathrum.VMF2OBJ.VMF2OBJ;
import com.lathrum.VMF2OBJ.fileStructure.VMFFileEntry;

public class VMF2OBJCLI {

	public static void main(String[] args) throws Exception {

		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption("h", "help", false, "Show this message");
		options.addOption("o", "output", true, "Name of the output files. Defaults to the name of the VMF file");
		options.addOption("r", "resourcePaths", true,
				"Semi-colon separated list of VPK files and folders for external custom content (such as materials or models)");
		options.addOption("q", "quiet", false, "Suppress warnings");
		options.addOption("t", "tools", false, "Ignore tool brushes");

		Job job = new Job();

		// Prepare Arguments
		try {
			job.file = new VMFFileEntry(new File(args[0]), args[1]);

			// parse the command line arguments
			CommandLine cmd = parser.parse(options, args);
			if (cmd.hasOption("h") || args[0].charAt(0) == '-') {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("vmf2obj [VMF_FILE] [args...]", options, false);
				System.exit(0);
			}
			if (cmd.hasOption("o")) {
				job.file = new VMFFileEntry(new File(args[0]), cmd.getOptionValue("o"));
			} else {
				job.file = new VMFFileEntry(new File(args[0]));
			}
			if (cmd.hasOption("r")) {
				String[] resourcePaths = cmd.getOptionValue("r").split(";");
				for (String path : resourcePaths) {
					job.resourcePaths.add(Paths.get(path));
				}
			}
			if (cmd.hasOption("q")) {
				job.SuppressWarnings = true;
			}
			if (cmd.hasOption("t")) {
				job.skipTools = true;
			}
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("vmf2obj [VMF_FILE] [args...]", options, false);
			System.exit(0);
		} catch (Exception e) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("vmf2obj [VMF_FILE] [args...]", options, false);
			System.exit(0);
		}

		// Check for valid arguments
		if (job.file.objFile.getParent() == null) {
			System.err.println("Invalid output file. Make sure it's either an absolute or relative path");
			System.exit(0);
		}

		VMF2OBJ.main(job);
	}
}