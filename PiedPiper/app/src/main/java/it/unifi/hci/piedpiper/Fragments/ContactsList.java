package it.unifi.hci.piedpiper.Fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import it.unifi.hci.piedpiper.Fragments.Elements.ContactElement;
import it.unifi.hci.piedpiper.MainActivity;
import it.unifi.hci.piedpiper.Models.ContactModel;
import it.unifi.hci.piedpiper.R;

public class ContactsList extends Fragment implements PermissionsChecker {
    public static View.OnClickListener myContactsOnClickListener;

    private static RecyclerView.Adapter adapter;
    private PreCachingLayoutManager layoutManager;
    private static RecyclerView recyclerView;
    private static Button allContactsButton;
    private static Button onlyPPUsersButton;
    private EditText searchBox;
    private static LinearLayout lateralBar;
    private static String[] letters;
    private boolean allContacts = true;
    private static ArrayList<ContactModel> contacts = new ArrayList<ContactModel>();
    private static ArrayList<ContactModel> contactsTemp = new ArrayList<ContactModel>();

    public ContactsList() {}

    public static ContactsList newInstance() {
        ContactsList fragment = new ContactsList();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myContactsOnClickListener = new MyOnClickListener(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        contacts = new ArrayList<ContactModel>();
        contactsTemp = new ArrayList<ContactModel>();

        LinearLayout contacts_container_filter = view.findViewById(R.id.contacts_container_filter);
        contacts_container_filter.setElevation(50);
        allContactsButton = view.findViewById(R.id.contacts_all_filter);
        onlyPPUsersButton = view.findViewById(R.id.contacts_pponly_filter);
        if(allContacts){
            allContactsButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            onlyPPUsersButton.setBackgroundColor(getResources().getColor(R.color.colorUnselected));
        } else {
            allContactsButton.setBackgroundColor(getResources().getColor(R.color.colorUnselected));
            onlyPPUsersButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }
        allContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LinearLayout bottomBar = (LinearLayout) view.getParent();
                allContactsButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                onlyPPUsersButton.setBackgroundColor(getResources().getColor(R.color.colorUnselected));

                allContacts=true;
                searchBox.setText("");
                recyclerView.removeAllViews();
                ((ContactElement)recyclerView.getAdapter()).update(contactsTemp);
            }
        });
        onlyPPUsersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LinearLayout bottomBar = (LinearLayout) view.getParent();
                onlyPPUsersButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                allContactsButton.setBackgroundColor(getResources().getColor(R.color.colorUnselected));

                allContacts=false;
                searchBox.setText("");

                ArrayList<ContactModel> contactsPPUsers = new ArrayList<ContactModel>();

                for(ContactModel d: contactsTemp){
                    if (d.isPPUser()) {
                        contactsPPUsers.add(d);
                    }
                }

                recyclerView.removeAllViews();
                ((ContactElement)recyclerView.getAdapter()).update(contactsPPUsers);
            }
        });

        searchBox = view.findViewById(R.id.contact_search_box);
        searchBox.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                ArrayList<ContactModel> temp = new ArrayList<ContactModel>();
                ArrayList<ContactModel> listToUse = new ArrayList<ContactModel>();

                if(allContacts){
                    listToUse.addAll(contactsTemp);
                }else{
                    for(ContactModel d: contactsTemp){
                        if (d.isPPUser()) {
                            listToUse.add(d);
                        }
                    }
                }
                for(ContactModel d: listToUse){
                    if(d.getName().toLowerCase().contains(s.toString().toLowerCase())){
                        temp.add(d);
                    }
                }
                recyclerView.removeAllViews();
                if(((MainActivity) getActivity()).checkContactsPermission(ContactsList.this)){
                    ((ContactElement) recyclerView.getAdapter()).update(temp);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        recyclerView = view.findViewById(R.id.contacts_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(200);
        lateralBar = view.findViewById(R.id.lateral_bar);
        Activity activity =  getActivity();
        if(((MainActivity) activity).checkContactsPermission(this)) {
            letters = ContactModel.getAllLetters(getContext());
            for (int k = 0; k < lateralBar.getChildCount(); k++) {
                if (!Arrays.asList(letters).contains(lateralBar.getChildAt(k).getTag().toString())) {
                    ((TextView) lateralBar.getChildAt(k)).setTextColor(Color.GRAY);
                    lateralBar.getChildAt(k).setClickable(false);
                }
                lateralBar.getChildAt(k).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!Arrays.asList(letters).contains(view.getTag().toString())) {
                            return;
                        }
                        int pos = 0;
                        for (Object model : contacts.toArray()) {
                            if ((((ContactModel) model).getName()).startsWith((String) view.getTag())) {
                                pos = contacts.indexOf(model);
                                break;
                            }
                        }
                        RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(view.getContext()) {
                            @Override
                            protected int getVerticalSnapPreference() {
                                return LinearSmoothScroller.SNAP_TO_START;
                            }
                        };
                        smoothScroller.setTargetPosition(pos);
                        layoutManager.startSmoothScroll(smoothScroller);
                    }
                });
            }
        }
        layoutManager = new PreCachingLayoutManager(getActivity());
        layoutManager.setSmoothScrollbarEnabled(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                if(firstVisibleItem<=0){return;}
                Character targetLetter = contacts.get(firstVisibleItem).getName().charAt(0);
                for (int k=0; k<lateralBar.getChildCount();k++) {
                    Character firstChar = lateralBar.getChildAt(k).getTag().toString().charAt(0);
                    if(firstChar==targetLetter){

                        LinearLayout lateralBar2 = getActivity().findViewById(R.id.lateral_bar);
                        ((TextView) lateralBar2.getChildAt(k)).setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
                        if(((TextView)lateralBar2.getChildAt(k)).getTypeface().getStyle()!=Typeface.BOLD) {

                            ((TextView) lateralBar2.getChildAt(k)).setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                            lateralBar2.getChildAt(k).setBackgroundColor(getResources().getColor(R.color.colorSecondaryDark));
                            ((TextView) lateralBar2.getChildAt(k)).setTextColor(Color.WHITE);

                            ViewGroup row = (ViewGroup) lateralBar2.getChildAt(k).getParent();
                            for (int itemPos = 0; itemPos < row.getChildCount(); itemPos++) {
                                View v = row.getChildAt(itemPos);
                                if (v instanceof TextView && v.getTag() != lateralBar2.getChildAt(k).getTag()) {
                                    TextView letter = (TextView) v;
                                    if(Arrays.asList(letters).contains(letter.getText().toString())) {
                                        letter.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
                                        letter.setBackgroundColor(Color.TRANSPARENT);
                                        letter.setTextColor(getResources().getColor(R.color.colorSecondary));
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        });
        if(((MainActivity) activity).checkContactsPermission(this)){
            // FOR TEST ONLY
            //contacts.clear();
            //contacts.add(ContactModel.getTestContact());
            //contacts.addAll(ContactModel.getAll(getContext()));
            // NORMAL FLOW
            contacts = ContactModel.getAll(getContext());
            contacts.add(ContactModel.getEmptyOne());
            contactsTemp.clear();
            contactsTemp.addAll(contacts);
            letters = ContactModel.getAllLetters(getContext());

            adapter = new ContactElement(contacts,getContext());
            adapter.setHasStableIds(true);
            recyclerView.setAdapter(adapter);
        }
        return view;
    }

    @Override
    public void onPermissionGranted() {
        contacts = ContactModel.getAll(getContext());
        contactsTemp.clear();
        contactsTemp.addAll(contacts);
        letters = ContactModel.getAllLetters(getContext());
        adapter = new ContactElement(contacts,getContext());
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

    }

    public static class MyOnClickListener implements View.OnClickListener {

        private final Context context;

        private int collapsedHeight;

        private MyOnClickListener(Context context) {
            this.context = context;
        }

        @Override
        public void onClick(View v) {
            toggleCard(v);
        }

        private void toggleCard(final View v_root) {
            final CardView v = v_root.findViewById(R.id.card_view);
            if(v.getCardElevation()==(5 * context.getResources().getDisplayMetrics().density)){
                collapseCard(v_root);
            }else{
                expandCard(v_root);
            }
        }

        private void expandCard(final View root) {
            final CardView v = root.findViewById(R.id.card_view);
            v.setCardElevation(5 * context.getResources().getDisplayMetrics().density);

            TextView idT = v.findViewById(R.id.contact_name);
            idT.setSelected(true);

            LinearLayout parent = v.findViewById(R.id.contact_elapsed);
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
            final CardView v = root.findViewById(R.id.card_view);
            v.setCardElevation(4 * context.getResources().getDisplayMetrics().density);


            TextView idT = v.findViewById(R.id.contact_name);
            idT.setSelected(false);

            final int initialHeight = v.getMeasuredHeight();

            final int distanceToCollapse = initialHeight - collapsedHeight;

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
