package com.maldawr.chatsimulator;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    private Ui(){}
    private static final class SimTextView extends TextView {
        SimTextView(Context c){super(c);}
        @Override public void setText(CharSequence text,BufferType type){CharSequence value=text;if(getContext() instanceof ChatActivity){String s=text==null?"":text.toString();if(!CustomizationPrefs.showReadReceipts(getContext())&&s.contains("✓✓"))value=s.replace("✓✓","").trim();if(!CustomizationPrefs.showTyping(getContext())&&s.toLowerCase().contains("typing"))value="online • simulated contact";}super.setText(value,type);}
    }
    private static final class FictionalAvatarView extends View {
        private final Store.Bot bot; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final int seed;
        FictionalAvatarView(Context c,Store.Bot bot){super(c);this.bot=bot;String n=bot==null?"sim":bot.name;seed=Math.abs((n==null?"sim":n).hashCode());setContentDescription((bot==null?"Fictional profile":bot.name)+" • SIM");}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float s=Math.min(getWidth(),getHeight());float cx=getWidth()/2f,cy=getHeight()/2f;Path clip=new Path();clip.addCircle(cx,cy,s/2f,Path.Direction.CW);canvas.save();canvas.clipPath(clip);int[] bg={0xFF356B78,0xFF6A567A,0xFF735B45,0xFF3F6B56,0xFF59697B,0xFF7A4E59};canvas.drawColor(bg[seed%bg.length]);if(bot!=null&&bot.groupChat)drawGroup(canvas,cx,cy,s);else drawPerson(canvas,cx,cy,s,seed);canvas.restore();p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1f,s*.025f));p.setColor(0x55FFFFFF);canvas.drawCircle(cx,cy,s*.485f,p);p.setStyle(Paint.Style.FILL);}
        private void drawPerson(Canvas c,float cx,float cy,float s,int h){int[] skin={0xFFFFD2B3,0xFFE9B08A,0xFFD4936E,0xFFB87352,0xFF8C5C43};int skinColor=skin[h%skin.length];p.setColor(0xFF25333B);c.drawOval(new RectF(cx-s*.34f,cy+s*.20f,cx+s*.34f,cy+s*.65f),p);p.setColor(skinColor);c.drawOval(new RectF(cx-s*.22f,cy-s*.27f,cx+s*.22f,cy+s*.28f),p);int[] hair={0xFF2A211D,0xFF3B2E28,0xFF17191B,0xFF5A3C2B};p.setColor(hair[(h/3)%hair.length]);c.drawArc(new RectF(cx-s*.235f,cy-s*.31f,cx+s*.235f,cy+s*.04f),180f,180f,true,p);p.setColor(0xFF20262A);c.drawCircle(cx-s*.075f,cy-s*.035f,s*.018f,p);c.drawCircle(cx+s*.075f,cy-s*.035f,s*.018f,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(s*.018f);p.setStrokeCap(Paint.Cap.ROUND);c.drawArc(new RectF(cx-s*.085f,cy+s*.02f,cx+s*.085f,cy+s*.13f),18f,144f,false,p);p.setStyle(Paint.Style.FILL);}
        private void drawGroup(Canvas c,float cx,float cy,float s){int[] skin={0xFFE9B08A,0xFFD4936E,0xFFB87352};float[] xs={cx-s*.20f,cx+s*.20f,cx};float[] ys={cy-s*.08f,cy-s*.08f,cy+s*.10f};for(int i=0;i<3;i++){p.setColor(0xFF26353C);c.drawOval(new RectF(xs[i]-s*.17f,ys[i]+s*.10f,xs[i]+s*.17f,ys[i]+s*.36f),p);p.setColor(skin[(seed+i)%skin.length]);c.drawCircle(xs[i],ys[i],s*.13f,p);p.setColor(0xFF24201E);c.drawArc(new RectF(xs[i]-s*.14f,ys[i]-s*.14f,xs[i]+s*.14f,ys[i]+s*.08f),180f,180f,true,p);}}
    }
    public static int dp(Context c,int value){return Math.round(value*c.getResources().getDisplayMetrics().density);}public static boolean isDark(Context c){int mode=c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK;return mode==Configuration.UI_MODE_NIGHT_YES;}
    public static int bg(Context c){return isDark(c)?Color.rgb(17,27,33):Color.WHITE;}public static int card(Context c){return isDark(c)?Color.rgb(31,44,51):Color.WHITE;}public static int text(Context c){return isDark(c)?Color.rgb(233,237,239):Color.rgb(17,27,33);}public static int sub(Context c){return isDark(c)?Color.rgb(134,150,160):Color.rgb(102,119,129);}public static int divider(Context c){return isDark(c)?Color.rgb(42,57,66):Color.rgb(236,239,241);}public static int chatBg(Context c){return CustomizationPrefs.chatBackground(c);}public static int brand(){return Color.rgb(11,107,93);}public static int brandDark(){return Color.rgb(8,83,74);}public static int brandBright(){return Color.rgb(33,161,121);}public static int red(){return Color.rgb(211,60,60);}
    public static GradientDrawable rounded(int color,float radiusDp,Context c){if(c instanceof ChatActivity){if(color==0xFF202C33)color=CustomizationPrefs.incomingBubble(c);else if(color==0xFF075E54)color=CustomizationPrefs.outgoingBubble(c);if(color==CustomizationPrefs.incomingBubble(c)||color==CustomizationPrefs.outgoingBubble(c))radiusDp=CustomizationPrefs.getBubbleRadius(c);}GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,Math.round(radiusDp)));return d;}
    public static GradientDrawable circle(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setShape(GradientDrawable.OVAL);return d;}
    public static TextView label(Context c,String value,float sp,boolean bold){TextView v=c instanceof ChatActivity?new SimTextView(c):new TextView(c);if(c instanceof ChatActivity&&sp==17)sp=CustomizationPrefs.getMessageFontSize(c);v.setText(value);v.setTextSize(sp);v.setTextColor(text(c));v.setIncludeFontPadding(false);v.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);return v;}
    public static Button button(Context c,String title){Button b=new Button(c);b.setText(title);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(Color.WHITE);b.setBackground(rounded(CustomizationPrefs.accent(c),12,c));b.setPadding(dp(c,16),dp(c,7),dp(c,16),dp(c,7));return b;}
    public static TextView iconButton(Context c,String glyph,int sizeDp,float sp,int bgColor,int textColor){TextView v=new TextView(c);v.setText(glyph);v.setTextSize(sp);v.setTextColor(textColor);v.setGravity(Gravity.CENTER);v.setIncludeFontPadding(false);if(bgColor!=Color.TRANSPARENT)v.setBackground(circle(bgColor));v.setLayoutParams(new ViewGroup.LayoutParams(dp(c,sizeDp),dp(c,sizeDp)));v.setClickable(true);v.setFocusable(true);return v;}
    public static TextView oneLine(Context c,String value,float sp,int color){TextView v=label(c,value,sp,false);v.setTextColor(color);v.setSingleLine(true);v.setEllipsize(TextUtils.TruncateAt.END);return v;}
    public static View divider(Context c,int startDp){View v=new View(c);v.setBackgroundColor(divider(c));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(c,1));lp.setMargins(dp(c,startDp),0,0,0);v.setLayoutParams(lp);return v;}
    public static TextView safetyBanner(Context c){TextView b=new TextView(c);b.setText("محاكاة شخصية • بيانات خيالية");b.setTextSize(10);b.setTextColor(sub(c));b.setGravity(Gravity.CENTER);b.setIncludeFontPadding(false);b.setPadding(dp(c,8),dp(c,4),dp(c,8),dp(c,4));return b;}
    public static View avatar(Context c,Store.Bot bot,int sizeDp){int px=dp(c,sizeDp);FrameLayout f=new FrameLayout(c);f.setLayoutParams(new ViewGroup.LayoutParams(px,px));if(bot!=null&&bot.avatarUri!=null&&!bot.avatarUri.isEmpty())try{ImageView i=new ImageView(c);i.setScaleType(ImageView.ScaleType.CENTER_CROP);i.setImageURI(Uri.parse(bot.avatarUri));i.setBackground(circle(Color.rgb(95,118,126)));i.setClipToOutline(true);f.addView(i,new FrameLayout.LayoutParams(px,px));return f;}catch(Exception ignored){}FictionalAvatarView avatar=new FictionalAvatarView(c,bot);f.addView(avatar,new FrameLayout.LayoutParams(px,px));return f;}
    public static LinearLayout cardRow(Context c){LinearLayout r=new LinearLayout(c);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(c,14),dp(c,11),dp(c,14),dp(c,11));r.setBackground(rounded(card(c),12,c));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(c,10),dp(c,5),dp(c,10),dp(c,5));r.setLayoutParams(p);return r;}
}
