package com.fastsmartsystem.saf.loaders;
import com.forcex.io.*;
import java.util.*;
import com.fastsmartsystem.saf.*;
import com.forcex.gfx3d.*;
import com.forcex.core.*;
import com.forcex.gui.widgets.*;
import com.forcex.gui.*;
import com.forcex.utils.*;
import java.io.*;
import com.forcex.*;
import com.forcex.app.threading.*;

public class ZFileStream {
	private static int INCLUDE_UV = 0x2;
	private static int INCLUDE_NORMALS = 0x4;
	private static int INCLUDE_MULTIUV = 0x8;
	
	public static ZContainer read(String path) {
		try{
			ZContainer zfile = new ZContainer();
			BinaryStreamReader is = FX.fs.open(path, FileSystem.ReaderType.MEMORY);
			byte strlen = 0;
			zfile.from_store = is.readBoolean();
			strlen = is.readByte();
			zfile.name = is.readString(strlen);
			int nodes = is.readShort();
			int objects = is.readShort();
			ArrayList<WrapperNode> nodelist = new ArrayList<>();
			int i = 0;
			for(i = 0;i < nodes;i++){
				WrapperNode n = new WrapperNode();
				Znode node = new Znode();
				strlen = is.readByte();
				node.name = is.readString(strlen);
				node.geo_idx = is.readShort();
				node.isGeometry = node.geo_idx != -1;
				n.parentIdx = is.readShort();
				n.node = node;
				nodelist.add(n);
			}
			for(i = 0;i < objects;i++){
				Mesh mesh = new Mesh(true);
				int flags = is.readShort();
				byte splits = is.readByte();
				int vcount = is.readInt();
				mesh.setVertices(is.readFloatArray(vcount*3));
				if((flags & INCLUDE_UV) != 0){
					if((flags & INCLUDE_MULTIUV) != 0){
						mesh.setTextureCoords(is.readFloatArray(vcount*2*2));
					}else{
						mesh.setTextureCoords(is.readFloatArray(vcount*2));
					}
				}
				if((flags & INCLUDE_NORMALS) != 0){
					mesh.setNormals(is.readFloatArray(vcount*3));
				}
				for(byte j = 0;j < splits;j++){
					int tcount = is.readInt();
					MeshPart part = new MeshPart(is.readShortArray(tcount*3));
					part.material.color.set(is.readByte() & 0xff,is.readByte() & 0xff,is.readByte() & 0xff, is.readByte() & 0xff);
					strlen = is.readByte();
					part.material.textureName = is.readString(strlen);
					part.type = GL.GL_TRIANGLES;
					mesh.addPart(part);
				}
				zfile.objects.add(new ZObject(mesh));
			}
			zfile.dff_offset = is.readInt();
			if(zfile.dff_offset != -1){
				strlen = is.readByte();
				zfile.dff_path = is.readString(strlen);
				zfile.dff_offset += 1 + strlen;
			}
			is.clear();
			WrapperNode root = null;
			for(i = 0;i < nodes;i++){
				WrapperNode t = nodelist.get(i);
				if(t.parentIdx == -1){
					root = t;
				}else{
					nodelist.get(t.parentIdx)
						.node.addChild(t.node);
				}
			}
			zfile.root = root.node;
			return zfile;
		}catch(Exception e){
			Logger.log(e);
			FX.gpu.queueTask(new Task(){
					@Override
					public boolean execute() {
						Zmdl.app().showBugReport();
						return true;
					}
				});
			return null;
		}
	}
	
	public static boolean write(String path, String name, Znode root, String dff_path, boolean from_store) {
		try{
			BinaryStreamWriter os = new BinaryStreamWriter(new FileOutputStream(path));
			ArrayList<ZObject> objects = new ArrayList<>();
			ArrayList<WrapperNode> nlist = new ArrayList<WrapperNode>();
			NodeIndexer(nlist, objects,root,-1);
			int offset = 0;
			os.writeByte(from_store ? 1 : 0);
			os.writeByte(name.length());
			os.writeString(name);
			os.writeShort(nlist.size());
			os.writeShort(objects.size());
			offset += name.length() + 6;
			int i = 0;
			for(i = 0;i < nlist.size();i++){
				WrapperNode t = nlist.get(i);
				os.writeByte(t.node.name.length());
				os.writeString(t.node.name);
				os.writeShort(indexObject(objects,t.node.model_kh));
				os.writeShort(t.parentIdx);
				offset += 5 + t.node.name.length();
			}
			for(i = 0;i < objects.size();i++){
				ZObject o = objects.get(i);
				Mesh mesh = o.getMesh();
				int flags = 0;
				int vertex_count = o.getMesh().getVertexInfo().vertexCount;
				if(mesh.getVertexInfo().hasTextureCoords()){
					if((mesh.getVertexData().uvs.length / 4) == vertex_count){
						flags |= INCLUDE_MULTIUV;
					}
					flags |= INCLUDE_UV;
				}
				if(mesh.getVertexInfo().hasNormals()){
					flags |= INCLUDE_NORMALS;
				}
				os.writeShort(flags);
				os.writeByte(mesh.getParts().list.size());
				os.writeInt(vertex_count);
				os.writeFloatArray(mesh.getVertexData().vertices);
				offset += 7 + (vertex_count * 3 * 4);
				if((flags & INCLUDE_UV) != 0){
					os.writeFloatArray(mesh.getVertexData().uvs);
					offset += mesh.getVertexData().uvs.length * 4;
				}
				if((flags & INCLUDE_NORMALS) != 0){
					os.writeFloatArray(mesh.getVertexData().normals);
					offset += vertex_count * 3 * 4;
				}
				for(MeshPart p : mesh.getParts().list){
					if(p.type != GL.GL_TRIANGLES){
						Toast.info(Zmdl.gt("zfile_t_err"),5);
						return false;
					}
					os.writeInt(p.indxSize / 3);
					os.writeShortArray(p.index);
					os.writeByteArray(p.material.color.getData());
					os.writeByte(p.material.textureName.length());
					os.writeString(p.material.textureName);
					offset += 9 + p.material.textureName.length() + p.index.length*2;
				}
			}
			os.writeInt(dff_path.length() == 0 ? -1 : offset);
			if(dff_path.length() > 0){
				os.writeByte(dff_path.length());
				os.writeString(dff_path);
				try{
					if(!new File(dff_path).exists()){
						Toast.error(Zmdl.gt("zmdl_dff_ne"),4);
						return false;
					}
					FileInputStream is = new FileInputStream(dff_path);
					byte[] data = new byte[is.available()];
					is.read(data);
					is.close();
				}catch(Exception e){
					Logger.log(e);
					return false;
				}
			}
			os.finish();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return true;
	}
	
	private static void NodeIndexer(ArrayList<WrapperNode> nlist,ArrayList<ZObject> objects,Znode root,int parentIdx) {
		WrapperNode n = new WrapperNode();
		n.node = root;
		n.parentIdx = parentIdx;
		if(root.geo_idx != -1){
			objects.add(Zmdl.go(root.model_kh));
		}
		nlist.add(n);
		int p = nlist.size() - 1;
		for(TreeNode tn : root.getChildren()){
			NodeIndexer(nlist,objects,(Znode)tn,p);
		}
	}
	
	private static int indexObject(ArrayList<ZObject> objects,short kh) {
		for(int i = 0;i < objects.size();i++){
			if(objects.get(i).getID() == kh){
				return i;
			}
		}
		return -1;
	}
	
	private static class WrapperNode{
		int parentIdx;
		Znode node;
	}
}
