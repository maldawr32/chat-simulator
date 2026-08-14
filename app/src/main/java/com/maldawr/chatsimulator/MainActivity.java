package com.maldawr.chatsimulator;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {
    private static final int CHAT_SOUND = 41, CALL_SOUND = 42;
    private LinearLayout content;
    private int page = 0;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        Store.ensureSeeded(this);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 90);
        build();
    }

    @Override protected void onResume() { super.onResume(); if (content != null) render(); }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Ui.bg(this)); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout head = new LinearLayout(this); head.setOrientation(LinearLayout.VERTICAL); head.setPadding(Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,12)); head.setBackgroundColor(Ui.teal());
        TextView t=Ui.label(this,"Chat Simulator",24,true); t.setTextColor(0xffffffff); head.addView(t);
        TextView s=Ui.label(this,"Fictional local bots",12,false); s.setTextColor(0xffd9f0f1); head.addView(s); root.addView(head); root.addView(Ui.safetyBanner(this));
        ScrollView scroll=new ScrollView(this); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(0,Ui.dp(this,6),0,Ui.dp(this,16)); scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setBackgroundColor(Ui.card(this));
        String[] names={"Chats","Calls","Settings"}; for(int i=0;i<3;i++){ final int p=i; Button x=Ui.button(this,names[i]); x.setOnClickListener(v->{page=p;render();}); nav.addView(x,new LinearLayout.LayoutParams(0,-2,1)); }
        root.addView(nav); setContentView(root); render();
    }

    private void render(){ content.removeAllViews(); if(page==0) chats(); else if(page==1) calls(); else settings(); }

    private void chats(){
        LinearLayout top=Ui.cardRow(this); top.addView(Ui.label(this,"Fictional conversations",18,true),new LinearLayout.LayoutParams(0,-2,1)); Button add=Ui.button(this,"+ Bot"); add.setOnClickListener(v->startActivity(new Intent(this,BotEditorActivity.class))); top.addView(add); content.addView(top);
        for(Store.Bot bot:Store.loadBots(this)){
            LinearLayout row=Ui.cardRow(this); row.addView(Ui.avatar(this,bot,50)); LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(Ui.dp(this,10),0,Ui.dp(this,6),0); info.addView(Ui.label(this,bot.name,16,true)); TextView p=Ui.label(this,bot.lastMessage,12,false); p.setTextColor(Ui.sub(this)); info.addView(p); row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            TextView badge=Ui.label(this,bot.unread>999?"999+":String.valueOf(bot.unread),12,true); badge.setGravity(Gravity.CENTER); badge.setTextColor(0xffffffff); badge.setPadding(10,5,10,5); badge.setBackground(Ui.rounded(Ui.teal(),16,this)); row.addView(badge);
            row.setOnClickListener(v->{Intent i=new Intent(this,ChatActivity.class);i.putExtra("bot_id",bot.id);startActivity(i);}); row.setOnLongClickListener(v->{Intent i=new Intent(this,BotEditorActivity.class);i.putExtra("bot_id",bot.id);startActivity(i);return true;}); content.addView(row);
        }
    }

    private void calls(){
        LinearLayout top=Ui.cardRow(this); top.addView(Ui.label(this,"Simulated call history",18,true),new LinearLayout.LayoutParams(0,-2,1)); Button b=Ui.button(this,"Schedule"); b.setOnClickListener(v->chooseBot()); top.addView(b); content.addView(top);
        for(Store.CallItem c:Store.loadCalls(this)){ Store.Bot bot=Store.getBot(this,c.botId); if(bot==null)continue; LinearLayout r=Ui.cardRow(this); r.addView(Ui.avatar(this,bot,44)); LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(Ui.dp(this,10),0,0,0);x.addView(Ui.label(this,bot.name,15,true)); TextView y=Ui.label(this,("missed".equals(c.type)?"Missed":"incoming".equals(c.type)?"Incoming":"Outgoing")+" simulated call",12,false);y.setTextColor("missed".equals(c.type)?Ui.red():Ui.sub(this));x.addView(y);TextView z=Ui.label(this,Store.formatDateTime(c.time),11,false);z.setTextColor(Ui.sub(this));x.addView(z);r.addView(x);content.addView(r); }
    }

    private void settings(){
        LinearLayout p=Ui.cardRow(this);p.setOrientation(LinearLayout.VERTICAL);p.addView(Ui.label(this,"Fictional calling code",16,true));EditText e=new EditText(this);e.setText(Store.getPrefix(this));p.addView(e,new LinearLayout.LayoutParams(-1,-2));Button save=Ui.button(this,"Save");save.setOnClickListener(v->{String q=e.getText().toString().trim();Store.setPrefix(this,q.isEmpty()?"+963":q);Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show();});p.addView(save);content.addView(p);
        LinearLayout snd=Ui.cardRow(this);snd.setOrientation(LinearLayout.VERTICAL);snd.addView(Ui.label(this,"Sounds",16,true));Button cs=Ui.button(this,"Choose chat notification sound");cs.setOnClickListener(v->pick(false));snd.addView(cs);Button rs=Ui.button(this,"Choose simulated call ringtone");rs.setOnClickListener(v->pick(true));snd.addView(rs);content.addView(snd);
        LinearLayout icons=Ui.cardRow(this);icons.setOrientation(LinearLayout.VERTICAL);icons.addView(Ui.label(this,"Launcher icon",16,true));for(String n:new String[]{"green","blue","purple"}){Button ib=Ui.button(this,n);ib.setOnClickListener(v->icon(n));icons.addView(ib);}content.addView(icons);
        LinearLayout demo=Ui.cardRow(this);demo.setOrientation(LinearLayout.VERTICAL);demo.addView(Ui.label(this,"Demo dataset",16,true));Button reset=Ui.button(this,"Reset hundreds of unread messages");reset.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Reset demo data?").setPositiveButton("Reset",(d,w)->{Store.resetDemo(this);render();}).setNegativeButton("Cancel",null).show());demo.addView(reset);content.addView(demo);
        Button sys=Ui.button(this,"Android notification settings");sys.setOnClickListener(v->{Intent i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);i.putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(i);});content.addView(sys);
    }

    private void chooseBot(){ List<Store.Bot> bots=Store.loadBots(this);String[] n=new String[bots.size()];for(int i=0;i<bots.size();i++)n[i]=bots.get(i).name+"  "+bots.get(i).phone;new AlertDialog.Builder(this).setTitle("Choose fictional bot").setItems(n,(d,w)->delay(bots.get(w))).show(); }
    private void delay(Store.Bot bot){String[] n={"10 seconds","1 minute","5 minutes","15 minutes","60 minutes"};long[] ms={10000,60000,300000,900000,3600000};new AlertDialog.Builder(this).setTitle("When should it ring?").setItems(n,(d,w)->schedule(bot,ms[w])).show();}
    private void schedule(Store.Bot bot,long ms){Intent i=new Intent(this,CallAlarmReceiver.class);i.putExtra("bot_id",bot.id);PendingIntent pi=PendingIntent.getBroadcast(this,(int)(System.currentTimeMillis()&0x7fffffff),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);((AlarmManager)getSystemService(ALARM_SERVICE)).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+ms,pi);Toast.makeText(this,"Simulated call scheduled",Toast.LENGTH_SHORT).show();}
    private void pick(boolean call){Intent i=new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,call?RingtoneManager.TYPE_RINGTONE:RingtoneManager.TYPE_NOTIFICATION);i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,true);i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,true);startActivityForResult(i,call?CALL_SOUND:CHAT_SOUND);}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK||data==null)return;Uri u=data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);String x=u==null?"":u.toString();if(req==CALL_SOUND)Store.setCallSound(this,x);if(req==CHAT_SOUND)Store.setNotificationSound(this,x);}
    private void icon(String selected){PackageManager pm=getPackageManager();alias(pm,".LauncherGreen","green".equals(selected));alias(pm,".LauncherBlue","blue".equals(selected));alias(pm,".LauncherPurple","purple".equals(selected));Store.setIcon(this,selected);}
    private void alias(PackageManager pm,String a,boolean on){pm.setComponentEnabledSetting(new ComponentName(this,getPackageName()+a),on?PackageManager.COMPONENT_ENABLED_STATE_ENABLED:PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);}
}
