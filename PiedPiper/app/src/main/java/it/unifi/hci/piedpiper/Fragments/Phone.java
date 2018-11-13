package it.unifi.hci.piedpiper.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.telephony.PhoneNumberUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TableLayout;
import android.widget.TableRow;

import org.linphone.core.LinphoneCoreException;

import java.util.ArrayList;

import it.unifi.hci.piedpiper.CallActivity;
import it.unifi.hci.piedpiper.Controllers.VoipManager;
import it.unifi.hci.piedpiper.Fragments.Elements.PercentageLayout;
import it.unifi.hci.piedpiper.Fragments.Elements.PhoneInputText;
import it.unifi.hci.piedpiper.Fragments.Elements.PhoneSearchList;
import it.unifi.hci.piedpiper.MainActivity;
import it.unifi.hci.piedpiper.Models.ContactModel;
import it.unifi.hci.piedpiper.R;

public class Phone extends Fragment {
    private static PhoneSearchList phoneSearchList;

    public Phone() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_phone_main_list, container, false);

        Activity activity =  getActivity();

        phoneSearchList = view.findViewById(R.id.contacts_phone_search_list);
        phoneSearchList.setHasFixedSize(true);

        phoneSearchList.setLayoutManager(new LinearLayoutManager(activity));
        phoneSearchList.setItemAnimator(new DefaultItemAnimator());

        phoneSearchList.setActivity(getActivity());

        final TableLayout numbers = view.findViewById(R.id.phone_keyboard_numbers);
        TableLayout actions = view.findViewById(R.id.phone_keyboard_actions);
        ImageButton keyboard_clear_number = view.findViewById(R.id.keyboard_clear_number);

        if(MainActivity.getNumberToCall()!=null) {
            phoneSearchList.setPhoneNumber(MainActivity.getNumberToCall());
            PhoneInputText et = numbers.getRootView().findViewById(R.id.phone_input);
            et.setText(MainActivity.getNumberToCall());
            et.setContact(ContactModel.searchSingleContactByNumber(getContext(), MainActivity.getNumberToCall()));
            numbers.getRootView().findViewById(R.id.pdel).setVisibility(View.VISIBLE);
            numbers.getRootView().findViewById(R.id.keyboard_clear_number).setVisibility(View.VISIBLE);
            numbers.getRootView().findViewById(R.id.poptions).setVisibility(View.GONE);
        }

        keyboard_clear_number.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ((EditText) v.getRootView().findViewById(R.id.phone_input)).setText("");
                ((PhoneInputText) v.getRootView().findViewById(R.id.phone_input)).deleteContact();
                ((PhoneSearchList) v.getRootView().findViewById(R.id.contacts_phone_search_list)).removeSearchPhoneNumber();
                v.getRootView().findViewById(R.id.phone_input_container).setElevation(0);
                v.getRootView().findViewById(R.id.phone_keyboard_numbers_container).setElevation(0);
                v.getRootView().findViewById(R.id.phone_keyboard_actions_container).setElevation(0);
                v.getRootView().findViewById(R.id.poptions).setVisibility(View.VISIBLE);
                v.setVisibility(View.GONE);
                v.getRootView().findViewById(R.id.pdel).setVisibility(View.GONE);
            }
        });

        View child_row = actions.getChildAt(0);
        if (child_row instanceof TableRow) {
            TableRow row = (TableRow) child_row;
            ImageButton button_view = (ImageButton) row.getChildAt(1);
            button_view.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    LinearLayout root = v.getRootView().findViewById(R.id.phone_container);
                    ImageButton phide = root.findViewById(R.id.phide);

                    final PercentageLayout phone_list = root.findViewById(R.id.contacts_phone_main_list_container);
                    final PercentageLayout phone_keyboard = root.findViewById(R.id.phone_keyboard_numbers_container);

                    final float pl_perc = ((LinearLayout.LayoutParams) phone_list.getLayoutParams()).weight;
                    final float pk_perc = ((LinearLayout.LayoutParams) phone_keyboard.getLayoutParams()).weight;
                    final float delta_perc = phone_keyboard.getDefaultPercentage();

                    Animation a = new Animation() {
                        @Override
                        protected void applyTransformation(float interpolatedTime, Transformation t) {
                            ((LinearLayout.LayoutParams) phone_list.getLayoutParams()).weight = pl_perc - delta_perc * interpolatedTime;
                            ((LinearLayout.LayoutParams) phone_keyboard.getLayoutParams()).weight = pk_perc + delta_perc * interpolatedTime;
                            if(interpolatedTime > 0.3){
                                phone_keyboard.getChildAt(0).setVisibility(View.VISIBLE);
                            }
                            phone_list.requestLayout();
                            phone_keyboard.requestLayout();
                        }

                        @Override
                        public boolean willChangeBounds() {
                            return true;
                        }
                    };

                    a.setDuration((long) delta_perc*60);
                    root.startAnimation(a);

                    v.setVisibility(View.GONE);
                    phide.setVisibility(View.VISIBLE);
                }
            });

            // hide
            ImageButton button_hide = (ImageButton) row.getChildAt(2);
            button_hide.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    LinearLayout root = v.getRootView().findViewById(R.id.phone_container);
                    ImageButton pview = root.findViewById(R.id.pview);

                    final PercentageLayout phone_list = root.findViewById(R.id.contacts_phone_main_list_container);
                    final PercentageLayout phone_keyboard = root.findViewById(R.id.phone_keyboard_numbers_container);

                    final float pl_perc = ((LinearLayout.LayoutParams) phone_list.getLayoutParams()).weight;
                    final float pk_perc = ((LinearLayout.LayoutParams) phone_keyboard.getLayoutParams()).weight;
                    final float delta_perc = pk_perc;

                    Animation a = new Animation() {
                        @Override
                        protected void applyTransformation(float interpolatedTime, Transformation t) {
                            ((LinearLayout.LayoutParams) phone_list.getLayoutParams()).weight = pl_perc + delta_perc * interpolatedTime;
                            ((LinearLayout.LayoutParams) phone_keyboard.getLayoutParams()).weight = pk_perc - delta_perc * interpolatedTime;
                            if(interpolatedTime > 0.7){
                                phone_keyboard.getChildAt(0).setVisibility(View.INVISIBLE);
                            }
                            phone_list.requestLayout();
                            phone_keyboard.requestLayout();
                        }

                        @Override
                        public boolean willChangeBounds() {
                            return true;
                        }
                    };

                    a.setDuration((long) delta_perc*60);
                    root.startAnimation(a);

                    v.setVisibility(View.GONE);
                    pview.setVisibility(View.VISIBLE);
                }
            });

            // call
            ImageButton button_call = (ImageButton) row.getChildAt(3);
            button_call.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    PhoneInputText et = v.getRootView().findViewById(R.id.phone_input);
                    if(et.hasContact()){
                        try {
                            VoipManager.getInstance().invite(et.getContact());
                        } catch (LinphoneCoreException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Boolean found = Boolean.FALSE;
                        PhoneSearchList pSL = v.getRootView().findViewById(R.id.contacts_phone_search_list);
                        ArrayList<ContactModel.SingleContact> singleContacts = pSL.getFiltered();
                        if(singleContacts.size() > 0){
                            for (ContactModel.SingleContact sc: singleContacts){
                                if(sc.isPPUser() && PhoneNumberUtils.compare(sc.getNumber(), et.getText().toString())){
                                    try {
                                        VoipManager.getInstance().invite(sc);
                                    } catch (LinphoneCoreException e) {
                                        e.printStackTrace();
                                    }
                                    found = Boolean.TRUE;
                                    break;
                                }
                            }
                        }
                        if(!found){
                            try {
                                VoipManager.getInstance().invite(et.getText().toString());
                            } catch (LinphoneCoreException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });

            // opt
            final ImageButton button_opt = (ImageButton) row.getChildAt(4);
            button_opt.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    PopupMenu dropDownMenu = new PopupMenu(getContext(), button_opt);
                    dropDownMenu.getMenuInflater().inflate(R.menu.call_menu, dropDownMenu.getMenu());
                    dropDownMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            return true;
                        }
                    });
                    dropDownMenu.show();
                }
            });

            // del
            ImageButton button_del = (ImageButton) row.getChildAt(5);
            button_del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    PhoneInputText et = v.getRootView().findViewById(R.id.phone_input);
                    PhoneSearchList pSL = v.getRootView().findViewById(R.id.contacts_phone_search_list);
                    pSL.removeLastSearchNumber();
                    if (et.getText().length() > 0){
                        et.setText(pSL.getSearchPhoneNumber());
                        et.deleteContact();
                    }
                    if(et.getText().length() == 0){
                        v.getRootView().findViewById(R.id.phone_input_container).setElevation(0);
                        v.getRootView().findViewById(R.id.phone_keyboard_numbers_container).setElevation(0);
                        v.getRootView().findViewById(R.id.phone_keyboard_actions_container).setElevation(0);
                        v.getRootView().findViewById(R.id.poptions).setVisibility(View.VISIBLE);
                        v.getRootView().findViewById(R.id.pdel).setVisibility(View.GONE);
                        v.getRootView().findViewById(R.id.keyboard_clear_number).setVisibility(View.GONE);
                    }
                }
            });
            button_del.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    ((EditText) v.getRootView().findViewById(R.id.phone_input)).setText("");
                    ((PhoneSearchList) v.getRootView().findViewById(R.id.contacts_phone_search_list)).removeSearchPhoneNumber();
                    v.getRootView().findViewById(R.id.poptions).setVisibility(View.VISIBLE);
                    v.setVisibility(View.GONE);
                    v.getRootView().findViewById(R.id.keyboard_clear_number).setVisibility(View.GONE);
                    return true;
                }
            });

        }
        return view;
    }
}


