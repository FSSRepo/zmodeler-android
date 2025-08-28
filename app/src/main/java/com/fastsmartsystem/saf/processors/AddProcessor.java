package com.fastsmartsystem.saf.processors;
import com.fastsmartsystem.saf.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.fastsmartsystem.saf.adapters.*;
import com.forcex.core.gpu.*;
import com.forcex.*;

public class AddProcessor extends PanelFragment implements ListView.OnItemClickListener {
	Layout main;
	int object;
	
	public AddProcessor(){
		main = Zmdl.lay(false);
		MenuAdapter adapter = new MenuAdapter();
		adapter.add(Texture.load("zmdl/box.png"),Zmdl.gt("box"));
		adapter.add(Texture.load("zmdl/sphere.png"),Zmdl.gt("sphere"));
		adapter.add(Texture.load("zmdl/cylinder.png"),Zmdl.gt("cylinder"));
		ListView menu = new ListView(0.25f,0.6f,adapter);
		menu.setInterlinedColor(210,210,210,210);
		menu.setOnItemClickListener(this);
		main.add(menu);
	}

	public void requestShow(){
		if(!Zmdl.im().hasCurrentInstance()){
			return;
		}
		if(Zmdl.inst().type != 1){
			Toast.error(Zmdl.gt("just_dff"),4f);
			return;
		}
		if(Zmdl.tlay(main)){
			Zmdl.app().panel.dismiss();
			return;
		}
		Zmdl.apl(main);
	}

	@Override
	public boolean isShowing() {
		return Zmdl.tlay(main);
	}

	@Override
	public void close() {}

	@Override
	public void onItemClick(ListView view, Object item, short position, boolean longclick) {
		Zmdl.app().panel.dismiss();
		object = position;
		Zmdl.tip().add = true;
		Zmdl.tip().showOperator();
		Toast.info("Press long a node",4);
	}
}
