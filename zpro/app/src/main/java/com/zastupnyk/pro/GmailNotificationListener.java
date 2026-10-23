package com.zastupnyk.pro;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public class GmailNotificationListener extends NotificationListenerService {
 @Override public void onNotificationPosted(StatusBarNotification sbn){
  try{
   if(!"com.google.android.gm".equals(sbn.getPackageName()))return;
   Bundle e=sbn.getNotification().extras;
   String title=s(e.getCharSequence(Notification.EXTRA_TITLE));
   String text=s(e.getCharSequence(Notification.EXTRA_TEXT));
   String big=s(e.getCharSequence(Notification.EXTRA_BIG_TEXT));
   String raw=(title+" "+text+" "+big).trim();
   String low=raw.toLowerCase(Locale.ROOT);
   if(!(low.contains("ktmsg@ukr.net")||low.contains("красноградський аграрно-технічний коледж")||low.contains("красноградський аграрно")) ) return;
   String subject=!text.isEmpty()?text:(!big.isEmpty()?big:title);
   String fp=sha(raw+"|"+(sbn.getPostTime()/60000));
   TaskDb db=new TaskDb(this);db.addTask(sbn.getPostTime(),"ktmsg@ukr.net",subject,"gmail_notification",raw,fp);
  }catch(Exception ignored){}
 }
 private static String s(CharSequence x){return x==null?"":x.toString();}
 private static String sha(String s){try{byte[]h=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:h)b.append(String.format("%02x",x));return b.toString();}catch(Exception e){return String.valueOf(s.hashCode());}}
}
