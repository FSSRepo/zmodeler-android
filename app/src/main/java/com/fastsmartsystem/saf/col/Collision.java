package com.fastsmartsystem.saf.col;
import java.util.*;

public class Collision {
	protected int boxesOffset = -1;
	protected int sphereOffset = -1;
	protected int numsOffset = -1;
	protected int boundOffset = 0;
	protected int nameOffset = 0;
	protected int SizeOffset = 0;
	protected int originalNumSpheres;
	protected int originalNumBoxes;
	
	public ArrayList<COLModel> boxes = new ArrayList<>();
	public ArrayList<COLModel> spheres = new ArrayList<>();
	
	public COLModel bound;
	public byte type = 1;
	public String name = "";
}
