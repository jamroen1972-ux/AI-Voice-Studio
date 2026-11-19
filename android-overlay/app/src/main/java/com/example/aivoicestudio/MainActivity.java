package com.example.aivoicestudio;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
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
  private Spinner voice,language,style;
  private TextView voiceAvatar;
  private final Button[] characterButtons=new Button[6];
  private final String[] characterNames={"👧 เด็กผู้หญิง","👦 เด็กผู้ชาย","👩 ผู้หญิง","👨 ผู้ชาย","👴 คุณตา","👵 คุณยาย"};
  private final int[] characterVoices={5,1,7,2,22,29};
  private final int[] characterStyles={0,1,5,4,2,3};
  private int selectedCharacter=-1;
  private void selectCharacter(int index){
    selectedCharacter=index;
    for(int i=0;i<characterButtons.length;i++){
      if(characterButtons[i]!=null){characterButtons[i].setBackground(shape(i==index?accent:panel));characterButtons[i].setAlpha(i==index?1f:.82f);}
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
    add(settings,label("🔐 ตั้งค่า Gemini API Key"),12);
    TextView help=label("คีย์จะถูกเก็บเข้ารหัสในมือถือ ไม่แสดงใน GitHub");help.setTextColor(0xffaab6d5);help.setTextSize(12);add(settings,help,6);
    keyField=edit("วาง Gemini API Key",1);keyField.setSingleLine(true);keyField.setInputType(129);add(settings,keyField,12);
    String stored=loadKey();if(!stored.isEmpty())keyField.setText(stored);
    Button store=button("บันทึก API Key");add(settings,store,10);store.setOnClickListener(v->{try{saveKey(keyField.getText().toString().trim());message("บันทึกคีย์แบบเข้ารหัสแล้ว");}catch(Exception ex){message("บันทึกคีย์ไม่สำเร็จ: "+ex.getMessage());}});
    TextView note=label("ต้องใช้อินเทอร์เน็ตและอาจมีค่าใช้จ่ายตามโควตา Google");note.setTextColor(0xffaab6d5);note.setTextSize(12);add(settings,note,18);
    add(studio,label("01  ข้อความพากย์"),16);
    script=edit("พิมพ์ข้อความที่ต้องการพากย์...",5);script.setText("สวัสดีค่ะ ยินดีต้อนรับสู่ AI Voice Studio");add(studio,script,8);
    add(studio,label("02  เลือกตัวละครและเสียง"),18);
    TextView characterHelp=label("เลือกตัวละครจาก 6 กลุ่ม แล้วปรับเสียงเพิ่มเติมได้");characterHelp.setTextSize(12);characterHelp.setTextColor(0xffaab6d5);add(studio,characterHelp,6);
    LinearLayout characterGrid=new LinearLayout(this);characterGrid.setOrientation(1);add(studio,characterGrid,10);
    for(int row=0;row<2;row++){
      LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);characterGrid.addView(line);
      for(int col=0;col<3;col++){
        final int index=row*3+col;
        Button card=button(characterNames[index]);card.setTextSize(13);card.setGravity(Gravity.CENTER);card.setPadding(dp(4),dp(12),dp(4),dp(12));card.setBackground(shape(panel));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(84),1);if(col>0)cp.leftMargin=dp(7);if(row>0)cp.topMargin=dp(8);line.addView(card,cp);
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
  private void generate(){String key=keyField.getText().toString().trim(),words=script.getText().toString().trim();if(key.isEmpty()||words.isEmpty()){message("กรุณากรอก API Key และข้อความ");return;}String chosenVoice=VOICES[voice.getSelectedItemPosition()],lang=codes[language.getSelectedItemPosition()],style=instructions.getText().toString().trim();message("กำลังสร้างเสียงด้วย Gemini…");audio=null;
    new Thread(()->{HttpURLConnection con=null;try{
      URL url=new URL("https://generativelanguage.googleapis.com/v1beta/models/"+MODEL+":generateContent");con=(HttpURLConnection)url.openConnection();con.setRequestMethod("POST");con.setConnectTimeout(20000);con.setReadTimeout(120000);con.setDoOutput(true);con.setRequestProperty("Content-Type","application/json");con.setRequestProperty("x-goog-api-key",key);
      JSONObject speech=new JSONObject().put("voiceConfig",new JSONObject().put("prebuiltVoiceConfig",new JSONObject().put("voiceName",chosenVoice)));
      JSONObject config=new JSONObject().put("responseModalities",new JSONArray().put("AUDIO")).put("speechConfig",speech);
      String prompt="Speak the following text in "+lang+". "+(STYLE_PROMPTS[MainActivity.this.style.getSelectedItemPosition()]+(style.isEmpty()?"":" "+style))+"\n\n"+words;
      JSONObject req=new JSONObject().put("contents",new JSONArray().put(new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",prompt))))).put("generationConfig",config);
      try(OutputStream os=con.getOutputStream()){os.write(req.toString().getBytes(StandardCharsets.UTF_8));}
      int code=con.getResponseCode();InputStream stream=code<400?con.getInputStream():con.getErrorStream();ByteArrayOutputStream buffer=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int n;while((n=stream.read(chunk))!=-1)buffer.write(chunk,0,n);JSONObject result=new JSONObject(buffer.toString("UTF-8"));
      if(code>=400){JSONObject err=result.optJSONObject("error");throw new IOException("Google API "+code+": "+(err==null?"Request failed":err.optString("message")));}
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
