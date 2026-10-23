package com.moivitrati.pro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BankNotificationParser {
    public static class Parsed {
        public double amount; public String merchant; public String category; public String bank; public String raw; public String fingerprint;
    }

    private static final Pattern AMOUNT = Pattern.compile("(?<!\\d)(\\d{1,3}(?:[ \\u00A0]\\d{3})*(?:[.,]\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)\\s*(?:₴|грн\\.?|UAH)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final String[] SPEND = {"покуп", "оплат", "списан", "витрат", "розрах", "payment", "purchase", "paid", "знят", "переказ", "transfer"};
    private static final String[] INCOME = {"зарах", "надход", "поповнен", "отриман", "повернен", "refund", "cashback", "кешбек", "зарплат", "виплат"};

    public static Parsed parse(String appLabel, String packageName, String title, String text, long when) {
        String raw = join(title, text).trim(); if (raw.isEmpty()) return null;
        String all = normalize((appLabel+" "+packageName+" "+raw));
        String bank = detectBank(all); if (bank == null) return null;
        if (containsAny(all, INCOME) && !containsAny(all, SPEND)) return null;

        Matcher m = AMOUNT.matcher(raw); List<Double> vals = new ArrayList<>(); List<String> tokens = new ArrayList<>();
        while(m.find()){ try { double v=Double.parseDouble(m.group(1).replace(" ","").replace("\u00A0","").replace(',','.')); if(v>0){vals.add(v);tokens.add(m.group());} } catch(Exception ignored){} }
        if(vals.isEmpty()) return null;

        double amount = chooseAmount(raw, vals, tokens);
        if(amount<=0) return null;
        String merchant = extractMerchant(raw, tokens.get(0));
        String category = autoCategory(normalize(merchant+" "+raw));
        Parsed p = new Parsed(); p.amount=amount; p.merchant=merchant; p.category=category; p.bank=bank; p.raw=raw;
        p.fingerprint = sha256(bank+"|"+Math.round(amount*100)+"|"+normalize(merchant)+"|"+(when/60000));
        return p;
    }

    private static double chooseAmount(String raw, List<Double> vals, List<String> tokens) {
        if(vals.size()==1)return vals.get(0);
        int best=-1,bestScore=-9999;
        Matcher m=AMOUNT.matcher(raw); int i=0;
        while(m.find() && i<vals.size()){
            int s=Math.max(0,m.start()-45), e=Math.min(raw.length(),m.end()+45); String ctx=normalize(raw.substring(s,e)); int score=0;
            if(containsAny(ctx,SPEND))score+=5; if(ctx.contains("баланс")||ctx.contains("залишок")||ctx.contains("available"))score-=7; if(ctx.contains("комісі"))score-=4;
            if(score>bestScore){bestScore=score;best=i;} i++;
        }
        return vals.get(best>=0?best:0);
    }

    private static String extractMerchant(String raw,String amountToken){
        String x=raw.replace(amountToken," ").replaceAll("(?i)(баланс|залишок|доступно|карта|картка|рахунок|комісія|успішно|операція|покупка|оплата|списано|грн|uah|₴)"," ");
        x=x.replaceAll("[\\n\\r:;•|]+"," ").replaceAll("\\s+"," ").trim();
        if(x.length()>55)x=x.substring(0,55).trim();
        return x.isEmpty()?"Банківська операція":x;
    }

    public static String autoCategory(String x){
        if(has(x,"атб","silpo","сільпо","varus","novus","metro","маркет","supermarket","продукт","fora","фора"))return "Продукти";
        if(has(x,"аптек","tabletki","anc","911","подорожник","бажаємо здоров'я","бажаємо здоровя"))return "Здоров’я";
        if(has(x,"eva","prostor","watsons","makeup","космет","beauty"))return "Краса / побут";
        if(has(x,"wog","okko","upg","brsm","бенз","diesel","gas station","азс"))return "Авто";
        if(has(x,"bolt","uklon","uber","таксі","taxi","автобус","укрзалізниц","uz"))return "Транспорт";
        if(has(x,"restaurant","cafe","coffee","кафе","ресторан","pizza","kfc","mcdonald","суші","sushi"))return "Кафе / ресторани";
        if(has(x,"epicentr","епіцентр","leroy","будмат","нова лінія"))return "Дім";
        if(has(x,"rozetka","prom.ua","алло","comfy","foxtrot","технік"))return "Техніка / покупки";
        if(has(x,"vodafone","kyivstar","lifecell","інтернет","internet","mobile"))return "Зв’язок / інтернет";
        if(has(x,"комун","газопостач","електро","водоканал","тепло","yasno"))return "Комунальні";
        if(has(x,"дитяч","kid","baby","антошка","chicco","pampers","підгуз"))return "Дитина";
        if(has(x,"одяг","clothes","reserved","sinsay","lc waikiki","zara","bershka","intertop"))return "Одяг";
        if(has(x,"кіно","cinema","netflix","spotify","youtube","megogo","розваг"))return "Розваги";
        return "Інше";
    }

    private static String detectBank(String x){
        if(has(x,"а-банк","a-bank","abank","a bank"))return "А-Банк";
        if(has(x,"ощад","oschad"))return "Ощадбанк";
        if(has(x,"приват","privat"))return "ПриватБанк";
        if(has(x,"mono","monobank","універсал банк","universal bank"))return "monobank";
        return null;
    }
    private static boolean has(String x,String...keys){for(String k:keys)if(x.contains(k))return true;return false;}
    private static boolean containsAny(String x,String[] keys){for(String k:keys)if(x.contains(k))return true;return false;}
    private static String join(String a,String b){return (a==null?"":a)+" "+(b==null?"":b);}
    private static String normalize(String s){String n=Normalizer.normalize((s==null?"":s).toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replace('і','i').replace('ї','i').replace('є','е');}
    private static String sha256(String s){try{byte[] h=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:h)b.append(String.format("%02x",x));return b.toString();}catch(Exception e){return String.valueOf(s.hashCode());}}
}
