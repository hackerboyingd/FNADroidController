package com.fnadroid.controller;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    public void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL); l.setPadding(32,32,32,32);
        l.setBackgroundColor(Color.rgb(18,18,22));
        TextView t=new TextView(this); t.setText("FNADroid Controller 2.0"); t.setTextSize(24); t.setTextColor(Color.WHITE);
        l.addView(t);
        Button p=new Button(this); p.setText("Разрешение оверлея");
        p.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())));}catch(Exception e){startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));}});
        l.addView(p);
        Button s=new Button(this); s.setText("ЗАПУСТИТЬ КОНТРОЛЛЕР");
        s.setOnClickListener(v->{if(Settings.canDrawOverlays(this)) startService(new Intent(this,OverlayService.class));});
        l.addView(s);
        Button x=new Button(this); x.setText("ОСТАНОВИТЬ"); x.setOnClickListener(v->stopService(new Intent(this,OverlayService.class))); l.addView(x);
        setContentView(l);
    }
}
