package com.fnadroid.controller;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import java.io.*;
import java.util.*;
import org.json.*;

public class OverlayService extends Service {
    WindowManager wm;
    final ArrayList<View> views = new ArrayList<>();
    JSONObject cfg = new JSONObject();
    java.lang.Process inputProcess;
    BufferedWriter inputWriter;
    String bin;
    final HashSet<String> heldKeys = new HashSet<>();

    @Override public IBinder onBind(Intent i){ return null; }

    @Override public int onStartCommand(Intent i,int flags,int id){
        if(!Settings.canDrawOverlays(this)) return START_NOT_STICKY;
        if(views.isEmpty()) {
            loadConfig();
            startInput();
            setup();
        }
        return START_STICKY;
    }

    void loadConfig(){
        try {
            File f = new File("/sdcard/FNADroidController/config.json");
            cfg = new JSONObject(new String(java.nio.file.Files.readAllBytes(f.toPath())));
        } catch(Exception e) {
            Log.e("FNADroid", "CONFIG LOAD FAILED", e);
            cfg = new JSONObject();
        }
        bin = getApplicationInfo().nativeLibraryDir + "/fnainput";
    }

    void startInput(){
        try {
            inputProcess = new ProcessBuilder("su", "-c", bin).redirectErrorStream(true).start();
            inputWriter = new BufferedWriter(new OutputStreamWriter(inputProcess.getOutputStream()));
        } catch(Exception e) {
            Log.e("FNADroid", "Unhandled overlay exception", e);
        }
    }

    synchronized void send(String action,String key){
        if(inputWriter == null) return;
        try {
            inputWriter.write(action + " " + key);
            inputWriter.newLine();
            inputWriter.flush();
        } catch(Exception e) {
            Log.e("FNADroid", "Unhandled overlay exception", e);
        }
    }

    void keyDown(String key){
        if(key == null || key.isEmpty()) return;
        synchronized(heldKeys) {
            if(heldKeys.add(key)) send("down", key);
        }
    }

    void keyUp(String key){
        if(key == null || key.isEmpty()) return;
        synchronized(heldKeys) {
            if(heldKeys.remove(key)) send("up", key);
        }
    }

    int W(){return getResources().getDisplayMetrics().widthPixels;}
    int H(){return getResources().getDisplayMetrics().heightPixels;}

    WindowManager.LayoutParams lp(int w,int h,int x,int y){
        int type=Build.VERSION.SDK_INT>=26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(
            w,h,type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.LEFT;
        p.x=x; p.y=y;
        return p;
    }

    void setup(){
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);

        try {
            JSONObject jo=cfg.optJSONObject("joystick");
            if(jo != null) {
                float jx=(float)jo.optDouble("x",.15);
                float jy=(float)jo.optDouble("y",.78);
                float size=(float)jo.optDouble("size",.14);
                int jr=(int)(Math.min(W(),H())*size);
                int cx=(int)(W()*jx), cy=(int)(H()*jy);
                JoystickView j=new JoystickView(this,jr);
                views.add(j);
                wm.addView(j,lp(jr*2,jr*2,cx-jr,cy-jr));
            }
        } catch(Exception e) {
            Log.e("FNADroid", "Unhandled overlay exception", e);
        }

        try {
            JSONArray a=cfg.getJSONArray("buttons");
            int r=(int)(Math.min(W(),H())*.105f);
            for(int i=0;i<a.length();i++){
                JSONObject o=a.getJSONObject(i);
                ButtonView v=new ButtonView(
                    this,
                    o.optString("label","BTN"),
                    o.optString("key","SPACE"),
                    r
                );
                int cx=(int)(W()*o.optDouble("x",.8));
                int cy=(int)(H()*o.optDouble("y",.8));
                views.add(v);
                wm.addView(v,lp(r*2,r*2,cx-r,cy-r));
            }
        } catch(Exception e) {
            Log.e("FNADroid", "Unhandled overlay exception", e);
        }
    }

    void releaseAll(){
        synchronized(heldKeys){
            for(String k: new ArrayList<>(heldKeys)) send("up",k);
            heldKeys.clear();
        }
    }

    @Override public void onDestroy(){
        releaseAll();
        try { if(inputWriter != null){ inputWriter.write("quit\n"); inputWriter.flush(); } } catch(Exception e) {
            Log.e("FNADroid", "Unhandled overlay exception", e);
        }
        if(inputProcess != null) inputProcess.destroy();
        for(View v:views) try{wm.removeView(v);} catch(Exception e) {
            Log.e("FNADroid", "Unhandled overlay exception", e);
        }
        views.clear();
        super.onDestroy();
    }

    class ButtonView extends View {
        Paint p=new Paint(3);
        String label,key;
        int r;
        boolean down;

        ButtonView(Context c,String l,String k,int rr){
            super(c); label=l; key=k; r=rr;
            p.setTypeface(Typeface.DEFAULT_BOLD);
        }

        protected void onDraw(Canvas c){
            float cx=getWidth()/2f,cy=getHeight()/2f,rr=r*.86f;
            p.setStyle(Paint.Style.FILL);
            p.setColor(down?0xCFFFFFFF:0x661E2028);
            p.setShadowLayer(10,0,5,0x99000000);
            c.drawCircle(cx,cy,rr,p);
            p.clearShadowLayer();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(4);
            p.setColor(0xB8FFFFFF);
            c.drawCircle(cx,cy,rr,p);
            p.setStyle(Paint.Style.FILL);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(label.length()>3?rr*.25f:rr*.40f);
            p.setColor(0xEEFFFFFF);
            c.drawText(label,cx,cy-(p.ascent()+p.descent())/2,p);
        }

        public boolean onTouchEvent(MotionEvent e){
            int a=e.getActionMasked();
            if(a==MotionEvent.ACTION_DOWN){
                down=true; invalidate(); keyDown(key); return true;
            }
            if(a==MotionEvent.ACTION_UP || a==MotionEvent.ACTION_CANCEL){
                down=false; invalidate(); keyUp(key); return true;
            }
            return true;
        }
    }

    class JoystickView extends View {
        Paint p=new Paint(3);
        int r;
        float kx,ky;
        final HashSet<String> joyHeld=new HashSet<>();
        long lastDraw;

        JoystickView(Context c,int rr){super(c);r=rr;}

        protected void onDraw(Canvas c){
            float cx=getWidth()/2f,cy=getHeight()/2f;
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x66202028);
            c.drawCircle(cx,cy,r*.98f,p);

            p.setColor(0x99FFFFFF);
            c.drawCircle(cx+kx,cy+ky,r*.30f,p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(5);
            p.setColor(0xAAFFFFFF);
            c.drawCircle(cx,cy,r*.98f,p);
            p.setStyle(Paint.Style.FILL);
        }

        void updateDirections(float x,float y){
            float cx=getWidth()/2f,cy=getHeight()/2f;
            float dx=x-cx,dy=y-cy;
            float len=(float)Math.hypot(dx,dy);
            float max=r*.68f;
            if(len>max){dx*=max/len;dy*=max/len;}
            kx=dx; ky=dy; invalidate();

            boolean right = dx > r*.22f;
            boolean left  = dx < -r*.22f;
            boolean down  = dy > r*.22f;
            boolean up    = dy < -r*.22f;

            updateJoy("RIGHT",right);
            updateJoy("LEFT",left);
            updateJoy("DOWN",down);
            updateJoy("UP",up);
        }

        void updateJoy(String key,boolean wanted){
            if(wanted) {
                if(joyHeld.add(key)) keyDown(key);
            } else {
                if(joyHeld.remove(key)) keyUp(key);
            }
        }

        void reset(){
            kx=ky=0;
            for(String k:new ArrayList<>(joyHeld)) keyUp(k);
            joyHeld.clear();
            invalidate();
        }

        public boolean onTouchEvent(MotionEvent e){
            int a=e.getActionMasked();
            if(a==MotionEvent.ACTION_DOWN || a==MotionEvent.ACTION_MOVE){
                updateDirections(e.getX(),e.getY());
                return true;
            }
            if(a==MotionEvent.ACTION_UP || a==MotionEvent.ACTION_CANCEL){
                reset();
                return true;
            }
            return true;
        }
    }
}
