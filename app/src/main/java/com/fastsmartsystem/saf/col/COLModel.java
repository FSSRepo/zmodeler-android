package com.fastsmartsystem.saf.col;

import java.util.*;
import com.forcex.math.*;
import com.forcex.io.*;

public class COLModel
{
	/*
	Object : value
	
	Sphere:
	0: center
	1: radius
	2: surface
	
	Box:
	0: min
	1: max
	2: surface
	
	Bound:
	0: min
	1: max
	2: center
	3: radius

	*/
	
	public ModelType type;
	public ArrayList<Object> values = new ArrayList<>();
	
	public Vector3f getVector(int idx){
		return (Vector3f)values.get(idx);
	}
	
	public COLMaterial getMaterial(int idx){
		return (COLMaterial)values.get(idx);
	}
	
	public float getFloat(int idx){
		return (float)values.get(idx);
	}
	
	public void writeSphere(BinaryStreamWriter os){
		os.writeFloat(getFloat(1));
		os.writeVector3(getVector(0));
		os.writeByte(getMaterial(2).material);
		os.writeByte(getMaterial(2).flags);
		os.writeByte(getMaterial(2).brightness);
		os.writeByte(getMaterial(2).light);
	}
	
	public void writeBox(BinaryStreamWriter os){
		os.writeVector3(getVector(0));
		os.writeVector3(getVector(1));
		os.writeByte(getMaterial(2).material);
		os.writeByte(getMaterial(2).flags);
		os.writeByte(getMaterial(2).brightness);
		os.writeByte(getMaterial(2).light);
	}
	
	public COLModel readBound(BinaryStreamReader is,boolean col1){
		if(col1){
			float radius = is.readFloat();
			Vector3f cnt = new Vector3f(is.readFloat(),is.readFloat(),is.readFloat());
			values.add(new Vector3f(is.readFloat(),is.readFloat(),is.readFloat()));
			values.add(new Vector3f(is.readFloat(),is.readFloat(),is.readFloat()));
			values.add(cnt);
			values.add(radius);
		}else{
			values.add(new Vector3f(is.readFloat(),is.readFloat(),is.readFloat()));
			values.add(new Vector3f(is.readFloat(),is.readFloat(),is.readFloat()));
			values.add(new Vector3f(is.readFloat(),is.readFloat(),is.readFloat()));
			values.add(is.readFloat());
		}
		type = ModelType.BOUND;
		return this;
	}
	
	public COLModel readBox(BinaryStreamReader is){
		values.add(new Vector3f(is.readFloat(),is.readFloat(),is.readFloat()));
		values.add(new Vector3f(is.readFloat(),is.readFloat(),is.readFloat()));
		values.add(new COLMaterial().read(is));
		type = ModelType.BOX;
		return this;
	}
	
	public COLModel readSphere(BinaryStreamReader is,boolean col1){
		if(col1){
			float radius = is.readFloat();
			Vector3f cnt = new Vector3f(is.readFloat(),is.readFloat(),is.readFloat());
			values.add(cnt);
			values.add(radius);
		}else{
			values.add(new Vector3f(is.readFloat(),is.readFloat(),is.readFloat()));
			values.add(is.readFloat());
		}
		values.add(new COLMaterial().read(is));
		type = ModelType.SPHERE;
		return this;
	}
}
