package com.fastsmartsystem.saf.loaders;
import android.util.Log;

import com.forcex.FX;
import com.forcex.gfx3d.*;
import com.forcex.math.*;
import java.util.*;
import com.forcex.io.*;
import com.forcex.utils.*;
import com.forcex.core.*;
import com.fastsmartsystem.saf.*;
import com.forcex.gui.*;
import java.io.*;

public class OBJStream {
	private static class Index {
		int v_idx = -1,n_idx = -1,t_idx = -1;
		int material = -1;
		int mat_idx = -1;
	}
	
	public static class OBJItem{
		public int lineIndex = -1;
		public String name = "";
	}
	
	public static class OBJMaterial {
		public Material mat;
		public String name = "";
	}
	
	public static ArrayList<OBJItem> getObjectList(String path) {
		String str = new String(FX.fs.open(path, FileSystem.ReaderType.MEMORY).getData());
		String[] lines = str.split("\n");
		ArrayList<OBJItem> items = new ArrayList<>();
		for(int i = 0;i < lines.length;i++){
			String line = lines[i];
			if(line.length() == 0){
				continue;
			}
			if(line.startsWith("o ")){
				OBJItem itm = new OBJItem();
				itm.name = line.split(" ")[1];
				itm.lineIndex = i;
				items.add(itm);
			}
		}
		return items;
	}
	
	public static Mesh read(String path,onLoadListener listener,int lineIndex,LanguageString lang){
		try{
			Mesh obj = new Mesh(true);
			String[] lines = new String(FX.fs.open(path, FileSystem.ReaderType.MEMORY).getData()).split("\n");
			ArrayList<OBJMaterial> included_mats = null;
			Log.i("OBJ", "Lineas -> " + lines.length);
			if(new File(path.replace(".obj",".mtl")).exists()) {
				included_mats = new ArrayList<>();
				String[] mats = new String(FX.fs.open(path.replace(".obj",".mtl"), FileSystem.ReaderType.MEMORY).getData()).split("\n");
				OBJMaterial current = null;
				for(String line : mats) {
					if(line.length() == 0){
						continue;
					}else if(line.startsWith("Kd ")){
						String[] v = line.replace(',','.').split(" ");
						current.mat.color.set((int)(Float.parseFloat(v[1])*255.0f),(int)(Float.parseFloat(v[2])*255.0f),(int)(Float.parseFloat(v[3])*255.0f));
					}else if(line.startsWith("newmtl ")){
						if(current != null){
							included_mats.add(current);
						}
						current = new OBJMaterial();
						current.mat = new Material();
						current.name = line.split(" ")[1];
					}else if(line.startsWith("map_Kd")){
						current.mat.textureName = line.split(" ")[1];
					}
				}
				included_mats.add(current);
			}
			ArrayList<Vector3f> vertices = new ArrayList<>();
			ArrayList<Vector2f> texcoords = new ArrayList<>();
			ArrayList<Vector3f> normals = new ArrayList<>();
			ArrayList<Index> indexes = new ArrayList<>();
			boolean hasNormals = false,hasTexcoords= false;
			int offset = 0;
			int materials = 0;
			int mat_indx = 0;
			for(int k = lineIndex + 1;k < lines.length;k++){
				String line = lines[k];
				if(line.length() == 0){
					continue;
				}else if(line.startsWith("v ")){
					String[] sf = line.split(" ");
					vertices.add(
						new Vector3f(
							Float.parseFloat(sf[1]),
							Float.parseFloat(sf[2]),
							Float.parseFloat(sf[3])));
				} else if(line.startsWith("o ")) {
					break;
				} else if(line.startsWith("vt ")) {
					String[] sf = line.split(" ");
					texcoords.add(
						new Vector2f(
							Float.parseFloat(sf[1]),
							Float.parseFloat(sf[2])));
					hasTexcoords = true;
				} else if(line.startsWith("vn ")) {
					String[] sf = line.split(" ");
					normals.add(
						new Vector3f(
							Float.parseFloat(sf[1]),
							Float.parseFloat(sf[2]),
							Float.parseFloat(sf[3])));
					hasNormals = true;
				} else if(line.startsWith("usemtl")) {
					materials++;
					if(included_mats != null){
						mat_indx = indxof(included_mats,line.split(" ")[1]);
					}
				} else if(line.startsWith("f ")) {
					if(materials == 0){
						materials++;
					}
					String[] tks = line.split(" ");
					if(tks.length > 5) {
						listener.error(Zmdl.gt("objn_compatible"));
						return null;
					}
					if(tks.length == 4){
						for(byte i = 0;i < 3;i++){
							Index idx = new Index();
							String[] sf = tks[i + 1].split("/");
							idx.v_idx = Integer.parseInt(sf[0]) - 1;
							if(hasTexcoords){
								idx.t_idx = Integer.parseInt(sf[1]) - 1;
							}
							if(hasNormals){
								idx.n_idx = Integer.parseInt(sf[2]) - 1;
							}
							idx.material = materials;
							idx.mat_idx = mat_indx;
							indexes.add(idx);
						}
					}else{
						Index[] idxs = new Index[4];
						for(byte i = 0;i < 4;i++){
							Index idx = new Index();
							String[] sf = tks[i + 1].split("/");
							idx.v_idx = Integer.parseInt(sf[0]) - 1;
							if(hasTexcoords){
								idx.t_idx = Integer.parseInt(sf[1]) - 1;
							}
							if(hasNormals){
								idx.n_idx = Integer.parseInt(sf[2]) - 1;
							}
							idx.material = materials;
							idx.mat_idx = mat_indx;
							idxs[i] = idx;
						}
						indexes.add(idxs[0]); indexes.add(idxs[1]); indexes.add(idxs[3]);
						indexes.add(idxs[1]); indexes.add(idxs[2]); indexes.add(idxs[3]);
						idxs = null;
					}
				}
				listener.progress(40.0f * (float)offset / lines.length);
				offset++;
			}
			lines = null;
			Toast.info(Zmdl.gt("optimizing"),4f);
			ArrayList<Index> filtered = filter(listener,indexes);
			float[] vertex = new float[filtered.size() * 3];
			for(int i = 0;i < vertex.length;i += 3){
				Vector3f u = vertices.get(filtered.get(i/3).v_idx);
				vertex[i] = u.x;
				vertex[i+1] = u.y;
				vertex[i+2] = u.z;
			}
			obj.setVertices(vertex);
			vertex = null;
			vertices.clear();
			if(hasTexcoords){
				float[] uv = new float[filtered.size()*2];
				for(int i = 0;i < uv.length;i += 2){
					Vector2f u = texcoords.get(filtered.get(i / 2).t_idx);
					uv[i] = u.x;
					uv[i+1] = u.y;
				}
				obj.setTextureCoords(uv);
				uv = null;
				texcoords.clear();
			}
			if(hasNormals){
				float[] normal = new float[filtered.size()*3];
				for(int i = 0;i < normal.length;i += 3){
					Vector3f u = normals.get(filtered.get(i/3).n_idx);
					normal[i] = u.x;
					normal[i+1] = u.y;
					normal[i+2] = u.z;
				}
				obj.setNormals(normal);
				normals.clear();
				normal = null;
			}
			int index_offset = 0;
			for(int i = 1;i <= materials;i++) {
				int size = getnum(indexes,i);
				short[] indx = new short[size];
				offset = 0;
				for(int j = index_offset;j < (index_offset + size);j ++) {
					indx[offset] = (short)get(filtered,indexes.get(j));
					offset++;
					listener.progress(80.0f + 20.0f * ((float)(index_offset + offset) / indexes.size()));
				}
				index_offset += size;
				MeshPart part = new MeshPart(indx);
				if(included_mats != null) {
					part.material = included_mats.get(getmat(indexes,i)).mat;
				}
				obj.addPart(part);
			}
			return obj;
		}catch(Exception e){
			Logger.log(e);
			listener.error(e.toString());
		}
		return null;
	}
	
	private static int indxof(ArrayList<OBJMaterial> mats,String name){
		for(int i = 0;i < mats.size();i++){
			if(mats.get(i).name.equals(name)){
				return i;
			}
		}
		return -1;
	}
	
	private static int getmat(ArrayList<Index> indexes,int mat){
		for(Index i : indexes){
			if(i.material == mat){
				return i.mat_idx;
			}
		}
		return -1;
	}
	
	private static int getnum(ArrayList<Index> indexes,int mat){
		int size = 0;
		for(Index idx :indexes){
			if(idx.material == mat){
				size++;
			}
		}
		return size;
	}
	
	private static ArrayList<Index> filter(onLoadListener listen, ArrayList<Index> indexes){
		ArrayList<Index> out = new ArrayList<>();
		int offset = 0;
		for(Index i : indexes){
			if(get(out,i) == -1){
				out.add(i);
			}
			listen.progress(40.0f + 40.0f * (float)offset / indexes.size());
			offset++;
		}
		return out;
	}
	
	private static int get(ArrayList<Index> indexes,Index idx) {
		int offset = 0;
		for(Index i : indexes){
			if(i.v_idx == idx.v_idx && i.t_idx == idx.t_idx && i.n_idx == idx.n_idx){
				return offset;
			}
			offset++;
		}
		return -1;
	}
	
	public static class OBJParams{
		public boolean export_materials;
		public boolean export_creator;
		public String creator;
	}

	public static boolean write(
		String path,
		onLoadListener listen,
		LanguageString lang,
		Mesh obj,
		String name,OBJParams params){
		if(obj.getPrimitiveType() == GL.GL_TRIANGLE_STRIP){
			listen.error(lang.get("obj_tri_strip_error"));
			return false;
		}
		try{
			float[] vertices = optimize(obj.getVertexData().vertices,"%.4f");
			if((vertices.length / 3) > 65535){
				vertices = null;
				listen.error(lang.get("obj_many_vertices"));
				return false;
			}
			long start = System.currentTimeMillis();
			float[] texcoord = null, normals = null;
			listen.progress(2f);
			int totalTriangles = 0;
			for(MeshPart p : obj.getParts().list){
				totalTriangles += (p.index.length / 3);
			}
			if(obj.getVertexInfo().hasTextureCoords()){
				texcoord = optimize(obj.getVertexData().uvs,"%.3f");
				listen.progress(3f);
			}
			if(obj.getVertexInfo().hasNormals()){
				normals = optimize(obj.getVertexData().normals,"%.3f");
				listen.progress(4f);
			}
			ArrayList<Vector3f> v_f = new ArrayList<>();
			listen.progress(5);
			for(int i = 0;i < vertices.length;i += 3){
				Vector3f temp =  new Vector3f(vertices,i);
				boolean add = true;
				if(i == 0){
					v_f.add(temp);
				}
				for(int j = 0;j < v_f.size();j++){
					Vector3f v = v_f.get(j);
					if(v.x == temp.x && v.y == temp.y && v.z == temp.z){
						add = false;
						break;
					}
				}
				if(add){
					v_f.add(temp);
				}
			}
			listen.progress(8);
			ArrayList<Vector2f> t_f = new ArrayList<>();
			if(texcoord != null){
				for(int i = 0;i < texcoord.length;i += 2){
					Vector2f tmp = new Vector2f(texcoord[i],texcoord[i + 1]);
					boolean add = true;
					for(Vector2f v : t_f){
						if(v.x == tmp.x && v.y == tmp.y){
							add = false;
							break;
						}
					}
					if(add){
						t_f.add(tmp);
					}
				}
			}
			listen.progress(10);
			ArrayList<Vector3f> n_f = new ArrayList<>();
			if(normals != null){
				for(int i = 0;i < normals.length;i += 3){
					Vector3f temp =  new Vector3f(normals,i);
					boolean add = true;
					for(Vector3f v : n_f){
						if(v.x == temp.x && v.y == temp.y && v.z == temp.z){
							add = false;
							break;
						}
					}
					if(add){
						n_f.add(temp);
					}
				}
			}
			listen.progress(15);
			BinaryStreamWriter os = new BinaryStreamWriter(new FileOutputStream(path + name + ".obj"));
			BinaryStreamWriter os_mat = null;
			os.writeString("# exported in Zmodeler Android (FSS)\n");
			if(params.export_materials){
				os_mat = new BinaryStreamWriter(new FileOutputStream(path + name + ".mtl"));
				os_mat.writeString("# exported in Zmodeler Android (FSS)\n");
				if(params.export_creator){
					os_mat.writeString("# created by: '"+params.creator+"'\n");
				}
				os_mat.writeString("# "+obj.getParts().list.size()+" materials\n");
			}
			if(params.export_creator){
				os.writeString("# created by: '"+params.creator+"'\n");
			}
			os.writeString("# "+v_f.size()+" vertices - "+totalTriangles+" faces\n");
			if(params.export_materials){
				os.writeString("mtllib "+name+".mtl\n");
			}
			os.writeString("o "+name+"\n");
			String form4 = "v %.4f %.4f %.4f\n";
			for(int i = 0;i < v_f.size();i ++){
				Vector3f v = v_f.get(i);
				os.writeString(String.format(form4,v.x,v.y,v.z).replace(',','.'));
				listen.progress(20 + 10 * ((float)i/v_f.size()));
			}
			if(texcoord != null){
				String form = "vt %.3f %.3f\n";
				for(int i = 0;i < t_f.size();i ++){
					Vector2f t = t_f.get(i);
					os.writeString(String.format(form,t.x,t.y).replace(',','.'));
					listen.progress(30 + 5 * ((float)i/t_f.size()));
				}
			}
			if(normals != null){
				String form = "vn %.3f %.3f %.3f\n";
				for(int i = 0;i < n_f.size();i ++){
					Vector3f n = n_f.get(i);
					os.writeString(String.format(form,n.x,n.y,n.z).replace(',','.'));
					listen.progress(35 + 10 * ((float)i/n_f.size()));
				}
			}
			float constant = 55.0f / obj.getParts().list.size();
			float progess = 45f;
			for(int j = 0;j < obj.getParts().list.size();j++){
				MeshPart p = obj.getPart(j);
				if(params.export_materials){
					Material mat = p.material;
					os.writeString("usemtl Material"+j+"\n");
					os_mat.writeString("\nnewmtl Material"+j+"\n");
					os_mat.writeString("Ns 320.000\n");
					os_mat.writeString("Ka 1.0000 1.0000 1.0000\n");
					os_mat.writeString(String.format("Kd %.4f %.4f %.4f\n",(mat.color.r / 255.0f),(mat.color.g / 255.0f),(mat.color.b / 255.0f)).replace(',','.'));
					os_mat.writeString("Ks 1.0000 1.0000 1.0000\n");
					os_mat.writeString("Ke 0.0000 0.0000 0.0000\n");
					os_mat.writeString("illum 2\n");
					if(mat.textureName.length() > 0){
						os_mat.writeString("map_Kd "+mat.textureName+"\n");
					}
				}
				Vector3f temp = new Vector3f();
				Vector2f tmp = new Vector2f();
				int m = 0;
				for(int i = 0;i < p.index.length;i += 3){
					os.writeString("f ");
					for(byte k = 0;k < 3;k++){
						String indice = "";
						int idx = (p.index[i + k] & 0xffff);
						//find vertice
						temp.set(vertices,idx * 3);
						for(m = 0;m < v_f.size(); m++){
							Vector3f v = v_f.get(m);
							if(v.x == temp.x && v.y == temp.y && v.z == temp.z){
								indice += ""+(m + 1);
								break;
							}
						}
						if(texcoord != null){
							tmp.set(texcoord,idx * 2);
							for(m = 0;m < t_f.size(); m++){
								Vector2f v = t_f.get(m);
								if(v.x == tmp.x && v.y == tmp.y){
									indice += "/" +(m + 1);
									break;
								}
							}
						}else if(texcoord == null && normals != null){
							indice += "/";
						}
						if(normals != null){
							temp.set(normals,idx * 3);
							for(m = 0;m < n_f.size(); m++){
								Vector3f v = n_f.get(m);
								if(v.x == temp.x && v.y == temp.y && v.z == temp.z){
									indice += "/" + (m + 1);
									break;
								}
							}
						}
						os.writeString(indice);
						indice = null;
						if(k != 2){
							os.writeString(" ");
						}
					}
					if((j + 1) == obj.getParts().list.size() && (i + 3) == p.index.length){
						break;
					}else{
						os.writeString("\n");
					}
					listen.progress(progess + constant * ((float)i / p.index.length));
				}
				progess += constant;
			}
			Toast.info(String.format("%.2f",(System.currentTimeMillis()-start)/1000.0f)+" segs",5);
			os.finish();
			v_f.clear();
			v_f = null;
			if(texcoord != null){
				t_f.clear();
				t_f = null;
			}
			if(normals != null){
				n_f.clear();
				n_f = null;
			}
			return true;
		}catch(Exception e){
			Logger.log(e);
			listen.error(e.toString());
			return false;
		}
	}
	
	public static float[] optimize(float[] input,String form) {
		float[] output = new float[input.length];
		for(int i = 0;i < input.length;i++) {
			output[i] = Float.parseFloat(String.format(form,input[i]).replace(',','.'));
		}
		return output;
	}
}
