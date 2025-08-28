package com.fastsmartsystem.saf.processors;
import com.fastsmartsystem.saf.*;
import com.fastsmartsystem.saf.instance.*;
import com.forcex.*;
import com.forcex.app.*;
import com.forcex.core.gpu.*;
import com.forcex.gfx3d.*;
import com.forcex.gfx3d.shapes.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.forcex.math.*;
import com.forcex.utils.*;
import java.util.*;
import com.fastsmartsystem.saf.adapters.*;
import com.forcex.gfx3d.shader.*;
import com.forcex.gtasdk.*;

public class SelectorWrapper extends PanelFragment implements ToggleButton.OnToggleListener,ProgressBar.onSeekListener,View.OnClickListener,QuadSelector.OnSelectionListener
{
	Layout main;
	TextView tv_instance,tv_scale,tv_scaleY,tv_scaleZ,tv_sensibility;
	float sensibility = 0.5f;
	
	// objeto y instancia actual
	ZObject obj_current;
	ZInstance instance;
	
	ToggleButton select,tbBox,tbSphere,tbCuad;
	
	// propiedades de la seleccion
	public int type_select = -1; // -1: none 0: vertex, 1: triangle
	public int split_index = -1;
	
	/*
	 Si se trabaja con vertex solo se necesita la seleccion de ese vertice en especifico.
	 si se trabaja con triangulos solo se necesita la seleccion del triangulo definido en alguno de los split
	*/
	
	boolean[] selection_state;
	float[] projected_vertices;
	OnFinishSelection listener;
	
	// propiedades del selector
	boolean state;
	Color select_color,unselect_color;
	DrawDynamicTriangle triangles;
	SelectorObject selector;
	ProgressBar skScale,skScaleY,skScaleZ,skSensibility;
	float scale_object = 3f;
	boolean pause_update = false;
	public QuadSelector scr_selector;
	ArrayList<Float> distances = new ArrayList<>();
	
	public SelectorWrapper(){
		triangles = new DrawDynamicTriangle();
		Zmdl.rp().select_triangle = triangles;
		select_color = new Color(240,160,34,200);
		unselect_color = new Color(30,30,30);
		main = Zmdl.lay(0.25f,false);
		tv_instance = new TextView(Zmdl.gdf());
		tv_instance.setTextSize(0.045f);
		tv_instance.setText(Zmdl.gt("selector"));
		tv_instance.setConstraintWidth(0.24f);
		tv_instance.setAlignment(Layout.CENTER);
		tv_instance.setMarginBottom(0.01f);
		main.add(tv_instance);
		Layout main2 = Zmdl.lay(true);
		main2.setAlignment(Layout.CENTER);
		tbCuad = new ToggleButton(Zmdl.gt("norm"),Zmdl.gdf(), 0.08f,0.05f);
		tbCuad.setTextSize(0.04f);
		tbCuad.setOnToggleListener(this);
		tbCuad.setId(0x4343);
		main2.add(tbCuad);
		tbBox = new ToggleButton(Zmdl.gt("box"),Zmdl.gdf(), 0.08f,0.05f);
		tbBox.setTextSize(0.04f);
		tbBox.setOnToggleListener(this);
		tbBox.setId(0x4342);
		main2.add(tbBox);
		tbSphere = new ToggleButton(Zmdl.gt("sphere"),Zmdl.gdf(), 0.08f,0.05f);
		tbSphere.setTextSize(0.04f);
		tbSphere.setOnToggleListener(this);
		tbSphere.setId(0x4343);
		main2.add(tbSphere);
		main2.setMarginTop(0.02f);
		main.add(main2);
		tv_scale = new TextView(Zmdl.gdf());
		tv_scale.setTextSize(0.035f);
		tv_scale.setAlignment(Layout.CENTER);
		tv_scale.setText(Zmdl.gt("scale")+" X");
		tv_scale.setMarginBottom(0.01f);
		main.add(tv_scale);
		skScale = new ProgressBar(0.23f,0.035f);
		skScale.useSeekBar(true); skScale.setId(0x45D);
		skScale.setProgress(0.1f * 50f);
		skScale.setAlignment(Layout.CENTER);
		skScale.setOnSeekListener(this);
		main.add(skScale);
		tv_scaleY = new TextView(Zmdl.gdf());
		tv_scaleY.setTextSize(0.035f);
		tv_scaleY.setAlignment(Layout.CENTER);
		tv_scaleY.setText(Zmdl.gt("scale")+" Y");
		tv_scaleY.setMarginTop(0.01f);
		tv_scaleY.setMarginBottom(0.01f);
		main.add(tv_scaleY);
		skScaleY = new ProgressBar(0.23f,0.035f);
		skScaleY.useSeekBar(true);skScaleY.setId(0x45E);
		skScaleY.setProgress(0.1f * 50f); 
		skScaleY.setAlignment(Layout.CENTER);
		skScaleY.setOnSeekListener(this);
		main.add(skScaleY);
		tv_scaleZ = new TextView(Zmdl.gdf());
		tv_scaleZ.setTextSize(0.035f);
		tv_scaleZ.setAlignment(Layout.CENTER);
		tv_scaleZ.setText(Zmdl.gt("scale")+" Z");
		tv_scaleZ.setMarginTop(0.01f);
		tv_scaleZ.setMarginBottom(0.01f);
		main.add(tv_scaleZ);
		skScaleZ = new ProgressBar(0.23f,0.035f);
		skScaleZ.useSeekBar(true); skScaleZ.setId(0x45F);
		skScaleZ.setProgress(0.1f * 50f);
		skScaleZ.setAlignment(Layout.CENTER);
		skScaleZ.setOnSeekListener(this);
		main.add(skScaleZ);
		tv_sensibility = new TextView(Zmdl.gdf());
		tv_sensibility.setTextSize(0.035f);
		tv_sensibility.setAlignment(Layout.CENTER);
		tv_sensibility.setText(Zmdl.gt("sensibility"));
		tv_sensibility.setMarginTop(0.01f);
		tv_sensibility.setMarginBottom(0.01f);
		main.add(tv_sensibility);
		skSensibility = new ProgressBar(0.24f,0.035f);
		skSensibility.useSeekBar(true);
		skSensibility.setProgress(sensibility / 0.03f);
		skSensibility.setAlignment(Layout.CENTER);
		skSensibility.setOnSeekListener(new ProgressBar.onSeekListener(){
				@Override
				public void seek(int id, float progress){
					sensibility = (progress * 0.03f);
				}
				@Override
				public void finish(float final_progress) {
					// TODO: Implement this method
				}
			});
		main.add(skSensibility);
		select = new ToggleButton(Zmdl.gt("select"),Zmdl.gdf(),0.12f,0.05f);
		select.setId(0x3FE);
		select.setOnToggleListener(this);
		select.setAlignment(Layout.CENTER);
		select.setMarginTop(0.01f);
		main.add(select);
		Layout main4 = Zmdl.lay(true);
		main4.setAlignment(Layout.CENTER);
		main4.setMarginTop(0.02f);
		Button btnGenerateRender = new Button(Zmdl.gt("generate_render"),Zmdl.gdf(),0.2f,0.05f);
		btnGenerateRender.setOnClickListener(this);
		btnGenerateRender.setId(0x558);
		btnGenerateRender.setAlignment(Layout.CENTER);
		btnGenerateRender.setMarginTop(0.02f);
		main.add(btnGenerateRender);
		Button btnAccept = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.12f,0.04f);
		btnAccept.setOnClickListener(this);
		btnAccept.setId(0x553);
		main4.add(btnAccept);
		Button btnCancel = new Button(Zmdl.gt("cancel"),Zmdl.gdf(),0.12f,0.04f);
		btnCancel.setOnClickListener(this);
		btnCancel.setMarginLeft(0.01f);
		btnCancel.setId(0x554);
		main4.add(btnCancel);
		main.add(main4);
		main4.setMarginTop(0.01f);
		selector = new SelectorObject();
		Zmdl.rp().selector = selector;
		scr_selector = new QuadSelector(0.75f,0.865f);
		scr_selector.setRelativePosition(0.25f,-0.135f);
		scr_selector.setVisibility(View.INVISIBLE);
		scr_selector.setListener(this);
		scr_selector.setNormalize(true);
		Zmdl.ctx().addUntouchableView(scr_selector);
		Zmdl.ctx().addSlotPriority(scr_selector);
	}
	
	byte[] vertex_color;
	
	public void requestShow(ZObject obj){
		obj_current = obj;
		obj_current.getTransform().setIdentity();
		obj_current.getTransform().setScale(scale_object,scale_object,scale_object);
		obj_current.selected = false;
		Mesh mesh = obj_current.getMesh();
		if(type_select == 0){
			selection_state = new boolean[mesh.getVertexData().vertices.length / 3];
			obj_current.object_wire = new ModelObject(new WireFrameObject(obj_current.getMesh()));
			vertex_color = new byte[obj_current.object_wire.getMesh().getVertexInfo().vertexCount * 4];
			updateSelectionColor();
			obj_current.hasSelector = true;
		}else if(type_select == 1){
			selection_state = new boolean[mesh.getPart(split_index).index.length / 3];
			obj_current.object_wire = new ModelObject(new WireFrameObject(obj_current.getMesh().getVertexData().vertices,obj_current.getMesh().getPart(split_index)));
			obj_current.wireFrame = true;
			triangles.setup(mesh.getVertexData().vertices,select_color,scale_object);
		}
		projected_vertices = new float[mesh.getVertexInfo().vertexCount * 3];
		Zmdl.svo(obj_current,false);
		instance = Zmdl.inst();
		Zmdl.ep().pick_object = false;
		tbBox.setToggle(false);
		tbSphere.setToggle(false);
		Zmdl.apl(main);
	}
	
	private void updateSelectionColor(){
		for(int i = 0,j = 0;i < vertex_color.length;i += 4,j++){
			if(selection_state[j]){
				vertex_color[i] = (byte)select_color.r;
				vertex_color[i + 1] = (byte)select_color.g;
				vertex_color[i + 2] = (byte)select_color.b;
			}else{
				vertex_color[i] = (byte)unselect_color.r;
				vertex_color[i + 1] = (byte)unselect_color.g;
				vertex_color[i + 2] = (byte)unselect_color.b;
			}
			vertex_color[i + 3] = (byte)255;
		}
		obj_current.object_wire.getMesh().setVertexColor(vertex_color);
	}
	
	@Override
	public void onToggle(ToggleButton btn, boolean z) {
		if(btn == select){
			state = z;
		}else if(btn == tbBox){
			tbSphere.setToggle(false);
			tbCuad.setToggle(false);
			tbBox.setToggle(true);
			pause_update = false;
			switchType();
		}else if(btn == tbSphere){
			tbSphere.setToggle(true);
			tbBox.setToggle(false);
			tbCuad.setToggle(false);
			pause_update = false;
			switchType();
		}else if(btn == tbCuad){
			tbSphere.setToggle(false);
			tbBox.setToggle(false);
			tbCuad.setToggle(true);
			switchType();
			pause_update = true;
		}
	}
	
	public void switchCamControl(boolean z){
		if(isShowing() && tbCuad.isToggled()){
			scr_selector.setVisibility(z ? View.INVISIBLE : View.VISIBLE);
		}
	}
	
	private void switchType() {
		tv_scale.setText(tbBox.isToggled() ? Zmdl.gt("scale")+" X" : Zmdl.gt("radius"));
		tv_scaleY.setVisibility(tbBox.isToggled() ? View.VISIBLE : View.GONE);
		tv_scaleZ.setVisibility(tbBox.isToggled() ? View.VISIBLE : View.GONE);
		skScaleY.setVisibility(tbBox.isToggled() ? View.VISIBLE : View.GONE);
		skScaleZ.setVisibility(tbBox.isToggled() ? View.VISIBLE : View.GONE);
		tv_scale.setVisibility(tbCuad.isToggled() ? View.GONE : View.VISIBLE);
		skScale.setVisibility(tbCuad.isToggled() ? View.GONE : View.VISIBLE);
		tv_sensibility.setVisibility(tbCuad.isToggled() ? View.GONE : View.VISIBLE);
		skSensibility.setVisibility(tbCuad.isToggled() ? View.GONE : View.VISIBLE);
		scr_selector.setVisibility(View.INVISIBLE);
		if(!selector.isSphere() && tbSphere.isToggled()){
			selector.reset();
			selector.setType(true);
			selector.radius = skScale.getProgress() * 0.02f;
			Toast.info(Zmdl.gt("sphere_created"),4f);
		}else if(selector.isSphere() && tbBox.isToggled()){
			selector.reset();
			selector.setType(false);
			selector.scale.x = skScale.getProgress() * 0.02f;
			selector.scale.y = skScaleY.getProgress() * 0.02f;
			selector.scale.z = skScaleZ.getProgress() * 0.02f;
			Toast.info(Zmdl.gt("box_created"),4f);
		}else if(tbCuad.isToggled()){
			selector.reset();
			selector.selector_visible = false;
			scr_selector.setVisibility(View.VISIBLE);
			pause_update = true;
		}
	}

	@Override
	public void seek(int id, float progress) {
		if(!selector.selector_visible){
			return;
		}
		switch(id){
			case 0x45D:
				if(selector.isSphere()){
					selector.radius = progress * 0.02f;
				}else{
					selector.scale.x = progress * 0.02f;
				}
				break;
			case 0x45E:
				selector.scale.y = progress * 0.02f;
				break;
			case 0x45F:
				selector.scale.z = progress * 0.02f;
				break;
		}
	}

	@Override
	public void finish(float final_progress) {
		// TODO: Implement this method
	}
	
	@Override
	public void OnClick(View view) {
		switch(view.getId()){
			case 0x553:
				if(listener != null){
					listener.onFinish(getSelectedLenght() == 0,getSelectionData());
				}
				dispose();
				break;
			case 0x554:
				if(listener != null){
					listener.onFinish(true,null);
				}
				dispose();
				break;
			case 0x558:
				Zmdl.rp().requestRenderDialog(obj_current);
				break;
		}
	}
	
	public void cancel() {
		if(listener != null){
			listener.onFinish(true,null);
		}
		dispose();
	}
	
	private ArrayList<Short> getSelectionData() {
		ArrayList<Short> data = new ArrayList<>();
		for(short i = 0;i < selection_state.length;i++){
			if(selection_state[i]){
				data.add(i);
			}
		}
		return data;
	}
	
	Vector3f temp_pos = new Vector3f();
	
	public void OnTouch(float x,float y,byte type){
		if(selector.selector_visible && type == EventType.TOUCH_DRAGGING){
			Ray ray = Zmdl.rp().getCamera().getPickRay(x, y);
			temp_pos.set(ray.direction);
			temp_pos.multLocal(3f * sensibility);
			temp_pos.addLocal(ray.origin);
			selector.getTransform().setLocation(temp_pos);
		}
	}
	
	float time = 0.5f;
	
	public void update(float delta){
		if(!pause_update && time < 0.0f && selector.selector_visible){
			processSelection();
		}
		time -= delta;
	}

	@Override
	public void click(Vector2f center) {
		
	}

	@Override
	public void selecting(Vector2f center,float width, float height) {
		updateProjection();
		if(type_select == 1){
			distances.clear();
			MeshPart part = obj_current.getMesh().getPart(split_index);
			for(int i = 0,j = 0;i < part.index.length;i += 3,j++) {
				if(testTriangle(part.index,i,center,width,height)){
					selection_state[j] = state;
				} else {
					distances.add(selection_state[j] ? -2f : -1f);
				}
			}
			float media = getDistanceMedia();
			for(int j = 0;j < distances.size();j++){
				if(selection_state[j] && distances.get(j) > 0){
					selection_state[j] = distances.get(j) < media;
				}
			}
			triangles.prepareBufferToFill(getSelectedLenght() * 3);
			for(int i = 0;i < selection_state.length;i++){
				if(selection_state[i]){
					triangles.fill(part.index,i*3);
				}
			}
			triangles.setupIndexBuffer();
		}else{
			for(int i = 0,j = 0;i < projected_vertices.length;i += 3,j++){
				if(testRect(
					projected_vertices[i],
					projected_vertices[i + 1],center,width,height)){
					selection_state[j] = state;
				}
			}
			updateSelectionColor();
		}
	}
	
	private float getDistanceMedia() {
		float dist = 0;
		int count = 0;
		for(float d : distances){
			if(d > 0){
				dist += d;
				count++;
			}
		}
		return dist / count;
	}
	
	private boolean testTriangle(short[] part,int off,Vector2f center,float width,float height){
		int idx0 = (part[off] & 0xffff) * 3,
			idx1 = (part[off+1] & 0xffff)* 3,
			idx2 = (part[off+2] & 0xffff) * 3;
		if(testRect(projected_vertices[idx0],projected_vertices[idx0 + 1],center,width,height) &&
			testRect(projected_vertices[idx1],projected_vertices[idx1 + 1],center,width,height) &&
			testRect(projected_vertices[idx2],projected_vertices[idx2 + 1],center,width,height)){
			 distances.add((projected_vertices[idx0 + 2] + projected_vertices[idx1 + 2] + projected_vertices[idx2 + 2]) * 0.333f);
			return true;
		}
		return false;
	}
	
	private void updateProjection() {
		Vector3f tmp = new Vector3f();
		float[] vertices = obj_current.getMesh().getVertexData().vertices;
		for(int i = 0;i < vertices.length;i += 3){
			tmp.set(
				vertices[i] * scale_object,
				vertices[i + 1] * scale_object,
				vertices[i + 2] * scale_object);
			Vector3f res = tmp.project(Zmdl.rp().getCamera().getProjViewMatrix());
			projected_vertices[i] = res.x;
			projected_vertices[i + 1] = res.y;
			projected_vertices[i + 2] = res.z;
		}
	}
	
	private void processSelection(){
		if(type_select == 0){
			float[] vertices = obj_current.getMesh().getVertexData().vertices;
			for(int i = 0,j = 0;i < vertices.length;i += 3,j++){
				if(selector.test(vertices,i)){
					selection_state[j] = state;
				}
			}
			updateSelectionColor();
		}else if(type_select == 1){
			MeshPart part = obj_current.getMesh().getPart(split_index);
			float[] vertices = obj_current.getMesh().getVertexData().vertices;
			for(int i = 0,j = 0;i < part.index.length;i += 3,j++){
				if(
					selector.test(vertices,(part.index[i] & 0xffff) * 3) &&
					selector.test(vertices,(part.index[i+1] & 0xffff)*3) && 
					selector.test(vertices,(part.index[i+2] & 0xffff)*3)){
					selection_state[j] = state;
				}
			}
			triangles.prepareBufferToFill(getSelectedLenght()*3);
			for(int i = 0;i < selection_state.length;i++){
				if(selection_state[i]){
					triangles.fill(part.index,i*3);
				}
			}
			triangles.setupIndexBuffer();
		}
	}
	
	public void setOnFinishedListener(OnFinishSelection listener){
		this.listener = listener;
	}

	@Override
	public boolean isShowing() {
		return 
			Zmdl.app().panel.isShowing() && 
			Zmdl.app().panel.getContent().getId() == main.getId();
	}
	
	public int getSelectedLenght(){
		int val = 0;
		for(boolean v : selection_state){
			if(v){
				val++;
			}
		}
		return val;
	}
	
	@Override
	public void close() {
		if(isShowing()){
			dispose();
			Zmdl.app().panel.dismiss();
		}
	}
	
	private void dispose(){
		Zmdl.svo(null,true);
		Zmdl.rp().testVisibilityFacts();
		Znode n = Zmdl.tip().getNodeByModelId(obj_current.getID());
		if(n != null){
			obj_current.setTransform(((DFFSDK)instance.obj).getFrame(n.frame_idx).getLocalModelMatrix());
		}
		obj_current.hasSelector = false;
		obj_current.wireFrame = false;
		obj_current.object_wire.delete();
		obj_current.object_wire = null;
		triangles.reset();
		if(selector != null && selector.selector_visible){
			selector.reset();
			selector.selector_visible = false;
		}
		scr_selector.setVisibility(View.INVISIBLE);
	}
	
	private class SelectorObject extends ModelObject {
		ModelObject wireframe;
		boolean selector_visible = false;
		Vector3f min = null;
		Vector3f max = null;
		Vector3f scale = new Vector3f(0.1f);
		float radius = 0.1f;
		
		public boolean isSphere() {
			return min == null;
		}
		
		public void setType(boolean sphere) {
			if(sphere){
				wireframe = new ModelObject(new WireSphere(1f,30));
				setMesh(new Sphere(1f,20,20));
			}else{
				wireframe = new ModelObject(new WireBox(1f,1f,1f));
				setMesh(new Box(1,1,1));
				min = new Vector3f(-1,-1,-1);
				max = new Vector3f(1,1,1);
			}
			getMesh().getPart(0).material.color.set(210,210,210,40);
			wireframe.getMesh().getPart(0).material.color.set(240,210,210);
			selector_visible = true;
		}
		
		@Override
		public void update() {
			if(!selector_visible){
				return;
			}
			super.update();
			if(min != null){
				getTransform().getLocation(min);
				getTransform().getLocation(max);
				min.x -= scale.x; max.x += scale.x;
				min.y -= scale.y; max.y += scale.y;
				min.z -= scale.z; max.z += scale.z;
				getTransform().data[Matrix4f.M00] = scale.x;
				getTransform().data[Matrix4f.M11] = scale.y;
				getTransform().data[Matrix4f.M22] = scale.z;
				wireframe.setTransform(getTransform());
			}else{
				getTransform().data[Matrix4f.M00] = radius;
				getTransform().data[Matrix4f.M11] = radius;
				getTransform().data[Matrix4f.M22] = radius;
				wireframe.setTransform(getTransform());
			}
		}
		
		public boolean test(float[] vert,int index){
			if(min != null){
				float vx = vert[index] * scale_object;
				float vy = vert[index + 1] * scale_object;
				float vz = vert[index + 2] * scale_object;
				return
					(vx >= min.x && vx <= max.x) &&
					(vy >= min.y && vy <= max.y) &&
					(vz >= min.z && vz <= max.z);
			}else{
				float vx = vert[index + 0] * scale_object - getTransform().data[Matrix4f.M03];
				float vy = vert[index + 1] * scale_object - getTransform().data[Matrix4f.M13];
				float vz = vert[index + 2] * scale_object - getTransform().data[Matrix4f.M23];
				float lenght = vx * vx + vy * vy + vz * vz;
				return lenght < (radius * radius);
			}
		}
		
		public void reset() {
			if(getMesh() != null){
				getMesh().delete();
				setMesh(null);
			}
			if(wireframe != null){
				wireframe.delete();
				wireframe = null;
			}
			if(min != null){
				max = null;
				min = null;
			}
		}
		
		@Override
		public void render(DefaultShader shader) {
			if(!selector_visible){
				return;
			}
			super.render(shader);
			wireframe.render(shader);
		}
	}

	public static interface OnFinishSelection{
		void onFinish(boolean cancel,ArrayList<Short> selection);
	}
	
	public static boolean testRect(float x,float y,Vector2f position,float width,float height){
		if(x < -1 || x > 1 || y < -1 || y > 1){
			return false;
		}
		return 
			x >= (position.x - width) && 
			x <= (position.x + width) &&    
			y >= (position.y - height) &&     
			y <= (position.y + height);
	}
} 
