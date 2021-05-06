package com.lathrum.VMF2OBJ;

import java.util.logging.*;

public class SimpleFormatter extends Formatter {
	@Override
	public String format(LogRecord record) {
		StringBuilder sb = new StringBuilder();
		sb.append(record.getLevel()).append(": ");
		sb.append(record.getMessage()).append("\n");
		return sb.toString();
	}
}