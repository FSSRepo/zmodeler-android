package com.fastsmartsystem.saf.editors;
import com.fastsmartsystem.saf.processors.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.fastsmartsystem.saf.instance.*;
import com.fastsmartsystem.saf.*;
import com.fastsmartsystem.saf.adapters.*;
import com.forcex.core.gpu.*;
import com.forcex.*;
import com.forcex.gtasdk.*;
import java.util.*;

public class FixTool extends PanelFragment
{
	Layout main;
	TextView tv_instance;
	ZInstance inst; // Current instance
	ToggleButton android_support;
	ListView lvErrors;
	MenuAdapter adapter;
	int warning;
	
	public FixTool(){
		main = Zmdl.lay(0.25f,false);
		tv_instance = new TextView(Zmdl.gdf());
		tv_instance.setTextSize(0.05f);
		tv_instance.setAlignment(Layout.CENTER);
		tv_instance.setMarginBottom(0.01f);
		main.add(tv_instance);
		android_support = new ToggleButton(Zmdl.gt("include_andsup"),Zmdl.gdf(),0.15f,0.045f);
		android_support.setAlignment(Layout.CENTER);
		main.add(android_support);
		android_support.setMarginBottom(0.01f);
		adapter = new MenuAdapter();
		lvErrors = new ListView(0.25f,0.2f,adapter);
		main.add(lvErrors);
		Layout main4 = Zmdl.lay(true);
		main4.setMarginTop(0.02f);
		Button btnAccept = new Button(Zmdl.gt("fix"),Zmdl.gdf(),0.12f,0.04f);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					warningFix();
				}
			});
		main4.add(btnAccept);
		Button btnCancel = new Button(Zmdl.gt("cancel"),Zmdl.gdf(),0.12f,0.04f);
		btnCancel.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					close();
				}
			});
		btnCancel.setMarginLeft(0.01f);
		main4.setAlignment(Layout.CENTER);
		main4.add(btnCancel);
		main.add(main4);
		warning = Texture.load(FX.fs.homeDirectory + "zmdl/warning.png");
	}
	
	private void warningFix(){
		Layout lay = new Layout(Zmdl.ctx());
		lay.setUseWidthCustom(true);
		lay.setWidth(0.6f);
		final Dialog diag = new Dialog(lay);
		ImageView iv_war = new ImageView(Texture.load(FX.fs.homeDirectory+"zmdl/warning.png"),0.07f,0.07f);
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
		info.setAlignment(Layout.CENTER); info.setConstraintWidth(0.6f); info.setText(Zmdl.gt("fix_warn"));
		info.setMarginBottom(0.01f);
		lay.add(info);
		Button btnAccept = new Button(Zmdl.gt("fix"),Zmdl.gdf(),0.15f,0.05f);
		btnAccept.setAlignment(Layout.CENTER);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					diag.dismiss();
					if(repair()){
						Toast.info(Zmdl.gt("fixed_correct"),4);
					}
					Zmdl.app().panel.dismiss();
				}
			});
		lay.add(btnAccept);
		diag.show();
	}

	public boolean showingThis(){
		return Zmdl.tlay(main);
	}

	public void requestShow(){
		if(!Zmdl.im().hasCurrentInstance()){
			return;
		}
		inst = Zmdl.inst();
		if(inst.type != 1){
			Toast.error(Zmdl.gt("just_dff"),4f);
			return;
		}
		tv_instance.setText(Zmdl.gt("fix")+":\n"+inst.name+"\n");
		android_support.setToggle(((DFFSDK)inst.obj).checkIsOnlyDFF());
		adapter.removeAll();
		for(String v : inst.error_stack){
			adapter.add(new MenuItem(warning,v));
		}
		Zmdl.apl(main);
	}
	
	private boolean repair() {
		DFFSDK dff = (DFFSDK)inst.obj;
		for(int i = 0;i < dff.fms.size();i++){
			DFFFrame f = dff.getFrame(i);
			if(f.name.contains("no_name")){
				Command.rename(Zmdl.tip().getNodeByModelId(f.model_id),"zmdl"+i);
			}else{
				int index = 2;
				for(int j = 0;j < dff.fms.size();j++){
					if(j != i && dff.fms.get(j).name.equals(f.name)){
						DFFFrame dest = dff.fms.get(j);
						String name = f.name+"_"+index;
						if(dest.geoAttach != -1){
							DFFGeometry geo = dff.geom.get(dest.geoAttach);
							ZObject o = Zmdl.rp().getObject(dest.model_id);
							if(o == null){
								Toast.error("Error rename!! object not exist",4f);
								return false;
							}
							o.setName(name);
							geo.name = name;
							Zmdl.inst().addHash(name,(int)dest.model_id);
						}
						dest.name = name;
						index++;
					}
				}
				Zmdl.inst().root = null;
				Zmdl.inst().root = FileProcessor.setTreeNodes(dff,dff.getFrameRoot());
				Zmdl.app().tree_adapter.setTreeNode(Zmdl.inst().root);
				Zmdl.ns();
			}
		}
		if(android_support.isToggled()){
			dff.convertOnlyDFF();
			Zmdl.ns();
		}
		return true;
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
