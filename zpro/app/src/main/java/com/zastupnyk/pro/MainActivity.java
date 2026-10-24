package com.zastupnyk.pro;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.provider.Settings;
import android.webkit.*;
import android.widget.Toast;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
 private WebView web; private TaskDb db;
 @Override public void onCreate(Bundle b){
  super.onCreate(b);db=new TaskDb(this);web=new WebView(this);setContentView(web);
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(true);
  web.setWebViewClient(new WebViewClient());web.setWebChromeClient(new WebChromeClient());web.addJavascriptInterface(new Bridge(),"Android");
  web.loadUrl("file:///android_asset/www/index.html");
  if(Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},77);
 }
 @Override protected void onResume(){super.onResume();if(web!=null)web.postDelayed(()->web.evaluateJavascript("window.refreshAll&&window.refreshAll()",null),300);}
 public class Bridge {
  @JavascriptInterface public String getData(){
   try{
    JSONObject o=new JSONObject();o.put("tasks",db.all());o.put("stats",db.stats(System.currentTimeMillis(),System.currentTimeMillis()+7L*86400000L));o.put("notificationAccess",hasAccess());o.put("sender","ktmsg@ukr.net");return o.toString();
   }catch(Exception e){return "{\"tasks\":[],\"stats\":{}}";}
  }
  @JavascriptInterface public void openNotificationAccess(){runOnUiThread(()->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));}
  @JavascriptInterface public void openMailSync(){runOnUiThread(()->{Intent i=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://script.google.com/macros/s/AKfycbwolURgpDFlNqPGS8T_XTC18NFoIhkvnLy7HN4c82VxBG7WSrfODNYMtOhk3RMueAd4/exec"));startActivity(i);});}
  @JavascriptInterface public long addManual(String subject){
   long id=db.addTask(System.currentTimeMillis(),"",subject,"manual",subject,"manual-"+System.nanoTime());refresh();return id;
  }
  @JavascriptInterface public void saveTask(long id,String reg,String topic,String task,long deadline,String status,String note){
   db.save(id,reg,topic,task,deadline,status,note);
   if("done".equals(status))ReminderScheduler.cancel(MainActivity.this,id);else ReminderScheduler.schedule(MainActivity.this,id,subjectFor(id),task,deadline);
   refresh();
  }
  @JavascriptInterface public void deleteTask(long id){ReminderScheduler.cancel(MainActivity.this,id);db.delete(id);refresh();}
 }
 private String subjectFor(long id){
  try{org.json.JSONArray a=db.all();for(int i=0;i<a.length();i++){org.json.JSONObject o=a.getJSONObject(i);if(o.getLong("id")==id)return o.optString("subject");}}catch(Exception ignored){}return "Робоче завдання";
 }
 private boolean hasAccess(){String e=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");return e!=null&&e.contains(getPackageName());}
 private void refresh(){runOnUiThread(()->web.evaluateJavascript("window.refreshAll&&window.refreshAll()",null));}
}
