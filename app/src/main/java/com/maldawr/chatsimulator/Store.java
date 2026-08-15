package com.maldawr.chatsimulator;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class Store {
    private static final String PREFS="chat_simulator_settings_v6";
    private static final String KEY_PREFIX="prefix", KEY_CALL_SOUND="call_sound", KEY_ICON="icon", KEY_ICON_STYLE="icon_style";
    private static final Random RANDOM=new Random();
    private Store(){}

    public static final class Bot {
        public long id; public String name,phone,status,lastMessage,avatarUri,replyMode,groupSubtitle,lastSender;
        public int unread,activeFrom,activeTo,maxBurst; public long lastTime;
        public boolean autoReply,initiative,favorite,groupChat;
        public Bot(long id,String name,String phone){this.id=id;this.name=name;this.phone=phone;status="Fictional bot";unread=0;lastMessage="Simulation ready";lastTime=System.currentTimeMillis();autoReply=true;avatarUri="";activeFrom=0;activeTo=24;replyMode="natural";initiative=true;maxBurst=3;favorite=false;groupChat=false;groupSubtitle="";lastSender="";}
    }
    public static final class Message { public long id,botId,time; public String text; public boolean incoming; public Message(long id,long botId,String text,boolean incoming,long time){this.id=id;this.botId=botId;this.text=text;this.incoming=incoming;this.time=time;} }
    public static final class CallItem { public long id,botId,time; public String type; public int durationSec; public CallItem(long id,long botId,String type,long time,int durationSec){this.id=id;this.botId=botId;this.type=type;this.time=time;this.durationSec=durationSec;} }
    public static final class ReplyPlan { public final List<String> messages=new ArrayList<>(); public final List<Long> delaysMs=new ArrayList<>(); public boolean isEmpty(){return messages.isEmpty();} public void add(String t,long d){messages.add(t);delaysMs.add(Math.max(500,d));} }

    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);} private static ChatDatabase.DaoApi dao(Context c){return ChatDatabase.get(c).dao();}

    public static synchronized void ensureSeeded(Context c){
        if(dao(c).botCount()>0)return;
        long now=System.currentTimeMillis();
        String[] names={"Maya","Sami","Project Team","Kareem","Family Group","Rami","Dalia","Omar"};
        String[] previews={"تمام، منحكي بعدين","وصلتني التفاصيل","Nour: الموعد صار 9:30","تمام 👍","Lina: مين جاي بكرا؟","منحكي بكرا","شكراً إلك","تمام، شفتها"};
        List<ChatDatabase.BotRow> br=new ArrayList<>(); List<ChatDatabase.MessageRow> mr=new ArrayList<>();
        for(int i=0;i<names.length;i++){
            Bot b=new Bot(1001+i,names[i],String.format(Locale.US,"+963 000 000 %03d",i+1)); b.status="شخصية خيالية • محاكاة"; b.unread=i%2==0?4:3; b.lastMessage=previews[i]; b.lastTime=now-(long)(i*13+2)*60000L; b.replyMode=i==1?"slow":i==2?"instant":"natural"; b.favorite=(i==0||i==2||i==4); b.groupChat=(i==2||i==4); b.groupSubtitle=b.groupChat?(i==2?"Maya, Nour, Sami, You":"Lina, Omar, Rami, You"):""; b.lastSender=b.groupChat?(i==2?"Nour":"Lina"):b.name; br.add(toRow(b));
            mr.add(toRow(new Message(now+b.id,b.id,"مرحبا، كيفك؟",true,now-3600000L))); mr.add(toRow(new Message(now+b.id+1,b.id,"تمام الحمد لله، وإنت؟",false,now-3420000L))); mr.add(toRow(new Message(now+b.id+2,b.id,b.lastMessage,true,b.lastTime)));
        }
        dao(c).putBots(br);dao(c).putMessages(mr);
        List<ChatDatabase.CallRow> cr=new ArrayList<>(); long[] ages={1500000L,7200000L,28800000L,93600000L,259200000L}; String[] types={"missed","incoming","outgoing","missed","incoming"}; for(int i=0;i<ages.length;i++)cr.add(toRow(new CallItem(now+i,1001+i,types[i],now-ages[i],i%2==0?0:65+i*20)));dao(c).putCalls(cr);
        prefs(c).edit().putString(KEY_PREFIX,"+963").putString(KEY_ICON,"green").putString(KEY_ICON_STYLE,"rounded").apply();
    }

    public static List<Bot> loadBots(Context c){ensureSeeded(c);return bots(dao(c).botsOrdered());}
    public static List<Bot> loadUnreadBots(Context c){ensureSeeded(c);return bots(dao(c).unreadBotsOrdered());}
    public static List<Bot> loadFavoriteBots(Context c){ensureSeeded(c);return bots(dao(c).favoriteBotsOrdered());}
    public static List<Bot> loadGroupBots(Context c){ensureSeeded(c);return bots(dao(c).groupBotsOrdered());}
    private static List<Bot> bots(List<ChatDatabase.BotRow> rows){List<Bot> r=new ArrayList<>();for(ChatDatabase.BotRow x:rows)r.add(fromRow(x));return r;}
    public static int totalUnread(Context c){ensureSeeded(c);return dao(c).totalUnread();} public static int favoriteCount(Context c){ensureSeeded(c);return dao(c).favoriteCount();} public static int groupCount(Context c){ensureSeeded(c);return dao(c).groupCount();}
    public static Bot getBot(Context c,long id){ensureSeeded(c);ChatDatabase.BotRow x=dao(c).bot(id);return x==null?null:fromRow(x);} public static synchronized void saveBot(Context c,Bot b){dao(c).putBot(toRow(b));if(b.unread==0)NotificationHelper.cancelMessage(c,b.id);} public static void setFavorite(Context c,long id,boolean v){dao(c).setFavorite(id,v);} public static synchronized void markRead(Context c,long id){dao(c).markRead(id);NotificationHelper.cancelMessage(c,id);}

    public static List<Message> loadMessages(Context c,long id){ensureSeeded(c);List<Message> r=new ArrayList<>();for(ChatDatabase.MessageRow x:dao(c).messages(id))r.add(fromRow(x));return r;}
    public static synchronized void addMessage(Context c,Message m){ensureSeeded(c);dao(c).putMessage(toRow(m));Bot b=getBot(c,m.botId);if(b==null)return;b.lastMessage=m.text;b.lastTime=m.time;b.lastSender=m.incoming?b.name:"You";if(m.incoming)b.unread=ChatActivity.isConversationVisible(m.botId)?0:Math.min(99,b.unread+1);else b.unread=0;dao(c).putBot(toRow(b));if(b.unread==0)NotificationHelper.cancelMessage(c,b.id);}
    public static List<CallItem> loadCalls(Context c){ensureSeeded(c);List<CallItem>r=new ArrayList<>();for(ChatDatabase.CallRow x:dao(c).calls())r.add(fromRow(x));return r;} public static synchronized void addCall(Context c,CallItem x){dao(c).putCall(toRow(x));}

    public static ReplyPlan buildReplyPlan(Bot bot,String input){ReplyPlan p=new ReplyPlan();if(bot==null||!bot.autoReply||!isBotActive(bot))return p;String mode=bot.replyMode==null?"natural":bot.replyMode;int chance="instant".equals(mode)?95:"slow".equals(mode)?72:84;if(RANDOM.nextInt(100)>=chance)return p;int cap=Math.max(1,Math.min(5,bot.maxBurst));int roll=RANDOM.nextInt(100);int count=roll<48?1:roll<78?2:roll<92?3:roll<98?4:5;count=Math.min(cap,count);List<String> c=contextualReplies(input);long d=firstDelay(mode);for(int i=0;i<count;i++){p.add(i<c.size()?c.get(i):genericContinuation(i),d);d+=gapDelay(mode);}if(bot.initiative&&RANDOM.nextInt(100)<24)p.add(followUpLine(),Math.max(d+60000L,randomBetween(480000L,4200000L)));return p;}
    private static List<String> contextualReplies(String input){String x=input==null?"":input.toLowerCase(Locale.ROOT);List<String>r=new ArrayList<>();if(x.contains("مرحبا")||x.contains("هلا")){r.add("أهلا 😊");r.add("كيفك؟");r.add("شو الأخبار؟");return r;}if(x.contains("موعد")||x.contains("وقت")){r.add("تمام");r.add("ابعتلي الوقت المناسب.");r.add("إذا تغيّر شي خبرني.");return r;}String[][]p={{"تمام 👍","وصلتني.","بشوف وبردلك."},{"أوكي.","فهمت عليك.","منحكي فيها بعدين."},{"ماشي.","خبرني إذا صار شي.","أنا موجود."}};for(String s:p[RANDOM.nextInt(p.length)])r.add(s);return r;}
    private static String genericContinuation(int i){String[]a={"وبعدين منحكي بالتفصيل.","خليني أرتب الموضوع.","إي وصلت الفكرة.","تمام، خليك خبرني."};return a[Math.floorMod(i+RANDOM.nextInt(a.length),a.length)];} private static String followUpLine(){String[]a={"على فكرة، تذكرت شغلة.","لسا موجود؟","إذا فضيت خبرني.","رجعت راجعت الموضوع."};return a[RANDOM.nextInt(a.length)];} private static long firstDelay(String m){if("instant".equals(m))return randomBetween(700,2200);if("slow".equals(m))return randomBetween(120000,660000);return randomBetween(9000,75000);} private static long gapDelay(String m){return "instant".equals(m)?randomBetween(800,3500):randomBetween(3000,18000);} private static long randomBetween(long min,long max){long v=RANDOM.nextLong()&Long.MAX_VALUE;return min+(v%Math.max(1,max-min));} public static String smartReply(String input){List<String>r=contextualReplies(input);return r.isEmpty()?"تمام 👍":r.get(0);}

    public static String formatTime(long t){return new SimpleDateFormat("h:mm a",Locale.getDefault()).format(new Date(t));} public static String formatDateTime(long t){return new SimpleDateFormat("MMM d, h:mm a",Locale.getDefault()).format(new Date(t));}
    public static String getPrefix(Context c){return prefs(c).getString(KEY_PREFIX,"+963");} public static void setPrefix(Context c,String v){prefs(c).edit().putString(KEY_PREFIX,v).apply();} public static String getCallSound(Context c){return prefs(c).getString(KEY_CALL_SOUND,"");} public static void setCallSound(Context c,String v){prefs(c).edit().putString(KEY_CALL_SOUND,v==null?"":v).apply();} public static String getIcon(Context c){return prefs(c).getString(KEY_ICON,"green");} public static void setIcon(Context c,String v){prefs(c).edit().putString(KEY_ICON,v).apply();} public static String getIconStyle(Context c){return prefs(c).getString(KEY_ICON_STYLE,"rounded");} public static void setIconStyle(Context c,String v){prefs(c).edit().putString(KEY_ICON_STYLE,v).apply();}
    public static boolean isBotActive(Bot b){int h=Integer.parseInt(new SimpleDateFormat("H",Locale.US).format(new Date()));if(b.activeFrom==b.activeTo)return true;if(b.activeFrom<b.activeTo)return h>=b.activeFrom&&h<b.activeTo;return h>=b.activeFrom||h<b.activeTo;} public static synchronized void resetDemo(Context c){ChatDatabase.DaoApi d=dao(c);d.clearMessages();d.clearCalls();d.clearBots();ensureSeeded(c);}

    private static ChatDatabase.BotRow toRow(Bot b){ChatDatabase.BotRow x=new ChatDatabase.BotRow();x.id=b.id;x.name=b.name;x.phone=b.phone;x.status=b.status;x.unread=b.unread;x.lastMessage=b.lastMessage;x.lastTime=b.lastTime;x.autoReply=b.autoReply;x.avatarUri=b.avatarUri;x.activeFrom=b.activeFrom;x.activeTo=b.activeTo;x.replyMode=b.replyMode;x.initiative=b.initiative;x.maxBurst=b.maxBurst;x.favorite=b.favorite;x.groupChat=b.groupChat;x.groupSubtitle=b.groupSubtitle==null?"":b.groupSubtitle;x.lastSender=b.lastSender==null?"":b.lastSender;return x;}
    private static Bot fromRow(ChatDatabase.BotRow x){Bot b=new Bot(x.id,x.name,x.phone);b.status=x.status;b.unread=x.unread;b.lastMessage=x.lastMessage;b.lastTime=x.lastTime;b.autoReply=x.autoReply;b.avatarUri=x.avatarUri;b.activeFrom=x.activeFrom;b.activeTo=x.activeTo;b.replyMode=x.replyMode;b.initiative=x.initiative;b.maxBurst=x.maxBurst;b.favorite=x.favorite;b.groupChat=x.groupChat;b.groupSubtitle=x.groupSubtitle;b.lastSender=x.lastSender;return b;}
    private static ChatDatabase.MessageRow toRow(Message m){ChatDatabase.MessageRow x=new ChatDatabase.MessageRow();x.id=m.id;x.botId=m.botId;x.text=m.text;x.incoming=m.incoming;x.time=m.time;return x;} private static Message fromRow(ChatDatabase.MessageRow x){return new Message(x.id,x.botId,x.text,x.incoming,x.time);} private static ChatDatabase.CallRow toRow(CallItem c){ChatDatabase.CallRow x=new ChatDatabase.CallRow();x.id=c.id;x.botId=c.botId;x.type=c.type;x.time=c.time;x.durationSec=c.durationSec;return x;} private static CallItem fromRow(ChatDatabase.CallRow x){return new CallItem(x.id,x.botId,x.type,x.time,x.durationSec);}
}
