package com.fastsmartsystem.saf.col;
import com.forcex.FX;
import com.forcex.io.*;
import com.forcex.math.*;

import java.io.FileOutputStream;

public class COLStream
{
	public static COLFile read(String path, boolean id) {
		COLFile file = new COLFile();
		BinaryStreamReader is = FX.fs.open(path, FileSystem.ReaderType.MEMORY);
		int numColls = 0;
		for(;true;){
			if(((char)is.readByte()) == 'C' && ((char)is.readByte()) == 'O' && ((char)is.readByte()) == 'L'){
				is.skip(1);
				int size = is.readInt();
				is.skip(size);
				if(is.getOffset() == is.length()){
					break;
				}
				numColls++;
			}else{
				break;
			}
		}
		is.seek(0);
		for(int i = 0;i < numColls;i++){
			Collision col = new Collision();
			col.SizeOffset = is.getOffset();
			is.skip(3);
			switch((char)is.readByte()){
				case '1':
					col.type = 1;
					break;
				case '2':
					col.type = 2;
					break;
				case '3':
					col.type = 3;
					break;
			}
			final int size = is.readInt();
			col.nameOffset = is.getOffset();
			col.name = cortarnombre(is.readString(20));
			is.skip(4);
			if(col.type == 1){
				col.boundOffset = is.getOffset();
				col.bound = new COLModel().readBound(is,true);
				col.sphereOffset = is.getOffset();
				int numSpheres = is.readInt();
				if(numSpheres == 0){
					col.sphereOffset = -1;
				}
				for(int s = 0;s < numSpheres;s++){
					col.spheres.add(new COLModel().readSphere(is,true));
				}
				is.skip(4);
				col.boxesOffset = is.getOffset();
				int numBoxes = is.readInt();
				if(numBoxes == 0){
					col.boundOffset = -1;
				}
				for(int s = 0;s < numBoxes;s++){
					col.boxes.add(new COLModel().readBox(is));
				}
			}else{
				col.boundOffset = is.getOffset();
				col.bound = new COLModel().readBound(is,false);
				col.numsOffset = is.getOffset();
				int numSpheres = is.readShort();
				int numBoxes = is.readShort();
				is.skip(8);
				int spofs = is.readInt();
				int bofs = is.readInt();
				is.skip(16);
				if(col.type == 3){
					is.skip(12);
				}
				if(numSpheres != 0){
					is.seek(col.SizeOffset + spofs + 4);
					col.sphereOffset = is.getOffset();
					for(int s = 0;s < numSpheres;s++){
						col.spheres.add(new COLModel().readSphere(is,false));
					}
				}
				if(numBoxes != 0){
					is.seek(col.SizeOffset + bofs + 4);
					col.boxesOffset = is.getOffset();
					for(int s = 0;s < numBoxes;s++){
						col.boxes.add(new COLModel().readBox(is));
					}
				}
			}
			is.seek(col.SizeOffset + size + 8);
			file.cols.add(col);
		}
		return file;
	}
	
	public static void save(String path,COLFile file){
		// He must reread the COL file due to the lack of documentation.
//		try {
//			BinaryStreamWriter os = new BinaryStreamWriter(new FileOutputStream(path));
//			for(int i = 0;i < file.cols.size();i++) {
//				Collision col = file.cols.get(i);
//				os.seek(col.nameOffset);
//				os.writeStringFromSize(20,col.name);
//				os.seek(col.boundOffset);
//				if(col.type == 1){
//					os.writeFloat(col.bound.getFloat(3));
//					os.writeVector3(col.bound.getVector(2));
//					os.writeVector3(col.bound.getVector(0));
//					os.writeVector3(col.bound.getVector(1));
//					if(col.sphereOffset == -1){
//						os.seek(col.sphereOffset);
//						os.writeInt(col.spheres.size());
//						for(COLModel sph : col.spheres){
//							sph.writeSphere(os);
//						}
//					}
//					if(col.boxesOffset == -1){
//						os.seek(col.boxesOffset);
//						os.writeInt(col.boxes.size());
//						for(COLModel box : col.boxes){
//							box.writeBox(os);
//						}
//					}
//				}else{
//					os.writeVector3(col.bound.getVector(0));
//					os.writeVector3(col.bound.getVector(1));
//					os.writeVector3(col.bound.getVector(2));
//					os.writeFloat(col.bound.getFloat(3));
//				}
//			}
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
	}
	
	private static String cortarnombre(String str) {
        int indexOf = str.indexOf(0);
        return indexOf > 0 ? str.substring(0, indexOf) : str;
    }
}
