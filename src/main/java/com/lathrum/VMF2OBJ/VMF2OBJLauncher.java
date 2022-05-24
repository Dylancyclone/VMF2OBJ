package com.lathrum.VMF2OBJ;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Logger;

import com.lathrum.VMF2OBJ.cli.VMF2OBJCLI;
import com.lathrum.VMF2OBJ.gui.VMF2OBJFrame;

public class VMF2OBJLauncher {
	public static void main(String args[]) throws Exception {
		// Set up logger
		Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		Formatter formatter = new SimpleFormatter();
		// logger.getParent().setLevel(Level.FINEST); // Print all levels of logging
		for (Handler handler : logger.getParent().getHandlers()) {
			handler.setFormatter(formatter); // Use custom formatter
			// handler.setLevel(Level.FINEST); // Print all levels of logging
		}

		if (System.console() == null) {
			VMF2OBJFrame.main(args);
		} else {
			VMF2OBJCLI.main(args);
		}
	}
}