package com.lathrum.VMF2OBJ.dataStructure.map;

public class Solid {
	public String id;
	public Side[] sides;

	public static boolean isDisplacementSolid(Solid solid) {
		for (Side side : solid.sides) {
			if (side.dispinfo != null) {
				return true;
			}
		}
		return false;
	}
}
