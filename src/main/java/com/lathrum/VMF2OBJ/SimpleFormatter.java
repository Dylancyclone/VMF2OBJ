package com.lathrum.VMF2OBJ;

import java.util.logging.*;

public class SimpleFormatter extends Formatter {
	@Override
	public String format(LogRecord record) {
		String output = "";
		String level = record.getLevel().toString();
		String message = record.getMessage();

		if (record.getLevel() == Level.INFO) {
			if (message.endsWith("/s")) {
				// If progress bar, put on same line
				output = "\r" + message;
			} else {
				// If INFO, don't put logging level
				output = message + "\n";
			}
		} else {
			if (!(record.getLevel() == Level.WARNING && VMF2OBJ.quietMode)) {
				// Dont print warning messages in quiet mode
				// [LEVEL]: [MESSAGE]
				output = level + ": " + message + "\n";
			}
		}

		return output;
	}
}