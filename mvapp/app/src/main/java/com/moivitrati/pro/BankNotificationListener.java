package com.moivitrati.pro;

import android.app.Notification;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class BankNotificationListener extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            Notification n = sbn.getNotification(); Bundle e = n.extras;
            String title = str(e.getCharSequence(Notification.EXTRA_TITLE));
            String text = str(e.getCharSequence(Notification.EXTRA_TEXT));
            String big = str(e.getCharSequence(Notification.EXTRA_BIG_TEXT)); if(!big.isEmpty()) text += " " + big;
            String appLabel = sbn.getPackageName();
            try { ApplicationInfo ai=getPackageManager().getApplicationInfo(sbn.getPackageName(),0); appLabel=String.valueOf(getPackageManager().getApplicationLabel(ai)); } catch(Exception ignored){}
            long when = sbn.getPostTime()>0?sbn.getPostTime():System.currentTimeMillis();
            BankNotificationParser.Parsed p = BankNotificationParser.parse(appLabel,sbn.getPackageName(),title,text,when);
            if(p==null)return;
            FinanceDb db=new FinanceDb(this); p.category=db.categoryForMerchant(p.merchant,p.category);
            long id=db.addTransaction(p.amount,p.category,p.merchant,"",p.bank,"notification",when,p.raw,p.fingerprint);
            if(id>0){Intent i=new Intent("com.moivitrati.pro.DATA_CHANGED");i.setPackage(getPackageName());sendBroadcast(i);}
        } catch(Exception ignored){}
    }
    private String str(CharSequence s){return s==null?"":s.toString();}
}
