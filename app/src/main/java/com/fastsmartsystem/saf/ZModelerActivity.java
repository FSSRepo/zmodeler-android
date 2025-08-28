package com.fastsmartsystem.saf;

import android.content.*;
import android.os.*;

import com.fastsmartsystem.saf.adapters.*;
import com.fastsmartsystem.saf.editors.*;
import com.fastsmartsystem.saf.instance.*;
import com.fastsmartsystem.saf.processors.*;
import com.fastsmartsystem.saf.store.*;
import com.forcex.*;
import com.forcex.android.*;
import com.forcex.app.*;
import com.forcex.app.threading.*;
import com.forcex.core.*;
import com.forcex.core.gpu.*;
import com.forcex.gui.*;
import com.forcex.gui.widgets.*;
import com.forcex.math.*;
import com.forcex.utils.*;

import android.content.pm.PackageInfo;

public class ZModelerActivity extends ForceXApp {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(new ZModelerApp(this), true);
    }

    public void openStore() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent act = new Intent(getApplicationContext(), ZMStoreScreen.class);
                startActivity(act);
            }
        });
    }

    public static class ZModelerApp extends Game implements View.OnClickListener, ToggleButton.OnToggleListener, InputListener {
        FileProcessor file_proc;
        ProgressScreen pbGlobal;
        AddProcessor add_proc;
        EditorProcessor edit_proc;
        RenderProcessor render_proc;
        InstanceManager instance_manager;
        TextureManager texture_manager;
        public SlidePanel panel;
        public UIContext ctx;
        public LanguageString lang;
        public TaskPool tasks;
        TextView tv_fps;
        public ZTreeAdapter tree_adapter;
        TreeItemProcessor tree_proc;
        MaterialEditor mat_editor;
        TransformEditor trans_editor;
        PartPlayer part_player;
        DetachTool detach_tool;
        AttachTool attach_tool;
        UVEditor uv_mapper;
        UVTools uv_tools;
        SelectorWrapper selector;
        UndoManager undo_manager;
        ZModelerActivity app;
        boolean editing_mode = false;
        public TabView tab_files;
        FixTool fix_tool;
        SettingFile setting_file;
        SettingPanel settings;
        AnimTool anim_tool;
        PoseTools pose_tool;
        GeometryTools geo_tool;
        public DebugInfo debug;
        RayTracingPanel ray_tracing;
        public boolean mandatory_help;

        public ZModelerApp(ZModelerActivity app) {
            this.app = app;
        }

        @Override
        public void create() {

            Zmdl.init(this);
            setting_file = new SettingFile();
            if (setting_file.num_opened >= 10) {
                setting_file.num_opened = 0;
                mandatory_help = setting_file.help;
            }
            setting_file.num_opened++;
            setting_file.save();
            undo_manager = new UndoManager();
            tasks = new TaskPool();
            tasks.start();
            instance_manager = new InstanceManager(this);
            texture_manager = new TextureManager();
            lang = new LanguageString("zmdl/lang" + app.getString(R.string.lang) + ".ls");
            ctx = new UIContext();
            ctx.bindKeyBoard(0.7f);
            Layout main = new Layout(ctx);
            ImageView label = new ImageView(-1, 1, 0.045f);
            label.setIgnoreTouch(true);
            main.add(label);
            TextView tv_label = new TextView(ctx.default_font);
            tv_label.setMarginLeft(0.01f);
            tv_label.setNoApplyConstraintY(true);
            tv_label.setTextSize(0.045f);
            tv_fps = new TextView(ctx.default_font);
            tv_fps.setTextSize(0.045f);
            tv_fps.setNoApplyConstraintY(true);
            tv_fps.setAlignment(Layout.RIGHT);
            main.add(tv_fps);
            main.add(tv_label);
            ImageView iv_toolbar = new ImageView(-1, 1, 0.045f);
            iv_toolbar.setMixColor(210, 210, 210);
            iv_toolbar.setIgnoreTouch(true);
            main.add(iv_toolbar);
            Layout toolbar = new Layout(ctx);
            toolbar.setOrientation(Layout.HORIZONTAL);
            toolbar.setNoApplyConstraintY(true);
            toolbar.setToWrapContent();
            ctx.useHelpTips();
            HelpTip tips = ctx.getHelpTip();
            tips.setDragIcon(Texture.load("zmdl/drag.png"));
            tips.setLanguage(lang);
            createButton(toolbar, "file", 0x4445);
            createButton(toolbar, "add", 0x4446);
            createButton(toolbar, "edit", 0x4447);
            createButton(toolbar, "Render", 0x4448);
            createButton(toolbar, "about", 0x4449);
            createButton(toolbar, "undo", 0x1245);
            createToggleButton(toolbar, "edit_mode", 0x453);
            (toolbar.findViewByID(0x453)).setWidth(0.11f);
            main.add(toolbar);
            Layout viewbar = new Layout(ctx);
            viewbar.setToWrapContent();
            viewbar.setOrientation(Layout.HORIZONTAL);
            viewbar.setAlignment(Layout.RIGHT);
            viewbar.setNoApplyConstraintY(true);
            createToggleButton(viewbar, "zoom", 0x456);
            createToggleButton(viewbar, "pan_mode", 0x457);
            createToggleButton(viewbar, "orbit", 0x458);
            main.add(viewbar);
            tab_files = new TabView(0.1f, 1.0f, 0.045f);
            tab_files.addTab(Zmdl.gt("wel_c"));
            tab_files.setId(0x5678);
            tab_files.setOnTabListener(new TabView.OnTabListener() {
                @Override
                public void onTabClick(String text, byte position, boolean longclick) {
                    if (!instance_manager.hasInstances()) {
                        return;
                    }
                    if (!longclick) {
                        if (instance_manager.hasCurrentInstance() && instance_manager.getCurrentInstance() == instance_manager.get(position)) {
                            return;
                        } else {
                            instance_manager.setInstanceCurrent(instance_manager.get(position));
                            requestCloseAllPanels();
                        }
                    } else {
                        showTabProperties(instance_manager.get(position));
                    }
                }
            });
            main.add(tab_files);
            TextView tvWelcome = new TextView(new Font("fonts/century.fft"));
            tvWelcome.setAnimationScroll(true);
            tvWelcome.setConstraintWidth(0.24f);
            tvWelcome.setText(Zmdl.gt("welcome") + " - Fast Smart System");
            tvWelcome.setTextSize(0.1f);
            tvWelcome.setId(0x2425);
            tvWelcome.setTextColor(24, 100, 240);
            main.add(tvWelcome);
            Layout tvLay = new Layout(ctx);
            tvLay.setToWrapContent();
            ScrollView scroll = new ScrollView(tvLay, 0.35f);
            tvLay.setUseWidthCustom(true);
            tvLay.setWidth(0.25f);
            TextView tvTextInitial = new TextView(new Font("fonts/arial.fft"));
            tvTextInitial.setConstraintWidth(0.25f);
            tvTextInitial.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
            tvTextInitial.setText(Zmdl.gt("recommendations"));
            tvTextInitial.setTextSize(0.035f);
            scroll.setId(0x2426);
            tvLay.add(tvTextInitial);
            main.add(scroll);
            tree_adapter = new ZTreeAdapter(null);
            TreeView treeview = new TreeView(0.25f, 0.4f, tree_adapter);
            treeview.setId(0x7862);
            treeview.setVisibility(View.GONE);
            treeview.setOnTreeListener(new TreeView.OnTreeListener() {
                @Override
                public void onClick(TreeView tree, TreeNode node, boolean longclick) {
                    if (longclick) {
                        if (tree_proc.processBridge((Znode) node)) {
                            tree_proc.requestShow((Znode) node);
                        }
                    } else {
                        node.setExpand(!node.isExpand());
                    }
                }
            });
            main.add(treeview);
            MenuAdapter options = new MenuAdapter();
            options.add(Texture.load("zmdl/zmstore.png"), "ZMStore");
            options.add(Texture.load("zmdl/settings.png"), Zmdl.gt("settings"));
            options.add(Texture.load("zmdl/fix.png"), Zmdl.gt("fix"));
            options.add(Texture.load("zmdl/bug.png"), Zmdl.gt("bug_report"));
            options.add(Texture.load("zmdl/texture.png"), Zmdl.gt("texture_manager"));
            ListView lvOptions = new ListView(0.25f, 0.4f, options);
            lvOptions.setOnItemClickListener(new ListView.OnItemClickListener() {
                @Override
                public void onItemClick(ListView view, Object item, short position, boolean longclick) {
                    switch (position) {
                        case 0:
                            app.openStore();
                            break;
                        case 1:
                            settings.requestShow();
                            break;
                        case 2:
                            fix_tool.requestShow();
                            break;
                        case 3:
                            showBugReport();
                            break;
                        case 4:
                            texture_manager.showDriver(null);
                            break;
                    }
                }
            });
            main.add(lvOptions);
            panel = new SlidePanel(new Layout(ctx));
            panel.setHeight(0.865f);
            panel.setYPosition(-0.135f);
            ImageView render_fade = new ImageView(-1, 0.75f, 0.865f);
            render_fade.setMixColor(new Color(0, 0, 0, 190));
            render_fade.setRelativePosition(0.25f, -0.135f);
            ctx.addUntouchableView(render_fade);
            file_proc = new FileProcessor();
            add_proc = new AddProcessor();
            edit_proc = new EditorProcessor();
            render_proc = new RenderProcessor();
            tree_proc = new TreeItemProcessor();
            mat_editor = new MaterialEditor();
            trans_editor = new TransformEditor();
            part_player = new PartPlayer();
            detach_tool = new DetachTool();
            attach_tool = new AttachTool();
            anim_tool = new AnimTool();
            uv_mapper = new UVEditor();
            fix_tool = new FixTool();
            selector = new SelectorWrapper();
            settings = new SettingPanel();
            uv_tools = new UVTools();
            pose_tool = new PoseTools();
            geo_tool = new GeometryTools();
            ray_tracing = new RayTracingPanel(render_proc.getView());
            debug = new DebugInfo();
            ctx.setSlidePanel(panel);
            ctx.setContentView(main);
            FX.device.addInputListener(this);
            loadHelp();
            pbGlobal = new ProgressScreen(ctx);
            try {
                PackageInfo inf = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
                tv_label.setText("Zmodeler - {|40,200,10:=v" + inf.versionName + "|} " + (Zmdl.rp().isSkinSopported() ? "" : "{|230,45,45:=GPU not supported.|}"));
            } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            }
        }

        public String getText(String id) {
            return lang.get(id);
        }

        private void createButton(Layout toolbar, String text, int id) {
            Button btn = new Button(lang.get(text), ctx.default_font, 0.09f, 0.045f);
            btn.setMarginLeft(0.01f);
            btn.setId(id);
            btn.setRoundBorders(10);
            btn.setOnClickListener(this);
            toolbar.add(btn);
        }

        public void setRenderControlVisible(boolean z) {
            ctx.findViewByID(0x456).setVisibility(z ? View.VISIBLE : View.INVISIBLE);
            ctx.findViewByID(0x457).setVisibility(z ? View.VISIBLE : View.INVISIBLE);
            ctx.findViewByID(0x458).setVisibility(z ? View.VISIBLE : View.INVISIBLE);
        }

        private void createToggleButton(Layout editbar, String text, int id) {
            ToggleButton btn = new ToggleButton(lang.get(text), ctx.default_font, 0.09f, 0.045f);
            btn.setMarginLeft(0.01f);
            btn.setId(id);
            btn.setOnToggleListener(this);
            editbar.add(btn);
        }

        @Override
        public void render(float deltaTime) {
            render_proc.render();
            FX.gl.glClearColor(0.9f, 0.9f, 0.9f, 1.0f);
            FX.gl.glClear(GL.GL_COLOR_BUFFER_BIT);
            ctx.draw();
            tv_fps.setText("" + FX.gpu.getFPS());
            if (selector.isShowing()) {
                selector.update(deltaTime);
            }
            if (anim_tool.isShowing()) {
                anim_tool.update();
            }
            if (time_back > 0) {
                time_back -= deltaTime;
            }
            pbGlobal.update();
        }

        @Override
        public void onTouch(float x, float y, byte type, int pointer) {
            if (pbGlobal.isVisible()) {
                return;
            }
            Vector2f norm = GameUtils.getTouchNormalized(x, y);
            if (ctx.testTouch(norm.x, norm.y)) {
                ctx.onTouch(norm.x, norm.y, type);
                return;
            }
            if (render_proc.getView().testTouch(norm.x, norm.y)) {
                if (!editing_mode || render_proc.cameraMode != 0) {
                    render_proc.onTouch(norm.x, norm.y, type);
                } else {
                    RenderView rv = render_proc.getView();
                    Vector2f n_res = norm.sub(rv.local).multLocal(1f / rv.getWidth(), 1f / rv.getHeight());
                    if (edit_proc.pick_object) {
                        edit_proc.OnPickObject(n_res.x, n_res.y, type);
                        n_res = null;
                        rv = null;
                    } else if (trans_editor.isShowing()) {
                        trans_editor.OnTouch(x, y, type);
                        trans_editor.OnPickAxis(n_res.x, n_res.y, type);
                    } else if (selector.isShowing()) {
                        selector.OnTouch(n_res.x, n_res.y, type);
                    } else if (pose_tool.isShowing()) {
                        pose_tool.OnTouch(n_res.x, n_res.y, type);
                    }
                }
            }
        }

        @Override
        public void OnClick(View view) {
            if (trans_editor.isShowing() ||
                    mat_editor.isShowing() ||
                    attach_tool.isShowing() ||
                    detach_tool.isShowing() ||
                    anim_tool.isShowing() ||
                    pose_tool.isShowing() ||
                    ray_tracing.isShowing()) {
                if (view.getId() == 0x1245) {
                    Toast.info(Zmdl.gt("close_the_cure"), 3f);
                }
                return;
            }
            if (selector.isShowing()) {
                selector.cancel();
            }
            switch (view.getId()) {
                case 0x4445:
                    file_proc.requestShow();
                    break;
                case 0x4446:
                    add_proc.requestShow();
                    break;
                case 0x4447:
                    edit_proc.requestShow();
                    break;
                case 0x4448:
                    render_proc.requestShow();
                    break;
                case 0x1245:
                    undo_manager.undo();
                    break;
            }
        }

        public boolean isEditMode() {
            return editing_mode;
        }

        @Override
        public void onToggle(ToggleButton btn, boolean z) {
            switch (btn.getId()) {
                case 0x456:
                    selector.switchCamControl(z);
                    ((ToggleButton) ctx.findViewByID(0x457)).setToggle(false);
                    ((ToggleButton) ctx.findViewByID(0x458)).setToggle(false);
                    render_proc.cameraMode = z ? 1 : 0;
                    debug.print("Camera Zoom: " + (z ? "{|0,255,0:=ON|}" : "{|255,0,0:=OFF|}"));
                    break;
                case 0x457:
                    selector.switchCamControl(z);
                    ((ToggleButton) ctx.findViewByID(0x456)).setToggle(false);
                    ((ToggleButton) ctx.findViewByID(0x458)).setToggle(false);
                    render_proc.cameraMode = z ? 2 : 0;
                    debug.print("Camera Pan Mode: " + (z ? "{|0,255,0:=ON|}" : "{|255,0,0:=OFF|}"));
                    break;
                case 0x458:
                    selector.switchCamControl(z);
                    ((ToggleButton) ctx.findViewByID(0x456)).setToggle(false);
                    ((ToggleButton) ctx.findViewByID(0x457)).setToggle(false);
                    render_proc.cameraMode = z ? 3 : 0;
                    debug.print("Camera Orbit: " + (z ? "{|0,255,0:=ON|}" : "{|255,0,0:=OFF|}"));
                    break;
                case 0x453:
                    editing_mode = z;
                    break;
            }
        }

        public void showBugReport() {
            Layout lay = new Layout(ctx);
            lay.setUseWidthCustom(true);
            lay.setWidth(0.35f);
            final Dialog diag = new Dialog(lay);
            Button btnGmail = new Button(" Gmail", ctx.default_font, 0.3f, 0.07f);
            btnGmail.setTextSize(0.05f);
            btnGmail.setApplyAspectRatio(true);
            btnGmail.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void OnClick(View view) {
                    diag.dismiss();
                    sendEmailDialog(false);
                }
            });
            btnGmail.setBackgroundColor(0xD3, 0x40, 0x40);
            btnGmail.setIconTexture(Texture.load("zmdl/gmail.png"));
            lay.add(btnGmail);
            btnGmail.setAlignment(Layout.CENTER);

            diag.setTitle(Zmdl.gt("bug_report"));
            diag.show();
        }

        public void sendEmailDialog(final boolean dff_fail) {
            Layout lay = new Layout(ctx);
            final Dialog diag = new Dialog(lay);
            final EditText etMsg = new EditText(ctx, 0.5f, 0.2f, 0.05f);
            etMsg.setAutoFocus(true);
            etMsg.setHint(Zmdl.gt("message"));
            TextView tvInclude = new TextView(ctx.default_font);
            tvInclude.setConstraintWidth(0.5f);
            String text = Zmdl.gt("include_sc");
            tvInclude.setTextSize(0.045f);
            if (dff_fail) {
                text += "\n" + Zmdl.gt("dff_fail_email");
            }
            tvInclude.setText(text);
            Button btnSend = new Button(Zmdl.gt("send"), ctx.default_font, 0.2f, 0.05f);
            btnSend.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void OnClick(View view) {
//                    app.sendEmail((dff_fail ? "DFF Failure:\n" : "") + etMsg.getText());
                    diag.dismiss();
                    etMsg.detachKeyBoard();
                }
            });
            btnSend.setAlignment(Layout.CENTER);
            lay.add(etMsg);
            lay.add(tvInclude);
            lay.add(btnSend);
            diag.setTitle(Zmdl.gt("bug_report"));
            diag.show(0, 0.5f);
        }

        float time_back = 0;

        @Override
        public int pause(int type) {
            if (pbGlobal.isVisible()) {
                Toast.info(Zmdl.gt("th_nfp"), 3f);
                return EventType.NOTHING;
            }
            if (type == EventType.BACK_BUTTON) {
                if (time_back > 0) {
                    return EventType.REQUEST_EXIT;
                } else {
                    time_back = 2f;
                    Toast.info("Press back again", 3f);
                }
            }
            return EventType.NOTHING;
        }

        public InstanceManager getInstanceManager() {
            return instance_manager;
        }

        public MaterialEditor getMaterialEditor() {
            return mat_editor;
        }

        public TransformEditor getTransformEditor() {
            return trans_editor;
        }

        public PartPlayer getPartPlayer() {
            return part_player;
        }

        public DetachTool getDetachTool() {
            return detach_tool;
        }

        public AttachTool getAttachTool() {
            return attach_tool;
        }

        public FixTool getFixTool() {
            return fix_tool;
        }

        public UVEditor getUVMapper() {
            return uv_mapper;
        }

        public UVTools getUVTools() {
            return uv_tools;
        }

        public AnimTool getAnimTool() {
            return anim_tool;
        }

        public PoseTools getPoseTool() {
            return pose_tool;
        }

        public RayTracingPanel getRayTracing() {
            return ray_tracing;
        }

        public SelectorWrapper getSelectorWrap() {
            return selector;
        }

        @Override
        public void onKeyEvent(byte key, boolean down) {
            // TODO: Implement this method
        }

        public RenderProcessor getRenderProcessor() {
            return render_proc;
        }

        public EditorProcessor getEditorProcessor() {
            return edit_proc;
        }

        public GeometryTools getGeometryTools() {
            return geo_tool;
        }

        public TextureManager getTextureManager() {
            return texture_manager;
        }

        public void requestCloseAllPanels() {
            trans_editor.close();
            mat_editor.close();
            attach_tool.close();
            tree_proc.close();
            detach_tool.close();
            selector.close();
            part_player.close();
            settings.close();
            fix_tool.close();
            uv_tools.close();
            uv_mapper.close();
            anim_tool.close();
            Zmdl.rp().unselectAll();
        }

        public void tip(String id, View pointer, int drag) {
            if (!mandatory_help) return;
            if (ctx.getHelpTip() == null) {
                return;
            }
            if (drag == 0) {
                ctx.getHelpTip().add(Zmdl.gt(id + "t"), Zmdl.gt(id + "i"), pointer);
            } else if (drag == 1) {
                ctx.getHelpTip().addAndDragX(Zmdl.gt(id + "t"), Zmdl.gt(id + "i"), pointer);
            } else if (drag == 2) {
                ctx.getHelpTip().addAndDragY(Zmdl.gt(id + "t"), Zmdl.gt(id + "i"), pointer);
            }
        }

        public void tip(String id, int viewID, int drag) {
            if (!mandatory_help) return;
            if (ctx.getHelpTip() == null) {
                return;
            }
            if (drag == 0) {
                ctx.getHelpTip().add(Zmdl.gt(id + "t"), Zmdl.gt(id + "i"), ctx.findViewByID(viewID));
            } else if (drag == 1) {
                ctx.getHelpTip().addAndDragX(Zmdl.gt(id + "t"), Zmdl.gt(id + "i"), ctx.findViewByID(viewID));
            } else if (drag == 2) {
                ctx.getHelpTip().addAndDragY(Zmdl.gt(id + "t"), Zmdl.gt(id + "i"), ctx.findViewByID(viewID));
            }
        }

        private void loadHelp() {
            // Main buttons
            tip("file", 0x4445, 0);
            tip("add", 0x4446, 0);
            tip("edit", 0x4447, 0);
            tip("Render", 0x4448, 0);
            tip("about", 0x4449, 0);
            tip("undo", 0x1245, 0);
            tip("editmode", 0x453, 0);

            // Control butons
            tip("zoom", 0x456, 0);
            tip("pan", 0x457, 0);
            tip("orbit", 0x458, 0);
            tip("tabv", 0x5678, 0);
        }

        public ProgressScreen getProgressScreen() {
            return pbGlobal;
        }

        @Override
        public void resume() {

        }

        private void showTabProperties(final ZInstance inst) {
            Layout lay = new Layout(Zmdl.ctx());
            lay.setWidth(0.4f);
            lay.setUseWidthCustom(true);
            final Dialog diag = new Dialog(lay);
            Layout prop = new Layout(Zmdl.ctx());
            ScrollView scroll = new ScrollView(prop, 0.3f);
            prop.setToWrapContent();
            ToggleButton tbShow = new ToggleButton(Zmdl.gt("show"), Zmdl.gdf(), 0.2f, 0.05f);
            tbShow.setMarginTop(0.01f);
            tbShow.setAlignment(Layout.CENTER);
            final TextView tvProps = new TextView(Zmdl.gdf());
            prop.setWidth(0.4f);
            prop.setUseWidthCustom(true);
            tvProps.setTextSize(0.05f);
            tvProps.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
            tbShow.setToggle(inst.model_visible);
            tbShow.setOnToggleListener(new ToggleButton.OnToggleListener() {
                @Override
                public void onToggle(ToggleButton btn, boolean z) {
                    inst.model_visible = z;
                    Zmdl.rp().testVisibilityFacts();
                }
            });
            tvProps.setText(Zmdl.gt("inst_props",
                    inst.name,
                    (inst.type == 1 ? "DFF" : (inst.type == 2 ? "OBJ" : "3DS")),
                    inst.getNumModels(),
                    inst.type == 1 && inst.error_stack.size() > 0 ? Zmdl.gt("has_errors").replace('#', '=') : Zmdl.gt("no_errors").replace('#', '='),
                    inst.using_this ? Zmdl.gt("yes") : Zmdl.gt("no")));
            prop.add(tvProps);
            lay.add(scroll);
            lay.add(tbShow);
            Button btnAccept = new Button(Zmdl.gt("accept"), Zmdl.gdf(), 0.12f, 0.05f);
            btnAccept.setMarginTop(0.03f);
            btnAccept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void OnClick(View view) {
                    diag.dismiss();
                }
            });
            btnAccept.setAlignment(Layout.CENTER);
            lay.add(btnAccept);
            diag.setTitle(Zmdl.gt("properties"));
            diag.show();
        }

        @Override
        public void destroy() {
            ctx.destroy();
            Zmdl.destroy();
            ctx = null;
            lang = null;
        }
    }
}
