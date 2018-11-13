package it.unifi.hci.piedpiper.Fragments.Elements;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.unifi.hci.piedpiper.Fragments.PermissionsChecker;
import it.unifi.hci.piedpiper.MainActivity;
import it.unifi.hci.piedpiper.Models.ContactModel;


public class PhoneSearchList extends RecyclerView implements PermissionsChecker{
    private ArrayList<ContactModel> contacts = new ArrayList<ContactModel>();
    private ArrayList<ContactModel.SingleContact> filtered = new ArrayList<ContactModel.SingleContact>();
    private String searchPhoneNumber = "";
    private HashMap<Integer, String> inputCharsHistory = new HashMap<Integer, String>();
    private HashMap<Integer, ArrayList<String>> serchPatternsHistory = new HashMap<Integer, ArrayList<String>>();
    private MainActivity mainActivity = null;
    private Boolean contactAccess = Boolean.FALSE;
    private Boolean onlyNumberSearch = Boolean.FALSE;
    private Boolean contactSelection = Boolean.FALSE;
    private String numberSelected = "";

    public PhoneSearchList(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAdapter(new PhoneListSearchElement(filtered, getContext()));
    }

    public String getSearchPhoneNumber(){
        if(contactSelection){
            return numberSelected;
        } else {
            return searchPhoneNumber;
        }
    }

    public void setActivity(Activity activity){
        if(activity instanceof MainActivity){
            mainActivity = (MainActivity) activity;
            if(mainActivity.checkContactsPermission(this)){
                contactAccess = Boolean.TRUE;
                contacts = ContactModel.getAll(getContext());
            }
        }
    }

    @Override
    public void onPermissionGranted() {
        contactAccess = Boolean.TRUE;
        contacts = ContactModel.getAll(getContext());
        updateFiltered();
    }

    public void addSearchNumber(char num, String chars){
        searchPhoneNumber+=num;
        if(chars.length()==0){
            onlyNumberSearch = Boolean.TRUE;
        }
        if(!onlyNumberSearch){
            inputCharsHistory.put(searchPhoneNumber.length(), chars);
            serchPatternsHistory.put(searchPhoneNumber.length(), new ArrayList<String>());
            if(searchPhoneNumber.length()==1){
                for(int i=0; i < chars.length(); i++){
                    serchPatternsHistory.get(searchPhoneNumber.length()).add(chars.substring(i,i+1));
                }
            } else {
                for(String prefix: serchPatternsHistory.get(searchPhoneNumber.length()-1)){
                    for(int i=0; i < chars.length(); i++){
                        serchPatternsHistory.get(searchPhoneNumber.length()).add(prefix+"\\s*"+chars.substring(i,i+1));
                    }
                }
           }
        }
        updateFiltered();
    }

    public void removeLastSearchNumber(){
        if(contactSelection){
            contactSelection = Boolean.FALSE;
            numberSelected = "";
        } else {
            if(onlyNumberSearch){
                if(inputCharsHistory.containsKey(searchPhoneNumber.length()-1) || searchPhoneNumber.length()==1){
                    onlyNumberSearch = Boolean.FALSE;
                }
            } else {
                inputCharsHistory.remove(searchPhoneNumber.length());
                serchPatternsHistory.remove(searchPhoneNumber.length());
            }
            searchPhoneNumber = searchPhoneNumber.substring(0, searchPhoneNumber.length()-1);
        }
        updateFiltered(Boolean.TRUE);
    }

    public void removeSearchPhoneNumber(){
        onlyNumberSearch = Boolean.FALSE;
        inputCharsHistory.clear();
        serchPatternsHistory.clear();
        searchPhoneNumber= "";
        contactSelection = Boolean.FALSE;
        numberSelected = "";
        updateFiltered();
    }

    public void setPhoneNumber(String number){
        contactSelection = Boolean.TRUE;
        numberSelected = number;
        updateFiltered();
    }

    private void updateFiltered(){
        updateFiltered(Boolean.FALSE);
    }

    private void updateFiltered(Boolean deletion){
        if(contactAccess){
            ArrayList<ContactModel.SingleContact> displayed = new ArrayList<ContactModel.SingleContact>();
            if(searchPhoneNumber.length()>0 && !contactSelection){
                if(!onlyNumberSearch) {
                    ArrayList<ContactModel.SingleContact> bottoms = new ArrayList<ContactModel.SingleContact>();
                    String pattern = TextUtils.join("|", serchPatternsHistory.get(searchPhoneNumber.length()));
                    Pattern p = Pattern.compile(pattern);
                    if (filtered.size() == 0 || deletion) {
                        for (ContactModel c : contacts) {
                            Matcher m = p.matcher(c.getName().toLowerCase());
                            if (m != null && pattern.length()>0) {
                                if (m.find()) {
                                    displayed.addAll(c.getSingleContact());
                                }
                            }
                        }
                        ArrayList<ContactModel.SingleContact> searchByNumber = ContactModel.searchSingleContactsByNumber(getContext(), searchPhoneNumber);
                        for (ContactModel.SingleContact sc : searchByNumber) {
                            Matcher m = p.matcher(sc.getName().toLowerCase());
                            if (m != null) {
                                if (!m.find() || pattern.length()==0) { bottoms.add(sc);}
                            }
                        }
                    } else {
                        for (ContactModel.SingleContact c : filtered) {
                            if (c.getNumber().contains(searchPhoneNumber)){
                                bottoms.add(c);
                            } else {
                                Matcher m = p.matcher(c.getName().toLowerCase());
                                if (m != null && pattern.length()>0)
                                    { if (m.find()) { displayed.add(c); } }
                            }
                        }
                    }
                    displayed.addAll(bottoms);
                    filtered.clear();
                    filtered.addAll(displayed);
                    // necessary search optimization
                    ArrayList<String> toRemove = new ArrayList<String>();
                    for(String sp: serchPatternsHistory.get(searchPhoneNumber.length())){
                        Boolean match = Boolean.FALSE;
                        Pattern pt = Pattern.compile(sp);
                        Iterator<ContactModel.SingleContact> it = displayed.listIterator();
                        while (it.hasNext() && !match){
                            Matcher m = pt.matcher(it.next().getName().toLowerCase());
                            if (m != null) { if (m.find()) { match = Boolean.TRUE; } }
                        }
                        if(!match){ toRemove.add(sp); }
                    }
                    serchPatternsHistory.get(searchPhoneNumber.length()).removeAll(toRemove);
                } else {
                    displayed = ContactModel.searchSingleContactsByNumber(getContext(), searchPhoneNumber);
                }
            } else {
                if(contactSelection){
                    displayed = ContactModel.searchSingleContactsByNumber(getContext(), numberSelected);
                }
            }
            ((PhoneListSearchElement) getAdapter()).updateList(displayed, getPatternsMap());
        }
    }

    public HashMap<String, String> getPatternsMap(){
        if( searchPhoneNumber.length()>0 || contactSelection){
            HashMap<String, String> patternsMap = new HashMap<>();
            if(contactSelection) {
                patternsMap.put("num", numberSelected);
            } else {
                patternsMap.put("num", searchPhoneNumber);
                if(!onlyNumberSearch){
                    if(serchPatternsHistory.get(searchPhoneNumber.length()).size()>0){
                        patternsMap.put("txt", TextUtils.join("|", serchPatternsHistory.get(searchPhoneNumber.length())));
                    }
                }
            }
            return patternsMap;
        }
        return null;
    }

    public ArrayList<ContactModel.SingleContact> getFiltered() {
        return (ArrayList<ContactModel.SingleContact>) filtered.clone();
    }
}
