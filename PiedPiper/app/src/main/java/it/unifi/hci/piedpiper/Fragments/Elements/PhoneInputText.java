package it.unifi.hci.piedpiper.Fragments.Elements;

import android.content.Context;
import android.text.TextWatcher;
import android.util.AttributeSet;

import it.unifi.hci.piedpiper.Models.ContactModel;

public class PhoneInputText extends android.support.v7.widget.AppCompatEditText {
    private ContactModel.SingleContact contact = null;

    public PhoneInputText(Context context) {
        super(context);
    }

    public PhoneInputText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setContact(ContactModel.SingleContact contact) {
        this.contact = contact;
    }

    public void deleteContact() {
        this.contact = null;
    }

    public boolean hasContact() {
        return this.contact != null;
    }

    public ContactModel.SingleContact getContact() {
        return contact;
    }
}
