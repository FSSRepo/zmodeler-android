package com.fastsmartsystem.saf.editors;
import com.fastsmartsystem.saf.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.fastsmartsystem.saf.instance.*;
import com.fastsmartsystem.saf.processors.*;
import com.forcex.gtasdk.*;

public class PartPlayer extends PanelFragment{
	Layout main;
	TextView tv_instance, tv_part_number;
	ZInstance inst; // Current instance
	short cursor = 0;
	ToggleButton select;

	public PartPlayer(){
		main = Zmdl.lay(0.25f,false);
		tv_instance = new TextView(Zmdl.gdf());
		tv_instance.setTextSize(0.05f);
		tv_instance.setAlignment(Layout.CENTER);
		tv_instance.setConstraintWidth(0.24f);
		tv_instance.setMarginBottom(0.01f);
		main.add(tv_instance);

		Layout main2 = Zmdl.lay(true);
		main2.setAlignment(Layout.CENTER);
		Button previus = new Button(Zmdl.gt("previus"),Zmdl.gdf(),0.09f,0.05f);
		main2.add(previus);
		Button next = new Button(Zmdl.gt("next"),Zmdl.gdf(),0.09f,0.05f);
		next.setMarginLeft(0.02f);
		main2.add(next);
		main2.setMarginBottom(0.03f);
		previus.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(cursor > 0){
						cursor--;
						setPlay();
					}
				}
			});
		next.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(cursor < (inst.getNumModels() - 1)){
						cursor++;
						setPlay();
					}
				}
		});
		main.add(main2);

		tv_part_number = new TextView(Zmdl.gdf());
		tv_part_number.setTextSize(0.05f);
		tv_part_number.setMarginRight(0.02f);
		tv_part_number.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
		tv_part_number.setAlignment(Layout.RIGHT);
		main.add(tv_part_number);

		select = new ToggleButton(Zmdl.gt("select"),Zmdl.gdf(),0.11f,0.05f);
		select.setAlignment(Layout.CENTER);
		select.setOnToggleListener(new ToggleButton.OnToggleListener(){
				@Override
				public void onToggle(ToggleButton btn, boolean z) {
					Zmdl.rp().unselectAll();
					after.selected = z;
				}
		});
		main.add(select);
		Button treenode = new Button(Zmdl.gt("node"),Zmdl.gdf(),0.11f,0.05f);
		treenode.setAlignment(Layout.CENTER);
		treenode.setMarginTop(0.02f);
		treenode.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(inst.type != 1){
						Toast.error(Zmdl.gt("just_dff"),4f);
						return;
					}
					if(((DFFSDK)inst.obj).isSkin()){
						return;
					}
					Zmdl.app().panel.dismiss();
					if(after != null){
						after.setShowLabel(false);
						Zmdl.inst().root.collapseAll();
						Znode n = Zmdl.tip().getNodeByModelId(after.getID());
						if(n != null){
							n.expandBack();
							n.emphasize();
						}
						after = null;
					}
					Zmdl.svo(null,true);
					Zmdl.rp().testVisibilityFacts();
				}
			});
		main.add(treenode);
		Button material = new Button(Zmdl.gt("materials"),Zmdl.gdf(),0.11f,0.05f);
		material.setAlignment(Layout.CENTER);
		material.setMarginTop(0.02f);
		material.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(inst.type != 1){
						Toast.error(Zmdl.gt("just_dff"),4f);
						return;
					}
					if(after != null){
						after.setShowLabel(false);
						Zmdl.app().getMaterialEditor().justObtainMaterials = true;
						Zmdl.app().getMaterialEditor().requestShow();
						Zmdl.app().getMaterialEditor().setObjectCurrent(after.getID());
						after = null;
					}
					Zmdl.svo(null,true);
					Zmdl.rp().testVisibilityFacts();
				}
			});
		main.add(material);
		Button finish = new Button(Zmdl.gt("finish"),Zmdl.gdf(),0.11f,0.05f);
		finish.setAlignment(Layout.CENTER);
		finish.setMarginTop(0.02f);
		finish.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					
					Zmdl.ep().requestShow();
					if(after != null){
						after.setShowLabel(false);
						after = null;
					}
					Zmdl.svo(null,true);
					Zmdl.rp().testVisibilityFacts();
				}
		});
		main.add(finish);
	}

	public void requestShow(){
		if(!Zmdl.im().hasCurrentInstance() || Zmdl.tlay(main)){
			return;
		}
		inst = Zmdl.inst();
		Zmdl.apl(main);
		cursor = 0;
		setPlay();
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
	
	ZObject after;
	
	private void setPlay(){
		if(after != null){
			after.setShowLabel(false);
			after = null;
		}
		after = Zmdl.go(inst.getModelHash(cursor));
		if(after != null) {
			select.setToggle(after.selected);
			after.setShowLabel(true);
			Zmdl.svo(after,false);
			after.setVisible(true);
			tv_part_number.setText((cursor + 1) + "/" + inst.getNumModels());
			tv_instance.setText(Zmdl.gt("part_player")+":\n"+inst.name+" -> "+after.getName());
		}
	}
}
