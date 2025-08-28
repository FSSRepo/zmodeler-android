package com.fastsmartsystem.saf.instance;
import com.fastsmartsystem.saf.*;

public class Bridge {
	public Znode node = null;
	public ZInstance src = null;
	
	public void dispose() {
		node = null;
		src = null;
	}
}
