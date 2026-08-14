package com.maldawr.chatsimulator;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class Store {
    private static final String PREFS = "chat_simulator_data";
    private static final String KEY_BOTS = "bots";
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_CALLS = "calls";
    private static final String KEY_PREFIX = "prefix";
    private static final String KEY_NOTIFICATION_SOUND = "notification_sound";
    private static final String KEY_CALL_SOUND = "call_sound";
    private static final String KEY_ICON = "icon";
    private static final String KEY_SCHEMA = "schema_version";
    private static final int SCHEMA_V2 = 2;
    private static final Random RANDOM = new Random();

    private Store() {}

    public static final class Bot {
        public long id; public String name; public String phone; public String status;
        public int unread; public String lastMessage; public long lastTime;
        public boolean autoReply; public String avatarUri; public int activeFrom; public int activeTo;

        public Bot(long id, String name, String phone) {
            this.id=id; this.name=name; this.phone=phone; this.status="Fictional bot";
            this.unread=0; this.lastMessage="Simulation ready"; this.lastTime=System.currentTimeMillis();
            this.autoReply=true; this.avatarUri=""; this.activeFrom=0; this.activeTo=24;
        }
        JSONObject toJson(){ JSONObject o=new JSONObject(); try{
            o.put("id",id);o.put("name",name);o.put("phone",phone);o.put("status",status);o.put("unread",unread);
            o.put("lastMessage",lastMessage);o.put("lastTime",lastTime);o.put("autoReply",autoReply);o.put("avatarUri",avatarUri);
            o.put("activeFrom",activeFrom);o.put("activeTo",activeTo);
        }catch(Exception ignored){} return o; }
        static Bot fromJson(JSONObject o){ Bot b=new Bot(o.optLong("id"),o.optString("name"),o.optString("phone"));
            b.status=o.optString("status","Fictional bot");b.unread=o.optInt("unread",0);b.lastMessage=o.optString("lastMessage","Simulation ready");
            b.lastTime=o.optLong("lastTime",System.currentTimeMillis());b.autoReply=o.optBoolean("autoReply",true);b.avatarUri=o.optString("avatarUri","");
            b.activeFrom=o.optInt("activeFrom",0);b.activeTo=o.optInt("activeTo",24);return b; }
    }

    public static final class Message {
        public long id; public long botId; public String text; public boolean incoming; public long time;
        public Message(long id,long botId,String text,boolean incoming,long time){this.id=id;this.botId=botId;this.text=text;this.incoming=incoming;this.time=time;}
        JSONObject toJson(){JSONObject o=new JSONObject();try{o.put("id",id);o.put("botId",botId);o.put("text",text);o.put("incoming",incoming);o.put("time",time);}catch(Exception ignored){}return o;}
        static Message fromJson(JSONObject o){return new Message(o.optLong("id"),o.optLong("botId"),o.optString("text"),o.optBoolean("incoming"),o.optLong("time"));}
    }

    public static final class CallItem {
        public long id; public long botId; public String type; public long time; public int durationSec;
        public CallItem(long id,long botId,String type,long time,int durationSec){this.id=id;this.botId=botId;this.type=type;this.time=time;this.durationSec=durationSec;}
        JSONObject toJson(){JSONObject o=new JSONObject();try{o.put("id",id);o.put("botId",botId);o.put("type",type);o.put("time",time);o.put("durationSec",durationSec);}catch(Exception ignored){}return o;}
        static CallItem fromJson(JSONObject o){return new CallItem(o.optLong("id"),o.optLong("botId"),o.optString("type"),o.optLong("time"),o.optInt("durationSec"));}
    }

    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}

    public static synchronized void ensureSeeded(Context c){
        if(!prefs(c).contains(KEY_BOTS)) seed(c);
        migrateV2(c);
    }

    private static void seed(Context c){
        long now=System.currentTimeMillis();
        List<Bot> bots=new ArrayList<>();
        String[] names={"Maya","Sami","Nour","Kareem","Lina","Rami","Dalia","Omar"};
        String[] previews={"تمام، منحكي بعدين","وصلتني التفاصيل","بعتلك الموعد","تمام 👍","وين صرت؟","منحكي بكرا","شكراً إلك","تمام، شفتها"};
        for(int i=0;i<names.length;i++){
            Bot b=new Bot(1001+i,names[i],String.format(Locale.US,"+963 000 000 %03d",i+1));
            b.status="شخصية خيالية • محاكاة"; b.unread=(i%2==0?4:3); b.lastMessage=previews[i];
            b.lastTime=now-(long)(i*11+2)*60_000L; b.activeFrom=0;b.activeTo=24; bots.add(b);
        }
        saveBots(c,bots);
        List<Message> messages=new ArrayList<>();
        for(Bot b:bots){
            messages.add(new Message(now+b.id,b.id,"مرحبا، كيفك؟",true,now-3_600_000L));
            messages.add(new Message(now+b.id+1,b.id,"تمام الحمد لله، وإنت؟",false,now-3_420_000L));
            messages.add(new Message(now+b.id+2,b.id,b.lastMessage,true,b.lastTime));
            if((b.id%2)==0) messages.add(new Message(now+b.id+3,b.id,"بشوفك بعدين",false,b.lastTime+60_000L));
        }
        saveMessages(c,messages);
        List<CallItem> calls=new ArrayList<>();
        long[] ages={25*60_000L,2*3_600_000L,8*3_600_000L,26*3_600_000L,3*86_400_000L};
        String[] types={"missed","incoming","missed","outgoing","incoming"};
        for(int i=0;i<ages.length;i++)calls.add(new CallItem(now+i,bots.get(i).id,types[i],now-ages[i],i%2==0?0:65+i*20));
        saveCalls(c,calls);
        prefs(c).edit().putString(KEY_PREFIX,"+963").putString(KEY_ICON,"green").putInt(KEY_SCHEMA,SCHEMA_V2).apply();
    }

    private static synchronized void migrateV2(Context c){
        if(prefs(c).getInt(KEY_SCHEMA,0)>=SCHEMA_V2)return;
        List<Bot> bots=loadBots(c); int i=0;
        for(Bot b:bots){ if(b.unread>4)b.unread=(i++%2==0?4:3); }
        saveBots(c,bots); prefs(c).edit().putInt(KEY_SCHEMA,SCHEMA_V2).apply();
    }

    public static List<Bot> loadBots(Context c){List<Bot> r=new ArrayList<>();try{JSONArray a=new JSONArray(prefs(c).getString(KEY_BOTS,"[]"));for(int i=0;i<a.length();i++)r.add(Bot.fromJson(a.getJSONObject(i)));}catch(Exception ignored){}return r;}
    public static synchronized void saveBots(Context c,List<Bot> bots){JSONArray a=new JSONArray();for(Bot b:bots)a.put(b.toJson());prefs(c).edit().putString(KEY_BOTS,a.toString()).apply();}
    public static Bot getBot(Context c,long id){for(Bot b:loadBots(c))if(b.id==id)return b;return null;}
    public static synchronized void saveBot(Context c,Bot bot){List<Bot> bots=loadBots(c);boolean found=false;for(int i=0;i<bots.size();i++)if(bots.get(i).id==bot.id){bots.set(i,bot);found=true;break;}if(!found)bots.add(0,bot);saveBots(c,bots);}

    public static List<Message> loadMessages(Context c,long botId){List<Message> r=new ArrayList<>();for(Message m:loadAllMessages(c))if(m.botId==botId)r.add(m);return r;}
    private static List<Message> loadAllMessages(Context c){List<Message> r=new ArrayList<>();try{JSONArray a=new JSONArray(prefs(c).getString(KEY_MESSAGES,"[]"));for(int i=0;i<a.length();i++)r.add(Message.fromJson(a.getJSONObject(i)));}catch(Exception ignored){}return r;}
    private static void saveMessages(Context c,List<Message> messages){JSONArray a=new JSONArray();for(Message m:messages)a.put(m.toJson());prefs(c).edit().putString(KEY_MESSAGES,a.toString()).apply();}
    public static synchronized void addMessage(Context c,Message m){List<Message> all=loadAllMessages(c);all.add(m);if(all.size()>2000)all=new ArrayList<>(all.subList(all.size()-2000,all.size()));saveMessages(c,all);}

    public static List<CallItem> loadCalls(Context c){List<CallItem> r=new ArrayList<>();try{JSONArray a=new JSONArray(prefs(c).getString(KEY_CALLS,"[]"));for(int i=0;i<a.length();i++)r.add(CallItem.fromJson(a.getJSONObject(i)));}catch(Exception ignored){}return r;}
    private static void saveCalls(Context c,List<CallItem> calls){JSONArray a=new JSONArray();for(CallItem x:calls)a.put(x.toJson());prefs(c).edit().putString(KEY_CALLS,a.toString()).apply();}
    public static synchronized void addCall(Context c,CallItem x){List<CallItem> calls=loadCalls(c);calls.add(0,x);if(calls.size()>200)calls=new ArrayList<>(calls.subList(0,200));saveCalls(c,calls);}

    public static String smartReply(String input){String x=input==null?"":input.toLowerCase(Locale.ROOT);if(x.contains("مرحبا")||x.contains("hello")||x.contains("hi"))return "أهلا، تمام الحمد لله 😊";if(x.contains("موعد")||x.contains("meeting"))return "تمام، ابعتلي الوقت المناسب.";if(x.contains("مكالمة")||x.contains("call"))return "أكيد، منحكي بعد شوي.";if(x.contains("شكرا")||x.contains("thanks"))return "العفو 🙏";String[] r={"تمام 👍","وصلتني.","أوكي، منحكي بعدين.","تمام، بشوف وبردلك.","ماشي، خبرني لما تفضى."};return r[RANDOM.nextInt(r.length)];}
    public static String formatTime(long t){return new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(t));}
    public static String formatDateTime(long t){return new SimpleDateFormat("dd/MM/yyyy  HH:mm",Locale.getDefault()).format(new Date(t));}
    public static String getPrefix(Context c){return prefs(c).getString(KEY_PREFIX,"+963");} public static void setPrefix(Context c,String v){prefs(c).edit().putString(KEY_PREFIX,v).apply();}
    public static String getNotificationSound(Context c){return prefs(c).getString(KEY_NOTIFICATION_SOUND,"");} public static void setNotificationSound(Context c,String u){prefs(c).edit().putString(KEY_NOTIFICATION_SOUND,u==null?"":u).apply();}
    public static String getCallSound(Context c){return prefs(c).getString(KEY_CALL_SOUND,"");} public static void setCallSound(Context c,String u){prefs(c).edit().putString(KEY_CALL_SOUND,u==null?"":u).apply();}
    public static String getIcon(Context c){return prefs(c).getString(KEY_ICON,"green");} public static void setIcon(Context c,String i){prefs(c).edit().putString(KEY_ICON,i).apply();}
    public static boolean isBotActive(Bot b){int h=Integer.parseInt(new SimpleDateFormat("H",Locale.US).format(new Date()));if(b.activeFrom==b.activeTo)return true;if(b.activeFrom<b.activeTo)return h>=b.activeFrom&&h<b.activeTo;return h>=b.activeFrom||h<b.activeTo;}
    public static synchronized void resetDemo(Context c){prefs(c).edit().remove(KEY_BOTS).remove(KEY_MESSAGES).remove(KEY_CALLS).remove(KEY_SCHEMA).apply();seed(c);}
}
