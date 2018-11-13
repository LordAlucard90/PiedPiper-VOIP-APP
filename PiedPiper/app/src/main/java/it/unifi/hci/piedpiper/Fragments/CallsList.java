package it.unifi.hci.piedpiper.Fragments;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import it.unifi.hci.piedpiper.Fragments.Elements.CallElement;
import it.unifi.hci.piedpiper.MainActivity;
import it.unifi.hci.piedpiper.Models.CallModel;
import it.unifi.hci.piedpiper.R;

public class CallsList extends Fragment {
    public static View.OnClickListener myOnClickListener;

    private static RecyclerView.Adapter adapter;
    private PreCachingLayoutManager layoutManager;
    private static RecyclerView recyclerCallView;
    private static ArrayList<CallModel> dialed_calls = new ArrayList<CallModel>();
    private static ArrayList<CallModel> received_calls = new ArrayList<CallModel>();
    private static ArrayList<CallModel> lost_calls = new ArrayList<CallModel>();
    private static ArrayList<CallModel> all_calls = new ArrayList<CallModel>();
    private static ArrayList<CallModel> all_calls_temp = new ArrayList<CallModel>();
    private Button allCallsBtn;
    private Button doneCallsBtn;
    private Button lostCallsBtn;
    private boolean allCalls = true;

    private OnFragmentInteractionListener mListener;

    public CallsList() {}

    public static CallsList newInstance() {
        CallsList fragment = new CallsList();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myOnClickListener = new MyCallOnClickListener(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calls_list, container, false);

        dialed_calls = new ArrayList<CallModel>();
        received_calls = new ArrayList<CallModel>();
        lost_calls = new ArrayList<CallModel>();
        all_calls = new ArrayList<CallModel>();
        all_calls_temp = new ArrayList<CallModel>();

        recyclerCallView = (RecyclerView) view.findViewById(R.id.calls_list);
        recyclerCallView.setHasFixedSize(true);
        recyclerCallView.setItemViewCacheSize(200);

        allCallsBtn = (Button) view.findViewById(R.id.calls_all_filter) ;
        doneCallsBtn = (Button) view.findViewById(R.id.calls_dialed_filter) ;
        lostCallsBtn = (Button) view.findViewById(R.id.calls_lost_filter) ;
        allCallsBtn.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        allCallsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                allCallsBtn.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                doneCallsBtn.setBackgroundColor(getResources().getColor(R.color.colorUnselected));
                lostCallsBtn.setBackgroundColor(getResources().getColor(R.color.colorUnselected));

                allCalls=true;
                recyclerCallView.removeAllViews();
                ((CallElement) recyclerCallView.getAdapter()).update(all_calls_temp);
            }
        });
        doneCallsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doneCallsBtn.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                allCallsBtn.setBackgroundColor(getResources().getColor(R.color.colorUnselected));
                lostCallsBtn.setBackgroundColor(getResources().getColor(R.color.colorUnselected));

                allCalls=false;

                ArrayList<CallModel> dialedCalls = new ArrayList<CallModel>();

                for(CallModel c: all_calls_temp){
                    if (c.getType().equals("dialed")||c.getType().equals("received")) {
                        dialedCalls.add(c);
                    }
                }
                dialedCalls.add(CallModel.getEmptyOne("dialed"));
                recyclerCallView.removeAllViews();
                ((CallElement) recyclerCallView.getAdapter()).update(dialedCalls);
            }
        });
        lostCallsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lostCallsBtn.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                allCallsBtn.setBackgroundColor(getResources().getColor(R.color.colorUnselected));
                doneCallsBtn.setBackgroundColor(getResources().getColor(R.color.colorUnselected));

                allCalls=false;

                ArrayList<CallModel> lostCalls = new ArrayList<CallModel>();

                for(CallModel c: all_calls_temp){
                    if (c.getType().equals("lost")) {
                        lostCalls.add(c);
                    }
                }
                recyclerCallView.removeAllViews();
                ((CallElement) recyclerCallView.getAdapter()).update(lostCalls);
            }
        });

        layoutManager = new PreCachingLayoutManager(getActivity());
        layoutManager.setSmoothScrollbarEnabled(true);
        recyclerCallView.setLayoutManager(layoutManager);
        recyclerCallView.setItemAnimator(new DefaultItemAnimator());

        recyclerCallView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                if(firstVisibleItem<=0){return;}
            }
        });


        lost_calls = CallModel.getCalls(getContext(),"lost");
        received_calls = CallModel.getCalls(getContext(),"received");
        dialed_calls = CallModel.getCalls(getContext(),"dialed");

        all_calls.clear();
        all_calls.addAll(lost_calls);
        all_calls.addAll(received_calls);
        all_calls.addAll(dialed_calls);
        all_calls.add(CallModel.getEmptyOne("lost"));
        recyclerCallView.removeAllViews();
        Collections.sort(all_calls, new Comparator<CallModel>(){
            public int compare(CallModel o1, CallModel o2)
            {
                return o2.getDate().compareTo(o1.getDate());
            }
        });

        all_calls_temp.addAll(all_calls);

        adapter = new CallElement(all_calls, getContext(), (MainActivity) getActivity());
        adapter.setHasStableIds(true);
        recyclerCallView.setAdapter(adapter);



        return view;

    }


    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public static class MyCallOnClickListener implements View.OnClickListener {

        private final Context context;

        private int collapsedHeight;

        private MyCallOnClickListener(Context context) {
            this.context = context;
        }

        @Override
        public void onClick(View v) {
            toggleCard(v);
        }

        private void toggleCard(final View v_root) {
            final CardView v = (CardView) v_root.findViewById(R.id.call_card_view);
            if(v.getCardElevation()==(5 * context.getResources().getDisplayMetrics().density)){
                collapseCard(v_root);
            }else{
                expandCard(v_root);
            }
        }

        private void expandCard(final View root) {
            final CardView v = (CardView) root.findViewById(R.id.call_card_view);
            v.setCardElevation(5 * context.getResources().getDisplayMetrics().density);

            TextView idT = (TextView) v.findViewById(R.id.caller_name);
            idT.setSelected(true);

            LinearLayout parent = (LinearLayout) v.findViewById(R.id.call_elapsed);
            int childCount = parent.getChildCount();
            int count = 0;
            for (int i = 0; i < childCount; i++) {
                if (parent.getChildAt(i).getVisibility() == View.VISIBLE) {
                    count++;
                }else{
                    parent.getChildAt(i).setVisibility(View.VISIBLE);
                }
            }

            final int children = count;
            parent.setVisibility(View.VISIBLE);

            if (count <= 0) {

                final int initialHeight = v.getHeight();
                collapsedHeight = v.getHeight();

                root.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                int targetHeight = ( (root.getMeasuredHeight()/5))+root.getMeasuredHeight();
                final int distanceToExpand = targetHeight - initialHeight;

                Animation a = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        v.getLayoutParams().height = (int) (initialHeight + (distanceToExpand * interpolatedTime));
                        v.requestLayout();
                    }

                    @Override
                    public boolean willChangeBounds() {
                        return true;
                    }
                };

                a.setDuration((long) distanceToExpand);
                v.startAnimation(a);
            }else{
                final int initialHeight = v.getHeight();
                collapsedHeight = v.getHeight();

                root.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);


                LayoutInflater inflater = (LayoutInflater)root.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View numberContainer = inflater.inflate(R.layout.contact_numbers_layout, null);

                int h = numberContainer.getHeight();
                int targetHeight = (count * (h+10))+root.getMeasuredHeight();

                final int distanceToExpand = targetHeight - initialHeight;

                Animation a = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        if (interpolatedTime == 1) {
                            //Toast.makeText(root.getContext(), "ESPANSO", Toast.LENGTH_SHORT).show();
                        }

                        v.getLayoutParams().height = (int) (initialHeight + (distanceToExpand * interpolatedTime));
                        v.requestLayout();
                    }

                    @Override
                    public boolean willChangeBounds() {
                        return true;
                    }
                };

                a.setDuration((long) distanceToExpand);
                v.startAnimation(a);
            }
            parent.setVisibility(View.VISIBLE);
        }
        private void collapseCard(final View root) {
            final CardView v = (CardView) root.findViewById(R.id.call_card_view);
            v.setCardElevation(4 * context.getResources().getDisplayMetrics().density);


            TextView idT = (TextView) v.findViewById(R.id.caller_name);
            idT.setSelected(false);

            final int initialHeight = v.getMeasuredHeight();


            final int distanceToCollapse = (int) (initialHeight - collapsedHeight);

            Animation a = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {

                   /* if (interpolatedTime == 1){
                        Toast.makeText(root.getContext(),"COLLASSATO",Toast.LENGTH_SHORT).show();
                    }*/

                    v.getLayoutParams().height = (int) (initialHeight - (distanceToCollapse * interpolatedTime));
                    v.requestLayout();
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }
            };

            a.setDuration((long) distanceToCollapse);
            v.startAnimation(a);
        }

    }

    public class PreCachingLayoutManager extends LinearLayoutManager {
        private static final int DEFAULT_EXTRA_LAYOUT_SPACE = 600;
        private int extraLayoutSpace = -1;
        private Context context;

        public PreCachingLayoutManager(Context context) {
            super(context);
            this.context = context;
        }

        public PreCachingLayoutManager(Context context, int extraLayoutSpace) {
            super(context);
            this.context = context;
            this.extraLayoutSpace = extraLayoutSpace;
        }

        public PreCachingLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
            this.context = context;
        }

        public void setExtraLayoutSpace(int extraLayoutSpace) {
            this.extraLayoutSpace = extraLayoutSpace;
        }

        @Override
        protected int getExtraLayoutSpace(RecyclerView.State state) {
            if (extraLayoutSpace > 0) {
                return extraLayoutSpace;
            }
            return DEFAULT_EXTRA_LAYOUT_SPACE;
        }
    }
}
