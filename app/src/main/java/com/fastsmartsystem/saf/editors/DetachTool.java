package com.fastsmartsystem.saf.editors;
import com.fastsmartsystem.saf.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.fastsmartsystem.saf.adapters.*;
import com.fastsmartsystem.saf.instance.*;
import com.forcex.gtasdk.*;
import java.util.*;
import com.forcex.gfx3d.*;
import com.fastsmartsystem.saf.processors.*;
import com.forcex.app.threading.*;
import com.forcex.core.*;
import com.forcex.*;

public class DetachTool extends PanelFragment implements ToggleButton.OnToggleListener {
	Layout main,tools;
	TextView tv_instance;
	ZInstance inst; // Current instance
	ToggleButton tbMaterial, tbGeometry;
	EditText etGeometry;
	SelectorWrapper selector;
	TextAdapter splits;
	ListView lvSplit;
	ZObject obj_current;
	int split_selected = 0;
	ArrayList<Short> triangles_selected;

	public DetachTool(){
		main = Zmdl.lay(0.25f,false);
		tv_instance = new TextView(Zmdl.gdf());
		tv_instance.setTextSize(0.05f);
		tv_instance.setAlignment(Layout.CENTER);
		tv_instance.setMarginBottom(0.01f);
		main.add(tv_instance);
		splits = new TextAdapter();
		lvSplit = new ListView(0.25f,0.4f,splits);
		main.add(lvSplit);

		tools = Zmdl.lay(0.25f,false);
		tools.setVisibility(View.GONE);
		tools.setMarginTop(0.02f);
		Layout main2 = Zmdl.lay(true);
		main2.setAlignment(Layout.CENTER);
		main2.setMarginTop(0.02f);

		tbMaterial = new ToggleButton(Zmdl.gt("part"),Zmdl.gdf(),0.1f,0.045f);
		tbMaterial.setOnToggleListener(this);
		main2.add(tbMaterial);

		tbGeometry = new ToggleButton(Zmdl.gt("geometry"),Zmdl.gdf(),0.1f,0.045f);
		tbGeometry.setOnToggleListener(this);
		main2.add(tbGeometry);
		main2.setMarginBottom(0.03f);
		tools.add(main2);

		etGeometry = new EditText(Zmdl.ctx(),0.2f,0.04f,0.04f);
		etGeometry.setAlignment(Layout.CENTER);
		etGeometry.setHint(Zmdl.gt("geometry_name"));
		etGeometry.setMarginBottom(0.01f);
		etGeometry.setMarginTop(0.01f);
		tools.add(etGeometry);
		Layout main4 = Zmdl.lay(true);
		main4.setAlignment(Layout.CENTER);
		main4.setMarginTop(0.02f);
		Button btnAccept = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.12f,0.04f);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(split_selected == -1){
						return;
					}
					if(tools.isVisible()){
						invokeDetach();
					}else{
						tools.setVisibility(View.VISIBLE);
						lvSplit.setVisibility(View.GONE);
						selector.type_select = 1;
						selector.split_index = split_selected;
						selector.setOnFinishedListener(new SelectorWrapper.OnFinishSelection(){
								@Override
								public void onFinish(boolean cancel, ArrayList<Short> selection) {
									if(cancel){
										dispose();
										Zmdl.svo(null,true);
										Zmdl.rp().testVisibilityFacts();
									}else{
										triangles_selected = selection;
										Zmdl.apl(main);
									}
								}
						});
						selector.requestShow(obj_current);
					}
				}
		});
		btnAccept.setId(0x453);
		main4.add(btnAccept);
		main4.setAlignment(Layout.CENTER);
		Button btnCancel = new Button(Zmdl.gt("cancel"),Zmdl.gdf(),0.12f,0.04f);
		btnCancel.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					dispose();
					Zmdl.svo(null,true);
					Zmdl.rp().testVisibilityFacts();
				}
		});
		btnCancel.setMarginLeft(0.01f);
		btnCancel.setId(0x454);
		main4.add(btnCancel);
		main.add(tools);
		main.add(main4);
		lvSplit.setOnItemClickListener(new ListView.OnItemClickListener(){
				@Override
				public void onItemClick(ListView view, Object item, short position, boolean longclick) {
					if(!longclick){
						obj_current.setSplitShow(position);
						split_selected = position;
					}else{
						showDialogDeletePart(position);
					}
				}
		});
		etGeometry.setVisibility(View.INVISIBLE);
		
	}
	
	private void showDialogDeletePart(final int pos){
		Layout lay = Zmdl.lay(false);
		TextView tv = new TextView(Zmdl.gdf());
		tv.setText(Zmdl.gt("delete_split"));
		tv.setTextSize(0.045f);
		lay.add(tv);
		Button btnOk = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.1f,0.045f);
		btnOk.setMarginTop(0.01f);
		btnOk.setAlignment(Layout.CENTER);
		lay.add(btnOk);
		final Dialog diag = new Dialog(lay);
		btnOk.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					diag.dismiss();
					deletePart(pos);
				}
			});
		diag.setTitle(Zmdl.gt("delete"));
		diag.show();
		
	}
	
	private void deletePart(int position){
		if(inst.type != 1){
			Toast.error(Zmdl.gt("just_dff"),4f);
			return;
		}
		obj_current.getMesh().getParts().list.remove(position);
		Znode node = Zmdl.tip().getNodeByModelId(obj_current.getID());
		DFFSDK dff = (DFFSDK)inst.obj;
		DFFGeometry geo_cur = dff.geom.get(dff.isSkin() ? 0 : node.geo_idx);
		int mat = geo_cur.splits.remove(position).material;
		boolean unused = true;
		for(DFFIndices idx : geo_cur.splits){
			if(idx.material == mat){
				unused = false;
			}
		}
		if(unused){
			for(DFFIndices i : geo_cur.splits) {
				if(i.material > position){
					i.material--;
				}
			}
			geo_cur.materials.remove(mat);
		}
		splits.removeAll();
		for(int i = 0;i < obj_current.getMesh().getParts().list.size();i++){
			splits.add(Zmdl.gt("split")+" "+i);
		}
		if(splits.getNumItem() > 0){
			obj_current.selected = false;
			obj_current.setSplitShow(0);
			split_selected = 0;
		}else{
			split_selected = -1;
		}
		processGeometry(geo_cur,null);
		Toast.info(Zmdl.gt("split_deleted"),4f);
	}
	
	public void requestShow(){
		if(!Zmdl.app().isEditMode()){
			Toast.warning(Zmdl.gt("enable_req_editm"),4f);
			return;
		}
		if(!Zmdl.hs() || !Zmdl.im().hasCurrentInstance() || Zmdl.tlay(main)){
			return;
		}
		selector = Zmdl.app().getSelectorWrap();
		obj_current = Zmdl.gos();
		splits.removeAll();
		for(int i = 0;i < obj_current.getMesh().getParts().list.size();i++){
			if(obj_current.getMesh().getPart(i).type == GL.GL_TRIANGLE_STRIP){
				selector = null;
				obj_current = null;
				Toast.error(Zmdl.gt("tri_strip_dete"),4);
				return;
			}
			splits.add(Zmdl.gt("split")+" "+i);
		}
		obj_current.selected = false;
		obj_current.setSplitShow(0);
		split_selected = 0;
		Zmdl.ep().pick_object = false;
		inst = Zmdl.inst();
		tv_instance.setText(Zmdl.gt("detach_tool")+":\n"+inst.name+" -> "+obj_current.getName());
		tbMaterial.setToggle(true);
		tbGeometry.setToggle(false);
		Zmdl.apl(main);
		Zmdl.svo(obj_current,false);
	}
	
	@Override
	public boolean isShowing() {
		return Zmdl.tlay(main);
	}
	
	private void dispose(){
		tools.setVisibility(View.GONE);
		lvSplit.setVisibility(View.VISIBLE);
		obj_current.selected = true;
		Zmdl.ep().pick_object = true;
		Zmdl.ep().requestShow();
		split_selected = 0;
		obj_current.setSplitShow(-1);
	}
	
	@Override
	public void close() {
		if(isShowing()){
			dispose();
			Zmdl.app().panel.dismiss();
		}
	}
	
	@Override
	public void onToggle(ToggleButton btn, boolean z) {
		if(btn == tbMaterial){
			tbGeometry.setToggle(!z);
			etGeometry.setVisibility(!z ? View.VISIBLE : View.GONE);
		}else if(btn == tbGeometry){
			tbMaterial.setToggle(!z);
			etGeometry.setVisibility(z ? View.VISIBLE : View.GONE);
		}
	}
	
	private void invokeDetach() {
		Zmdl.adtsk(new Task(){
				@Override
				public boolean execute(){
					detach();
					Zmdl.svo(null,true);
					Zmdl.rp().testVisibilityFacts();
					return true;
				}
			});
	}

	private void detach() {
		if(etGeometry.isVisible() && etGeometry.isEmpty()) {
			Toast.error(Zmdl.gt("geoname_empty"),5f);
			etGeometry.setEdgeMultiColor(210,25,25,245,20,20);
			return;
		}
		if(etGeometry.isVisible() && Zmdl.tip().getNodeByName(etGeometry.getText()) != null) {
			Toast.error(Zmdl.gt("name_a_e"),5f);
			etGeometry.setEdgeMultiColor(210,25,25,245,20,20);
			return;
		}
		MeshPart part = obj_current.getMesh().getPart(split_selected);
		if((triangles_selected.size() * 3) > part.index.length){
			Toast.error("Error: "+triangles_selected.size() +" is larger than "+(part.indxSize/3),4f);
			return;
		}
		Zmdl.app().getProgressScreen().show();
		short[] output = new short[part.index.length - (triangles_selected.size() * 3)];
		short[] output2 = new short[triangles_selected.size() * 3];
		for(int i = 0,j = 0,k = 0;i < part.index.length; i += 3){
			if(!existSelection(i / 3)){
				output[j] = part.index[i];
				output[j + 1] = part.index[i + 1];
				output[j + 2] = part.index[i + 2];
				j += 3;
			} else {
				output2[k] = part.index[i];
				output2[k + 1] = part.index[i + 1];
				output2[k + 2] = part.index[i + 2];
				k += 3;
			}
			Zmdl.app().getProgressScreen().setProgress(50 * (float)i/part.index.length);
		}
		Znode node = Zmdl.tip().getNodeByModelId(obj_current.getID());
		DFFSDK dff = (DFFSDK)inst.obj;
		final DFFGeometry geo_cur = dff.geom.get(node.geo_idx);
		if(output.length == 0) {
			obj_current.getMesh().getParts().list.remove(split_selected);
			int mat = geo_cur.splits.remove(split_selected).material;
			boolean unused = true;
			for(DFFIndices idx : geo_cur.splits) {
				if(idx.material == mat){
					unused = false;
				}
			}
			if(unused) {
				for(DFFIndices i : geo_cur.splits) {
					if(i.material > mat){
						i.material--;
					}
				}
				geo_cur.materials.remove(mat);
			}
		}
		final MeshPart newpart = new MeshPart(output2);
		newpart.material = part.material.clone();
		DFFIndices dffi = new DFFIndices();
		DFFMaterial mat = new DFFMaterial();
		mat.color = newpart.material.color;
		mat.texture = part.material.textureName;
		mat.surfaceProp = new float[]{1,1,1};
		obj_current.setSplitShow(-1);
		dffi.index = output2;
		if(etGeometry.isVisible()) { // as geometry
			final DFFGeometry geo_new = new DFFGeometry();
			geo_new.flags = (DFFGeometry.GEOMETRY_FLAG_POSITIONS | DFFGeometry.GEOMETRY_FLAG_TRISTRIP);
			Zmdl.app().getProgressScreen().setProgress(51f);
			if(output.length > 0){
				geo_cur.splits.get(split_selected).index = output;
			}
			ArrayList<Integer> vertex_deleted = getIndicesToSeparate(output2);
			loadGeometry(geo_cur,geo_new,vertex_deleted,output2);
			processGeometry(geo_cur,vertex_deleted);
			newpart.index = output2;
			geo_new.materials.add(mat);
			geo_new.splits.add(dffi);
			geo_new.hasMeshExtension = true;
			dffi.material = geo_new.materials.size() - 1;
			DFFFrame frame_new = new DFFFrame();
			DFFFrame frame_cur = dff.getFrame(node.frame_idx);
			frame_new.name = geo_new.name;
			frame_new.rotation = frame_cur.rotation;
			frame_new.position = frame_cur.position.clone();
			frame_new.flags = frame_cur.flags;
			dff.addFrame(frame_new);
			dff.addGeometry(geo_new);
			frame_new.geoAttach = (short)(dff.geometryCount - 1);
			geo_new.frameIdx = (short)(dff.frameCount - 1);
			DFFAtomic atomic = dff.findAtomicByFrame(geo_cur.frameIdx).clone();
			atomic.frameIdx = geo_new.frameIdx;
			atomic.geoIdx = frame_new.geoAttach;
			dff.addAtomic(atomic);
			if(frame_cur.parent != null){
				frame_new.parent = frame_cur.parent;
				frame_cur.parent.children.add(frame_new);
			}else{
				frame_new.parent = frame_cur;
				frame_cur.children.add(frame_new);
			}
			dff.updateParents(dff.getFrameRoot());
			Toast.info(Zmdl.gt("finished")+" "+etGeometry.getText(),4f);
			Mesh mesh = new Mesh(true);
			mesh.setVertices(geo_new.vertices);
			if(geo_new.uvs != null){
				mesh.setTextureCoords(geo_new.uvs);
			}
			if(geo_new.hasNormals()){
				mesh.setNormals(geo_new.normals);
			}
			mesh.addPart(newpart);
			final ZObject obj_new = new ZObject(mesh);
			obj_new.setID(Zmdl.im().genID());
			obj_new.setName(geo_new.name);
			frame_new.model_id = obj_new.getID();
			geo_new.model_id = obj_new.getID();
			inst.addHash(obj_new.getName(),(int)obj_new.getID());
			obj_new.setTransform(obj_current.getTransform());
			inst.root = FileProcessor.setTreeNodes(dff,dff.getFrameRoot());
			Zmdl.im().setInstanceCurrent(inst);
			Zmdl.ep().pick_object = true;
			Zmdl.ep().requestShow();
			Zmdl.rp().addObject(obj_new);
			Zmdl.rp().rewind();
			Zmdl.ns();
			etGeometry.setVisibility(View.GONE);
		} else { // as part
			if(output.length > 0) {
				part.index = output;
				part.buffer.reset();
				part.indxSize = output.length;
				part.visible = true;
				geo_cur.splits.get(split_selected).index = output;
			}
			Zmdl.app().getProgressScreen().setProgress(80f);
			geo_cur.materials.add(mat);
			geo_cur.splits.add(dffi);
			dffi.material = geo_cur.materials.size() - 1;
			Toast.info(Zmdl.gt("finished"),4f);
			FX.gpu.queueTask(new Task(){
					@Override
					public boolean execute()
					{
						obj_current.getMesh().addPart(newpart);
						MaterialEditor mate = Zmdl.app().getMaterialEditor();
						mate.justObtainMaterials = true;
						mate.requestShow();
						mate.setObjectCurrent(obj_current.getID());
						mate.setCurrentMaterial(geo_cur.materials.size() - 1);
						return true;
					}
			});
		}
		tools.setVisibility(View.GONE);
		lvSplit.setVisibility(View.VISIBLE);
		Zmdl.app().getProgressScreen().dismiss();
		Zmdl.ns();
	} 

	private ArrayList<Integer> getIndicesToSeparate(short[] idx) {
		ArrayList<Integer> indices = new ArrayList<>();
		for (short value : idx) {
			if (!indices.contains(value & 0xffff)) {
				indices.add(value & 0xffff);
			}
		}
		return indices;
	}

	private void loadGeometry(DFFGeometry src,DFFGeometry dest,ArrayList<Integer> indices,short[] index) {
		dest.vertices = new float[indices.size() * 3];
		dest.vertexCount = indices.size();
		for(int i = 0;i < indices.size(); i++){
			int k = indices.get(i);
			dest.vertices[i * 3] = src.vertices[k * 3];
			dest.vertices[i * 3 + 1] = src.vertices[k * 3 + 1];
			dest.vertices[i * 3 + 2] = src.vertices[k * 3 + 2];
			Zmdl.app().getProgressScreen().setProgress(51 + 5f * (float)i/indices.size());
		}
		if(src.uvs != null) {
			dest.uvs = new float[indices.size() * 2 * ((src.flags & DFFGeometry.GEOMETRY_FLAG_MULTIPLEUVSETS) != 0 ?  src.uvsets : 1)];
			dest.flags |= DFFGeometry.GEOMETRY_FLAG_TEXCOORDS;
			for(int i = 0;i < indices.size();i++){
				int k = indices.get(i);
				dest.uvs[i * 2] = src.uvs[k * 2];
				dest.uvs[i * 2+1] = src.uvs[k * 2+1];
				Zmdl.app().getProgressScreen().setProgress(56 + 2f * (float)i/indices.size());
			}
			if((src.flags & DFFGeometry.GEOMETRY_FLAG_MULTIPLEUVSETS) != 0 && src.uvsets > 1){
				dest.uvsets = 2;
				for(int i = indices.size() * 2;i < dest.uvs.length;i++){
					dest.uvs[i] = 0;
				}
				dest.flags |= DFFGeometry.GEOMETRY_FLAG_MULTIPLEUVSETS;
			}else{
				dest.uvsets = 1;
			}
		}
		if(src.normals != null) {
			dest.normals = new float[indices.size()*3];
			dest.flags |= DFFGeometry.GEOMETRY_FLAG_NORMALS;
			for(int i = 0;i < indices.size();i++){
				int k = indices.get(i);
				dest.normals[i * 3] = src.normals[k*3];
				dest.normals[i * 3 + 1] = src.normals[k*3+1];
				dest.normals[i * 3 + 2] = src.normals[k*3+2];
				Zmdl.app().getProgressScreen().setProgress(58 + 5f * (float)i/indices.size());
			}
		}
		if(src.isModulateMaterial()){
			dest.flags |= DFFGeometry.GEOMETRY_FLAG_MODULATEMATCOLOR;
		}
		if((src.flags & DFFGeometry.GEOMETRY_FLAG_DYNAMIC_LIGHTING) != 0){
			dest.flags |= DFFGeometry.GEOMETRY_FLAG_DYNAMIC_LIGHTING;
		}
		dest.name = etGeometry.getText();
		Toast.debug("("+indices.size()+" vertices "+(index.length/3)+" triangles)",4f);
		for(int j = 0;j < index.length;j++){
			int k = indices.indexOf(index[j] & 0xffff);
			index[j] = (short)k;
			Zmdl.app().getProgressScreen().setProgress(63 + 2f * (float)j/index.length);
		}
	}
	
	private void processGeometry(final DFFGeometry src,ArrayList<Integer> indxs) {
		HashMap<Integer,Integer> indextime = new HashMap<>();
		for(int i = 0,j = 0;i < src.vertexCount;i++){
			if((indxs != null && indxs.indexOf(i) == -1) || existInIndex(src,i)){
				indextime.put(i,j);
				j++;
			}
			Zmdl.app().getProgressScreen().setProgress(65f + 30f * (float)i/src.vertexCount);
		}
		int newVertexCount = indextime.size();
		float[] vertices = new float[newVertexCount * 3];
		float[] normals = null,texcoords = null;
		if(src.uvs != null){
			texcoords = new float[newVertexCount * 2 * src.uvsets];
		}
		if(src.normals != null){
			normals = new float[newVertexCount * 3];
		}
		for(Integer old : indextime.keySet()){
			Integer nidx = indextime.get(old);
			vertices[nidx * 3] = src.vertices[old * 3];
			vertices[nidx * 3 + 1] = src.vertices[old * 3 + 1];
			vertices[nidx * 3 + 2] = src.vertices[old * 3 + 2];
			if(texcoords != null){
				texcoords[nidx * 2] = src.uvs[old * 2];
				texcoords[nidx * 2 + 1] = src.uvs[old * 2+1];
			}
			if(normals != null){
				normals[nidx * 3] = src.normals[old * 3];
				normals[nidx * 3 + 1] = src.normals[old * 3+1];
				normals[nidx * 3 + 2] = src.normals[old * 3+2];
			}
		}
		src.vertexCount = newVertexCount;
		src.vertices = vertices;
		if(texcoords != null){
			if((src.flags & DFFGeometry.GEOMETRY_FLAG_MULTIPLEUVSETS) != 0 && src.uvsets > 1){
				for(int i = src.vertexCount * 2;i < texcoords.length;i++){
					texcoords[i] = 0;
				}
			}
			src.uvs = texcoords;
		}
		if(normals != null){
			src.normals = normals;
		}
		for(int j = 0;j < src.splits.size();j++) {
			DFFIndices split = src.splits.get(j);
			for(int i = 0;i < split.index.length;i++){
				int k = indextime.get(split.index[i] & 0xffff);
				split.index[i] = (short)k;
			}
			Zmdl.app().getProgressScreen().setProgress(95f + 5f * (float)j/src.splits.size());
		}
		FX.gpu.queueTask(new Task(){
				@Override
				public boolean execute() {
					obj_current.getMesh().setVertices(src.vertices);
					if(src.uvs != null){
						obj_current.getMesh().setTextureCoords(src.uvs);
					}
					if(src.normals != null){
						obj_current.getMesh().setNormals(src.normals);
					}
					for(int i = 0;i < src.splits.size();i++){
						obj_current.getMesh().getPart(i).index = src.splits.get(i).index;
						obj_current.getMesh().getPart(i).buffer.reset();
						obj_current.getMesh().getPart(i).indxSize = src.splits.get(i).index.length;
						obj_current.getMesh().getPart(i).visible = true;
					}
					return true;
				}
		});
	}
	
	private boolean existInIndex(DFFGeometry src,int i) {
		for(DFFIndices idx : src.splits){
			for(short j : idx.index){
				if((j & 0xffff) == i){
					return true;
				}
			}
		}
		return false;
	}

	private boolean existSelection(int triangle) {
		ListIterator<Short> it = triangles_selected.listIterator();
		while(it.hasNext()) {
			if(it.next() == triangle) {
				return true;
			}
		}
		return false;
	}
	
} 
