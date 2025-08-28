package com.fastsmartsystem.saf.processors;
import com.fastsmartsystem.saf.*;
import com.forcex.gtasdk.*;
import java.util.*;
import com.forcex.utils.*;

public class UVProcessor
{
	public static void flip(boolean u,ZObject obj){
		DFFGeometry geo = ((DFFSDK)Zmdl.inst().obj).findGeometry(obj.getID());
		float[] temp = new float[geo.vertexCount * 2 * geo.uvsets];
		for(int i = 0;i < geo.vertexCount * 2;i += 2){
			if(u){
				temp[i] = 1 - geo.uvs[i];
				temp[i+1] = geo.uvs[i+1];
			}else{
				temp[i] = geo.uvs[i];
				temp[i+1] = 1 - geo.uvs[i+1];
			}
		}
		geo.uvs = temp;
		obj.getMesh().setTextureCoords(temp);
		temp = null;
	}

	// unwrap surface
	public static ArrayList<Island> getIslands(short[] indices){
		ArrayList<Island> islands = new ArrayList<>();
		ArrayList<Integer> processed = new ArrayList<>();
		Island isl_cur = new Island();
		int numTriangles = indices.length / 3;
		Logger.log(numTriangles+"");
		int previusTriangle = 0;
		int numTriangleProcessed = 0;
		while(numTriangleProcessed < numTriangles) {
			Zmdl.app().getProgressScreen().setProgress(100f * ((float)numTriangleProcessed / numTriangles));
			byte connect = 0;
			if(processed.indexOf(previusTriangle) == -1) {
				processed.add(previusTriangle);
			}
			for(int j = 0;j < numTriangles; j++) {
				if(isl_cur.exist(j) || processed.indexOf(j) != -1) {
					continue;
				}
				for(byte x = 0;x < 3;x++) {
					for(byte y = 0;y < 3;y++) {
						if(indices[previusTriangle * 3 + x] == indices[j*3 + y]){
							connect++;
						}
						if(connect == 2) {
							isl_cur.add(previusTriangle);
							isl_cur.add(j);
							previusTriangle = j;
							numTriangleProcessed += 2;
							processed.add(j);
							break;
						}
					}
					if(connect >= 2){
						break;
					}
				}
				if(connect >= 2){
					break;
				}
			}
			if(connect < 2) {
				islands.add(isl_cur);
				isl_cur = new Island();
				while(true) {
					int index = (int)(Math.random() * numTriangles);
					if(processed.indexOf(index) == -1){
						previusTriangle = index;
						processed.add(index);
						break;
					}
					if(processed.size() == numTriangles) {
						break;
					}
				}
				if(processed.size() == numTriangles) {
					isl_cur.faces.add(previusTriangle);
					numTriangleProcessed++;
					break;
				}
			}
		}
		islands.add(isl_cur);
		return islands;
	}
}
