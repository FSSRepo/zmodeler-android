package com.fastsmartsystem.saf.editors;
import android.os.Build;

import com.fastsmartsystem.saf.*;
import com.fastsmartsystem.saf.adapters.*;
import com.fastsmartsystem.saf.ifp.*;
import com.fastsmartsystem.saf.instance.*;
import com.fastsmartsystem.saf.processors.*;
import com.forcex.*;
import com.forcex.anim.*;
import com.forcex.app.threading.*;
import com.forcex.core.gpu.*;
import com.forcex.gtasdk.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.forcex.math.*;
import com.forcex.gfx3d.*;
import com.forcex.core.*;
import com.forcex.utils.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class AnimTool extends PanelFragment implements View.OnClickListener,ProgressBar.onSeekListener
{
	Layout main;
	TextView tv_instance,tv_animloaded,tv_speed,tv_time,tv_duration;
	Button load_ifp,save_ifp,save_anim,btnPlay;
	ZInstance instance;
	boolean saving = false;
	ListView lvAnims;
	TextAdapter adapter;
	Animator animator;
	Animation current_anim;
	ZObject obj_current;
	int tex_play,tex_pause;
	ToggleButton loop;
	Dialog anim_timeline;
	ProgressBar sbTimeLine;
	int numKeyFrames = 0;
	
	public AnimTool() {
		main = Zmdl.lay(0.25f, false);
		showAnimTimeLine();
		tv_instance = new TextView(Zmdl.gdf());
		tv_instance.setTextSize(0.045f);
		tv_instance.setConstraintWidth(0.24f);
		tv_instance.setAlignment(Layout.CENTER);
		tv_instance.setMarginBottom(0.02f);
		main.add(tv_instance);
		load_ifp = new Button(Zmdl.gt("load_ifp"), Zmdl.gdf(), 0.2f, 0.045f);
		load_ifp.setAlignment(Layout.CENTER);
		load_ifp.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(saving){
						Toast.warning(Zmdl.gt("please_wait"),4);
						return;
					}
					if(FX.device.getAndroidVersion() < Build.VERSION_CODES.P) {
						FileDialog.create(Zmdl.ctx(), Zmdl.gt("select")+" IFP", Zmdl.fp().getCurrentPath(), ".ifp", new FileDialog.OnResultListener(){
							@Override
							public boolean tryCancel(short id) {
								return true;
							}
							@Override
							public void open(short id,final String path) {
								Zmdl.adtsk(new Task(){
									@Override
									public boolean execute()
									{
										instance.ifp_anim = null;
										instance.ifp_anim = IFPStream.read(path);
										if(instance.ifp_anim == null){
											Toast.error("Fail to load IFP",4);
											return true;
										}
										Toast.info(Zmdl.gt("anim_file_loaded"),4f);
										FX.gpu.queueTask(new Task(){
											@Override
											public boolean execute() {
												load_ifp.setText("Loaded: "+instance.ifp_anim.name);
												loadAnimList();
												return true;
											}
										});
										return true;
									}
								});
							}
						},Zmdl.app().lang,0x45);
					} else {
						FX.device.invokeFileChooser(true, Zmdl.gt("select")+" IFP", "", new SystemDevice.OnAndroidFileStream() {
							@Override
							public void open(InputStream is, String name) {
								if(!name.endsWith(".ifp")) {
									Toast.error("El formato debe ser .ifp",4);
									return;
								}
								FileProcessor.copy_temp(is, FX.fs.homeDirectory + name);
								Zmdl.adtsk(new Task(){
									@Override
									public boolean execute()
									{
										instance.ifp_anim = null;
										instance.ifp_anim = IFPStream.read(FX.fs.homeDirectory + name);
										if(instance.ifp_anim == null){
											Toast.error("Fail to load IFP",4);
											return true;
										}
										Toast.info(Zmdl.gt("anim_file_loaded"),4f);
										FX.gpu.queueTask(new Task(){
											@Override
											public boolean execute() {
												load_ifp.setText("Loaded: "+instance.ifp_anim.name);
												loadAnimList();
												return true;
											}
										});
										return true;
									}
								});
							}

							@Override
							public void save(OutputStream os) {

							}
						});
					}
				}
		});
		save_ifp = new Button(Zmdl.gt("save_ifp"), Zmdl.gdf(), 0.2f, 0.045f);
		save_ifp.setMarginTop(0.02f); save_ifp.setMarginBottom(0.02f);
		save_ifp.setAlignment(Layout.CENTER);
		save_ifp.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(!instance.hasAnimation()){
						Toast.error(Zmdl.gt("no_ifp_save"),4);
						return;
					}
					Zmdl.app().getProgressScreen().show();
					save_ifp.setVisibility(View.GONE);
					Zmdl.adtsk(new Task(){
							@Override
							public boolean execute() {
								saving = true;
								Toast.info(Zmdl.gt("saving_anim"),4f);
								if (IFPStream.write(instance.ifp_anim, instance.ifp_anim.path, new OnAnimWriteListener(){
											@Override
											public void onLoading(int progress) {
												Zmdl.app().getProgressScreen().setProgress(progress);
											}		
								})){
									Toast.info(Zmdl.gt("anim_file_saved"),4f);
								}
								saving = false;
								Zmdl.app().getProgressScreen().dismiss();
								save_ifp.setVisibility(View.VISIBLE);
								return true;
							}
						});
				}
			});
		main.add(load_ifp);
		main.add(save_ifp);
		save_anim = new Button(Zmdl.gt("save_anim"), Zmdl.gdf(), 0.2f, 0.045f);
		save_anim.setVisibility(View.GONE);
		save_anim.setMarginBottom(0.02f);
		save_anim.setAlignment(Layout.CENTER);
		save_anim.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if((view.getId() >= 0x180 && view.getId() <= 0x183) && current_anim == null){
						Toast.error(Zmdl.gt("1st_load_anim"),4);
						return;
					}
					Zmdl.app().getProgressScreen().show();
					save_anim.setVisibility(View.GONE);
					Zmdl.adtsk(new Task(){
							@Override
							public boolean execute() {
								saveCurrentAnim();
								Zmdl.app().getProgressScreen().dismiss();
								save_anim.setVisibility(View.VISIBLE);
								return true;
							}
						});
				}
			});
		main.add(save_anim);
		adapter = new TextAdapter();
		final ProgressBar sbSpeed = new ProgressBar(0.23f,0.05f);
		sbSpeed.useSeekBar(true); sbSpeed.setOnSeekListener(this);
		sbSpeed.setProgress(50f); sbSpeed.setMarginTop(0.02f);
		sbSpeed.setAlignment(Layout.CENTER); sbSpeed.setId(0x400);
		lvAnims = new ListView(0.25f,0.3f,adapter);
		lvAnims.setOnItemClickListener(new ListView.OnItemClickListener(){
				@Override
				public void onItemClick(ListView view, Object item, short position, boolean longclick) {
					IFPAnim ifpa = instance.ifp_anim.getAnimation(position);
					current_anim = ifpa.getFXAnim();
					animator.doAnimation(current_anim, false);
					numKeyFrames = ifpa.getNumKeyFrames();
					tv_animloaded.setText(Zmdl.gt("anim_loaded",current_anim.name,numKeyFrames));
					animator.control.putCommand(AnimationControl.CMD_PAUSE);
					animator.control.putCommand(loop.isToggled() ? AnimationControl.CMD_LOOP : AnimationControl.CMD_NO_LOOP);
					btnPlay.setIconTexture(tex_play);
					setTime(tv_duration,current_anim.getDuration());
					setTime(tv_time,0);
					updateTimeline();
					animator.control.speed = sbSpeed.getProgress() * 0.02f;
					save_anim.setVisibility(View.VISIBLE);
				}
		});
		main.add(lvAnims);
		tv_animloaded = new TextView(Zmdl.gdf());
		tv_animloaded.setTextSize(0.045f); tv_animloaded.setText(Zmdl.gt("no_anim_loaded"));
		tv_animloaded.setConstraintWidth(0.23f); tv_animloaded.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
		tv_animloaded.setMarginLeft(0.01f);
		tv_animloaded.setMarginTop(0.02f);
		main.add(tv_animloaded);
		loop = new ToggleButton(Zmdl.gt("loop"),Zmdl.gdf(),0.1f,0.045f); loop.setMarginTop(0.02f); loop.setMarginLeft(0.02f);
		loop.setOnToggleListener(new ToggleButton.OnToggleListener(){
				@Override
				public void onToggle(ToggleButton btn, boolean z) {
					animator.control.putCommand(z ? AnimationControl.CMD_LOOP : AnimationControl.CMD_NO_LOOP);
				}
		});
		main.add(loop);
		
		main.add(sbSpeed);
		tv_speed = new TextView(Zmdl.gdf());
		tv_speed.setTextSize(0.045f); tv_speed.setNoApplyConstraintY(true);
		tv_speed.setConstraintWidth(0.24f); tv_speed.setText(Zmdl.gt("speed",1f));
		tv_speed.setAlignment(Layout.CENTER);
		main.add(tv_speed);
		Button btnClose = new Button(Zmdl.gt("close"), Zmdl.gdf(), 0.1f, 0.05f);
		btnClose.setAlignment(Layout.CENTER);
		btnClose.setMarginTop(0.02f);
		btnClose.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					dispose();
					Zmdl.ep().pick_object = true;
					Zmdl.ep().requestShow();
				}
			});
		main.add(btnClose);
	}
	
	private void showAnimTimeLine() {
		Layout lay = Zmdl.lay(0.5f,false);
		Layout controls = Zmdl.lay(true);
		controls.setMarginTop(0.02f);
		controls.setOrientation(Layout.HORIZONTAL);
		controls.setAlignment(Layout.CENTER);
		tex_play = Texture.load(FX.fs.homeDirectory+"zmdl/play.png");
		tex_pause = Texture.load(FX.fs.homeDirectory+"zmdl/pause.png");
		btnPlay = new Button(0.04f, 0.04f); btnPlay.setApplyAspectRatio(true);
		btnPlay.setId(0x180); btnPlay.setIconTexture(tex_play);
		btnPlay.setOnClickListener(this);
		Button btnStop = new Button(0.04f, 0.04f); btnStop.setApplyAspectRatio(true);
		btnStop.setId(0x181); btnStop.setIconTexture(Texture.load(FX.fs.homeDirectory+"zmdl/reset_icon.png"));
		btnStop.setOnClickListener(this);
		controls.add(btnStop);
		Button btnPrevius = new Button(0.04f, 0.04f); btnPrevius.setApplyAspectRatio(true);
		btnPrevius.setId(0x183); btnPrevius.setIconTexture(Texture.load(FX.fs.homeDirectory+"zmdl/previus.png"));
		btnPrevius.setOnClickListener(this);
		controls.add(btnPrevius);
		controls.add(btnPlay);
		Button btnNext = new Button(0.04f, 0.04f); btnNext.setApplyAspectRatio(true);
		btnNext.setId(0x182); btnNext.setIconTexture(Texture.load(FX.fs.homeDirectory+"zmdl/next.png"));
		btnNext.setOnClickListener(this);
		controls.add(btnNext);
		lay.add(controls);
		tv_time = new TextView(Zmdl.gdf());
		tv_time.setTextSize(0.05f); 
		tv_time.setMarginLeft(0.02f);
		tv_time.setNoApplyConstraintY(true);
		lay.add(tv_time);
		tv_duration = new TextView(Zmdl.gdf());
		tv_duration.setTextSize(0.05f);
		tv_duration.setAlignment(Layout.RIGHT);
		tv_duration.setNoApplyConstraintY(true); tv_duration.setMarginRight(0.02f);
		lay.add(tv_duration);
		sbTimeLine = new ProgressBar(0.48f,0.04f);
		sbTimeLine.useSeekBar(true); sbTimeLine.setOnSeekListener(this);
		sbTimeLine.setAlignment(Layout.CENTER); sbTimeLine.setId(0x401);
		lay.add(sbTimeLine);
		anim_timeline = new Dialog(lay);
		anim_timeline.setUseCloseButton(false);
		anim_timeline.setTitle("Timeline");
	}

	@Override
	public void OnClick(View view) {
		if((view.getId() >= 0x180 && view.getId() <= 0x183) && current_anim == null){
			Toast.error(Zmdl.gt("1st_load_anim"),4);
			return;
		}
		AnimationControl cntl = animator.control;
		switch(view.getId()){
			case 0x180:
				if(cntl.isRunning()){
					btnPlay.setIconTexture(tex_play);
					cntl.putCommand(AnimationControl.CMD_PAUSE);
					update_state = 0;
				}else{
					btnPlay.setIconTexture(tex_pause);
					cntl.putCommand(AnimationControl.CMD_PLAY);
					update_state = 1;
				}
				break;
			case 0x181:
				btnPlay.setIconTexture(tex_play);
				cntl.putCommand(AnimationControl.CMD_PAUSE);
				cntl.time = 0.0f;
				sbTimeLine.setProgress(0f);
				break;
			case 0x182:
				if(cntl.time + 0.1f < current_anim.getDuration()){
					cntl.time += 0.1f;
				}else{
					cntl.time = current_anim.getDuration();
				}
				state = (byte)(animator.control.isRunning() ? 1 : 0);
				animator.control.putCommand(AnimationControl.CMD_PLAY);
				if(state == 0){
					animator.update();
					animator.control.putCommand(AnimationControl.CMD_PAUSE);
				}
				updateTimeline();
				break;
			case 0x183:
				if(cntl.time - 0.1f > 0){
					cntl.time -= 0.1f;
				}else{
					cntl.time = 0;
				}
				state = (byte)(animator.control.isRunning() ? 1 : 0);
				animator.control.putCommand(AnimationControl.CMD_PLAY);
				if(state == 0){
					animator.update();
					animator.control.putCommand(AnimationControl.CMD_PAUSE);
				}
				updateTimeline();
				break;
		}
	}

	public void requestShow(ZObject obj) {
		instance = Zmdl.inst();
		obj_current = obj;
		obj_current.selected = false;
		Zmdl.ep().pick_object = false;
		if(!obj_current.getMesh().getVertexInfo().hasBones()){
			Toast.error(Zmdl.gt("no_bones_obj"),4);
			return;
		}
		tv_instance.setText(Zmdl.gt("anim_tool")+": "+instance.name);
		if(instance.hasAnimation()){
			load_ifp.setText(instance.ifp_anim.name);
			loadAnimList();
		}
		animator = obj_current.getAnimator();
		anim_timeline.show(0.25f,-0.7f);
		Zmdl.apl(main);
	}
	
	byte update_state = 0;
	byte state = -1;
	
	@Override
	public void seek(int id, float progress) {
		switch(id){
			case 0x400:
				animator.control.speed = (progress * 0.02f);
				tv_speed.setText(Zmdl.gt("speed",animator.control.speed));
				break;
			case 0x401:
				if(current_anim != null) {
					state = (byte)(animator.control.isRunning() ? 1 : 0);
					animator.control.putCommand(AnimationControl.CMD_PLAY);
					animator.control.time = current_anim.getDuration() * (progress * 0.01f);
					if(state == 0){
						animator.update();
						animator.control.putCommand(AnimationControl.CMD_PAUSE);
					}
					setTime(tv_time,animator.control.time);
				}
				break;
		}
	}

	@Override
	public void finish(float final_progress) {
		
	}
	
	public void update() {
		if(!Zmdl.rp().isSkinSopported()) {
			animator.updateTime();
		}
		if(animator.control.isRunning()){
			updateTimeline();
		}
		if(
			update_state == 1 && 
			!loop.isToggled() && 
			!animator.control.isRunning()){
				btnPlay.setIconTexture(tex_play);
				update_state = 0;
		}
	}
	
	private void updateTimeline(){
		if(current_anim != null){
			sbTimeLine.setProgress((animator.control.time / current_anim.getDuration()) * 100f);
			setTime(tv_time,animator.control.time);
		}
	}
	
	private void setTime(TextView tv,float time){
		int minutes = (int)(time / 60);
		int seconds = (int)(time % 60);
		int cseconds = (int)((time - (seconds - minutes * 60)) * 100f);
		float pcnt = time / current_anim.getDuration();
		tv.setText(((minutes < 10 ? "0":"")+minutes)+":"+((seconds<10?"0":"")+seconds)+"."+((cseconds<10?"0":"")+cseconds)+" ("+(int)(pcnt != 1f ? (pcnt * numKeyFrames) + 1 : numKeyFrames)+")");
	}
	
	public void dispose() {
		tv_time.setText("--:--.-- (?)");
		tv_duration.setText("--:--.-- (?)");
		animator.control.putCommand(AnimationControl.CMD_PAUSE);
		current_anim = null;
		btnPlay.setIconTexture(tex_play);
		tv_animloaded.setText(Zmdl.gt("no_anim_loaded"));
		anim_timeline.hide();
		if(!Zmdl.rp().isSkinSopported()) {
			obj_current.clearCpuAnim();
		}
		obj_current.selected = true;
		sbTimeLine.setProgress(0);
		((ProgressBar)main.findViewByID(0x400)).setProgress(50f);
		tv_speed.setText(Zmdl.gt("speed",1f));
	}
	
	private void loadAnimList(){
		adapter.removeAll();
		for(String str : instance.ifp_anim.getListAnimation()){
			adapter.add(str);
		}
	}
	
	@Override
	public boolean isShowing(){
		return Zmdl.tlay(main);
	}

	@Override
	public void close() {
		if (isShowing()) {
			anim_timeline.hide();
			Zmdl.app().panel.dismiss();
			dispose();
		}
	}
	
	private void saveCurrentAnim() {
		if(current_anim == null){
			Toast.error(Zmdl.gt("1st_load_anim"),4);
			return;
		}
		int vcount = obj_current.getMesh().getVertexInfo().vertexCount;
		Matrix4f[] boneMatrices = animator.getBoneMatrices();
		Matrix3f[] boneMatricesNormal = new Matrix3f[boneMatrices.length];
		for(int i = 0;i<boneMatrices.length;i++){
			boneMatricesNormal[i] = boneMatrices[i].getUpperLeft().invert().transpose();
		}
		VertexData data = obj_current.getMesh().getVertexData();
		float[] dst_v = new float[vcount * 3];
		float[] dst_n = null;
		if(obj_current.getMesh().getVertexInfo().hasNormals()){
			dst_n = new float[vcount * 3];
		}
		Vector3f v = new Vector3f();
		Vector3f n = new Vector3f();
		for(int i = 0, j = 0,k = 0;i < vcount;i++,j += 3,k += 4){
			v.set(data.vertices[j],data.vertices[j+1],data.vertices[j+2]);
			Vector3f vs = boneMatrices[data.bone_indices[k]].mult(data.bone_weights[k]).mult(v);
			vs.addLocal(boneMatrices[data.bone_indices[k+1]].mult(data.bone_weights[k+1]).mult(v));
			vs.addLocal(boneMatrices[data.bone_indices[k+2]].mult(data.bone_weights[k+2]).mult(v));
			vs.addLocal(boneMatrices[data.bone_indices[k+3]].mult(data.bone_weights[k+3]).mult(v));
			dst_v[j] = vs.x;
			dst_v[j+1] = vs.y;
			dst_v[j+2] = vs.z;
			vs = null;
			if(dst_n != null){
				n.set(data.normals,j);
				Vector3f ns = boneMatricesNormal[data.bone_indices[k]].mult(data.bone_weights[k]).mult(n);
				ns.addLocal(boneMatricesNormal[data.bone_indices[k+1]].mult(data.bone_weights[k+1]).mult(n));
				ns.addLocal(boneMatricesNormal[data.bone_indices[k+2]].mult(data.bone_weights[k+2]).mult(n));
				ns.addLocal(boneMatricesNormal[data.bone_indices[k+3]].mult(data.bone_weights[k+3]).mult(n));
				dst_n[j] = ns.x;
				dst_n[j+1] = ns.y;
				dst_n[j+2] = ns.z;
				ns = null;
			}
			Zmdl.app().getProgressScreen().setProgress(((float)i/vcount) * 100f);
		}
		boneMatricesNormal = null;
		boneMatrices = null;
		DFFSDK dff = new DFFSDK();
		dff.game = DFFGame.GTASA;
		dff.name = obj_current.getName()+"_"+current_anim.name;
		DFFFrame frame_new = new DFFFrame();
		frame_new.name = dff.name;
		frame_new.rotation = new Matrix3f();
		frame_new.position = new Vector3f();
		frame_new.flags = 0;
		frame_new.parentIdx = -1;
		dff.addFrame(frame_new);
		DFFGeometry geo_new = new DFFGeometry();
		geo_new.name = frame_new.name;
		geo_new.flags = (DFFGeometry.GEOMETRY_FLAG_DYNAMIC_LIGHTING | DFFGeometry.GEOMETRY_FLAG_POSITIONS | DFFGeometry.GEOMETRY_FLAG_TRISTRIP);
		geo_new.vertices = dst_v;
		geo_new.vertexCount = vcount;
		if(obj_current.getMesh().getVertexInfo().hasTextureCoords()){
			boolean multiple = (data.uvs.length/2) > geo_new.vertexCount;
			if(multiple){
				geo_new.flags |= DFFGeometry.GEOMETRY_FLAG_MULTIPLEUVSETS;
				geo_new.uvsets = 2;
			}else{
				geo_new.uvsets = 1;
			}
			geo_new.flags |= DFFGeometry.GEOMETRY_FLAG_TEXCOORDS;
			geo_new.uvs = data.uvs;
		}
		if(dst_n != null){
			geo_new.normals = dst_n;
			geo_new.flags |= DFFGeometry.GEOMETRY_FLAG_NORMALS;
		}
		int i = 0;
		for(MeshPart p : obj_current.getMesh().getParts().list) {
			DFFIndices indx = new DFFIndices();
			DFFMaterial mat = new DFFMaterial();
			geo_new.isTriangleStrip = obj_current.getMesh().getPrimitiveType() == GL.GL_TRIANGLE_STRIP;
			indx.index = p.index;
			indx.material = i;
			mat.color = p.material.color;
			mat.texture = p.material.textureName;
			geo_new.materials.add(mat);
			geo_new.splits.add(indx);
			i++;
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
		Zmdl.adtsk(new FileProcessor.SaveDFF(instance.path+"/",dff));
	}
}
