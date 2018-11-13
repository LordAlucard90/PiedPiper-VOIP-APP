package it.unifi.hci.piedpiper.Fragments.Elements;

import android.content.Context;
import android.graphics.PorterDuff;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextPaint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.linphone.core.LinphoneCoreException;

import java.util.ArrayList;

import it.unifi.hci.piedpiper.Controllers.VoipManager;
import it.unifi.hci.piedpiper.Fragments.PermissionsChecker;
import it.unifi.hci.piedpiper.MainActivity;
import it.unifi.hci.piedpiper.Models.CallModel;
import it.unifi.hci.piedpiper.Models.ContactModel;
import it.unifi.hci.piedpiper.R;

public class CallElement  extends RecyclerView.Adapter<CallElement.CallViewHolder> implements PermissionsChecker {

    private ArrayList<CallModel> calls;
    private Context context;
    private MainActivity activity;
    private View elementView;
    private boolean canRead = Boolean.FALSE;

    @Override
    public void onPermissionGranted() {
        ArrayList<CallModel> tmp = this.calls;
        this.update(tmp);
        canRead = Boolean.TRUE;
    }

    public static class CallViewHolder extends RecyclerView.ViewHolder {
        TextView textViewCallerName;
        TextView textViewCallerNumber;
        TextView textViewCallDate;
        TextView textViewCallTime;
        TextView textViewCallMessage;
        Button imageButtonCallMessageIcon;
        ImageView imageViewCallType;
        LinearLayout layoutElapsed;

        public CallViewHolder(View itemView,Context context) {
            super(itemView);
            setIsRecyclable(false);
            this.textViewCallerName = itemView.findViewById(R.id.caller_name);
            this.textViewCallerNumber = itemView.findViewById(R.id.caller_number);
            this.textViewCallDate = itemView.findViewById(R.id.call_date);
            this.textViewCallTime = itemView.findViewById(R.id.call_time);
            this.textViewCallMessage = itemView.findViewById(R.id.call_message);
            this.imageButtonCallMessageIcon = itemView.findViewById(R.id.call_message_icon);
            this.layoutElapsed = itemView.findViewById(R.id.call_elapsed);
            this.imageViewCallType = itemView.findViewById(R.id.call_type_image);
        }

    }

    public CallElement(ArrayList<CallModel> data, Context context, MainActivity mainActivity) {
        this.calls = data;
        this.context = context;
        this.activity = mainActivity;
    }

    public void update(ArrayList<CallModel> datas){
        calls.clear();
        calls.addAll(datas);
        notifyDataSetChanged();
    }
    @Override
    public CallViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.call_element, parent, false);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    VoipManager.getInstance().invite(ContactModel.searchSingleContactByNumber(view.getContext(),((TextView) view.findViewById(R.id.caller_number)).getText().toString()));
                } catch (LinphoneCoreException e) {
                    e.printStackTrace();
                }
            }
        });
        elementView=null;
        elementView=view;
        return new CallViewHolder(elementView,context);
    }

    @Override
    public void onBindViewHolder(final CallViewHolder holder, final int listPosition) {
        holder.setIsRecyclable(false);

        holder.layoutElapsed.setVisibility(View.GONE);
        holder.textViewCallerName.setText(calls.get(listPosition).getName());
        holder.textViewCallerNumber.setText(calls.get(listPosition).getNumber());
        holder.textViewCallDate.setText(calls.get(listPosition).getDate());
        holder.textViewCallTime.setText(calls.get(listPosition).getTime());
        holder.textViewCallMessage.setText(calls.get(listPosition).getMessage());

        TextPaint paint = holder.textViewCallMessage.getPaint();
        int wordwidth=(int)paint.measureText("a",0,1);
        int screenwidth = holder.textViewCallMessage.getContext().getResources().getDisplayMetrics().widthPixels;
        int num = screenwidth/wordwidth;

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.textViewCallMessage.getLayoutParams();
        params.height = holder.textViewCallMessage.getLineHeight()*(2+Math.round(calls.get(listPosition).getMessage().length()/num));
        holder.textViewCallMessage.setLayoutParams(params);

        if(calls.get(listPosition).getType().equals("dialed")){
            holder.imageViewCallType.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_phone_outgoing_black_36dp));
            holder.imageViewCallType.setColorFilter(context.getResources().getColor(R.color.colorPrimaryDark));
        }
        if(calls.get(listPosition).getType().equals("received")){
            holder.imageViewCallType.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_phone_incoming_black_36dp));
            holder.imageViewCallType.setColorFilter(context.getResources().getColor(R.color.colorPrimaryDark));
        }
        if(calls.get(listPosition).getType().equals("lost")){
            holder.imageViewCallType.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_phone_missed_black_36dp));
            holder.imageViewCallType.setColorFilter(context.getResources().getColor(R.color.colorMissed));
        }

        if(calls.get(listPosition).getName().equals("") && calls.get(listPosition).getNumber().equals("")){
            holder.imageViewCallType.setBackground(null);
            holder.imageViewCallType.setImageDrawable(null);
        }

        if(calls.get(listPosition).getAudio().equals("") && calls.get(listPosition).getMessage().equals("")){
            holder.imageButtonCallMessageIcon.setCompoundDrawables(null, null, null, null);
        }else{
            if(!calls.get(listPosition).getMessage().equals("")){//c'è un mess di testo
                holder.imageButtonCallMessageIcon.setCompoundDrawablesWithIntrinsicBounds(context.getResources().getDrawable(R.drawable.ic_message_text_black_24dp), null, null, null);

                holder.imageButtonCallMessageIcon.setClickable(false);
                holder.imageButtonCallMessageIcon.setOnClickListener(new View.OnClickListener() {
                    int collapsedHeight;
                    @Override
                    public void onClick(View v) {
                        toggleCard(v);
                    }
                    private void toggleCard(final View v_root) {

                        final CardView v = holder.itemView.findViewById(R.id.call_card_view);

                        if(v.getCardElevation()==(5 * context.getResources().getDisplayMetrics().density)){
                            collapseCard(holder.itemView);
                        }else{
                            expandCard(holder.itemView);
                        }
                    }
                    private void expandCard(final View root) {
                        final CardView v = root.findViewById(R.id.call_card_view);
                        v.setCardElevation(5 * context.getResources().getDisplayMetrics().density);

                        TextView idT = v.findViewById(R.id.caller_name);
                        idT.setSelected(true);

                        LinearLayout parent = v.findViewById(R.id.call_elapsed);
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
                        final CardView v = root.findViewById(R.id.call_card_view);
                        v.setCardElevation(4 * context.getResources().getDisplayMetrics().density);
                        TextView idT = v.findViewById(R.id.caller_name);
                        idT.setSelected(false);
                        final int initialHeight = v.getMeasuredHeight();
                        final int distanceToCollapse = initialHeight - collapsedHeight;
                        Animation a = new Animation() {
                            @Override
                            protected void applyTransformation(float interpolatedTime, Transformation t) {
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
                });
            }else{//c'è un mess audio
                holder.imageButtonCallMessageIcon.setCompoundDrawablesWithIntrinsicBounds(null, context.getResources().getDrawable(R.drawable.ic_microfono), null, null);
                if(activity.checkReadStoragePermission(CallElement.this)){
                    Uri uri = Uri.parse(Environment.getExternalStorageDirectory()+"/PiedPiper/received/"+calls.get(listPosition).getAudio());
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(context, uri);
                    String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    int sec = Integer.parseInt(durationStr)/1000;
                    holder.imageButtonCallMessageIcon.setText(String.format("%02d s", sec));
                } else {
                    holder.imageButtonCallMessageIcon.setText("?? s");
                }
                holder.imageButtonCallMessageIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(activity.checkReadStoragePermission(CallElement.this)){
                            Uri uri = Uri.parse(Environment.getExternalStorageDirectory()+"/PiedPiper/received/"+calls.get(listPosition).getAudio());
                            final MediaPlayer player = MediaPlayer.create(context,uri);
                            player.setLooping(false);
                            player.start();
                            final long totalDuration = player.getDuration()/1000;
                            holder.imageButtonCallMessageIcon.getCompoundDrawables()[1].setColorFilter(context.getResources().getColor(R.color.remainingTimeNone), PorterDuff.Mode.SRC_ATOP);
                            new CountDownTimer((totalDuration+1)*1000, 100) {
                                public void onTick(long millisUntilFinished) {
                                    holder.imageButtonCallMessageIcon.setText(String.format("%02d s", totalDuration-(player.getCurrentPosition()/1000)));
                                }
                                public void onFinish() {
                                    holder.imageButtonCallMessageIcon.setText(String.format("%02d s", totalDuration));
                                    holder.imageButtonCallMessageIcon.getCompoundDrawables()[1].setColorFilter(context.getResources().getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_ATOP);
                                }
                            }.start();
                            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                }
                            });
                        }
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return calls.size();
    }

}