package com.fastsmartsystem.saf;
import com.forcex.gtasdk.*;
import com.forcex.gui.*;
import com.fastsmartsystem.saf.processors.*;

public class Command
{
	public static int rename(Znode node,String name){
		if (Zmdl.tip().getNodeByName(name) != null) {
			Toast.error(Zmdl.gt("name_a_e"),5f);
			return 0;
		}
		if (Zmdl.inst().type == 1) {
			DFFSDK dff = ((DFFSDK)Zmdl.inst().obj);
			DFFFrame frame = dff.getFrame(node.frame_idx);
			if(node.isGeometry){
				DFFGeometry geo = dff.geom.get(frame.geoAttach);
				ZObject o = Zmdl.rp().getObject(frame.model_id);
				if(o == null){
					Toast.error("Error rename!! object not exist",4f);
					return 0;
				}
				o.setName(name);
				geo.name = name;
				if(Zmdl.inst().removeHash(node.name)){
					Zmdl.inst().addHash(name,geo.model_id);
				}
				node.name = name;
			}
			frame.name = name;
			Zmdl.inst().root = null;
			Zmdl.inst().root = FileProcessor.setTreeNodes(dff,dff.getFrameRoot());
			Zmdl.app().tree_adapter.setTreeNode(Zmdl.inst().root);
		} else {
			ZObject o = (ZObject)Zmdl.inst().obj;
			if(o == null){
				Toast.error("Error rename!! object not exist",4f);
				return 0;
			}
			node.name = name;
			o.setName(name);
			if (Zmdl.inst().removeHash(node.name)) {
				Zmdl.inst().addHash(name,node.model_kh);
			}
			Zmdl.inst().root.name = name;
			Zmdl.app().tree_adapter.setTreeNode(Zmdl.inst().root);
		}
		Zmdl.ns();
		return 1;
	}
}
