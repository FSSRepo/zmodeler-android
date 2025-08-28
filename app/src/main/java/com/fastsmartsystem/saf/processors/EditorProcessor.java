package com.fastsmartsystem.saf.processors;

import com.fastsmartsystem.saf.*;
import com.forcex.gui.*;
import com.fastsmartsystem.saf.adapters.*;
import com.forcex.gui.widgets.*;
import com.forcex.core.gpu.*;
import com.forcex.*;
import com.forcex.utils.*;
import com.forcex.math.*;

import java.util.*;

import com.forcex.gfx3d.*;
import com.forcex.app.*;
import com.fastsmartsystem.saf.instance.*;

public class EditorProcessor extends PanelFragment implements ListView.OnItemClickListener {
    Layout main;
    public boolean pick_object = true;

    public EditorProcessor() {
        main = Zmdl.lay(false);
        TextView tv_editor = new TextView(Zmdl.gdf());
        tv_editor.setTextSize(0.05f);
        tv_editor.setText(Zmdl.gt("editing_tools"));
        tv_editor.setAlignment(Layout.CENTER);
        tv_editor.setMarginBottom(0.01f);
        main.add(tv_editor);
        MenuAdapter adapter = new MenuAdapter();
        adapter.add(Texture.load("zmdl/play.png"), Zmdl.gt("pose_mode"));
        adapter.add(Texture.load("zmdl/material.png"), Zmdl.gt("materials"));
        adapter.add(Texture.load("zmdl/attach.png"), Zmdl.gt("attach"));
        adapter.add(Texture.load("zmdl/detach.png"), Zmdl.gt("detach"));
        adapter.add(Texture.load("zmdl/play_part.png"), Zmdl.gt("part_player"));
        adapter.add(Texture.load("zmdl/transform.png"), Zmdl.gt("transform"));
        adapter.add(Texture.load("zmdl/uv_mapper.png"), "UV Tools");
        adapter.add(Texture.load("zmdl/transform.png"), Zmdl.gt("vertex_editor"));
        ListView menu = new ListView(0.25f, 0.6f, adapter);
        menu.setInterlinedColor(210, 210, 210, 210);
        menu.setMarginTop(0.02f);
        menu.setOnItemClickListener(this);
        main.add(menu);
    }

    public void requestShow() {
        if (Zmdl.tlay(main)) {
            Zmdl.app().panel.dismiss();
            return;
        }
        Zmdl.apl(main);
    }

    ArrayList<ZObject> candidates = new ArrayList<>();
    ZObject winner = null;
    float distance_current = 1000.0f;

    public void OnPickObject(float x, float y, byte type) {
        if (type != EventType.TOUCH_PRESSED || !Zmdl.im().hasCurrentInstance()) {
            return;
        }
        Camera cam = Zmdl.rp().getCamera();
        Ray ray = cam.getPickRay(x, y);
        winner = null;
        distance_current = 1000.0f;
        // Seleccionar candidatos intersectados por el rayo
        ZInstance inst = Zmdl.inst();
        for (short i = 0; i < inst.getNumModels(); i++) {
            ZObject o = Zmdl.go(inst.getModelHash(i));
            if (o.rayTest(ray)) {
                candidates.add(o);
            }
            o.selected = false;
        }
        inst = null;
        // Discriminar los candidatos mas lejanos de la camara
        for (ZObject c : candidates) {
            float dist_test = cam.getPosition().distance(c.getPosition());
            if (dist_test < distance_current) {
                distance_current = dist_test;
                winner = c;
            }
        }
        candidates.clear();
        if (winner != null) {
            winner.selected = true;
        }
    }

    @Override
    public void onItemClick(ListView view, Object item, short position, boolean longclick) {
        switch (position) {
            case 0:
                Zmdl.app().getPoseTool().requestShow();
                break;
            case 1:
                Zmdl.app().getMaterialEditor().requestShow();
                break;
            case 2:
                if (Zmdl.rp().hasSelected()) {
                    Zmdl.app().getAttachTool().requestShow();
                } else {
                    Toast.warning(Zmdl.gt("select_a_obj"), 4f);
                }
                break;
            case 3:
                if (Zmdl.rp().hasSelected()) {
                    Zmdl.app().getDetachTool().requestShow();
                } else {
                    Toast.warning(Zmdl.gt("select_a_obj"), 4f);
                }
                break;
            case 4:
                Zmdl.app().getPartPlayer().requestShow();
                break;
            case 5:
                if (Zmdl.rp().hasSelected()) {
                    Zmdl.app().getTransformEditor().requestShow();
                } else {
                    Toast.warning(Zmdl.gt("select_a_obj"), 4f);
                }
                break;
            case 6:
                if (Zmdl.rp().hasSelected()) {
                    Zmdl.app().getUVTools().requestShow();
                } else {
                    Toast.warning(Zmdl.gt("select_a_obj"), 4f);
                }
                break;
            case 7:
                if (Zmdl.rp().hasSelected()) {
                    Zmdl.app().getGeometryTools().requestShow();
                } else {
                    Toast.warning(Zmdl.gt("select_a_obj"), 4f);
                }
                break;
        }
    }

    @Override
    public boolean isShowing() {
        return Zmdl.tlay(main);
    }

    @Override
    public void close() {
        if (isShowing()) {
            Zmdl.app().panel.dismiss();
        }
    }
}
