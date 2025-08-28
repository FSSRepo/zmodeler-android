package com.fastsmartsystem.saf.editors;
import com.fastsmartsystem.saf.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.fastsmartsystem.saf.instance.*;
import com.fastsmartsystem.saf.adapters.*;
import com.fastsmartsystem.saf.processors.*;
import com.forcex.gtasdk.*;
import java.util.*;
import com.forcex.math.*;
import com.forcex.gfx3d.*;
import com.forcex.core.gpu.*;
import com.forcex.anim.*;
import com.forcex.*;

public class AttachTool extends PanelFragment
{
	Layout main;
	TextView tv_instance;
	ZInstance dst_inst; // Current instance
	ZObject source,dest;
	ObjectAdapter obj_adapter;
	ListView lvObjects;
	TextAdapter splits;
	ListView lvSplit;
	int split_selected = -1;
	int object_select = -1;
	boolean editing = false;
	
	public AttachTool(){
		main = Zmdl.lay(0.25f,false);
		tv_instance = new TextView(Zmdl.gdf());
		tv_instance.setTextSize(0.05f);
		tv_instance.setAlignment(Layout.CENTER);
		tv_instance.setMarginBottom(0.01f);
		main.add(tv_instance);
		obj_adapter = new ObjectAdapter();
		lvObjects = new ListView(0.25f,0.3f,obj_adapter); 
		lvObjects.setId(0x4500);
		main.add(lvObjects);
		splits = new TextAdapter();
		lvSplit = new ListView(0.25f,0.4f,splits);
		lvSplit.setVisibility(View.GONE);
		main.add(lvSplit);
		Layout main4 = Zmdl.lay(true);
		main4.setAlignment(Layout.CENTER);
		main4.setMarginTop(0.02f);
		final Button btnAccept = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.12f,0.04f);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(lvObjects.isVisible() && object_select != -1){
						Zmdl.svo(null,true);
						Zmdl.rp().testVisibilityFacts();
						source = Zmdl.go(object_select);
						source.setVisible(true);
						lvObjects.setVisibility(View.GONE);
						lvSplit.setVisibility(View.VISIBLE);
						splits.removeAll();
						splits.add(Zmdl.gt("all"));
						for(int i = 0;i < source.getMesh().getParts().list.size();i++){
							splits.add(Zmdl.gt("split")+" "+i);
						}
						if(splits.getNumItem() > 0){
							source.selected = false;
							source.setSplitShow(-1);
							split_selected = 0;
						}else{
							split_selected = -1;
						}
					}
					if(split_selected == -1){
						return;
					}
					if(!lvObjects.isVisible() && lvSplit.isVisible()){
						source.setSplitShow(-1);
						attach();
						dispose();
						Zmdl.ep().requestShow();
					}
				}
			});
		main4.add(btnAccept);
		Button btnCancel = new Button(Zmdl.gt("cancel"),Zmdl.gdf(),0.12f,0.04f);
		btnCancel.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					Zmdl.svo(null,true);
					Zmdl.rp().testVisibilityFacts();
					dispose();
					Zmdl.ep().requestShow();
				}
			});
		btnCancel.setMarginLeft(0.01f);
		btnCancel.setId(0x454);
		main4.add(btnCancel);
		main.add(main4);
		lvObjects.setOnItemClickListener(new ListView.OnItemClickListener(){
				@Override
				public void onItemClick(ListView view, Object item, short position, boolean longclick) {
					object_select = ((ObjectItem)item).model_hash;
					ZObject o = Zmdl.go(object_select);
					if(o != null){
						btnAccept.setVisibility(View.VISIBLE);
						Zmdl.svo(o,false);
						o = null;
					} else {
						object_select = -1;
						Toast.error("Error object not founded.",4f);
					}
				}
			});
		lvSplit.setOnItemClickListener(new ListView.OnItemClickListener(){
				@Override
				public void onItemClick(ListView view, Object item, short position, boolean longclick) {
					source.setSplitShow(position - 1);
					split_selected = position;
				}
			});
	}
	
	public boolean showingThis(){
		return Zmdl.tlay(main);
	}
	
	public void requestShow(){
		if(!Zmdl.im().hasCurrentInstance()){
			return;
		}
		dst_inst = Zmdl.inst();
		if(dst_inst.type != 1){
			Toast.error(Zmdl.gt("just_dff"),4f);
			return;
		}
		tv_instance.setConstraintWidth(0.24f);
		tv_instance.setText(Zmdl.gt("attach_tool")+":\n"+dst_inst.name+"\n"+Zmdl.gt("select_treeview"));
		dest = Zmdl.gos();
		Zmdl.svo(dest,false);
		obj_adapter.removeAll();
		int geom_ic = Texture.load(FX.fs.homeDirectory + "zmdl/geom_ic.png");
		if(((DFFSDK)Zmdl.inst().obj).isSkin()) {
			ArrayList<GeometryInstance> geoms = Zmdl.im().getModels();
			for(GeometryInstance geom : geoms){
				if(dest.getID() == geom.model_id || geom.inst.type != 1){
					continue;
				}
				ObjectItem o = new ObjectItem(geom_ic,geom.name);
				o.model_hash = geom.model_id;
				obj_adapter.add(o);
			}
			geoms.clear();
			geoms = null;
		}else{
			for(DFFGeometry geo : ((DFFSDK)Zmdl.inst().obj).geom){
				if(dest.getID() == geo.model_id){
					continue;
				}
				ObjectItem o = new ObjectItem(geom_ic,geo.name);
				o.model_hash = geo.model_id;
				obj_adapter.add(o);
			}
		}
		Zmdl.apl(main);
	}
	
	private void attach(){
		ZInstance src_inst = dst_inst.existModelId(source.getID()) ? dst_inst : Zmdl.im().getInstanceByModelId(source.getID());
		if(src_inst != null){
			DFFSDK dff_dst = ((DFFSDK)dst_inst.obj);
			DFFGeometry geo_dst = dff_dst.findGeometry(dest.getID());
			if(src_inst.type == 1){
				DFFSDK dff_src = ((DFFSDK)src_inst.obj);
				DFFGeometry geo_src = dff_src.findGeometry(source.getID());
				Matrix4f ltm = dff_src.getFrame(geo_src.frameIdx).getLocalModelMatrix();
				Matrix4f ltm_dest = dff_dst.getFrame(geo_dst.frameIdx).getLocalModelMatrix();
				Vector3f pos_src = new Vector3f(),pos_dest = new Vector3f();
				ltm.getLocation(pos_src);
				ltm_dest.getLocation(pos_dest);
				pos_src.subLocal(pos_dest);
				ltm.setLocation(0,0,0);
				ltm_dest = null;
				pos_dest = null;
				Vector3f tmp = new Vector3f();
				if(split_selected == 0){
					int newVertexCount = geo_dst.vertexCount + geo_src.vertexCount;
					int offset = 0;
					boolean hasMt = geo_dst.isModulateMaterial();
					boolean triStrip = (geo_dst.flags & DFFGeometry.GEOMETRY_FLAG_TEXCOORDS) != 0;
					boolean dynamicLight = (geo_dst.flags & DFFGeometry.GEOMETRY_FLAG_DYNAMIC_LIGHTING) != 0;
					geo_dst.flags = DFFGeometry.GEOMETRY_FLAG_POSITIONS;
					if(hasMt){
						geo_dst.flags |= DFFGeometry.GEOMETRY_FLAG_MODULATEMATCOLOR;
					}
					if(triStrip){
						geo_dst.flags |= DFFGeometry.GEOMETRY_FLAG_TRISTRIP;
					}
					if(dynamicLight){
						geo_dst.flags |= DFFGeometry.GEOMETRY_FLAG_DYNAMIC_LIGHTING;
					}
					float[] vertices = new float[newVertexCount * 3];
					float[] normals = null,texcoords = null,bone_weights = null;
					byte[] bone_indices = null;
					if(geo_dst.uvs != null && geo_src.uvs != null){
						texcoords = new float[newVertexCount * 2 * geo_src.uvsets];
						geo_dst.flags |= DFFGeometry.GEOMETRY_FLAG_TEXCOORDS;
					}
					if(geo_dst.normals != null && geo_src.normals != null){
						normals = new float[newVertexCount * 3];
						geo_dst.flags |= DFFGeometry.GEOMETRY_FLAG_NORMALS;
					}
					if(dff_dst.isSkin()){
						bone_weights = new float[newVertexCount * 4];
						bone_indices = new byte[newVertexCount * 4];
					}
					for(int i = 0;i < geo_dst.vertexCount;i++){
						vertices[i * 3] = geo_dst.vertices[i * 3];
						vertices[i * 3 + 1] = geo_dst.vertices[i * 3 + 1];
						vertices[i * 3 + 2] = geo_dst.vertices[i * 3 + 2];
						if(texcoords != null){
							texcoords[i * 2] = geo_dst.uvs[i * 2];
							texcoords[i * 2 + 1] = geo_dst.uvs[i * 2+1];
						}
						if(normals != null){
							normals[i * 3] = geo_dst.normals[i * 3];
							normals[i * 3 + 1] = geo_dst.normals[i * 3+1];
							normals[i * 3 + 2] = geo_dst.normals[i * 3+2];
						}
						if(dff_dst.isSkin()){
							bone_weights[i * 4] = geo_dst.skin.boneWeigts[i * 4];
							bone_weights[i * 4 + 1] = geo_dst.skin.boneWeigts[i * 4+1];
							bone_weights[i * 4 + 2] = geo_dst.skin.boneWeigts[i * 4+2];
							bone_weights[i * 4 + 3] = geo_dst.skin.boneWeigts[i * 4+3];
							bone_indices[i * 4] = geo_dst.skin.boneIndices[i * 4];
							bone_indices[i * 4 + 1] = geo_dst.skin.boneIndices[i * 4+1];
							bone_indices[i * 4 + 2] = geo_dst.skin.boneIndices[i * 4+2];
							bone_indices[i * 4 + 3] = geo_dst.skin.boneIndices[i * 4+3];
						}
						offset++;
					}
					final int init_offset = offset;
					for(int i = 0;i < geo_src.vertexCount;i++){
						tmp.set(geo_src.vertices,i*3);
						Vector3f result = ltm.mult(tmp);
						result.addLocal(pos_src);
						vertices[offset * 3 + 0] = result.x;
						vertices[offset * 3 + 1] = result.y;
						vertices[offset * 3 + 2] = result.z;
						if(texcoords != null){
							texcoords[offset * 2] = geo_src.uvs[i * 2];
							texcoords[offset * 2 + 1] = geo_src.uvs[i * 2+1];
						}
						if(normals != null){
							normals[offset * 3] = geo_src.normals[i * 3];
							normals[offset * 3 + 1] = geo_src.normals[i * 3 + 1];
							normals[offset * 3 + 2] = geo_src.normals[i * 3 + 2];
						}
						if(dff_dst.isSkin()){
							bone_weights[offset * 4] = 0;
							bone_weights[offset * 4 + 1] = 0;
							bone_weights[offset * 4 + 2] = 0;
							bone_weights[offset * 4 + 3] = 0;
							bone_indices[offset * 4] = 0;
							bone_indices[offset * 4 + 1] = 0;
							bone_indices[offset * 4 + 2] = 0;
							bone_indices[offset * 4 + 3] = 0;
						}
						offset++;
					}
					geo_dst.vertexCount = newVertexCount;
					geo_dst.vertices = vertices;
					if(texcoords != null){
						if((geo_src.flags & DFFGeometry.GEOMETRY_FLAG_MULTIPLEUVSETS) != 0 && geo_src.uvsets > 1){
							for(int i = geo_dst.vertexCount * 2;i < texcoords.length;i++){
								texcoords[i] = 0;
							}
							geo_dst.flags |= DFFGeometry.GEOMETRY_FLAG_MULTIPLEUVSETS;
						}
						geo_dst.uvs = texcoords;
						geo_dst.uvsets = geo_src.uvsets;
					}
					if(normals != null){
						geo_dst.normals = normals;
					}
					if(dff_dst.isSkin()){
						geo_dst.skin.boneWeigts = bone_weights;
						geo_dst.skin.boneIndices = bone_indices;
					}
					final int material_offset = geo_dst.materials.size();
					for(DFFMaterial mat : geo_src.materials){
						geo_dst.materials.add(mat);
					}
					for(DFFIndices idx : geo_src.splits){
						DFFIndices idx0 = new DFFIndices();
						idx0.material = idx.material + material_offset;
						idx0.index = new short[idx.index.length];
						for(int j = 0;j < idx0.index.length;j++){
							idx0.index[j] = (short)(idx.index[j] + init_offset);
						}
						geo_dst.splits.add(idx0);
					}
					ZObject o = Zmdl.go(geo_dst.model_id);
					o.getMesh().setVertices(geo_dst.vertices);
					if(geo_dst.uvs != null){
						o.getMesh().setTextureCoords(geo_dst.uvs);
					}
					if(geo_dst.normals != null){
						o.getMesh().setNormals(geo_dst.normals);
					}
					if(dff_dst.isSkin()){
						o.getMesh().setBoneIndices(bone_indices);
						o.getMesh().setBoneWeights(bone_weights);
					}
					Zmdl.app().getTextureManager().removeMesh(o.getMesh());
					o.getMesh().getParts().delete();
					for(int j = 0;j < geo_dst.splits.size();j++){
						DFFIndices indx = geo_dst.splits.get(j);
						DFFMaterial mat = geo_dst.materials.get(indx.material);
						MeshPart part = new MeshPart(indx.index);
						part.material.color.set(mat.color);
						part.material.textureName = mat.texture;
						o.getMesh().addPart(part);
					}
					Zmdl.app().getTextureManager().update(o.getMesh(),Zmdl.inst().path);
				}else{
					ArrayList<Integer> attach_v = getAttachVertices();
					int newVertexCount = geo_dst.vertexCount + attach_v.size();
					float[] vertices = new float[newVertexCount * 3];
					float[] normals = null,texcoords = null,bone_weights = null;
					byte[] bone_indices = null;
					if(geo_dst.uvs != null && geo_src.uvs != null){
						texcoords = new float[newVertexCount * 2 * geo_src.uvsets];
						geo_dst.flags |= DFFGeometry.GEOMETRY_FLAG_TEXCOORDS;
					}
					if(geo_dst.normals != null && geo_src.normals != null){
						normals = new float[newVertexCount * 3];
						geo_dst.flags |= DFFGeometry.GEOMETRY_FLAG_NORMALS;
					}
					if(dff_dst.isSkin()) {
						bone_weights = new float[newVertexCount*4];
						bone_indices = new byte[newVertexCount*4];
					}
					int offset = 0;
					for(int i = 0;i < geo_dst.vertexCount; i++){
						vertices[i * 3] = geo_dst.vertices[i * 3];
						vertices[i * 3 + 1] = geo_dst.vertices[i * 3 + 1];
						vertices[i * 3 + 2] = geo_dst.vertices[i * 3 + 2];
						if(texcoords != null) {
							texcoords[i * 2] = geo_dst.uvs[i * 2];
							texcoords[i * 2 + 1] = geo_dst.uvs[i * 2+1];
						}
						if(normals != null) {
							normals[i * 3] = geo_dst.normals[i * 3];
							normals[i * 3 + 1] = geo_dst.normals[i * 3+1];
							normals[i * 3 + 2] = geo_dst.normals[i * 3+2];
						}
						if(dff_dst.isSkin()) {
							bone_weights[i * 4] = geo_dst.skin.boneWeigts[i * 4];
							bone_weights[i * 4 + 1] = geo_dst.skin.boneWeigts[i * 4+1];
							bone_weights[i * 4 + 2] = geo_dst.skin.boneWeigts[i * 4+2];
							bone_weights[i * 4 + 3] = geo_dst.skin.boneWeigts[i * 4+3];
							bone_indices[i * 4] = geo_dst.skin.boneIndices[i * 4];
							bone_indices[i * 4 + 1] = geo_dst.skin.boneIndices[i * 4+1];
							bone_indices[i * 4 + 2] = geo_dst.skin.boneIndices[i * 4+2];
							bone_indices[i * 4 + 3] = geo_dst.skin.boneIndices[i * 4+3];
						}
						offset++;
					}
					HashMap<Integer,Integer> indextime = new HashMap<>();
					for(int i = 0;i < attach_v.size();i++){
						int index = attach_v.get(i);
						tmp.set(geo_src.vertices,index*3);
						indextime.put(index,offset);
						Vector3f result = ltm.mult(tmp);
						result.addLocal(pos_src);
						vertices[offset * 3 + 0] = result.x;
						vertices[offset * 3 + 1] = result.y;
						vertices[offset * 3 + 2] = result.z;
						if(texcoords != null){
							texcoords[offset * 2] = geo_src.uvs[index * 2];
							texcoords[offset * 2 + 1] = geo_src.uvs[index * 2+1];
						}
						if(normals != null){
							normals[offset * 3] = geo_src.normals[index * 3];
							normals[offset * 3 + 1] = geo_src.normals[index * 3+1];
							normals[offset * 3 + 2] = geo_src.normals[index * 3+2];
						}
						if(dff_dst.isSkin()){
							bone_weights[offset * 4] = 0;
							bone_weights[offset * 4 + 1] = 0;
							bone_weights[offset * 4 + 2] = 0;
							bone_weights[offset * 4 + 3] = 0;
							bone_indices[offset * 4] = 0;
							bone_indices[offset * 4 + 1] = 0;
							bone_indices[offset * 4 + 2] = 0;
							bone_indices[offset * 4 + 3] = 0;
						}
						offset++;
					}
					geo_dst.vertexCount = newVertexCount;
					geo_dst.vertices = vertices;
					if(texcoords != null){
						if((geo_src.flags & DFFGeometry.GEOMETRY_FLAG_MULTIPLEUVSETS) != 0 && geo_src.uvsets > 1){
							for(int i = geo_dst.vertexCount * 2;i < texcoords.length;i++){
								texcoords[i] = 0;
							}
							geo_dst.flags |= DFFGeometry.GEOMETRY_FLAG_MULTIPLEUVSETS;
						}
						geo_dst.uvs = texcoords;
						geo_dst.uvsets = geo_src.uvsets;
					}
					if(normals != null){
						geo_dst.normals = normals;
					}
					if(dff_dst.isSkin()){
						geo_dst.skin.boneWeigts = bone_weights;
						geo_dst.skin.boneIndices = bone_indices;
					}
					DFFIndices idx = geo_src.splits.get(split_selected - 1);
					geo_dst.materials.add(geo_src.materials.get(idx.material));
					DFFIndices idx0 = new DFFIndices();
					idx0.material = geo_dst.materials.size() - 1;
					idx0.index = new short[idx.index.length];
					for(int j = 0;j < idx0.index.length;j++){
						int k = indextime.get(idx.index[j] & 0xffff);
						idx0.index[j] = (short)k;
					}
					geo_dst.splits.add(idx0);
					ZObject o = Zmdl.go(geo_dst.model_id);
					o.getMesh().setVertices(geo_dst.vertices);
					if(geo_dst.uvs != null){
						o.getMesh().setTextureCoords(geo_dst.uvs);
					}
					if(geo_dst.normals != null){
						o.getMesh().setNormals(geo_dst.normals);
					}
					if(dff_dst.isSkin()){
						o.getMesh().setBoneIndices(bone_indices);
						o.getMesh().setBoneWeights(bone_weights);
					}
					Zmdl.app().getTextureManager().removeMesh(o.getMesh());
					o.getMesh().getParts().delete();
					for(int j = 0;j < geo_dst.splits.size();j++){
						DFFIndices indx = geo_dst.splits.get(j);
						DFFMaterial mat = geo_dst.materials.get(indx.material);
						MeshPart part = new MeshPart(indx.index);
						part.material.color.set(mat.color);
						part.material.textureName = mat.texture;
						o.getMesh().addPart(part);
					}
					Zmdl.app().getTextureManager().update(o.getMesh(),Zmdl.inst().path);
				}
				Toast.info(Zmdl.gt("attached"),4f);
			}else{
				Toast.error(Zmdl.gt("attach_not_comp"),5f);
			}
		}else{
			Toast.error("Error: Instance not found.",5f);
		}
	}
	
	private ArrayList<Integer> getAttachVertices() {
		ArrayList<Integer> out = new ArrayList<>();
		short[] indices = source.getMesh().getPart(split_selected - 1).index;
		for(int i = 0;i < indices.length;i++){
			if(out.indexOf(indices[i] & 0xffff) == -1) {
				out.add(indices[i] & 0xffff);
			}
		}
		return out;
	}
	
	private void dispose(){
		object_select = -1;
		source = null;
		splits.removeAll();
		obj_adapter.removeAll();
		lvObjects.setVisibility(View.VISIBLE);
		lvSplit.setVisibility(View.GONE);
		dest = null;
		dst_inst = null;
		Zmdl.svo(null,true);
	}
	
	@Override
	public boolean isShowing() {
		return Zmdl.tlay(main);
	}

	@Override
	public void close() {
		if(isShowing()){
			dispose();
			Zmdl.app().panel.dismiss();
		}
	}
}
