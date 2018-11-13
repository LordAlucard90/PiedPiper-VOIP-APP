package it.unifi.hci.piedpiper.Fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import it.unifi.hci.piedpiper.Controllers.VoipManager;
import it.unifi.hci.piedpiper.R;

public class InProgressCallFragment extends Fragment {

    private ImageButton endCallBtn;
    private TextView timerTextView;
    private String mode = "";
    private String contact_name = "";
    private String contact_number = "";

    public InProgressCallFragment() {}

    public static InProgressCallFragment newInstance() {
        InProgressCallFragment fragment = new InProgressCallFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        mode = arguments.getString("call_mode");
        contact_name = arguments.getString("call_contact_name");
        contact_number = arguments.getString("call_contact_number");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =inflater.inflate(R.layout.fragment_in_progress_call, container, false);

        ((TextView) view.findViewById(R.id.incoming_info_caller_name_text)).setText(contact_name);
        ((TextView)view.findViewById(R.id.incoming_info_caller_number_text)).setText(contact_number);

        endCallBtn= view.findViewById(R.id.call_end);
        endCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VoipManager.getInstance().terminateCall();
            }
        });

        timerTextView = view.findViewById(R.id.call_timer);

        if(mode==("accepted")) {
            final Date start = Calendar.getInstance().getTime();
            Timer timer = new Timer("Call Time");
            TimerTask lTask = new TimerTask() {
                @Override
                public void run() {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                        if(VoipManager.getInstance().existsInrogressCall()) {
                            Date now = Calendar.getInstance().getTime();
                            long diff = now.getTime() - start.getTime();
                            int all_secs = (int) (diff / 1000);
                            int secs = all_secs % 60;
                            int min = all_secs / 60;
                            timerTextView.setText(String.format("%d : %02d", min, secs));
                        } else {
                            cancel();
                        }
                        }
                    });
                }
            };
            timer.schedule(lTask, 0, 500);
        }
        return view;
    }
}
