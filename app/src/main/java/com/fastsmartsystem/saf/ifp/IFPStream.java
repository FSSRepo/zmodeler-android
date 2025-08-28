package com.fastsmartsystem.saf.ifp;
import com.forcex.FX;
import com.forcex.io.*;
import com.forcex.utils.*;
import java.io.*;
import com.forcex.gui.*;
import com.fastsmartsystem.saf.*;

public class IFPStream
{
	public static IFPSDK read(String path) {
		try{
			IFPSDK ifp = new IFPSDK();
			ifp.path = path;
			BinaryStreamReader is = FX.fs.open(path, FileSystem.ReaderType.MEMORY);
			String version = is.readString(4);
			is.skip(4);
			if(version.contains("ANP3")) {
				ifp.name = cortarnombre(is.readString(24));
				int anims = is.readInt();
				for(int i = 0;i < anims;i++){
					IFPAnim anim = new IFPAnim();
					anim.name = cortarnombre(is.readString(24));
					int numObjects = is.readInt();
					anim.value_1 = is.readInt();
					anim.value_2 = is.readInt();
					for(int o = 0;o < numObjects;o++){
						IFPBone object = new IFPBone();
						object.name = cortarnombre(is.readString(24));
						object.frameType = is.readInt();
						int numFrames = is.readInt();
						object.boneID = is.readInt();
						object.numKeyFrames = numFrames;
						if(object.frameType == IFPKeyframe.RotTransFrame){
							for(int f = 0;f < numFrames;f++){
								IFPKeyframe frame = new IFPKeyframe();
								frame.rotation.x = is.readShort()/4096.0f;
								frame.rotation.y = is.readShort()/4096.0f;
								frame.rotation.z = is.readShort()/4096.0f;
								frame.rotation.w = is.readShort()/4096.0f;
								frame.time = is.readShort()/60.0f;
								frame.position.x = is.readShort()/1024.0f;
								frame.position.y = is.readShort()/1024.0f;
								frame.position.z = is.readShort()/1024.0f;
								object.keyframes.add(frame);
							}
						}else if(object.frameType == IFPKeyframe.RotFrame){
							for(int f = 0;f < numFrames;f++){
								IFPKeyframe frame = new IFPKeyframe();
								frame.rotation.x = is.readShort() / 4096.0f;
								frame.rotation.y = is.readShort() / 4096.0f;
								frame.rotation.z = is.readShort() / 4096.0f;
								frame.rotation.w = is.readShort() / 4096.0f;
								frame.time = is.readShort() / 60.0f;
								object.keyframes.add(frame);
							}
						}else{
							for(int f = 0;f < numFrames;f++){
								IFPKeyframe frame = new IFPKeyframe();
								frame.unknown_data = is.readByteArray(32);
								object.keyframes.add(frame);
							}
						}
						anim.bones.add(object);
					}
					ifp.animations.add(anim);
				}
			}else{
				return null;
			}
			is.clear();
			return ifp;
		}catch(Exception e){
			Logger.log(e);
			return null;
		}
	}
	
	public static boolean write(IFPSDK ifp,String path,OnAnimWriteListener listener){
		try{
			BinaryStreamWriter os = new BinaryStreamWriter(new FileOutputStream(path));
			os.writeString("ANP3");
			os.writeInt(ifp.getFileSize());
			os.writeStringFromSize(24,ifp.name);
			os.writeInt(ifp.animations.size());
			listener.onLoading(20);
			for(int a = 0;a < ifp.animations.size(); a++){
				IFPAnim anim = ifp.getAnimation(a);
				os.writeStringFromSize(24,anim.name);
				os.writeInt(anim.bones.size());
				os.writeInt(anim.value_1);
				os.writeInt(anim.value_2);
				listener.onLoading((int)((float)a / ifp.animations.size() * 80f + 20));
				for(int o = 0;o < anim.bones.size();o++){
					IFPBone object = anim.bones.get(o);
					os.writeStringFromSize(24,object.name);
					os.writeInt(object.frameType);
					os.writeInt(object.numKeyFrames);
					os.writeInt(object.boneID);
					if(object.frameType == IFPKeyframe.RotTransFrame){
						for(int f = 0;f < object.numKeyFrames;f++){
							IFPKeyframe frame = object.keyframes.get(f);
							os.writeShort((short)(frame.rotation.x * 4096.0f));//rx
							os.writeShort((short)(frame.rotation.y * 4096.0f));//ry
							os.writeShort((short)(frame.rotation.z * 4096.0f));//rz
							os.writeShort((short)(frame.rotation.w * 4096.0f));//rw
							os.writeShort((short)(frame.time * 60.0f));//time
							os.writeShort((short)(frame.position.x * 1024.0f));//x
							os.writeShort((short)(frame.position.y * 1024.0f));//y
							os.writeShort((short)(frame.position.z * 1024.0f));//z
						}
					}else if(object.frameType == IFPKeyframe.RotFrame){
						for(int f = 0;f < object.numKeyFrames;f++){
							IFPKeyframe frame = object.keyframes.get(f);
							os.writeShort((short)(frame.rotation.x * 4096.0f));//rx
							os.writeShort((short)(frame.rotation.y * 4096.0f));//ry
							os.writeShort((short)(frame.rotation.z * 4096.0f));//rz
							os.writeShort((short)(frame.rotation.w * 4096.0f));//rw
							os.writeShort((short)(frame.time * 60.0f));//time
						}
					}else{
						for(int f = 0;f < object.numKeyFrames;f++){
							IFPKeyframe frame = object.keyframes.get(f);
							os.writeByteArray(frame.unknown_data);//unknown data
						}
					}
				}
			}
			return true;
		}catch(Exception e){
			Logger.log(e);
			Toast.error("Error: see log for details",4);
			return false;
		}
	}
	
	private static String cortarnombre(String str) {
        int indexOf = str.indexOf(0);
        return indexOf > 0 ? str.substring(0, indexOf) : str;
    }
}
