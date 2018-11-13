package it.unifi.hci.piedpiper;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import it.unifi.hci.piedpiper.Fragments.CallsList;
import it.unifi.hci.piedpiper.Fragments.ContactsList;
import it.unifi.hci.piedpiper.Fragments.PermissionsChecker;
import it.unifi.hci.piedpiper.Fragments.Phone;
import it.unifi.hci.piedpiper.Controllers.Services.VoipService;
import it.unifi.hci.piedpiper.Models.ContactModel;
import it.unifi.hci.piedpiper.Permission.PermissionValues;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private HashMap<Integer, PermissionsChecker> requests ;
    private static String numberToCall = null;

    public static String getNumberToCall() {
        return numberToCall;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requests = new HashMap<Integer, PermissionsChecker>();
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_list_calls);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_phone_white_48dp);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_account_multiple);
        tabLayout.getTabAt(1).select();
        getSupportActionBar().hide();

        SharedPreferences prefs = getSharedPreferences("MY_NUMBER", MODE_PRIVATE);
        String restoredText = prefs.getString("my_number", null);
        if (restoredText == null) {
            showInputDialogForNumber();
        } else {
            startVoipService(restoredText);
        }
        final String number = prefs.getString("my_number", "");

        Intent intent = getIntent();
        if(intent.getStringExtra("from_notify")!=null){
            if(intent.getStringExtra("from_notify").equals("message")
                    || intent.getStringExtra("from_notify").equals("audio")){
                tabLayout.getTabAt(0).select();
                intent.removeExtra("from_notify");
            }
        } else {
            tabLayout.getTabAt(1).select();
            if(intent.getStringExtra("from_notify_call")!=null) {
                numberToCall = intent.getStringExtra("from_notify_call");
                intent.removeExtra("from_notify_call");
            } else {
                numberToCall = null;
            }
        }

        checkAllPermissions();
    }

    private void startVoipService(final String number){
        if(!VoipService.isCreated()){
            startService(new Intent(this, VoipService.class));
            Timer timer = new Timer("Register");
            TimerTask lTask = new TimerTask() {
                @Override
                public void run() {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if(VoipService.isCreated()){
                                VoipService.getInstance().register(number);
                                cancel();
                            }
                        }
                    });
                }
            };
            timer.schedule(lTask, 1000, 500);
        }
    }

    private void checkAllPermissions(){
        if( ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                                                new String[]{ Manifest.permission.READ_CONTACTS,
                                                              Manifest.permission.RECORD_AUDIO,
                                                              Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                              Manifest.permission.READ_EXTERNAL_STORAGE},
                                                PermissionValues.All);
        }

    }

    public Boolean checkContactsPermission(PermissionsChecker fragment){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED){
            return Boolean.TRUE;
        }else{
            requests.put(PermissionValues.CONTACTS, fragment);
            Snackbar.make(findViewById(R.id.main_activity),getString(R.string.accept_contacts),Snackbar.LENGTH_INDEFINITE).setAction(
                    "OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.READ_CONTACTS},
                                    PermissionValues.CONTACTS);
                        }
                    }
            ).show();
            return Boolean.FALSE;
        }
    }

    public Boolean checkReadStoragePermission(PermissionsChecker fragment){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED){
            return Boolean.TRUE;
        }else{
            requests.put(PermissionValues.READ_STORAGE, fragment);
            Snackbar.make(findViewById(R.id.main_activity), getString(R.string.accept_storage), Snackbar.LENGTH_INDEFINITE).setAction(
                    "OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    PermissionValues.READ_STORAGE);
                        }
                    }
            ).show();
            return Boolean.FALSE;
        }
    }

    @Override
    public void onBackPressed() {this.moveTaskToBack(true);}


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        switch (requestCode){
            case PermissionValues.CONTACTS:
                if(grantResults.length==1 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    requests.get(PermissionValues.CONTACTS).onPermissionGranted();
                    viewPager.getAdapter().notifyDataSetChanged();
                    tabLayout.setupWithViewPager(viewPager);
                    tabLayout.getTabAt(0).setIcon(R.drawable.ic_list_calls);
                    tabLayout.getTabAt(1).setIcon(R.drawable.ic_phone_white_48dp);
                    tabLayout.getTabAt(2).setIcon(R.drawable.ic_account_multiple);
                    tabLayout.getTabAt(1).select();
                } else {
                    Snackbar.make(findViewById(R.id.main_activity),getString(R.string.accept_contacts_2), Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.action_cancel), null)
                            .setAction(
                            getString(R.string.action_ok), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.READ_CONTACTS},
                                            PermissionValues.CONTACTS);
                                }
                            }).show();
                }
                break;
            case PermissionValues.READ_STORAGE:
                if(grantResults.length==1 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    requests.get(PermissionValues.READ_STORAGE).onPermissionGranted();
                }else{
                    Snackbar.make(findViewById(R.id.main_activity),getString(R.string.accept_storage_2), Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.action_cancel), null)
                            .setAction(
                                    getString(R.string.action_ok), new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            ActivityCompat.requestPermissions(MainActivity.this,
                                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                                    PermissionValues.READ_STORAGE);
                                        }
                                    }).show();
                }
                break;
            case PermissionValues.All:
                // not interest to result
                break;
            default:
                break;
        }
    }

    private void showInputDialogForNumber() {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptView = layoutInflater.inflate(R.layout.mynumber_input, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);

        final EditText editText = promptView.findViewById(R.id.edittext);
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton(getString(R.string.action_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        InputMethodManager imm = (InputMethodManager)editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                        SharedPreferences.Editor editor = getSharedPreferences("MY_NUMBER", MODE_PRIVATE).edit();
                        editor.putString("my_number", editText.getText().toString());
                        editor.apply();
                        editor.commit();
                        startVoipService(editText.getText().toString());
                    }
                })
                .setNegativeButton(getString(R.string.action_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                finish();
                            }
                        });

        final AlertDialog alert = alertDialogBuilder.create();
        alert.setOnShowListener( new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimaryText));
            }
        });
        alert.show();
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new CallsList(), "calls");
        adapter.addFragment(new Phone(), "phone");
        adapter.addFragment(new ContactsList(), "contacts");
        viewPager.setAdapter(adapter);

    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return null;
        }
    }
}
