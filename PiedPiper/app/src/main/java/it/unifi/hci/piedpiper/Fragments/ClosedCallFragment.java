package it.unifi.hci.piedpiper.Fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.apache.commons.io.FileUtils;
import org.linphone.core.LinphoneCoreException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import it.unifi.hci.piedpiper.CallActivity;
import it.unifi.hci.piedpiper.Controllers.VoipManager;
import it.unifi.hci.piedpiper.Helpers.VolleyMultipartRequest;
import it.unifi.hci.piedpiper.MainActivity;
import it.unifi.hci.piedpiper.Models.ContactModel;
import it.unifi.hci.piedpiper.R;

public class ClosedCallFragment extends Fragment implements PermissionsChecker{
    private ImageButton closeCallBtn;
    private ImageButton sendMessageBtn;
    private ImageButton rcrdMessageBtn;
    private ImageButton recallBtn;
    private TextView closeCallTextView;
    private CountDownTimer closeTimer;
    private CountDownTimer closeRecordTimer;
    private int closeSeconds = 10;
    private int closeRecordSeconds = 30;
    private String contact_name;
    private String contact_number;
    private Boolean connection;
    private Boolean user_call;
    private final SimpleDateFormat outFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    private final SimpleDateFormat nameFormat = new SimpleDateFormat("yyyyMMdd");

    public ClosedCallFragment() {
    }

    public static ClosedCallFragment newInstance() {
        ClosedCallFragment fragment = new ClosedCallFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            contact_name = arguments.getString("call_contact_name");
            contact_number = arguments.getString("call_contact_number");
            connection = arguments.getBoolean("connection", false);
            user_call = arguments.getBoolean("user_call", false);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_closed_call, container, false);

        // reset visibility
        setMessageView(view);

        ((TextView) view.findViewById(R.id.incoming_info_caller_name_text)).setText(contact_name);
        ((TextView) view.findViewById(R.id.incoming_info_caller_number_text)).setText(contact_number);

        closeCallBtn = view.findViewById(R.id.call_close);
        sendMessageBtn = view.findViewById(R.id.call_sendmessage);
        rcrdMessageBtn = view.findViewById(R.id.call_sendrecord);
        recallBtn = view.findViewById(R.id.call_recall);
        closeCallTextView = view.findViewById(R.id.call_close_text);

        createAndStartCloseTime(10);

        recallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    VoipManager.getInstance().invite(ContactModel.searchSingleContactByNumber(view.getContext(), contact_number));
                } catch (LinphoneCoreException e) {
                    e.printStackTrace();
                }
                closeTimer.cancel();
            }
        });

        if (!connection && user_call) {
            view.findViewById(R.id.send_message_container).setVisibility(View.VISIBLE);
            sendMessageBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                closeTimer.cancel();
                showInputDialog(view);
                }
            });
            rcrdMessageBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(((CallActivity) getActivity()).checkWriteStoragePermission(ClosedCallFragment.this)){
                        try {
                            showRecordDialog(view);
                            closeTimer.cancel();
                            Toast.makeText(view.getContext(), getString(R.string.record_start), Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(view.getContext(), getString(R.string.accept_storage), Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            view.findViewById(R.id.send_message_container).setVisibility(View.INVISIBLE);
        }

        closeCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeTimer.cancel();
                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onPermissionGranted() {
        try {
            showRecordDialog(getView());
            closeTimer.cancel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createAndStartCloseTime(int countDown) {
        closeSeconds = countDown;
        createAndStartCloseTime();
    }

    private void createAndStartCloseTime() {
        closeTimer = new CountDownTimer((closeSeconds+1)*1000, 1000) {
            public void onTick(long millisUntilFinished) {
                closeCallTextView.setText(getCountDownText(getString(R.string.call_close), closeSeconds));
                closeSeconds--;
            }
            public void onFinish() {
                closeCallTextView.setText(getCountDownText(getString(R.string.call_close),0));
                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
            }
        }.start();

    }

    private Spannable getCountDownText(String text, int count){
        String spanTxt = String.format("%s (%d)", text, count);
        int spanTxtSize = spanTxt.length();
        Spannable spannable = new SpannableString(spanTxt);
        if(count >= 10){
            spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.remainingTimeBig)), spanTxtSize-3, spanTxtSize-1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        } else if(count > 5){
            spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.remainingTimeSmall)), spanTxtSize-2, spanTxtSize-1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        } else {
            spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.remainingTimeNone)), spanTxtSize-2, spanTxtSize-1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        return spannable;
    }

    private void setMessageView(View view){
        view.getRootView().findViewById(R.id.call_close).setVisibility(View.VISIBLE);
        view.getRootView().findViewById(R.id.call_close_text).setVisibility(View.VISIBLE);
        view.getRootView().findViewById(R.id.cancel_record).setVisibility(View.GONE);
        view.getRootView().findViewById(R.id.cancel_record_text).setVisibility(View.GONE);
        view.getRootView().findViewById(R.id.call_sendmessage).setVisibility(View.VISIBLE);
        view.getRootView().findViewById(R.id.call_sendrecord).setVisibility(View.VISIBLE);
        view.getRootView().findViewById(R.id.call_sendmessage_text).setVisibility(View.VISIBLE);
        view.getRootView().findViewById(R.id.send_record).setVisibility(View.GONE);
        view.getRootView().findViewById(R.id.send_record_text).setVisibility(View.GONE);
        view.getRootView().findViewById(R.id.call_recall).setVisibility(View.VISIBLE);
        view.getRootView().findViewById(R.id.call_recall_text).setVisibility(View.VISIBLE);
    }

    private void setRecordView(View view){
        view.getRootView().findViewById(R.id.call_close).setVisibility(View.GONE);
        view.getRootView().findViewById(R.id.call_close_text).setVisibility(View.GONE);
        view.getRootView().findViewById(R.id.cancel_record).setVisibility(View.VISIBLE);
        view.getRootView().findViewById(R.id.cancel_record_text).setVisibility(View.VISIBLE);
        view.getRootView().findViewById(R.id.call_sendmessage).setVisibility(View.GONE);
        view.getRootView().findViewById(R.id.call_sendrecord).setVisibility(View.GONE);
        view.getRootView().findViewById(R.id.call_sendmessage_text).setVisibility(View.GONE);
        view.getRootView().findViewById(R.id.send_record).setVisibility(View.VISIBLE);
        view.getRootView().findViewById(R.id.send_record_text).setVisibility(View.VISIBLE);
        view.getRootView().findViewById(R.id.call_recall).setVisibility(View.INVISIBLE);
        view.getRootView().findViewById(R.id.call_recall_text).setVisibility(View.INVISIBLE);
    }

    private void showInputDialog(final View view) {
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        View promptView = layoutInflater.inflate(R.layout.call_message_input, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(promptView);

        final EditText editText = promptView.findViewById(R.id.edittext);

        alertDialogBuilder.setCancelable(false)
                .setPositiveButton(getString(R.string.action_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(editText.getText().length() > 0){
                            RequestQueue queue = Volley.newRequestQueue(view.getContext());
                            StringRequest postRequest = new StringRequest(
                                    Request.Method.POST,
                                    "http://"+VoipManager.getInstance().getDomain()+"/send_message.php",
                                    new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            // response
                                        }
                                    },
                                    new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError e) {
                                            e.printStackTrace();
                                        }
                                    }) {
                                @Override
                                protected Map<String, String> getParams() {
                                    Map<String, String> params = new HashMap<>();
                                    params.put("src_num", VoipManager.getInstance().getNumber());
                                    params.put("dst_num", contact_number);
                                    params.put("datetime", outFormat.format(Calendar.getInstance().getTime()));
                                    params.put("msg", editText.getText().toString());
                                    return params;
                                }
                            };
                            queue.add(postRequest);
                            Toast.makeText(view.getContext(), getString(R.string.record_or_message_sending), Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getActivity(), MainActivity.class);
                            startActivity(intent);
                        } else {
                            createAndStartCloseTime(7);
                            dialog.cancel();
                            Toast.makeText(view.getContext(), getString(R.string.send_msg_error_empty), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(getString(R.string.action_cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        InputMethodManager imm = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        createAndStartCloseTime(7);
                        dialog.cancel();
                        }
                    });

        final AlertDialog alert = alertDialogBuilder.create();
        alert.setOnShowListener( new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                alert.getButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
                alert.getButton(android.support.v7.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimaryText));
            }
        });
        alert.show();
    }

    private void showRecordDialog(final View view) throws IOException {
        setRecordView(view);
        closeRecordSeconds=30;

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File storageDir = new File(Environment.getExternalStorageDirectory() + "/PiedPiper/send");
            if (!storageDir.exists()) { storageDir.mkdirs(); }

            int n = 0;
            File audioFile = new File(storageDir, String.format("%s_%s_%03d.3gp", nameFormat.format(Calendar.getInstance().getTime()), contact_number, n));
            while (audioFile.exists()) {
                n++;
                audioFile = new File(storageDir, String.format("%s_%s_%03d.3gp", nameFormat.format(Calendar.getInstance().getTime()), contact_name, n));
            }

            final String audioFilePath = audioFile.getPath();
            final MediaRecorder recorder = new MediaRecorder();

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(audioFilePath);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.prepare();
            recorder.start();

            closeRecordTimer = new CountDownTimer((closeRecordSeconds+1)*1000, 1000) {
                public void onTick(long millisUntilFinished) {
                    ((TextView) view.getRootView().findViewById(R.id.send_record_text)).setText(getCountDownText(getString(R.string.record_send), closeRecordSeconds));
                    closeRecordSeconds--;
                }

                public void onFinish() {
                    ((TextView) view.getRootView().findViewById(R.id.send_record_text)).setText(getCountDownText(getString(R.string.record_send), 0));
                    recorder.stop();
                    recorder.release();
                    Toast.makeText(view.getContext(), getString(R.string.record_or_message_sending), Toast.LENGTH_SHORT).show();
                    sendRecord(view.getContext(), audioFilePath);
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);

                }
            }.start();

            view.getRootView().findViewById(R.id.send_record).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeRecordTimer.cancel();
                    recorder.stop();
                    recorder.release();
                    Toast.makeText(view.getContext(), getString(R.string.record_or_message_sending), Toast.LENGTH_SHORT).show();
                    sendRecord(view.getContext(), audioFilePath);
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);

                }
            });
            view.getRootView().findViewById(R.id.cancel_record).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeRecordTimer.cancel();
                    recorder.stop();
                    recorder.release();
                    (new File(audioFilePath)).delete();
                    setMessageView(view);
                    createAndStartCloseTime(7);
                }
            });
        } else {
            Toast.makeText(view.getContext(), "Oops!! There is no SD Card.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendRecord(Context context, final String path){
        RequestQueue queue = Volley.newRequestQueue(context);
        VolleyMultipartRequest postRequest = new VolleyMultipartRequest(
                Request.Method.POST,
                "http://"+VoipManager.getInstance().getDomain()+"/send_message.php",
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        e.printStackTrace();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("src_num", VoipManager.getInstance().getNumber());
                params.put("dst_num", contact_number);
                params.put("datetime", outFormat.format(Calendar.getInstance().getTime()));
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                try {
                    DataPart dataPart = new DataPart("audio.3gp", FileUtils.readFileToByteArray(new File(path)), "audio/3gpp");
                    params.put("audio", dataPart);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return params;
            }
        };
        queue.add(postRequest);

    }
}