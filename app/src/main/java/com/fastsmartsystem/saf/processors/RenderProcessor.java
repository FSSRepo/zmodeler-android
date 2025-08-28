package com.fastsmartsystem.saf.processors;
import com.fastsmartsystem.saf.*;
import com.fastsmartsystem.saf.adapters.*;
import com.fastsmartsystem.saf.instance.*;
import com.forcex.*;
import com.forcex.app.*;
import com.forcex.app.threading.*;
import com.forcex.core.*;
import com.forcex.core.gpu.*;
import com.forcex.gfx3d.*;
import com.forcex.gfx3d.effect.*;
import com.forcex.gfx3d.shapes.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.forcex.math.*;
import java.util.*;
import com.forcex.utils.*;
import com.forcex.postprocessor.*;
import com.forcex.anim.*;

public class RenderProcessor extends PanelFragment implements ListView.OnItemClickListener {
	Layout main;
	RenderView view;
	TransformAxis transform_axis;
	ModelObject floor;
	Camera camera,camera2,camera3,camera_uv;
	ModelObject material_obj;
	ModelRenderer object_batch,editor_batch,select_batch;
	ArrayList<ZObject> objects,queue;
	float ox,oy;
	boolean queueObjects = false,
	update_bound = false,
	first = true,
	block_camera = false,
	renderAllWireFrame = false,
	renderAllLimits = false,
	show_obj_att = false;
	public int cameraMode = 0;
	Light light;
	ModelObject rotation_pivot;
	public ModelObject selector,obj_selector;
	int block_cam_icon,unblock_cam_icon;
	public DrawDynamicTriangle select_triangle;
	public RenderView render_slot;
	float timer_show = 0.0f;
	FrameBuffer fbo_stack;
	public boolean transformed = false;
	
	public RenderProcessor(){
		createInterface();
		create3DRender();
	}
	
	private void createInterface(){
		main = Zmdl.lay(false);
		block_cam_icon = Texture.load("zmdl/block_camera.png");
		unblock_cam_icon = Texture.load("zmdl/unblock_camera.png");
		MenuAdapter adapter = new MenuAdapter();
		adapter.add(Texture.load("zmdl/add.png"),Zmdl.gt("ray_tracing"));
		adapter.add(block_cam_icon,Zmdl.gt("block_camera"));
		adapter.add(Texture.load("zmdl/add.png"),"WireFrame");
		adapter.add(Texture.load("zmdl/add.png"),Zmdl.gt("bounds"));
		ListView menu = new ListView(0.25f,0.6f,adapter);
		menu.setOnItemClickListener(this);
		main.add(menu);
		menu.setInterlinedColor(210,210,210,210);
		view = new RenderView(0.75f,0.865f);
		view.setRelativePosition(0.25f,-0.135f);
		Zmdl.ctx().addUntouchableView(view);
		fbo_stack = new FrameBuffer(128,128);
	}
	
	private void create3DRender() {
		camera = new Camera((0.75f * FX.gpu.getWidth()) / (0.865f * FX.gpu.getHeight()));
		camera.setSmoothMovement(Zmdl.sf().smooth_camera);
		camera.setUseZUp(true);
		camera.setPosition(-5,5,3);
		camera.lookAt(0,0,0);
		camera2 = new Camera(1f);
		camera2.setSmoothMovement(Zmdl.sf().smooth_camera);
		camera2.setUseZUp(true);
		camera3 = new Camera(1f);
		camera3.setUseZUp(true);
		camera_uv = new Camera(1f);
		camera_uv.setProjectionType(Camera.ProjectionType.ORTHOGRAPHIC);
		camera_uv.setUseZUp(true);
		transform_axis = new TransformAxis();
		transform_axis.setVisible(false);
		floor = new ModelObject(new GridRectangle(1,40));
		floor.getMesh().getPart(0).material.color.set(150,150,150,120);
		rotation_pivot = new ModelObject(new DynamicCircle(0.8f,300));
		floor.setRotation(90,0,0);
		rotation_pivot.setVisible(false);
		object_batch = new ModelRenderer(isSkinSopported());
		light = new Light();
		light.setPosition(20,80,90);
		light.setAmbientColor(90,90,90);
		object_batch.useGammaCorrection(true);
		object_batch.getEnvironment().setLight(light);
		material_obj = new ModelObject(new Sphere(2.5f,30,30));
		editor_batch = new ModelRenderer();
		select_batch = new ModelRenderer();
		select_batch.useVertexColor(true);
		objects = new ArrayList<>();
		queue = new ArrayList<>();
	}
	
	public boolean isSkinSopported() {
		String[] unsopported = {
			"Adreno (TM) 305",
			"Adreno (TM) 306",
			"Adreno (TM) 308",
			"Adreno (TM) 320",
			"PowerVR"
		};
		for(String s : unsopported){
			if(FX.gpu.getGPUModel().contains(s)){
				return false;
			}
		}
		return true;
	}

	public void requestShow(){
		if(Zmdl.tlay(main)){
			Zmdl.app().panel.dismiss();
			return;
		}
		Zmdl.apl(main);
	}
	
	public ArrayList<ZObject> getList(){
		return objects;
	}
	
	public void addObject(ZObject obj){
		queue.add(obj);
	}
	
	public Camera getCameraUV(){
		return camera_uv;
	}
	
	public ZObject getObject(int keyhash){
		for(ZObject o : objects){
			if(o.getID() == keyhash){
				return o;
			}
		}
		return null;
	}
	
	public void unselectAll(){
		for(ZObject o : objects){
			o.selected = false;
		}
	}
	
	public void setVisibleByInstance(ZInstance inst,boolean z){
		for(int i = 0;i < inst.getNumModels();i++) {
			ZObject o = Zmdl.go(inst.getModelHash(i));
			if(o != null){
				o.setVisible(z);
			}else{
				break;
			}
		}
	}
	
	public Light getLight(){
		return light;
	}
	
	public void setVisibleObjectByNode(Znode node){
		ArrayList<Integer> ids_collected = new ArrayList<>();
		recurrentNodeCollect(ids_collected,node);
		for(ZObject o : objects) {
			if(ids_collected.indexOf((int)o.getID()) != -1){
				o.setVisible(true);
			}else{
				o.setVisible(false);
			}
		}
	}
	
	public void removeObjectsNode(Znode node){
		ArrayList<Integer> ids_collected = new ArrayList<>();
		recurrentNodeCollect(ids_collected,node);
		ListIterator<ZObject> it = objects.listIterator();
		while(it.hasNext()) {
			ZObject o = it.next();
			if(ids_collected.indexOf((int)o.getID()) != -1){
				it.remove();
				if(!Zmdl.inst().removeHash(o.getID())){
					Toast.error("Error deleting hash",10);
				}
				o = null;
			}
		}
	}
	
	private void recurrentNodeCollect(ArrayList<Integer> collect, Znode node) {
		if(node.isGeometry) {
			collect.add((int)node.model_kh);
		}
		for(TreeNode n : node.getChildren()){
			recurrentNodeCollect(collect,(Znode)n);
		}
	}
	
	public void setVisibleOneObject(ZObject except,boolean z){
		for(ZObject o : objects){
			if(except == null || o.getID() != except.getID()){
				o.setVisible(z);
			}else{
				o.setVisible(!z);
			}
		}
	}
	
	public boolean hasSelected(){
		for(ZObject o : objects){
			if(o.selected){
				return true;
			}
		}
		return false;
	}
	
	public ZObject getSelected(){
		for(ZObject o : objects){
			if(o.selected){
				return o;
			}
		}
		return null;
	}
	
	public void remove(ZInstance inst){
		for(int i = 0;i < inst.getNumModels();i++){
			ZObject o = getObject(inst.getModelHash(i));
			if(o != null) {
				o.delete();
				objects.remove(o);
			}
		}
	}

	public void rewind(){
		queueObjects = true;
		update_bound = true;
	}
	
	public Camera getCamera(){
		return camera;
	}
	
	public ModelObject getRotPivot(){
		return rotation_pivot;
	}
	
	public TransformAxis getTransformAxis(){
		return transform_axis;
	}
	
	public int snapMaterial(Color color,float specular,int texture){
		camera3.setPosition(0f,6,0f);
		camera3.lookAt(0,0,0);
		camera3.update();
		fbo_stack.begin();
		FX.gl.glClearColor(0,0,0,0);
		FX.gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		object_batch.begin(camera3);
		material_obj.getMesh().getPart(0).material.color.set(color);
		material_obj.getMesh().getPart(0).material.specular = specular;
		material_obj.getMesh().getPart(0).material.diffuseTexture = texture;
		object_batch.render(material_obj);
		object_batch.end();
		fbo_stack.end();
		int temp = fbo_stack.getTexture();
		fbo_stack.createTexture();
		return temp;
	}
	
	boolean delta_first = true;
	Vector2f move_delta = new Vector2f();
	float timelapse = 0;
	
	public void render(){
		if(queueObjects){
			for(ZObject o : queue){
				objects.add(o);
			}
			queue.clear();
			queueObjects = false;
		}
		if(update_bound){
			updateObjectsBounds();
		}
		view.begin();
		FX.gl.glClearColor(0.6f,0.6f,0.6f,1.0f);
		FX.gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		renderInstances(camera);
		view.end();
		if(render_slot != null){
			render_slot.begin();
			FX.gl.glClearColor(0.6f,0.6f,0.6f,1.0f);
			FX.gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
			renderInstances(camera2);
			render_slot.end();
		}
		if(show_obj_att){
			timer_show += FX.gpu.getDeltaTime();
			if(timer_show > 2f){
				show_obj_att = false;
				timer_show = 0.0f;
				setVisibleOneObject(null,true);
				testVisibilityFacts();
			}
		}
	}
	
	float deltaX = 0;
	float deltaY = 0;
	float touch_time = 0;
	long start_touch = 0;
	
	public void onTouch(float x,float y,byte type){
		//if(!block_camera)
		{
			if(first){
				ox = x;
				oy = y;
				first = false;
			}
			if(cameraMode == 3){
				camera.setInputType(type);
			}
			if(type == EventType.TOUCH_DRAGGING) {
				float sensor = Maths.clamp(camera.position.length() / 6f,0.08f,3f);
				if(cameraMode != 0) {
					camera.setFieldOfView((sensor > 1.0f) ? 60 : (sensor * 60));
				}
				float dx = (x - ox);
				float dy = (y - oy);
				Zmdl.app().debug.print("Dx: "+String.format("%.4f", dx)+" Dy: "+String.format("%.4f", dy)+" Sensor: "+String.format("%.4f", sensor));
				Zmdl.app().debug.print("CameraPos: "+camera.position);
				Zmdl.app().debug.back();
				Zmdl.app().debug.back();
				
				switch(cameraMode){
					case 1:
						camera.zoom(-(dy * (sensor * 5f)));
						break;
					case 2: {
							float nx = dx * (sensor * 5f);
							float ny = dy * (sensor * 5f);
							camera.move(nx,0, ny);
						}
						break;
					case 3: {
							camera.setResistance((sensor + 0.3f) * Zmdl.sf().sensibility_cam);
							float nx = dx * ((sensor + 0.8f) * 70f);
							float ny = dy * ((sensor + 0.8f) * 70f);

							camera.orbit_cam = true;

							camera.rot_x += -ny;
							camera.rot_y += nx;
							camera.updateDelta(dx, dy);
						}break;
				}
			}
			ox = x;
			oy = y;
		}
	}
	
	float init_distance = 0;
	
	public void updateObjectsBounds(){
		Zmdl.adtsk(new Task(){
				@Override
				public boolean execute() {
					Zmdl.app().getProgressScreen().show();
					init_distance = 0;
					int offset = 0;
					for(ZObject obj : objects){
						obj.calculateBounds();
						float dist = obj.getBound().center.add(obj.getBound().extent).length();
						if(init_distance < dist){
							init_distance = dist;
						}
						Zmdl.app().getProgressScreen().setProgress(100.0f * (float)offset/objects.size());
					}
					if(init_distance == 0.0f || init_distance > 4f || Float.isNaN(init_distance)){
						init_distance = 5f;
					}
					if(!transformed){
						camera.position.set(camera.direction.mult(-init_distance * 2f));
						if(Zmdl.im().numInstances() == 1 && Zmdl.sf().help){
							FX.gpu.queueTask(new Task(){
									@Override
									public boolean execute() {
										MaskPass mask_transparent = new MaskPass(object_batch,0.75f,0.865f); mask_transparent.setNormalColor(true); mask_transparent.setTransparentBackground(true);
										for(ZObject o : objects){
											mask_transparent.addMaskObject(o);
										}
										camera.update();
										mask_transparent.render(camera);
										ImageView temp = new ImageView(mask_transparent.getTextureMask(),0.75f,0.865f);
										temp.setFrameBufferTexture(true); 
										temp.onCreate(Zmdl.ctx().getDrawer());
										temp.setRelativePosition(0.25f,-0.135f);
										temp.updateExtent();
										Zmdl.ctx().getHelpTip().add(Zmdl.gt("obj_in_scene"),Zmdl.gt("ois_info"),temp);
										return true;
									}
								});
						}
					}
					transformed = false;
					Zmdl.app().getProgressScreen().dismiss();
					return objects.size() > 0;
				}
			});
		update_bound = false;
	}
	
	public void rewindCamera(){
		init_distance = 0;
		for(ZObject obj : objects){
			float dist = obj.getBound().center.add(obj.getBound().extent).length();
			if(init_distance < dist){
				init_distance = dist;
			}
		}
		if(init_distance == 0 || Float.isNaN(init_distance)){
			init_distance = 4f;
		}
		camera.position.set(camera.direction.mult(-init_distance * 1.5f));
	}
	
	public RenderView getView(){
		return view;
	}
	
	@Override
	public void onItemClick(ListView view, Object item, short position, boolean longclick) {
		switch(position){
			case 0:
				Zmdl.app().getRayTracing().requestShow();
				return;
			case 1:
				block_camera = !block_camera;
				if(block_camera){
					Toast.info(Zmdl.gt("cam_blocked"),20f);
					((MenuItem)item).icon = unblock_cam_icon;
				}else{
					((MenuItem)item).icon = block_cam_icon;
					Toast.info(Zmdl.gt("cam_unblocked"),20f);
				}
				break;
			case 2:
				renderAllWireFrame = !renderAllWireFrame;
				for(ZObject obj : objects){
					obj.wireFrame = renderAllWireFrame;
				}
				break;
			case 3:
				renderAllLimits = !renderAllLimits;
				for(ZObject obj : objects){
					obj.drawLimits = renderAllLimits;
				}
				break;
		}
		Zmdl.app().panel.dismiss();
	}

	
	private void renderInstances(Camera cam){
		cam.update();
		object_batch.begin(cam);
		for(ZObject o : objects){
			if(!o.hasSelector && !o.draw_weights){
				object_batch.render(o);
			}
			if(o.hasSelector){
				obj_selector = o.object_wire;
				obj_selector.setTransform(o.getTransform());
			}
		}
		object_batch.end();
		editor_batch.begin(cam);
		editor_batch.render(transform_axis);
		if(transform_axis.isVisible()) {
			editor_batch.render(transform_axis.coneX);
			editor_batch.render(transform_axis.coneY);
			editor_batch.render(transform_axis.coneZ);
		}else if(transform_axis.show_long_axis){
			editor_batch.render(transform_axis.boxX);
			editor_batch.render(transform_axis.boxY);
			editor_batch.render(transform_axis.boxZ);
		}
		for(ZObject o : objects){
			if(o.drawLimits && o.isVisible()){
				editor_batch.render(o.bound_wire);
			}else if(o.wireFrame){
				editor_batch.render(o.object_wire);
			}
			if(o.show_label){
				o.object_label.lookAt(camera.getPosition(),true);
				editor_batch.render(o.object_label);
			}
			if(o.draw_origin){
				o.origin.lookAt(camera.getPosition(),true);
				editor_batch.render(o.origin);
			}
		}
		for(ZObject o : objects){
			if(o.draw_weights){
				select_batch.begin(cam);
				select_batch.render(o);
				select_batch.end();
			}
		}
		editor_batch.render(select_triangle);
		if(selector != null){
			editor_batch.render(selector);
		}
		editor_batch.render(rotation_pivot);
		editor_batch.render(floor);
		if(Zmdl.im().hasCurrentInstance() && Zmdl.inst().hasSkeletonRendering()){
			for(SkeletonObject s : Zmdl.inst().skeleton_objects){
				editor_batch.render(s);
			}
		}
		editor_batch.end();
		if(obj_selector != null){
			select_batch.begin(cam);
			select_batch.render(obj_selector);
			select_batch.end();
		}
	}
	
	Dialog diag_render;

	public void requestRenderDialog(final ZObject obj){
		if(diag_render != null){
			diag_render.dismiss();
			render_slot = null;
			diag_render = null;
		}
		MenuAdapter adp = new MenuAdapter();
		adp.add(-1,Zmdl.gt("left"));
		adp.add(-1,Zmdl.gt("right"));
		adp.add(-1,Zmdl.gt("top"));
		adp.add(-1,Zmdl.gt("bottom"));
		adp.add(-1,Zmdl.gt("front"));
		adp.add(-1,Zmdl.gt("back"));
		ListView lv = new ListView(0.25f,0.3f,adp);
		Layout lay  = Zmdl.lay(false);
		lay.add(lv);
		final Dialog diag = new Dialog(lay);
		diag.setTitle(Zmdl.gt("point_view"));
		lv.setOnItemClickListener(new ListView.OnItemClickListener(){
				@Override
				public void onItemClick(ListView view, Object item, short position, boolean longclick) {
					diag.dismiss();
					showRenderDialog(position,obj);
				}
			});
		diag.show(0.5f,0);
	}
	
	int camera2_mode = 0;

	public void showRenderDialog(int point_of_view,ZObject obj){
		if(diag_render != null){
			diag_render.dismiss();
			render_slot = null;
			diag_render = null;
		}
		final Layout lay  = Zmdl.lay(false);
		final ToggleButton zoom = new ToggleButton(Zmdl.gt("zoom"),Zmdl.gdf(),0.09f,0.045f);
		final ToggleButton orbit = new ToggleButton(Zmdl.gt("orbit"),Zmdl.gdf(),0.09f,0.045f);
		zoom.setOnToggleListener(new ToggleButton.OnToggleListener(){
				@Override
				public void onToggle(ToggleButton btn, boolean z) {
					camera2_mode = z ? 1 : 0;
					orbit.setToggle(false);
				}
		});
		orbit.setOnToggleListener(new ToggleButton.OnToggleListener(){
				@Override
				public void onToggle(ToggleButton btn, boolean z) {
					camera2_mode = z ? 2 : 0;
					zoom.setToggle(false);
				}
			});
		Layout controls = Zmdl.lay(true);
		controls.add(zoom);
		controls.add(orbit);
		lay.add(controls);
		render_slot = new RenderView(0.3f,0.3f);
		render_slot.setOnRenderListener(new RenderView.onRenderListener(){
				@Override
				public void touch(RenderView v, float x, float y, float ox, float oy, byte type)	{
					if(camera2_mode == 2){
						camera2.setInputType(type);
					}
					if(type == EventType.TOUCH_DRAGGING){
						float sensor = Maths.clamp(camera2.position.length() / 6f,0.08f,3f);
						camera2.setResistance((sensor + 0.2f) * Zmdl.sf().sensibility_cam);
						float dx = (x - ox);
						float dy = (y - oy);
						switch(camera2_mode){
							case 1:
								camera2.zoom(-(dy * (sensor * 5f)));
								break;
							case 2: {
								float nx =  dx * ((sensor + 0.3f) * 30f);
								float ny =  dy * ((sensor + 0.3f) * 30f);
								camera2.rot_x += ny;
								camera2.rot_y += nx;
								camera2.updateDelta(dx,dy);
							}
								break;
						}
					}
				}	
			});
		render_slot.setApplyAspectRatio(true);
		camera2.setDirection(point_of_view);
		float lenght = obj != null ? obj.getBound().extent.length() * 2f : camera.position.length() * 0.6f;
		camera2.position.set(camera2.direction).multLocal(-lenght);
		camera2.lookAt(0,0,0);
		lay.add(render_slot);
		diag_render = new Dialog(lay);
		diag_render.setTitle("Renderer");
		diag_render.setOnDismissListener(new Dialog.OnDimissListener(){
				@Override
				public boolean dismiss() {
					render_slot.onDestroy();
					lay.remove(render_slot);
					render_slot = null;
					diag_render = null;
					return true;
				}
			});
		diag_render.show();
	}
	
	public void testVisibilityFacts() {
		for(ZInstance inst : Zmdl.im().getInstances()) {
			setVisibleByInstance(inst,inst.model_visible);
		}
		
		for(ZObject o : objects){
			if(o.getName().endsWith("_vlo")){
				o.setVisible(false);
			}
		}
	}
	
	public Vector2f getScreenPos(Vector3f v) {
		Vector3f out = v.project(camera.getProjViewMatrix());
		return new Vector2f(
			out.x * view.getExtentWidth() + view.relative.x,
			out.y * view.getExtentHeight() + view.relative.y);
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
}
