package com.maldawr.chatsimulator;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {
    private static final int CHAT_SOUND=41,CALL_SOUND=42;
    private LinearLayout content;
    private TextView title;
    private int page=0;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        Store.ensureSeeded(this);
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},90);
        build();
    }

    @Override protected void onResume(){super.onResume();if(content!=null)render();}

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Ui.bg(this));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.HORIZONTAL);head.setGravity(Gravity.CENTER_VERTICAL);
        int side=Ui.dp(this,16),baseTop=Ui.dp(this,11),bottom=Ui.dp(this,10);head.setPadding(side,baseTop,side,bottom);
        head.setBackgroundColor(Ui.isDark(this)?0xFF202C33:Ui.brand());
        head.setOnApplyWindowInsetsListener((v,insets)->{int top;if(Build.VERSION.SDK_INT>=30)top=insets.getInsets(WindowInsets.Type.statusBars()).top;else top=insets.getSystemWindowInsetTop();v.setPadding(side,baseTop+top,side,bottom);return insets;});

        title=Ui.label(this,"الدردشات",22,true);title.setTextColor(Color.WHITE);head.addView(title,new LinearLayout.LayoutParams(0,Ui.dp(this,44),1f));
        TextView search=Ui.iconButton(this,"⌕",42,26,Color.TRANSPARENT,Color.WHITE);search.setContentDescription("Search");head.addView(search);
        TextView more=Ui.iconButton(this,"⋮",42,27,Color.TRANSPARENT,Color.WHITE);more.setContentDescription("More");more.setOnClickListener(v->page=2);head.addView(more);
        root.addView(head);

        TextView simulated=Ui.safetyBanner(this);root.addView(simulated);

        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Ui.bg(this));
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setBackgroundColor(Ui.bg(this));
        scroll.addView(content,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setBackgroundColor(Ui.card(this));
        int navBase=Ui.dp(this,5);nav.setPadding(0,navBase,0,navBase);
        nav.setOnApplyWindowInsetsListener((v,insets)->{int bot;if(Build.VERSION.SDK_INT>=30)bot=insets.getInsets(WindowInsets.Type.navigationBars()).bottom;else bot=insets.getSystemWindowInsetBottom();v.setPadding(0,navBase,0,navBase+bot);return insets;});
        addNav(nav,"▣\nالدردشات",0);addNav(nav,"☎\nالمكالمات",1);addNav(nav,"⚙\nالإعدادات",2);
        root.addView(nav);
        setContentView(root);root.requestApplyInsets();render();
    }

    private void addNav(LinearLayout nav,String text,int target){
        TextView x=Ui.label(this,text,12,target==page);x.setGravity(Gravity.CENTER);x.setTextColor(target==page?Ui.brandBright():Ui.sub(this));x.setPadding(0,Ui.dp(this,4),0,Ui.dp(this,3));
        x.setOnClickListener(v->{page=target;render();});nav.addView(x,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1f));
    }

    private void render(){
        content.removeAllViews();
        if(page==0){title.setText("الدردشات");chats();}else if(page==1){title.setText("المكالمات");calls();}else{title.setText("الإعدادات");settings();}
    }

    private void chats(){
        List<Store.Bot> bots=Store.loadBots(this);
        for(int i=0;i<bots.size();i++){
            Store.Bot bot=bots.get(i);
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(this,12),Ui.dp(this,10),Ui.dp(this,12),Ui.dp(this,10));row.setBackgroundColor(Ui.card(this));
            row.addView(Ui.avatar(this,bot,54));

            LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(Ui.dp(this,12),0,Ui.dp(this,8),0);
            LinearLayout line1=new LinearLayout(this);line1.setOrientation(LinearLayout.HORIZONTAL);line1.setGravity(Gravity.CENTER_VERTICAL);
            TextView name=Ui.oneLine(this,bot.name,16,Ui.text(this));name.setTypeface(null,android.graphics.Typeface.BOLD);line1.addView(name,new LinearLayout.LayoutParams(0,Ui.dp(this,25),1f));
            TextView time=Ui.oneLine(this,Store.formatTime(bot.lastTime),11,bot.unread>0?Ui.brandBright():Ui.sub(this));time.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);line1.addView(time);
            info.addView(line1);

            LinearLayout line2=new LinearLayout(this);line2.setOrientation(LinearLayout.HORIZONTAL);line2.setGravity(Gravity.CENTER_VERTICAL);
            TextView preview=Ui.oneLine(this,bot.lastMessage,14,Ui.sub(this));line2.addView(preview,new LinearLayout.LayoutParams(0,Ui.dp(this,27),1f));
            if(bot.unread>0){TextView badge=Ui.label(this,String.valueOf(Math.min(bot.unread,99)),11,true);badge.setGravity(Gravity.CENTER);badge.setTextColor(Color.WHITE);badge.setBackground(Ui.circle(Ui.brandBright()));line2.addView(badge,new LinearLayout.LayoutParams(Ui.dp(this,24),Ui.dp(this,24)));}
            info.addView(line2);row.addView(info,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
            row.setOnClickListener(v->{Intent in=new Intent(this,ChatActivity.class);in.putExtra("bot_id",bot.id);startActivity(in);});
            row.setOnLongClickListener(v->{Intent in=new Intent(this,BotEditorActivity.class);in.putExtra("bot_id",bot.id);startActivity(in);return true;});
            content.addView(row,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,76)));
            if(i<bots.size()-1)content.addView(Ui.divider(this,78));
        }
        TextView add=Ui.iconButton(this,"＋",56,30,Ui.brandBright(),Color.WHITE);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(Ui.dp(this,56),Ui.dp(this,56));ap.gravity=Gravity.END;ap.setMargins(Ui.dp(this,16),Ui.dp(this,18),Ui.dp(this,16),Ui.dp(this,18));add.setLayoutParams(ap);add.setOnClickListener(v->startActivity(new Intent(this,BotEditorActivity.class)));content.addView(add);
    }

    private void calls(){
        List<Store.CallItem> calls=Store.loadCalls(this);for(int i=0;i<calls.size();i++){Store.CallItem c=calls.get(i);Store.Bot bot=Store.getBot(this,c.botId);if(bot==null)continue;
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(Ui.dp(this,12),Ui.dp(this,10),Ui.dp(this,12),Ui.dp(this,10));row.setBackgroundColor(Ui.card(this));row.addView(Ui.avatar(this,bot,50));
            LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(Ui.dp(this,12),0,0,0);info.addView(Ui.label(this,bot.name,16,true));
            String kind="missed".equals(c.type)?"↙ مكالمة فائتة":"incoming".equals(c.type)?"↙ واردة":"↗ صادرة";TextView sub=Ui.label(this,kind+" • "+Store.formatDateTime(c.time),12,false);sub.setTextColor("missed".equals(c.type)?Ui.red():Ui.sub(this));info.addView(sub);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            TextView call=Ui.iconButton(this,"☎",42,20,Color.TRANSPARENT,Ui.brandBright());row.addView(call);content.addView(row,new LinearLayout.LayoutParams(-1,Ui.dp(this,72)));if(i<calls.size()-1)content.addView(Ui.divider(this,74));}
        TextView schedule=Ui.label(this,"＋ جدولة مكالمة محاكاة",15,true);schedule.setTextColor(Ui.brandBright());schedule.setGravity(Gravity.CENTER);schedule.setPadding(0,Ui.dp(this,18),0,Ui.dp(this,18));schedule.setOnClickListener(v->chooseBot());content.addView(schedule);
    }

    private void settings(){
        LinearLayout p=Ui.cardRow(this);p.setOrientation(LinearLayout.VERTICAL);p.addView(Ui.label(this,"مقدمة الرقم الخيالي",15,true));EditText e=new EditText(this);e.setText(Store.getPrefix(this));p.addView(e,new LinearLayout.LayoutParams(-1,-2));TextView save=Ui.label(this,"حفظ",15,true);save.setTextColor(Ui.brandBright());save.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,10));save.setOnClickListener(v->{String q=e.getText().toString().trim();Store.setPrefix(this,q.isEmpty()?"+963":q);Toast.makeText(this,"تم الحفظ",Toast.LENGTH_SHORT).show();});p.addView(save);content.addView(p);
        LinearLayout snd=Ui.cardRow(this);snd.setOrientation(LinearLayout.VERTICAL);TextView s1=Ui.label(this,"صوت إشعار الدردشة",15,true);s1.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,10));s1.setOnClickListener(v->pick(false));snd.addView(s1);TextView s2=Ui.label(this,"رنة المكالمة المحاكية",15,true);s2.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,10));s2.setOnClickListener(v->pick(true));snd.addView(s2);content.addView(snd);
        LinearLayout demo=Ui.cardRow(this);demo.setOrientation(LinearLayout.VERTICAL);demo.addView(Ui.label(this,"بيانات المحاكاة",15,true));TextView reset=Ui.label(this,"إعادة إنشاء دردشات بعدادات 3–4",14,false);reset.setTextColor(Ui.brandBright());reset.setPadding(0,Ui.dp(this,12),0,Ui.dp(this,12));reset.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("إعادة البيانات؟").setPositiveButton("إعادة",(d,w)->{Store.resetDemo(this);render();}).setNegativeButton("إلغاء",null).show());demo.addView(reset);content.addView(demo);
        TextView sys=Ui.label(this,"إعدادات إشعارات Android",14,false);sys.setGravity(Gravity.CENTER);sys.setPadding(0,Ui.dp(this,18),0,Ui.dp(this,18));sys.setOnClickListener(v->{Intent in=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);in.putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(in);});content.addView(sys);
    }

    private void chooseBot(){List<Store.Bot> bots=Store.loadBots(this);String[] n=new String[bots.size()];for(int i=0;i<bots.size();i++)n[i]=bots.get(i).name+"  "+bots.get(i).phone;new AlertDialog.Builder(this).setTitle("اختر شخصية خيالية").setItems(n,(d,w)->delay(bots.get(w))).show();}
    private void delay(Store.Bot bot){String[] n={"10 ثوان","دقيقة","5 دقائق","15 دقيقة","ساعة"};long[] ms={10000,60000,300000,900000,3600000};new AlertDialog.Builder(this).setTitle("متى ترن؟").setItems(n,(d,w)->schedule(bot,ms[w])).show();}
    private void schedule(Store.Bot bot,long ms){Intent in=new Intent(this,CallAlarmReceiver.class);in.putExtra("bot_id",bot.id);PendingIntent pi=PendingIntent.getBroadcast(this,(int)(System.currentTimeMillis()&0x7fffffff),in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);((AlarmManager)getSystemService(ALARM_SERVICE)).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+ms,pi);Toast.makeText(this,"تمت جدولة المكالمة المحاكية",Toast.LENGTH_SHORT).show();}
    private void pick(boolean call){Intent in=new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);in.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,call?RingtoneManager.TYPE_RINGTONE:RingtoneManager.TYPE_NOTIFICATION);in.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,true);in.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,true);startActivityForResult(in,call?CALL_SOUND:CHAT_SOUND);}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK||data==null)return;Uri u=data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);String x=u==null?"":u.toString();if(req==CALL_SOUND)Store.setCallSound(this,x);if(req==CHAT_SOUND)Store.setNotificationSound(this,x);}
    private void icon(String selected){PackageManager pm=getPackageManager();alias(pm,".LauncherGreen","green".equals(selected));alias(pm,".LauncherBlue","blue".equals(selected));alias(pm,".LauncherPurple","purple".equals(selected));Store.setIcon(this,selected);}
    private void alias(PackageManager pm,String a,boolean on){pm.setComponentEnabledSetting(new ComponentName(this,getPackageName()+a),on?PackageManager.COMPONENT_ENABLED_STATE_ENABLED:PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);}
}
