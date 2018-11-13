package it.unifi.hci.piedpiper.Fragments.Elements;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.telephony.PhoneNumberUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.linphone.core.LinphoneCoreException;

import java.util.ArrayList;
import java.util.List;

import it.unifi.hci.piedpiper.Fragments.ContactsList;
import it.unifi.hci.piedpiper.Models.ContactModel;
import it.unifi.hci.piedpiper.Controllers.VoipManager;
import it.unifi.hci.piedpiper.R;

public class ContactElement extends RecyclerView.Adapter<ContactElement.ContactViewHolder> {

    private ArrayList<ContactModel> contacts;
    private Context context;
    private View elementView;

    public static class ContactViewHolder extends RecyclerView.ViewHolder {

        ImageView imageViewContactImage;
        TextView textViewContactName;
        ImageView imageViewContactIsPPUser;
        ImageButton imageButtonUserInfo;
        LinearLayout layoutElapsed;

        public ContactViewHolder(View itemView,Context context) {
            super(itemView);
            setIsRecyclable(false);
            this.imageViewContactImage = itemView.findViewById(R.id.contact_image);
            this.textViewContactName = itemView.findViewById(R.id.contact_name);
            this.imageViewContactIsPPUser = itemView.findViewById(R.id.contact_is_pp_user);
            this.layoutElapsed = itemView.findViewById(R.id.contact_elapsed);
            this.imageButtonUserInfo = itemView.findViewById(R.id.contact_info);
        }

    }

    public ContactElement(ArrayList<ContactModel> data, Context context) {
        this.contacts = data;
        this.context = context;
    }

    public void update(ArrayList<ContactModel> datas){
        contacts.clear();
        contacts.addAll(datas);
        notifyDataSetChanged();
    }

    @Override
    public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contact_element, parent, false);

        view.setOnClickListener(ContactsList.myContactsOnClickListener);

        elementView=null;
        elementView=view;


        return new ContactViewHolder(view,context);
    }

    @Override
    public void onBindViewHolder(final ContactViewHolder holder, final int listPosition) {
        holder.setIsRecyclable(false);

        ImageView imgViewImg = holder.imageViewContactImage;
        TextView textViewName = holder.textViewContactName;
        ImageButton imageButtonUserInfo = holder.imageButtonUserInfo;
        ImageView imgtViewPPUser = holder.imageViewContactIsPPUser;
        LinearLayout layoutElapsed = holder.layoutElapsed;
        holder.imageViewContactImage.setImageDrawable(null);

        layoutElapsed.setVisibility(View.GONE);

        if(contacts.get(listPosition).getPhotoUri()!=null && Uri.parse(contacts.get(listPosition).getPhotoUri())!=null){

            Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contacts.get(listPosition).getID());
            if(contactUri!=null){
                Picasso.with(holder.imageViewContactImage.getContext()).load(contactUri).into(holder.imageViewContactImage);
            }

        }else{
            holder.imageViewContactImage.setImageDrawable(ContextCompat.getDrawable(context,(R.drawable.ic_account_white_48dp)));
            holder.imageViewContactImage.setColorFilter(context.getResources().getColor(R.color.colorPrimaryDark));
        }
        textViewName.setText(contacts.get(listPosition).getName());
        textViewName.setGravity(Gravity.CENTER_VERTICAL);
        if(contacts.get(listPosition).isPPUser()){
            imgtViewPPUser.setVisibility(View.VISIBLE);
        }

        List<String> alreadyInsered = new ArrayList<String>();
        for(final ContactModel.SingleContact sc: contacts.get(listPosition).getSingleContact()){

            String number = sc.getNumber().replaceAll("\\s+","");
            if(!alreadyInsered.contains(number)) {
                alreadyInsered.add(number);

                LayoutInflater inflater = (LayoutInflater)elementView.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                View view = inflater.inflate(R.layout.contact_numbers_layout, null);
                layoutElapsed.addView(view);

                TextView t = view.findViewById(R.id.contact_number);
                t.setText(number);
                t.setTextSize(20);
                t.setTypeface(null, Typeface.BOLD);

                ImageButton callBtn = view.findViewById(R.id.contact_call);

                callBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            VoipManager.getInstance().invite(sc);
                        } catch (LinphoneCoreException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
        imageButtonUserInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(contacts.get(listPosition).getID()));
                intent.setData(contactUri);
                context.startActivity(intent);
            }
        });

        if(contacts.get(listPosition).getID()==0&&contacts.get(listPosition).getName().equals("")&&contacts.get(listPosition).getNumbers().isEmpty()){
            // utente empty
            imgtViewPPUser.setVisibility(View.GONE);
            imageButtonUserInfo.setVisibility(View.GONE);
            holder.imageViewContactImage.setImageDrawable(null);
            holder.imageViewContactImage.getRootView().findViewById(R.id.card_view).setClickable(false);
            holder.imageViewContactImage.getRootView().findViewById(R.id.card_view).setBackgroundColor(Color.TRANSPARENT);

        }
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

}
