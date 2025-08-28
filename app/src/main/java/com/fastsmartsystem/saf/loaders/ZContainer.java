package com.fastsmartsystem.saf.loaders;
import com.fastsmartsystem.saf.*;
import java.util.*;

public class ZContainer {
	public Znode root;
	public int dff_offset;
	public String dff_path = "";
	public boolean from_store;
	public String name;
	public ArrayList<ZObject> objects = new ArrayList<>();
}
