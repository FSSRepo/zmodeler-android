package com.fastsmartsystem.saf.editors;
import com.fastsmartsystem.saf.processors.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.fastsmartsystem.saf.instance.*;
import com.fastsmartsystem.saf.*;
import com.forcex.app.*;
import com.forcex.math.*;
import com.forcex.gtasdk.*;
import com.forcex.collision.*;

public class GeometryTools extends PanelFragment implements View.OnClickListener
{
	Layout main;
	TextView tv_instance;
	ZInstance instance;
	ZObject obj_current;
	
	public GeometryTools() {
		main = Zmdl.lay(0.25f, false);
		tv_instance = new TextView(Zmdl.gdf());
		tv_instance.setTextSize(0.045f);
		tv_instance.setConstraintWidth(0.24f);
		tv_instance.setAlignment(Layout.CENTER);
		tv_instance.setMarginBottom(0.02f);
		main.add(tv_instance);
		
		Button btnOGC = new Button(Zmdl.gt("weights"), Zmdl.gdf(), 0.1f, 0.045f);
		btnOGC.setAlignment(Layout.CENTER);
		btnOGC.setOnClickListener(this);
		btnOGC.setMarginTop(0.02f);
		btnOGC.setId(0x783);
		main.add(btnOGC);
		Button btnClose = new Button(Zmdl.gt("close"), Zmdl.gdf(), 0.1f, 0.05f);
		btnClose.setAlignment(Layout.CENTER);
		btnClose.setMarginTop(0.02f);
		btnClose.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					Zmdl.app().panel.dismiss();
					dispose();
				}
			});
		main.add(btnClose);
	}

	@Override
	public void OnClick(View view) {
		switch(view.getId()){
			case 0x783: {
					OriginToGeometryCenter();
				}
				break;
		}
	}
	
	public void requestShow() {
		if (!Zmdl.im().hasCurrentInstance() || Zmdl.tlay(main)) {
			return;
		}
		if(Zmdl.inst().type != 1){
			Toast.error(Zmdl.gt("must_be_dff"),4);
			return;
		}
		if(!Zmdl.rp().hasSelected()){
			Toast.info(Zmdl.gt("select_a_obj"),4);
			return;
		}
		instance = Zmdl.inst();
		obj_current = Zmdl.rp().getSelected();
		obj_current.draw_origin = true;
		obj_current.selected = false;
		Zmdl.ep().pick_object = false;
		tv_instance.setText(Zmdl.gt("vertex_editor")+": "+instance.name);
		Zmdl.apl(main);
	}

	public void OnTouch(float x,float y,byte type){
		if(type == EventType.TOUCH_PRESSED){
			Ray ray = Zmdl.rp().getCamera().getPickRay(x, y);
			
		}
	}
	
	private void OriginToGeometryCenter() {
		float[] vert = obj_current.getMesh().getVertexData().vertices;
		float[] temp = new float[vert.length];
		Vector3f center = obj_current.getBound().center;
		for(int i = 0;i < temp.length;i += 3){
			temp[i] = vert[i] - center.x;
			temp[i + 1] = vert[i + 1] - center.y;
			temp[i + 2] = vert[i + 2] - center.z;
		}
		DFFSDK dff = (DFFSDK)Zmdl.inst().obj;
		obj_current.getMesh().setVertices(temp);
		DFFGeometry geo = dff.findGeometry(obj_current.getID());
		geo.vertices = temp;
		DFFFrame fm = dff.getFrame(geo.frameIdx);
		if(fm.position.isZero()){
			fm.position.set(center);
		}
		fm.invalidateLTM();
		TransformEditor.updateFrameObject(fm);
	}

	public void dispose() {
		obj_current.draw_origin = false;
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
