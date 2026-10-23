package com.zastupnyk.pro;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import java.util.Calendar;

public class ReminderScheduler {
 public static void cancel(Context c,long taskId){
  AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  for(int d:new int[]{7,2,1,0}){int code=(int)(taskId*10+d);PendingIntent pi=PendingIntent.getBroadcast(c,code,new Intent(c,ReminderReceiver.class),PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);if(pi!=null)am.cancel(pi);}
 }
 public static void schedule(Context c,long id,String subject,String task,long deadline){
  cancel(c,id); if(deadline<=0)return;
  AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  for(int d:new int[]{7,2,1,0}){
   Calendar cal=Calendar.getInstance();cal.setTimeInMillis(deadline);cal.add(Calendar.DAY_OF_MONTH,-d);cal.set(Calendar.HOUR_OF_DAY,9);cal.set(Calendar.MINUTE,0);cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);
   if(cal.getTimeInMillis()<=System.currentTimeMillis())continue;
   String when=d==0?"сьогодні":d==1?"завтра":"через "+d+" дні";
   Intent i=new Intent(c,ReminderReceiver.class);i.putExtra("id",(int)(id*10+d));i.putExtra("title","Заступник PRO: дедлайн "+when);i.putExtra("body",(subject==null||subject.isEmpty()?task:subject));
   PendingIntent pi=PendingIntent.getBroadcast(c,(int)(id*10+d),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);
  }
 }
 public static void rescheduleAll(Context c){
  TaskDb db=new TaskDb(c);try(Cursor cur=db.reminders()){while(cur.moveToNext()){long id=cur.getLong(0),deadline=cur.getLong(3);schedule(c,id,cur.getString(1),cur.getString(2),deadline);}}
 }
}
