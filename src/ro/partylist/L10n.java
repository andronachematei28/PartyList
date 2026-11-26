package ro.zilefrumoase;
import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;
import org.json.JSONObject;
import java.util.Locale;
import java.util.HashMap;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

final class L10n {
    static final String[] CODES={"ro","en","de","fr","it","bg","tr","zh-CN","zh-TW","ja","ru","es","pt","nl","pl","cs","sk","hu","uk","el","sv","da","nb","fi","et","lv","lt","sl","hr","sr","ar","he","fa","hi","bn","ur","id","ms","vi","th","ko","ta","te","mr","sw","af","ca","eu","gl","is"};
    private static final HashMap<String,JSONObject> dictionaries=new HashMap<>();
    static String code(Locale locale){
        if(locale.getLanguage().equals("zh"))
            return locale.getCountry().equals("TW")||locale.getCountry().equals("HK")||locale.getScript().equals("Hant")?"zh-TW":"zh-CN";
        String language=locale.getLanguage();
        if(language.equals("iw"))language="he";
        if(language.equals("in"))language="id";
        if(language.equals("no"))language="nb";
        for(String known:CODES)if(known.equals(language))return known;
        return "en";
    }
    static Context context(Context base){
        String chosen=base.getSharedPreferences("zile",0).getString("language","ro");
        Locale locale=Locale.forLanguageTag(chosen.isEmpty()?code(base.getResources().getConfiguration().getLocales().get(0)):chosen);
        Configuration configuration=new Configuration(base.getResources().getConfiguration());
        configuration.setLocales(new LocaleList(locale));
        configuration.setLayoutDirection(locale);
        return base.createConfigurationContext(configuration);
    }
    static synchronized String text(Context context,String key,Object... values){
        String code=code(context.getResources().getConfiguration().getLocales().get(0));
        JSONObject dictionary=dictionaries.get(code);
        if(dictionary==null){
            try(InputStream input=context.getAssets().open("i18n/"+code+".json")){
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                byte[] buffer=new byte[8192];int size;
                while((size=input.read(buffer))!=-1)bytes.write(buffer,0,size);
                dictionary=new JSONObject(bytes.toString("UTF-8"));
            }catch(Exception failure){throw new IllegalStateException("Missing language "+code,failure);}
            dictionaries.put(code,dictionary);
        }
        String result=dictionary.optString(key,key);
        for(int i=0;i<values.length;i++){
            Object value=values[i];
            String formatted=value instanceof Number?java.text.NumberFormat.getIntegerInstance(context.getResources().getConfiguration().getLocales().get(0)).format(value):String.valueOf(value);
            result=result.replace("{"+i+"}",formatted);
        }
        return result;
    }
    static String duration(Context context,int count,boolean hours){
        return android.icu.text.MeasureFormat.getInstance(context.getResources().getConfiguration().getLocales().get(0),android.icu.text.MeasureFormat.FormatWidth.WIDE)
            .formatMeasures(new android.icu.util.Measure(count,hours?android.icu.util.MeasureUnit.HOUR:android.icu.util.MeasureUnit.DAY));
    }
    static String years(Context context,int number){
        Locale locale=context.getResources().getConfiguration().getLocales().get(0);
        String value=java.text.NumberFormat.getIntegerInstance(locale).format(number);
        String language=locale.getLanguage();
        if(language.equals("ja"))return value+"歳";
        if(language.equals("zh"))return value+(code(locale).equals("zh-TW")?"歲":"岁");
        if(language.equals("ko"))return value+"세";
        if(language.equals("vi"))return value+" tuổi";

        return android.icu.text.MeasureFormat.getInstance(context.getResources().getConfiguration().getLocales().get(0),android.icu.text.MeasureFormat.FormatWidth.WIDE)
            .formatMeasures(new android.icu.util.Measure(number,android.icu.util.MeasureUnit.YEAR));
    }
}
