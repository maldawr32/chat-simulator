package com.maldawr.chatsimulator;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Random;

public class ChatActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Random random=new Random();
    private long botId; private Store.Bot bot; private LinearLayout messages; private ScrollView scroll; private EditText input; private TextView send;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        Store.ensureSeeded(this);botId=getIntent().getLongExtra("bot_id",-1L);bot=Store.getBot(this,botId);if(bot==null){finish();return;}
        bot.unread=0;Store.saveBot(this,bot);buildUi();renderMessages();
    }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Ui.chatBg(this));root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);top.setBackgroundColor(Ui.isDark(this)?0xFF202C33:Ui.brand());
        int side=Ui.dp(this,8),baseTop=Ui.dp(this,6),bottom=Ui.dp(this,7);top.setPadding(side,baseTop,side,bottom);
        top.setOnApplyWindowInsetsListener((v,insets)->{int t;if(Build.VERSION.SDK_INT>=30)t=insets.getInsets(WindowInsets.Type.statusBars()).top;else t=insets.getSystemWindowInsetTop();v.setPadding(side,baseTop+t,side,bottom);return insets;});

        TextView back=Ui.iconButton(this,"‹",42,34,Color.TRANSPARENT,Color.WHITE);back.setContentDescription("Back");back.setOnClickListener(v->finish());top.addView(back);
        top.addView(Ui.avatar(this,bot,42));
        LinearLayout meta=new LinearLayout(this);meta.setOrientation(LinearLayout.VERTICAL);meta.setPadding(Ui.dp(this,10),0,Ui.dp(this,6),0);
        TextView name=Ui.oneLine(this,bot.name,16,Color.WHITE);name.setTypeface(null,android.graphics.Typeface.BOLD);meta.addView(name,new LinearLayout.LayoutParams(-1,Ui.dp(this,24)));
        TextView status=Ui.oneLine(this,"متصل الآن • محاكاة",11,0xFFD8E7E4);meta.addView(status,new LinearLayout.LayoutParams(-1,Ui.dp(this,18)));top.addView(meta,new LinearLayout.LayoutParams(0,-2,1f));
        TextView video=Ui.iconButton(this,"▻",40,24,Color.TRANSPARENT,Color.WHITE);top.addView(video);TextView phone=Ui.iconButton(this,"☎",40,20,Color.TRANSPARENT,Color.WHITE);top.addView(phone);TextView menu=Ui.iconButton(this,"⋮",36,26,Color.TRANSPARENT,Color.WHITE);menu.setOnClickListener(v->chooseMessageDelay());top.addView(menu);root.addView(top);

        TextView badge=Ui.safetyBanner(this);badge.setBackgroundColor(Ui.chatBg(this));root.addView(badge);

        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Ui.chatBg(this));messages=new LinearLayout(this);messages.setOrientation(LinearLayout.VERTICAL);messages.setPadding(Ui.dp(this,6),Ui.dp(this,8),Ui.dp(this,6),Ui.dp(this,10));scroll.addView(messages,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));

        LinearLayout composer=new LinearLayout(this);composer.setOrientation(LinearLayout.HORIZONTAL);composer.setGravity(Gravity.BOTTOM|Gravity.CENTER_VERTICAL);composer.setBackgroundColor(Ui.chatBg(this));
        int cb=Ui.dp(this,5);composer.setPadding(Ui.dp(this,6),cb,Ui.dp(this,6),cb);
        composer.setOnApplyWindowInsetsListener((v,insets)->{int botInset;if(Build.VERSION.SDK_INT>=30)botInset=insets.getInsets(WindowInsets.Type.navigationBars()).bottom;else botInset=insets.getSystemWindowInsetBottom();v.setPadding(Ui.dp(this,6),cb,Ui.dp(this,6),cb+botInset);return insets;});

        LinearLayout field=new LinearLayout(this);field.setOrientation(LinearLayout.HORIZONTAL);field.setGravity(Gravity.CENTER_VERTICAL);field.setBackground(Ui.rounded(Ui.isDark(this)?0xFF202C33:Color.WHITE,24,this));field.setPadding(Ui.dp(this,8),0,Ui.dp(this,8),0);
        TextView emoji=Ui.iconButton(this,"☺",38,22,Color.TRANSPARENT,Ui.sub(this));field.addView(emoji);
        input=new EditText(this);input.setSingleLine(false);input.setMaxLines(5);input.setHint("رسالة");input.setTextSize(16);input.setTextColor(Ui.text(this));input.setHintTextColor(Ui.sub(this));input.setBackgroundColor(Color.TRANSPARENT);input.setPadding(Ui.dp(this,4),Ui.dp(this,8),Ui.dp(this,4),Ui.dp(this,8));input.setImeOptions(EditorInfo.IME_ACTION_SEND);field.addView(input,new LinearLayout.LayoutParams(0,-2,1f));
        TextView attach=Ui.iconButton(this,"⌕",38,22,Color.TRANSPARENT,Ui.sub(this));field.addView(attach);TextView camera=Ui.iconButton(this,"◉",38,20,Color.TRANSPARENT,Ui.sub(this));field.addView(camera);
        composer.addView(field,new LinearLayout.LayoutParams(0,-2,1f));

        send=Ui.iconButton(this,"🎙",50,20,Ui.brandBright(),Color.WHITE);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(Ui.dp(this,50),Ui.dp(this,50));sp.setMargins(Ui.dp(this,5),0,0,0);send.setLayoutParams(sp);send.setOnClickListener(v->{if(input.getText().toString().trim().isEmpty())Toast.makeText(this,"اكتب رسالة أولاً",Toast.LENGTH_SHORT).show();else sendMessage();});composer.addView(send);root.addView(composer);

        input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){send.setText(s.toString().trim().isEmpty()?"🎙":"➤");}public void afterTextChanged(Editable e){}});
        input.setOnEditorActionListener((v,actionId,event)->{if(actionId==EditorInfo.IME_ACTION_SEND&&!input.getText().toString().trim().isEmpty()){sendMessage();return true;}return false;});
        setContentView(root);root.requestApplyInsets();
    }

    private void renderMessages(){messages.removeAllViews();List<Store.Message> list=Store.loadMessages(this,botId);long lastDay=-1;for(Store.Message m:list){long day=m.time/86_400_000L;if(day!=lastDay){TextView d=Ui.label(this,day==(System.currentTimeMillis()/86_400_000L)?"اليوم":"رسائل سابقة",11,false);d.setTextColor(Ui.sub(this));d.setGravity(Gravity.CENTER);d.setBackground(Ui.rounded(Ui.isDark(this)?0xFF182229:0xFFE2F2F6,7,this));LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-2,Ui.dp(this,28));dp.gravity=Gravity.CENTER;dp.setMargins(0,Ui.dp(this,6),0,Ui.dp(this,6));messages.addView(d,dp);lastDay=day;}addBubble(m);}messages.post(()->scroll.fullScroll(View.FOCUS_DOWN));}

    private void addBubble(Store.Message m){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);row.setGravity(m.incoming?Gravity.START:Gravity.END);row.setPadding(Ui.dp(this,5),Ui.dp(this,2),Ui.dp(this,5),Ui.dp(this,2));
        LinearLayout bubble=new LinearLayout(this);bubble.setOrientation(LinearLayout.VERTICAL);bubble.setPadding(Ui.dp(this,9),Ui.dp(this,6),Ui.dp(this,9),Ui.dp(this,5));bubble.setBackground(Ui.rounded(m.incoming?(Ui.isDark(this)?0xFF202C33:Color.WHITE):(Ui.isDark(this)?0xFF005C4B:0xFFD9FDD3),9,this));
        TextView text=Ui.label(this,m.text,15,false);text.setTextColor(Ui.text(this));text.setMaxWidth((int)(getResources().getDisplayMetrics().widthPixels*0.74f));text.setGravity(Gravity.START);bubble.addView(text);
        LinearLayout meta=new LinearLayout(this);meta.setOrientation(LinearLayout.HORIZONTAL);meta.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);TextView time=Ui.label(this,Store.formatTime(m.time),10,false);time.setTextColor(Ui.sub(this));meta.addView(time);if(!m.incoming){TextView ticks=Ui.label(this," ✓✓",11,true);ticks.setTextColor(0xFF53BDEB);meta.addView(ticks);}bubble.addView(meta);
        row.addView(bubble,new LinearLayout.LayoutParams(-2,-2));messages.addView(row,new LinearLayout.LayoutParams(-1,-2));
    }

    private void playSound(int resId){try{MediaPlayer mp=MediaPlayer.create(this,resId);if(mp==null)return;mp.setOnCompletionListener(MediaPlayer::release);mp.start();}catch(Exception ignored){}}

    private void sendMessage(){String text=input.getText().toString().trim();if(text.isEmpty())return;long now=System.currentTimeMillis();Store.addMessage(this,new Store.Message(now,botId,text,false,now));bot.lastMessage=text;bot.lastTime=now;bot.unread=0;Store.saveBot(this,bot);input.setText("");playSound(R.raw.message_outgoing);renderMessages();if(!bot.autoReply)return;if(!Store.isBotActive(bot))return;long delay=900L+random.nextInt(1700);handler.postDelayed(()->{Store.Bot latest=Store.getBot(this,botId);if(latest==null||isFinishing())return;String reply=Store.smartReply(text);long t=System.currentTimeMillis();Store.addMessage(this,new Store.Message(t,botId,reply,true,t));latest.lastMessage=reply;latest.lastTime=t;latest.unread=0;Store.saveBot(this,latest);bot=latest;playSound(R.raw.message_incoming);renderMessages();},delay);}

    private void chooseMessageDelay(){String[] labels={"رسالة بعد 10 ثوان","بعد دقيقة","بعد 5 دقائق","بعد 15 دقيقة"};long[] delays={10_000L,60_000L,300_000L,900_000L};new AlertDialog.Builder(this).setTitle("جدولة رسالة واردة محاكية").setItems(labels,(d,w)->scheduleMessage(delays[w])).show();}
    private void scheduleMessage(long delayMs){Intent in=new Intent(this,MessageAlarmReceiver.class);in.putExtra("bot_id",botId);int requestCode=(int)(System.currentTimeMillis()&0x7fffffff);PendingIntent pi=PendingIntent.getBroadcast(this,requestCode,in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+delayMs,pi);Toast.makeText(this,"تمت جدولة الرسالة",Toast.LENGTH_SHORT).show();}
}
