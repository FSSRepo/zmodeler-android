package com.fastsmartsystem.saf.loaders;
import com.forcex.FX;
import com.forcex.io.*;
import com.fastsmartsystem.saf.*;
import com.forcex.gfx3d.*;
import com.forcex.utils.*;

public class Load3DS {
	boolean endReached;
	int chunkEndOffset;

	public ZObject read(String path,onLoadListener listener,LanguageString lang) {
		try{
			BinaryStreamReader is = FX.fs.open(path, FileSystem.ReaderType.MEMORY);
			if(readChunk(is) == 0x4D4D) {
				ZObject obj = new ZObject(new Mesh(true));
				while(!endReached) {
					readSections(is,obj);
				}
				return obj;
			}else{
				listener.error(lang.get("no_3ds_file"));
				return null;
			}
		}catch(Exception e){
			e.printStackTrace();
			listener.error(lang.get("3ds_file_error"));
			return null;
		}
	}

	private void readSections(BinaryStreamReader stream,ZObject obj) {
		switch (readChunk(stream)) {
			case 0x3D3D:
				break;
			case 0x4000:
				obj.setName(stream.readString());
				Logger.log(obj.getName());
				break;
			case 0x4100:
				break;
			case 0x4110:
				{
					short numVertices = stream.readShort();
					float[] vertices = new float[numVertices*3];
					for(short i = 0;i < numVertices;i++){
						vertices[i*3] = stream.readFloat();
						vertices[i*3+1] = stream.readFloat();
						vertices[i*3+2] = stream.readFloat();
					}
					obj.getMesh().setVertices(vertices);
				}
				break;
			case 0x4120:
				{
					short triangles = stream.readShort();
					short[] indices = new short[triangles * 3];
					for (short i = 0; i < triangles; i++) {
						indices[i*3] = stream.readShort();
						indices[i*3+1] = stream.readShort();
						indices[i*3+2] = stream.readShort();
						stream.skip(2);
					}
					obj.getMesh().addPart(new MeshPart(indices));
				}
				break;
			case 0x4140:
				{
					short numVertices = stream.readShort();
					float[] texcoords = new float[numVertices*2];
					for(short i = 0;i < numVertices;i++){
						texcoords[i*2] = stream.readFloat();
						texcoords[i*2+1] = stream.readFloat() * -1f;
					}
				}
				break;
			case 0xA000:
				stream.readString();
				break;
			case 0xA300:
				stream.readString();
				break;
			case 0x4130:
				stream.readString();
				int numFaces = stream.readShort();
				stream.skip(numFaces*2);
				break;
			case 0xAFFF:
				break;
			case 0xA200:
				break;
			default:
				skipRead(stream);
		}
	}

	private void skipRead(BinaryStreamReader stream){
		for(int i = 0; (i < chunkEndOffset - 6) && !endReached; i++){
			stream.readByte();
			endReached = stream.isEndOfFile();
		}
	}

	public int readChunk(BinaryStreamReader is){
		int chunkID = is.readShort();
		chunkEndOffset = is.readInt();
		endReached = is.isEndOfFile();
		return chunkID;
	}
}
