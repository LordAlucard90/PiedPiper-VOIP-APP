package it.unifi.hci.piedpiper.Controllers.Services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.linphone.core.LinphoneCoreException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import it.unifi.hci.piedpiper.Controllers.VoipManager;
import it.unifi.hci.piedpiper.MainActivity;
import it.unifi.hci.piedpiper.Models.CallModel;
import it.unifi.hci.piedpiper.Models.ContactModel;
import it.unifi.hci.piedpiper.R;

import static android.media.AudioManager.STREAM_RING;


public class VoipService extends Service {
    private static VoipService instance;
    private Timer networkTimer;
    private String baseReqUrl;
    private String audioUrl = "";
    private String srcNumber = "";
    private Date date=null;
    private final SimpleDateFormat storeFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    private final SimpleDateFormat nameFormat = new SimpleDateFormat("yyyyMMdd");
    private File storageDir;
    private AudioManager audioManager;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private Uri notificationTone;



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        VoipManager.create(getBaseContext());
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            storageDir = new File(Environment.getExternalStorageDirectory() + "/PiedPiper/received");
        } else {
            Toast.makeText(getBaseContext(), "Oops!! There is no SD Card.", Toast.LENGTH_SHORT).show();
        }

        if (!storageDir.exists()) { storageDir.mkdirs(); }

        notificationTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        audioManager = ((AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE));
        vibrator = (Vibrator) getBaseContext().getSystemService(Context.VIBRATOR_SERVICE);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    public static boolean isCreated() {
        return instance != null;
    }

    public synchronized static VoipService getInstance() {
        return instance;
    }

    public synchronized void register(String number){
        if(!VoipManager.getInstance().isRegistered()){
            try {
                VoipManager.getInstance().setAccount(number, "", "unsecurepassword");
                baseReqUrl = "http://"+VoipManager.getInstance().getDomain();
            } catch (LinphoneCoreException e) {
                e.printStackTrace();
            }
        }

        if(networkTimer == null){
            networkTimer = new Timer("Network Scheduler");

            TimerTask netTask = new TimerTask() {
                @Override
                public void run() {
                    final String reqUrl;
                    final Boolean canStore;
                    if(ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                                            == PackageManager.PERMISSION_GRANTED){
                        reqUrl = baseReqUrl + "/get_messages.php";
                        canStore = Boolean.TRUE;
                    } else {
                        reqUrl = baseReqUrl;
                        canStore = Boolean.FALSE;
                    }
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            RequestQueue queue = Volley.newRequestQueue(getBaseContext());
                            StringRequest stringRequest = new StringRequest(
                                    Request.Method.POST, reqUrl,
                                    new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            VoipManager.getInstance().setNetworkReachability(Boolean.TRUE);
                                            if(canStore){
                                                try {
                                                    JSONArray jArr = new JSONArray(response);
                                                    for (int i = 0; i < jArr.length(); i++){
                                                        JSONObject jObj = jArr.getJSONObject(i);
                                                        srcNumber = jObj.getString("src");
                                                            date = storeFormat.parse(jObj.getString("datetime"));
                                                        if(jObj.has("msg")){
                                                            String msg = jObj.getString("msg");
                                                            CallModel.addMessage(getApplicationContext(), jObj.getString("src"), jObj.getString("datetime"), jObj.getString("msg"), false);
                                                            sendNotification(srcNumber, ContactModel.searchSingleContactByNumber(getApplicationContext(), srcNumber).getName(), msg, false);
                                                        } else {
                                                            String audio = jObj.getString("audio");
                                                            audioUrl = baseReqUrl+"/audios/"+audio;
                                                            new DownloadFile().execute();
                                                            audioUrl="";
                                                        }
                                                        date=null;
                                                        srcNumber="";
                                                    }
                                                } catch (JSONException e) {
                                                    date=null;
                                                    srcNumber=audioUrl="";
                                                    e.printStackTrace();
                                                } catch (ParseException e) {
                                                    date=null;
                                                    srcNumber=audioUrl="";
                                                    e.printStackTrace();
                                                }
                                            }
                                        }},
                                    new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError e) {
                                            //e.printStackTrace();
                                            VoipManager.getInstance().setNetworkReachability(Boolean.FALSE);
                                        }
                                    }) {
                                        @Override
                                        protected Map<String, String> getParams() {
                                            Map<String, String>  params = new HashMap<String, String>();
                                            params.put("number", VoipManager.getInstance().getNumber());
                                            return params;
                                        }
                            };
                            queue.add(stringRequest);
                        }
                    });
                }
            };
            networkTimer.schedule(netTask, 0, 1000);
        }
    }

    private void updateCallRegister(String src, Date date, String audio){
        CallModel.addMessage(getApplicationContext(), src, storeFormat.format(date), audio, true);
        sendNotification(src, ContactModel.searchSingleContactByNumber(getApplicationContext(), src).getName(), "Audio", true);
    }

    private void sendNotification(String number,String sender,String msg,boolean isAudio) {
        Notification n;
        Notification.Builder b;
        if(isAudio){
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            intent.putExtra("from_notify","audio");
            Intent intentCall = new Intent(getBaseContext(), MainActivity.class);
            intentCall.putExtra("from_notify_call",number);
            PendingIntent pIntent = PendingIntent.getActivity(getBaseContext(), (int) System.currentTimeMillis(), intent, 0);
            PendingIntent pIntentCall = PendingIntent.getActivity(getBaseContext(), (int) System.currentTimeMillis(), intentCall, 0);
            b  = new Notification.Builder(getBaseContext())
                    .setContentTitle(sender)
                    .setContentText("Messaggio Audio")
                    .setContentIntent(pIntent)
                    .setSmallIcon(R.drawable.ic_action_pied_piper_notification_green_dark)
                    .setAutoCancel(true)
                    .addAction(R.drawable.ic_microfono, "Play audio", pIntent)
                    .addAction(R.drawable.call_accept, "Call "+sender, pIntentCall);
        } else {
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            intent.putExtra("from_notify","message");
            Intent intentCall = new Intent(getBaseContext(), MainActivity.class);
            intentCall.putExtra("from_notify_call",number);
            PendingIntent pIntent = PendingIntent.getActivity(getBaseContext(), (int) System.currentTimeMillis(), intent, 0);
            PendingIntent pIntentCall = PendingIntent.getActivity(getBaseContext(), (int) System.currentTimeMillis(), intentCall, 0);
            b  = new Notification.Builder(getBaseContext())
                    .setContentTitle(sender)
                    .setContentText(msg)
                    .setContentIntent(pIntent)
                    .setSmallIcon(R.drawable.ic_action_pied_piper_notification_green_dark)
                    .setAutoCancel(true)
                    .addAction(R.drawable.ic_message_text_black_36dp, "View Message", pIntent)
                    .addAction(R.drawable.call_accept, "Call "+sender, pIntentCall);
        }

        n= b.build();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(0, n);
        try {
            ringNotification();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ringNotification() throws IOException {
        if ((audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE || audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) && vibrator != null) {
            long[] patern = {0,1000,1000};
            vibrator.vibrate(patern, -1);
        }
        if (mediaPlayer == null) {
            audioManager.requestAudioFocus(null, STREAM_RING, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT );
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(STREAM_RING);
            mediaPlayer.setDataSource(getBaseContext(), notificationTone);
            mediaPlayer.prepare();
            mediaPlayer.setLooping(false);
            mediaPlayer.start();
        }
    }

    private class DownloadFile extends AsyncTask<Void, Void, Void> {
        private String curUrl = audioUrl;
        private String curNum = srcNumber;
        private Date curDate = date;

        @Override
        protected synchronized Void doInBackground(Void... voids) {
            if(curUrl.length()>0){
                try {
                    URL url = new URL(curUrl);
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setRequestMethod("GET");
                    c.connect();

                    if (c.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        System.out.println("HTTP Error: Server returned HTTP " + c.getResponseCode() + " " + c.getResponseMessage());
                    }

                    int n = 0;
                    File audioFile = new File(storageDir, String.format("%s_%s_%03d.3gp", nameFormat.format(curDate), curNum, n));
                    while (audioFile.exists()) {
                        n++;
                        audioFile = new File(storageDir, String.format("%s_%s_%03d.3gp", nameFormat.format(curDate), curNum, n));
                    }
                    audioFile.createNewFile();
                    FileOutputStream fos = new FileOutputStream(audioFile);

                    InputStream is = c.getInputStream();

                    byte[] buffer = new byte[1024];
                    int bufSize;
                    while ((bufSize = is.read(buffer)) != -1) { fos.write(buffer, 0, bufSize); }

                    fos.close();
                    is.close();
                    updateCallRegister(curNum, curDate, audioFile.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

}
