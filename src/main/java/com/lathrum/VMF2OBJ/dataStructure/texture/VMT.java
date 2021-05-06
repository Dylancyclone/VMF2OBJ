package com.lathrum.VMF2OBJ.dataStructure.texture;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lathrum.VMF2OBJ.VMF2OBJ;

public class VMT {
	public String name;

	public String basetexture;

	public int translucent;
	public int alphatest;

	public String bumpmap;

	public String detail;
	public String detailscale;
	public String detailblendfactor;

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

	public static VMT parseVMT(String text) {

		if (text.length() > 6) {
			if (text.substring(1, 6).equals("Water")) // If water texture
			{
				// Water is weird. It doesn't really have a displayable texture other than a
				// normal map,
				// which shouldn't really be used anyways in this case. So we'll give it an
				// obvious texture
				// So it can be easily changed
				VMT vmt = VMF2OBJ.gson.fromJson("{\"basetexture\":\"TOOLS/TOOLSDOTTED\"}", VMT.class);
				return vmt;
			}
		}

		String keyValueRegex = "(\"[a-zA-z._0-9 ]+\")(\"[^\"]*\")";
		String cleanUpRegex = ", *([}\\]])";

		// Holy moly do I wish Valve was consistant in their kv files.
		// All the following are just to format the file correctly.

		text = text.replaceAll("\\\\", "/"); // Replace backslashs with forwardslashs
		text = text.replaceAll("//(.*)", ""); // Remove all commented lines
		text = text.replaceAll("\\x1B", ""); // Remove all illegal characters
		text = text.replaceAll("!?srgb\\?", ""); // Remove all weirdos
		text = text.replaceAll("360\\?", ""); // Remove all weirdos
		text = text.replaceAll("-dx10", ""); // Remove all dx10 fallback textures
		text = text.replaceAll("[^\"](\\$[^\" \\t]+)", "\"$1\""); // fix unquoted keys
		text = text.replaceAll("(\".+\"[ \\t]+)([^\" \\t\\s].*)", "$1\"$2\""); // fix unquoted values
		text = text.replaceAll("\\$", ""); // Remove all key prefixes
		text = text.replaceAll("\"%.+", ""); // Remove all lines with keys that start with percentage signs
		// text = text.replaceAll("(\".+)[{}](.+\")", "$1$2"); // Remove brackets in quotes
		text = text.replaceAll("[\\t\\r\\n]", ""); // Remove all whitespaces and newlines not in quotes
		text = text.replaceAll("\" +\"", "\"\""); // Remove all whitespaces and newlines not in quotes
		// text = text.replaceAll("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)", ""); // Remove all whitespaces and newlines not in quotes

		Pattern bracketPattern = Pattern.compile("\\{");
		Matcher bracketMatcher = bracketPattern.matcher(text);
		if (bracketMatcher.find()) {
			int startIndex = bracketMatcher.end() - 1;
			int endIndex = findClosingBracketMatchIndex(text, startIndex);
			if (endIndex == -1) // Invalid vmt
			{
				VMT vmt = VMF2OBJ.gson.fromJson("{\"basetexture\":\"TOOLS/TOOLSDOTTED\"}", VMT.class);
				return vmt;
			}
			text = text.substring(startIndex, endIndex + 1);

			// Pattern proxiesPattern = Pattern.compile("\"proxies\""); // check if the materials has proxies
			// https://developer.valvesoftware.com/wiki/Half-Life_2_Shader_Fallbacks
			Pattern proxiesPattern = Pattern.compile("((\"([^\" \\t]+)\")|(hdr)|(proxies))\\s*\\{", Pattern.CASE_INSENSITIVE); // check if the materials has proxies or fallback shaders
			Matcher proxiesMatcher = proxiesPattern.matcher(text);
			while (proxiesMatcher.find()) {
				int proxiesStartIndex = proxiesMatcher.end() - 1;
				int proxiesEndIndex = findClosingBracketMatchIndex(text, proxiesStartIndex);
				text = text.replace(text.substring(proxiesMatcher.start(), proxiesEndIndex + 1), ""); // snip the proxy/fallback shader
				proxiesMatcher = proxiesPattern.matcher(text);
			}

			text = text.replaceAll(keyValueRegex, "$1:$2,");
			text = text.replaceAll(cleanUpRegex, "$1"); // remove commas at the end of a list
		}
		text = text.toLowerCase();

		text = text.replaceAll("([a-zA-Z_]+dx[6-9])", "\"$1\":"); // Fix fallback shaders

		// System.out.println(text);
		VMT vmt = VMF2OBJ.gson.fromJson(text, VMT.class);
		return vmt;
	}

}
