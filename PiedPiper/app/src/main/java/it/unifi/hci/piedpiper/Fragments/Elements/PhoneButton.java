package it.unifi.hci.piedpiper.Fragments.Elements;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v7.widget.AppCompatButton;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import it.unifi.hci.piedpiper.R;

import static android.view.View.OnClickListener;
import static android.view.View.OnLongClickListener;


public class PhoneButton extends AppCompatButton implements OnClickListener, OnLongClickListener {
    private String primaryText;
    private String secondaryText;
    private String searchLetters;
    private String visibleText;

    public PhoneButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.PhoneButton,
                0, 0);
        try {
            primaryText = a.getString(R.styleable.PhoneButton_primary_text);
            secondaryText = a.getString(R.styleable.PhoneButton_secondary_text);
            searchLetters = a.getString(R.styleable.PhoneButton_search_letters);
        } finally {
            a.recycle();
        }
        setVisibleText();
        setOnClickListener(this);
        setOnLongClickListener(this);
    }

    private void setVisibleText(){
        if(primaryText.length() > 0){
            if(searchLetters.length() > 0){
                 visibleText = primaryText + "\n" + searchLetters.toUpperCase();
            } else if(secondaryText.length() > 0){
                visibleText = primaryText + "\n" + secondaryText.toUpperCase();
            } else {
                visibleText = primaryText + "\n";
            }
        } else {
            visibleText = " \n";
        }

        Spannable spannable = new SpannableString(visibleText);
        spannable.setSpan(new RelativeSizeSpan(1.8f), 0, 1,Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        spannable.setSpan(new RelativeSizeSpan(0.8f), 2, visibleText.length(),Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(Color.LTGRAY), 2, visibleText.length(),Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        setText(spannable);

    }

    public String getText(){
        return visibleText;
    }

    @Override
    public void onClick(View v) {
        EditText et = v.getRootView().findViewById(R.id.phone_input);
        PhoneSearchList psl = v.getRootView().findViewById(R.id.contacts_phone_search_list);
        et.append(primaryText);
        psl.addSearchNumber(primaryText.charAt(0), searchLetters);
        if(et.getText().length()==1){
            v.getRootView().findViewById(R.id.phone_input_container).setElevation(2);
            v.getRootView().findViewById(R.id.phone_keyboard_numbers_container).setElevation(2);
            v.getRootView().findViewById(R.id.phone_keyboard_actions_container).setElevation(2);
            v.getRootView().findViewById(R.id.pdel).setVisibility(View.VISIBLE);
            v.getRootView().findViewById(R.id.keyboard_clear_number).setVisibility(View.VISIBLE);
            v.getRootView().findViewById(R.id.poptions).setVisibility(View.GONE);
        }
    }


    @Override
    public boolean onLongClick(View v) {
        if(secondaryText.length() > 0){
            EditText et = v.getRootView().findViewById(R.id.phone_input);
            et.append(secondaryText);
            PhoneSearchList psl = v.getRootView().findViewById(R.id.contacts_phone_search_list);
            psl.addSearchNumber(secondaryText.charAt(0), "");
            if(et.getText().length()==1){
                v.getRootView().findViewById(R.id.phone_input_container).setElevation(2);
                v.getRootView().findViewById(R.id.phone_keyboard_numbers_container).setElevation(2);
                v.getRootView().findViewById(R.id.phone_keyboard_actions_container).setElevation(2);
                v.getRootView().findViewById(R.id.keyboard_clear_number).setVisibility(View.VISIBLE);
                v.getRootView().findViewById(R.id.pdel).setVisibility(View.VISIBLE);
                v.getRootView().findViewById(R.id.poptions).setVisibility(View.GONE);
            }
        }
        return true;
    }
}
