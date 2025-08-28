package com.fastsmartsystem.saf.instance;
import java.util.*;
import com.forcex.gui.*;
import com.forcex.utils.*;
import com.fastsmartsystem.saf.*;

public class UndoManager {
	public HashMap<Integer,UndoListener> undo_listeners = new HashMap<>();
	
	public void addListener(UndoListener listener,int type) {
		undo_listeners.put(type,listener);
	}
	
	public void addUndoData(Object data,int type,String name) {
		UndoHistory h = new UndoHistory();
		h.data = data;
		h.type = type;
		h.name = name;
		Zmdl.inst().undo_historial.add(h);
	}
	
	public void undo(){
		if(!Zmdl.im().hasCurrentInstance() || Zmdl.inst().undo_historial.size() == 0){
			Toast.info(Zmdl.gt("no_step"),2f);
			return;
		}
		UndoHistory last = Zmdl.inst().undo_historial.get(Zmdl.inst().undo_historial.size() - 1);
		UndoListener result = undo_listeners.get(last.type);
		if(result != null){
			result.undo(last.data);
		}
		Zmdl.inst().undo_historial.remove(last);
	}
	
	public ArrayList<Object> getDataFromType(int type){
		ArrayList<Object> o = new ArrayList<>();
		for(UndoHistory h : Zmdl.inst().undo_historial){
			if(h.type == type){
				o.add(h.data);
			}
		}
		return o;
	}
	
	public void removeFromData(int type,Object data){
		ListIterator<UndoHistory> it = Zmdl.inst().undo_historial.listIterator();
		while(it.hasNext()){
			UndoHistory h = it.next();
			if(h.type == type && h.data == data){
				it.remove();
			}
		}
	}
	
	public boolean checkHas(int type){
		for(UndoHistory h : Zmdl.inst().undo_historial){
			if(h.type == type){
				return true;
			}
		}
		return false;
	}
}
