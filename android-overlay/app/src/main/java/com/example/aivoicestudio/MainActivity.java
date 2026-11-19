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
  private static final String[] VOICES={"Kore","Puck","Charon","Aoede","Fenrir","Leda","Orus","Zephyr","Schedar","Achird","Gacrux","Pulcherrima","Sadachbia","Vindemiatrix","Zubenelgenubi"};
  private EditText script,keyField,instructions;
  private Spinner voice,language;
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
    TextView title=label("✦ AI Voice Studio • Gemini");title.setTextSize(25);add(root,title,0);
    add(root,label("Gemini API Key (เก็บเข้ารหัสในเครื่อง)"),22);
    keyField=edit("วาง Gemini API Key",1);keyField.setSingleLine(true);keyField.setInputType(129);add(root,keyField,8);
    String stored=loadKey();if(!stored.isEmpty())keyField.setText(stored);
    Button store=button("บันทึก API Key");add(root,store,8);store.setOnClickListener(v->{try{saveKey(keyField.getText().toString().trim());message("บันทึกคีย์แบบเข้ารหัสแล้ว");}catch(Exception ex){message("บันทึกคีย์ไม่สำเร็จ: "+ex.getMessage());}});
    add(root,label("ข้อความพากย์"),20);script=edit("ใส่ข้อความที่ต้องการให้ AI พูด",5);script.setText("สวัสดีค่ะ ยินดีต้อนรับสู่ AI Voice Studio");add(root,script,8);
    add(root,label("ภาษา"),18);language=spinner(languages);add(root,language,8);
    add(root,label("เสียง Gemini AI"),18);voice=spinner(VOICES);add(root,voice,8);
    add(root,label("สไตล์การพูด (ไม่บังคับ)"),18);instructions=edit("เช่น พูดอย่างอบอุ่น ชัดเจน สนุกสนาน",2);add(root,instructions,8);
    Button generate=button("▶ สร้างเสียง Gemini AI");add(root,generate,20);generate.setOnClickListener(v->generate());
    Button play=button("▶ ฟังเสียงที่สร้าง");add(root,play,10);play.setOnClickListener(v->play());
    Button save=button("↓ บันทึก WAV");add(root,save,10);save.setOnClickListener(v->saveAudio());
    status=label("พร้อมใช้งาน • ใช้ API Key ส่วนตัวของคุณ");status.setTextSize(13);add(root,status,20);
    TextView note=label("หมายเหตุ: ต้องเชื่อมต่ออินเทอร์เน็ต การใช้งาน API อาจมีค่าใช้จ่ายตามโควตา Google และเสียงที่เลือกไม่ใช่เสียงผู้ชาย/ผู้หญิงตายตัว");note.setTextColor(0xffaeb8ce);note.setTextSize(12);add(root,note,15);
  }
  private SecretKey secret()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(!ks.containsAlias("gemini_key")){KeyGenerator kg=KeyGenerator.getInstance("AES","AndroidKeyStore");kg.init(new KeyGenParameterSpec.Builder("gemini_key",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());kg.generateKey();}return (SecretKey)ks.getKey("gemini_key",null);}
  private void saveKey(String value)throws Exception{Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,secret());byte[] enc=c.doFinal(value.getBytes(StandardCharsets.UTF_8));getPreferences(0).edit().putString("key",Base64.getEncoder().encodeToString(enc)).putString("iv",Base64.getEncoder().encodeToString(c.getIV())).apply();}
  private String loadKey(){try{String enc=getPreferences(0).getString("key",""),iv=getPreferences(0).getString("iv","");if(enc.isEmpty())return "";Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,secret(),new GCMParameterSpec(128,Base64.getDecoder().decode(iv)));return new String(c.doFinal(Base64.getDecoder().decode(enc)),StandardCharsets.UTF_8);}catch(Exception e){return "";}}
  private void generate(){String key=keyField.getText().toString().trim(),words=script.getText().toString().trim();if(key.isEmpty()||words.isEmpty()){message("กรุณากรอก API Key และข้อความ");return;}String chosenVoice=VOICES[voice.getSelectedItemPosition()],lang=codes[language.getSelectedItemPosition()],style=instructions.getText().toString().trim();message("กำลังสร้างเสียงด้วย Gemini…");audio=null;
    new Thread(()->{HttpURLConnection con=null;try{
      URL url=new URL("https://generativelanguage.googleapis.com/v1beta/models/"+MODEL+":generateContent");con=(HttpURLConnection)url.openConnection();con.setRequestMethod("POST");con.setConnectTimeout(20000);con.setReadTimeout(120000);con.setDoOutput(true);con.setRequestProperty("Content-Type","application/json");con.setRequestProperty("x-goog-api-key",key);
      JSONObject speech=new JSONObject().put("voiceConfig",new JSONObject().put("prebuiltVoiceConfig",new JSONObject().put("voiceName",chosenVoice)));
      JSONObject config=new JSONObject().put("responseModalities",new JSONArray().put("AUDIO")).put("speechConfig",speech);
      String prompt="Speak the following text in "+lang+". "+(style.isEmpty()?"Use a natural, clear speaking style.":style)+"\n\n"+words;
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
