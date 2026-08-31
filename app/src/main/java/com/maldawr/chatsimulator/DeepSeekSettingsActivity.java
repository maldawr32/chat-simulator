package com.maldawr.chatsimulator;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class DeepSeekSettingsActivity extends Activity {
    private Switch enabled, thinking;
    private EditText keyInput;
    private Spinner model, effort;
    private TextView status;

    @Override protected void onCreate(Bundle state){ super.onCreate(state); build(); }

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xFF0B141A);root.addView(Ui.safetyBanner(this));
        ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(Ui.dp(this,18),Ui.dp(this,18),Ui.dp(this,18),Ui.dp(this,32));
        TextView title=Ui.label(this,"DeepSeek AI • SIM",25,true);title.setTextColor(Color.WHITE);body.addView(title);
        TextView note=Ui.label(this,"Optional AI for fictional simulator replies. When enabled, recent simulated chat text is sent to DeepSeek API. The API key is encrypted locally with Android Keystore and is never stored in this GitHub project.",13,false);note.setTextColor(0xFF8696A0);note.setPadding(0,Ui.dp(this,6),0,Ui.dp(this,16));body.addView(note);

        enabled=new Switch(this);enabled.setText("Use DeepSeek for replies");enabled.setTextColor(Color.WHITE);enabled.setChecked(AiPrefs.isEnabled(this));body.addView(enabled);
        keyInput=new EditText(this);keyInput.setSingleLine(true);keyInput.setHint(AiPrefs.getApiKey(this).isEmpty()?"DeepSeek API key":"API key saved • enter only to replace");keyInput.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);keyInput.setTextColor(Color.WHITE);keyInput.setHintTextColor(0xFF8696A0);keyInput.setBackground(Ui.rounded(0xFF1F2C33,14,this));keyInput.setPadding(Ui.dp(this,14),0,Ui.dp(this,14),0);LinearLayout.LayoutParams kp=new LinearLayout.LayoutParams(-1,Ui.dp(this,52));kp.setMargins(0,Ui.dp(this,10),0,Ui.dp(this,12));body.addView(keyInput,kp);

        TextView ml=Ui.label(this,"Model",14,true);ml.setTextColor(Color.WHITE);body.addView(ml);
        model=new Spinner(this);model.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"DeepSeek V4 Flash","DeepSeek V4 Pro"}));model.setSelection("deepseek-v4-pro".equals(AiPrefs.getModel(this))?1:0);body.addView(model);

        thinking=new Switch(this);thinking.setText("Thinking mode");thinking.setTextColor(Color.WHITE);thinking.setChecked(AiPrefs.isThinking(this));body.addView(thinking);
        TextView el=Ui.label(this,"Reasoning effort",14,true);el.setTextColor(Color.WHITE);body.addView(el);
        effort=new Spinner(this);effort.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Low","High","Max"}));String e=AiPrefs.getEffort(this);effort.setSelection("max".equals(e)?2:"high".equals(e)?1:0);body.addView(effort);

        status=Ui.label(this,statusText(),13,false);status.setTextColor(0xFF9FC9BB);status.setGravity(Gravity.START);status.setPadding(0,Ui.dp(this,14),0,Ui.dp(this,8));body.addView(status);
        Button save=Ui.button(this,"Save DeepSeek settings");save.setOnClickListener(v->{if(saveSettings()){Toast.makeText(this,"DeepSeek settings saved",Toast.LENGTH_SHORT).show();status.setText(statusText());}});body.addView(save);
        Button test=Ui.button(this,"Test DeepSeek connection");test.setOnClickListener(v->{if(!saveSettings())return;status.setText("Testing connection…");DeepSeekClient.testAsync(this,new DeepSeekClient.Callback(){public void onSuccess(String value){status.setText("Connected • "+AiPrefs.getModel(DeepSeekSettingsActivity.this)+" • "+value);Toast.makeText(DeepSeekSettingsActivity.this,"DeepSeek connected",Toast.LENGTH_SHORT).show();}public void onError(String error){status.setText("Connection failed • "+error);}});});body.addView(test);
        Button clear=Ui.button(this,"Clear saved API key");clear.setOnClickListener(v->{AiPrefs.clearApiKey(this);AiPrefs.setEnabled(this,false);enabled.setChecked(false);keyInput.setText("");keyInput.setHint("DeepSeek API key");status.setText(statusText());Toast.makeText(this,"API key cleared",Toast.LENGTH_SHORT).show();});body.addView(clear);
        scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }

    private boolean saveSettings(){
        String typed=keyInput.getText().toString().trim();
        if(!typed.isEmpty()&&!AiPrefs.saveApiKey(this,typed)){Toast.makeText(this,"Could not encrypt API key",Toast.LENGTH_LONG).show();return false;}
        if(enabled.isChecked()&&AiPrefs.getApiKey(this).isEmpty()){keyInput.setError("Enter an API key before enabling DeepSeek");return false;}
        AiPrefs.setEnabled(this,enabled.isChecked());
        AiPrefs.setModel(this,model.getSelectedItemPosition()==1?"deepseek-v4-pro":"deepseek-v4-flash");
        AiPrefs.setThinking(this,thinking.isChecked());
        AiPrefs.setEffort(this,effort.getSelectedItemPosition()==2?"max":effort.getSelectedItemPosition()==1?"high":"low");
        keyInput.setText("");keyInput.setHint(AiPrefs.getApiKey(this).isEmpty()?"DeepSeek API key":"API key saved • enter only to replace");
        return true;
    }
    private String statusText(){return AiPrefs.getApiKey(this).isEmpty()?"API key: not configured":"API key: encrypted locally • "+AiPrefs.getModel(this)+(AiPrefs.isEnabled(this)?" • enabled":" • disabled");}
}
