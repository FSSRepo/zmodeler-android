package com.fastsmartsystem.saf.adapters;
import com.forcex.gui.widgets.*;
import com.forcex.gui.*;
import com.fastsmartsystem.saf.*;
import com.forcex.core.gpu.*;
import com.forcex.*;

public class ZTreeAdapter extends TreeAdapter {
	public ZTreeAdapter(TreeNode node){
		super(Zmdl.ctx(),node);
	}
	
	TextView tv_name;
	ImageView iv_state, iv_type;
	int frame, geometry;
	
	@Override
	protected void createView(Layout container) {
		container.setOrientation(Layout.HORIZONTAL);
		iv_state = new ImageView(Texture.load("zmdl/arrow.png"),0.01f,0.01f);
		iv_state.setApplyAspectRatio(true);
		iv_state.setMarginTop(0.03f);
		iv_state.setMarginLeft(0.02f);
		container.add(iv_state);
		iv_type = new ImageView(-1,0.015f,0.015f);
		iv_type.setApplyAspectRatio(true);
		iv_type.setMarginTop(0.02f);
		iv_type.setMarginLeft(0.01f);
		container.add(iv_type);
		frame = Texture.load("zmdl/frame_ic.png");
		geometry = Texture.load("zmdl/geom_ic.png");
		tv_name = new TextView(getContext().default_font);
		tv_name.setTextSize(0.05f);
		container.add(tv_name);
	}

	@Override
	protected void updateView(TreeNode node, Layout container) {
		iv_state.setVisibility(node.hasChildren() ? View.VISIBLE : View.INVISIBLE);
		if(node.isExpand()) {
			iv_state.setRotation(90f);
		} else {
			iv_state.setRotation(0);
		}
		iv_type.setTexture(((Znode)node).isGeometry ? geometry : frame);
		tv_name.setText(" "+((Znode)node).name);
	}
}
