package com.zastupnyk.pro;

import android.app.*;
import android.content.*;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){
  String title=i.getStringExtra("title");String body=i.getStringExtra("body");int id=i.getIntExtra("id",1);
  NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  String ch="deadlines";
  if(Build.VERSION.SDK_INT>=26){NotificationChannel nc=new NotificationChannel(ch,"Дедлайни",NotificationManager.IMPORTANCE_HIGH);nm.createNotificationChannel(nc);}
  Intent open=new Intent(c,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(c,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,ch):new Notification.Builder(c);
  b.setSmallIcon(com.zastupnyk.pro.R.drawable.ic_launcher).setContentTitle(title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setAutoCancel(true).setContentIntent(pi);
  nm.notify(id,b.build());
 }
}
