package com.fastsmartsystem.saf.editors;
import com.fastsmartsystem.saf.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.fastsmartsystem.saf.adapters.*;
import com.fastsmartsystem.saf.instance.*;
import com.forcex.gtasdk.*;
import java.util.*;
import com.forcex.gfx3d.*;
import com.forcex.utils.*;
import java.io.*;
import com.forcex.core.gpu.*;
import com.fastsmartsystem.saf.processors.*;
import com.forcex.*;
import com.forcex.app.threading.*;
import com.forcex.core.*;

public class MaterialEditor extends PanelFragment implements GridView.OnItemClickListener, UndoListener {
	Layout main;
	MaterialAdapter mat_adapter;
	ObjectAdapter obj_adapter;
	TextView tv_instance,tv_matinfo;
	ImageView iv_texture;
	ZInstance inst; // Current instance
	
	// exclusivo de dff
	ToggleButton reflex,specular;
	EditText etTexture;
	ListView lvObjects;
	GridView mats;
	Button chooseColor,btnChooseTexture;
	Dialog di_olor_picker;
	
	// Textura
	String texture_temp = "";
	int texture_id = -1;
	Button btnBack;
	Layout layTexture;
	public boolean justObtainMaterials = false;
	int geom_ic = Texture.load(FX.fs.homeDirectory + "zmdl/geom_ic.png");
	int model_hash_current = -1;
	
	public MaterialEditor(){
		main = Zmdl.lay(false);
		tv_instance = new TextView(Zmdl.gdf());
		tv_instance.setTextSize(0.05f);
		tv_instance.setAlignment(Layout.CENTER);
		tv_instance.setMarginBottom(0.01f);
		main.add(tv_instance);
		btnBack = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.24f,0.04f);
		btnBack.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(!lvObjects.isVisible()){
						if(di_olor_picker != null && di_olor_picker.isVisible()){
							di_olor_picker.dismiss();
							di_olor_picker = null;
						}
						if(obj_current != null){
							obj_current.setShowLabel(false);
						}
						if(mat_current != null){
							reset();
						}
						lvObjects.setVisibility(View.VISIBLE);
						mats.setVisibility(View.GONE);
						btnBack.setText(Zmdl.gt("accept"));
						model_hash_current = -1;
					}else if(model_hash_current != -1) {
						if(Zmdl.inst().type == 1){
							Zmdl.svo(null,true);
							Zmdl.rp().testVisibilityFacts();
							setObjectCurrent(model_hash_current);
						}
					}else{
						Toast.warning(Zmdl.gt("select_a_object"),4f);
					}
				}
			});
		main.add(btnBack);
		btnBack.setAlignment(Layout.CENTER);
		obj_adapter = new ObjectAdapter();
		lvObjects = new ListView(0.25f,0.3f,obj_adapter); lvObjects.setId(0x3405);
		lvObjects.setBackgroundColor(0,0,0,0);
		main.add(lvObjects);
		mat_adapter = new MaterialAdapter();
		mats = new GridView(0.25f,0.3f,3,mat_adapter);
		mats.setBackgroundColor(250,250,250,255);
		mats.setOnItemClickListener(this);
		main.add(mats);
		lvObjects.setBackgroundColor(250,250,250,255);
		lvObjects.setOnItemClickListener(new ListView.OnItemClickListener(){
				@Override
				public void onItemClick(ListView view, Object item, short position, boolean longclick) {
					model_hash_current = ((ObjectItem)item).model_hash;
					ZObject o = Zmdl.go(model_hash_current);
					if(o != null){
						Zmdl.svo(o,false);
						o = null;
					} else {
						model_hash_current = -1;
						Toast.error("Error object not founded.",4f);
					}
				}
		});
		Layout main2 = Zmdl.lay(true);
		iv_texture = new ImageView(-1,0.1f,0.1f);
		iv_texture.setApplyAspectRatio(true);
		iv_texture.setMarginLeft(0.01f);
		iv_texture.setMarginRight(0.01f);
		main2.add(iv_texture);
		tv_matinfo = new TextView(Zmdl.gdf());
		tv_matinfo.setTextSize(0.038f);
		tv_matinfo.setConstraintWidth(0.13f);
		tv_matinfo.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
		main2.add(tv_matinfo);
		main2.setMarginTop(0.02f);
		etTexture = new EditText(Zmdl.ctx(),0.2f,0.05f,0.05f);
		etTexture.setHint(Zmdl.gt("texture"));
		etTexture.setMarginBottom(0.01f);
		etTexture.setMarginTop(0.01f);
		Layout main3 = Zmdl.lay(true);
		Layout main4 = Zmdl.lay(false);
		chooseColor = new Button(Zmdl.gt("choose_color"),Zmdl.gdf(),0.13f,0.05f);
		chooseColor.setOnClickListener(new Button.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(di_olor_picker != null && di_olor_picker.isVisible()){
						return;
					}
					showColorPicker();
				}
		});
		main4.setMarginLeft(0.01f);
		main.add(main2);
		layTexture = Zmdl.lay(true);
		layTexture.add(etTexture);
		btnChooseTexture = new Button(0.03f,0.03f);
		btnChooseTexture.setApplyAspectRatio(true);
		btnChooseTexture.setIconTexture(Texture.load(FX.fs.homeDirectory+"zmdl/texture.png"));
		layTexture.add(btnChooseTexture);
		layTexture.setAlignment(Layout.CENTER);
		main.add(layTexture);
		main3.add(chooseColor);
		main3.add(main4);
		reflex = new ToggleButton(Zmdl.gt("refleccion"),Zmdl.gdf(),0.11f,0.04f);
		main4.add(reflex);
		reflex.setMarginBottom(0.01f);
		specular = new ToggleButton(Zmdl.gt("specular"),Zmdl.gdf(),0.11f,0.04f);
		specular.setMarginBottom(0.01f);
		main4.add(specular);
		main.add(main3);
		Button btnOk = new Button(Zmdl.gt("close"),Zmdl.gdf(),0.11f,0.045f);
		btnOk.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(di_olor_picker != null && di_olor_picker.isVisible()){
						di_olor_picker.dismiss();
						di_olor_picker = null;
					}
					Zmdl.ep().requestShow();
					if(obj_current != null){
						obj_current.setShowLabel(false);
						obj_current.restoreAlpha();
						if(texture_id != -1){
							if(texture_changed){
								Texture.remove(texture_id);
							}
							texture_id = -1;
							texture_temp = "";
						}
					}
				}
		});
		main4.add(btnOk);
		specular.setVisibility(View.INVISIBLE);
		reflex.setVisibility(View.INVISIBLE);
		layTexture.setVisibility(View.INVISIBLE);
		Zmdl.um().addListener(this,2);
	}
	
	public void setObjectCurrent(int model_hash){
		btnBack.setText(Zmdl.gt("back"));
		DFFSDK dff = (DFFSDK)inst.obj;
		DFFGeometry geo = dff.findGeometry((short)model_hash);
		mat_adapter.removeAll();
		ArrayList<MeshPart> parts = Zmdl.go(geo.model_id).getMesh().getParts().list;
		byte i = 0;
		for(DFFMaterial m : geo.materials){
			MaterialItem itm = new MaterialItem();
			itm.name = "Material."+i;
			itm.material_index = i;
			itm.color = m.color;
			itm.geo_index = (byte)dff.geom.indexOf(geo);
			itm.model_keyhash = model_hash;
			int part = getSplitMaterial(geo,itm.material_index);
			if(part == -1){
				Toast.info("Fatal Error: material unused",4);
				return;
			}
			itm.snap_material = Zmdl.rp().snapMaterial(itm.color,parts.get(part).material.specular,parts.get(part).material.diffuseTexture);
			mat_adapter.add(itm);
			i++;
		}
		mats.setVisibility(View.VISIBLE);
		lvObjects.setVisibility(View.GONE);
		Zmdl.go(geo.model_id).setShowLabel(true);
	}
	
	public void requestShow(){
		if(!Zmdl.im().hasCurrentInstance() || Zmdl.tlay(main)){
			return;
		}
		if(Zmdl.hs()){
			Zmdl.gos().selected = false;
		}
		if(obj_current != null) {
			if(mat_current != null) {
				iv_texture.setTexture(-1);
				iv_texture.setMixColor(mat_current.color);
			}
			obj_current.setShowLabel(true);
			chooseColor.setVisibility(View.VISIBLE);
		}else{
			chooseColor.setVisibility(View.INVISIBLE);
		}
		inst = Zmdl.inst();
		tv_instance.setText(Zmdl.gt("material_editor")+":\n"+inst.name);
		mat_adapter.clean();
		mat_adapter.removeAll();
		obj_adapter.removeAll();
		if(inst.type == 1){
			lvObjects.setVisibility(View.VISIBLE);
			mats.setVisibility(View.GONE);
			btnBack.setVisibility(View.VISIBLE);
			btnBack.setText(Zmdl.gt("accept"));
			DFFSDK dff = (DFFSDK)inst.obj;
			for(DFFGeometry geo : dff.geom){
				ObjectItem o = new ObjectItem(geom_ic,geo.name);
				o.model_hash = geo.model_id;
				obj_adapter.add(o);
			}
			if(!justObtainMaterials){
				Zmdl.app().tip("snap_select",lvObjects,2);
			}
			justObtainMaterials = false;
			dff = null;
		}else if(inst.type == 2 || inst.type == 3){
			lvObjects.setVisibility(View.GONE);
			mats.setVisibility(View.VISIBLE);
			btnBack.setVisibility(View.GONE);
			ZObject model = (ZObject)inst.obj;
			ArrayList<MeshPart> parts = model.getMesh().getParts().list;
			for(byte i = 0;i < parts.size();i++){
				MaterialItem itm = new MaterialItem();
				itm.name = "Material."+i;
				itm.color = parts.get(i).material.color;
				itm.material_index = i;
				itm.model_keyhash = model.getID();
				itm.snap_material = Zmdl.rp().snapMaterial(itm.color,0.4f,parts.get(i).material.diffuseTexture);
				mat_adapter.add(itm);
			}
			specular.setVisibility(View.INVISIBLE);
			reflex.setVisibility(View.INVISIBLE);
		}
		Zmdl.apl(main);
	}
	
	MaterialItem mat_current;
	ZObject obj_current;
	boolean texture_changed = false;
	
	@Override
	public void onItemClick(Object item, short position, boolean longclick) {
		mat_current = (MaterialItem)item;
		if(longclick){
			deleteMaterial(position);
		}else{
			if(texture_id != -1){
				texture_id = -1;
				texture_temp = "";
			}
			setMaterial();
		}
	}
	
	private void deleteMaterial(final short position){
		Layout lay = new Layout(Zmdl.ctx());
		lay.setWidth(0.3f);
		lay.setUseWidthCustom(true);
		final Dialog diag = new Dialog(lay);
		TextView tvInfo = new TextView(Zmdl.gdf());
		tvInfo.setText(Zmdl.gt("delete_material")+" '"+mat_current.name+"'?");
		tvInfo.setTextSize(0.05f);
		lay.add(tvInfo);
		Layout main4 = Zmdl.lay(true);
		main4.setMarginTop(0.02f);
		Button btnAccept = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.1f,0.04f);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					ZObject obj = Zmdl.rp().getObject(mat_current.model_keyhash);
					if(obj == null){
						Toast.info("Error: obj_current was deleted!!",3f);
						return;
					}
					obj.restoreAlpha();
					if(Zmdl.inst().type == 1){
						DFFSDK dff = ((DFFSDK)Zmdl.inst().obj);
						DFFGeometry geo = dff.geom.get(mat_current.geo_index);
						if(position >= geo.materials.size()){
							Toast.info("Error: material was deleted!",3f);
							mat_current = null;
							return;
						}
						geo.materials.remove(position);
						int index = getSplitMaterial(geo,position);
						if(index == -1) {
							Toast.info("Fatal Error: material unused",4);
							return;
						}else{
							for(DFFIndices i : geo.splits) {
								if(i.material > position){
									i.material--;
								}
							}
							geo.splits.remove(index);
							obj.getMesh().getParts().list.remove(index);
						}
						for(short i = 0;i < mat_adapter.getNumItems();i++){
							if(i > position){
								mat_adapter.getItem(i).material_index--;
							}
						}
						mat_adapter.remove(position);
						Toast.info(Zmdl.gt("deleted"),4f);
					}else{
						obj.getMesh().getParts().list.remove(mat_current.material_index);
					}
					mat_current = null;
					obj = null;
					diag.dismiss();
					tv_matinfo.setText("");
					iv_texture.setMixColor(255,255,255);
					iv_texture.setTexture(-1);
					chooseColor.setVisibility(View.INVISIBLE);
					obj_current = null;
					reflex.setVisibility(View.INVISIBLE);
					specular.setVisibility(View.INVISIBLE);
					layTexture.setVisibility(View.INVISIBLE);
				}
			});
		main4.add(btnAccept);
		Button btnCancel = new Button(Zmdl.gt("cancel"),Zmdl.gdf(),0.1f,0.04f);
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
		diag.show();
	}
	
	public void setCurrentMaterial(int index){
		if(index ==-1){
			return;
		}
		mat_current = mat_adapter.getItem((short)index);
		setMaterial();
	}
	
	private void setMaterial() {
		obj_current = Zmdl.go(mat_current.model_keyhash);
		if(obj_current == null){
			Toast.error("Error: Object premature remove",4f);
			return;
		}
		obj_current.saveAlpha();
		chooseColor.setVisibility(View.VISIBLE);
		if(di_olor_picker != null && di_olor_picker.isVisible()){
			di_olor_picker.dismiss();
			di_olor_picker = null;
			showColorPicker();
		}
		if(inst.type == 1){
			specular.setVisibility(View.VISIBLE);
			reflex.setVisibility(View.VISIBLE);
			DFFSDK dff = (DFFSDK)inst.obj;
			if(mat_current.geo_index >= dff.geometryCount){
				Toast.error("Error Geometry Index",3f);
				return;
			}
			final DFFGeometry geo = dff.geom.get(mat_current.geo_index);
			if(geo == null){
				Toast.error("Error Material",3f);
				return;
			}
			final DFFMaterial mat = geo.materials.get(mat_current.material_index);
			obj_current.setUseAlpha(getSplitMaterial(geo,mat_current.material_index),70);
			tv_matinfo.setText(Zmdl.gt("material_info")+":\n"+Zmdl.gt("geometry")+":\n"+geo.name+"\nIndex:"+mat_current.material_index);
			int part = getSplitMaterial(geo,mat_current.material_index);
			if(part == -1){
				Toast.info("Fatal Error: material unused",4);
				return;
			}
			final Material obj_mat = obj_current.getMesh().getPart(part).material;
			iv_texture.setTexture(mat_current.snap_material);
			iv_texture.setFrameBufferTexture(true);
			iv_texture.setMixColor(new Color(Color.WHITE));
			reflex.setToggle(mat.hasReflectionMat);
			specular.setToggle(mat.hasSpecularMat);
			if(mat.hasTexture() || geo.uvs != null){
				etTexture.setText(mat.texture);
				texture_temp = mat.texture;
				texture_id = obj_mat.diffuseTexture;
				layTexture.setVisibility(View.VISIBLE);
				btnChooseTexture.setOnClickListener(new View.OnClickListener(){
						@Override
						public void OnClick(View view) {
							Zmdl.app().getTextureManager().showDriver(new TextureManager.OnResultListener(){
									@Override
									public void select(boolean success, TextureManager.TextureInfo tex) {
										if(success) {
											mat.texture = tex.name;
											obj_mat.diffuseTexture = tex.glId;
											etTexture.setText(tex.name);
											Texture.remove(mat_current.snap_material);
											mat_current.snap_material = Zmdl.rp().snapMaterial(obj_mat.color,obj_mat.specular,obj_mat.diffuseTexture);
											iv_texture.setTexture(mat_current.snap_material);
											Zmdl.ns();
										} else {
											Toast.info(Zmdl.gt("select_atex"),4f);
										}
									}
								});
						}
					});
				etTexture.setOnEditTextListener(new EditText.onEditTextListener(){
						@Override
						public void onChange(View view, String text, boolean deleting) {
							registNewUndoPointer();
							if(Zmdl.app().getTextureManager().exist(text)){
								mat.texture = text;
								int texture = Zmdl.app().getTextureManager().get(text);
								obj_mat.diffuseTexture = texture;
							} else if(new File(inst.path + "/" + text + ".png").exists()) {
								mat.texture = text;
								String path = inst.path + "/" + mat.texture + ".png";
								Image img = new Image(path);
								int texture_new = Texture.load(img.width,img.height,img.getBuffer(),false);
								Zmdl.app().getTextureManager().add(text,path,texture_new,img);
								obj_mat.diffuseTexture = texture_new;
							} else {
								mat.texture = texture_temp;
								obj_mat.diffuseTexture = texture_id;
							}
							Texture.remove(mat_current.snap_material);
							mat_current.snap_material = Zmdl.rp().snapMaterial(obj_mat.color,obj_mat.specular,obj_mat.diffuseTexture);
							iv_texture.setTexture(mat_current.snap_material);
							Zmdl.ns();
						}
						@Override
						public void onEnter(View view, String text) {
							
						}
				});
			}else{
				layTexture.setVisibility(View.INVISIBLE);
				etTexture.setOnEditTextListener(null);
			}
			reflex.setOnToggleListener(new ToggleButton.OnToggleListener(){
					@Override
					public void onToggle(ToggleButton btn, boolean z) {
						registNewUndoPointer();
						if(mat.reflectionAmount == null){
							mat.reflectionAmount = new float[]{1.0f,1.0f,1.0f,1.0f};
						}
						mat.hasReflectionMat = z;
						mat.reflectionIntensity = 1.5f;
						Zmdl.ns();
					}
			});
			specular.setOnToggleListener(new ToggleButton.OnToggleListener(){
					@Override
					public void onToggle(ToggleButton btn, boolean z) {
						registNewUndoPointer();
						mat.hasSpecularMat = z;
						mat.specular_level = 0.8f;
						mat.specular_name = "";
						Zmdl.ns();
						obj_mat.diffuse = z ? 0.5f : 0.8f;
						obj_mat.specular = z ? 1.0f : 0.1f;
						
						iv_texture.setTexture(mat_current.snap_material);
					}
				}); 
		}else if(inst.type == 2 || inst.type == 3){
			final Material obj_mat = obj_current.getMesh().getPart(mat_current.material_index).material;
			obj_current.setUseAlpha(mat_current.material_index,90);
			tv_matinfo.setText(Zmdl.gt("material_info")+":\n"+Zmdl.gt("geometry")+":\n"+obj_current.getName()+"\nIndex:"+mat_current.material_index);
			iv_texture.setTexture(mat_current.snap_material);
			iv_texture.setFrameBufferTexture(true);
			if(obj_current.getMesh().getVertexData().uvs != null){
				etTexture.setText(obj_current.getMesh().getPart(mat_current.material_index).material.textureName);
				texture_temp = obj_mat.textureName;
				texture_id = obj_mat.diffuseTexture;
				layTexture.setVisibility(View.VISIBLE);
				btnChooseTexture.setOnClickListener(new View.OnClickListener(){
						@Override
						public void OnClick(View view) {
							Zmdl.app().getTextureManager().showDriver(new TextureManager.OnResultListener(){
									@Override
									public void select(boolean success, TextureManager.TextureInfo tex) {
										if(success) {
											obj_mat.textureName = tex.name;
											obj_mat.diffuseTexture = tex.glId;
											etTexture.setText(tex.name);
											Texture.remove(mat_current.snap_material);
											mat_current.snap_material = Zmdl.rp().snapMaterial(obj_mat.color,obj_mat.specular,obj_mat.diffuseTexture);
											iv_texture.setTexture(mat_current.snap_material);
											Zmdl.ns();
										} else {
											Toast.info(Zmdl.gt("select_atex"),4f);
										}
									}
								});
						}
					});
				etTexture.setOnEditTextListener(new EditText.onEditTextListener(){
						@Override
						public void onChange(View view, String text, boolean deleting) {
							if(Zmdl.app().getTextureManager().exist(text)){
								obj_mat.textureName = text;
								int texture = Zmdl.app().getTextureManager().get(text);
								obj_mat.diffuseTexture = texture;
							} else if(new File(inst.path + "/" + text + ".png").exists()) {
								obj_mat.textureName = text;
								String path = inst.path + "/" + text + ".png";
								Image img = new Image(path);
								int texture_new = Texture.load(img.width,img.height,img.getBuffer(),false);
								Zmdl.app().getTextureManager().add(text,path,texture_new,img);
								obj_mat.diffuseTexture = texture_new;
							} else {
								obj_mat.textureName = text;
								obj_mat.diffuseTexture = texture_id;
							}
							Texture.remove(mat_current.snap_material);
							mat_current.snap_material = Zmdl.rp().snapMaterial(obj_mat.color,obj_mat.specular,obj_mat.diffuseTexture);
							iv_texture.setTexture(mat_current.snap_material);
							Zmdl.ns();
						}
						@Override
						public void onEnter(View view, String text) {

						}
					});
			}else{
				layTexture.setVisibility(View.INVISIBLE);
				etTexture.setOnEditTextListener(null);
			}
		}
	}
	
	private void showColorPicker(){
		Zmdl.ns();
		Layout pickers = new Layout(Zmdl.ctx());
		final EditText etHexColor = new EditText(Zmdl.ctx(),0.2f,0.05f,0.05f);
		etHexColor.setAlignment(Layout.CENTER);
		etHexColor.setText("#"+mat_current.color.toHex().toUpperCase());
		etHexColor.setMarginTop(0.01f);
		pickers.add(etHexColor);
		final ColorPicker picker = new ColorPicker(0.3f,0.25f);
		picker.setColor(mat_current.color);
		picker.setMarginTop(0.01f);
		picker.setOnColorPickListener(new ColorPicker.OnColorPickListener(){
				@Override
				public void pick(int color){
					registNewUndoPointer();
					mat_current.color.set(color);
					etHexColor.setText("#"+mat_current.color.toHex().toUpperCase());
					Texture.remove(mat_current.snap_material);
					if(inst.type == 1){
						DFFGeometry geo = ((DFFSDK)inst.obj).geom.get(mat_current.geo_index);
						geo.materials.get(mat_current.material_index).color.set(color);
						int part = getSplitMaterial(geo,mat_current.material_index);
						if(part == -1){
							Toast.info("Fatal Error: material unused",4);
							return;
						}else{
							Material mat = obj_current.getMesh().getPart(part).material;
							mat.color.set(mat_current.color);
							mat_current.snap_material = Zmdl.rp().snapMaterial(mat.color,mat.specular,mat.diffuseTexture);
							iv_texture.setTexture(mat_current.snap_material);
						}
					}else{
						Material mat = obj_current.getMesh().getPart(mat_current.material_index).material;
						mat.color.set(mat_current.color);
						mat_current.snap_material = Zmdl.rp().snapMaterial(mat.color,mat.specular,mat.diffuseTexture);
						iv_texture.setTexture(mat_current.snap_material);
					}
				}
			});
		pickers.add(picker);
		etHexColor.setOnEditTextListener(new EditText.onEditTextListener(){
				@Override
				public void onChange(View view, String text, boolean deleting) {
					Color color = new Color();
					etHexColor.setEdgeMultiColor(210,25,25,245,20,20);
					if(text.isEmpty() || !text.startsWith("#")){
						if(!text.startsWith("#")) {
							Toast.error(Zmdl.gt("color_code_error"),4f);
						}
						return;
					} else {
						 if(text.length() == 9) {
							try{
								color.a = (short)Integer.parseInt(text.substring(1,3),16);
								color.set(
									Integer.parseInt(text.substring(3,5),16),
									Integer.parseInt(text.substring(5,7),16),
									Integer.parseInt(text.substring(7,9),16)
									);
								etHexColor.setEdgeMultiColor(7,128,255,220,220,220);
							}catch(Exception e){
								return;
							}
						} else {
							return;
						}
					}
					registNewUndoPointer();
					mat_current.color.set(color);
					picker.setColor(color);
					Texture.remove(mat_current.snap_material);
					if(inst.type == 1){
						DFFGeometry geo = ((DFFSDK)inst.obj).geom.get(mat_current.geo_index);
						geo.materials.get(mat_current.material_index).color.set(color);
						int part = getSplitMaterial(geo,mat_current.material_index);
						if(part == -1){
							Toast.info("Fatal Error: material unused",4);
							return;
						}else{
							Material mat = obj_current.getMesh().getPart(part).material;
							mat.color.set(mat_current.color);
							mat_current.snap_material = Zmdl.rp().snapMaterial(mat.color,mat.specular,mat.diffuseTexture);
							iv_texture.setTexture(mat_current.snap_material);
						}
					}else{
						Material mat = obj_current.getMesh().getPart(mat_current.material_index).material;
						mat.color.set(mat_current.color);
						mat_current.snap_material = Zmdl.rp().snapMaterial(mat.color,mat.specular,mat.diffuseTexture);
						iv_texture.setTexture(mat_current.snap_material);
					}
					
				}
				
				@Override
				public void onEnter(View view, String text) {
					// TODO: Implement this method
				}
		});
		TextView tvOtherColors = new TextView(Zmdl.gdf());
		tvOtherColors.setText(Zmdl.gt("other_colors"));
		tvOtherColors.setTextSize(0.045f); tvOtherColors.setAlignment(Layout.CENTER);
		pickers.add(tvOtherColors);
		ColorMaterialAdapter adapter = new ColorMaterialAdapter();
		GridView colors = new GridView(0.3f,0.2f,5,adapter);
		colors.setOnItemClickListener(new GridView.OnItemClickListener(){
				@Override
				public void onItemClick(Object item, short position, boolean longclick)
				{
					registNewUndoPointer();
					Color itm = (Color)item;
					mat_current.color.set(itm.r,itm.g,itm.b);
					etHexColor.setText("0x"+mat_current.color.toHex().toUpperCase());
					Texture.remove(mat_current.snap_material);
					if(inst.type == 1){
						DFFGeometry geo = ((DFFSDK)inst.obj).geom.get(mat_current.geo_index);
						int part = getSplitMaterial(geo,mat_current.material_index);
						if(part == -1){
							Toast.info("Fatal Error: material unused",4);
							return;
						}
						Material mat = obj_current.getMesh().getPart(part).material;
						mat.color.set(mat_current.color);
						mat_current.snap_material = Zmdl.rp().snapMaterial(mat.color,mat.specular,mat.diffuseTexture);
						iv_texture.setTexture(mat_current.snap_material);
						geo.materials.get(mat_current.material_index).color.set(mat_current.color);
					}else{
						Material mat = obj_current.getMesh().getPart(mat_current.material_index).material;
						mat.color.set(mat_current.color);
						mat_current.snap_material = Zmdl.rp().snapMaterial(mat.color,mat.specular,mat.diffuseTexture);
						iv_texture.setTexture(mat_current.snap_material);
					}
					picker.setColor(mat_current.color);
				}
			});
		pickers.add(colors);
		Button btnOk = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.075f,0.038f);
		btnOk.setAlignment(Layout.CENTER); btnOk.setMarginTop(0.02f);
		btnOk.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					di_olor_picker.dismiss();
					di_olor_picker = null;
				}
			});
		pickers.add(btnOk);
		di_olor_picker = new Dialog(pickers);
		di_olor_picker.setIcon(Texture.load(FX.fs.homeDirectory+"zmdl/material.png"));
		di_olor_picker.setTitle("Color Picker");
		di_olor_picker.show();
	}
	
	@Override
	public boolean isShowing() {
		return Zmdl.tlay(main);
	}

	@Override
	public void close() {
		if(isShowing()){
			Zmdl.app().panel.dismiss();
		}
	}
	
	private class ColorMaterialAdapter extends GridAdapter {
		ImageView iv;
		
		@Override
		protected void createView(Layout container) {
			iv = new ImageView(-1,0.055f,0.055f); iv.setMarginTop(0.01f);
			iv.setApplyAspectRatio(true); 
			iv.setAlignment(Layout.CENTER);
			container.setUseWidthCustom(true);
			container.add(iv);
		}

		@Override
		protected void updateView(Object item, short position, Layout container) {
			iv.setMixColor(((Color)item));
		}
		
		public ColorMaterialAdapter(){
			super(Zmdl.ctx());
			for(int bright = 255;bright >= 0;bright -= 51){
				add(new Color(bright,bright,bright));
			}
			for(int bright = 51;bright <= 255;bright += 51){
				add(new Color(bright,0,0));
				add(new Color(0,bright,0));
				add(new Color(0,0,bright));
			}
			add(new Color(255,255,0));
			add(new Color(0,255,255));
			add(new Color(255,0,255));
		}
	}
	
	public void reset(){
		if(obj_current != null) {
			obj_current.restoreAlpha();
			obj_current.show_label = false;
		}
		model_hash_current = -1;
		mat_current = null;
		tv_matinfo.setText("");
		iv_texture.setMixColor(255,255,255);
		iv_texture.setTexture(-1);
		chooseColor.setVisibility(View.INVISIBLE);
		obj_current = null;
		reflex.setVisibility(View.INVISIBLE);
		specular.setVisibility(View.INVISIBLE);
		layTexture.setVisibility(View.INVISIBLE);
	}

	@Override
	public void undo(Object data) {
		if(inst.type == 1){
			MaterialUndo und = (MaterialUndo)data;
			DFFGeometry geo = ((DFFSDK)inst.obj).geom.get(und.geom_index);
			DFFMaterial pointer = geo.materials.get(und.mat_index);
			pointer.color = und.dff_mat.color;
			pointer.hasMaterialEffect = und.dff_mat.hasMaterialEffect;
			pointer.hasReflectionMat = und.dff_mat.hasReflectionMat;
			pointer.hasSpecularMat = und.dff_mat.hasSpecularMat;
			pointer.specular_name = und.dff_mat.specular_name;
			pointer.reflectionIntensity = und.dff_mat.reflectionIntensity;
			ZObject temp = Zmdl.go(und.model_hash);
			int part = getSplitMaterial(geo,und.mat_index);
			if(part == -1){
				Toast.info("Fatal Error: material unused",4);
				return;
			}else{
				temp.getMesh().getPart(part).material.color.set(und.dff_mat.color);
			}
			temp = null;
			und = null;
		}
	}
	
	private void registNewUndoPointer(){
		if(inst.type != 1 || mat_current == null){
			return;
		}
		ArrayList<Object> last_data = Zmdl.um().getDataFromType(2);
		for(Object o : last_data){
			MaterialUndo u = (MaterialUndo)o;
			if(u.mat_index == mat_current.material_index){
				return;
			}
		}
		MaterialUndo u = new MaterialUndo();
		DFFMaterial pointer = new DFFMaterial();
		DFFMaterial dff_mat = ((DFFSDK)inst.obj).geom.get(mat_current.geo_index).materials.get(mat_current.material_index);
		pointer.color = new Color(dff_mat.color);
		pointer.hasMaterialEffect = dff_mat.hasMaterialEffect;
		pointer.hasReflectionMat = dff_mat.hasReflectionMat;
		pointer.hasSpecularMat = dff_mat.hasSpecularMat;
		pointer.specular_name = dff_mat.specular_name;
		pointer.reflectionIntensity = dff_mat.reflectionIntensity;
		u.dff_mat = pointer;
		u.geom_index = mat_current.geo_index;
		u.mat_index = mat_current.material_index;
		u.model_hash = mat_current.model_keyhash;
		Zmdl.um().addUndoData(u,2,"Material");
	}
	
	private int getSplitMaterial(DFFGeometry geo,int mat_idx){
		int offset = 0;
		for(DFFIndices idx : geo.splits){
			if(idx.material == mat_idx){
				return offset;
			}
			offset++;
		}
		return -1;
	}
	
	private class MaterialUndo{
		byte geom_index;
		byte mat_index;
		int model_hash;
		DFFMaterial dff_mat;
	}
} 
