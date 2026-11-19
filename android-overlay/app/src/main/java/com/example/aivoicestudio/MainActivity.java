package com.example.aivoicestudio;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.net.Uri;
import android.provider.Settings;
import android.security.keystore.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.*;

public class MainActivity extends Activity {
  private static final String MODEL="gemini-2.5-flash-preview-tts";
  private static final String[] VOICES={"Zephyr","Puck","Charon","Kore","Fenrir","Leda","Orus","Aoede","Callirrhoe","Autonoe","Enceladus","Iapetus","Umbriel","Algieba","Despina","Erinome","Algenib","Rasalgethi","Laomedeia","Achernar","Alnilam","Schedar","Gacrux","Pulcherrima","Achird","Zubenelgenubi","Vindemiatrix","Sadachbia","Sadaltager","Sulafat"};
  private static final String[] VOICE_LABELS={"Zephyr — สดใส","Puck — ร่าเริง","Charon — ให้ข้อมูล","Kore — หนักแน่น","Fenrir — ตื่นเต้น","Leda — อ่อนเยาว์","Orus — มั่นคง","Aoede — ผ่อนคลาย","Callirrhoe — สบาย ๆ","Autonoe — สดใส","Enceladus — นุ่มนวล","Iapetus — ชัดเจน","Umbriel — สบาย ๆ","Algieba — นุ่มนวล","Despina — เรียบลื่น","Erinome — ชัดเจน","Algenib — แหบเล็กน้อย","Rasalgethi — ให้ข้อมูล","Laomedeia — ร่าเริง","Achernar — นุ่มนวล","Alnilam — มั่นคง","Schedar — สม่ำเสมอ","Gacrux — ผู้ใหญ่","Pulcherrima — ตรงไปตรงมา","Achird — เป็นมิตร","Zubenelgenubi — สบาย ๆ","Vindemiatrix — อ่อนโยน","Sadachbia — มีชีวิตชีวา","Sadaltager — รอบรู้","Sulafat — อบอุ่น"};
  private static final String[] STYLES={"แนวเสียงเด็กผู้หญิง","แนวเสียงเด็กผู้ชาย","แนวเสียงคุณตา (ผู้สูงอายุชาย)","แนวเสียงคุณยาย (ผู้สูงอายุหญิง)","ธรรมชาติ","อบอุ่นและเป็นกันเอง","สดใส สนุกสนาน","จริงจังแบบผู้ประกาศข่าว","เล่าเรื่องสารคดี","นุ่มนวล ผ่อนคลาย","ตื่นเต้น เร้าใจ","โฆษณาสินค้า","เล่านิทาน","อ่านช้าและชัดเจน"};
  private static final String[] STYLE_PROMPTS={"Speak in a youthful, playful, childlike feminine character voice with clear natural pronunciation.","Speak in a youthful, playful, childlike masculine character voice with clear natural pronunciation.","Speak as a gentle elderly grandfather character, with a mature warm slightly weathered tone, unhurried pacing, and clear Thai pronunciation.","Speak as a kind elderly grandmother character, with a mature warm slightly weathered tone, unhurried pacing, and clear Thai pronunciation.","Speak naturally and clearly.","Speak warmly and conversationally.","Speak cheerfully and playfully.","Speak professionally like a news presenter.","Narrate like a documentary presenter.","Speak softly and calmly.","Speak with excitement and energy.","Speak persuasively like a polished commercial.","Tell the story expressively and gently.","Speak slowly with very clear articulation."};
  private EditText script,keyField,instructions;
  private Button keySaveButton,keyEditButton;
  private LinearLayout keyList;
  private EditText keyNameField,keyEmailField;
  private int editingKeyIndex=-1;
  private boolean keyLocked=false;
  private void setKeyLocked(boolean locked){
    keyLocked=locked;
    keyField.setEnabled(!locked);
    keyField.setFocusable( !locked );
    keyField.setFocusableInTouchMode(!locked);
    keyField.setTextColor(locked?0xff8e97ae:Color.WHITE);
    keyField.setAlpha(locked?.55f:1f);
    keySaveButton.setVisibility(locked?View.GONE:View.VISIBLE);
    keyEditButton.setVisibility(locked?View.VISIBLE:View.GONE);
    if(locked){keyField.setText("••••••••••••••••");}else{keyField.setText("");keyField.requestFocus();}
  }
  private Spinner voice,language,style;
  private TextView voiceAvatar;
  private final LinearLayout[] characterButtons=new LinearLayout[8];
  private final String[] characterNames={"เด็กผู้หญิง","เด็กผู้ชาย","ผู้หญิง","ผู้ชาย","คุณตา","คุณยาย","นักพากย์ชาย","นักพากย์หญิง"};
  private final int[] characterVoices={5,1,7,2,22,29,11,0};
  private final int[] characterStyles={0,1,5,4,2,3,8,7};
  private int selectedCharacter=-1;
  private void selectCharacter(int index){
    selectedCharacter=index;
    for(int i=0;i<characterButtons.length;i++){
      if(characterButtons[i]!=null){characterButtons[i].setBackground(shape(i==index?accent:panel));characterButtons[i].setAlpha(i==index?1f:.9f);}
    }
    voice.setSelection(characterVoices[index]);style.setSelection(characterStyles[index]);
    voiceAvatar.setText(characterNames[index]+"  •  "+VOICE_LABELS[characterVoices[index]]);
  }
  private TextView status;
  private byte[] audio;
  private AudioTrack player;
  private int bg=0xff101525, panel=0xff242c42, accent=0xff7964ed;
  private String[] languages={"ไทย","English","日本語","한국어","中文","Español","Français","Deutsch","Tiếng Việt","हिन्दी"};
  private String[] codes={"Thai","English","Japanese","Korean","Chinese","Spanish","French","German","Vietnamese","Hindi"};
  private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density);}
  private GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));return d;}
  private TextView label(String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(-1);t.setTextSize(16);return t;}
  private void add(LinearLayout root,View v,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);root.addView(v,p);}
  private EditText edit(String hint,int lines){EditText e=new EditText(this);e.setTextColor(-1);e.setHintTextColor(0xffa5aec9);e.setHint(hint);e.setMinLines(lines);e.setPadding(dp(12),dp(12),dp(12),dp(12));e.setBackground(shape(panel));return e;}
  private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(-1);b.setAllCaps(false);b.setBackground(shape(accent));return b;}
  private Spinner spinner(String[] items){Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,items));s.setBackground(shape(panel));return s;}
  private void message(String s){runOnUiThread(()->status.setText(s));}
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
    ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(bg);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(24),dp(18),dp(30));scroll.addView(root);setContentView(scroll);
    TextView title=label("✦ AI Voice Studio");title.setTextSize(26);add(root,title,0);
    TextView subtitle=label("สร้างเสียงพากย์ AI • Gemini");subtitle.setTextColor(0xffaab6d5);subtitle.setTextSize(13);add(root,subtitle,4);
    LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);add(root,nav,20);
    Button studioTab=button("🎙 สตูดิโอ"), settingsTab=button("⚙ ตั้งค่า");
    nav.addView(studioTab,new LinearLayout.LayoutParams(0,dp(50),1));
    LinearLayout.LayoutParams navp=new LinearLayout.LayoutParams(0,dp(50),1);navp.leftMargin=dp(8);nav.addView(settingsTab,navp);
    LinearLayout studio=new LinearLayout(this);studio.setOrientation(1);add(root,studio,10);
    LinearLayout settings=new LinearLayout(this);settings.setOrientation(1);add(root,settings,10);settings.setVisibility(View.GONE);
    studioTab.setOnClickListener(v->{studio.setVisibility(View.VISIBLE);settings.setVisibility(View.GONE);studioTab.setAlpha(1f);settingsTab.setAlpha(.65f);});
    settingsTab.setOnClickListener(v->{studio.setVisibility(View.GONE);settings.setVisibility(View.VISIBLE);studioTab.setAlpha(.65f);settingsTab.setAlpha(1f);});
    settingsTab.setAlpha(.65f);
    add(settings,label("🔐 จัดการ Gemini API Key หลายบัญชี"),12);
    TextView help=label("เลือกเปิดใช้งานได้ทีละคีย์ • คีย์เก็บเข้ารหัสในมือถือ");help.setTextColor(0xffaab6d5);help.setTextSize(12);add(settings,help,6);
    keyList=new LinearLayout(this);keyList.setOrientation(1);add(settings,keyList,12);
    keyNameField=edit("ชื่อบัญชี เช่น Google บัญชี 2",1);keyNameField.setSingleLine(true);add(settings,keyNameField,12);
    keyEmailField=edit("Gmail เจ้าของคีย์ เช่น example@gmail.com",1);keyEmailField.setSingleLine(true);keyEmailField.setInputType(33);add(settings,keyEmailField,8);
    keyField=edit("วาง Gemini API Key ใหม่",1);keyField.setSingleLine(true);keyField.setInputType(129);add(settings,keyField,8);
    keySaveButton=button("＋ เพิ่ม API Key");add(settings,keySaveButton,10);
    keyEditButton=button("ยกเลิกการแก้ไข");add(settings,keyEditButton,8);keyEditButton.setVisibility(View.GONE);
    keySaveButton.setOnClickListener(v->{
      String newKey=keyField.getText().toString().trim();
      String name=keyNameField.getText().toString().trim();
      String email=keyEmailField.getText().toString().trim();
      if(!email.isEmpty()&&!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){message("รูปแบบอีเมลไม่ถูกต้อง");return;}
      if(newKey.isEmpty()){message("กรุณากรอก API Key ก่อนบันทึก");return;}
      try{
        JSONObject data=readKeyStore();
        JSONArray items=data.getJSONArray("items");
        if(editingKeyIndex>=0&&editingKeyIndex<items.length()){
          JSONObject old=items.getJSONObject(editingKeyIndex);
          old.put("name",name.isEmpty()?old.optString("name","บัญชี "+(editingKeyIndex+1)):name);
          old.put("email",email);
          old.put("value",newKey);
        }else{
          JSONObject item=new JSONObject().put("name",name.isEmpty()?"บัญชี "+(items.length()+1):name).put("email",email).put("value",newKey);
          items.put(item);
          if(items.length()==1)data.put("active",0);
        }
        saveKey(data.toString());
        resetKeyEditor();refreshKeyList();message("บันทึก API Key แบบเข้ารหัสเรียบร้อย");
      }catch(Exception ex){message("บันทึกไม่สำเร็จ: "+ex.getMessage());}
    });
    keyEditButton.setOnClickListener(v->{resetKeyEditor();message("ยกเลิกการแก้ไขแล้ว");});
    refreshKeyList();
    TextView note=label("ต้องใช้อินเทอร์เน็ตและอาจมีค่าใช้จ่ายตามโควตา Google");note.setTextColor(0xffaab6d5);note.setTextSize(12);add(settings,note,18);
    add(studio,label("01  ข้อความพากย์"),16);
    script=edit("พิมพ์ข้อความที่ต้องการพากย์...",5);script.setText("สวัสดีค่ะ ยินดีต้อนรับสู่ AI Voice Studio");add(studio,script,8);
    add(studio,label("02  เลือกตัวละครและเสียง"),18);
    TextView characterHelp=label("เลือกภาพตัวละคร 8 แบบ แล้วปรับเสียง Gemini เพิ่มเติมได้");characterHelp.setTextSize(12);characterHelp.setTextColor(0xffaab6d5);add(studio,characterHelp,6);
    Bitmap portraitSprite=BitmapFactory.decodeResource(getResources(),R.drawable.portrait_sprite);
    LinearLayout characterGrid=new LinearLayout(this);characterGrid.setOrientation(1);add(studio,characterGrid,10);
    for(int row=0;row<2;row++){
      LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);characterGrid.addView(line);
      for(int col=0;col<4;col++){
        final int index=row*4+col;
        LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setGravity(Gravity.CENTER);card.setPadding(dp(3),dp(3),dp(3),dp(5));card.setBackground(shape(panel));
        ImageView photo=new ImageView(this);
        if(portraitSprite!=null){
          int tileW=portraitSprite.getWidth()/4,tileH=portraitSprite.getHeight()/2;
          Bitmap portrait=Bitmap.createBitmap(portraitSprite,col*tileW,row*tileH,tileW,tileH);
          photo.setImageBitmap(portrait);
        }
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(photo,new LinearLayout.LayoutParams(-1,dp(75)));
        TextView name=label(characterNames[index]);name.setTextSize(11);name.setGravity(Gravity.CENTER);name.setSingleLine(true);name.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams nameParams=new LinearLayout.LayoutParams(-1,dp(28));card.addView(name,nameParams);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(111),1);if(col>0)cp.leftMargin=dp(5);if(row>0)cp.topMargin=dp(8);line.addView(card,cp);
        characterButtons[index]=card;card.setOnClickListener(v->selectCharacter(index));
      }
    }
    voiceAvatar=label("เลือกตัวละคร");voiceAvatar.setTextSize(14);voiceAvatar.setGravity(Gravity.CENTER);voiceAvatar.setPadding(dp(10),dp(12),dp(10),dp(12));voiceAvatar.setBackground(shape(panel));add(studio,voiceAvatar,12);
    add(studio,label("เสียง Gemini (ปรับเองได้)"),10);
    voice=spinner(VOICE_LABELS);add(studio,voice,8);
    voice.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
      public void onNothingSelected(android.widget.AdapterView<?> parent){}
      public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){
        String character=selectedCharacter>=0?characterNames[selectedCharacter]:"เสียงทั่วไป";
        voiceAvatar.setText(character+"  •  "+VOICE_LABELS[position]);
      }
    });
    add(studio,label("03  ภาษาและรูปแบบการพูด"),18);
    language=spinner(languages);add(studio,language,8);
    style=spinner(STYLES);add(studio,style,8);style.setSelection(4);selectCharacter(0);
    instructions=edit("คำสั่งเพิ่มเติม เช่น พูดอย่างอบอุ่น (ไม่บังคับ)",2);add(studio,instructions,8);
    Button generate=button("✦ สร้างเสียง Gemini AI");add(studio,generate,20);generate.setOnClickListener(v->generate());
    LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);add(studio,actions,10);
    Button play=button("▶ ฟังเสียง");actions.addView(play,new LinearLayout.LayoutParams(0,dp(54),1));play.setOnClickListener(v->play());
    Button save=button("↓ บันทึก WAV");LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(54),1);ap.leftMargin=dp(8);actions.addView(save,ap);save.setOnClickListener(v->saveAudio());
    status=label("พร้อมใช้งาน • เลือกเสียงแล้วกดสร้าง");status.setTextSize(13);status.setTextColor(0xffaab6d5);add(root,status,18);
  }
  private SecretKey secret()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(!ks.containsAlias("gemini_key")){KeyGenerator kg=KeyGenerator.getInstance("AES","AndroidKeyStore");kg.init(new KeyGenParameterSpec.Builder("gemini_key",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());kg.generateKey();}return (SecretKey)ks.getKey("gemini_key",null);}
  private void saveKey(String value)throws Exception{Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,secret());byte[] enc=c.doFinal(value.getBytes(StandardCharsets.UTF_8));getPreferences(0).edit().putString("key",Base64.getEncoder().encodeToString(enc)).putString("iv",Base64.getEncoder().encodeToString(c.getIV())).apply();}
  private String loadKey(){try{String enc=getPreferences(0).getString("key",""),iv=getPreferences(0).getString("iv","");if(enc.isEmpty())return "";Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,secret(),new GCMParameterSpec(128,Base64.getDecoder().decode(iv)));return new String(c.doFinal(Base64.getDecoder().decode(enc)),StandardCharsets.UTF_8);}catch(Exception e){return "";}}
  private JSONObject readKeyStore()throws Exception{
    String saved=loadKey();
    if(saved.isEmpty())return new JSONObject().put("active",-1).put("items",new JSONArray());
    try{
      JSONObject obj=new JSONObject(saved);
      if(obj.has("items")&&obj.optJSONArray("items")!=null)return obj;
    }catch(JSONException ignored){}
    // Migrate the original single encrypted key without losing it.
    return new JSONObject().put("active",0).put("items",new JSONArray().put(new JSONObject().put("name","Google บัญชี 1").put("value",saved)));
  }
  private String activeApiKey(){
    try{
      JSONObject data=readKeyStore();
      JSONArray items=data.getJSONArray("items");
      int active=data.optInt("active",-1);
      if(active>=0&&active<items.length())return items.getJSONObject(active).optString("value","");
    }catch(Exception ignored){}
    return "";
  }
  private void resetKeyEditor(){
    editingKeyIndex=-1;keyNameField.setText("");keyEmailField.setText("");keyField.setText("");
    keySaveButton.setText("＋ เพิ่ม API Key");keyEditButton.setVisibility(View.GONE);
  }
  private void refreshKeyList(){
    if(keyList==null)return;
    keyList.removeAllViews();
    try{
      JSONObject data=readKeyStore();
      JSONArray items=data.getJSONArray("items");
      int active=data.optInt("active",-1);
      if(items.length()==0){TextView empty=label("ยังไม่มี API Key • เพิ่มคีย์แรกด้านล่าง");empty.setTextColor(0xffaab6d5);add(keyList,empty,4);}
      for(int i=0;i<items.length();i++){
        final int index=i;
        JSONObject item=items.getJSONObject(i);
        LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(12),dp(12),dp(12),dp(12));card.setBackground(shape(i==active?0xff384468:panel));add(keyList,card,8);
        String name=item.optString("name","บัญชี "+(i+1));
        TextView heading=label(name+(i==active?"  ✓ เปิดใช้งาน":"  • ยังไม่เลือก"));heading.setTextSize(15);add(card,heading,0);
        String email=item.optString("email","");
        TextView emailLabel=label(email.isEmpty()?"ยังไม่ได้ระบุ Gmail":email);emailLabel.setTextSize(12);emailLabel.setTextColor(0xffaab6d5);add(card,emailLabel,4);
        String value=item.optString("value","");
        TextView masked=label("••••••••••••"+(value.length()>4?value.substring(value.length()-4):""));masked.setTextColor(0xffaab6d5);masked.setTextSize(12);add(card,masked,5);
        LinearLayout controls=new LinearLayout(this);controls.setOrientation(0);add(card,controls,8);
        Button activate=button(i==active?"✓ กำลังใช้":"เปิดใช้งาน");activate.setEnabled(i!=active);
        controls.addView(activate,new LinearLayout.LayoutParams(0,dp(44),2));
        activate.setOnClickListener(v->{try{JSONObject d=readKeyStore();d.put("active",index);saveKey(d.toString());refreshKeyList();message("เปิดใช้งาน "+name+" แล้ว");}catch(Exception e){message("เปลี่ยนคีย์ไม่สำเร็จ: "+e.getMessage());}});
        Button editButton=button("แก้ไข");LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(0,dp(44),1);ep.leftMargin=dp(5);controls.addView(editButton,ep);
        editButton.setOnClickListener(v->{editingKeyIndex=index;keyNameField.setText(name);keyEmailField.setText(email);keyField.setText("");keyField.setHint("วางคีย์ใหม่สำหรับ "+name);keySaveButton.setText("บันทึกการแก้ไข");keyEditButton.setVisibility(View.VISIBLE);message("ใส่ API Key ใหม่เพื่อแทนคีย์ของ "+name);});
        Button deleteButton=button("ลบ");LinearLayout.LayoutParams delp=new LinearLayout.LayoutParams(0,dp(44),1);delp.leftMargin=dp(5);controls.addView(deleteButton,delp);
        deleteButton.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("ลบ "+name+"?").setMessage("ลบคีย์นี้ออกจากมือถือโดยถาวรหรือไม่?").setNegativeButton("ยกเลิก",null).setPositiveButton("ลบ",(dialog,which)->{
          try{
            JSONObject d=readKeyStore();JSONArray arr=d.getJSONArray("items");JSONArray remaining=new JSONArray();
            for(int j=0;j<arr.length();j++)if(j!=index)remaining.put(arr.getJSONObject(j));
            int current=d.optInt("active",-1);
            int next=remaining.length()==0?-1:current==index?0:current>index?current-1:current;
            d.put("items",remaining).put("active",next);saveKey(d.toString());resetKeyEditor();refreshKeyList();message("ลบ "+name+" แล้ว");
          }catch(Exception e){message("ลบไม่สำเร็จ: "+e.getMessage());}
        }).show());
      }
    }catch(Exception e){TextView err=label("อ่านข้อมูลคีย์ไม่ได้: "+e.getMessage());add(keyList,err,4);}
  }
  private String quotaMessage(JSONObject error){
    String raw=error.optString("message","");
    String lower=raw.toLowerCase(java.util.Locale.ROOT);
    boolean daily=lower.contains("per day")||lower.contains("daily")||lower.contains("free_tier_requests");
    String wait="";
    java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)please retry in\\s*([0-9]+(?:\\.[0-9]+)?(?:h|m|s))+").matcher(raw);
    if(m.find()){
      String value=m.group().replaceFirst("(?i)please retry in\\s*","");
      java.util.regex.Matcher unit=java.util.regex.Pattern.compile("([0-9]+)(?:\\.[0-9]+)?([hms])").matcher(value);
      StringBuilder result=new StringBuilder();
      while(unit.find()){if(result.length()>0)result.append(" ");String u=unit.group(2);result.append(unit.group(1)).append(u.equals("h")?" ชั่วโมง":u.equals("m")?" นาที":" วินาที");}
      wait=result.toString();
    }
    if(wait.isEmpty()){
      JSONArray details=error.optJSONArray("details");
      if(details!=null)for(int i=0;i<details.length();i++){
        JSONObject item=details.optJSONObject(i);
        if(item!=null&&item.optString("@type","").endsWith("RetryInfo")){
          String delay=item.optString("retryDelay","");
          java.util.regex.Matcher sec=java.util.regex.Pattern.compile("([0-9]+)s").matcher(delay);
          if(sec.find()){long seconds=Long.parseLong(sec.group(1));wait=(seconds/3600>0?(seconds/3600)+" ชั่วโมง ":"")+(seconds%3600/60>0?(seconds%3600/60)+" นาที ":"")+(seconds%60)+" วินาที";}
        }
      }
    }
    String result=daily?"ใช้โควตาสร้างเสียงรายวันครบแล้ว":"มีการเรียกใช้งาน Gemini มากเกินขีดจำกัด";
    return result+(wait.isEmpty()?" กรุณาลองใหม่ภายหลัง":" กรุณารอประมาณ "+wait+" แล้วลองใหม่อีกครั้ง");
  }
  private void generate(){String key=activeApiKey(),words=script.getText().toString().trim();if(key.isEmpty()||words.isEmpty()){message("กรุณากรอก API Key และข้อความ");return;}String chosenVoice=VOICES[voice.getSelectedItemPosition()],lang=codes[language.getSelectedItemPosition()],style=instructions.getText().toString().trim();message("กำลังสร้างเสียงด้วย Gemini…");audio=null;
    new Thread(()->{HttpURLConnection con=null;try{
      URL url=new URL("https://generativelanguage.googleapis.com/v1beta/models/"+MODEL+":generateContent");con=(HttpURLConnection)url.openConnection();con.setRequestMethod("POST");con.setConnectTimeout(20000);con.setReadTimeout(120000);con.setDoOutput(true);con.setRequestProperty("Content-Type","application/json");con.setRequestProperty("x-goog-api-key",key);
      JSONObject speech=new JSONObject().put("voiceConfig",new JSONObject().put("prebuiltVoiceConfig",new JSONObject().put("voiceName",chosenVoice)));
      JSONObject config=new JSONObject().put("responseModalities",new JSONArray().put("AUDIO")).put("speechConfig",speech);
      String prompt="Speak the following text in "+lang+". "+(STYLE_PROMPTS[MainActivity.this.style.getSelectedItemPosition()]+(style.isEmpty()?"":" "+style))+"\n\n"+words;
      JSONObject req=new JSONObject().put("contents",new JSONArray().put(new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",prompt))))).put("generationConfig",config);
      try(OutputStream os=con.getOutputStream()){os.write(req.toString().getBytes(StandardCharsets.UTF_8));}
      int code=con.getResponseCode();InputStream stream=code<400?con.getInputStream():con.getErrorStream();ByteArrayOutputStream buffer=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int n;while((n=stream.read(chunk))!=-1)buffer.write(chunk,0,n);JSONObject result=new JSONObject(buffer.toString("UTF-8"));
      if(code>=400){JSONObject err=result.optJSONObject("error");if(code==429){message(quotaMessage(err==null?new JSONObject():err));return;}throw new IOException("Google API "+code+": "+(err==null?"Request failed":err.optString("message")));}
      JSONArray parts=result.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts");String base64=null;for(int i=0;i<parts.length();i++){JSONObject inline=parts.getJSONObject(i).optJSONObject("inlineData");if(inline!=null){base64=inline.optString("data");break;}}
      if(base64==null||base64.isEmpty())throw new IOException("Gemini ไม่ส่งข้อมูลเสียงกลับมา");
      audio=Base64.getDecoder().decode(base64);message("สร้างเสียงสำเร็จ • "+audio.length+" bytes • กดฟังหรือบันทึก WAV");
    }catch(Exception ex){message("เกิดข้อผิดพลาด: "+ex.getMessage());}finally{if(con!=null)con.disconnect();}}).start();
  }
  private void play(){if(audio==null){message("กรุณาสร้างเสียงก่อน");return;}if(player!=null){player.stop();player.release();player=null;}byte[] pcm=audio;new Thread(()->{try{int min=AudioTrack.getMinBufferSize(24000,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);AudioTrack t=new AudioTrack(AudioManager.STREAM_MUSIC,24000,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,8192),AudioTrack.MODE_STREAM);player=t;t.play();t.write(pcm,0,pcm.length);t.stop();t.release();if(player==t)player=null;message("เล่นเสียงเสร็จแล้ว");}catch(Exception ex){message("เล่นเสียงไม่ได้: "+ex.getMessage());}}).start();}
  private void saveAudio(){if(audio==null){message("กรุณาสร้างเสียงก่อน");return;}Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("audio/wav");i.putExtra(Intent.EXTRA_TITLE,"AI-Voice-Studio.wav");startActivityForResult(i,101);}
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==101&&result==RESULT_OK&&data!=null&&audio!=null){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){writeWav(out,audio);message("บันทึกไฟล์ WAV สำเร็จ");}catch(Exception e){message("บันทึกไม่สำเร็จ: "+e.getMessage());}}}
  private void writeWav(OutputStream out,byte[] pcm)throws IOException{int len=pcm.length;ByteBuffer b=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);b.put("RIFF".getBytes(StandardCharsets.US_ASCII));b.putInt(len+36);b.put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII));b.putInt(16);b.putShort((short)1);b.putShort((short)1);b.putInt(24000);b.putInt(48000);b.putShort((short)2);b.putShort((short)16);b.put("data".getBytes(StandardCharsets.US_ASCII));b.putInt(len);out.write(b.array());out.write(pcm);}
  @Override protected void onDestroy(){if(player!=null){try{player.stop();player.release();}catch(Exception ignored){}}super.onDestroy();}
}
