package com.lathrum.VMF2OBJ;

import com.lathrum.VMF2OBJ.cli.VMF2OBJCLI;
import com.lathrum.VMF2OBJ.gui.VMF2OBJFrame;

public class VMF2OBJLauncher {
	public static void main(String args[]) throws Exception {
		if (System.console() == null) {
			VMF2OBJFrame.main(args);
		} else {
			VMF2OBJCLI.main(args);
		}
	}
}