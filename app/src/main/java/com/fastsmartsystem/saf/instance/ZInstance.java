package com.fastsmartsystem.saf.instance;
import java.util.*;
import com.forcex.gui.widgets.*;
import com.fastsmartsystem.saf.*;
import com.forcex.anim.*;
import com.forcex.gtasdk.*;
import com.fastsmartsystem.saf.ifp.*;
import com.forcex.gui.*;
import com.fastsmartsystem.saf.processors.*;
import com.forcex.math.*;

public class ZInstance {
	public String name, path;
	public int id;
	public Znode root;
	public ArrayList<Hash> model_hashes = new ArrayList<>();
	public Object obj;
	public int type;
	public boolean using_this = false;
	protected boolean need_save = false;
	ArrayList<UndoHistory> undo_historial = new ArrayList<>();
	public ArrayList<String> error_stack;
	public IFPSDK ifp_anim;
	public boolean model_visible = true;
	public SkeletonNode skeleton;
	public ArrayList<SkeletonObject> skeleton_objects = new ArrayList<>();
	public boolean showSkeleton;
	
	public void addHash(String name,int model_hash)	 {
		Hash h = new Hash();
		h.name =  name;
		h.model_hash = model_hash;
		model_hashes.add(h);
	}
	
	public int getModelHash(int index){
		return model_hashes.get(index).model_hash;
	}
	
	public int getModelHash(String name){
		if(name.length() == 0) {
			return -1;
		}else if(name == null) {
			Zmdl.app().sendEmailDialog(true);
			Toast.error(Zmdl.gt("geoname_null"), 5f);
			return -1;
		}
		for(Hash h : model_hashes) {
			if(h.name.equals(name)) {
				return h.model_hash;
			}
		}
		return -1;
	}
	
	public boolean removeHash(String name){
		if(name.length() == 0){
			return false;
		}else if(name == null){
			Zmdl.app().sendEmailDialog(true);
			Toast.error(Zmdl.gt("geoname_null"), 5f);
			return false;
		}
		ListIterator<Hash> it = model_hashes.listIterator();
		while(it.hasNext()) {
			if(it.next().name.equals(name)) {
				it.remove();
				return true;
			}
		}
		return false;
	}
	
	public boolean removeHash(int model_hash){
		ListIterator<Hash> it = model_hashes.listIterator();
		while(it.hasNext()) {
			if(it.next().model_hash == model_hash) {
				it.remove();
				return true;
			}
		}
		return false;
	}
	
	public boolean existModelId(int model_hash){
		for(Hash h : model_hashes){
			if(h.model_hash == model_hash){
				return true;
			}
		}
		return false;
	}
	
	public int getNumModels(){
		return model_hashes.size();
	}
	
	public boolean hasAnimation(){
		return ifp_anim != null;
	}
	
	public void setObject(Object obj,int type){
		this.obj = obj;
		this.type = type;
	}
	
	public void setSaveState(boolean z) {
		need_save = z;
		Zmdl.im().updateListInstances();
	}
	
	public boolean hasSkeletonRendering(){
		return skeleton_objects.size() > 0 && showSkeleton;
	}
	
	public void loadSkeleton(){
		if(skeleton_objects.size() > 0 || skeleton == null)return;
		serialize(skeleton);
	}
	
	private void serialize(SkeletonNode skl){
		SkeletonObject o = new SkeletonObject();
		o.attach = skl;
		o.setTransform(skl.getLocalModelMatrix());
		skeleton_objects.add(o);
		for(SkeletonNode n : skl.children){
			serialize(n);
		}
	}
	
	public void update(Animator anim){
		HashMap<SkeletonNode,Matrix4f> b_m = anim.getBoneMap();
		for(SkeletonObject o : skeleton_objects){
			o.setTransform(b_m.get(o.attach));
		}
	}
	
	void destroy() {
		obj = null;
		model_hashes.clear();
		undo_historial.clear();
		if(hasAnimation()){
			ifp_anim = null;
			skeleton = null;
		}
		error_stack = null;
	}
	
	public static class Hash {
		public int model_hash;
		public String name = "";
	}
}
