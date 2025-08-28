package com.fastsmartsystem.saf.col;
import com.forcex.io.*;

public class COLMaterial{
	public short material;
	public short flags;
	public short brightness;
	public short light;
	
	public COLMaterial read(BinaryStreamReader is){
		material = is.readByte();
		flags = is.readByte();
		brightness = is.readByte();
		light = is.readByte();
		return this;
	}
}
