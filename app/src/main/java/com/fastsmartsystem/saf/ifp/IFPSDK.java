package com.fastsmartsystem.saf.ifp;
import java.util.*;

public class IFPSDK {
	ArrayList<IFPAnim> animations = new ArrayList<IFPAnim>();
	public String name;
	public String path = "";
	
	public IFPAnim getAnimation(int idx){
		return animations.get(idx);
	}
	
	public IFPAnim getAnimation(String name){
		name = name.toLowerCase();
		for(IFPAnim anim : animations){
			anim.name = anim.name.toLowerCase();
			if(anim.name.startsWith(name)){
				return anim;
			}
		}
		return null;
	}
	
	public ArrayList<String> getListAnimation(){
		ArrayList<String> list = new ArrayList<>();
		Iterator i = animations.iterator();
		while(i.hasNext()){
			list.add(((IFPAnim)i.next()).name);
		}
		return list;
	}
	
	
	public int getFileSize(){
		int size = 28;
		for(IFPAnim a : animations){
			size += 36;
			for(IFPBone b : a.bones){
				size += 36;
				if(b.frameType == IFPKeyframe.RotTransFrame){
					size += b.numKeyFrames * 16;
				}else if(b.frameType == IFPKeyframe.RotFrame){
					size += b.numKeyFrames * 10;
				}else{
					size += b.numKeyFrames * 32;
				}
			}
		}
		return size;
	}
	
}
