package com.fastsmartsystem.saf.instance;
import java.util.*;
import com.fastsmartsystem.saf.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.fastsmartsystem.saf.adapters.*;
import com.forcex.utils.*;
import com.forcex.gtasdk.*;

public class InstanceManager {
	ArrayList<ZInstance> instances = new ArrayList<>();
	ZModelerActivity.ZModelerApp app;
	Bridge bridge;
	static int generator = 0x10;
	
	public InstanceManager(ZModelerActivity.ZModelerApp app){
		this.app = app;
		bridge = new Bridge();
	}
	
	public int genID(){
		int temp = generator++;
		return temp;
	}
	
	public Bridge getBridge(){
		return bridge;
	}
	
	public void add(ZInstance inst){
		if(!app.ctx.findViewByID(0x7862).isVisible()){
			app.ctx.findViewByID(0x2425).setVisibility(View.GONE);
			app.ctx.findViewByID(0x2426).setVisibility(View.GONE);
			app.ctx.findViewByID(0x7862).setVisibility(View.VISIBLE);
			Zmdl.app().tip("tree",0x7862,2);
		}
		instances.add(inst); 
	}
	
	public ZInstance get(int index){
		return instances.get(index);
	}
	
	public ZInstance getById(int id){
		for(ZInstance i : instances){
			if(i.id == id){
				return i;
			}
		}
		return null;
	}
	
	public ZInstance getCurrentInstance(){
		for(ZInstance inst : instances){
			if(inst.using_this){
				return inst;
			}
		}
		return null;
	}
	
	public ArrayList<ZInstance> getInstances(){
		return instances;
	}
	
	public void rewind(){
		for(ZInstance inst : instances) {
			inst.using_this = false;
		}
	}
	
	public boolean hasInstances(){
		return instances.size() > 0;
	}
	
	public boolean hasCurrentInstance(){
		for(ZInstance inst : instances){
			if(inst.using_this){
				return true;
			}
		}
		return false;
	}
	
	public void setInstanceCurrent(ZInstance inst){
		rewind();
		app.tree_adapter.setTreeNode(inst.root);
		inst.using_this = true;
		updateListInstances();
	}
	
	public void updateListInstances(){
		app.tab_files.removeAll();
		for(short i = 0;i < instances.size();i++){
			app.tab_files.addTab(instances.get(i).name + (instances.get(i).need_save ? "*" : ""));
			if(instances.get(i).using_this){
				app.tab_files.setSelect(i);
			}
		}
	}
	
	public int numInstances(){
		return instances.size();
	}
	
	public ArrayList<GeometryInstance> getModels() {
		ArrayList<GeometryInstance> list = new ArrayList<>();
		ZInstance cur = getCurrentInstance();
		for(ZInstance inst : instances){
			if(inst != cur){
				if(inst.type == 1){
					DFFSDK dff = (DFFSDK)inst.obj;
					for(DFFGeometry g : dff.geom){
						GeometryInstance gi = new GeometryInstance();
						gi.dff = true;
						gi.model_id = g.model_id;
						gi.name = g.name;
						gi.inst = inst;
						list.add(gi);
					}
				}else{
					GeometryInstance gi = new GeometryInstance();
					gi.dff = false;
					ZObject g = ((ZObject)inst.obj);
					gi.model_id = g.getID();
					gi.name = g.getName();
					gi.inst = inst;
					list.add(gi);
				}
				
			}
		}
		return list;
	}
	
	public ZInstance getInstanceByModelId(int model_id){
		for(ZInstance inst : instances){
			if(!inst.using_this && inst.existModelId(model_id)){
				return inst;
			}
		}
		return null;
	}
	
	public void closeInstance(){
		if(hasCurrentInstance()){
			Layout lay = Zmdl.lay(false);
			TextView tv = new TextView(Zmdl.gdf());
			tv.setText(Zmdl.gt("close_instance"));
			tv.setTextSize(0.045f);
			lay.add(tv);
			Button btnOk = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.1f,0.045f);
			btnOk.setMarginTop(0.01f);
			btnOk.setAlignment(Layout.CENTER);
			lay.add(btnOk);
			final Dialog diag = new Dialog(lay);
			btnOk.setOnClickListener(new View.OnClickListener(){
					@Override
					public void OnClick(View view) {
						diag.dismiss();
						if(Zmdl.inst().need_save){
							discardChanges();
						}else{
							close();
						}
					}
			});
			diag.setTitle(Zmdl.gt("close"));
			diag.show();
		}else{
			Toast.error(Zmdl.gt("no_instances"),3f);
		}
	}
	
	private void discardChanges(){
		Layout lay = new Layout(Zmdl.ctx());
		final Dialog diag = new Dialog(lay);
		TextView tvInfo  =new TextView(Zmdl.gdf());
		tvInfo.setTextSize(0.05f);
		tvInfo.setText(Zmdl.gt("discard_changes"));
		lay.add(tvInfo);
		Layout main4 = Zmdl.lay(true);
		main4.setMarginTop(0.02f);
		Button btnAccept = new Button(Zmdl.gt("accept"),Zmdl.gdf(),0.1f,0.04f);
		btnAccept.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					diag.dismiss();
					close();
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
		diag.setTitle(Zmdl.gt("close"));
		diag.show(0,0.2f);
	}
	
	private void close(){
		ZInstance inst = getCurrentInstance();
		app.getMaterialEditor().reset();
		Zmdl.rp().remove(inst);
		inst.destroy();
		instances.remove(inst);
		app.tree_adapter.setTreeNode(null);
		updateListInstances();
		Zmdl.rp().rewindCamera();
		inst = null;
	}
}
