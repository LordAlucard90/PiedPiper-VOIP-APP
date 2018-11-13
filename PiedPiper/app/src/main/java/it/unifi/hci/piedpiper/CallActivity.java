package it.unifi.hci.piedpiper;

import android.Manifest;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.HashMap;

import it.unifi.hci.piedpiper.Controllers.VoipManager;
import it.unifi.hci.piedpiper.Fragments.ClosedCallFragment;
import it.unifi.hci.piedpiper.Fragments.InProgressCallFragment;
import it.unifi.hci.piedpiper.Fragments.IncomingCallFragment;
import it.unifi.hci.piedpiper.Fragments.PermissionsChecker;
import it.unifi.hci.piedpiper.Permission.PermissionValues;

public class CallActivity extends AppCompatActivity {
    private HashMap<Integer, PermissionsChecker> requests = new HashMap<>();
    Bundle mSavedInstanceState = null;

    @Override
    public void onBackPressed(){}

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(checkAudioPermission()){
            lauchCallStateView(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        if(checkAudioPermission()){
            lauchCallStateView(getIntent());
        } else {
            mSavedInstanceState = savedInstanceState;
        }
        getWindow().setNavigationBarColor(getResources().getColor(R.color.callBackgroundColor));
        getWindow().setStatusBarColor(getResources().getColor(R.color.callBackgroundColor));
    }

    public Boolean checkWriteStoragePermission(PermissionsChecker fragment){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED){
            return Boolean.TRUE;
        }else{
            requests.put(PermissionValues.WRITE_TORAGE, fragment);
            Snackbar.make(findViewById(R.id.main_activity), getString(R.string.accept_storage),Snackbar.LENGTH_INDEFINITE).setAction(
                    "OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(CallActivity.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    PermissionValues.WRITE_TORAGE);
                        }
                    }
            ).show();
            return Boolean.FALSE;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PermissionValues.RECORD:
                if (grantResults.length==1 && grantResults[0]== PackageManager.PERMISSION_GRANTED) {
                    lauchCallStateView(getIntent());
                } else {
                    VoipManager.getInstance().terminateCall();
                    finish();
                }
                break;

            case PermissionValues.WRITE_TORAGE:
                if(grantResults.length==1 && grantResults[0]== PackageManager.PERMISSION_GRANTED) {
                    requests.get(PermissionValues.WRITE_TORAGE).onPermissionGranted();
                }
                break;

            default:
                break;
        }
    }

    private void lauchCallStateView(Intent intentUpdate){
        if (mSavedInstanceState == null) {
            String name = getIntent().getStringExtra("call_contact_name");
            String number = getIntent().getStringExtra("call_contact_number");
            String state = intentUpdate.getStringExtra("call_state");

            switch (state){
                case "incoming":
                    IncomingCallFragment incFrag = new IncomingCallFragment();
                    Bundle incArgs = new Bundle();
                    incArgs.putString("call_contact_name", name);
                    incArgs.putString("call_contact_number", number);
                    incFrag.setArguments(incArgs);
                    final FragmentTransaction inFT = getFragmentManager().beginTransaction();
                    inFT.replace(R.id.call_activity, incFrag);
                    inFT.commit();
                    break;

                case "outgoing":
                    InProgressCallFragment startFrag = new InProgressCallFragment();
                    Bundle startArgs = new Bundle();
                    startArgs.putString("call_contact_name", name);
                    startArgs.putString("call_contact_number", number);
                    startFrag.setArguments(startArgs);
                    final FragmentTransaction sFT = getFragmentManager().beginTransaction();
                    sFT.replace(R.id.call_activity, startFrag);
                    sFT.commit();
                    break;

                case "connected":
                    InProgressCallFragment connFrag = new InProgressCallFragment();
                    Bundle connArgs = new Bundle();
                    connArgs.putString("call_contact_name", name);
                    connArgs.putString("call_contact_number", number);
                    connArgs.putString("call_mode", "accepted");
                    connFrag.setArguments(connArgs);
                    final FragmentTransaction cFT = getFragmentManager().beginTransaction();
                    cFT.replace(R.id.call_activity, connFrag);
                    cFT.commit();
                    break;

                case "ended":
                    ClosedCallFragment closeFrag = new ClosedCallFragment();
                    Bundle closeArgs = new Bundle();
                    closeArgs.putString("call_contact_name", name);
                    closeArgs.putString("call_contact_number", number);
                    closeArgs.putBoolean("connection", intentUpdate.getBooleanExtra("connection", false));
                    closeArgs.putBoolean("user_call", intentUpdate.getBooleanExtra("user_call", false));
                    closeFrag.setArguments(closeArgs);
                    final FragmentTransaction clFT = getFragmentManager().beginTransaction();
                    clFT.replace(R.id.call_activity, closeFrag);
                    clFT.commit();
                    break;

                default:
                    finish();
            }
        }

    }

    public Boolean checkAudioPermission(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED){
            return Boolean.TRUE;
        }else{
            Snackbar.make(findViewById(R.id.call_activity),getString(R.string.accept_record),Snackbar.LENGTH_INDEFINITE).setAction(
                    "OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(CallActivity.this,
                                    new String[]{Manifest.permission.RECORD_AUDIO},
                                    PermissionValues.RECORD);
                        }
                    }
            ).show();
            return Boolean.FALSE;
        }
    }


}
