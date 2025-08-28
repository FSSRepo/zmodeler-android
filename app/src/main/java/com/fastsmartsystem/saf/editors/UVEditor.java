package com.fastsmartsystem.saf.editors;
import com.fastsmartsystem.saf.*;
import com.fastsmartsystem.saf.adapters.*;
import com.fastsmartsystem.saf.instance.*;
import com.fastsmartsystem.saf.processors.*;
import com.forcex.core.*;
import com.forcex.gfx3d.shapes.*;
import com.forcex.gtasdk.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.forcex.*;
import com.forcex.gfx3d.*;

public class UVEditor extends PanelFragment implements View.OnClickListener,ToggleButton.OnToggleListener
{
	Layout main,main1,main5,main6;
	TextView tv_instance;
	TextView tv_texture;
	ImageView iv_texture;
	ZInstance inst; // Current instance
	ListView lvSplit;
	TextAdapter splits;
	ZObject obj_current;
	int split_selected = -1;
	UVMapView uv_view;
	String texture_name;
	DFFSDK dff;
	DFFGeometry geometry;
	Button btnReset,btnFullScreen;
	Dialog fs_diag;
	byte[] vertex_color;
	ToggleButton translate, rotate,scale, x_axis, y_axis;
	
	public UVEditor() {
		main = Zmdl.lay(0.25f,false);
		tv_instance = new TextView(Zmdl.gdf());
		tv_instance.setTextSize(0.05f);
		tv_instance.setAlignment(Layout.CENTER);
		tv_instance.setMarginBottom(0.01f);
		main.add(tv_instance);
		createFullScreenControl();
		main1 = Zmdl.lay(true);
		tv_texture = new TextView(Zmdl.gdf());
		tv_texture.setTextSize(0.04f); 
		tv_texture.setConstraintWidth(0.115f);
		tv_texture.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
		tv_texture.setText("No texture");
		iv_texture = new ImageView(-1,0.12f,0.12f);
		iv_texture.setApplyAspectRatio(true);
		iv_texture.setMarginLeft(0.01f);
		main1.add(iv_texture);
		Layout main2 = Zmdl.lay(false);
		main2.setMarginLeft(0.01f);
		main2.add(tv_texture);
		final ToggleButton unselect = new ToggleButton(Zmdl.gt("unselect"), Zmdl.gdf(), 0.115f, 0.045f);
		unselect.setMarginTop(0.01f);
		unselect.setVisibility(View.GONE);
		final ToggleButton move = new ToggleButton(Zmdl.gt("move"), Zmdl.gdf(), 0.115f, 0.045f);
		main2.add(move);
		final ToggleButton zoom = new ToggleButton(Zmdl.gt("zoom"), Zmdl.gdf(), 0.115f, 0.045f);
		zoom.setMarginTop(0.01f);
		main2.add(zoom);
		final ToggleButton select = new ToggleButton(Zmdl.gt("select"), Zmdl.gdf(), 0.115f, 0.045f);
		move.setMarginTop(0.01f);
		select.setMarginTop(0.01f);
		move.setOnToggleListener(new ToggleButton.OnToggleListener(){
				@Override
				public void onToggle(ToggleButton btn, boolean z) {
					select.setToggle(false);
					zoom.setToggle(false);
					unselect.setVisibility(View.GONE);
					uv_view.setMode(z ? 1 : 0);
				}
			});
		zoom.setOnToggleListener(new ToggleButton.OnToggleListener(){
				@Override
				public void onToggle(ToggleButton btn, boolean z) {
					select.setToggle(false);
					move.setToggle(false);
					unselect.setVisibility(View.GONE);
					uv_view.setMode(z ? 2 : 0);
				}
			});
		select.setOnToggleListener(new ToggleButton.OnToggleListener(){
				@Override
				public void onToggle(ToggleButton btn, boolean z) {
					move.setToggle(false);
					zoom.setToggle(false);
					unselect.setToggle(false);
					unselect.setVisibility(z ? View.VISIBLE : View.GONE);
					uv_view.setMode(z ? 3 : 0);
					uv_view.setUnselecting(false);
					Zmdl.rp().obj_selector = z ? obj_current.object_wire : null;
					update();
				}
			});
		main2.add(select);
		unselect.setOnToggleListener(new ToggleButton.OnToggleListener(){
				@Override
				public void onToggle(ToggleButton btn, boolean z) {
					unselect.setVisibility(select.isToggled() ? View.VISIBLE : View.GONE);
					uv_view.setUnselecting(z);
				}
		});
		main2.add(unselect);
		main1.add(main2);
		main.add(main1);
		Layout main3 = Zmdl.lay(true);
		main3.setMarginTop(0.02f);
		btnReset = new Button(Zmdl.gt("reset"),Zmdl.gdf(),0.12f,0.04f);
		btnReset.setVisibility(View.GONE);
		btnReset.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					uv_view.reset();
					/*
					Zmdl.adtsk(new Task(){
							@Override
							public boolean execute()
							{
								Zmdl.app().getProgressScreen().show();
								ArrayList<Island> islands = UVProcessor.getIslands(obj_current.getMesh().getPart(split_selected).index);
								Logger.log(islands.size()+"");
								for(Island i : islands){
									Logger.log(i.toString());
								}
								Zmdl.app().getProgressScreen().dismiss();

								return true;
							}
						});*/
				}
			});
		main3.setAlignment(Layout.CENTER);
		main3.add(btnReset);
		btnFullScreen = new Button(Zmdl.gt("fullscreen"),Zmdl.gdf(),0.12f,0.04f);
		btnFullScreen.setMarginLeft(0.01f);
		btnFullScreen.setVisibility(View.GONE);
		btnFullScreen.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					uv_view.setFullScreen(true);
					zoom.setToggle(false);
					select.setToggle(false);
					move.setToggle(false);
					unselect.setVisibility(View.GONE);
					uv_view.setUnselecting(false);
					fs_diag.show(-0.8f,-0.6f);
				}
			});
		main3.add(btnFullScreen);
		main1.setVisibility(View.GONE);
		main5 = Zmdl.lay(true);
		main5.setAlignment(Layout.CENTER);
		translate = new ToggleButton(Zmdl.gt("translate"), Zmdl.gdf(), 0.08f, 0.045f);
		translate.setOnToggleListener(this);
		main5.add(translate);
		rotate = new ToggleButton(Zmdl.gt("rotate"), Zmdl.gdf(), 0.08f, 0.045f);
		rotate.setOnToggleListener(this);
		main5.add(rotate);
		scale = new ToggleButton(Zmdl.gt("scale"), Zmdl.gdf(), 0.08f, 0.045f);
		scale.setOnToggleListener(this);
		main5.add(scale);
		main5.setMarginTop(0.02f);
		main6 = Zmdl.lay(true);
		main6.setMarginTop(0.02f);
		main6.setOrientation(Layout.HORIZONTAL);
		main6.setAlignment(Layout.CENTER);
		x_axis = new ToggleButton("X", Zmdl.gdf(), 0.05f, 0.045f);
		x_axis.setId(0x356); x_axis.setBackgroundColor(255,0,0);
		x_axis.setOnToggleListener(this);
		main6.add(x_axis);
		x_axis.setMarginRight(0.01f);
		y_axis = new ToggleButton("Y", Zmdl.gdf(), 0.05f, 0.045f);
		y_axis.setId(0x357); y_axis.setBackgroundColor(0,255,0);
		y_axis.setOnToggleListener(this);
		main6.add(y_axis);
		main.add(main3);
		main.add(main5);
		main.add(main6);
		splits = new TextAdapter();
		lvSplit = new ListView(0.25f,0.3f,splits);
		main.add(lvSplit);
		Layout main4 = Zmdl.lay(true);
		main4.setMarginTop(0.02f);
		final Button btnAccept = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.12f,0.04f);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(split_selected == -1) {
						Toast.info(Zmdl.gt("select_split"),3f);
					} else if(!main1.isVisible()) {
						lvSplit.setVisibility(View.GONE);
						obj_current.object_wire = new ModelObject(new WireFrameObject(obj_current.getMesh().getVertexData().vertices,obj_current.getMesh().getPart(split_selected)));
						
						obj_current.keep_selector = true;
						main1.setVisibility(View.VISIBLE);
						main5.setVisibility(View.VISIBLE);
						main6.setVisibility(View.VISIBLE);
						btnReset.setVisibility(View.VISIBLE);
						btnFullScreen.setVisibility(View.VISIBLE);
						Toast.debug("EN: Yet in development\nES: Tovadia en desarrollo\nPT: Ainda em desenvolvimento",120f);
						DFFMaterial mat = geometry.materials.get(geometry.splits.get(split_selected).material);
						if(mat.texture.length() > 0){
							if(Zmdl.app().getTextureManager().exist(mat.texture)){
								TextureManager.TextureInfo tex = Zmdl.app().getTextureManager().getInfo(mat.texture);
								tv_texture.setText(Zmdl.gt("texture")+": "+mat.texture);
								iv_texture.setTexture(tex.glId);
								texture_name = mat.texture;
								uv_view.setTexture(tex.glId);
								uv_view.setAspectTexture(tex.width,tex.height);
								uv_view.setVisibility(View.VISIBLE);
								uv_view.setMesh(obj_current.getMesh());
								uv_view.setMeshPart(split_selected);
								setRightRender(true);
								return;
							}
						}
						selectTexture();
					}else{
						setRightRender(false);
						dispose();
						Zmdl.app().panel.dismiss();
					}
				}
			});
		main4.add(btnAccept);
		Button btnCancel = new Button(Zmdl.gt("cancel"),Zmdl.gdf(),0.12f,0.04f);
		btnCancel.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					dispose();
					setRightRender(false);
					Zmdl.app().panel.dismiss();
				}
			});
		btnCancel.setMarginLeft(0.01f);
		btnCancel.setId(0x454);
		main4.add(btnCancel);
		main4.setAlignment(Layout.CENTER);
		main.add(main4);
		lvSplit.setOnItemClickListener(new ListView.OnItemClickListener(){
				@Override
				public void onItemClick(ListView view, Object item, short position, boolean longclick) {
					obj_current.setSplitShow(position);
					split_selected = position;
					
					tv_instance.setText(Zmdl.gt("uv_editor")+":\n"+obj_current.getName()+" -> "+position);
				}
			});
		uv_view = new UVMapView();
		uv_view.setVisibility(View.INVISIBLE);
		uv_view.setListener(new OnUVListener(){
				@Override
				public void select() {
					update();
				}
		});
		
		Zmdl.ctx().addUntouchableView(uv_view);
		Zmdl.ctx().addSlotPriority(uv_view);
	}
	
	private void selectTexture() {
		Zmdl.app().getTextureManager().showDriver(new TextureManager.OnResultListener(){
				@Override
				public void select(boolean success, TextureManager.TextureInfo tex) {
					if(success) {
						tv_texture.setText(Zmdl.gt("texture",tex.name));
						iv_texture.setTexture(tex.glId);
						texture_name = tex.name;
						uv_view.setTexture(tex.glId);
						uv_view.dispose();
						uv_view.setMesh(obj_current.getMesh());
						uv_view.setMeshPart(split_selected);
						uv_view.setAspectTexture(tex.width,tex.height);
						uv_view.setVisibility(View.VISIBLE);
						setRightRender(true);
					} else {
						Toast.info(Zmdl.gt("select_atex"),4f);
					}
				}
			});
	}
	
	
	public boolean showingThis(){
		return Zmdl.tlay(main);
	}

	public void requestShow(ZObject obj_current) {
		inst = Zmdl.inst();
		this.obj_current = obj_current;
		splits.removeAll();
		dff = (DFFSDK)inst.obj;
		geometry = dff.findGeometry(obj_current.getID());
		if(geometry == null){
			return;
		}
		split_selected = -1;
		for(int i = 0;i < obj_current.getMesh().getParts().list.size();i++){
			if(obj_current.getMesh().getPart(i).type == GL.GL_TRIANGLE_STRIP){
				obj_current = null;
				Toast.error(Zmdl.gt("tri_strip_uv"),4);
				return;
			}
			splits.add(Zmdl.gt("split")+" "+i);
		}
		obj_current.selected = false;
		obj_current.setSplitShow(0);
		Zmdl.ep().pick_object = false;
		tv_instance.setText(Zmdl.gt("uv_editor")+":\n"+obj_current.getName());
		Zmdl.apl(main);
		Zmdl.svo(obj_current,false);
		main1.setVisibility(View.GONE);
		btnReset.setVisibility(View.GONE);
		btnFullScreen.setVisibility(View.GONE);
		lvSplit.setVisibility(View.VISIBLE);
		main5.setVisibility(View.GONE);
		main6.setVisibility(View.GONE);
		setRightRender(false);
	}
	
	@Override
	public void OnClick(View view) {
		
	}
	
	private void createFullScreenControl() {
		Layout mn = new Layout(Zmdl.ctx());
		final ToggleButton unselect = new ToggleButton(Zmdl.gt("unselect"), Zmdl.gdf(), 0.15f, 0.05f);
		unselect.setMarginTop(0.01f);
		unselect.setVisibility(View.GONE);
		final ToggleButton move = new ToggleButton(Zmdl.gt("move"), Zmdl.gdf(), 0.15f, 0.05f);
		mn.add(move);
		final ToggleButton zoom = new ToggleButton(Zmdl.gt("zoom"), Zmdl.gdf(), 0.15f, 0.05f);
		zoom.setMarginTop(0.01f);
		mn.add(zoom);
		final ToggleButton select = new ToggleButton(Zmdl.gt("select"), Zmdl.gdf(), 0.15f, 0.05f);
		move.setMarginTop(0.01f);
		select.setMarginTop(0.01f);
		move.setOnToggleListener(new ToggleButton.OnToggleListener(){
				@Override
				public void onToggle(ToggleButton btn, boolean z) {
					select.setToggle(false);
					zoom.setToggle(false);
					unselect.setVisibility(View.GONE);
					uv_view.setMode(z ? 1 : 0);
				}
			});
		zoom.setOnToggleListener(new ToggleButton.OnToggleListener(){
				@Override
				public void onToggle(ToggleButton btn, boolean z) {
					select.setToggle(false);
					move.setToggle(false);
					unselect.setVisibility(View.GONE);
					uv_view.setMode(z ? 2 : 0);
				}
			});
		select.setOnToggleListener(new ToggleButton.OnToggleListener(){
				@Override
				public void onToggle(ToggleButton btn, boolean z) {
					move.setToggle(false);
					zoom.setToggle(false);
					unselect.setToggle(false);
					unselect.setVisibility(z ? View.VISIBLE : View.GONE);
					uv_view.setMode(z ? 3 : 0);
				}
			});
		mn.add(select);
		unselect.setOnToggleListener(new ToggleButton.OnToggleListener(){
				@Override
				public void onToggle(ToggleButton btn, boolean z) {
					unselect.setVisibility(select.isToggled() ? View.VISIBLE : View.GONE);
					uv_view.setUnselecting(z);
				}
			});
		mn.add(unselect);
		Button btnClose = new Button(Zmdl.gt("exit"),Zmdl.gdf(),0.15f,0.05f);
		btnClose.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					uv_view.setFullScreen(false);
					uv_view.setMode(0);
					fs_diag.hide();
				}
			});
		btnClose.setMarginTop(0.01f);
		mn.add(btnClose);
		fs_diag = new Dialog(mn);
		fs_diag.setUseCloseButton(false);
		fs_diag.setTitle(Zmdl.gt("fullscreen"));
	}
	
	private void update() {
		if(vertex_color == null) {
			vertex_color = new byte[obj_current.getMesh().getVertexInfo().vertexCount*4];
		}
		for(int i = 0,j = 0;i < vertex_color.length;i += 4,j++){
			if(uv_view.state_vertices[j]){
				vertex_color[i] = (byte)250;
				vertex_color[i + 1] = (byte)190;
				vertex_color[i + 2] = (byte)10;
			}else{
				vertex_color[i] = (byte)255;
				vertex_color[i + 1] = (byte)255;
				vertex_color[i + 2] = (byte)255;
			}
			vertex_color[i + 3] = (byte)255;
		}
		obj_current.object_wire.getMesh().setVertexColor(vertex_color);
	}

	private void dispose(){
		obj_current.keep_selector = false;
		if(obj_current.object_wire != null){
			obj_current.object_wire.delete();
		}
		obj_current.object_wire = null;
		Zmdl.rp().obj_selector = null;
		obj_current.selected = true;
		uv_view.dispose();
		split_selected = -1;
		obj_current.setSplitShow(-1);
		Zmdl.ep().pick_object = true;
		inst = null;
		Zmdl.svo(null,true);
		Zmdl.rp().testVisibilityFacts();
		uv_view.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onToggle(ToggleButton btn, boolean z) {
		if (btn == translate)
		{
			rotate.setToggle(false);
			scale.setToggle(false);
		}
		else if (btn == rotate)
		{
			translate.setToggle(false);
			scale.setToggle(false);
			
		}
		else if(btn == scale) {
			rotate.setToggle(false);
			translate.setToggle(false);
			
		}else if (btn == x_axis)
		{
			y_axis.setToggle(false);
			
		}
		else if (btn == y_axis)
		{
			x_axis.setToggle(false);
			
		}
	}
	
	private void setRightRender(boolean z){
		if(z){
			Zmdl.rp().getView().setDimens(0.375f,0.865f);
			Zmdl.rp().getView().setRelativePosition(0.625f,-0.135f);
			Zmdl.rp().getCamera().setAspectRatio((0.375f * FX.gpu.getWidth()) / (0.865f * FX.gpu.getHeight()));
		}else{
			Zmdl.rp().getView().setDimens(0.75f,0.865f);
			Zmdl.rp().getView().setRelativePosition(0.25f,-0.135f);
			Zmdl.rp().getCamera().setAspectRatio((0.75f * FX.gpu.getWidth()) / (0.865f * FX.gpu.getHeight()));
		}
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
