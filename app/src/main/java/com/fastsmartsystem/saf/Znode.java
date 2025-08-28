package com.fastsmartsystem.saf;
import com.forcex.gui.widgets.*;

public class Znode extends TreeNode {
	public short model_kh = -1;
	public short frame_idx;
	public short geo_idx;
	public boolean isGeometry;
	public String name;
	
	public boolean isChild(Znode child) {
		if(getChildren().indexOf(child) != -1){
			return true;
		}
		for(TreeNode n : getChildren()){
			if(((Znode)n).isChild(child)){
				return true;
			}
		}
		return false;
	}
}
