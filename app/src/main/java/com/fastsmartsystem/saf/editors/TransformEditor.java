package com.fastsmartsystem.saf.editors;
import com.fastsmartsystem.saf.*;
import com.fastsmartsystem.saf.instance.*;
import com.fastsmartsystem.saf.processors.*;
import com.forcex.app.*;
import com.forcex.gfx3d.*;
import com.forcex.gfx3d.shapes.*;
import com.forcex.gtasdk.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.forcex.math.*;
import com.forcex.utils.*;
import java.util.*;
import com.forcex.postprocessor.*;
import com.forcex.*;

public class TransformEditor extends PanelFragment implements ToggleButton.OnToggleListener,View.OnClickListener, UndoListener
{
	Layout main;
	TextView tv_instance;
	int current_axis = -1;
	float sensibility = 5f;
	ModelObject rot_pivot;
	TransformAxis transform_axis;
	// copia en caso de cancelar la operacion
	Vector3f backup_pos;
	Matrix3f backup_rot;
	
	// instancia actual
	ZObject obj_current;
	ZInstance instance;
	ToggleButton translate,rotate,scale,xaxis,yaxis,zaxis;
	
	// solo dff
	DFFFrame frame;
	Vector3f transform_rotation,transform_position;
	boolean isDummy = false;
	String dummyName = "";
	float[] temp;
	DFFGeometry geo;
	Vector3f scale_val = new Vector3f(1f);
	boolean scaleTransformed = false;
	
	public TransformEditor() {
		main = Zmdl.lay(0.25f, false);
		tv_instance = new TextView(Zmdl.gdf());
		tv_instance.setTextSize(0.045f);
		tv_instance.setConstraintWidth(0.24f);
		tv_instance.setAlignment(Layout.CENTER);
		tv_instance.setMarginBottom(0.01f);
		main.add(tv_instance);
		Layout main2 = Zmdl.lay(true);
		main2.setAlignment(Layout.CENTER);
		translate = new ToggleButton(Zmdl.gt("translate"), Zmdl.gdf(), 0.08f, 0.045f);
		translate.setOnToggleListener(this);
		main2.add(translate);
		rotate = new ToggleButton(Zmdl.gt("rotate"), Zmdl.gdf(), 0.08f, 0.045f);
		rotate.setOnToggleListener(this);
		main2.add(rotate);
		scale = new ToggleButton(Zmdl.gt("scale"), Zmdl.gdf(), 0.08f, 0.045f);
		scale.setOnToggleListener(this);
		main2.add(scale);
		main2.setMarginTop(0.02f);
		Layout main3 = Zmdl.lay(true);
		main3.setMarginTop(0.02f);
		main3.setOrientation(Layout.HORIZONTAL);
		main3.setAlignment(Layout.CENTER);
		xaxis = new ToggleButton("X", Zmdl.gdf(), 0.05f, 0.045f);
		xaxis.setId(0x356); xaxis.setBackgroundColor(255,0,0);
		xaxis.setOnToggleListener(this);
		main3.add(xaxis);
		xaxis.setMarginRight(0.01f);
		yaxis = new ToggleButton("Y", Zmdl.gdf(), 0.05f, 0.045f);
		yaxis.setId(0x357); yaxis.setBackgroundColor(0,255,0);
		yaxis.setOnToggleListener(this);
		main3.add(yaxis);
		yaxis.setMarginRight(0.01f);
		zaxis = new ToggleButton("Z", Zmdl.gdf(), 0.05f, 0.045f);
		zaxis.setId(0x358); zaxis.setBackgroundColor(0,0,255);
		zaxis.setOnToggleListener(this);
		main3.add(zaxis);
		zaxis.setMarginRight(0.01f);
		main.add(main2);
		main.add(main3);
		TextView tv_sens = new TextView(Zmdl.gdf());
		tv_sens.setTextSize(0.04f);
		tv_sens.setAlignment(Layout.CENTER);
		tv_sens.setText(Zmdl.gt("sensibility")+":");
		tv_sens.setMarginBottom(0.01f);
		main.add(tv_sens);
		ProgressBar skSens = new ProgressBar(0.23f, 0.045f);
		skSens.useSeekBar(true);
		skSens.setProgress(sensibility * 2f);
		skSens.setAlignment(Layout.CENTER);
		skSens.setOnSeekListener(new ProgressBar.onSeekListener(){
				@Override
				public void seek(int id, float progress) {
					sensibility = progress * 0.5f;
				}
				@Override
				public void finish(float final_progress) {
					// TODO: Implement this method
				}
			});
		main.add(skSens);
		Layout main4 = Zmdl.lay(true);
		main4.setOrientation(Layout.HORIZONTAL);
		main4.setAlignment(Layout.CENTER);
		main4.setMarginTop(0.02f);
		Button btnAccept = new Button(Zmdl.gt("accept"), Zmdl.gdf(), 0.12f, 0.04f);
		btnAccept.setOnClickListener(this);
		btnAccept.setId(0x453);
		btnAccept.setRoundBorders(10);
		main4.add(btnAccept);
		Button btnCancel = new Button(Zmdl.gt("cancel"), Zmdl.gdf(), 0.12f, 0.04f);
		btnCancel.setOnClickListener(this);
		btnCancel.setMarginLeft(0.01f);
		btnCancel.setId(0x454);
		btnCancel.setRoundBorders(10);
		main4.add(btnCancel);
		main.add(main4);
		main4.setMarginBottom(0.01f);
		initialize();
		Zmdl.um().addListener(this,4);
	}
	
	public void setDummyEdition(String name){
		isDummy = true;
		dummyName = name;
	}
	
	public void requestShow() {
		if (!Zmdl.im().hasCurrentInstance() || Zmdl.tlay(main)) {
			return;
		}
		if(Zmdl.inst().type == 1 && ((DFFSDK)Zmdl.inst().obj).isSkin()){
			Toast.error(Zmdl.gt("op_nsp_skin"),4);
			return;
		}
		if (!Zmdl.app().isEditMode()) {
			Toast.warning(Zmdl.gt("enable_req_editm"), 4f);
			return;
		}
		if (transform_axis == null) {
			transform_axis = Zmdl.rp().getTransformAxis();
			rot_pivot = Zmdl.rp().getRotPivot();
		}
		if(!isDummy){
			obj_current = Zmdl.gos();
			obj_current.selected = false;
		}
		Zmdl.ep().pick_object = false;
		instance = Zmdl.inst();
		prepareEdition();
		Zmdl.apl(main);
	}

	float angle;
	float ox,oy;
	boolean first = true;
	
	public void OnPickAxis(float x,float y,byte type){
		if(xaxis.isToggled() || yaxis.isToggled() || zaxis.isToggled()){
			return;
		}
		if(type == EventType.TOUCH_DROPPED){
			rot_pivot.setVisible(false);
		}
		if(type != EventType.TOUCH_PRESSED) {
			return;
		}
		Camera cam = Zmdl.rp().getCamera();
		Ray ray = cam.getPickRay(x,y);
		current_axis = transform_axis.testRay(ray);
		transform_axis.setShowPointer(current_axis);
		if(rotate.isToggled()){
			if (current_axis != -1) {
				rot_pivot.setVisible(true);
				((DynamicCircle)rot_pivot.getMesh()).setAxis(current_axis);
			}else{
				rot_pivot.setVisible(false);
			}
		}
	}

	public void OnTouch(float x, float y, byte type)
	{
		if (first)
		{
			ox = x;
			oy = y;
			first = false;
		}
		if (type == EventType.TOUCH_DRAGGING)
		{
			if(transform_rotation == null || transform_position == null){
				Toast.info("Error: premature transformation nullpointers",4f);
				return;
			}
			if(scale.isToggled()) {
				float delta = -(y - oy) * sensibility * FX.gpu.getDeltaTime() * 0.03f;
				switch(current_axis){
					case -1:
						scale_val.x += delta;
						scale_val.y += delta;
						scale_val.z += delta;
						break;
					case 0:scale_val.x += delta;break;
					case 1:scale_val.y += delta;break;
					case 2:scale_val.z += delta;break;
				}
				for(int i = 0;i < geo.vertices.length; i+=3){
					temp[i] = geo.vertices[i] * scale_val.x;
					temp[i+1] = geo.vertices[i+1] * scale_val.y;
					temp[i+2] = geo.vertices[i+2] * scale_val.z;
				}
				obj_current.getMesh().setVertices(temp);
			} else if (rotate.isToggled())
			{
				float deltaX = (x - ox) * sensibility * FX.gpu.getDeltaTime() * 2f;
				float deltaY = (y - oy) * sensibility * FX.gpu.getDeltaTime() * 2f;
				switch (current_axis)
				{
					case 0:
						transform_rotation.x += (deltaX + deltaY);
						break;
					case 1:
						transform_rotation.y += (deltaX + deltaY);
						break;
					case 2:
						transform_rotation.z += (deltaX + deltaY);
						break;
				}
				switch (current_axis)
				{
					case 0:angle = transform_rotation.x;break;
					case 1:angle = transform_rotation.y;break;
					case 2:angle = transform_rotation.z;break;
				}
				((DynamicCircle)rot_pivot.getMesh()).setAngle(angle);
			}
			else
			{
				float deltaX = (ox - x) * sensibility * FX.gpu.getDeltaTime() * 0.1f;
				float deltaY = (y - oy) * sensibility * FX.gpu.getDeltaTime() * 0.1f;
				switch (current_axis)
				{
					case 0:
						transform_position.x += (deltaX + deltaY);
						break;
					case 1:
						transform_position.y += (deltaX + deltaY);
						break;
					case 2:
						transform_position.z += (deltaX + -deltaY);
						break;
				}
			}
			updateTransform();
		}
		ox = x;
		oy = y;
	}

	@Override
	public void onToggle(ToggleButton btn, boolean z)
	{
		if (btn == translate)
		{
			rotate.setToggle(false);
			rot_pivot.setVisible(false);
			scale.setToggle(false);
		}
		else if (btn == rotate)
		{
			translate.setToggle(false);
			scale.setToggle(false);
			rot_pivot.setVisible(true);
		}
		else if(btn == scale) {
			if(Zmdl.inst().type != 1){
				Toast.error(Zmdl.gt("just_dff"),4f);
				scale.setToggle(false);
				return;
			}
			if(isDummy){
				Toast.error(Zmdl.gt("isnt_geometry"),4f);
				scale.setToggle(false);
				return;
			}
			rotate.setToggle(false);
			rot_pivot.setVisible(false);
			translate.setToggle(false);
			if(geo == null && z) {
				geo = ((DFFSDK)Zmdl.inst().obj).findGeometry(obj_current.getID());
				temp = new float[geo.vertexCount * 3];
				scaleTransformed = true;
			}
		}else if (btn == xaxis)
		{
			current_axis = z ? 0 : -1;
			yaxis.setToggle(false);
			zaxis.setToggle(false);
			if (rotate.isToggled() && current_axis != -1)
			{
				((DynamicCircle)rot_pivot.getMesh()).setAxis(current_axis);
			}
		}
		else if (btn == yaxis)
		{
			current_axis = z ? 1 : -1;
			xaxis.setToggle(false);
			zaxis.setToggle(false);
			if (rotate.isToggled() && current_axis != -1)
			{
				((DynamicCircle)rot_pivot.getMesh()).setAxis(current_axis);
			}
		}
		else if (btn == zaxis)
		{
			current_axis = z ? 2 : -1;
			yaxis.setToggle(false);
			xaxis.setToggle(false);
			if (rotate.isToggled() && current_axis != -1)
			{
				((DynamicCircle)rot_pivot.getMesh()).setAxis(current_axis);
			}
		}
	}

	@Override
	public void OnClick(View view)
	{
		switch (view.getId())
		{
			case 0x453:
				if (instance.type == 1) {
					backup_pos = null;
					backup_rot = null;
					if(scaleTransformed) {
						geo.vertices = temp;
						geo = null;
						temp = null;
						scale_val.set(1f,1f,1f);
						scaleTransformed = false;
					}
				}
				Zmdl.rp().transformed = true;
				Zmdl.rp().rewind();
				dispose();
				break;
			case 0x454:
				if (instance.type == 1)
				{
					frame.position.set(backup_pos);
					frame.rotation.set(backup_rot);
					frame.invalidateLTM();
					backup_pos = null;
					backup_rot = null;
					updateFrameMatrix(frame);
					if(scaleTransformed) {
						obj_current.getMesh().setVertices(geo.vertices);
						geo = null;
						temp = null;
						scale_val.set(1f,1f,1f);
						scaleTransformed = false;
					}
				}else{
					obj_current.getTransform().setIdentity();
				}
				dispose();
				break;
		}
	}

	private void dispose()
	{
		if(!isDummy){
			obj_current.selected = true;
		}
		Zmdl.ep().pick_object = true;
		Zmdl.ep().requestShow();
		transform_axis.setVisible(false);
		transform_axis.show_long_axis = false;
		rot_pivot.setVisible(false);
		isDummy = false;
		dummyName = "";
		instance = null;
		transform_position = null;
		transform_rotation = null;
		
	}

	private void prepareEdition()
	{
		tv_instance.setText("Transform Editor:\n" + instance.name + " -> " +(isDummy ? dummyName : obj_current.getName()));
		if (instance.type == 1)
		{
			frame = ((DFFSDK)instance.obj).findFrame(isDummy ? dummyName : obj_current.getName());
			if (frame == null) {
				Toast.error("Error: frame == null", 3f);
				return;
			}
			backup_pos = frame.position.clone();
			transform_position = frame.position.clone();
			backup_rot = new Matrix3f();
			backup_rot.set(frame.rotation);
			register();
			transform_rotation = Quaternion.fromRotationMatrix(frame.rotation).getAngles();
			if(isDummy) {
				transform_axis.setVisible(true);
				transform_axis.setPosition(frame.getLocalModelMatrix().getLocation(null));
			}
		}else{
			transform_position = new Vector3f();
			transform_rotation = new Vector3f();
		}
		translate.setToggle(true);
		if(!isDummy){
			transform_axis.setPosition(obj_current.getPosition());
			rot_pivot.setPosition(obj_current.getPosition());
		} else {
			rot_pivot.setPosition(transform_axis.getPosition());
		}
		transform_axis.setVisible(true);
		rot_pivot.setVisible(false);
		transform_axis.setShowPointer(-1);
		current_axis = -1;
		scale.setToggle(false);
		rotate.setToggle(false);
		xaxis.setToggle(false);
		yaxis.setToggle(false);
		zaxis.setToggle(false);
	}

	private void updateTransform() {
		if (instance.type == 1) {
			if (rotate.isToggled()) {
				quatX.setEulerAngles(transform_rotation.x,0,0);
				quatY.setEulerAngles(0,transform_rotation.y,0);
				quatZ.setEulerAngles(0,0,transform_rotation.z);
				quatZ.multLocal(quatY.multLocal(quatX));
				quatZ.getMatrix(frame.rotation);
			} else {
				frame.position.set(transform_position);
			}
			frame.invalidateLTM();
			updateFrameMatrix(frame);
			if(!isDummy){
				transform_axis.setPosition(obj_current.getPosition());
				rot_pivot.setPosition(obj_current.getPosition());
			}
			Zmdl.ns();
		}else{
			obj_current.setRotation(transform_rotation.x,transform_rotation.y,transform_rotation.z);
			obj_current.setPosition(transform_position);
		}
	}

	private void updateFrameMatrix(DFFFrame target) {
		if (target.model_id != -1) {
			Zmdl.go(target.model_id).setTransform(target.getLocalModelMatrix());
		}else if(isDummy && target.name.equals(dummyName)){
			transform_axis.getTransform().setLocation(target.getLocalModelMatrix().getLocation(null));
			rot_pivot.setPosition(transform_axis.getPosition());
		}
		for (DFFFrame f : target.children) {
			updateFrameMatrix(f);
		}
	}
	public static void updateFrameObject(DFFFrame target) {
		if (target.model_id != -1) {
			Zmdl.go(target.model_id).setTransform(target.getLocalModelMatrix());
		}
		for (DFFFrame f : target.children) {
			updateFrameObject(f);
		}
	}
	
	@Override
	public boolean isShowing(){
		return Zmdl.tlay(main);
	}
	
	Quaternion quatX,quatY,quatZ;
	
	private void initialize(){
		quatX = new Quaternion();
		quatY = new Quaternion();
		quatZ = new Quaternion();
	}

	@Override
	public void close() {
		if (isShowing())
		{
			dispose();
			Zmdl.app().panel.dismiss();
		}
	}
	
	private void register(){
		int frame_idx = ((DFFSDK)instance.obj).indexOfFrame(frame.name);
		ArrayList<Object> last_data = Zmdl.um().getDataFromType(4);
		for(Object o : last_data){
			TransformUndo u = (TransformUndo)o;
			if(u.frame_idx == frame_idx){
				return;
			}
		}
		TransformUndo t = new TransformUndo();
		t.frame_idx = frame_idx;
		t.rot = backup_rot;
		t.pos = backup_pos;
		Zmdl.um().addUndoData(t,4,"Transform");
	}

	@Override
	public void undo(Object data) {
		TransformUndo t = (TransformUndo)data;
		DFFFrame fop = ((DFFSDK)Zmdl.inst().obj).getFrame(t.frame_idx);
		fop.rotation.set(t.rot);
		fop.position.set(t.pos);
		fop.invalidateLTM();
		updateFrameMatrix(fop);
	}
	
	private class TransformUndo {
		int frame_idx;
		Matrix3f rot;
		Vector3f pos;
	}
} 
