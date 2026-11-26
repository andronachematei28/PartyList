package ro.zilefrumoase;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PersistableBundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TreeSet;

public class PlannedNotifications extends JobService {
    private static final int FIRST = 9401, SECOND = 9402;
    private static final String CHANNEL = "birthdays";
    private static final String DUE = "deliveryTime";

    static void ensureScheduled(Context context) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler.getPendingJob(FIRST) == null && scheduler.getPendingJob(SECOND) == null) schedule(context);
    }

    static void schedule(Context context) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        scheduler.cancel(FIRST);
        scheduler.cancel(SECOND);
        enqueueNext(context, FIRST);
    }

    private static void enqueueNext(Context context, int jobId) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF, MODE_PRIVATE);
        if (!prefs.getBoolean("notify", true) || prefs.getString(MainActivity.DATA,"[]").equals("[]")) return;
        TreeSet<LocalTime> slots = new TreeSet<>();
        if (prefs.getBoolean("intervalMode", prefs.getInt("interval",0)>0)) {
            LocalTime start = LocalTime.parse(prefs.getString("intervalStart","09:00"));
            int step = 60 * Math.max(1, Math.min(24,prefs.getInt("interval",2)));
            for (int minute = start.getHour()*60 + start.getMinute(); minute<1440; minute+=step)
                slots.add(LocalTime.of(minute/60,minute%60));
        } else {
            for (String entry : prefs.getString("times","09:00").split(",")) {
                String[] hm=entry.trim().split(":");
                if(hm.length!=2)continue;
                try {slots.add(LocalTime.of(Integer.parseInt(hm[0]),Integer.parseInt(hm[1])));}
                catch(IllegalArgumentException | java.time.DateTimeException invalid) { /* Ignore malformed legacy times. */ }
            }
        }
        if(slots.isEmpty())return;
        ZonedDateTime now=ZonedDateTime.now();
        long next=Long.MAX_VALUE;
        for(LocalTime slot:slots){
            ZonedDateTime candidate=now.toLocalDate().atTime(slot).atZone(now.getZone());
            if(!candidate.isAfter(now)) candidate=now.toLocalDate().plusDays(1).atTime(slot).atZone(now.getZone());
            next=Math.min(next,candidate.toInstant().toEpochMilli());
        }
        long delay=Math.max(0,next-System.currentTimeMillis());
        PersistableBundle payload=new PersistableBundle(); payload.putLong(DUE,next);
        JobInfo job=new JobInfo.Builder(jobId,new ComponentName(context,PlannedNotifications.class))
                .setMinimumLatency(delay).setOverrideDeadline(delay+30_000)
                .setPersisted(true).setExtras(payload).build();
        context.getSystemService(JobScheduler.class).schedule(job);
    }

    @Override public boolean onStartJob(JobParameters params) {
        if (getSharedPreferences(MainActivity.PREF,MODE_PRIVATE).getBoolean("notify",true)) {
            long due=params.getExtras().getLong(DUE);
            ZoneId zone=ZoneId.systemDefault();
            if(due<=System.currentTimeMillis() && Instant.ofEpochMilli(due).atZone(zone).toLocalDate()
                    .equals(java.time.LocalDate.now(zone))) postBirthdays();
            enqueueNext(this,params.getJobId()==FIRST?SECOND:FIRST);
        }
        return false;
    }

    @Override public boolean onStopJob(JobParameters params) { return true; }

    private void postBirthdays() {
        NotificationManager manager=getSystemService(NotificationManager.class);
        if(!manager.areNotificationsEnabled()) return;
        Context localized=L10n.context(this);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL,L10n.text(localized,"Aniversări"),NotificationManager.IMPORTANCE_DEFAULT));
        SharedPreferences prefs=getSharedPreferences(MainActivity.PREF,MODE_PRIVATE);
        int before=prefs.getInt("before",7), cadence=Math.max(1,prefs.getInt("every",1));
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        try{
            JSONArray people=new JSONArray(prefs.getString(MainActivity.DATA,"[]"));
            for(int i=0;i<people.length();i++){
                JSONObject person=people.optJSONObject(i);
                if(person==null)continue;
                int days=MainActivity.days(person);
                if(days>before || (days!=0 && (before-days)%cadence!=0))continue;
                String when=days==0?L10n.text(localized,"Astăzi"):days==1?L10n.text(localized,"Mâine"):L10n.text(localized,"Peste {0} zile",L10n.duration(localized,days,false));
                String title=MainActivity.displayName(person);
                Notification notification=new Notification.Builder(this,CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                    .setContentTitle(title).setContentText(L10n.text(localized,"Ziua de naștere")+" · "+when)
                    .setVisibility(Notification.VISIBILITY_PUBLIC).setAutoCancel(true)
                    .setContentIntent(open).build();
                manager.notify(i+1,notification);
            }
        }catch(JSONException invalid){android.util.Log.e("PartyList","Datele persoanelor nu pot fi citite",invalid);}
    }
}
