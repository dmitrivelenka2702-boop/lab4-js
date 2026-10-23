package com.zastupnyk.pro;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.*;

public class TaskDb extends SQLiteOpenHelper {
 public TaskDb(Context c){super(c,"zastupnyk.db",null,1);}
 @Override public void onCreate(SQLiteDatabase db){
  db.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT, received_at INTEGER NOT NULL, sender TEXT, subject TEXT, reg_no TEXT, topic TEXT, task_text TEXT, deadline INTEGER, status TEXT NOT NULL DEFAULT 'new', note TEXT, source TEXT, raw TEXT, fingerprint TEXT UNIQUE)");
  db.execSQL("CREATE INDEX idx_deadline ON tasks(deadline)");
 }
 @Override public void onUpgrade(SQLiteDatabase db,int a,int b){}
 public long addTask(long received,String sender,String subject,String source,String raw,String fp){
  ContentValues v=new ContentValues();v.put("received_at",received);v.put("sender",sender);v.put("subject",subject);v.put("source",source);v.put("raw",raw);v.put("fingerprint",fp);
  try{return getWritableDatabase().insertOrThrow("tasks",null,v);}catch(Exception e){return -1;}
 }
 public void save(long id,String reg,String topic,String task,long deadline,String status,String note){
  ContentValues v=new ContentValues();v.put("reg_no",reg);v.put("topic",topic);v.put("task_text",task);if(deadline>0)v.put("deadline",deadline);else v.putNull("deadline");v.put("status",status);v.put("note",note);
  getWritableDatabase().update("tasks",v,"id=?",new String[]{String.valueOf(id)});
 }
 public void delete(long id){getWritableDatabase().delete("tasks","id=?",new String[]{String.valueOf(id)});}
 public JSONArray all() throws JSONException{
  JSONArray a=new JSONArray();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,received_at,sender,subject,reg_no,topic,task_text,deadline,status,note,source,raw FROM tasks ORDER BY CASE WHEN status='done' THEN 1 ELSE 0 END, CASE WHEN deadline IS NULL THEN 1 ELSE 0 END, deadline ASC, received_at DESC",null)){
   while(c.moveToNext()){JSONObject o=new JSONObject();o.put("id",c.getLong(0));o.put("receivedAt",c.getLong(1));o.put("sender",n(c.getString(2)));o.put("subject",n(c.getString(3)));o.put("regNo",n(c.getString(4)));o.put("topic",n(c.getString(5)));o.put("taskText",n(c.getString(6)));if(!c.isNull(7))o.put("deadline",c.getLong(7));else o.put("deadline",0);o.put("status",c.getString(8));o.put("note",n(c.getString(9)));o.put("source",n(c.getString(10)));o.put("raw",n(c.getString(11)));a.put(o);}
  }return a;
 }
 public JSONObject stats(long now,long week) throws JSONException{
  JSONObject o=new JSONObject();o.put("open",scalar("SELECT COUNT(*) FROM tasks WHERE status<>'done'"));o.put("week",scalar2("SELECT COUNT(*) FROM tasks WHERE status<>'done' AND deadline>=? AND deadline<?",now,week));o.put("overdue",scalar("SELECT COUNT(*) FROM tasks WHERE status<>'done' AND deadline IS NOT NULL AND deadline<?",now));o.put("done",scalar("SELECT COUNT(*) FROM tasks WHERE status='done'"));return o;
 }
 public Cursor reminders(){return getReadableDatabase().rawQuery("SELECT id,subject,task_text,deadline,status FROM tasks WHERE deadline IS NOT NULL AND status<>'done'",null);}
 private long scalar(String sql,Object...args){String[] s=new String[args.length];for(int i=0;i<args.length;i++)s[i]=String.valueOf(args[i]);try(Cursor c=getReadableDatabase().rawQuery(sql,s)){return c.moveToFirst()?c.getLong(0):0;}}
 private long scalar2(String sql,Object a,Object b){return scalar(sql,a,b);}
 private static String n(String s){return s==null?"":s;}
}
