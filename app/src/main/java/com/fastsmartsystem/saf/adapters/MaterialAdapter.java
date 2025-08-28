package com.fastsmartsystem.saf.adapters;
import com.forcex.gui.widgets.*;
import com.forcex.gui.*;
import com.forcex.core.gpu.*;
import com.fastsmartsystem.saf.*;

public class MaterialAdapter extends GridAdapter<MaterialItem>
{
	ImageView iv_icon;
	TextView tv_name;
	
	public MaterialAdapter(){
		super(Zmdl.ctx());
	}
	
	@Override
	protected void createView(Layout container) {
		iv_icon = new ImageView(-1,0.05f,0.05f);
		iv_icon.setAlignment(Layout.CENTER);
		iv_icon.setApplyAspectRatio(true);
		iv_icon.setFrameBufferTexture(true);
		iv_icon.setMarginTop(0.01f);
		tv_name = new TextView(getContext().default_font);
		tv_name.setTextSize(0.035f);
		tv_name.setMarginTop(0.01f);
		tv_name.setAlignment(Layout.CENTER);
		tv_name.setConstraintWidth(container.getWidth());
		container.add(iv_icon);
		container.add(tv_name);
	}

	@Override
	protected void updateView(MaterialItem item, short position, Layout container) {
		iv_icon.setTexture(item.snap_material);
		tv_name.setText(item.name);
	}
	
	public void clean() {
		for(short i = 0;i < getNumItems();i++){
			Texture.remove(getItem(i).snap_material);
			getItem(i).snap_material = -1;
		}
	}

	@Override
	public void remove(short index) {
		if(getItem(index).snap_material != -1){
			Texture.remove(getItem(index).snap_material);
		}
		super.remove(index);
	}
}
