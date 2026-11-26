package ro.zilefrumoase;
import android.content.*;
public class Boot extends BroadcastReceiver {public void onReceive(Context c,Intent i){PlannedNotifications.schedule(c);}}
