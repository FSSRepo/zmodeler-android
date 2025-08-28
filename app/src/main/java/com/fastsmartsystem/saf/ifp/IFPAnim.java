package com.fastsmartsystem.saf.ifp;
import java.util.*;
import com.forcex.anim.*;
import com.forcex.utils.*;

public class IFPAnim
{
	public ArrayList<IFPBone> bones = new ArrayList<>();
	public String name;
	int value_1, value_2;

	public ArrayList<String> getBonesList(){
		ArrayList<String> list = new ArrayList<>();
		Iterator<IFPBone> i = bones.iterator();
		while(i.hasNext()){
			list.add((i.next()).name);
		}
		return list;
	}
	
	public int getNumKeyFrames(){
		int kf = 0;
		for(IFPBone b : bones){
			if(kf < b.numKeyFrames){
				kf = b.numKeyFrames;
			}
		}
		return kf;
	}
	
	
	public Animation getFXAnim(){
		Animation anim = new Animation(name);
		for(IFPBone b : bones){
			Bone bo = new Bone(b.boneID,b.frameType == IFPKeyframe.RotTransFrame);
			for(IFPKeyframe kf : b.keyframes){
				KeyFrame k = new KeyFrame();
				k.rotation = kf.rotation;
				if(bo.hasPosition){
					k.position = kf.position;
				}
				k.time = kf.time;
				bo.addKeyFrame(k);
			}
			anim.addBone(bo);
		}
		return anim;
	}
}
