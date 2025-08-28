package com.fastsmartsystem.saf.processors;
import java.util.*;

public class Island {
	public ArrayList<Integer> faces = new ArrayList<>();

	protected boolean exist(int index){
		return faces.indexOf(index) != -1;
	}

	public void add(int index){
		if(!exist(index)){
			faces.add(index);
		}
	}

	@Override
	public String toString() {
		return faces.toString();
	}
}
