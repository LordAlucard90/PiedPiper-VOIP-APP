package it.unifi.hci.piedpiper.Controllers;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.widget.Toast;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCallStats;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneContent;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListener;
import org.linphone.core.LinphoneEvent;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.LinphoneFriendList;
import org.linphone.core.LinphoneInfoMessage;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.PublishState;
import org.linphone.core.Reason;
import org.linphone.core.SubscriptionState;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import it.unifi.hci.piedpiper.CallActivity;
import it.unifi.hci.piedpiper.MainActivity;
import it.unifi.hci.piedpiper.Models.CallModel;
import it.unifi.hci.piedpiper.Models.ContactModel;
import it.unifi.hci.piedpiper.R;

import static android.media.AudioManager.MODE_RINGTONE;
import static android.media.AudioManager.STREAM_RING;
import static android.media.AudioManager.STREAM_VOICE_CALL;

public class VoipManager implements LinphoneCoreListener {
    private static VoipManager instance;
    private Context context;
    private LinphoneCore linphoneCore;
    private Timer timer;
    private String number;
    private String password;
    private String domain;
    private Boolean isRegistered = Boolean.FALSE;
    private Boolean isNetworkReachable = Boolean.FALSE;

    private AudioManager audioManager;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private Uri alarmTone;
    private final String ringBackAudioFile;
    private final SimpleDateFormat storeFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");


    protected VoipManager(final Context c) {
        context = c;
        ringBackAudioFile = c.getFilesDir().getAbsolutePath() + "/ringback.wav";
        alarmTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        audioManager = ((AudioManager) c.getSystemService(Context.AUDIO_SERVICE));
        vibrator = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        storeAudioFiles();
    }

    private void storeAudioFiles(){
        File ringBackFile = new File(ringBackAudioFile);
        if(!ringBackFile.exists()){
            try {
                FileOutputStream lOutputStream = context.openFileOutput(ringBackFile.getName(), 0);
                InputStream lInputStream = context.getResources().openRawResource(R.raw.ringback);
                int readByte;
                byte[] buff = new byte[8048];
                while ((readByte = lInputStream.read(buff)) != -1) {
                    lOutputStream.write(buff, 0, readByte);
                }
                lOutputStream.flush();
                lOutputStream.close();
                lInputStream.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public static synchronized VoipManager getInstance() {
        if (instance != null) return instance;
        throw new RuntimeException("VoipManager has not been created");
    }

    public static synchronized LinphoneCore getLc() {
        if (instance == null) { return null; }
        return getInstance().linphoneCore;
    }

    public synchronized static VoipManager create(Context c){
        if (instance == null){
            instance = new VoipManager(c);
            instance.start(c);
        }
        return instance;
    }

    private synchronized void start(Context c) {
        try {
            linphoneCore = LinphoneCoreFactory.instance().createLinphoneCore(this, c);
        } catch (Exception e) {
            e.printStackTrace();
        }
        TimerTask lTask = new TimerTask() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (linphoneCore != null) { linphoneCore.iterate(); }
                    }
                });
            }
        };
        timer = new Timer("Scheduler");
        timer.schedule(lTask, 0, 20);
    }

    public void setAccount(String uNumber, String uDomain, String uPsw) throws LinphoneCoreException {
        number = uNumber;
        password = uPsw;
        domain = uDomain;
        LinphoneAuthInfo authInfo = LinphoneCoreFactory.instance().createAuthInfo(number, password, null, domain);
        LinphoneProxyConfig prxCfg = getLc().createProxyConfig();
        prxCfg.setIdentity("sip:" + number + "@" + this.domain);
        prxCfg.setProxy("sip:" + this.domain);
        try {
            prxCfg.setExpires(3000);
        } catch (NumberFormatException nfe) {
            throw new LinphoneCoreException(nfe);
        }
        getLc().addProxyConfig(prxCfg);
        getLc().addAuthInfo(authInfo);
    }

    public String getNumber() {
        return number;
    }

    public String getDomain() {
        return domain;
    }

    public Boolean isRegistered() {
        return isRegistered;
    }

    @Override
    public void registrationState(LinphoneCore linphoneCore, LinphoneProxyConfig linphoneProxyConfig, LinphoneCore.RegistrationState state, String s) {
        isRegistered = state == LinphoneCore.RegistrationState.RegistrationOk;
    }

    public void setNetworkReachability(Boolean reachability){
        isNetworkReachable = reachability;
        if(!isNetworkReachable && curCall != null){
            terminateCall();
        }
    }

    @Override
    public void globalState(LinphoneCore lc, LinphoneCore.GlobalState state, String s) {
        if (state == LinphoneCore.GlobalState.GlobalOn){
            linphoneCore = lc;
            linphoneCore.setRingback(ringBackAudioFile);
        }
    }

    private ContactModel.SingleContact contact = null;
    private LinphoneCall curCall = null;
    private Boolean userCall = false;
    private Boolean userTermination = false;
    private Boolean connection = false;
    private Boolean mAudioFocused = false;
    private Boolean isRinging = false;

    public void invite(String number) throws LinphoneCoreException {
        Intent phoneIntent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null));
        context.startActivity(phoneIntent);
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.fromParts("tel", number, null));
    }

    public void invite(ContactModel.SingleContact singleContact) throws LinphoneCoreException {
        if(singleContact.isPPUser()){
            if(isRegistered && isNetworkReachable){
                contact = singleContact;
                Intent intent = new Intent(context, CallActivity.class);
                intent.putExtra("call_contact_name", contact.getName());
                intent.putExtra("call_contact_number", contact.getNumber());
                intent.putExtra("call_state", "outgoing");
                context.startActivity(intent);
                LinphoneAddress address = LinphoneCoreFactory.instance().createLinphoneAddress("sip:"+singleContact.getNumber()+"@"+domain);
                curCall = getLc().invite(address);
                userCall = Boolean.TRUE;
            } else {
                Toast.makeText(context,R.string.connection_error,Toast.LENGTH_LONG).show();
            }
        } else {
            invite(singleContact.getNumber());
        }
    }

    public void acceptCall() throws LinphoneCoreException {
        if (curCall != null){
            getLc().acceptCall(curCall);
        }
    }

    public void terminateCall(){
        if (curCall != null){
            userTermination = true;
            getLc().terminateCall(curCall);
        }
    }
    public boolean existsInrogressCall(){
        return curCall != null;
    }

    private void storeCall(String state){
        CallModel.saveCall(context, contact.getName(), contact.getNumber(),
                storeFormat.format(Calendar.getInstance().getTime()),
                (connection?String.valueOf(curCall.getDuration()):"0")+" sec",
                "", "", state);
    }

    @Override
    public void callState(LinphoneCore lc, LinphoneCall call, LinphoneCall.State state, String message) {
        if (state == LinphoneCall.State.IncomingReceived) {
            if (curCall == null) {
                curCall = call;
                Intent intent = new Intent(context, CallActivity.class);
                contact = ContactModel.searchSingleContactByNumber(context, call.getRemoteAddress().getUserName());
                intent.putExtra("call_contact_name", contact.getName());
                intent.putExtra("call_contact_number", contact.getNumber());
                intent.putExtra("call_state", "incoming");
                context.startActivity(intent);
                requestAudioFocus(STREAM_RING);
                curCall = call;
                startRinging();
            } else  {
                linphoneCore.declineCall(call, Reason.Busy);
            }
        } else if (call == curCall && isRinging) {
            stopRinging();
        }

        if (state == LinphoneCall.State.OutgoingInit
                || state == LinphoneCall.State.OutgoingRinging
                || state == LinphoneCall.State.OutgoingProgress) {
            if (audioManager.getMode() != AudioManager.MODE_IN_COMMUNICATION) {
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            }
            requestAudioFocus(STREAM_VOICE_CALL);
        }

        if(state == LinphoneCall.State.Connected){
            Intent intent = new Intent(context, CallActivity.class);
            intent.putExtra("call_state", "connected");
            context.startActivity(intent);
            connection = Boolean.TRUE;
        }

        if (state == LinphoneCall.State.CallEnd || state == LinphoneCall.State.Error) {
            if (linphoneCore.getCallsNb() == 0) {
                if (mAudioFocused) {
                    audioManager.abandonAudioFocus(null);
                    mAudioFocused = false;
                }
            }
            if(userCall){
                storeCall("dialed");
            } else if(connection){
                storeCall("received");
            } else {
                storeCall("lost");
            }
            if (userTermination){
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
            } else {
                if(userCall || connection){
                    Intent intent = new Intent(context, CallActivity.class);
                    intent.putExtra("call_state", "ended");
                    intent.putExtra("user_call", userCall);
                    intent.putExtra("connection", connection);
                    context.startActivity(intent);
                } else {
                    Intent intent = new Intent(context, MainActivity.class);
                    context.startActivity(intent);
                }
            }
            userTermination = Boolean.FALSE;
            userCall = Boolean.FALSE;
            connection = Boolean.FALSE;
            contact = null;
            curCall = null;
        }
    }

    private synchronized void startRinging()  {
        audioManager.setMode(MODE_RINGTONE);
        try {
            if ((audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE || audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) && vibrator != null) {
                long[] patern = {0,1000,1000};
                vibrator.vibrate(patern, 1);
            }
            if (mediaPlayer == null) {
                requestAudioFocus(STREAM_RING);
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(STREAM_RING);
                mediaPlayer.setDataSource(context, alarmTone);
                mediaPlayer.prepare();
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        isRinging = true;
    }

    private synchronized void stopRinging() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
        isRinging = false;
    }

    private void requestAudioFocus(int stream){
        if (!mAudioFocused){
            int res = audioManager.requestAudioFocus(null, stream, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT );
            if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) mAudioFocused=true;
        }
    }

    @Override
    public void authInfoRequested(LinphoneCore linphoneCore, String s, String s1, String s2) {}
    @Override
    public void authenticationRequested(LinphoneCore linphoneCore, LinphoneAuthInfo linphoneAuthInfo, LinphoneCore.AuthMethod authMethod){}
    @Override
    public void callStatsUpdated(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCallStats linphoneCallStats) {}
    @Override
    public void newSubscriptionRequest(LinphoneCore linphoneCore, LinphoneFriend linphoneFriend, String s) {}
    @Override
    public void notifyPresenceReceived(LinphoneCore linphoneCore, LinphoneFriend linphoneFriend) {}
    @Override
    public void dtmfReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, int i) {}
    @Override
    public void notifyReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneAddress linphoneAddress, byte[] bytes) {}
    @Override
    public void transferState(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCall.State state) {}
    @Override
    public void infoReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneInfoMessage linphoneInfoMessage) {}
    @Override
    public void subscriptionStateChanged(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, SubscriptionState subscriptionState) {}
    @Override
    public void publishStateChanged(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, PublishState publishState) {}
    @Override
    public void show(LinphoneCore linphoneCore) {}
    @Override
    public void displayStatus(LinphoneCore linphoneCore, String s) {}
    @Override
    public void displayMessage(LinphoneCore linphoneCore, String s) {}
    @Override
    public void displayWarning(LinphoneCore linphoneCore, String s) {}
    @Override
    public void fileTransferProgressIndication(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, int i) {}
    @Override
    public void fileTransferRecv(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, byte[] bytes, int i) {}
    @Override
    public int fileTransferSend(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, ByteBuffer byteBuffer, int i) {return 0;}
    @Override
    public void configuringStatus(LinphoneCore linphoneCore, LinphoneCore.RemoteProvisioningState remoteProvisioningState, String s) {}
    @Override
    public void messageReceived(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom, LinphoneChatMessage linphoneChatMessage) {}
    @Override
    public void messageReceivedUnableToDecrypted(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom, LinphoneChatMessage linphoneChatMessage) {}
    @Override
    public void callEncryptionChanged(LinphoneCore linphoneCore, LinphoneCall linphoneCall, boolean b, String s) {}
    @Override
    public void notifyReceived(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, String s, LinphoneContent linphoneContent) {}
    @Override
    public void isComposingReceived(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom) {}
    @Override
    public void ecCalibrationStatus(LinphoneCore linphoneCore, LinphoneCore.EcCalibratorStatus ecCalibratorStatus, int i, Object o) {}
    @Override
    public void uploadProgressIndication(LinphoneCore linphoneCore, int i, int i1) {}
    @Override
    public void uploadStateChanged(LinphoneCore linphoneCore, LinphoneCore.LogCollectionUploadState logCollectionUploadState, String s) {}
    @Override
    public void friendListCreated(LinphoneCore linphoneCore, LinphoneFriendList linphoneFriendList) {}
    @Override
    public void friendListRemoved(LinphoneCore linphoneCore, LinphoneFriendList linphoneFriendList) {}
    @Override
    public void networkReachableChanged(LinphoneCore linphoneCore, boolean b) {}

}
