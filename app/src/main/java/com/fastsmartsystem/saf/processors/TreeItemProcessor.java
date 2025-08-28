package com.fastsmartsystem.saf.processors;
import com.fastsmartsystem.saf.*;
import com.fastsmartsystem.saf.adapters.*;
import com.fastsmartsystem.saf.editors.*;
import com.fastsmartsystem.saf.instance.*;
import com.forcex.*;
import com.forcex.core.gpu.*;
import com.forcex.gfx3d.*;
import com.forcex.gfx3d.shapes.*;
import com.forcex.gtasdk.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.forcex.math.*;
import com.forcex.utils.*;
import java.util.*;
import com.forcex.anim.*;

public class TreeItemProcessor extends PanelFragment implements ListView.OnItemClickListener {
	Layout main;
	TextView tv_name;
	Znode node;
	ZObject obj_current;
	MenuAdapter adapter;
	boolean copy;
	boolean move;
	boolean add;
	boolean join;
	Znode move_node;
	int inst_move_id;
	Dialog copy_diag;
	
	public TreeItemProcessor(){
		main = Zmdl.lay(0.25f,false);
		tv_name = new TextView(Zmdl.gdf());
		tv_name.setConstraintWidth(0.25f);
		tv_name.setTextSize(0.045f);
		main.add(tv_name);
		tv_name.setMarginBottom(0.01f);
		adapter = new MenuAdapter();
		adapter.add(Texture.load("zmdl/add.png"),Zmdl.gt("add"));
		adapter.add(Texture.load("zmdl/show.png"),Zmdl.gt("show"));
		adapter.add(-1,Zmdl.gt("select"));
		adapter.add(Texture.load("zmdl/extract.png"),Zmdl.gt("export"));
		adapter.add(Texture.load("zmdl/rename.png"),Zmdl.gt("rename"));
		adapter.add(Texture.load("zmdl/details.png"),Zmdl.gt("properties"));
		adapter.add(Texture.load("zmdl/material.png"),Zmdl.gt("materials"));
		adapter.add(Texture.load("zmdl/replace.png"),Zmdl.gt("replace"));
		adapter.add(Texture.load("zmdl/transform.png"),Zmdl.gt("transform"));
		adapter.add(Texture.load("zmdl/attach.png"),Zmdl.gt("join"));
		adapter.add(Texture.load("zmdl/extract.png"),Zmdl.gt("move"));
		adapter.add(Texture.load("zmdl/replace.png"),Zmdl.gt("copy"));
		adapter.add(Texture.load("zmdl/delete.png"),Zmdl.gt("delete"));
		ListView menu = new ListView(0.25f,0.5f,adapter);
		menu.setInterlinedColor(210,210,210,210);
		menu.setOnItemClickListener(this);
		main.add(menu);
		Button btnAccept = new Button(Zmdl.gt("close"),Zmdl.gdf(),0.245f,0.05f);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					close();
				}
			});
		btnAccept.setAlignment(Layout.CENTER);
		btnAccept.setMarginTop(0.02f);
		main.add(btnAccept);
	}
	
	public void requestShow(Znode node){
		if(Zmdl.tlay(main)){
			return;
		}
		this.node = node;
		if(node.isGeometry){
			obj_current = Zmdl.go(node.model_kh);
			if(obj_current == null){
				Toast.error("Internal Error: Geometry not found",3f);
				return;
			}
		}
		tv_name.setText(Zmdl.gt("options")+":\n"+node.name+" ["+(node.isGeometry ? "Geometry" : "Dummy")+"]");
		Zmdl.apl(main);
	}
	
	public Znode getNodeByModelId(int model_kh){
		return search(Zmdl.inst().root,model_kh);
	}
	
	public Znode getNodeByName(String name){
		return search(Zmdl.inst().root,name);
	}

	private Znode search(Znode root,String name){
		if(root.name.equals(name)){
			return root;
		}
		for(TreeNode n : root.getChildren()){
			Znode test = search((Znode)n,name);
			if(test != null){
				return test;
			}
		}
		return null;
	}
	
	private Znode search(Znode root,int kh){
		if(root.model_kh == kh){
			return root;
		}
		for(TreeNode n : root.getChildren()){
			Znode test = search((Znode)n,kh);
			if(test != null){
				return test;
			}
		}
		return null;
	}

	@Override
	public void onItemClick(ListView view, Object item, short position, boolean longclick) {
		switch(position){
			case 0: // ADD
				addNode();
				break;
			case 1: // SHOW
				if(node.isGeometry){
					Zmdl.svo(obj_current,false);
					Zmdl.rp().show_obj_att = true;
				}else{
					Zmdl.rp().setVisibleObjectByNode(node);
					Zmdl.rp().show_obj_att = true;
				}
				break;
			case 2: // SELECT
				if(node.isGeometry){
					Zmdl.rp().unselectAll();
					obj_current.selected = !obj_current.selected;
				}else{
					Toast.error(Zmdl.gt("isnt_geometry"),2f);
				}
				break;
			case 3: // EXPORT
				if(node.isGeometry){
					exportNode(node);
				}else{
					Toast.error(Zmdl.gt("isnt_geometry"),2f);
				}
				break;
			case 4: // RENAME
				rename(node);
				node = null;
				obj_current = null;
				break;
			case 5: // PROPERTIES
				properties();
				break;
			case 6: // MATERIALS
				if(node.isGeometry) {
					MaterialEditor mate = Zmdl.app().getMaterialEditor();
					mate.justObtainMaterials = true;
					mate.requestShow();
					mate.setObjectCurrent(obj_current.getID());
					Zmdl.ns();
				}else{
					Toast.error(Zmdl.gt("isnt_geometry"),2f);
				}
				break;
			case 7: // REPLACE
				if(node.isGeometry) {
					if (!Zmdl.app().isEditMode()) {
						Toast.warning(Zmdl.gt("enable_req_editm"), 4f);
						return;
					}
					showSelectGeometry(new OnSelectListener(){
							@Override
							public void dismiss() {
								// TODO: Implement this method
							}

							@Override
							public void select(ZInstance inst,short model_id) {
								if(model_id == -1){
									Toast.error(Zmdl.gt("select_rp"),3f);
									return;
								}
								DFFSDK dff = (DFFSDK)Zmdl.inst().obj;
								for(MeshPart p : obj_current.getMesh().getParts().list){
									p.material.diffuseTexture = -1;
								}
								if(inst.type == 1){
									DFFSDK dffsrc = (DFFSDK)inst.obj;
									dff.geom.get(node.geo_idx).replace(dffsrc.findGeometry(model_id),obj_current.getMesh());
								} else {
									DFFGeometry geo = new DFFGeometry();
									geo.fromMesh(((ZObject)inst.obj).getMesh());
									dff.geom.get(node.geo_idx).replace(geo,obj_current.getMesh());
								}
								obj_current.selected = true;
								Zmdl.app().getTransformEditor().requestShow();
								Zmdl.app().getTextureManager().update(obj_current.getMesh(),Zmdl.inst().path);
								node = null;
								obj_current = null;
								Zmdl.ns();
							}
					});
				}else{
					Toast.error(Zmdl.gt("isnt_geometry"),2f);
				}
				break;
			case 8: // TRANSFORM
				if(node.isGeometry) {
					if(Zmdl.app().isEditMode()){
						obj_current.selected = true;
						Zmdl.app().getTransformEditor().requestShow();
						node = null;
						obj_current = null;
						return;
					}else{
						Toast.warning(Zmdl.gt("enable_req_editm"), 4f);
					}
				} else {
					if(Zmdl.app().isEditMode()){
						Zmdl.app().getTransformEditor().setDummyEdition(node.name);
						Zmdl.app().getTransformEditor().requestShow();
						node = null;
						obj_current = null;
						return;
					}else{
						Toast.warning(Zmdl.gt("enable_req_editm"), 4f);
					}
				}
				break;
			case 9:
				if(Zmdl.inst().type != 1){
					Toast.error(Zmdl.gt("just_dff"),4f);
					return;
				}
				if(((DFFSDK)Zmdl.inst().obj).isSkin()){
					Toast.error(Zmdl.gt("join_skin"),4f);
					return;
				}
				if(node.isGeometry) {
					join = true;
					move_node = node;
					inst_move_id = Zmdl.inst().id;
					showOperator();
					Zmdl.ns();
				}else{
					Toast.error(Zmdl.gt("isnt_geometry"),2f);
				}
				break;
			case 10: // MOVE
				move = true;
				move_node = node;
				inst_move_id = Zmdl.inst().id;
				showOperator();
				break;
			case 11: // COPY
				copy = true;
				Bridge b = Zmdl.im().getBridge();
				b.node = node;
				b.src = Zmdl.inst();
				showOperator();
				break;
			case 12: // DELETE
				if(Zmdl.inst().type == 1){
					try{
						ZInstance inst = Zmdl.inst();
						DFFSDK dff = (DFFSDK)inst.obj;
						dff.getFrame(node.frame_idx).delete(dff);
						Zmdl.rp().removeObjectsNode(node);
						inst.root = FileProcessor.setTreeNodes(dff,dff.getFrameRoot());
						Zmdl.app().tree_adapter.setTreeNode(inst.root);
						node = null;
					}catch(Exception e){
						Logger.log(e);
						Zmdl.app().sendEmailDialog(true);
					}
				}else{
					Toast.error("Not compatible",4f);
				}
				break;
		}
		Zmdl.app().panel.dismiss();
	}
	
	public boolean processBridge(final Znode proc) {
		Bridge bridge = Zmdl.im().getBridge();
		if(copy){
			if(bridge.node == proc){
				Toast.error(Zmdl.gt("error_copy1"),5);
				return false;
			}else if(Zmdl.inst().type == 1){ 
				node = proc;
				if(bridge.src == Zmdl.inst()){
					if(bridge.node.model_kh != -1) {
						node_inst_id = Zmdl.inst().id;
						node_add_mid = bridge.node.model_kh;
					}
					addNode(bridge.node.name+"_copy");
				} else {
					if(bridge.node.model_kh != -1) {
						node_inst_id = bridge.src.id;
						node_add_mid = bridge.node.model_kh;
					}
					addNode(bridge.node.name+"_copy");
				}
				bridge.dispose();
				copy_diag.dismiss();
				copy_diag = null;
				copy = false;
			}else{
				Toast.error(Zmdl.gt("just_dff"),4f);
			}
			return false;
		} else if(move) {
			if(inst_move_id != Zmdl.inst().id){
				Toast.error(Zmdl.gt("error_inst_move1"),5);
				return false;
			}
			if(move_node == proc || move_node.isChild(proc)) {
				Toast.error(Zmdl.gt("error_move1"),5);
				return false;
			}   
			DFFSDK dff = (DFFSDK)Zmdl.inst().obj;
			DFFFrame target = dff.getFrame(proc.frame_idx);
			DFFFrame src = dff.getFrame(move_node.frame_idx);
			if(src.parent != null){
				src.parent.children.remove(src);
			}
			target.children.add(src);
			src.parent = target;
			src.invalidateLTM();
			dff.updateParents(dff.getFrameRoot());
			Zmdl.inst().root = FileProcessor.setTreeNodes(dff,dff.getFrameRoot());
			Zmdl.app().tree_adapter.setTreeNode(Zmdl.inst().root);
			TransformEditor.updateFrameObject(src);
			move_node = null;
			move = false;
			copy_diag.dismiss();
			copy_diag = null;
			return false;
		}else if(add) {
			Mesh mesh = null;
			String name = "";
			switch(Zmdl.ap().object){
				case 0:
					mesh = new Box(1,1,1f);
					name = "box";
					break;
				case 1:
					mesh = new Sphere(1f,30,30);
					name = "sphere";
					break;
				case 2:
					mesh = new Cylinder(10,10,1f,1f,2f,true);
					name = "cylinder";
					break;
			}
			DFFSDK dff = (DFFSDK)Zmdl.inst().obj;
			DFFFrame dest = dff.getFrame(proc.frame_idx);
			DFFFrame frame_new = new DFFFrame();
			frame_new.name = name;
			frame_new.rotation = new Matrix3f();
			frame_new.position = new Vector3f();
			frame_new.flags = 0;
			frame_new.parentIdx = -1;
			frame_new.parent = dest;
			frame_new.invalidateLTM();
			dest.children.add(frame_new);
			DFFGeometry geo = new DFFGeometry();
			geo.flags = (DFFGeometry.GEOMETRY_FLAG_TRISTRIP | DFFGeometry.GEOMETRY_FLAG_MODULATEMATCOLOR);
			geo.fromMesh(mesh);
			ZObject o = new ZObject();
			o.setMesh(mesh);
			geo.name = name;
			o.setName(name);
			o.setID(Zmdl.im().genID());
			Zmdl.inst().addHash(name,o.getID());
			geo.model_id = o.getID();
			frame_new.model_id = o.getID();
			geo.frameIdx = dff.frameCount;
			frame_new.geoAttach = (short)dff.geometryCount;
			dff.addGeometry(geo);
			DFFAtomic atomic = new DFFAtomic();
			atomic.frameIdx = geo.frameIdx;
			atomic.geoIdx = frame_new.geoAttach;
			atomic.unknown1 = 5;
			atomic.hasRenderToRight = true;
			atomic.RTRval1 = 0x120;
			atomic.RTRval2 = 0;
			dff.addAtomic(atomic);
			o.setTransform(frame_new.getLocalModelMatrix());
			Zmdl.app().getTextureManager().update(o.getMesh(),Zmdl.inst().path);
			Zmdl.rp().addObject(o);
			Zmdl.rp().rewind();
			dff.addFrame(frame_new);
			dff.updateParents(dff.getFrameRoot());
			Zmdl.inst().root = FileProcessor.setTreeNodes(dff,dff.getFrameRoot());
			Zmdl.app().tree_adapter.setTreeNode(Zmdl.inst().root);
			copy_diag.dismiss();
			copy_diag = null;
			add = false;
			return false;
		}else if(join) {
			if(inst_move_id != Zmdl.inst().id){
				Toast.error(Zmdl.gt("error_inst_join1"),5);
				return false;
			}
			if(move_node == proc) {
				Toast.error(Zmdl.gt("error_join"),5);
				return false;
			} 
			if(proc.geo_idx == -1){
				return false;
			}
			DFFSDK dff = (DFFSDK)Zmdl.inst().obj;
			DFFFrame dest = dff.getFrame(proc.frame_idx);
			DFFFrame src = dff.getFrame(move_node.frame_idx);
			joinGeometry(dff,src,dest);
			if(src.children.size() > 0){
				ZObject o = Zmdl.go(src.model_id);
				dff.deleteGeometry(src);
				if(o != null){
					Zmdl.rp().getList().remove(o);
				}
			}else{
				src.delete(dff);
				Zmdl.rp().removeObjectsNode(move_node);
			}
			src.model_id = -1;
			dff.updateParents(dff.getFrameRoot());
			Zmdl.inst().root = FileProcessor.setTreeNodes(dff,dff.getFrameRoot());
			Zmdl.app().tree_adapter.setTreeNode(Zmdl.inst().root);
			TransformEditor.updateFrameObject(dff.getFrameRoot());
			Zmdl.rp().transformed = true;
			Zmdl.rp().rewind();
			move_node = null;
			join = false;
			copy_diag.dismiss();
			copy_diag = null;
			return false;
		}
		return true;
	}
	
	@Override
	public boolean isShowing() {
		return Zmdl.tlay(main);
	} 
	
	public void showOperator() {
		if(copy_diag != null) {
			return;
		}
		Layout lay = new Layout(Zmdl.ctx());
		copy_diag = new Dialog(lay);
		copy_diag.setTitle(move ? Zmdl.gt("move") : (add ? Zmdl.gt("add") : (join ? Zmdl.gt("join"): Zmdl.gt("copy"))));
		Button btnCancel = new Button(Zmdl.gt("cancel"),Zmdl.gdf(),0.2f,0.04f);
		btnCancel.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					copy = false;
					move = false;
					add = false;
					join = false;
					move_node = null;
					Zmdl.im().getBridge().dispose();
					copy_diag.dismiss();
					copy_diag = null;
					Toast.info(Zmdl.gt("canceled"),4);
				}
			});
		btnCancel.setMarginLeft(0.01f);
		lay.add(btnCancel);
		copy_diag.setUseCloseButton(false);
		copy_diag.show(0.7f,-0.9f);
	}
	
	private static void exportNode(final Znode node){
		if(node == null){
			Toast.info("Error: node == null: export",3f);
			return;
		}
		Layout lay = new Layout(Zmdl.ctx());
		final Dialog diag = new Dialog(lay);
		Button btnOBJ = new Button("OBJ",Zmdl.gdf(),0.2f,0.06f);
		btnOBJ.setMarginBottom(0.02f);
		btnOBJ.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					FileDialog.create(Zmdl.ctx(), Zmdl.gt("select_folder"), Zmdl.fp().getCurrentPath(), new FileDialog.OnResultListener(){
							@Override
							public boolean tryCancel(short id) {
								return true;
							}
							@Override
							public void open(short id, String path) {
								final ZObject o = Zmdl.go(node.model_kh);
								FileProcessor.showExportOBJOptions(path+"/",o.getMesh(),node.name);
							}
					},Zmdl.app().lang,0x45);
					diag.dismiss();
				}
			});
		lay.add(btnOBJ);
		Button btnDFF = new Button("DFF",Zmdl.gdf(),0.2f,0.06f);
		btnDFF.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					FileDialog.create(Zmdl.ctx(), Zmdl.gt("select_folder"), Zmdl.fp().getCurrentPath(), new FileDialog.OnResultListener(){
							@Override
							public boolean tryCancel(short id) {
								return true;
							}
							@Override
							public void open(short id, String path) {
								exportDFFname(node,path+"/");
							}
						},Zmdl.app().lang,0x45);
					diag.dismiss();
				}
			});
		lay.add(btnDFF);
		Button btnCancel = new Button(Zmdl.gt("cancel"),Zmdl.gdf(),0.2f,0.06f);
		btnCancel.setMarginTop(0.02f);
		btnCancel.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					diag.dismiss();
				}
			});
		lay.add(btnCancel);
		diag.setTitle(Zmdl.gt("export"));
		diag.show();
	}
	
	private static void exportDFF(String name,DFFGeometry geo,ZObject obj,String path) {
		ZInstance inst = Zmdl.inst();
		DFFSDK dff = new DFFSDK();
		dff.game = DFFGame.GTASA;
		dff.name = name;
		DFFFrame frame_new = new DFFFrame();
		frame_new.name = geo != null ? geo.name : obj.getName();
		frame_new.rotation = new Matrix3f();
		frame_new.position = new Vector3f();
		frame_new.flags = 0;
		frame_new.parentIdx = -1;
		dff.addFrame(frame_new);
		DFFGeometry geo_new = new DFFGeometry();
		geo_new.name = frame_new.name;
		if(inst.type == 1){
			geo_new.replace(geo,null);
		}else{
			geo_new.flags = (DFFGeometry.GEOMETRY_FLAG_TRISTRIP | DFFGeometry.GEOMETRY_FLAG_MODULATEMATCOLOR);
			geo_new.fromMesh(obj.getMesh());
		}
		dff.addGeometry(geo_new);
		frame_new.geoAttach = (short)(dff.geometryCount - 1);
		geo_new.frameIdx = (short)(dff.frameCount - 1);
		DFFAtomic atomic = new DFFAtomic();
		atomic.frameIdx = geo_new.frameIdx;
		atomic.geoIdx = frame_new.geoAttach;
		atomic.unknown1 = 5;
		atomic.hasRenderToRight = true;
		atomic.RTRval1 = 0x120;
		atomic.RTRval2 = 0;
		dff.addAtomic(atomic);
		Zmdl.adtsk(new FileProcessor.SaveDFF(path,dff));
	}
	
	private static void exportDFFname(final Znode node,final String path){
		Layout lay = new Layout(Zmdl.ctx());
		final Dialog diag = new Dialog(lay);
		final EditText et = new EditText(Zmdl.ctx(),0.3f,0.05f,0.05f);
		et.setAutoFocus(true);
		et.setText(node.name);
		lay.add(et);
		Layout main4 = Zmdl.lay(true);
		main4.setMarginTop(0.02f);
		Button btnAccept = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.14f,0.045f);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(et.getText().isEmpty()){
						return;
					}
					if(Zmdl.inst().type == 1){
						exportDFF(et.getText(),((DFFSDK)Zmdl.inst().obj).findGeometry(node.model_kh),null,path);
					}else{
						exportDFF(et.getText(),null,(ZObject)Zmdl.inst().obj,path);
					}
					diag.dismiss();
				}
			});
		main4.add(btnAccept);
		Button btnCancel = new Button(Zmdl.gt("cancel"),Zmdl.gdf(),0.14f,0.045f);
		btnCancel.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					diag.dismiss();
				}
			});
		btnCancel.setMarginLeft(0.01f);
		main4.add(btnCancel);
		main4.setAlignment(Layout.CENTER);
		lay.add(main4);
		diag.setTitle(Zmdl.gt("export")+" DFF");
		diag.show(0,0.2f);
	}
	
	public static void rename(final Znode node){
		if(node == null){
			Toast.info("Error: node == null: rename",3f);
			return;
		}
		Layout lay = new Layout(Zmdl.ctx());
		final Dialog diag = new Dialog(lay);
		final EditText et = new EditText(Zmdl.ctx(),0.3f,0.05f,0.05f);
		et.setAutoFocus(true);
		et.setText(node.name);
		lay.add(et);
		Layout main4 = Zmdl.lay(true);
		main4.setMarginTop(0.02f);
		Button btnAccept = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.14f,0.045f);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					Command.rename(node,et.getText());
					diag.dismiss();
				}
			});
		main4.add(btnAccept);
		Button btnCancel = new Button(Zmdl.gt("cancel"),Zmdl.gdf(),0.14f,0.045f);
		btnCancel.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					diag.dismiss();
				}
			});
		btnCancel.setMarginLeft(0.01f);
		main4.add(btnCancel);
		main4.setAlignment(Layout.CENTER);
		lay.add(main4);
		diag.setTitle(Zmdl.gt("rename"));
		diag.show(0,0.2f);
	}
	
	private void properties(){
		if(node == null){
			Toast.info("Error: node == null: properties",3f);
			return;
		}
		Layout lay = new Layout(Zmdl.ctx());
		lay.setWidth(0.4f);
		lay.setUseWidthCustom(true);
		final Dialog diag = new Dialog(lay);
		Layout prop = new Layout(Zmdl.ctx());
		ScrollView scroll = new ScrollView(prop,0.3f);
		prop.setToWrapContent();
		ToggleButton multi_uv = new ToggleButton("Multi UV",Zmdl.gdf(),0.2f,0.05f);
		ToggleButton modulateMat = new ToggleButton("Material Modulate",Zmdl.gdf(),0.2f,0.05f);
		multi_uv.setMarginTop(0.01f); multi_uv.setAlignment(Layout.CENTER);
		modulateMat.setMarginTop(0.01f); modulateMat.setAlignment(Layout.CENTER);
		multi_uv.setVisibility(View.GONE);
		modulateMat.setVisibility(View.GONE);
		final TextView tvProps = new TextView(Zmdl.gdf());
		prop.setWidth(0.4f); prop.setUseWidthCustom(true);
		tvProps.setTextSize(0.05f); tvProps.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
		if(Zmdl.inst().type == 1){
			DFFSDK sdk = (DFFSDK)Zmdl.inst().obj;
			final DFFFrame frame = sdk.getFrame(node.frame_idx);
			if(node.isGeometry){
				final DFFGeometry geo = sdk.geom.get(frame.geoAttach);
				multi_uv.setVisibility(View.VISIBLE);
				modulateMat.setVisibility(View.VISIBLE);
				multi_uv.setToggle(geo.uvsets > 1); modulateMat.setToggle(geo.isModulateMaterial());
				multi_uv.setOnToggleListener(new ToggleButton.OnToggleListener(){
						@Override
						public void onToggle(ToggleButton btn, boolean z) {
							geo.changeUVChannel();
							tvProps.setText(getInfo(frame,geo));
							Zmdl.ns();
							
						}
				});
				modulateMat.setOnToggleListener(new ToggleButton.OnToggleListener(){
						@Override
						public void onToggle(ToggleButton btn, boolean z) {
							geo.setModulateMaterial();
							tvProps.setText(getInfo(frame,geo));
							Zmdl.ns();
							
						}
					});
				tvProps.setText(getInfo(frame,geo));
			}else{
				tvProps.setText(getInfo(frame,null));
			}
		}else{
			ZObject o = (ZObject)Zmdl.inst().obj;
			tvProps.setText("Name: "+o.getName()+"\nVertexCount: "+o.getMesh().getVertexInfo().vertexCount+"\nNumMaterials: "+o.getMesh().getParts().list.size()+"\nModel ID: 0x"+Integer.toHexString(o.getID()).toUpperCase());
		}
		prop.add(tvProps);
		lay.add(scroll);
		lay.add(multi_uv);
		lay.add(modulateMat);
		modulateMat.setMarginBottom(0.02f);
		Button btnAccept = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.13f,0.05f);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					diag.dismiss();
				}
			});
		btnAccept.setAlignment(Layout.CENTER);
		lay.add(btnAccept);
		diag.setTitle(Zmdl.gt("properties"));
		diag.show();
	}
	
	private String getInfo(DFFFrame frame,DFFGeometry geo){
		String info = Zmdl.gt("frame_prop",frame.name,frame.rotation,frame.position,frame.parent!=null?frame.parent.name:"none")+"\n";
		if(frame.hanim != null) {
			SkeletonNode skl = Zmdl.inst().skeleton.findByBoneId((short)frame.hanim.boneID);
			info += "\nBone Info\nId: "+skl.boneID+"\nModelMatrix:\n"+skl.modelMatrix+"\nLocalModelMatrix:\n"+frame.getLocalModelMatrix()+"\nInverse Bone Matrix:\n"+skl.InverseBoneMatrix;
		}
		if(node.isGeometry){
			String flag = "";
			if((geo.flags & DFFGeometry.GEOMETRY_FLAG_COLORS) != 0){
				flag += "DFF_VERTEX_COLORS\n";
			}
			if((geo.flags & DFFGeometry.GEOMETRY_FLAG_MULTIPLEUVSETS) != 0){
				flag += "DFF_MULTIUV_SETS\n";
			}
			if((geo.flags & DFFGeometry.GEOMETRY_FLAG_DYNAMIC_LIGHTING) != 0){
				flag += "DFF_DYNAMIC_LIGHTING\n";
			}
			if((geo.flags & DFFGeometry.GEOMETRY_FLAG_NORMALS) != 0){
				flag += "DFF_NORMALS\n";
			}
			if((geo.flags & DFFGeometry.GEOMETRY_FLAG_TRISTRIP) != 0){
				flag += "DFF_TRIANGLE_STRIP\n";
			}
			if((geo.flags & DFFGeometry.GEOMETRY_FLAG_TEXCOORDS) != 0){
				flag += "DFF_TEXTURE_COORDS\n";
			}
			if((geo.flags & DFFGeometry.GEOMETRY_FLAG_POSITIONS) != 0){
				flag += "DFF_VERTICES\n";
			}
			if((geo.flags & DFFGeometry.GEOMETRY_FLAG_MODULATEMATCOLOR) != 0){
				flag += "DFF_MODULATE_MATERIAL\n";
			}
			info += Zmdl.gt("geo_prop",geo.vertexCount,flag,geo.uvsets,geo.frameIdx,geo.materials.size(),geo.splits.size(),geo.skin != null ? "Yes":"No");
			info += "\nModel ID: 0x"+Integer.toHexString(geo.model_id).toUpperCase();
		}
		return info;
	}
	int select_position;
	
	private void showSelectGeometry(final OnSelectListener listen) {
		if(node == null){
			return;
		}
		if(Zmdl.inst().type != 1){
			Toast.error(Zmdl.gt("just_dff"),4f);
			return;
		}
		final ArrayList<GeometryInstance> geoms = Zmdl.im().getModels();
		if(geoms.size() == 0) {
			listen.dismiss();
			Toast.warning(Zmdl.gt("import_ois"),5);
			return;
		}
		select_position = -1;
		Layout lay = Zmdl.lay(0.25f,false);
		ObjectAdapter mn = new ObjectAdapter();
		final Filter filter = new Filter(new Filter.OnFilteringListener(){
				@Override
				public boolean filter(Object item, Object[] args) {
					String s = ((ObjectItem)item).text;
					return s.contains((String)args[0]);
				}
		});
		int geom_ic = Texture.load("zmdl/geom_ic.png");
		for(GeometryInstance g : geoms){
			ObjectItem itm = new ObjectItem(geom_ic,g.name);
			itm.model_hash = g.model_id;
			mn.add(itm);
		}
		mn.setFilter(filter);
		EditText etFilter = new EditText(Zmdl.ctx(),0.24f,0.05f,0.05f);
		etFilter.setHint(Zmdl.gt("search")); etFilter.setMarginBottom(0.02f);
		etFilter.setOnEditTextListener(new EditText.onEditTextListener(){
				@Override
				public void onChange(View view, String text, boolean deleting) {
					filter.filter(false,text);
				}
				@Override
				public void onEnter(View view, String text){}
		});
		lay.add(etFilter);
		ListView lv = new ListView(0.24f,0.24f,mn);
		lv.setApplyAspectRatio(true);
		lv.setOnItemClickListener(new ListView.OnItemClickListener(){
				@Override
				public void onItemClick(ListView view, Object item, short position, boolean longclick) {
					ZObject o = Zmdl.go(((ObjectItem)item).model_hash);
					if(o != null){
						select_position = position;
						Zmdl.svo(o,false);
					} else {
						Toast.error("Error object not founded.",4f);
					}
				}
		});
		lay.add(lv);
		final Dialog diag = new Dialog(lay);
		Button btnAccept = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.14f,0.05f);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(select_position == -1){
						Toast.info(Zmdl.gt("select_geo"),4f);
						return;
					}
					Zmdl.svo(null,true);
					Zmdl.rp().testVisibilityFacts();
					listen.select(geoms.get(select_position).inst,geoms.get(select_position).model_id);
					geoms.clear();
					diag.dismiss();
				}
			});
		btnAccept.setAlignment(Layout.CENTER);
		lay.add(btnAccept);
		diag.setOnDismissListener(new Dialog.OnDimissListener(){
				@Override
				public boolean dismiss() {
					Zmdl.svo(null,true);
					Zmdl.rp().testVisibilityFacts();
					geoms.clear();
					node = null;
					obj_current = null;
					listen.dismiss();
					return true;
				}
		});
		diag.setTitle(Zmdl.gt("select"));
		diag.show(-0.6f,0);
	}
	
	int node_add_mid = -1;
	int node_inst_id = -1;
	
	private void addNode() {
		if(Zmdl.inst().type != 1){
			Toast.error(Zmdl.gt("just_dff"),4f);
			return;
		}
		node_inst_id = -1;
		node_add_mid = -1;
		Layout lay = new Layout(Zmdl.ctx());
		final Dialog diag = new Dialog(lay);
		final EditText etName = new EditText(Zmdl.ctx(),0.3f,0.05f,0.05f);
		etName.setAutoFocus(true);
		etName.setHint(Zmdl.gt("node_name"));
		lay.add(etName);
		final Button btnSelect = new Button(Zmdl.gt("select_geo"),Zmdl.gdf(),0.3f,0.05f);
		btnSelect.setVisibility(Zmdl.im().getInstances().size() > 1 ? View.VISIBLE : View.GONE);
		btnSelect.setAlignment(Layout.CENTER);
		btnSelect.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					diag.hide();
					showSelectGeometry(new OnSelectListener(){
							@Override
							public void dismiss(){
								diag.show();
							}
							@Override
							public void select(ZInstance inst,short model_id) {
								diag.show();
								node_add_mid = model_id;
								node_inst_id = inst.id;
								btnSelect.setText("Model ID: 0x"+Integer.toHexString(Zmdl.go(model_id).getID()).toUpperCase());
							}
						});
				}
		});
		btnSelect.setMarginTop(0.02f);
		lay.add(btnSelect);
		Layout main4 = Zmdl.lay(true);
		main4.setMarginTop(0.02f);
		Button btnAccept = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.14f,0.05f);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(etName.getText().isEmpty()){
						etName.setBackground(255,230,230,false);
						return;
					}
					if(Zmdl.tip().getNodeByName(etName.getText()) != null){
						Toast.error(Zmdl.gt("name_a_e"),5f);
						return;
					}
					addNode(etName.getText());
					diag.dismiss();
				}
			});
		main4.add(btnAccept);
		Button btnCancel = new Button(Zmdl.gt("cancel"),Zmdl.gdf(),0.14f,0.05f);
		btnCancel.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					diag.dismiss();
				}
			});
		btnCancel.setMarginLeft(0.01f);
		main4.add(btnCancel);
		main4.setAlignment(Layout.CENTER);
		lay.add(main4);
		diag.setIcon(Texture.load("zmdl/geom_ic.png"));
		diag.setTitle(Zmdl.gt("add_node"));
		diag.show(0,0.2f);
	}
	
	private void addNode(String name) {
		DFFSDK dff = (DFFSDK)Zmdl.inst().obj;
		DFFFrame dest = dff.getFrame(node.frame_idx);
		DFFFrame frame_new = new DFFFrame();
		frame_new.name = name;
		frame_new.rotation = new Matrix3f();
		frame_new.position = new Vector3f();
		frame_new.flags = 0;
		frame_new.parentIdx = -1;
		frame_new.parent = dest;
		frame_new.invalidateLTM();
		dest.children.add(frame_new);
		if(node_add_mid != -1 && node_inst_id != -1) {
			ZInstance inst_src = Zmdl.im().getById(node_inst_id);
			if(inst_src != null) {
				ZObject o = new ZObject();
				Mesh mesh = new Mesh(true);
				DFFGeometry geo = new DFFGeometry();
				if(inst_src.type == 1){
					DFFSDK src = (DFFSDK)inst_src.obj;
					geo.replace(src.findGeometry((short)node_add_mid),mesh);
					geo.name = frame_new.name;
				} else {
					ZObject src = (ZObject)inst_src.obj;
					geo.flags = (DFFGeometry.GEOMETRY_FLAG_TRISTRIP | DFFGeometry.GEOMETRY_FLAG_MODULATEMATCOLOR);
					geo.fromMesh(src.getMesh());
					mesh.setVertices(geo.vertices);
					if(geo.uvs != null){
						mesh.setTextureCoords(geo.uvs);
					}else{
						mesh.getVertexInfo().removeFlag(VertexInfo.HAS_TEXTURE_COORDINATES);
					}
					if(geo.normals != null){
						mesh.setNormals(geo.normals);
					}else{
						mesh.getVertexInfo().removeFlag(VertexInfo.HAS_NORMALS);
					}
					for(MeshPart p : src.getMesh().getParts().list){
						MeshPart t = new MeshPart(p.index);
						t.material.color.set(p.material.color);
						t.material.textureName = p.material.textureName;
						t.type = p.type;
						mesh.addPart(t);
					}
				}
				o.setMesh(mesh);
				geo.name = name;
				o.setName(name);
				o.setID(Zmdl.im().genID());
				Zmdl.inst().addHash(name,o.getID());
				geo.model_id = o.getID();
				frame_new.model_id = o.getID();
				geo.frameIdx = dff.frameCount;
				frame_new.geoAttach = (short)dff.geometryCount;
				dff.addGeometry(geo);
				DFFAtomic atomic = new DFFAtomic();
				atomic.frameIdx = geo.frameIdx;
				atomic.geoIdx = frame_new.geoAttach;
				atomic.unknown1 = 5;
				atomic.hasRenderToRight = true;
				atomic.RTRval1 = 0x120;
				atomic.RTRval2 = 0;
				dff.addAtomic(atomic);
				o.setTransform(frame_new.getLocalModelMatrix());
				Zmdl.rp().addObject(o);
				Zmdl.app().getTextureManager().update(o.getMesh(),Zmdl.inst().path);
				Zmdl.rp().rewind();
			}else{
				Toast.error("Instance premature closed",4f);
			}
		}
		dff.addFrame(frame_new);
		dff.updateParents(dff.getFrameRoot());
		Zmdl.inst().root = FileProcessor.setTreeNodes(dff,dff.getFrameRoot());
		Zmdl.app().tree_adapter.setTreeNode(Zmdl.inst().root);
		node_inst_id = -1;
		node_add_mid = -1;
	}
	
	@Override
	public void close() {
		if(isShowing()){
			node = null;
			obj_current = null;
			Zmdl.app().panel.dismiss();
		}
	}
	
	private void joinGeometry(DFFSDK dff, DFFFrame src,DFFFrame dest){
		Matrix4f ltm = src.getLocalModelMatrix();
		Matrix4f ltm_dest = dest.getLocalModelMatrix();
		Vector3f pos_src = new Vector3f(),pos_dest = new Vector3f();
		ltm.getLocation(pos_src);
		ltm_dest.getLocation(pos_dest);
		pos_src.subLocal(pos_dest);
		ltm_dest = null;
		pos_dest = null;
		Vector3f tmp = new Vector3f();
		ltm.setLocation(0,0,0);
		DFFGeometry geo_src = dff.geom.get(src.geoAttach);
		DFFGeometry geo_dest = dff.geom.get(dest.geoAttach);
		int newVertexCount = geo_dest.vertexCount + geo_src.vertexCount;
		int offset = 0;
		geo_dest.flags = (DFFGeometry.GEOMETRY_FLAG_TRISTRIP | DFFGeometry.GEOMETRY_FLAG_POSITIONS | DFFGeometry.GEOMETRY_FLAG_MODULATEMATCOLOR);
		float[] vertices = new float[newVertexCount * 3];
		float[] normals = null,texcoords = null;
		if(geo_dest.uvs != null && geo_src.uvs != null){
			texcoords = new float[newVertexCount * 2 * geo_src.uvsets];
			geo_dest.flags |= DFFGeometry.GEOMETRY_FLAG_TEXCOORDS;
		}
		if(geo_dest.normals != null && geo_src.normals != null){
			normals = new float[newVertexCount * 3];
			geo_dest.flags |= DFFGeometry.GEOMETRY_FLAG_NORMALS;
		}
		for(int i = 0;i < geo_dest.vertexCount;i++){
			vertices[i * 3] = geo_dest.vertices[i * 3];
			vertices[i * 3 + 1] = geo_dest.vertices[i * 3 + 1];
			vertices[i * 3 + 2] = geo_dest.vertices[i * 3 + 2];
			if(texcoords != null){
				texcoords[i * 2] = geo_dest.uvs[i * 2];
				texcoords[i * 2 + 1] = geo_dest.uvs[i * 2+1];
			}
			if(normals != null){
				normals[i * 3] = geo_dest.normals[i * 3];
				normals[i * 3 + 1] = geo_dest.normals[i * 3+1];
				normals[i * 3 + 2] = geo_dest.normals[i * 3+2];
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
				normals[offset * 3 + 1] = geo_src.normals[i * 3+1];
				normals[offset * 3 + 2] = geo_src.normals[i * 3+2];
			}
			offset++;
		}
		geo_dest.vertexCount = newVertexCount;
		geo_dest.vertices = vertices;
		if(texcoords != null){
			if((geo_src.flags & DFFGeometry.GEOMETRY_FLAG_MULTIPLEUVSETS) != 0 && geo_src.uvsets > 1){
				for(int i = geo_dest.vertexCount * 2;i < texcoords.length;i++){
					texcoords[i] = 0;
				}
				geo_dest.flags |= DFFGeometry.GEOMETRY_FLAG_MULTIPLEUVSETS;
			}
			geo_dest.uvs = texcoords;
			geo_dest.uvsets = geo_src.uvsets;
		}
		if(normals != null){
			geo_dest.normals = normals;
		}
		final int material_offset = geo_dest.materials.size();
		for(DFFMaterial mat : geo_src.materials){
			geo_dest.materials.add(mat);
		}
		for(DFFIndices idx : geo_src.splits){
			DFFIndices idx0 = new DFFIndices();
			idx0.material = idx.material + material_offset;
			idx0.index = new short[idx.index.length];
			for(int j = 0;j < idx0.index.length;j++){
				idx0.index[j] = (short)(idx.index[j] + init_offset);
			}
			geo_dest.splits.add(idx0);
		}
		ZObject o = Zmdl.go(geo_dest.model_id);
		o.getMesh().setVertices(geo_dest.vertices);
		if(geo_dest.uvs != null){
			o.getMesh().setTextureCoords(geo_dest.uvs);
		}
		if(geo_dest.normals != null){
			o.getMesh().setNormals(geo_dest.normals);
		}
		Zmdl.app().getTextureManager().removeMesh(o.getMesh());
		o.getMesh().getParts().delete();
		for(int j = 0;j < geo_dest.splits.size();j++){
			DFFIndices indx = geo_dest.splits.get(j);
			DFFMaterial mat = geo_dest.materials.get(indx.material);
			MeshPart part = new MeshPart(indx.index);
			part.material.color.set(mat.color);
			part.material.textureName = mat.texture;
			o.getMesh().addPart(part);
		}
		Zmdl.app().getTextureManager().update(o.getMesh(),Zmdl.inst().path);
		geo_src.clear();
		Toast.info("Join successfully",4f);
	}
	
	private interface OnSelectListener {
		void select(ZInstance inst,short model_id);
		void dismiss();
	}
}
