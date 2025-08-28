package com.fastsmartsystem.saf.editors;
import com.fastsmartsystem.saf.processors.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.fastsmartsystem.saf.instance.*;
import com.fastsmartsystem.saf.adapters.*;
import com.fastsmartsystem.saf.*;
import com.forcex.gtasdk.*;

public class UVTools extends PanelFragment implements View.OnClickListener
{
	Layout main;
	TextView tv_instance;
	ZInstance inst; // Current instance
	ZObject obj_current;
	DFFSDK dff;
	DFFGeometry geometry;
	
	public UVTools() {
		main = Zmdl.lay(0.25f,false);
		tv_instance = new TextView(Zmdl.gdf());
		tv_instance.setTextSize(0.05f);
		tv_instance.setAlignment(Layout.CENTER);
		tv_instance.setMarginBottom(0.01f);
		main.add(tv_instance);
		Button btnUVEditor = new Button(Zmdl.gt("uv_editor"),Zmdl.gdf(),0.12f,0.045f);
		btnUVEditor.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					Zmdl.app().getUVMapper().requestShow(obj_current);
					dispose();
				}
			});
		btnUVEditor.setMarginTop(0.02f);
		btnUVEditor.setAlignment(Layout.CENTER);
		main.add(btnUVEditor);
		Button btnFlipU = new Button(Zmdl.gt("flip_u"),Zmdl.gdf(),0.12f,0.045f);
		btnFlipU.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					UVProcessor.flip(true,obj_current);
				}
			});
		btnFlipU.setMarginTop(0.02f);
		btnFlipU.setAlignment(Layout.CENTER);
		main.add(btnFlipU);
		Button btnFlipV = new Button(Zmdl.gt("flip_v"),Zmdl.gdf(),0.12f,0.045f);
		btnFlipV.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					UVProcessor.flip(false,obj_current);
				}
			});
		btnFlipV.setMarginTop(0.02f);
		btnFlipV.setAlignment(Layout.CENTER);
		main.add(btnFlipV);
		Layout main4 = Zmdl.lay(true);
		main4.setMarginTop(0.02f);
		final Button btnAccept = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.12f,0.045f);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					dispose();
					Zmdl.ep().requestShow();
				}
			});
		main4.add(btnAccept);
		Button btnCancel = new Button(Zmdl.gt("cancel"),Zmdl.gdf(),0.12f,0.045f);
		btnCancel.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					dispose();
					Zmdl.ep().pick_object = true;
					obj_current.selected = true;
					Zmdl.ep().requestShow();
				}
			});
		btnCancel.setMarginLeft(0.01f);
		btnCancel.setId(0x454);
		main4.add(btnCancel);
		main4.setAlignment(Layout.CENTER);
		main.add(main4);
	}

	public boolean showingThis(){
		return Zmdl.tlay(main);
	}

	public void requestShow(){
		if(!Zmdl.app().isEditMode()){
			Toast.warning(Zmdl.gt("enable_req_editm"),4f);
			return;
		}
		if(!Zmdl.im().hasCurrentInstance() || Zmdl.tlay(main)){
			return;
		}
		inst = Zmdl.inst();
		if(inst.type != 1){
			Toast.error(Zmdl.gt("just_dff"),4f);
			return;
		}
		obj_current = Zmdl.gos();
		dff = (DFFSDK)inst.obj;
		geometry = dff.findGeometry(obj_current.getID());
		if(geometry == null){
			return;
		}
		obj_current.selected = false;
		Zmdl.ep().pick_object = false;
		tv_instance.setText("UV Tools:\n"+obj_current.getName());
		Zmdl.apl(main);
		Zmdl.svo(obj_current,false);
	}

	@Override
	public void OnClick(View view) {

	}

	private void dispose(){
		obj_current.setSplitShow(-1);
		obj_current = null;
		inst = null;
		Zmdl.svo(null,true);
		Zmdl.rp().testVisibilityFacts();
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
