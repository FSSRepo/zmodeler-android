package com.fastsmartsystem.saf;
import android.util.Log;

import com.forcex.io.*;
import java.io.*;
import com.forcex.*;
import com.forcex.math.*;
import java.util.*;

public class SettingFile
{
	public boolean help = true;
	public boolean smooth_camera = true;
	public byte num_opened = 10;
	public float sensibility_cam = 10f;

	String setting_path = "";
	
	public SettingFile(){
		setting_path = FX.fs.homeDirectory + "setting.cfg";
		if(!new File(setting_path).exists()) {
			return;
		}
		BinaryStreamReader is = FX.fs.open(setting_path, FileSystem.ReaderType.MEMORY);
		int version = is.readInt();
		if(version >= 0x1){
			help = is.readBoolean();
			smooth_camera = is.readBoolean();
		}
		if(version >= 0x3){
			sensibility_cam = is.readFloat();
		}
		if(version >= 0x4){
			num_opened = is.readByte();
		}
		is.clear();
	}
	
	public void save() {
		try {
			BinaryStreamWriter is = new BinaryStreamWriter(new FileOutputStream(setting_path));
			is.writeInt(0x4);
			is.writeByte(help ? 1 : 0);
			is.writeByte(smooth_camera ? 1 : 0);
			is.writeFloat(sensibility_cam);
			is.writeByte(num_opened);
			is.finish();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
