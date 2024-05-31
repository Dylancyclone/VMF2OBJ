package com.lathrum.VMF2OBJ.dataStructure.model;

import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lathrum.VMF2OBJ.VMF2OBJ;

public class QC {
	public String ModelName;

	public String[] BodyGroups;

	public String[] CDMaterials;

	public SMDTriangle[] triangles;

	// public String TextureGroup; //oof

	public QC(String ModelName, String[] BodyGroups, String[] CDMaterials) {
		this.ModelName = ModelName;
		this.BodyGroups = BodyGroups;
		this.CDMaterials = CDMaterials;
	}

	public static QC parseQC(String text) {

		// Why does valve have to be so inconsistant yet so strict?
		// From https://developer.valvesoftware.com/wiki/$texturegroup
		// Bug: You must add spaces between the {} and the "". Adding a new skin with
		// {"skin0"} will not work, but { "skin0" } will.
		// Whyy

		text = text.replaceAll("\\\\", "/"); // Replace backslashs with forwardslashs
		text = text.replaceAll("//(.*)", ""); // Remove all commented lines
		text = text.replaceAll("\\$[^\" \\t]+\\n", ""); // Remove all keyless values
		text = text.replaceAll("[^\"](\\$[^\" \\t]+)", "\"$1\""); // fix unquoted keys
		text = text.replaceAll("(\".+\"[ \\t]+)([^\" \\t\\s\\{].*)", "$1\"$2\""); // fix unquoted values
		text = text.replaceAll("\\$", ""); // Remove all key prefixes
		text = text.replaceAll("\\{ (\".+\") +\"\\}\"", "$1"); // Fix texturegroup formatting
		text = text.replaceAll("[\\t\\r\\n]", ""); // Remove all whitespaces and newlines not in quotes
		text = text.replaceAll("\" +\"", "\"\""); // Remove all whitespaces and newlines not in quotes
		// text = text.replaceAll("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)", ""); // Remove all whitespaces and newlines not in quotes

		Matcher modelNameMatcher = Pattern.compile("\"modelname\"\"([,a-zA-Z0-9._/-]+)\"").matcher(text);
		String modelName = "model.smd";
		if (modelNameMatcher.find()) {
			modelName = modelNameMatcher.group(1);
		} else {
			VMF2OBJ.logger.log(Level.WARNING, "Failed to find modelName");
		}

		Matcher bodyGroupMatcher = Pattern.compile("(\"bodygroup\"\"[a-zA-Z0-9._/]+\"\\{([a-zA-Z0-9._/\" ]+)\\})")
				.matcher(text);
		Collection<String> bodygroups = new LinkedList<String>();
		while (bodyGroupMatcher.find()) {
			String working = bodyGroupMatcher.group(2);

			Matcher stringMatcher = Pattern.compile("\"([a-zA-Z0-9._/ ]+)\"").matcher(working);
			while (stringMatcher.find()) {
				bodygroups.add(stringMatcher.group(1));
			}

			text = text.replace(bodyGroupMatcher.group(1), ""); // snip this section
			bodyGroupMatcher = Pattern.compile("(\"bodygroup\"\"[a-zA-Z0-9._/]+\"\\{([a-zA-Z0-9._/\" ]+)\\})").matcher(text);
		}

		Matcher cdMaterialsMatcher = Pattern.compile("\"cdmaterials\"\"([a-zA-Z0-9._/]+)\"").matcher(text);
		Collection<String> cdMaterials = new LinkedList<String>();
		while (cdMaterialsMatcher.find()) {
			cdMaterials.add(cdMaterialsMatcher.group(1));
		}

		return new QC(modelName, bodygroups.toArray(new String[bodygroups.size()]),
				cdMaterials.toArray(new String[cdMaterials.size()]));
	}

}
