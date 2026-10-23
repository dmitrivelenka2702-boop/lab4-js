package com.moivitrati.pro;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView webView; private FinanceDb db; private String pendingExport="";
    private static final int VOICE=41, EXPORT=42, IMPORT=43;
    private final BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){refreshWeb();}};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);db=new FinanceDb(this);webView=new WebView(this);setContentView(webView);
        WebSettings s=webView.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(true);s.setAllowContentAccess(true);
        webView.setWebViewClient(new WebViewClient());webView.setWebChromeClient(new WebChromeClient());webView.addJavascriptInterface(new Bridge(),"Android");
        webView.loadUrl("file:///android_asset/www/index.html");
        IntentFilter f=new IntentFilter("com.moivitrati.pro.DATA_CHANGED");
        if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,f);
    }
    @Override protected void onDestroy(){try{unregisterReceiver(receiver);}catch(Exception ignored){}super.onDestroy();}
    private void refreshWeb(){runOnUiThread(()->webView.evaluateJavascript("window.refreshAll&&window.refreshAll()",null));}

    public class Bridge {
        @JavascriptInterface public String getData(){
            try{
                Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);long today=c.getTimeInMillis();c.set(Calendar.DAY_OF_MONTH,1);long month=c.getTimeInMillis();
                JSONObject o=new JSONObject();o.put("dashboard",db.getDashboard(today,month));o.put("transactions",db.getTransactions(500));o.put("categories",db.getCategoryStats(month));o.put("daily",db.getDailyStats(System.currentTimeMillis()-13L*86400000L));o.put("goals",db.getGoals());o.put("debts",db.getDebts());o.put("notificationAccess",hasAccess());return o.toString();
            }catch(Exception e){return "{\"error\":\"data\"}";}
        }
        @JavascriptInterface public long addExpense(double a,String c,String m,String n){if(a<=0)return-1;long id=db.addTransaction(a,c,m,n,"Готівка/інше","manual",System.currentTimeMillis(),"","manual-"+System.nanoTime());refreshWeb();return id;}
        @JavascriptInterface public void updateExpense(long id,double a,String c,String m,String n){db.updateTransaction(id,a,c,m,n);refreshWeb();}
        @JavascriptInterface public void deleteExpense(long id){db.deleteTransaction(id);refreshWeb();}
        @JavascriptInterface public void setBudget(double v){db.setBudget(Math.max(0,v));refreshWeb();}
        @JavascriptInterface public long addGoal(String t,double target,double saved){long id=db.addGoal(t,target,saved);refreshWeb();return id;}
        @JavascriptInterface public void updateGoal(long id,double saved){db.updateGoalSaved(id,saved);refreshWeb();}
        @JavascriptInterface public void deleteGoal(long id){db.deleteGoal(id);refreshWeb();}
        @JavascriptInterface public long addDebt(String p,double a,String d,String n){long id=db.addDebt(p,a,d,n);refreshWeb();return id;}
        @JavascriptInterface public void toggleDebt(long id,boolean paid){db.toggleDebt(id,paid);refreshWeb();}
        @JavascriptInterface public void deleteDebt(long id){db.deleteDebt(id);refreshWeb();}
        @JavascriptInterface public void openNotificationAccess(){runOnUiThread(()->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));}
        @JavascriptInterface public boolean hasNotificationAccess(){return hasAccess();}
        @JavascriptInterface public void startVoice(){runOnUiThread(MainActivity.this::voice);}
        @JavascriptInterface public void exportBackup(){runOnUiThread(()->{try{pendingExport=db.exportAll().toString(2);Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"MoiVitratiPRO-"+new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date())+".json");startActivityForResult(i,EXPORT);}catch(Exception e){toast("Не вдалося створити копію");}});}
        @JavascriptInterface public void importBackup(){runOnUiThread(()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,IMPORT);});}
    }

    private boolean hasAccess(){String e=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");return e!=null&&e.contains(getPackageName());}
    private void voice(){try{Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"uk-UA");i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Наприклад: 350 гривень аптека");startActivityForResult(i,VOICE);}catch(Exception e){toast("Голосове введення недоступне");}}
    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null)return;
        try{
            if(req==VOICE){ArrayList<String>a=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(a!=null&&!a.isEmpty()){String q=a.get(0).replace("\\","\\\\").replace("'","\\'");webView.evaluateJavascript("window.handleVoice&&window.handleVoice('"+q+"')",null);}}
            else if(req==EXPORT){Uri u=data.getData();if(u!=null)try(OutputStream out=getContentResolver().openOutputStream(u)){out.write(pendingExport.getBytes(StandardCharsets.UTF_8));toast("Резервну копію збережено");}}
            else if(req==IMPORT){Uri u=data.getData();if(u!=null){StringBuilder x=new StringBuilder();try(InputStream in=getContentResolver().openInputStream(u)){byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)x.append(new String(buf,0,n,StandardCharsets.UTF_8));}db.importAll(new JSONObject(x.toString()));refreshWeb();toast("Дані відновлено");}}
        }catch(Exception e){toast("Помилка роботи з файлом");}
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
