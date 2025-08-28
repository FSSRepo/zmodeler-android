package com.fastsmartsystem.saf.ifp;
import com.forcex.math.*;

public class IFPKeyframe
{
	public Quaternion rotation = new Quaternion();
	public Vector3f position = new Vector3f();
	public float time;
	public byte[] unknown_data;
	
	public static final int Unknown2Frame = 2;
	public static final int RotFrame = 3;
	public static final int RotTransFrame = 4;
}
