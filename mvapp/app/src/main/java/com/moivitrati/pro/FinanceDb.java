package com.moivitrati.pro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

public class FinanceDb extends SQLiteOpenHelper {
    public FinanceDb(Context c){super(c,"finance.db",null,1);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, amount REAL NOT NULL, category TEXT NOT NULL, merchant TEXT, note TEXT, bank TEXT, source TEXT NOT NULL, created_at INTEGER NOT NULL, raw_text TEXT, fingerprint TEXT UNIQUE)");
        db.execSQL("CREATE INDEX idx_transactions_created ON transactions(created_at)");
        db.execSQL("CREATE TABLE merchant_rules (merchant_key TEXT PRIMARY KEY, category TEXT NOT NULL)");
        db.execSQL("CREATE TABLE goals (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, target REAL NOT NULL, saved REAL NOT NULL DEFAULT 0, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE debts (id INTEGER PRIMARY KEY AUTOINCREMENT, person TEXT NOT NULL, amount REAL NOT NULL, direction TEXT NOT NULL, note TEXT, paid INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT)");
        ContentValues cv=new ContentValues();cv.put("key","monthly_budget");cv.put("value","0");db.insert("settings",null,cv);
    }
    @Override public void onUpgrade(SQLiteDatabase db,int a,int b){}

    public long addTransaction(double amount,String category,String merchant,String note,String bank,String source,long createdAt,String raw,String fp){
        ContentValues v=new ContentValues();v.put("amount",amount);v.put("category",category);v.put("merchant",merchant);v.put("note",note);v.put("bank",bank);v.put("source",source);v.put("created_at",createdAt);v.put("raw_text",raw);v.put("fingerprint",fp);
        try{return getWritableDatabase().insertOrThrow("transactions",null,v);}catch(Exception e){return -1;}
    }
    public void updateTransaction(long id,double amount,String category,String merchant,String note){
        ContentValues v=new ContentValues();v.put("amount",amount);v.put("category",category);v.put("merchant",merchant);v.put("note",note);
        getWritableDatabase().update("transactions",v,"id=?",new String[]{String.valueOf(id)});
        if(merchant!=null&&!merchant.trim().isEmpty())rememberRule(merchant,category);
    }
    public void deleteTransaction(long id){getWritableDatabase().delete("transactions","id=?",new String[]{String.valueOf(id)});}
    public JSONArray getTransactions(int limit)throws JSONException{
        JSONArray a=new JSONArray();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,amount,category,merchant,note,bank,source,created_at,raw_text FROM transactions ORDER BY created_at DESC LIMIT ?",new String[]{String.valueOf(limit)})){
            while(c.moveToNext()){JSONObject o=new JSONObject();o.put("id",c.getLong(0));o.put("amount",c.getDouble(1));o.put("category",c.getString(2));o.put("merchant",n(c.getString(3)));o.put("note",n(c.getString(4)));o.put("bank",n(c.getString(5)));o.put("source",c.getString(6));o.put("createdAt",c.getLong(7));o.put("rawText",n(c.getString(8)));a.put(o);}
        }return a;
    }
    public JSONObject getDashboard(long today,long month)throws JSONException{
        JSONObject o=new JSONObject();o.put("today",scalar("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE created_at>=?",today));o.put("month",scalar("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE created_at>=?",month));o.put("count",scalar("SELECT COUNT(*) FROM transactions WHERE created_at>=?",month));o.put("uncategorized",scalar("SELECT COUNT(*) FROM transactions WHERE created_at>=? AND category='Інше'",month));o.put("budget",getBudget());return o;
    }
    public JSONArray getCategoryStats(long month)throws JSONException{
        JSONArray a=new JSONArray();try(Cursor c=getReadableDatabase().rawQuery("SELECT category,SUM(amount) FROM transactions WHERE created_at>=? GROUP BY category ORDER BY SUM(amount) DESC",new String[]{String.valueOf(month)})){while(c.moveToNext()){JSONObject o=new JSONObject();o.put("category",c.getString(0));o.put("amount",c.getDouble(1));a.put(o);}}return a;
    }
    public JSONArray getDailyStats(long since)throws JSONException{
        JSONArray a=new JSONArray();try(Cursor c=getReadableDatabase().rawQuery("SELECT strftime('%Y-%m-%d',created_at/1000,'unixepoch','localtime'),SUM(amount) FROM transactions WHERE created_at>=? GROUP BY 1 ORDER BY 1",new String[]{String.valueOf(since)})){while(c.moveToNext()){JSONObject o=new JSONObject();o.put("date",c.getString(0));o.put("amount",c.getDouble(1));a.put(o);}}return a;
    }
    public String categoryForMerchant(String merchant,String fallback){
        String k=key(merchant);if(k.isEmpty())return fallback;try(Cursor c=getReadableDatabase().rawQuery("SELECT category FROM merchant_rules WHERE merchant_key=?",new String[]{k})){if(c.moveToFirst())return c.getString(0);}return fallback;
    }
    public void rememberRule(String merchant,String category){String k=key(merchant);if(k.isEmpty())return;ContentValues v=new ContentValues();v.put("merchant_key",k);v.put("category",category);getWritableDatabase().insertWithOnConflict("merchant_rules",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public double getBudget(){try(Cursor c=getReadableDatabase().rawQuery("SELECT value FROM settings WHERE key='monthly_budget'",null)){if(c.moveToFirst())try{return Double.parseDouble(c.getString(0));}catch(Exception ignored){}}return 0;}
    public void setBudget(double b){ContentValues v=new ContentValues();v.put("key","monthly_budget");v.put("value",String.valueOf(b));getWritableDatabase().insertWithOnConflict("settings",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public long addGoal(String title,double target,double saved){ContentValues v=new ContentValues();v.put("title",title);v.put("target",target);v.put("saved",saved);v.put("created_at",System.currentTimeMillis());return getWritableDatabase().insert("goals",null,v);}
    public void updateGoalSaved(long id,double saved){ContentValues v=new ContentValues();v.put("saved",saved);getWritableDatabase().update("goals",v,"id=?",new String[]{String.valueOf(id)});}
    public void deleteGoal(long id){getWritableDatabase().delete("goals","id=?",new String[]{String.valueOf(id)});}
    public JSONArray getGoals()throws JSONException{JSONArray a=new JSONArray();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,title,target,saved,created_at FROM goals ORDER BY created_at DESC",null)){while(c.moveToNext()){JSONObject o=new JSONObject();o.put("id",c.getLong(0));o.put("title",c.getString(1));o.put("target",c.getDouble(2));o.put("saved",c.getDouble(3));o.put("createdAt",c.getLong(4));a.put(o);}}return a;}
    public long addDebt(String person,double amount,String direction,String note){ContentValues v=new ContentValues();v.put("person",person);v.put("amount",amount);v.put("direction",direction);v.put("note",note);v.put("paid",0);v.put("created_at",System.currentTimeMillis());return getWritableDatabase().insert("debts",null,v);}
    public void toggleDebt(long id,boolean paid){ContentValues v=new ContentValues();v.put("paid",paid?1:0);getWritableDatabase().update("debts",v,"id=?",new String[]{String.valueOf(id)});}
    public void deleteDebt(long id){getWritableDatabase().delete("debts","id=?",new String[]{String.valueOf(id)});}
    public JSONArray getDebts()throws JSONException{JSONArray a=new JSONArray();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,person,amount,direction,note,paid,created_at FROM debts ORDER BY paid ASC,created_at DESC",null)){while(c.moveToNext()){JSONObject o=new JSONObject();o.put("id",c.getLong(0));o.put("person",c.getString(1));o.put("amount",c.getDouble(2));o.put("direction",c.getString(3));o.put("note",n(c.getString(4)));o.put("paid",c.getInt(5)==1);o.put("createdAt",c.getLong(6));a.put(o);}}return a;}
    public JSONObject exportAll()throws JSONException{JSONObject o=new JSONObject();o.put("version",1);o.put("transactions",getTransactions(100000));o.put("goals",getGoals());o.put("debts",getDebts());o.put("monthlyBudget",getBudget());return o;}
    public void importAll(JSONObject data)throws JSONException{
        SQLiteDatabase w=getWritableDatabase();w.beginTransaction();try{
            w.delete("transactions",null,null);w.delete("goals",null,null);w.delete("debts",null,null);w.delete("merchant_rules",null,null);
            JSONArray t=data.optJSONArray("transactions");if(t!=null)for(int i=0;i<t.length();i++){JSONObject o=t.getJSONObject(i);addTransaction(o.optDouble("amount"),o.optString("category","Інше"),o.optString("merchant"),o.optString("note"),o.optString("bank"),o.optString("source","import"),o.optLong("createdAt",System.currentTimeMillis()),o.optString("rawText"),"import-"+System.nanoTime()+"-"+i);}
            JSONArray g=data.optJSONArray("goals");if(g!=null)for(int i=0;i<g.length();i++){JSONObject o=g.getJSONObject(i);addGoal(o.optString("title","Ціль"),o.optDouble("target"),o.optDouble("saved"));}
            JSONArray d=data.optJSONArray("debts");if(d!=null)for(int i=0;i<d.length();i++){JSONObject o=d.getJSONObject(i);long id=addDebt(o.optString("person"),o.optDouble("amount"),o.optString("direction","owe_me"),o.optString("note"));if(o.optBoolean("paid"))toggleDebt(id,true);}
            setBudget(data.optDouble("monthlyBudget",0));w.setTransactionSuccessful();
        }finally{w.endTransaction();}
    }
    private double scalar(String sql,long arg){try(Cursor c=getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(arg)})){return c.moveToFirst()?c.getDouble(0):0;}}
    private static String n(String s){return s==null?"":s;}
    private static String key(String s){if(s==null)return"";String x=Normalizer.normalize(s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","");return x.replaceAll("[^a-zа-яіїєґ0-9]","");}
}
