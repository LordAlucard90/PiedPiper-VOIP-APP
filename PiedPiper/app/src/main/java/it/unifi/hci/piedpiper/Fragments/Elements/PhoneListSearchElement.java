package it.unifi.hci.piedpiper.Fragments.Elements;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.telephony.PhoneNumberUtils;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.unifi.hci.piedpiper.Models.ContactModel;
import it.unifi.hci.piedpiper.R;

public class PhoneListSearchElement extends RecyclerView.Adapter<PhoneListSearchElement.ContactViewHolder> {
    private ArrayList<ContactModel.SingleContact> contacts;
    private Context context;
    private View elementView;
    private HashMap<String, String> patterns;

    public static class ContactViewHolder extends RecyclerView.ViewHolder {

        ImageView imageViewContactImage;
        TextView textViewContactName;
        TextView textViewContactNumber;
        LinearLayout phoneCC;

        public ContactViewHolder(View itemView,Context context) {
            super(itemView);
            this.imageViewContactImage = itemView.findViewById(R.id.contact_image);
            this.textViewContactName = itemView.findViewById(R.id.contact_name);
            this.textViewContactNumber = itemView.findViewById(R.id.contact_number);
            this.phoneCC = itemView.getRootView().findViewById(R.id.phone_contact_content);
        }

    }

    public PhoneListSearchElement(ArrayList<ContactModel.SingleContact> data, Context context) {
        super();
        this.contacts = data;
        this.context = context;
    }

    @Override
    public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.phone_search_list_element, parent, false);
        elementView=view;
        return new ContactViewHolder(view,context);
    }

    @Override
    public void onBindViewHolder(final ContactViewHolder holder, final int listPosition) {
        holder.setIsRecyclable(false);
        ImageView imgViewImg = holder.imageViewContactImage;
        TextView textViewName = holder.textViewContactName;
        TextView textViewNumber = holder.textViewContactNumber;
        LinearLayout phoneCC = holder.phoneCC;
        String name = contacts.get(listPosition).getName();
        final String number = contacts.get(listPosition).getNumber();

        if(contacts.get(listPosition).getPhotoUri()!=null && Uri.parse(contacts.get(listPosition).getPhotoUri())!=null){

            Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contacts.get(listPosition).getID());

            if(contactUri==null){
                imgViewImg.setImageDrawable(ContextCompat.getDrawable(context,(R.drawable.ic_account_white_48dp)));
                holder.imageViewContactImage.setColorFilter(context.getResources().getColor(R.color.colorPrimaryDark));
            }else {
                Picasso.with(holder.imageViewContactImage.getContext()).load(contactUri).into(holder.imageViewContactImage);
            }
        }

        if(patterns == null){
            textViewName.setText(contacts.get(listPosition).getName());
            textViewNumber.setText(contacts.get(listPosition).getNumber());
        } else {
            Boolean nameMatch = Boolean.FALSE;
            if(patterns.containsKey("txt")){
                Pattern p = Pattern.compile(patterns.get("txt"));
                Matcher m = p.matcher(contacts.get(listPosition).getName().toLowerCase());
                if (m != null) {
                    if (m.find()) {
                        nameMatch = Boolean.TRUE;
                        String found = m.group();
                        int start = name.toLowerCase().indexOf(found);
                        Spannable spannable = new SpannableString(name);
                        spannable.setSpan(new ForegroundColorSpan(Color.RED), start, start+found.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                        textViewName.setText(spannable);
                        textViewNumber.setText(number);
                    }
                }
            }
            if(!nameMatch){
                String num = patterns.get("num");
                int start = number.indexOf(num);
                Spannable spannable = new SpannableString(number);
                spannable.setSpan(new ForegroundColorSpan(Color.RED), start, start+num.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                textViewName.setText(name);
                textViewNumber.setText(spannable);
            }
        }

        textViewName.setGravity(Gravity.CENTER_VERTICAL);
        textViewNumber.setGravity(Gravity.CENTER_VERTICAL);

        final ContactModel.SingleContact contact = contacts.get(listPosition);
        phoneCC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((EditText) v.getRootView().findViewById(R.id.phone_input)).setText(number);
                ((PhoneInputText) v.getRootView().findViewById(R.id.phone_input)).setContact(contact);
                ((PhoneSearchList) v.getRootView().findViewById(R.id.contacts_phone_search_list)).setPhoneNumber(number);
            }
        });

    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void updateList(ArrayList<ContactModel.SingleContact> contactsList, HashMap<String, String> newPatterns){
        contacts.clear();
        contacts.addAll(contactsList);
        patterns=newPatterns;
        notifyDataSetChanged();
    }
}