package ro.zilefrumoase;

import android.app.*;import android.os.*;import android.content.*;import android.content.pm.PackageManager;import android.database.Cursor;import android.graphics.*;import android.graphics.drawable.GradientDrawable;import android.net.Uri;import android.provider.ContactsContract;import android.text.*;import android.view.*;import android.widget.*;import org.json.*;import java.io.*;import java.text.*;import java.util.*;

public class MainActivity extends Activity {
    @Override protected void attachBaseContext(Context base){
        super.attachBaseContext(L10n.context(base));
    }
    String tr(String key,Object... args){return L10n.text(this,key,args);}
    String[] weekdayLabels(){
        String[] all=new java.text.DateFormatSymbols(calendarLocale).getShortWeekdays();
        return new String[]{all[2],all[3],all[4],all[5],all[6],all[7],all[1]};
    }
    String languageName(Locale locale){
        String name=locale.getDisplayName(locale);
        if(name.isEmpty())return name;
        int end=name.offsetByCodePoints(0,1);
        return name.substring(0,end).toUpperCase(locale)+name.substring(end);
    }
    void chooseLanguage(){
        String[] codes=L10n.CODES;
        String[] labels=new String[codes.length+1];labels[0]=tr("Limba dispozitivului");
        String saved=sp.getString("language","ro");int checked=0;
        for(int i=0;i<codes.length;i++){
            Locale locale=Locale.forLanguageTag(codes[i]);
            labels[i+1]=languageName(locale);
            if(codes[i].equals(saved))checked=i+1;
        }
        dialog().setTitle(tr("Limba")).setSingleChoiceItems(labels,checked,(d,n)->{
            sp.edit().putString("language",n==0?"":codes[n-1]).commit();
            d.dismiss();recreate();
        }).setNegativeButton(tr("Anulează"),null).show();
    }

 static final String PREF="zile", DATA="people"; android.content.SharedPreferences sp; JSONArray people; LinearLayout root,body,bar; int tab=0,monthOffset=0; int bg,card,ink,muted,accent,border; int birthYear=0; boolean noYear=true; float downX,downY; int settingsScroll=0,lastRenderedTab=-1; ArrayList<TextView> accentViews=new ArrayList<>(),navViews=new ArrayList<>(); ArrayList<CompoundButton> accentChecks=new ArrayList<>(); Uri pendingPhoto; Bitmap photo; EditText first,last,year; int day=1,month=1; TextView dateLabel,photoButton; ImageView photoPreview; int editing=-1; boolean dark; String exported="";
 int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+.5f);} 
 public void onCreate(Bundle b){super.onCreate(b);calendarLocale=getResources().getConfiguration().getLocales().get(0);if(b!=null){monthOffset=b.getInt("calendarOffset");chosenDay=b.getInt("calendarDay",chosenDay);yearView=b.getBoolean("calendarYear");calendarScroll=b.getInt("calendarScroll");tab=b.getInt("tab");previousTab=b.getInt("previousTab");}sp=getSharedPreferences(PREF,0);load(); theme(); if(Build.VERSION.SDK_INT>=23){ArrayList<String> p=new ArrayList<>();if(checkSelfPermission("android.permission.READ_CONTACTS")!=PackageManager.PERMISSION_GRANTED)p.add("android.permission.READ_CONTACTS");if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED)p.add("android.permission.POST_NOTIFICATIONS");if(!p.isEmpty())requestPermissions(p.toArray(new String[0]),100);} PlannedNotifications.ensureScheduled(this); show();}
 void load(){try{people=new JSONArray(sp.getString(DATA,"[]"));}catch(Exception e){people=new JSONArray();}}
 void save(){sp.edit().putString(DATA,people.toString()).apply();PlannedNotifications.schedule(this);}
 void theme(){dark=sp.getBoolean("dark",false);bg=Color.parseColor(dark?"#101010":"#F4F5F7");card=Color.parseColor(dark?"#1D1D1D":"#FFFFFF");ink=Color.parseColor(dark?"#F7F7F7":"#191C21");muted=Color.parseColor(dark?"#BDBDBD":"#59616B");border=Color.parseColor(dark?"#505050":"#BCC2C9");accent=accessibleAccent(sp.getInt("accent",Color.rgb(54,112,187)));getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);}
 int accessibleAccent(int color){int target=dark?Color.WHITE:Color.BLACK;double surface=luminance(card);int result=color;for(int i=0;i<=100;i++){double l=luminance(result);if((Math.max(l,surface)+.05)/(Math.min(l,surface)+.05)>=4.5)return result;float f=(i+1)/100f;result=Color.rgb((int)(Color.red(color)*(1-f)+Color.red(target)*f),(int)(Color.green(color)*(1-f)+Color.green(target)*f),(int)(Color.blue(color)*(1-f)+Color.blue(target)*f));}return target;}
 double luminance(int c){double[] v={Color.red(c)/255.,Color.green(c)/255.,Color.blue(c)/255.};for(int i=0;i<3;i++)v[i]=v[i]<=.04045?v[i]/12.92:Math.pow((v[i]+.055)/1.055,2.4);return .2126*v[0]+.7152*v[1]+.0722*v[2];}
 android.view.ContextThemeWrapper dialogContext(){return new android.view.ContextThemeWrapper(this,getResources().getIdentifier(dark?"PartyDialogDark":"PartyDialogLight","style",getPackageName()));}
 AlertDialog.Builder dialog(){return new AlertDialog.Builder(dialogContext()){
    @Override public AlertDialog create(){
        AlertDialog result=super.create();
        result.setOnShowListener(ignored->{
            for(int id:new int[]{-1,-2,-3}){
                android.widget.Button action=result.getButton(id);
                if(action!=null){action.setAllCaps(false);action.setMaxLines(2);action.setAutoSizeTextTypeUniformWithConfiguration(10,14,1,android.util.TypedValue.COMPLEX_UNIT_SP);}
            }
        });
        return result;
    }
 };}
 GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),border);return d;}
 TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
 LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;} LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;} void pad(View v,int a){v.setPadding(dp(a),dp(a),dp(a),dp(a));}
 LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));} void gap(LinearLayout l,int h){View v=new View(this);l.addView(v,lp(1,h));}
    LinearLayout bottomNav;
    int previousTab = 0;
    boolean selecting;
    final Set<JSONObject> selected = new LinkedHashSet<>();
    final Map<JSONObject, LinearLayout> personRows = new LinkedHashMap<>();
    final Map<JSONObject, View> selectionMarks = new LinkedHashMap<>();
    TextView personCount, selectButton, sortButton, addButton, removeSelection, selectAll;
    LinearLayout bulkTools, peopleList;
    EditText searchField;
    String query = "";
    final android.view.animation.Interpolator ease = new android.view.animation.PathInterpolator(.2f, 0f, 0f, 1f);

    int mix(int from, int to, float ratio) {
        return Color.rgb(Math.round(Color.red(from)*(1-ratio)+Color.red(to)*ratio),
                Math.round(Color.green(from)*(1-ratio)+Color.green(to)*ratio),
                Math.round(Color.blue(from)*(1-ratio)+Color.blue(to)*ratio));
    }

    void press(View view, float downScale) {
        int[] active = {android.R.attr.state_pressed};
        android.animation.StateListAnimator states = new android.animation.StateListAnimator();
        for (int index = 0; index < 2; index++) {
            float scale = index == 0 ? downScale : 1f;
            android.animation.ObjectAnimator anim = android.animation.ObjectAnimator.ofPropertyValuesHolder(view,
                android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, scale),
                android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, scale));
            anim.setDuration(index == 0 ? 85 : 190);
            anim.setInterpolator(ease);
            states.addState(index == 0 ? active : new int[0], anim);
        }
        view.setStateListAnimator(states);
    }

    android.graphics.drawable.Drawable ripple(GradientDrawable base) {
        return new android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf((accent & 0x00ffffff) | 0x26000000), base, null);
    }

    TextView button(String label, String purpose, Runnable onTap) {
        TextView v = text(label, 14, accent);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        v.setContentDescription(purpose);
        v.setMaxLines(2);v.setAutoSizeTextTypeUniformWithConfiguration(10,14,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        v.setPadding(dp(8), 0, dp(8), 0);
        v.setBackground(ripple(shape(mix(card, accent, dark ? .11f : .065f), 13)));
        v.setOnClickListener(ignored -> onTap.run());
        press(v, .965f);
        accentViews.add(v);
        return v;
    }

    void makePrimary(TextView v) {
        accentViews.remove(v);
        GradientDrawable fill = shape(accent, 13);
        fill.setStroke(0, accent);
        v.setBackground(ripple(fill));
        v.setTextColor(luminance(accent) > .179 ? Color.BLACK : Color.WHITE);
    }

    void destructive(TextView v) {
        accentViews.remove(v);
        int red = dark ? 0xffff9999 : 0xff9d2236;
        v.setTextColor(red);
        GradientDrawable fill = shape(mix(card,red,.09f), 13);
        fill.setStroke(dp(1),mix(card,red,.45f));
        v.setBackground(ripple(fill));
    }

    void confirmRemoval(String message, Runnable action) {
        AlertDialog dlg = dialog().setTitle(tr("Confirmă eliminarea")).setMessage(message)
            .setNegativeButton(tr("Anulează"), null).setPositiveButton(tr("Elimină"),(d,which)->action.run()).show();
        dlg.getButton(-1).setTextColor(dark ? 0xffff9999 : 0xff9d2236);
    }

    void show() {
        theme();
        accentViews.clear(); accentChecks.clear(); navViews.clear();
        root = col(); root.setBackgroundColor(bg);
        root.setFocusableInTouchMode(true);
        setContentView(root);
        root.setOnApplyWindowInsetsListener((v,i)->{
            v.setPadding(i.getSystemWindowInsetLeft(), i.getSystemWindowInsetTop(), i.getSystemWindowInsetRight(), i.getSystemWindowInsetBottom());
            return i.consumeSystemWindowInsets();
        });
        root.requestApplyInsets();
        bar = row();
        root.addView(bar, lp(-1,-2));
        body = col(); root.addView(body, new LinearLayout.LayoutParams(-1,0,1));
        bottomNav = row(); bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setPadding(dp(16),dp(6),dp(16),dp(6));
        LinearLayout track = row(); track.setBackground(shape(card,18)); pad(track,3);
        String[] tabs = {tr("Listă"),tr("Calendar")};
        for (int i=0; i<tabs.length; i++) {
            final int destination=i;
            TextView v=button(tabs[i],tabs[i],()->go(destination));
            navViews.add(v); track.addView(v,lp(108,44));
        }
        bottomNav.addView(track);
        root.addView(bottomNav,lp(-1,-2));
        render();
    }

    void go(int destination) {
        if (tab == destination) return;
        if (destination == 2) previousTab=tab;
        selecting=false; selected.clear();
        tab=destination; render();
    }

    void header() {
        bar.removeAllViews();
        bar.setPadding(dp(10),dp(7),dp(10),dp(5));
        FrameLayout strip=new FrameLayout(this); strip.setBackground(shape(card,18));
        bar.addView(strip,lp(-1,54));
        if(tab==2){
            TextView back=button(tr("Înapoi"),tr("Înapoi"),()->go(previousTab));
            FrameLayout.LayoutParams left=new FrameLayout.LayoutParams(dp(70),dp(42),Gravity.START|Gravity.CENTER_VERTICAL);
            left.setMarginStart(dp(6)); strip.addView(back,left);
            TextView label=text(tr("Setări"),19,ink); label.setTypeface(Typeface.DEFAULT_BOLD);
            label.setGravity(Gravity.CENTER); label.setMaxLines(2); label.setAutoSizeTextTypeUniformWithConfiguration(12,19,1,android.util.TypedValue.COMPLEX_UNIT_SP); strip.addView(label,new FrameLayout.LayoutParams(dp(Math.max(110,getResources().getConfiguration().screenWidthDp-180)),-1,Gravity.CENTER));
        } else {
            TextView brand=text("PartyList",21,ink); brand.setLetterSpacing(-.045f);
            brand.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            FrameLayout.LayoutParams left=new FrameLayout.LayoutParams(-2,-1,Gravity.START|Gravity.CENTER_VERTICAL);
            left.setMarginStart(dp(12)); strip.addView(brand,left);
            TextView transfer=button(tr("Transfer date"),tr("Transfer date"),this::transfer);
            transfer.setMaxLines(1);
            strip.addView(transfer,new FrameLayout.LayoutParams(dp(Math.min(150,Math.max(103,getResources().getConfiguration().screenWidthDp-210))),dp(40),Gravity.CENTER));
            TextView gear=button("⚙",tr("Setări"),()->go(2)); gear.setTextSize(21);
            FrameLayout.LayoutParams right=new FrameLayout.LayoutParams(dp(44),dp(42),Gravity.END|Gravity.CENTER_VERTICAL);
            right.setMarginEnd(dp(5)); strip.addView(gear,right);
        }
    }

    @Override public void onBackPressed() {
        if (selecting) {setSelection(false);return;}
        if (tab==2) go(previousTab);
        else if (tab==1 && yearView) setCalendarMonth(monthOffset,chosenDay,false);
        else if (tab==1) go(0);
        else super.onBackPressed();
    }

    void render() {
        int from = lastRenderedTab;
        if (from==2 && body.getChildCount()>0 && body.getChildAt(0) instanceof ScrollView)
            settingsScroll=body.getChildAt(0).getScrollY();
        header(); bar.setVisibility(tab==1 && yearView?View.GONE:View.VISIBLE); bottomNav.setVisibility(tab==2?View.GONE:View.VISIBLE);
        for(int i=0;i<navViews.size();i++){
            TextView v=navViews.get(i);
            v.setSelected(i==tab);
            if(i==tab) makePrimary(v);
            else { v.setTextColor(muted); v.setBackground(ripple(shape(card,14))); }
        }
        body.animate().cancel(); body.removeAllViews();
        personRows.clear(); selectionMarks.clear();
        if(tab==0) list(); else if(tab==1) calendar(); else settings();
        if(tab==2 && body.getChildAt(0) instanceof ScrollView) {
            ScrollView sc=(ScrollView)body.getChildAt(0);
            sc.post(()->sc.scrollTo(0,settingsScroll));
        }
        if (root.isAttachedToWindow()) {accentViews.removeIf(v->!v.isAttachedToWindow()); accentChecks.removeIf(v->!v.isAttachedToWindow());}
        lastRenderedTab=tab;
        boolean travel=from!=-1 && from!=tab;
        body.setTranslationX(travel ? dp(tab>from?22:-22) : 0);
        body.setAlpha(travel ? .77f:1f);
        if(travel) body.animate().translationX(0).alpha(1).setDuration(235).setInterpolator(ease).start();
    }


 LinearLayout panel(){LinearLayout l=col();l.setBackground(shape(card,19));l.setElevation(0);pad(l,15);return l;}

 void transfer(){dialog().setTitle(tr("Transfer date")).setItems(new String[]{tr("Importă"), tr("Exportă")},(d,n)->{if(n==0)importFile();else exportDialog();}).show();}

 ScrollView scroll(LinearLayout parent){ScrollView s=new ScrollView(this);s.setFillViewport(false);s.setClipToPadding(false);s.setPadding(dp(18),dp(4),dp(18),dp(12));parent.addView(s,new LinearLayout.LayoutParams(-1,0,1));return s;}
 ArrayList<JSONObject> sorted(){ArrayList<JSONObject> a=new ArrayList<>();for(int i=0;i<people.length();i++)a.add(people.optJSONObject(i));java.text.Collator rosterCollator=java.text.Collator.getInstance(getResources().getConfiguration().getLocales().get(0));int mode=sp.getInt("sort",1);Collections.sort(a,(x,y)->{if(mode==0)return rosterCollator.compare(name(x),name(y));int z=Integer.compare(days(x),days(y));return mode==1?z:-z;});return a;}
 String normalize(String s){return java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);}
 static String displayName(JSONObject person) {
     return (person.optString("last").trim() + " " + person.optString("first").trim()).trim();
 }
 String name(JSONObject person) {
     return displayName(person);
 }
 int index(JSONObject o){for(int i=0;i<people.length();i++)if(people.optJSONObject(i)==o)return i;return -1;}
 static Calendar birthday(JSONObject o){Calendar now=Calendar.getInstance(),c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);c.set(Calendar.DAY_OF_MONTH,1);c.set(Calendar.MONTH,o.optInt("month",1)-1);int d=o.optInt("day",1);c.set(Calendar.DAY_OF_MONTH,Math.min(d,c.getActualMaximum(Calendar.DAY_OF_MONTH)));Calendar today=(Calendar)now.clone();today.set(Calendar.HOUR_OF_DAY,0);today.set(Calendar.MINUTE,0);today.set(Calendar.SECOND,0);today.set(Calendar.MILLISECOND,0);if(c.before(today)){c.add(Calendar.YEAR,1);c.set(Calendar.DAY_OF_MONTH,1);c.set(Calendar.MONTH,o.optInt("month",1)-1);c.set(Calendar.DAY_OF_MONTH,Math.min(d,c.getActualMaximum(Calendar.DAY_OF_MONTH)));}return c;}
 static int days(JSONObject o){Calendar a=Calendar.getInstance(),b=birthday(o);a.set(Calendar.HOUR_OF_DAY,12);a.set(Calendar.MINUTE,0);a.set(Calendar.SECOND,0);a.set(Calendar.MILLISECOND,0);b.set(Calendar.HOUR_OF_DAY,12);return (int)Math.round((b.getTimeInMillis()-a.getTimeInMillis())/86400000.0);}
 String date(JSONObject o){
    Calendar date=new GregorianCalendar(o.optInt("year",0)>0?o.optInt("year"):2000,o.optInt("month")-1,o.optInt("day"));
    String pattern=android.text.format.DateFormat.getBestDateTimePattern(calendarLocale,o.optInt("year",0)>0?"yMMdd":"MMdd");
    return new SimpleDateFormat(pattern,calendarLocale).format(date.getTime());
 } String remaining(JSONObject o){int n=days(o);String r=n==0?tr("Astăzi"):n==1?tr("Mâine"):tr("Peste {0} zile",L10n.duration(this,n,false));int y=o.optInt("year",0);if(y>0){int age=birthday(o).get(Calendar.YEAR)-y;r+="  ·  "+L10n.years(this,age);}return r;}
 Bitmap bitmap(JSONObject o){try{String p=o.optString("photo","");if(p.isEmpty())return null;byte[] b=android.util.Base64.decode(p,0);return BitmapFactory.decodeByteArray(b,0,b.length);}catch(Exception e){return null;}}
 ImageView avatar(JSONObject o,int size){ImageView v=new ImageView(this);Bitmap b=bitmap(o);if(b!=null)v.setImageBitmap(b);else v.setImageBitmap(initials(o));v.setScaleType(ImageView.ScaleType.CENTER_CROP);v.setBackground(shape(bg,12));v.setClipToOutline(true);v.setContentDescription(name(o));return v;}
    static String roUnit(int count,String unit) {
        int tail=Math.abs(count)%100;
        return (tail>=20 || (tail==0 && count!=0) ? " de " : " ")+unit;
    }

    String countLabel(int count) {
        return tr("Persoane: {0}",count);
    }

    void list() {
        int available = getResources().getConfiguration().screenWidthDp;
        boolean stacked = available < 600 || getResources().getConfiguration().fontScale > 1.15f;
        LinearLayout tools = col(); tools.setPadding(dp(14),dp(8),dp(14),dp(6));
        LinearLayout strip = row();
        personCount = text(countLabel(people.length()),20,ink);
        personCount.setMaxLines(1);personCount.setAutoSizeTextTypeUniformWithConfiguration(14,20,1,android.util.TypedValue.COMPLEX_UNIT_SP);personCount.setTypeface(Typeface.DEFAULT_BOLD); personCount.setLetterSpacing(-.035f);
        if (stacked) tools.addView(personCount,lp(-1,36));
        else strip.addView(personCount,new LinearLayout.LayoutParams(0,dp(48),1));
        selectButton=button(tr("Selectare"),tr("Selectare"),()->setSelection(!selecting));
        sortButton=button(tr("Sortare"),tr("Sortare"),this::sortDialog);
        addButton=button("+",tr("Adaugă persoană"),this::chooseAdd); addButton.setTextSize(23);
        makePrimary(addButton);
        for(TextView v : new TextView[]{selectButton,sortButton,addButton}) {
            LinearLayout.LayoutParams params = v==addButton?lp(40,44):new LinearLayout.LayoutParams(0,dp(44),1);
            params.setMarginStart(dp(4)); strip.addView(v,params);
        }
        tools.addView(strip,lp(-1,48));
        FrameLayout line=new FrameLayout(this);
        searchField=field(tr("Caută"),query,android.text.InputType.TYPE_CLASS_TEXT);
        searchField.setTextSize(15); searchField.setContentDescription(tr("Caută"));
        searchField.setPadding(dp(12),0,dp(8),0);
        line.addView(searchField,new FrameLayout.LayoutParams(dp(Math.min(202,available-44)),dp(38),Gravity.START|Gravity.CENTER_VERTICAL));
        bulkTools=row(); bulkTools.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        selectAll=button(tr("Toate"),tr("Selectează toate persoanele afișate"),this::toggleVisible);
        removeSelection=button(tr("Elimină"),tr("Elimină persoanele selectate"),this::confirmSelected);
        destructive(removeSelection);
        bulkTools.addView(selectAll,lp(-2,38));
        LinearLayout.LayoutParams rem=lp(-2,38); rem.setMarginStart(dp(7));
        bulkTools.addView(removeSelection,rem);
        line.addView(bulkTools,new FrameLayout.LayoutParams(-1,dp(44),Gravity.CENTER_VERTICAL));
        tools.addView(line,lp(-1,46)); body.addView(tools);
        View divider=new View(this); divider.setBackgroundColor(border); body.addView(divider,lp(-1,1));
        ScrollView sc=scroll(body); sc.setVerticalScrollBarEnabled(false);
        peopleList=col(); sc.addView(peopleList);
        for(JSONObject person:sorted()) addPersonRow(person);
        TextView empty=text(tr("Nicio persoană"),16,muted); empty.setGravity(Gravity.CENTER);
        peopleList.addView(empty,lp(-1,72));
        searchField.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){query=s.toString();filterRows(empty);}
            public void afterTextChanged(Editable s){}
        });
        filterRows(empty); refreshSelection();
    }

    void filterRows(TextView empty) {
        String filter=normalize(query.trim());
        int shown=0;
        for(Map.Entry<JSONObject,LinearLayout> entry:personRows.entrySet()){
            boolean match=normalize(name(entry.getKey())).contains(filter);
            entry.getValue().setVisibility(match?View.VISIBLE:View.GONE);
            if(match) shown++;
        }
        empty.setText(people.length()==0?tr("Nicio persoană"):tr("Niciun rezultat"));
        empty.setVisibility(shown==0?View.VISIBLE:View.GONE);
    }

    int birthdayColor(JSONObject p){
        int n=days(p);
        return n==0?(dark?0xffff969d:0xffb42332):
            n<=sp.getInt("before",7)?(dark?0xffffb56b:0xff9b4800):ink;
    }
    String countdown(JSONObject p){
        int n=days(p);return n==0?tr("Astăzi"):n==1?tr("Mâine"):tr("Peste {0} zile",L10n.duration(this,n,false));
    }
    void addAge(LinearLayout tile,JSONObject p){
        int year=p.optInt("year",0);if(year<=0)return;
        int upcoming=birthday(p).get(Calendar.YEAR)-year;
        int current=Math.max(0,upcoming-(days(p)==0?0:1)); if(days(p)==0)current++;
        int color=birthdayColor(p);
        LinearLayout ages=col();ages.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        ages.setPadding(dp(7),0,0,0);
        TextView age=text(L10n.years(this,current),30,color);age.setMaxLines(1);age.setAutoSizeTextTypeUniformWithConfiguration(12,30,1,android.util.TypedValue.COMPLEX_UNIT_SP);age.setMaxWidth(dp(118));age.setTypeface(Typeface.DEFAULT_BOLD);
        age.setContentDescription(L10n.years(this,current));
        if(days(p)==0){TextView next=text(tr("Va împlini"),11,color);next.setTypeface(Typeface.DEFAULT_BOLD);next.setMaxWidth(dp(118));next.setGravity(Gravity.END);ages.addView(next);}ages.addView(age,lp(-1,-2));
        tile.addView(ages,lp(getResources().getConfiguration().screenWidthDp<380?82:118,-2));
    }

    void addPersonRow(JSONObject person) {

        LinearLayout tile=row(); tile.setPadding(dp(12),dp(11),dp(12),dp(11));
        tile.setMinimumHeight(dp(79)); press(tile,.987f);
        LinearLayout.LayoutParams layout=lp(-1,-2); layout.topMargin=dp(7);
        peopleList.addView(tile,layout);
        FrameLayout frame=new FrameLayout(this);
        frame.addView(avatar(person,52),new FrameLayout.LayoutParams(-1,-1));
        TextView mark=text("",12,ink); mark.setGravity(Gravity.CENTER);mark.setTypeface(Typeface.DEFAULT_BOLD);
        frame.addView(mark,new FrameLayout.LayoutParams(dp(21),dp(21),Gravity.END|Gravity.BOTTOM));
        selectionMarks.put(person,mark); tile.addView(frame,lp(52,52));
        LinearLayout info=col();
        LinearLayout.LayoutParams details=new LinearLayout.LayoutParams(0,-2,1); details.setMarginStart(dp(12));
        tile.addView(info,details);
        TextView title=text(name(person),17,ink); title.setTypeface(Typeface.create("sans-serif-medium",0));
        title.setLetterSpacing(-.015f); info.addView(title);
        TextView date=text(date(person),13,muted); info.addView(date);
        int status=birthdayColor(person);
        TextView daysLeft=text(countdown(person),13,status);
        daysLeft.setTypeface(Typeface.create("sans-serif-medium",0));
        GradientDrawable chip=shape(mix(card,status,.08f),8);chip.setStroke(0,status);
        daysLeft.setBackground(chip);daysLeft.setPadding(dp(6),dp(2),dp(6),dp(2));
        LinearLayout.LayoutParams dayParams=lp(-2,-2);dayParams.topMargin=dp(3);
        info.addView(daysLeft,dayParams);
        addAge(tile,person);
        personRows.put(person,tile);
        tile.setOnClickListener(v->{if(selecting){if(!selected.remove(person))selected.add(person);refreshSelection();}else personActions(person);});
        tile.setOnLongClickListener(v->{if(!selecting){setSelection(true); selected.add(person); refreshSelection();return true;}return false;});
    }

    void setSelection(boolean value) {
        selecting=value; selected.clear();
        if (value && searchField.hasFocus()){
            ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(searchField.getWindowToken(),0);
            root.requestFocus();
        }
        refreshSelection();
    }

    void refreshSelection() {
        selectButton.setText(selecting?tr("Închide"):tr("Selectare"));
        sortButton.setEnabled(!selecting); sortButton.setAlpha(selecting?.4f:1f);
        addButton.setEnabled(!selecting); addButton.setAlpha(selecting?.4f:1f);
        bulkTools.setVisibility(selecting?View.VISIBLE:View.GONE);
        searchField.setVisibility(selecting?View.INVISIBLE:View.VISIBLE);
        removeSelection.setEnabled(!selected.isEmpty());
        removeSelection.setAlpha(selected.isEmpty()?.4f:1f);
        removeSelection.setText(selected.isEmpty()?tr("Elimină"):tr("Elimină ({0})",selected.size()));
        boolean allVisible=true;int visible=0;
        for(Map.Entry<JSONObject,LinearLayout> item:personRows.entrySet()){
            JSONObject person=item.getKey(); LinearLayout tile=item.getValue();
            boolean checked=selected.contains(person);
            if(tile.getVisibility()==View.VISIBLE){visible++;if(!checked)allVisible=false;}
            GradientDrawable base=shape(checked?mix(card,accent,.09f):card,16);
            if(checked)base.setStroke(dp(2),accent);
            tile.setBackground(ripple(base));tile.setSelected(checked);
            TextView mark=(TextView)selectionMarks.get(person);
            GradientDrawable circle=shape(checked?accent:card,20);circle.setStroke(dp(2),checked?accent:muted);
            mark.setBackground(circle);mark.setVisibility(selecting?View.VISIBLE:View.GONE);
            mark.setText(checked?"✓":"");mark.setTextColor(luminance(accent)>.179?Color.BLACK:Color.WHITE);
            mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            tile.setContentDescription(name(person)+", "+date(person)+", "+remaining(person)+(selecting?(checked?tr(" selectată"):tr(" neselectată")):""));
        }
        selectAll.setText(allVisible&&visible>0?tr("Deselectează"):tr("Toate"));
        personCount.setText(countLabel(people.length()));
    }

    void toggleVisible() {
        ArrayList<JSONObject> visible=new ArrayList<>();
        for(Map.Entry<JSONObject,LinearLayout> item:personRows.entrySet())
            if(item.getValue().getVisibility()==View.VISIBLE)visible.add(item.getKey());
        if(selected.containsAll(visible))selected.removeAll(visible);else selected.addAll(visible);
        refreshSelection();
    }

    void confirmSelected() {
        Set<JSONObject> targets=new LinkedHashSet<>(selected);
        if(targets.isEmpty()) return;
        confirmRemoval(tr("Ștergi datele și fotografiile persoanelor selectate? Număr de persoane: {0}.",targets.size()),
            ()->deletePeople(targets));
    }

    void deletePeople(Set<JSONObject> targets) {
        for(int i=people.length()-1;i>=0;i--) if(targets.contains(people.optJSONObject(i))) people.remove(i);
        save(); selected.clear(); selecting=false;
        if(tab!=0 || peopleList==null){render();return;}
        android.transition.TransitionSet motion=new android.transition.TransitionSet()
            .addTransition(new android.transition.Fade(android.transition.Fade.OUT).setDuration(125))
            .addTransition(new android.transition.ChangeBounds().setDuration(230));
        motion.setInterpolator(ease);
        android.transition.TransitionManager.beginDelayedTransition(peopleList,motion);
        for(JSONObject target:targets){
            View v=personRows.remove(target); selectionMarks.remove(target);
            if(v!=null)peopleList.removeView(v);
        }
        filterRows((TextView)peopleList.getChildAt(peopleList.getChildCount()-1));refreshSelection();
    }


 void sortDialog(){String[] s={tr("Alfabetic"),tr("Ziua cea mai apropiată"),tr("Ziua cea mai îndepărtată")};dialog().setTitle(tr("Sortare")).setSingleChoiceItems(s,sp.getInt("sort",1),(d,n)->{sp.edit().putInt("sort",n).apply();d.dismiss();render();}).setNegativeButton(tr("Închide"),null).show();}
    void personActions(JSONObject person) {
        Dialog preview = new Dialog(dialogContext());
        LinearLayout sheet = panel();
        sheet.setGravity(Gravity.CENTER_HORIZONTAL);
        sheet.setPadding(dp(18),dp(20),dp(18),dp(14));
        sheet.addView(avatar(person,96),lp(96,96));
        gap(sheet,12);
        TextView title=text(name(person),23,ink);
        title.setTypeface(Typeface.DEFAULT_BOLD); title.setGravity(Gravity.CENTER);
        sheet.addView(title);
        LinearLayout details=col();
        details.setPadding(dp(4),dp(10),dp(4),dp(2));
        details.addView(text(tr("Data nașterii: {0}",date(person)),15,ink));
        int born=person.optInt("year",0);
        if(born>0){
            int upcoming=birthday(person).get(Calendar.YEAR)-born;
            int current=Math.max(0,upcoming-(days(person)==0?0:1));
            TextView age=text(tr("Vârsta actuală: {0}",L10n.years(this,current)),15,ink);
            LinearLayout.LayoutParams ageSpacing=lp(-1,-2);ageSpacing.topMargin=dp(5);
            details.addView(age,ageSpacing);
        }
        int left=days(person);
        String until=left==0?tr("Aniversarea este astăzi"):left==1?tr("Împlinește mâine"):tr("Aniversare peste {0} zile",L10n.duration(this,left,false));
        TextView next=text(until,15,left==0?birthdayColor(person):muted);
        LinearLayout.LayoutParams nextSpacing=lp(-1,-2);nextSpacing.topMargin=dp(5);
        details.addView(next,nextSpacing);
        sheet.addView(details,lp(-1,-2));        gap(sheet,16);settingDivider(sheet);
        LinearLayout actions=row(); actions.setGravity(Gravity.CENTER);
        TextView remove=button(tr("Elimină"),tr("Elimină persoana"),()->confirmRemoval(
            tr("Elimini persoana „{0}” din PartyList? Se vor șterge data de naștere și fotografia salvată în aplicație.",name(person)),
            ()->closePreview(preview,sheet,()->deletePeople(Collections.singleton(person)))));
        destructive(remove);
        actions.addView(remove,lp(95,44));
        TextView edit=button(tr("Editează"),tr("Editează persoana"),()->closePreview(preview,sheet,()->edit(index(person),null)));
        makePrimary(edit);
        LinearLayout.LayoutParams right=lp(98,44); right.setMarginStart(dp(9));
        actions.addView(edit,right);sheet.addView(actions);
        ScrollView wrap=new ScrollView(this);wrap.addView(sheet);
        preview.setContentView(wrap);preview.setCanceledOnTouchOutside(true);
        Window window=preview.getWindow(); window.setBackgroundDrawableResource(android.R.color.transparent);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams attrs=window.getAttributes();attrs.dimAmount=.54f;
        if(Build.VERSION.SDK_INT>=31){window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);attrs.flags|=WindowManager.LayoutParams.FLAG_BLUR_BEHIND;attrs.setBlurBehindRadius(dp(13));}
        window.setAttributes(attrs);preview.show();
        window.setLayout(Math.min(root.getWidth()-dp(36),dp(380)),-2);
        sheet.setAlpha(.5f);sheet.setScaleX(.965f);sheet.setScaleY(.965f);sheet.setTranslationY(dp(14));
        sheet.animate().alpha(1).scaleX(1).scaleY(1).translationY(0).setDuration(240).setInterpolator(ease).start();
    }

    void closePreview(Dialog preview, View sheet, Runnable after) {
        sheet.animate().alpha(0).translationY(dp(12)).setDuration(130).withEndAction(()->{
            preview.dismiss();if(after!=null)after.run();
        }).start();
    }

    Bitmap initials(JSONObject person) {
        String surname=person.optString("last").trim(), given=person.optString("first").trim();
        String letters=(surname.isEmpty()?"":surname.substring(0,surname.offsetByCodePoints(0,1))) +
                (given.isEmpty()?"":given.substring(0,given.offsetByCodePoints(0,1)));
        Bitmap tile=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(tile);canvas.drawColor(mix(card,muted,.10f));
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(ink);paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif-medium",0));paint.setTextSize(47);
        canvas.drawText(letters.toUpperCase(getResources().getConfiguration().getLocales().get(0)),64,64-(paint.ascent()+paint.descent())/2,paint);
        return tile;
    }


 void chooseAdd() {
     dialog().setTitle(tr("Adaugă o persoană")).setItems(new String[]{tr("Introdu manual"), tr("Alege din contacte")}, (d, choice) -> {
         if (choice == 0) {
             edit(-1, null);
             return;
         }
         try {
             startActivityForResult(new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI), 11);
         } catch (ActivityNotFoundException e) {
             Toast.makeText(this, tr("Agenda nu este disponibilă"), Toast.LENGTH_SHORT).show();
         }
     }).show();
 }
 EditText field(String hint,String value,int type){EditText e=new EditText(this);e.setSingleLine(true);e.setTextColor(ink);e.setHintTextColor(muted);e.setTextSize(16);e.setHint(hint);e.setText(value);e.setInputType(type);e.setPadding(dp(14),0,dp(14),0);e.setBackground(shape(bg,12));return e;}
 void edit(int ix, JSONObject initial) {
     editing = ix;
     JSONObject person = ix < 0 ? initial : people.optJSONObject(ix);
     String family = person == null ? "" : person.optString("last").trim();
     String given = person == null ? "" : person.optString("first").trim();
     // Older versions allowed a record with only a given or display name.
     if (family.isEmpty()) {
         family = given;
         given = "";
     }
     day = person == null ? 0 : person.optInt("day");
     month = person == null ? 0 : person.optInt("month");
     birthYear = person == null ? 0 : person.optInt("year");
     noYear = false;
     photo = person == null ? null : bitmap(person);
     Bitmap originalPhoto = photo;

     LinearLayout form = col();
     pad(form, 18);
     form.setFocusableInTouchMode(true);
     ScrollView scroll = new ScrollView(this);
     scroll.addView(form);
     int inputType = android.text.InputType.TYPE_CLASS_TEXT
             | android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME
             | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS;
     last = editorField(form, tr("Nume"), family, inputType);
     first = editorField(form, tr("Prenume (opțional)"), given, inputType);
     form.addView(text(tr("Data nașterii"), 13, muted));
     gap(form, 4);
     dateLabel = button("", tr("Alege data nașterii"), this::chooseDate);
     dateLabel.setPadding(dp(16), 0, dp(16), 0);
     updateDateLabel();
     form.addView(dateLabel, lp(-2, 48));
     gap(form, 16);
     LinearLayout photoRow = row();
     photoPreview = new ImageView(this);
     photoPreview.setContentDescription(tr("Fotografia aleasă"));
     photoPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
     photoPreview.setBackground(shape(bg, 8));
     photoPreview.setClipToOutline(true);
     photoPreview.setImageBitmap(photo);
     photoPreview.setVisibility(photo == null ? View.GONE : View.VISIBLE);
     LinearLayout.LayoutParams imageSpace = lp(52, 52);
     imageSpace.setMarginEnd(dp(10));
     photoRow.addView(photoPreview, imageSpace);
     photoButton = button(photo == null ? tr("Adaugă fotografie") : tr("Schimbă fotografia"),
             tr("Alege fotografia"), () -> {
                 Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                 pick.setType("image/*");
                 startActivityForResult(pick, 12);
             });
     photoButton.setTextSize(14);
     photoButton.setPadding(dp(12), 0, dp(12), 0);
     photoRow.addView(photoButton, lp(-2, 48));
     form.addView(photoRow);

     AlertDialog editor = dialog().setTitle(ix < 0 ? tr("Persoană nouă") : tr("Editează persoana"))
             .setView(scroll).setNegativeButton(tr("Anulează"), null)
             .setPositiveButton(tr("Salvează"), null).create();
     editor.setOnShowListener(ignored -> editor.getButton(-1).setOnClickListener(view -> {
         String surname = last.getText().toString().trim();
         if (surname.isEmpty()) {
             last.setError(tr("Completează numele"));
             last.requestFocus();
             return;
         }
         if (day == 0) {
             chooseDate();
             return;
         }
         try {
             JSONObject record = person == null ? new JSONObject() : new JSONObject(person.toString());
             record.put("last", surname);
             record.put("first", first.getText().toString().trim());
             record.put("day", day);
             record.put("month", month);
             record.put("year", noYear ? 0 : birthYear);
             if (photo != null && photo != originalPhoto) {
                 ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                 photo.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
                 record.put("photo", android.util.Base64.encodeToString(bytes.toByteArray(), android.util.Base64.NO_WRAP));
             }
             if (ix < 0) people.put(record);
             else people.put(ix, record);
             save();
             editor.dismiss();
             render();
             if(tab==0 && personRows.containsKey(record)) {
                 LinearLayout tile=personRows.get(record);
                 tile.setAlpha(.4f);
                 tile.setTranslationY(dp(10));
                 tile.animate().alpha(1).translationY(0).setDuration(240).setInterpolator(ease).start();
             }
         } catch (JSONException e) {
             Toast.makeText(this, tr("Persoana nu a putut fi salvată"), Toast.LENGTH_SHORT).show();
         }
     }));
     editor.show();
 }

 EditText editorField(LinearLayout form, String label, String value, int type) {
     EditText input = field("", value, type);
     input.setId(View.generateViewId());
     TextView title = text(label, 13, muted);
     title.setLabelFor(input.getId());
     form.addView(title);
     gap(form, 4);
     form.addView(input, lp(-1, 48));
     gap(form, 12);
     return input;
 }

 void updateDateLabel() {
     dateLabel.setText(day == 0 ? tr("Alege data") : String.format(Locale.ROOT, "%02d.%02d", day, month)
             + (noYear ? "" : "." + birthYear));
 }

 void chooseDate() {
     Calendar today = Calendar.getInstance();
     LinearLayout content = col();
     pad(content, 12);
     CheckBox unknown = new CheckBox(dialogContext());
     tintControl(unknown);
     unknown.setText(tr("Fără an"));
     unknown.setChecked(false);
     content.addView(unknown);
     LinearLayout selectors = row();
     selectors.setGravity(Gravity.CENTER);
     NumberPicker days = new NumberPicker(dialogContext());
     NumberPicker months = new NumberPicker(dialogContext());
     NumberPicker years = new NumberPicker(dialogContext());
     months.setMinValue(1);
     months.setMaxValue(12);
     months.setValue(month > 0 ? month : today.get(Calendar.MONTH) + 1);
     years.setMinValue(Math.min(1900, birthYear > 0 ? birthYear : 1900));
     years.setMaxValue(today.get(Calendar.YEAR));
     years.setValue(birthYear > 0 ? birthYear : today.get(Calendar.YEAR));
     Runnable updateDays = () -> {
         Calendar chosen = new GregorianCalendar(unknown.isChecked() ? 2000 : years.getValue(),
                 months.getValue() - 1, 1);
         days.setMaxValue(chosen.getActualMaximum(Calendar.DAY_OF_MONTH));
     };
     days.setMinValue(1);
     updateDays.run();
     days.setValue(day > 0 ? day : today.get(Calendar.DAY_OF_MONTH));
     months.setOnValueChangedListener((v, oldValue, newValue) -> updateDays.run());
     years.setOnValueChangedListener((v, oldValue, newValue) -> updateDays.run());
     for (NumberPicker picker : new NumberPicker[]{days, months, years}) {
         String label = picker == days ? tr("Zi") : picker == months ? tr("Lună") : tr("An");
         picker.setContentDescription(label);
         selectors.addView(picker, new LinearLayout.LayoutParams(0, dp(132), picker == years ? 1.15f : 1));
     }
     content.addView(selectors);
     years.setVisibility(View.VISIBLE);
     unknown.setOnCheckedChangeListener((v, checked) -> {
         
         years.setVisibility(checked ? View.GONE : View.VISIBLE);
         updateDays.run();
     });
     dialog().setTitle(tr("Data nașterii")).setView(content)
             .setNegativeButton(tr("Anulează"), null).setPositiveButton(tr("Aplică"), (d, which) -> {
                 selectors.clearFocus();
                 day = days.getValue();
                 month = months.getValue();
                 noYear = unknown.isChecked();
                 birthYear = noYear ? 0 : years.getValue();
                 updateDateLabel();
             }).show();
 }
 protected void onResume(){super.onResume();if(sp!=null)PlannedNotifications.ensureScheduled(this);}
 protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null)return;try{if (req == 11) {
    loadContact(data.getData());
} else if (req == 12) {
    Uri uri = data.getData();
    if (uri != null) {
        ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
        Bitmap chosen = ImageDecoder.decodeBitmap(source, (decoder, info, unused) -> {
            // ImageDecoder also applies the orientation recorded by the camera.
            int width = info.getSize().getWidth(), height = info.getSize().getHeight();
            float ratio = Math.min(1f, 2048f / Math.max(width, height));
            decoder.setTargetSize(Math.max(1, Math.round(width * ratio)), Math.max(1, Math.round(height * ratio)));
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
        crop(chosen);
    }
}
else if(req==13){prepareImport(data.getData());}else if(req==14){OutputStream out=getContentResolver().openOutputStream(data.getData());out.write(exported.getBytes("UTF-8"));out.close();Toast.makeText(this,tr("Export salvat"),0).show();}}catch(Exception e){Toast.makeText(this,tr("Operațiunea nu a reușit"),0).show();}}

 void loadContact(Uri uri) throws Exception {
     if (uri == null) return;
     JSONObject contact = new JSONObject();
     String id;
     try (Cursor cursor = getContentResolver().query(uri,
             new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME}, null, null, null)) {
         if (cursor == null || !cursor.moveToFirst()) return;
         id = cursor.getString(0);
         contact.put("last", cursor.getString(1));
     }
     try (Cursor names = getContentResolver().query(ContactsContract.Data.CONTENT_URI,
             new String[]{ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                          ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME},
             ContactsContract.Data.CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?",
             new String[]{id, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE}, null)) {
         if (names != null && names.moveToFirst() && !TextUtils.isEmpty(names.getString(0))) {
             contact.put("last", names.getString(0));
             contact.put("first", names.getString(1));
         }
     } catch (SecurityException unavailable) {
         // A picker URI can grant access to the display name without broad access to Data.
     }
     try (InputStream thumbnail = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), uri, false)) {
         if (thumbnail != null) {
             Bitmap picture = BitmapFactory.decodeStream(thumbnail);
             if (picture != null) {
                 int side = Math.min(picture.getWidth(), picture.getHeight());
                 Bitmap square = Bitmap.createBitmap(picture, (picture.getWidth()-side)/2, (picture.getHeight()-side)/2, side, side);
                 ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                 square.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
                 contact.put("photo", android.util.Base64.encodeToString(bytes.toByteArray(), android.util.Base64.NO_WRAP));
             }
         }
     } catch (IOException | SecurityException unavailable) {
         // The name can be used even if the provider has no readable photo.
     }
     edit(-1, contact);
 }
 void crop(Bitmap source) {
     LinearLayout content = col();
     pad(content, 10);
     TextView percentage = text("100%", 13, muted);
     percentage.setGravity(Gravity.CENTER);
     Crop cropper = new Crop(this, source, percentage);
     int available = root.getHeight() > 0 ? root.getHeight() : getResources().getDisplayMetrics().heightPixels;
     content.addView(cropper, new LinearLayout.LayoutParams(-1,
             Math.min(dp(380), Math.max(dp(96), available - dp(230)))));
     LinearLayout zoom = row();
     zoom.setGravity(Gravity.CENTER);
     gap(content, 8);
     zoom.addView(button("−", tr("Micșorează fotografia"), () -> cropper.stepZoom(1 / 1.3f)), lp(48, 48));
     zoom.addView(percentage, lp(66, 48));
     zoom.addView(button("+", tr("Mărește fotografia"), () -> cropper.stepZoom(1.3f)), lp(48, 48));
     content.addView(zoom);
     dialog().setTitle(tr("Potrivește fotografia")).setView(content)
             .setNegativeButton(tr("Anulează"), null).setPositiveButton(tr("Aplică"), (d, which) -> {
                 photo = cropper.result();
                 photoButton.setText(tr("Schimbă fotografia"));
                 photoPreview.setImageBitmap(photo);
                 photoPreview.setVisibility(View.VISIBLE);
             }).show();
 }

 class Crop extends View {
     private static final int MATTE = 0xff202020;
     private final Bitmap source;
     private final TextView percentage;
     private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
     private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
     private final RectF frame = new RectF();
     private final RectF destination = new RectF();
     private final ScaleGestureDetector pinch;
     private float initialScale, minScale, maxScale, scale, left, top;
     private float lastX, lastY, lastFocusX, lastFocusY;
     private int pointerId;

     Crop(Context context, Bitmap source, TextView percentage) {
         super(context);
         this.source = source;
         this.percentage = percentage;
         setContentDescription(tr("Cadru de decupare a fotografiei"));
         outline.setStyle(Paint.Style.STROKE);
         outline.setStrokeWidth(dp(2));
         outline.setColor(Color.WHITE);
         pinch = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
             public boolean onScaleBegin(ScaleGestureDetector detector) {
                 lastFocusX = detector.getFocusX();
                 lastFocusY = detector.getFocusY();
                 return true;
             }
             public boolean onScale(ScaleGestureDetector detector) {
                 float x = detector.getFocusX(), y = detector.getFocusY();
                 zoomAround(detector.getScaleFactor(), lastFocusX, lastFocusY, x, y);
                 lastFocusX = x;
                 lastFocusY = y;
                 return true;
             }
         });
         pinch.setQuickScaleEnabled(false);
     }

     @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
         if (w <= 0 || h <= 0) return;
         float side = Math.min(w, h) * .76f;
         frame.set((w-side)/2, (h-side)/2, (w+side)/2, (h+side)/2);
         initialScale = Math.min((float) w / source.getWidth(), (float) h / source.getHeight());
         minScale = Math.min(initialScale, side / Math.max(source.getWidth(), source.getHeight()));
         maxScale = Math.max(initialScale * 8, side / Math.min(source.getWidth(), source.getHeight()));
         scale = initialScale;
         left = (w - source.getWidth()*scale)/2;
         top = (h - source.getHeight()*scale)/2;
         updateTransform();
     }

     void stepZoom(float factor) {
         zoomAround(factor, frame.centerX(), frame.centerY(), frame.centerX(), frame.centerY());
     }

     private void zoomAround(float factor, float anchorX, float anchorY, float newX, float newY) {
         if (scale <= 0) return;
         float next = Math.min(maxScale, Math.max(minScale, scale * factor));
         left = newX - (anchorX - left) * next / scale;
         top = newY - (anchorY - top) * next / scale;
         scale = next;
         updateTransform();
     }

     private void updateTransform() {
         float w = source.getWidth() * scale, h = source.getHeight() * scale;
         left = constrain(left, frame.left, frame.right - w);
         top = constrain(top, frame.top, frame.bottom - h);
         destination.set(left, top, left + w, top + h);
         percentage.setText(Math.round(scale / initialScale * 100) + "%");
         invalidate();
     }

     private float constrain(float value, float a, float b) {
         return Math.min(Math.max(a,b), Math.max(Math.min(a,b), value));
     }

     @Override protected void onDraw(Canvas canvas) {
         canvas.drawColor(MATTE);
         imagePaint.setAlpha(77);
         canvas.drawBitmap(source, null, destination, imagePaint);
         canvas.save();
         canvas.clipRect(frame);
         paintSelection(canvas);
         canvas.restore();
         canvas.drawRect(frame, outline);
     }

     private void paintSelection(Canvas canvas) {
         canvas.drawColor(MATTE);
         imagePaint.setAlpha(255);
         canvas.drawBitmap(source, null, destination, imagePaint);
     }

     @Override public boolean onTouchEvent(MotionEvent event) {
         pinch.onTouchEvent(event);
         switch (event.getActionMasked()) {
             case MotionEvent.ACTION_DOWN:
                 pointerId = event.getPointerId(0);
                 lastX = event.getX();
                 lastY = event.getY();
                 getParent().requestDisallowInterceptTouchEvent(true);
                 break;
             case MotionEvent.ACTION_MOVE:
                 int index = event.findPointerIndex(pointerId);
                 if (index >= 0 && event.getPointerCount() == 1 && !pinch.isInProgress()) {
                     float x = event.getX(index), y = event.getY(index);
                     left += x - lastX;
                     top += y - lastY;
                     lastX = x;
                     lastY = y;
                     updateTransform();
                 }
                 break;
             case MotionEvent.ACTION_POINTER_UP:
                 int remaining = event.getActionIndex() == 0 ? 1 : 0;
                 pointerId = event.getPointerId(remaining);
                 lastX = event.getX(remaining);
                 lastY = event.getY(remaining);
                 break;
             case MotionEvent.ACTION_UP:
             case MotionEvent.ACTION_CANCEL:
                 getParent().requestDisallowInterceptTouchEvent(false);
                 break;
         }
         return true;
     }

     Bitmap result() {
         Bitmap output = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
         Canvas canvas = new Canvas(output);
         float ratio = output.getWidth() / frame.width();
         canvas.scale(ratio, ratio);
         canvas.translate(-frame.left, -frame.top);
         paintSelection(canvas);
         return output;
     }
 }

    void prepareImport(Uri uri) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(InputStream in=getContentResolver().openInputStream(uri)){
            byte[] buffer=new byte[8192];int count;
            while((count=in.read(buffer))!=-1){
                out.write(buffer,0,count);
                if(out.size()>30000000)throw new IOException(tr("Fișier prea mare"));
            }
        }
        JSONArray imported=new JSONObject(out.toString("UTF-8")).getJSONArray("people");
        if(imported.length()==0){
            dialog().setTitle(tr("Importă")).setMessage(tr("Fișierul nu conține persoane."))
                .setPositiveButton(tr("Închide"),null).show();
            return;
        }
        for(int i=0;i<imported.length();i++){
            JSONObject person=imported.optJSONObject(i);
            if(person==null || person.optInt("month")<1 || person.optInt("month")>12 ||
                person.optInt("day")<1 || person.optInt("day")>31 || name(person).isEmpty())
                throw new IOException(tr("Date incomplete"));
        }
        dialog().setTitle(tr("Importă ({0})",imported.length()))
            .setItems(new String[]{tr("Adaugă la listă"),tr("Înlocuiește lista")},(d,choice)->{
                if(choice==0)applyImport(imported,false);
                else confirmImportReplacement(imported);
            }).setNegativeButton(tr("Anulează"),null).show();
    }

    void confirmImportReplacement(JSONArray imported){
        int red=dark?0xffff969d:0xffb42332;
        String warning=tr("Se vor șterge toate persoanele și fotografiile din lista actuală. Persoane: {0}.",people.length());
        android.text.SpannableString message=new android.text.SpannableString(warning+"\n\n"+
            tr("Vor fi importate {0} persoane. Această acțiune nu poate fi anulată. Continui?",imported.length()));
        message.setSpan(new android.text.style.ForegroundColorSpan(red),0,warning.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        message.setSpan(new android.text.style.StyleSpan(Typeface.BOLD),0,warning.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        AlertDialog confirm=dialog().setTitle(tr("Înlocuiești lista?")).setMessage(message)
            .setNegativeButton(tr("Anulează"),null)
            .setPositiveButton(tr("Șterge și înlocuiește"),(d,which)->applyImport(imported,true)).show();
        confirm.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(red);
    }

    void applyImport(JSONArray imported,boolean replace){
        JSONArray result=new JSONArray();
        if(!replace)for(int i=0;i<people.length();i++)result.put(people.optJSONObject(i));
        for(int i=0;i<imported.length();i++)result.put(imported.optJSONObject(i));
        // Persist the complete result before changing the displayed list.
        if(!sp.edit().putString(DATA,result.toString()).commit()){
            Toast.makeText(this,tr("Importul nu a putut fi salvat"),Toast.LENGTH_LONG).show();
            return;
        }
        people=result;selecting=false;selected.clear();query="";
        calendarThumbs.evictAll();
        PlannedNotifications.schedule(this);
        render();
        Toast.makeText(this,tr("Persoane importate: {0}",imported.length()),Toast.LENGTH_SHORT).show();
    }

 void importFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,13);}
 void exportDialog(){if(people.length()==0){Toast.makeText(this,tr("Nu există persoane"),0).show();return;}LinearLayout l=col();pad(l,14);CheckBox all=new CheckBox(dialogContext());tintControl(all);all.setText(tr("Selectează toate"));all.setMinHeight(dp(48));l.addView(all);settingDivider(l);ScrollView sc=new ScrollView(this);LinearLayout list=col();sc.addView(list);CheckBox[] boxes=new CheckBox[people.length()];boolean[] changing={false};for(int i=0;i<boxes.length;i++){boxes[i]=new CheckBox(dialogContext());tintControl(boxes[i]);boxes[i].setText(name(people.optJSONObject(i)));boxes[i].setOnCheckedChangeListener((v,b)->{if(changing[0])return;boolean full=true;for(CheckBox box:boxes)if(box!=null&&!box.isChecked())full=false;changing[0]=true;all.setChecked(full);changing[0]=false;});boxes[i].setMinHeight(dp(48));list.addView(boxes[i]);}all.setOnCheckedChangeListener((v,b)->{if(changing[0])return;changing[0]=true;for(CheckBox box:boxes)box.setChecked(b);changing[0]=false;});l.addView(sc,new LinearLayout.LayoutParams(-1,Math.min(dp(300),Math.min(root.getHeight()/2,dp(48)*boxes.length))));AlertDialog d=dialog().setTitle(tr("Exportă")).setView(l).setNegativeButton(tr("Anulează"),null).setPositiveButton(tr("Exportă"),null).create();d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{try{JSONArray a=new JSONArray();for(int j=0;j<boxes.length;j++)if(boxes[j].isChecked())a.put(people.getJSONObject(j));if(a.length()==0){Toast.makeText(this,tr("Selectează persoane"),0).show();return;}JSONObject file=new JSONObject();file.put("format","partylist-1");file.put("people",a);exported=file.toString();String fn=a.length()==people.length()?tr("Lista Completă"):a.length()==1?name(a.getJSONObject(0)):countLabel(a.length());fn=fn.replaceAll("[\\\\/:*?\"<>|]","_");Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/octet-stream");intent.putExtra(Intent.EXTRA_TITLE,fn+".partylist");d.dismiss();startActivityForResult(intent,14);}catch(Exception e){Toast.makeText(this,tr("Exportul nu a reușit"),0).show();}}));d.show();}
    private Locale calendarLocale;
    private final android.util.LruCache<JSONObject, Bitmap> calendarThumbs =
            new android.util.LruCache<>(80);

    int chosenDay=Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    boolean yearView;
    int calendarScroll;
    static final int TODAY_RED=0xffe0444f;
    MonthPager activePager;
    LinearLayout yearGrid,yearAgenda;
    View selectedMini;

    int offsetFor(int year,int month) {
        Calendar now=Calendar.getInstance();
        return (year-now.get(Calendar.YEAR))*12+month-now.get(Calendar.MONTH);
    }

    void setCalendarMonth(int offset, int day, boolean overview) {
        monthOffset=offset;
        chosenDay=Math.min(day,displayMonth(offset).getActualMaximum(Calendar.DAY_OF_MONTH));
        yearView=overview;
        calendarScroll=0;
        render();
    }

    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putInt("calendarOffset",monthOffset);b.putInt("calendarDay",chosenDay);b.putBoolean("calendarYear",yearView);b.putInt("calendarScroll",calendarScroll);b.putInt("tab",tab);b.putInt("previousTab",previousTab);}

    void calendar() {
        activePager=null;
        selectedMini=null;
        LinearLayout heading=row();
        heading.setPadding(dp(10),dp(4),dp(10),0);
        TextView caption=button("", tr("Alege luna și anul"),this::pickMonth);
        caption.setTextSize(19);
        Runnable previous=()->{if(yearView)setCalendarMonth(monthOffset-12,chosenDay,true);else activePager.go(-1);};
        Runnable next=()->{if(yearView)setCalendarMonth(monthOffset+12,chosenDay,true);else activePager.go(1);};
        heading.addView(button("‹",yearView?tr("Anul precedent"):tr("Luna precedentă"),previous),lp(44,42));
        LinearLayout.LayoutParams center=new LinearLayout.LayoutParams(0,dp(42),1);
        center.setMargins(dp(7),0,dp(7),0);
        heading.addView(caption,center);
        heading.addView(button("›",yearView?tr("Anul următor"):tr("Luna următoare"),next),lp(44,42));
        body.addView(heading);
        LinearLayout tools=row();tools.setPadding(dp(10),dp(3),dp(10),dp(3));
        tools.addView(button(tr("Astăzi"),tr("Revino la ziua curentă"),()->setCalendarMonth(0,Calendar.getInstance().get(Calendar.DAY_OF_MONTH),yearView)),lp(-2,36));
        tools.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));
        tools.addView(button(yearView?tr("Lună"):tr("An întreg"),yearView?tr("Afișează luna"):tr("Afișează toate lunile"),()->setCalendarMonth(monthOffset,chosenDay,!yearView)),lp(-2,36));
        body.addView(tools);
        if(yearView){
            caption.setText(String.valueOf(displayMonth(monthOffset).get(Calendar.YEAR)));
            annualCalendar();
            return;
        }
        LinearLayout weekdays=row();
        weekdays.setPadding(dp(8),0,dp(8),0);
        for(String label:weekdayLabels()){
            TextView weekday=text(label,12,muted);weekday.setGravity(Gravity.CENTER);
            weekdays.addView(weekday,new LinearLayout.LayoutParams(0,dp(27),1));
        }
        body.addView(weekdays);
        activePager=new MonthPager(caption);
        body.addView(activePager,new LinearLayout.LayoutParams(-1,0,1));
    }

    void pickMonth() {
        Dialog dlg=new Dialog(dialogContext());
        LinearLayout sheet=panel();
        Calendar shown=displayMonth(monthOffset);
        int[] year={shown.get(Calendar.YEAR)};
        TextView label=text(String.valueOf(year[0]),22,ink);
        label.setGravity(Gravity.CENTER);
        LinearLayout top=row();
        top.addView(button("‹",tr("Anul precedent"),()->label.setText(String.valueOf(--year[0]))),lp(44,44));
        top.addView(label,new LinearLayout.LayoutParams(0,dp(44),1));
        top.addView(button("›",tr("Anul următor"),()->label.setText(String.valueOf(++year[0]))),lp(44,44));
        sheet.addView(top);
        for(int i=0;i<4;i++){
            LinearLayout line=row();sheet.addView(line);
            for(int j=0;j<3;j++){
                final int m=i*3+j;
                Calendar date=displayMonth(offsetFor(year[0],m));
                String title=calendarTitle(date,"MMMM");
                TextView choice=button(title,title,()->closePreview(dlg,sheet,()->setCalendarMonth(offsetFor(year[0],m),chosenDay,false)));
                choice.setAutoSizeTextTypeUniformWithConfiguration(10,15,1,android.util.TypedValue.COMPLEX_UNIT_SP);
                LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,dp(42),1);size.setMargins(dp(2),dp(5),dp(2),0);
                line.addView(choice,size);
            }
        }
        dlg.setContentView(sheet);dlg.setCanceledOnTouchOutside(true);
        Window w=dlg.getWindow();w.setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams attrs=w.getAttributes();attrs.dimAmount=.48f;attrs.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        if(Build.VERSION.SDK_INT>=31){attrs.flags|=WindowManager.LayoutParams.FLAG_BLUR_BEHIND;attrs.setBlurBehindRadius(dp(16));}
        w.setAttributes(attrs);
        // Blur the actual activity content even on devices without compositor blur.
        Bitmap backdrop=null;
        if(root.getWidth()>0 && root.getHeight()>0){
            backdrop=Bitmap.createBitmap(Math.max(1,root.getWidth()/14),Math.max(1,root.getHeight()/14),Bitmap.Config.ARGB_8888);
            Canvas c=new Canvas(backdrop);c.scale(backdrop.getWidth()/(float)root.getWidth(),backdrop.getHeight()/(float)root.getHeight());root.draw(c);
        }
        if(backdrop!=null){
            android.graphics.drawable.BitmapDrawable blur=new android.graphics.drawable.BitmapDrawable(getResources(),backdrop);
            blur.setFilterBitmap(true);blur.setBounds(0,0,root.getWidth(),root.getHeight());
            root.getOverlay().add(blur);dlg.setOnDismissListener(d->root.getOverlay().remove(blur));
        }
        dlg.show();w.setLayout(Math.min(dp(400),root.getWidth()-dp(28)),-2);
        sheet.setAlpha(.4f);sheet.setTranslationY(dp(12));sheet.animate().alpha(1).translationY(0).setDuration(190);
    }

    private Calendar displayMonth(int offset) {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.DAY_OF_MONTH, 1);
        date.add(Calendar.MONTH, offset);
        return date;
    }

    private String calendarTitle(Calendar date, String pattern) {
        String localPattern=pattern.equals("MMMM yyyy")?android.text.format.DateFormat.getBestDateTimePattern(calendarLocale,"yMMMM"):pattern;
        String label = new SimpleDateFormat(localPattern, calendarLocale).format(date.getTime());
        return label.substring(0, 1).toUpperCase(calendarLocale) + label.substring(1);
    }

    private final class MonthPager extends FrameLayout {
        private final TextView caption;
        private final int slop;
        private final android.animation.TimeInterpolator easing =
                new android.view.animation.PathInterpolator(.22f, .72f, .18f, 1f);
        private View current, neighbor;
        private android.animation.ValueAnimator animation;
        private float startX, startY, shift;
        private long gestureBegan;
        private int direction;
        private boolean swiping, maySwipe;

        MonthPager(TextView caption) {
            super(MainActivity.this);
            this.caption = caption;
            slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
            current = calendarPage(monthOffset);
            addView(current, new FrameLayout.LayoutParams(-1, -1));
            updateCaption();
        }

        private void updateCaption() {
            caption.setText(calendarTitle(displayMonth(monthOffset), "MMMM yyyy"));
        }

        private void prepare(int side) {
            if (neighbor != null && direction == side) return;
            if (neighbor != null) removeView(neighbor);
            direction = side;
            neighbor = calendarPage(monthOffset + side);
            addView(neighbor, new FrameLayout.LayoutParams(-1, -1));
            position(shift);
        }

        private void position(float value) {
            shift = Math.max(-getWidth(), Math.min(getWidth(), value));
            current.setTranslationX(shift);
            if (neighbor != null) neighbor.setTranslationX(direction * getWidth() + shift);
        }

        void go(int step) {
            if (getWidth() == 0 || animation != null || swiping) return;
            prepare(step);
            settle(true, 245);
        }

        private void settle(boolean commit, int duration) {
            final float end = commit ? -direction * getWidth() : 0;
            animation = android.animation.ValueAnimator.ofFloat(shift, end);
            animation.setDuration(duration);
            animation.setInterpolator(easing);
            animation.addUpdateListener(a -> position((float)a.getAnimatedValue()));
            animation.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator a) {
                    if (commit) {
                        removeView(current);
                        current = neighbor;
                        monthOffset += direction; calendarScroll=0; chosenDay=Math.min(chosenDay,displayMonth(monthOffset).getActualMaximum(Calendar.DAY_OF_MONTH));
                        updateCaption();
                    } else if (neighbor != null) removeView(neighbor);
                    neighbor = null;
                    direction = 0;
                    position(0);
                    animation = null;
                }
            });
            animation.start();
        }

        @Override public boolean onInterceptTouchEvent(MotionEvent event) {
            if (animation != null) return true;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    startY = event.getY();
                    gestureBegan = event.getEventTime();
                    maySwipe = startX >= dp(22) && startX <= getWidth() - dp(22);
                    swiping = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getX() - startX;
                    float dy = event.getY() - startY;
                    if (maySwipe && Math.abs(dx) > slop && Math.abs(dx) > 1.35f * Math.abs(dy)) {
                        swiping = true;
                        getParent().requestDisallowInterceptTouchEvent(true);
                        prepare(dx < 0 ? 1 : -1);
                        position(dx);
                        return true;
                    }
                    if (Math.abs(dy) > slop && Math.abs(dy) >= Math.abs(dx)) maySwipe = false;
                    break;
            }
            return false;
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (animation != null) return true;
            if (!swiping) return super.onTouchEvent(e);
            float dx = e.getX() - startX;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(dx) > slop) prepare(dx < 0 ? 1 : -1);
                    position(dx);
                    return true;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    boolean commit = e.getActionMasked() == MotionEvent.ACTION_UP
                            && dx * direction < 0
                            && (Math.abs(dx) > getWidth() * .23f ||
                                (e.getEventTime() - gestureBegan < 320 && Math.abs(dx) > dp(48)));
                    swiping = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    settle(commit, 190);
                    return true;
                default:
                    return true;
            }
        }

        @Override protected void onDetachedFromWindow() {
            if (animation != null) {
                animation.removeAllListeners();
                animation.cancel();
                animation = null;
            }
            super.onDetachedFromWindow();
        }
    }

    Map<Integer,ArrayList<JSONObject>> birthdays(Calendar month){
        Map<Integer,ArrayList<JSONObject>> result=new HashMap<>();
        int last=month.getActualMaximum(Calendar.DAY_OF_MONTH);
        for(int i=0;i<people.length();i++){
            JSONObject p=people.optJSONObject(i);
            if(p==null || p.optInt("month")!=month.get(Calendar.MONTH)+1)continue;
            int d=Math.min(last,p.optInt("day"));
            result.computeIfAbsent(d,k->new ArrayList<>()).add(p);
        }
        java.text.Collator sort=java.text.Collator.getInstance(calendarLocale);
        for(ArrayList<JSONObject> group:result.values())group.sort((a,b)->sort.compare(name(a),name(b)));
        return result;
    }
    void agenda(LinearLayout area,List<JSONObject> group){
        area.removeAllViews();
        TextView label=text(tr("Aniversări"),17,ink);label.setTypeface(Typeface.create("sans-serif-medium",0));
        area.addView(label,lp(-2,34));
        if(group==null || group.isEmpty()){
            area.addView(text(tr("Nicio aniversare astăzi"),14,muted),lp(-2,36));return;
        }
        int available=(root.getWidth()>0?root.getWidth():getResources().getDisplayMetrics().widthPixels)-dp(22);
        for(JSONObject p:group){
            LinearLayout tile=row();
            tile.setGravity(Gravity.CENTER_VERTICAL);tile.setBackground(shape(card,12));
            tile.setPadding(dp(8),dp(5),dp(12),dp(5));
            tile.addView(calendarAvatar(p),lp(32,32));
            TextView title=text(name(p),15,ink);title.setPadding(dp(9),0,0,0);title.setMaxWidth(available-dp(54));title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);
            tile.addView(title,new LinearLayout.LayoutParams(0,-2,1));addAge(tile,p);tile.setContentDescription(name(p));
            calendarClickable(tile,()->revealPerson(p),12);press(tile,.98f);
            area.addView(tile,lp(-1,-2));gap(area,4);
        }
    }

    void revealPerson(JSONObject person){
        if(!normalize(name(person)).contains(normalize(query.trim())))query="";
        go(0);
        LinearLayout tile=personRows.get(person);
        if(tile==null)return;
        ScrollView sc=(ScrollView)peopleList.getParent();
        sc.post(()->{
            if(!tile.isAttachedToWindow())return;
            int[] at=new int[2],rootAt=new int[2];sc.getLocationOnScreen(at);root.getLocationOnScreen(rootAt);
            int center=Math.max(tile.getHeight()/2,Math.min(sc.getHeight()-tile.getHeight()/2,rootAt[1]+root.getHeight()/2-at[1]));
            peopleList.post(()->{
                int limit=Math.max(0,peopleList.getHeight()+sc.getPaddingTop()+sc.getPaddingBottom()-sc.getHeight());
                int dest=Math.min(limit,Math.max(0,tile.getTop()+tile.getHeight()/2+sc.getPaddingTop()-center));
                android.animation.ValueAnimator travel=android.animation.ValueAnimator.ofInt(sc.getScrollY(),dest);
                travel.setDuration(320);travel.setInterpolator(ease);
                travel.addUpdateListener(a->sc.scrollTo(0,(int)a.getAnimatedValue()));
                travel.addListener(new android.animation.AnimatorListenerAdapter(){
                    public void onAnimationEnd(android.animation.Animator a){if(tile.isAttachedToWindow())pulsePerson(tile);}
                });travel.start();
            });
        });
    }

    void pulsePerson(View tile){
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0,0,tile.getWidth(),tile.getHeight(),new int[]{accent,accent&0xffffff,accent},null,Shader.TileMode.CLAMP));
        android.graphics.drawable.Drawable halo=new android.graphics.drawable.Drawable(){
            public void draw(Canvas c){float v=0;c.drawRoundRect(v,v,tile.getWidth()-v,tile.getHeight()-v,dp(15),dp(15),paint);}
            public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
            public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}
            public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
        };
        halo.setBounds(0,0,tile.getWidth(),tile.getHeight());halo.setAlpha(0);tile.getOverlay().add(halo);
        android.animation.ValueAnimator glow=android.animation.ValueAnimator.ofInt(0,76,0);
        glow.setDuration(1000);glow.setInterpolator(new android.view.animation.LinearInterpolator());
        glow.addUpdateListener(a->halo.setAlpha((int)a.getAnimatedValue()));
        glow.addListener(new android.animation.AnimatorListenerAdapter(){public void onAnimationEnd(android.animation.Animator a){tile.getOverlay().remove(halo);}});glow.start();
    }

    void dayBorder(View cell,boolean today,boolean selected,boolean mini){
        GradientDrawable outer=shape(mini?Color.TRANSPARENT:card,mini?3:9);
        outer.setStroke(today||selected?dp(mini?1:2):(mini?0:dp(1)),today?TODAY_RED:selected?accent:border);
        if(today && selected){
            GradientDrawable inner=shape(Color.TRANSPARENT,mini?2:6);inner.setStroke(dp(1),accent);
            android.graphics.drawable.LayerDrawable both=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{outer,inner});
            int inset=dp(mini?1:3);both.setLayerInset(1,inset,inset,inset,inset);cell.setBackground(both);
        }else cell.setBackground(outer);
        cell.setSelected(selected);
    }

    View calendarPage(int offset){
        Calendar date=displayMonth(offset);
        int last=date.getActualMaximum(Calendar.DAY_OF_MONTH),lead=(date.get(Calendar.DAY_OF_WEEK)+5)%7;
        Map<Integer,ArrayList<JSONObject>> groups=birthdays(date);
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content=col();content.setPadding(dp(8),0,dp(8),dp(12));scroll.addView(content);
        LinearLayout area=col();View[] cells=new View[last+1];
        int initial=Math.min(chosenDay,last);
        int today=offset==0?Calendar.getInstance().get(Calendar.DAY_OF_MONTH):-1;
        for(int week=0;week*7<lead+last;week++){
            LinearLayout line=row();line.setBaselineAligned(false);content.addView(line,lp(-1,-2));
            for(int j=0;j<7;j++){
                int d=week*7+j-lead+1;
                LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,-1,1);size.setMargins(dp(1),dp(2),dp(1),dp(2));
                if(d<1 || d>last){line.addView(new View(this),size);continue;}
                LinearLayout cell=col();cells[d]=cell;cell.setGravity(Gravity.CENTER);cell.setMinimumHeight(dp(42));
                cell.setPadding(dp(1),dp(3),dp(1),dp(3));line.addView(cell,size);
                dayBorder(cell,d==today,d==initial,false);
                cell.addView(text(String.valueOf(d),14,ink),lp(-2,-2));
                List<JSONObject> group=groups.get(d);
                if(group!=null){
                    boolean one=group.size()==1;
                    if(one)cell.addView(calendarAvatar(group.get(0)),lp(18,18));
                    String summary=one?name(group.get(0)):countLabel(group.size());
                    TextView count=text(summary,11,ink);count.setGravity(Gravity.CENTER);
                    count.setSingleLine(true);count.setMaxLines(1);count.setIncludeFontPadding(false);count.setEllipsize(TextUtils.TruncateAt.END);
                    if(!one){
                        count.setTypeface(Typeface.create("sans-serif-condensed",Typeface.NORMAL));
                        count.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or_,ob)->{
                            float natural=count.getPaint().measureText(summary)/count.getTextScaleX();
                            if(r>l && natural>0) count.setTextScaleX(Math.min(1f,(r-l-1f)/natural));
                        });
                    }
                    cell.addView(count,lp(-1,-2));
                }
                cell.setContentDescription(d+" "+calendarTitle(date,"MMMM yyyy").toLowerCase(calendarLocale));
                calendarClickable(cell,()->{
                    if(chosenDay<cells.length)dayBorder(cells[chosenDay],chosenDay==today,false,false);
                    chosenDay=d;dayBorder(cell,d==today,true,false);agenda(area,group);
                    area.setAlpha(.65f);area.animate().alpha(1).setDuration(150);
                },9);
            }
        }
        gap(content,7);content.addView(area);agenda(area,groups.get(initial));
        if(offset==monthOffset && calendarScroll>0){int saved=calendarScroll;scroll.post(()->scroll.scrollTo(0,saved));}
        scroll.setOnScrollChangeListener((View v,int x,int y,int ox,int oy)->{if(tab==1&&!yearView&&offset==monthOffset)calendarScroll=y;});
        return scroll;
    }

    void annualCalendar(){
        int yr=displayMonth(monthOffset).get(Calendar.YEAR);
        yearGrid=col();yearGrid.setPadding(dp(5),0,dp(5),0);
        body.addView(yearGrid,new LinearLayout.LayoutParams(-1,0,4));
        ScrollView agendaScroll=new ScrollView(this);
        yearAgenda=col();yearAgenda.setPadding(dp(10),0,dp(10),dp(3));agendaScroll.addView(yearAgenda);
        body.addView(agendaScroll,new LinearLayout.LayoutParams(-1,0,1));
        int cols=getResources().getConfiguration().orientation==2?6:3;
        LinearLayout line=null;
        for(int m=0;m<12;m++){
            if(m%cols==0){line=row();line.setBaselineAligned(false);yearGrid.addView(line,new LinearLayout.LayoutParams(-1,0,1));}
            Calendar date=displayMonth(offsetFor(yr,m));
            final int offset=offsetFor(yr,m),last=date.getActualMaximum(Calendar.DAY_OF_MONTH),lead=(date.get(Calendar.DAY_OF_WEEK)+5)%7;
            int today=offset==0?Calendar.getInstance().get(Calendar.DAY_OF_MONTH):-1;
            Map<Integer,ArrayList<JSONObject>> groups=birthdays(date);
            LinearLayout mini=col();mini.setBackground(shape(card,7));mini.setPadding(dp(2),dp(1),dp(2),dp(1));
            LinearLayout.LayoutParams item=new LinearLayout.LayoutParams(0,-1,1);item.setMargins(dp(1),dp(1),dp(1),dp(1));line.addView(mini,item);
            TextView title=text(calendarTitle(date,"MMMM"),12,ink);title.setTypeface(Typeface.DEFAULT_BOLD);title.setGravity(Gravity.CENTER);title.setMaxLines(1);title.setAutoSizeTextTypeUniformWithConfiguration(8,12,1,android.util.TypedValue.COMPLEX_UNIT_SP);title.setIncludeFontPadding(false);
            mini.addView(title,new LinearLayout.LayoutParams(-1,0,1.15f));
            calendarClickable(title,()->setCalendarMonth(offset,chosenDay,false),5);
            for(int week=0;week<6;week++){
                LinearLayout days=row();days.setBaselineAligned(false);mini.addView(days,new LinearLayout.LayoutParams(-1,0,1));
                for(int j=0;j<7;j++){
                    int d=week*7+j-lead+1;
                    LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,-1,1);
                    if(d<1 || d>last){days.addView(new View(this),size);continue;}
                    TextView key=text(String.valueOf(d),11,groups.containsKey(d)?accent:ink);key.setMaxLines(1);key.setIncludeFontPadding(false);key.setGravity(Gravity.CENTER);
                    key.setAutoSizeTextTypeUniformWithConfiguration(6,13,1,android.util.TypedValue.COMPLEX_UNIT_SP);
                    if(groups.containsKey(d))key.setTypeface(Typeface.DEFAULT_BOLD);
                    days.addView(key,size);
                    boolean selected=monthOffset==offset && d==chosenDay;
                    key.setTag(d==today);dayBorder(key,d==today,selected,true);
                    if(selected)selectedMini=key;
                    key.setContentDescription(d+" "+calendarTitle(date,"MMMM yyyy").toLowerCase(calendarLocale));
                    calendarClickable(key,()->{
                        if(selectedMini!=null)dayBorder(selectedMini,Boolean.TRUE.equals(selectedMini.getTag()),false,true);
                        selectedMini=key;monthOffset=offset;chosenDay=d;dayBorder(key,d==today,true,true);
                        agenda(yearAgenda,groups.get(d));agendaScroll.scrollTo(0,0);
                    },3);
                }
            }
            if(monthOffset==offset)agenda(yearAgenda,groups.get(chosenDay));
        }
        int saved=calendarScroll;agendaScroll.post(()->agendaScroll.scrollTo(0,saved));
        agendaScroll.setOnScrollChangeListener((View v,int x,int y,int ox,int oy)->{if(tab==1&&yearView)calendarScroll=y;});
    }


    private ImageView calendarAvatar(JSONObject person) {
        ImageView img = new ImageView(this);
        Bitmap thumb = calendarThumbs.get(person);
        if (thumb == null && !person.optString("photo").isEmpty()) {
            Bitmap photo = bitmap(person);
            if (photo != null) {
                thumb = Bitmap.createScaledBitmap(photo, 96, 96, true);
                calendarThumbs.put(person, thumb);
            }
        }
        img.setBackground(shape(bg, 5));
        img.setClipToOutline(true);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (thumb != null) img.setImageBitmap(thumb);
        else img.setImageBitmap(initials(person));
        img.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return img;
    }

    private void calendarClickable(View view, Runnable action, int radius) {
        int tint = (accent & 0x00ffffff) | 0x32000000;
        view.setForeground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(tint), null, shape(Color.WHITE, radius)));
        view.setOnClickListener(v -> action.run());
        view.setFocusable(true);
    }

    interface TimeResult { void done(String value); }

    void timePick(String initial, TimeResult callback) {
        java.time.LocalTime current = java.time.LocalTime.parse(initial);
        TimePickerDialog picker = new TimePickerDialog(dialogContext(),
                (view, hour, minute) -> callback.done(String.format(Locale.ROOT, "%02d:%02d", hour, minute)),
                current.getHour(), current.getMinute(), true);
        picker.setButton(DialogInterface.BUTTON_POSITIVE, tr("Aplică"), picker);
        picker.setButton(DialogInterface.BUTTON_NEGATIVE, tr("Anulează"), (dlg, which) -> dlg.dismiss());
        picker.show();
    }

    void settings() {
        ScrollView scrollView = scroll(body);
        LinearLayout page = col();
        scrollView.addView(page);
        gap(page, 8);
        String chosenLanguage=sp.getString("language","ro").isEmpty()?tr("Limba dispozitivului"):languageName(calendarLocale);
        FrameLayout languageRow=new FrameLayout(this);
        TextView language=button("",tr("Limba")+": "+chosenLanguage,this::chooseLanguage);
        languageRow.addView(language,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout languageLabels=row();
        languageLabels.setPadding(dp(14),0,dp(14),0);
        languageLabels.setClickable(false);
        TextView languageTitle=text(tr("Limba"),14,accent);
        TextView languageValue=text(chosenLanguage,14,accent);
        languageTitle.setMaxLines(1);languageValue.setMaxLines(1);
        languageTitle.setEllipsize(TextUtils.TruncateAt.END);languageValue.setEllipsize(TextUtils.TruncateAt.END);
        languageValue.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        languageTitle.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        languageLabels.addView(languageTitle,new LinearLayout.LayoutParams(0,-1,1));
        languageLabels.addView(languageValue,new LinearLayout.LayoutParams(0,-1,1));
        accentViews.add(languageTitle);accentViews.add(languageValue);
        languageRow.addView(languageLabels,new FrameLayout.LayoutParams(-1,-1));
        page.addView(languageRow,lp(-1,48));gap(page,8);

        LinearLayout reminders = panel();
        page.addView(reminders);
        LinearLayout masterRow = row();
        TextView title = settingTitle(tr("Notificări"));
        masterRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Switch master = new Switch(this);
        tintControl(master);
        master.setMinHeight(dp(48));
        master.setContentDescription(tr("Notificări"));
        boolean enabled = sp.getBoolean("notify", true);
        master.setChecked(enabled);
        masterRow.addView(master);
        reminders.addView(masterRow);

        LinearLayout options = col();
        reminders.addView(options);
        addProgramSelector(options);
        settingDivider(options);
        settingsNumber(options, tr("Prima notificare"), "before", 7, 0, 365);
        settingsNumber(options, tr("Repetare"), "every", 1, 1, 90);
        gateReminderSettings(options, enabled, false);
        master.setOnCheckedChangeListener((view, checked) -> {
            sp.edit().putBoolean("notify", checked).apply();
            gateReminderSettings(options, checked, true);
            PlannedNotifications.schedule(this);
        });

        gap(page, 14);
        LinearLayout appearance = panel();
        appearance.addView(settingTitle(tr("Aspect")), lp(-1, 40));
        page.addView(appearance);

        LinearLayout themeRow = labeledSetting(appearance, tr("Temă întunecată"));
        Switch night = new Switch(this);
        tintControl(night);
        night.setMinHeight(dp(48));
        night.setChecked(dark);
        night.setContentDescription(tr("Temă întunecată"));
        themeRow.addView(night);
        night.setOnCheckedChangeListener((v, checked) -> {
            settingsScroll = scrollView.getScrollY();
            sp.edit().putBoolean("dark", checked).apply();
            show();
        });

        settingDivider(appearance);
        LinearLayout iconRow = labeledSetting(appearance, tr("Pictograma aplicației"));
        TextView iconChoice = settingAction(sp.getBoolean("darkIcon", false) ? tr("Neagră") : tr("Albă"), tr("Alege pictograma"), () -> {});
        iconChoice.setOnClickListener(view -> {
            boolean old = sp.getBoolean("darkIcon", false);
            dialog().setTitle(tr("Pictograma aplicației")).setSingleChoiceItems(
                    new String[]{tr("Albă"), tr("Neagră")}, old ? 1 : 0, (dlg, which) -> {
                        if ((which == 1) != old) setIcon(which == 1);
                        iconChoice.setText(which == 1 ? tr("Neagră") : tr("Albă"));
                        dlg.dismiss();
                    }).setNegativeButton(tr("Anulează"), null).show();
        });
        iconRow.addView(iconChoice, lp(-2, 44));

        settingDivider(appearance);
        appearance.addView(text(tr("Culoarea accentelor"), 15, ink), lp(-1, 42));
        Palette palette = new Palette(this);
        appearance.addView(palette, lp(-1, 108));
        gap(page, 16);
    }

    TextView settingTitle(String label) {
        TextView title = text(label, 18, ink);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return title;
    }

    LinearLayout labeledSetting(LinearLayout parent, String label) {
        LinearLayout line = row();
        line.setMinimumHeight(dp(54));
        TextView name = text(label, 15, ink);
        name.setPadding(0, dp(6), dp(6), dp(6));
        line.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        parent.addView(line);
        return line;
    }

    void settingDivider(LinearLayout parent) {
        View divider = new View(this);
        divider.setBackgroundColor(border);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, dp(1));
        layout.setMargins(0, dp(7), 0, dp(7));
        parent.addView(divider, layout);
    }

    TextView settingAction(String label, String description, Runnable action) {
        TextView chip = button(label, description, action);
        chip.setTextSize(14);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        chip.setPadding(dp(12), 0, dp(12), 0);
        chip.setMinimumWidth(dp(44));
        chip.setMaxWidth(dp(Math.max(115,getResources().getConfiguration().screenWidthDp/2-25)));
        return chip;
    }

    void gateReminderSettings(LinearLayout group, boolean enabled, boolean animate) {
        setSettingsEnabled(group, enabled);
        group.setImportantForAccessibility(enabled ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        if (!enabled && group.hasFocus()) group.clearFocus();
        float opacity = enabled ? 1f : .3f;
        group.animate().cancel();
        if (animate) group.animate().alpha(opacity).setDuration(145).start();
        else group.setAlpha(opacity);
    }

    void setSettingsEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++) {
                setSettingsEnabled(parent.getChildAt(i), enabled);
            }
        }
    }

    void animateSettingChange(ViewGroup group) {
        if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return;
        android.transition.AutoTransition transition = new android.transition.AutoTransition();
        transition.setOrdering(android.transition.TransitionSet.ORDERING_TOGETHER);
        transition.setDuration(165);
        transition.setInterpolator(new android.view.animation.PathInterpolator(.2f, 0f, 0f, 1f));
        android.transition.TransitionManager.beginDelayedTransition(group, transition);
    }

    boolean usesIntervals() {
        return sp.getBoolean("intervalMode", sp.getInt("interval", 0) > 0);
    }

    void addProgramSelector(LinearLayout options) {
        String[] labels = {tr("Ore fixe"), tr("La interval")};
        LinearLayout selector = row();
        options.addView(selector);
        ProgramChevron arrow = new ProgramChevron();
        TextView mode = settingAction(labels[usesIntervals() ? 1 : 0], tr("Programul notificărilor"), () -> {});
        mode.setCompoundDrawablesRelative(null, null, arrow, null);
        mode.setCompoundDrawablePadding(dp(8));
        selector.addView(mode, lp(-2, 44));

        LinearLayout menu = col();
        menu.setBackground(shape(bg, 12));
        menu.setVisibility(View.GONE);
        LinearLayout.LayoutParams menuLayout = lp(-2, -2);
        menuLayout.setMargins(0, dp(5), 0, dp(4));
        options.addView(menu, menuLayout);
        LinearLayout dynamic = col();
        options.addView(dynamic);

        mode.setOnClickListener(v -> {
            boolean expanding = menu.getVisibility() != View.VISIBLE;
            animateSettingChange(options);
            menu.setVisibility(expanding ? View.VISIBLE : View.GONE);
            arrow.rotate(expanding);
            mode.setContentDescription(expanding ? tr("Închide") : tr("Deschide meniul de programare"));
        });
        for (int i = 0; i < labels.length; i++) {
            final boolean interval = i == 1;
            TextView choice = settingAction(labels[i], labels[i], () -> {
                animateSettingChange(options);
                menu.setVisibility(View.GONE);
                arrow.rotate(false);
                mode.setText(labels[interval ? 1 : 0]);
                mode.setContentDescription(tr("Deschide meniul de programare"));
                if (usesIntervals() == interval) return;
                sp.edit().putBoolean("intervalMode", interval)
                        .putInt("interval", Math.max(1, sp.getInt("interval", 2))).apply();
                populateProgram(dynamic);
                PlannedNotifications.schedule(this);
            });
            choice.setBackground(new android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(0x23777777), null, shape(card, 10)));
            choice.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            menu.addView(choice, lp(128, 44));
        }
        populateProgram(dynamic);
    }

    class ProgramChevron extends android.graphics.drawable.Drawable {
        final Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path path = new Path();
        float degrees;
        android.animation.ValueAnimator animator;

        ProgramChevron() { setBounds(0, 0, dp(14), dp(14)); }
        void rotate(boolean up) {
            if (animator != null) animator.cancel();
            animator = android.animation.ValueAnimator.ofFloat(degrees, up ? 180f : 0f);
            animator.setDuration(160);
            animator.addUpdateListener(a -> { degrees = (float) a.getAnimatedValue(); invalidateSelf(); });
            animator.start();
        }
        public void draw(Canvas canvas) {
            pen.setColor(accent);
            pen.setStyle(Paint.Style.STROKE);
            pen.setStrokeWidth(dp(1.6f));
            pen.setStrokeCap(Paint.Cap.ROUND);
            pen.setStrokeJoin(Paint.Join.ROUND);
            Rect bounds = getBounds();
            float x = bounds.exactCenterX(), y = bounds.exactCenterY(), radius = bounds.width() * .3f;
            canvas.save(); canvas.rotate(degrees, x, y);
            path.reset(); path.moveTo(x - radius, y - radius / 2);
            path.lineTo(x, y + radius / 2); path.lineTo(x + radius, y - radius / 2);
            canvas.drawPath(path, pen); canvas.restore();
        }
        public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        public void setAlpha(int value) {}
        public void setColorFilter(ColorFilter filter) {}
    }

    void populateProgram(LinearLayout group) {
        group.removeAllViews();
        if (usesIntervals()) {
            settingsNumber(group, tr("Interval"), "interval", 2, 1, 24);
            LinearLayout first = labeledSetting(group, tr("Prima oră"));
            String start = sp.getString("intervalStart", "09:00");
            TextView choice = settingAction(start, tr("Schimbă prima oră"), () -> {});
            choice.setOnClickListener(v -> timePick(sp.getString("intervalStart", "09:00"), time -> {
                sp.edit().putString("intervalStart", time).apply();
                choice.setText(time);
                PlannedNotifications.schedule(this);
            }));
            first.addView(choice, lp(-2, 44));
            return;
        }

        LinearLayout timesHeader = labeledSetting(group, tr("Ore de notificare"));
        TextView add = settingAction("+", tr("Adaugă o oră"), () -> timePick("09:00", time ->
                updateFixedTime(group, null, time)));
        add.setTextSize(21);
        timesHeader.addView(add, lp(46, 44));

        for (String time : fixedTimes()) {
            LinearLayout line = row();
            TextView edit = settingAction(time, tr("Modifică ora {0}",time), () -> timePick(time, next ->
                    updateFixedTime(group, time, next)));
            edit.setFontFeatureSettings("tnum");
            line.addView(edit, lp(86, 44));
            TextView remove = settingAction("×", tr("Elimină ora {0}",time), () -> confirmTimeRemoval(group, time));
            accentViews.remove(remove);
            remove.setTextSize(20);
            remove.setTextColor(dark ? 0xFFFFADAC : 0xFFA52A30);
            GradientDrawable danger = shape(dark ? 0xFF342123 : 0xFFFEF2F1, 12);
            danger.setStroke(dp(1), dark ? 0xFF825456 : 0xFFE1A5A7);
            remove.setBackground(danger);
            LinearLayout.LayoutParams removeLayout = lp(46, 44);
            removeLayout.setMargins(dp(8), 0, 0, 0);
            line.addView(remove, removeLayout);
            LinearLayout.LayoutParams lineLayout = lp(-1, -2);
            lineLayout.setMargins(0, 0, 0, dp(5));
            group.addView(line, lineLayout);
        }
    }

    SortedSet<String> fixedTimes() {
        SortedSet<String> times = new TreeSet<>();
        for (String saved : sp.getString("times", "09:00").split(",")) {
            String[] fields = saved.trim().split(":");
            if (fields.length != 2) continue;
            try {
                java.time.LocalTime value = java.time.LocalTime.of(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]));
                times.add(String.format(Locale.ROOT, "%02d:%02d", value.getHour(), value.getMinute()));
            } catch (IllegalArgumentException | java.time.DateTimeException ignored) {}
        }
        return times;
    }

    void confirmTimeRemoval(LinearLayout group, String time) {
        String message = fixedTimes().size() == 1
                ? tr("Ora {0} va fi eliminată. În modul Ore fixe, notificările se vor relua când adaugi o oră.",time)
                : tr("Ora {0} va fi eliminată din programul de notificări.",time);
        AlertDialog confirm = dialog().setTitle(tr("Elimini ora {0}?",time)).setMessage(message)
                .setNegativeButton(tr("Anulează"), null)
                .setPositiveButton(tr("Elimină"), (d, which) -> updateFixedTime(group, time, null)).create();
        confirm.show();
        confirm.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(dark ? 0xFFFFADAC : 0xFFA52A30);
    }

    void updateFixedTime(LinearLayout group, String previous, String next) {
        if (Objects.equals(previous, next)) return;
        SortedSet<String> times = fixedTimes();
        if (next != null && times.contains(next)) {
            Toast.makeText(this, tr("Ora {0} există deja.",next), Toast.LENGTH_SHORT).show();
            return;
        }
        if (previous != null) times.remove(previous);
        if (next != null) times.add(next);
        sp.edit().putString("times", TextUtils.join(",", times)).apply();
        animateSettingChange(group);
        populateProgram(group);
        PlannedNotifications.schedule(this);
    }

    void settingsNumber(LinearLayout parent, String label, String key, int fallback, int minimum, int maximum) {
        int current = Math.max(minimum, Math.min(maximum, sp.getInt(key, fallback)));
        LinearLayout line = labeledSetting(parent, label);
        TextView value = settingAction(scheduleQuantity(key, current), tr("Modifică")+" · "+label, () -> {});
        value.setOnClickListener(view -> {
            NumberPicker picker = new NumberPicker(dialogContext());
            picker.setMinValue(minimum);
            picker.setMaxValue(maximum);
            picker.setValue(Math.max(minimum, Math.min(maximum, sp.getInt(key, fallback))));
            picker.setWrapSelectorWheel(false);
            LinearLayout holder = col();
            holder.setGravity(Gravity.CENTER);
            holder.addView(picker, lp(96, 140));
            String title = key.equals("before") ? tr("Cu câte zile înainte?") : key.equals("every") ? tr("La câte zile?") : tr("La câte ore?");
            dialog().setTitle(title).setView(holder).setNegativeButton(tr("Anulează"), null)
                    .setPositiveButton(tr("Aplică"), (d, which) -> {
                        picker.clearFocus();
                        int result = picker.getValue();
                        sp.edit().putInt(key, result).apply();
                        value.setText(scheduleQuantity(key, result));
                        PlannedNotifications.schedule(this);
                    }).show();
        });
        line.addView(value, lp(-2, 44));
    }

    String scheduleQuantity(String key, int count) {
        if (key.equals("before")) {
            if (count == 0) return tr("În ziua aniversării");
            return count == 1 ? tr("Cu o zi înainte") : tr("Cu {0} zile înainte",L10n.duration(this,count,false));
        }
        if (key.equals("every")) return count == 1 ? tr("Zilnic") : tr("La fiecare {0} zile",L10n.duration(this,count,false));
        return count == 1 ? tr("O dată pe oră") : tr("La fiecare {0} ore",L10n.duration(this,count,true));
    }

 void setIcon(boolean d){sp.edit().putBoolean("darkIcon",d).apply();PackageManager pm=getPackageManager();pm.setComponentEnabledSetting(new ComponentName(this,d?"ro.zilefrumoase.DarkIcon":"ro.zilefrumoase.WhiteIcon"),PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);pm.setComponentEnabledSetting(new ComponentName(this,d?"ro.zilefrumoase.WhiteIcon":"ro.zilefrumoase.DarkIcon"),PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);}
 void tintControl(CompoundButton b){accentChecks.add(b);applyControlTint(b);}
 void applyControlTint(CompoundButton b){android.content.res.ColorStateList colors=new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{accent,muted});if(b instanceof Switch){Switch sw=(Switch)b;sw.setThumbTintList(colors);sw.setTrackTintList(colors);}else b.setButtonTintList(colors);}
 void refreshAccent(){
     for(TextView view:accentViews){
         view.setTextColor(accent);
         android.graphics.drawable.Drawable background=view.getBackground();
         if(background instanceof android.graphics.drawable.RippleDrawable){
             android.graphics.drawable.RippleDrawable ripple=(android.graphics.drawable.RippleDrawable)background;
             ripple.setColor(android.content.res.ColorStateList.valueOf((accent&0xffffff)|0x26000000));
             // Recolor the existing surface, keeping its border, ripple and pressed state.
             for(int layer=0;layer<ripple.getNumberOfLayers();layer++){
                 android.graphics.drawable.Drawable surface=ripple.getDrawable(layer);
                 if(ripple.getId(layer)!=android.R.id.mask && surface instanceof GradientDrawable)
                     ((GradientDrawable)surface).setColor(mix(card,accent,dark?.11f:.065f));
             }
         }
         for(android.graphics.drawable.Drawable icon:view.getCompoundDrawablesRelative())
             if(icon!=null)icon.invalidateSelf();
         view.invalidate();
     }
     for(CompoundButton control:accentChecks)applyControlTint(control);
 }
 class Palette extends View{Shader spectrum,shading;int shaderW,shaderH;Paint paint=new Paint(3);float x=.55f,y=.25f;Palette(Context c){super(c);float[] v=new float[3];Color.colorToHSV(sp.getInt("accent",Color.rgb(54,112,187)),v);x=v[0]/360f;y=Math.max(0,Math.min(1,(1-v[2])/.75f));setContentDescription(tr("Paletă culoare accent"));setFocusable(true);}protected void onDraw(Canvas c){super.onDraw(c);int w=getWidth(),h=getHeight();if(w==0||h==0)return;
 if(spectrum==null||shaderW!=w||shaderH!=h){
   int[] hues=new int[7];for(int i=0;i<7;i++)hues[i]=Color.HSVToColor(new float[]{(i*60)%360,.85f,1});
   spectrum=new LinearGradient(0,0,w,0,hues,null,Shader.TileMode.CLAMP);
   shading=new LinearGradient(0,0,0,h,Color.TRANSPARENT,0xbf000000,Shader.TileMode.CLAMP);
   shaderW=w;shaderH=h;
 }
 paint.setColor(Color.WHITE);paint.setShader(spectrum);c.drawRoundRect(0,0,w,h,dp(8),dp(8),paint);
 paint.setShader(shading);c.drawRoundRect(0,0,w,h,dp(8),dp(8),paint);paint.setShader(null);paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));c.drawCircle(x*w,y*h,dp(8),paint);paint.setStyle(Paint.Style.FILL);}public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN||e.getActionMasked()==MotionEvent.ACTION_MOVE||e.getActionMasked()==MotionEvent.ACTION_UP){getParent().requestDisallowInterceptTouchEvent(true);x=Math.max(0,Math.min(1,e.getX()/getWidth()));y=Math.max(0,Math.min(1,e.getY()/getHeight()));int raw=Color.HSVToColor(new float[]{x*360,.85f,1-.75f*y});if(e.getActionMasked()==MotionEvent.ACTION_UP)sp.edit().putInt("accent",raw).apply();accent=accessibleAccent(raw);refreshAccent();invalidate();if(e.getActionMasked()==MotionEvent.ACTION_UP)getParent().requestDisallowInterceptTouchEvent(false);return true;}return true;}}
}


