package it.unifi.hci.piedpiper.Fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import org.linphone.core.LinphoneCoreException;

import it.unifi.hci.piedpiper.Controllers.VoipManager;
import it.unifi.hci.piedpiper.R;


public class IncomingCallFragment extends Fragment {
    private ImageButton acceptCall;
    private ImageButton declineCall;
    private String contact_name = "";
    private String contact_number = "";

    public IncomingCallFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        contact_name = arguments.getString("call_contact_name");
        contact_number = arguments.getString("call_contact_number");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_incoming_call, container, false);

        acceptCall = view.findViewById(R.id.call_accept);
        declineCall = view.findViewById(R.id.call_decline);

        ((TextView) view.findViewById(R.id.incoming_info_caller_name_text)).setText(contact_name);
        ((TextView)view.findViewById(R.id.incoming_info_caller_number_text)).setText(contact_number);

        final Animation animShake = AnimationUtils.loadAnimation(view.getContext(), R.anim.shake);
        animShake.setRepeatMode(Animation.INFINITE);
        animShake.setRepeatCount(Animation.INFINITE);
        acceptCall.startAnimation(animShake);
        acceptCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    animShake.cancel();
                    animShake.reset();
                    VoipManager.getInstance().acceptCall();
                } catch (LinphoneCoreException e) {
                    e.printStackTrace();
                }
            }
        });
        declineCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animShake.cancel();
                animShake.reset();
                VoipManager.getInstance().terminateCall();
            }
        });
        return view;
    }
}
