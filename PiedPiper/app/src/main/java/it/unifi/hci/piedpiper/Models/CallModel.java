package it.unifi.hci.piedpiper.Models;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

import it.unifi.hci.piedpiper.R;
import it.unifi.hci.piedpiper.Models.ContactModel;

import static android.content.Context.MODE_PRIVATE;

public class CallModel {
    private String number;
    private String name;
    private String date;
    private String time;
    private String message;
    private String audio;
    private String type;

    public String getName() {
        return name;
    }
    public String getNumber() {
        return number;
    }
    public String getDate() {
        return date;
    }
    public String getTime() {
        return time;
    }
    public String getMessage() {
        return message;
    }
    public String getAudio() {
        return audio;
    }
    public String getType() {
        return type;
    }

    private CallModel(String Name, String Number, String date, String time, String message, String audio, String type){
        this.name = Name;
        this.number = Number;
        this.date = date;
        this.time = time;
        this.message = message;
        this.audio = audio;
        this.type = type;
    }

    public static ArrayList<CallModel> getCalls(Context context,String call_type) {
        ArrayList<CallModel> calls = new ArrayList<CallModel>();
        String jsonString = context.getString(R.string.calls_register);
        JSONObject json;
        try {
            json = new JSONObject(jsonString);

            JSONArray array_calls = json.getJSONArray(call_type);

            for (int i=0; i < array_calls.length(); i++) {
                JSONObject obj = array_calls.getJSONObject(i);
                calls.add(new CallModel(obj.getString("caller_name"),obj.getString("caller_number"),
                        obj.getString("date"),obj.getString("time"),obj.getString("pp_message"),obj.getString("pp_audio"),call_type));
            }

            SharedPreferences prefs = context.getSharedPreferences("CALLS_REGISTER", MODE_PRIVATE);
            jsonString = prefs.getString(call_type+"_calls",null);
            if(jsonString!=null){
                array_calls = new JSONArray(jsonString);

                for (int i=0; i < array_calls.length(); i++) {
                    JSONObject obj = array_calls.getJSONObject(i);
                    calls.add(new CallModel(obj.getString("caller_name"),obj.getString("caller_number"),
                            obj.getString("date"),obj.getString("time"),obj.getString("pp_message"),obj.getString("pp_audio"),call_type));
                }
            }
            return calls;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void saveCall(Context context,String caller_name,String caller_number,String date,String time,String pp_message,String pp_audio,String call_type){

        JSONObject call = new JSONObject();
        try {
            call.put("caller_name",caller_name);
            call.put("caller_number",caller_number);
            call.put("date",date);
            call.put("time",time);
            call.put("pp_message",pp_message);
            call.put("pp_audio",pp_audio);
            SharedPreferences sharedPreferences = context.getSharedPreferences("CALLS_REGISTER", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            String callsJson;
            if(call_type.equals("lost")){
                callsJson = sharedPreferences.getString("lost_calls",null);
                if(callsJson != null){
                    JSONArray jsonArray = new JSONArray(callsJson);
                    if(!pp_message.equals("")||!pp_audio.equals("")){
                        JSONArray list = new JSONArray();
                        for (int i=0;i<jsonArray.length()-1;i++){
                                list.put(jsonArray.get(i));
                        }
                        list.put(call);
                        editor.remove("lost_calls");
                        editor.putString("lost_calls", list.toString());
                    } else {
                        jsonArray.put(call);
                        editor.remove("lost_calls");
                        editor.putString("lost_calls", jsonArray.toString());
                    }
                    editor.apply();
                    editor.commit();
                } else{
                    JSONArray jsonArray = new JSONArray();
                    jsonArray.put(call);
                    editor.putString("lost_calls", jsonArray.toString());
                    editor.apply();
                    editor.commit();
                }
            } else if(call_type.equals("dialed")){
                callsJson = sharedPreferences.getString("dialed_calls",null);
                if(callsJson != null){
                    JSONArray jsonArray = new JSONArray(callsJson);
                    jsonArray.put(call);
                    editor.remove("dialed_calls");
                    editor.putString("dialed_calls", jsonArray.toString());
                    editor.apply();
                    editor.commit();
                } else {
                    JSONArray jsonArray = new JSONArray();
                    jsonArray.put(call);
                    editor.putString("dialed_calls", jsonArray.toString());
                    editor.commit();
                }
            } else if(call_type.equals("received")){
                callsJson = sharedPreferences.getString("received_calls",null);
                if(callsJson != null){
                    JSONArray jsonArray = new JSONArray(callsJson);
                    jsonArray.put(call);
                    editor.remove("received_calls");
                    editor.putString("received_calls", jsonArray.toString());
                    editor.commit();
                } else {
                    JSONArray jsonArray = new JSONArray();
                    jsonArray.put(call);
                    editor.putString("received_calls", jsonArray.toString());
                    editor.commit();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String toString() {
        return this.name;
    }

    public static void addMessage(Context context, String src, String datetime, String msg,Boolean isAudioMessage){
        SharedPreferences prefs =  context.getSharedPreferences("CALLS_REGISTER", MODE_PRIVATE);
        String jsonString = prefs.getString("lost_calls",null);

        if(jsonString!=null){
            try {
                JSONArray array_calls = new JSONArray(jsonString);
                JSONArray new_array_calls = new JSONArray();
                boolean found=false;
                for (int i=0; i < array_calls.length(); i++) {
                    JSONObject obj = array_calls.getJSONObject(i);
                    if(obj.getString("caller_number").equals(src)&&obj.getString("date").equals(datetime)){
                        found=true;
                        if(isAudioMessage){
                            obj.remove("audio");
                            obj.put("audio",msg);
                        }else{
                            obj.remove("msg");
                            obj.put("msg",msg);
                        }
                    }
                    new_array_calls.put(obj);
                }
                if(found){
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("lost_calls");
                    editor.putString("lost_calls", new_array_calls.toString());
                    editor.commit();
                }else{
                    if(isAudioMessage) {
                        saveCall(context, ContactModel.searchSingleContactByNumber(context, src).getName().toString(), src, datetime, "0 sec", "", msg, "lost");
                    }else{
                        saveCall(context, ContactModel.searchSingleContactByNumber(context, src).getName().toString(), src, datetime, "0 sec", msg, "", "lost");
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        } else {
            if(isAudioMessage) {
                saveCall(context, ContactModel.searchSingleContactByNumber(context, src).getName(), src, datetime, "0 sec", "", msg, "lost");
            } else {
                saveCall(context, ContactModel.searchSingleContactByNumber(context, src).getName().toString(), src, datetime, "0 sec", msg, "", "lost");
            }
        }
    }

    public static CallModel getEmptyOne(String type){
        return new CallModel("","","","","","",type);
    }
}
