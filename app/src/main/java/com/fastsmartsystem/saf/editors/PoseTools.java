package com.fastsmartsystem.saf.editors;
import com.fastsmartsystem.saf.*;
import com.fastsmartsystem.saf.instance.*;
import com.fastsmartsystem.saf.processors.*;
import com.forcex.anim.*;
import com.forcex.gtasdk.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.forcex.app.*;
import com.forcex.math.*;
import com.forcex.core.gpu.*;
import com.forcex.utils.*;
import com.forcex.*;
import java.util.*;
import com.forcex.gfx3d.*;

public class PoseTools extends PanelFragment implements View.OnClickListener,ToggleButton.OnToggleListener
{
	Layout main,pose_mode,weight_paint;
	TextView tv_instance,tv_bone;
	Button anim_editor;
	ZInstance instance;
	ZObject obj_current;
	CPUAnimation cpu_anim;
	boolean blockSelectBone = false;
	SphereInfluencer weight_influencer;
	float[] projected_vertices;
	ToggleButton add,rest,set;
	float strength = 0.2f;
	ArrayList<Float> distances = new ArrayList<>();
	Button btnCancel;
	
	public PoseTools() {
		main = Zmdl.lay(0.25f, false);
		pose_mode = Zmdl.lay(0.25f, false);
		weight_paint = Zmdl.lay(0.25f, false);
		weight_paint.setVisibility(View.GONE);
		tv_instance = new TextView(Zmdl.gdf());
		tv_instance.setTextSize(0.045f);
		tv_instance.setConstraintWidth(0.24f);
		tv_instance.setAlignment(Layout.CENTER);
		tv_instance.setMarginBottom(0.02f);
		main.add(tv_instance);
		tv_bone = new TextView(Zmdl.gdf());
		tv_bone.setTextSize(0.05f);
		tv_bone.setConstraintWidth(0.24f);
		tv_bone.setText("No bone selected");
		tv_bone.setTextColor(220,30,20);
		tv_bone.setAlignment(Layout.CENTER);
		tv_bone.setMarginBottom(0.02f);
		main.add(tv_bone);
		anim_editor = new Button(Zmdl.gt("anim_editor"), Zmdl.gdf(), 0.24f, 0.05f);
		anim_editor.setAlignment(Layout.CENTER);
		anim_editor.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					Zmdl.app().getAnimTool().requestShow(obj_current);
					dispose();
				}
			});
		pose_mode.add(anim_editor);
		Button btnWeights = new Button(Zmdl.gt("weights"), Zmdl.gdf(), 0.2f, 0.05f);
		btnWeights.setAlignment(Layout.CENTER);
		btnWeights.setOnClickListener(this);
		btnWeights.setMarginTop(0.02f);
		btnWeights.setId(0x783);
		pose_mode.add(btnWeights);
		Layout main4 = Zmdl.lay(true);
		main4.setAlignment(Layout.CENTER);
		main4.setMarginTop(0.02f);
		Button btnAccept = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.12f,0.045f);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(weight_paint.isVisible()){
						disposeWeight();
						DFFSkin skin = ((DFFSDK)Zmdl.inst().obj).findGeometry(obj_current.getID()).skin;
						skin.boneIndices = obj_current.getMesh().getVertexData().bone_indices;
						skin.boneWeigts = obj_current.getMesh().getVertexData().bone_weights;
					}else{
						Zmdl.ep().pick_object = true;
						dispose();
						Zmdl.app().panel.dismiss();
					}
				}
			});
		btnAccept.setId(0x453);
		main4.add(btnAccept);
		main4.setAlignment(Layout.CENTER);
		btnCancel = new Button(Zmdl.gt("close"),Zmdl.gdf(),0.12f,0.045f);
		btnCancel.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(pose_mode.isVisible()){
						obj_current.selected = true;
						obj_current.clearCpuAnim();
						dispose();
						Zmdl.ep().pick_object = true;
						Zmdl.ep().requestShow();
					} else if(weight_paint.isVisible()){
							disposeWeight();
						DFFSkin skin = ((DFFSDK)Zmdl.inst().obj).findGeometry(obj_current.getID()).skin;
						obj_current.getMesh().getVertexData().bone_indices = skin.boneIndices;
						obj_current.getMesh().getVertexData().bone_weights = skin.boneWeigts;
					}
				}
			});
		btnCancel.setMarginLeft(0.01f);
		btnCancel.setId(0x454);
		main4.add(btnCancel);
		main.add(weight_paint);
		Layout main2 = Zmdl.lay(true);
		main2.setAlignment(Layout.CENTER);
		add = new ToggleButton(Zmdl.gt("add"), Zmdl.gdf(), 0.12f, 0.045f);
		add.setOnToggleListener(this);
		main2.add(add);
		rest = new ToggleButton(Zmdl.gt("sub"), Zmdl.gdf(), 0.12f, 0.045f);
		rest.setOnToggleListener(this);
		main2.add(rest);
		main2.setMarginTop(0.02f);
		weight_paint.add(main2);
		set = new ToggleButton(Zmdl.gt("set"), Zmdl.gdf(), 0.1f, 0.045f);
		set.setAlignment(Layout.CENTER);
		set.setOnToggleListener(this);
		weight_paint.add(set);
		final TextView tv_weight = new TextView(Zmdl.gdf());
		tv_weight.setTextSize(0.035f);
		tv_weight.setText(Zmdl.gt("strength")+": 0.200");
		tv_weight.setAlignment(Layout.CENTER);
		tv_weight.setMarginTop(0.02f);
		tv_weight.setMarginBottom(0.02f);
		weight_paint.add(tv_weight);
		weight_influencer = new SphereInfluencer();
		weight_influencer.setVisibility(View.GONE);
		final ProgressBar pbWeight = new ProgressBar(0.23f,0.041f);
		pbWeight.setProgress(20);
		final Color temp_color = new Color();
		final Color tmp2 = new Color(0xffcccccc);
		getColorWeight(0.2f,temp_color);
		pbWeight.setAlignment(Layout.CENTER);
		pbWeight.setColor(tmp2,temp_color);
		weight_influencer.setProgressColor(temp_color.r,temp_color.g,temp_color.b);
		pbWeight.useSeekBar(true);
		pbWeight.setOnSeekListener(new ProgressBar.onSeekListener(){
				@Override
				public void seek(int id, float progress) {
					strength = progress * 0.01f;
					getColorWeight(strength,temp_color);
					pbWeight.setColor(tmp2,temp_color);
					weight_influencer.setProgressColor(temp_color.r,temp_color.g,temp_color.b);
					weight_influencer.setProgress(progress);
					weight_influencer.setIndeterminate(true);
					tv_weight.setText(Zmdl.gt("strength")+String.format(": %.3f",strength));
					weight_influencer.setVisibility(View.VISIBLE);
				}
				@Override
				public void finish(float final_progress) {
					weight_influencer.setVisibility(View.INVISIBLE);
					// TODO: Implement this method
				}
		});
		weight_paint.add(pbWeight);
		final TextView tv_radius = new TextView(Zmdl.gdf());
		tv_radius.setTextSize(0.035f);
		tv_radius.setText(Zmdl.gt("radius"));
		tv_radius.setAlignment(Layout.CENTER);
		tv_radius.setMarginTop(0.02f);
		tv_radius.setMarginBottom(0.02f);
		weight_paint.add(tv_radius);
		final ProgressBar pbRadius = new ProgressBar(0.23f,0.041f);
		pbRadius.setProgress(40);
		weight_influencer.setWidth(0.12f);
		weight_influencer.setHeight(0.12f);
		pbRadius.setAlignment(Layout.CENTER);
		pbRadius.useSeekBar(true);
		pbRadius.setOnSeekListener(new ProgressBar.onSeekListener(){
				@Override
				public void seek(int id, float progress) {
					weight_influencer.setWidth(progress * 0.003f);
					weight_influencer.setHeight(progress * 0.003f);
					weight_influencer.setVisibility(View.VISIBLE);
				}
				@Override
				public void finish(float final_progress) {
					weight_influencer.setVisibility(View.INVISIBLE);
					// TODO: Implement this method
				}
			});
		weight_paint.add(pbRadius);
		Zmdl.ctx().addUntouchableView(weight_influencer);
		main.add(pose_mode);
		main.add(main4);
	}

	@Override
	public void OnClick(View view) {
		switch(view.getId()){
			case 0x783: {
				if(hasBoneSelected()){
					weight_paint.setVisibility(View.VISIBLE);
					pose_mode.setVisibility(View.GONE);
					blockSelectBone = true;
					btnCancel.setText(Zmdl.gt("cancel"));
					projected_vertices = new float[obj_current.getMesh().getVertexInfo().vertexCount * 3];
					showWeigts();
				}else{
					Toast.warning(Zmdl.gt("select_bone"),4f);
				}
			}
				break;
		}
	}

	@Override
	public void onToggle(ToggleButton btn, boolean z) {
		if(btn == add){
			rest.setToggle(false);
			set.setToggle(false);
		}else if(btn == rest){
			set.setToggle(false);
			add.setToggle(false);
		}else if(btn == set){
			rest.setToggle(false);
			add.setToggle(false);
		}
	}
	
	private void showWeigts() {
		VertexData data = obj_current.getMesh().getVertexData();
		VertexInfo inf = obj_current.getMesh().getVertexInfo();
		byte[] colors = new byte[inf.vertexCount * 4];
		data.colors = null;
		SkeletonNode n = getBoneSelected();
		for(int i = 0;i < colors.length;i += 4){
			Color result = new Color(0,0,255);
			for(byte j = 0;j < 4;j++){
				if(data.bone_indices[i + j] == n.boneNum) {
					getColorWeight(data.bone_weights[i + j],result);
					break;
				}
			}
			colors[i] = (byte)result.r;
			colors[i + 1] = (byte)result.g;
			colors[i + 2] = (byte)result.b;
			colors[i + 3] = (byte)0xff;
		}
		obj_current.getMesh().setVertexColor(colors);
		colors = null;
		obj_current.draw_weights = true;
		instance.showSkeleton = false;
	}
	
	private void updateBoneWeight() {
		VertexData data = obj_current.getMesh().getVertexData();
		SkeletonNode n = getBoneSelected();
		for(int i = 0;i < data.colors.length;i += 4){
			Color result = new Color(0,0,255);
			for(byte j = 0;j < 4;j++){
				if(data.bone_indices[i + j] == n.boneNum){
					getColorWeight(data.bone_weights[i + j],result);
					break;
				}
			}
			data.colors[i] = (byte)result.r;
			data.colors[i + 1] = (byte)result.g;
			data.colors[i + 2] = (byte)result.b;
			data.colors[i + 3] = (byte)0xff;
		}
		obj_current.getMesh().getVertexBuffer().reset();
	}

	private void updateProjection() {
		Vector3f tmp = new Vector3f();
		VertexData data = obj_current.getMesh().getVertexData();
		for(int j = 0;j < data.vertices.length;j += 3){
			tmp.set(data.vertices[j],data.vertices[j+1],data.vertices[j+2]);
			Vector3f res = tmp.project(Zmdl.rp().getCamera().getProjViewMatrix());
			projected_vertices[j] = res.x;
			projected_vertices[j + 1] = res.y;
			projected_vertices[j + 2] = res.z;
		}
	}
	
	private void disposeWeight() {
		blockSelectBone = false;
		pose_mode.setVisibility(View.VISIBLE);
		weight_paint.setVisibility(View.GONE);
		btnCancel.setText(Zmdl.gt("close"));
		weight_influencer.setVisibility(View.GONE);
		instance.showSkeleton = true;
		projected_vertices = null;
		obj_current.getMesh().getVertexData().colors = null;
		VertexInfo inf = obj_current.getMesh().getVertexInfo();
		inf.removeFlag(VertexInfo.HAS_COLORS);
		inf.addFlag(VertexInfo.HAS_NORMALS);
		inf.addFlag(VertexInfo.HAS_BONES);
		obj_current.getMesh().getVertexBuffer().reset();
		obj_current.draw_weights = false;
	}
	
	private void getColorWeight(float weight,Color result) {
		if(weight > 0.5f){
			float pcnt = (weight - 0.5f) * 2f;
			result.set((int)(255.0f * pcnt),(int)(255f * (1 - pcnt)),0);
			return;
		}
		float pcnt = weight * 2f;
		result.set(0,(int)(255.0f * pcnt),(int)(255f * (1 - pcnt)));
	}
	
	private SkeletonNode getBoneSelected(){
		for(SkeletonObject o : instance.skeleton_objects){
			if(o.selected){
				return o.attach;
			}
		}
		return null;
	}
	
	private boolean hasBoneSelected(){
		for(SkeletonObject o :instance.skeleton_objects){
			if(o.selected){
				return true;
			}
		}
		return false;
	}

	public void requestShow() {
		if (!Zmdl.im().hasCurrentInstance() || Zmdl.tlay(main)) {
			return;
		}
		if(Zmdl.inst().type != 1){
			Toast.error(Zmdl.gt("must_be_dff"),4);
			return;
		}
		if(!((DFFSDK)Zmdl.inst().obj).isSkin()){
			Toast.error(Zmdl.gt("must_be_skin"),4);
			return;
		}
		if(!Zmdl.rp().hasSelected()){
			Toast.info(Zmdl.gt("select_object"),4);
			return;
		}
		if(!Zmdl.rp().isSkinSopported()){
			warningCpuAnim();
		}else{
			continueShow();
		}
	}
	
	private void continueShow(){
		instance = Zmdl.inst();
		obj_current = Zmdl.rp().getSelected();
		obj_current.selected = false;
		Zmdl.ep().pick_object = false;
		if(!obj_current.getMesh().getVertexInfo().hasBones()){
			Toast.error(Zmdl.gt("no_bones_obj"),4);
			return;
		}
		tv_instance.setText(Zmdl.gt("pose_mode")+": "+instance.name);
		DFFSDK dff = (DFFSDK)instance.obj;
		if(!obj_current.hasAnimator()){
			obj_current.setAnimator(new Animator(instance.skeleton,dff.bones.size()));
		}
		if(!Zmdl.rp().isSkinSopported()){
			cpu_anim = obj_current.setupCpuAnim();
			obj_current.setEffectAnimator(false);
			Zmdl.adtsk(cpu_anim);
		}
		instance.loadSkeleton();
		instance.showSkeleton = true;
		Zmdl.apl(main);
	}
	
	public void OnTouch(float x,float y,byte type){
		if(type == EventType.TOUCH_PRESSED &&!blockSelectBone){
			Ray ray = Zmdl.rp().getCamera().getPickRay(x, y);
			ZInstance inst = Zmdl.inst();
			for(SkeletonObject o : instance.skeleton_objects){
				o.selected = false;
			}
			boolean result = false;
			for(short i = 0;i < inst.skeleton_objects.size();i++){
				SkeletonObject o = inst.skeleton_objects.get(i);
				if(o.intersect(ray)){
					tv_bone.setText(o.attach.name);
					tv_bone.setTextColor(20,210,20);
					result = true;
					o.selected = true;
					break;
				}
			}
			if(!result){
				tv_bone.setText("No bone selected");
				tv_bone.setTextColor(220,30,20);
			}
			inst = null;
		}else if(weight_paint.isVisible() && blockSelectBone) {
			RenderView v = Zmdl.rp().getView();
			weight_influencer.setRelativePosition(
				x * v.getWidth() + v.local.x,
				y * v.getHeight() + v.local.y);
			switch(type){
				case EventType.TOUCH_PRESSED:
					weight_influencer.setVisibility(View.VISIBLE);
					updateProjection();
					break;
				case EventType.TOUCH_DRAGGING: {
					weight_influencer.begin(x,y);
					for(int i = 0,j = 0;i < projected_vertices.length;i += 3,j++){
						if(weight_influencer.testRect(
							   projected_vertices[i],
							   projected_vertices[i + 1])) {
							setWeight(j * 4);
						}
					}
					updateBoneWeight();
				}
					break;
				case EventType.TOUCH_DROPPED:
					weight_influencer.setVisibility(View.INVISIBLE);
					break;
			}
			
		}
	}
	
	private void setWeight(int vertex) {
		if(add.isToggled() || rest.isToggled() || set.isToggled()){
			SkeletonNode n = getBoneSelected();
			VertexData data = obj_current.getMesh().getVertexData();
			boolean result = false;
			byte b = 0;
			for(;b < 4; b++) {
				 if(data.bone_indices[vertex + b] == n.boneNum){
					 result = true;
					 break;
				 }
			}
			if(result) {
				if(add.isToggled()) {
					data.bone_weights[vertex + b] += strength * FX.gpu.getDeltaTime();
				}else if(rest.isToggled()){
					data.bone_weights[vertex + b] -= strength * FX.gpu.getDeltaTime();
				} else {
					data.bone_weights[vertex + b] = strength;
				}
				data.bone_weights[vertex + b] = Maths.clamp(data.bone_weights[vertex + b],0,1);
			}else{
				b = 0;
				for(;b < 4;b++) {
					if(data.bone_weights[vertex + b] < 0.05f){
						result = true;
						data.bone_indices[vertex + b] = (byte)n.boneNum;
						data.bone_weights[vertex + b] = 0.05f;
						break;
					}
				}
				if(result) {
					if(add.isToggled()){
						data.bone_weights[vertex + b] += strength * FX.gpu.getDeltaTime();
					}else if(rest.isToggled()){
						data.bone_weights[vertex + b] -= strength * FX.gpu.getDeltaTime();
					} else {
						data.bone_weights[vertex + b] = strength;
					}
					data.bone_weights[vertex + b] = Maths.clamp(data.bone_weights[vertex + b],0,1);
				}
			}
		}
	}
	
	private void warningCpuAnim() {
		Layout lay = new Layout(Zmdl.ctx());
		lay.setUseWidthCustom(true);
		lay.setWidth(0.6f);
		final Dialog diag = new Dialog(lay);
		ImageView iv_war = new ImageView(Texture.load(FX.fs.homeDirectory + "zmdl/warning.png"),0.07f,0.07f);
		iv_war.setAlignment(Layout.CENTER);
		iv_war.setApplyAspectRatio(true);
		lay.add(iv_war);
		TextView warning = new TextView(Zmdl.gdf());
		warning.setTextSize(0.06f); 
		warning.setAlignment(Layout.CENTER); warning.setTextColor(210,0,0);
		warning.setMarginBottom(0.01f); warning.setText(Zmdl.gt("warning"));
		lay.add(warning);
		TextView info = new TextView(Zmdl.gdf());
		info.setTextSize(0.05f); info.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
		info.setAlignment(Layout.CENTER); info.setConstraintWidth(0.6f); info.setText(Zmdl.gt("warning_cpu_anim"));
		info.setMarginBottom(0.01f);
		lay.add(info);
		Button btnAccept = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.15f,0.05f);
		btnAccept.setAlignment(Layout.CENTER);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					diag.dismiss();
					continueShow();
				}
			});
		lay.add(btnAccept);
		diag.show();
	}
	
	public void update() {
		
	}

	public void dispose() {
		cpu_anim = null;
		instance.showSkeleton = false;
		instance = null;
		obj_current = null;
	}

	@Override
	public boolean isShowing(){
		return Zmdl.tlay(main);
	}

	@Override
	public void close() {
		if (isShowing()) {
			if(obj_current != null){
				obj_current.selected = true;
			}
			Zmdl.app().panel.dismiss();
			dispose();
		}
	}
}
