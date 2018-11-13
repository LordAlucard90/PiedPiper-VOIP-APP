package it.unifi.hci.piedpiper.Models;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneNumberUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ContactModel {
    private static final String PHONE_NUMBER = CommonDataKinds.Phone.NUMBER;
    private static final String PHONE_CONTACT_ID = CommonDataKinds.Phone.CONTACT_ID;
    private static final String DISPLAY_NAME = CommonDataKinds.Phone.DISPLAY_NAME;
    private static final String HAS_PHONE_NUMBER = CommonDataKinds.Phone.HAS_PHONE_NUMBER;
    private static final String PHOTO_URI = Contacts.Photo.PHOTO_THUMBNAIL_URI;
    private static final String ACCOUNT_TYPE = ContactsContract.RawContacts.ACCOUNT_TYPE;

    private Integer ID;
    private String name;
    private String photo_uri;
    private ArrayList<String> numbers = new ArrayList<String>();
    private static String ppUsers = "0123456789;9876543210;"; // this could be loaded from a server..
    static private ArrayList<ContactModel> contacts = null;
    private Boolean isPPUser = false;
    private String ppNum = "";

    public Boolean isPPUser() {
        return isPPUser;
    }

    private ContactModel(Integer ID, String Name){
        this.ID = ID;
        this.name = Name;
    }

    private ContactModel(Integer ID, String Name, String photo_uri){
        this(ID, Name);
        this.photo_uri = photo_uri;
    }

    private ContactModel(Integer ID, String Name, String Photo_Uri, ArrayList<String> Numbers){
        this(ID, Name, Photo_Uri);
        this.numbers = Numbers;
    }

    public static ContactModel getEmptyOne(){
        ContactModel c = new ContactModel(0,"");
        c.isPPUser=true;
        return c;
    }

    public static ContactModel getTestContact(){
        ContactModel c = new ContactModel(1,"Test");
        ArrayList<String> nums = new ArrayList<String>();
        nums.add("6001");
        c.setNumbers(nums);
        c.isPPUser=true;
        c.ppNum="6001";
        return c;
    }

    public String getName(){
        return this.name;
    }

    public String getPhotoUri(){
        return this.photo_uri;
    }

    public Integer getID(){
        return this.ID;
    }

    private void setNumbers(ArrayList<String> numbers){
        this.numbers = numbers;
        for(String n : this.numbers){
            for (String pp_num : getPPUsers()) {
                if (PhoneNumberUtils.compare(n, pp_num)) {
                    this.isPPUser = true;
                    this.ppNum = pp_num;
                    return;
                }
            }
        }
    }

    public static String[] getPPUsers() {
        String[] pp_users_list = ppUsers.split(";");
        for (int i = 0; i < pp_users_list.length; i++)
            { pp_users_list[i] = pp_users_list[i].trim(); }
        return pp_users_list;
    }

    public ArrayList<String> getNumbers(){
        return (ArrayList<String>) this.numbers.clone();
    }

    private static void setContacts(Context context){
        contacts = new ArrayList<ContactModel>();
        HashMap<Integer, ArrayList<String>> numbersMap = new HashMap<>();
        ContentResolver cr = context.getContentResolver();
        Cursor pCur = cr.query(
                CommonDataKinds.Phone.CONTENT_URI,
                new String[]{DISPLAY_NAME, PHONE_CONTACT_ID, HAS_PHONE_NUMBER, PHONE_NUMBER, PHOTO_URI, ACCOUNT_TYPE},
                HAS_PHONE_NUMBER + " > 0",
                null,
                DISPLAY_NAME + " ASC"
        );
        if(pCur != null){
            if(pCur.getCount() > 0) {
                while (pCur.moveToNext()) {
                    Integer cId = pCur.getInt(pCur.getColumnIndex(PHONE_CONTACT_ID));
                    if(numbersMap.containsKey(cId)){
                        String cNumber = pCur.getString(pCur.getColumnIndex(PHONE_NUMBER)).replaceAll("\\s+","");
                        if (!numbersMap.get(cId).contains(cNumber))
                        { numbersMap.get(cId).add(cNumber); }
                    } else {
                        String cName =pCur.getString(pCur.getColumnIndex(DISPLAY_NAME));
                        String cNumber =pCur.getString(pCur.getColumnIndex(PHONE_NUMBER));
                        String image_uri = pCur.getString(pCur.getColumnIndex(PHOTO_URI));

                        String cType = pCur.getString(pCur.getColumnIndex(ACCOUNT_TYPE));

                        if(image_uri==null || cType.toLowerCase().contains("sim")){
                            contacts.add(new ContactModel(cId, cName));
                        }else{
                            contacts.add(new ContactModel(cId, cName,image_uri));
                        }
                        numbersMap.put(cId, new ArrayList<String>());
                        numbersMap.get(cId).add(cNumber.replaceAll("\\s+",""));
                    }
                }
                for (ContactModel contact: contacts){
                    contact.setNumbers(numbersMap.get(contact.getID()));
                }
            }
            pCur.close();
        }
        Collections.sort(contacts, new Comparator<ContactModel>() {
            @Override
            public int compare(ContactModel c1, ContactModel c2) {
                return c1.getName().toLowerCase().compareTo(c2.getName().toLowerCase());
            }
        });
    }

    public static ArrayList<ContactModel> getAll(Context context) {
        if(contacts == null){
            ContactModel.setContacts(context);
        }
        return (ArrayList<ContactModel>) contacts.clone();
    }

    public static ArrayList<SingleContact> searchSingleContactsByNumber(Context context, String number) {
        if (contacts==null) { setContacts(context); }
        ArrayList<SingleContact> singleContacts = new ArrayList<SingleContact>();
        for (ContactModel c: contacts){
            singleContacts.addAll(c.getSingleContactsWhithMatchNumber(number));
        }
        return singleContacts;
    }

    public static SingleContact searchSingleContactByNumber(Context context, String number) {
        // TEST
        if(number.equals("6001")) { return getTestContact().getSingleContact().get(0); }
        // normal flow
        if (contacts==null) { setContacts(context); }
        for (ContactModel c: contacts){
            SingleContact sc = c.getSingleContactWhithNumber(number);
            if (sc != null) { return sc; }
        }
        return null;
    }

    public static ArrayList<ContactModel> searchByNumber(Context context, String number) {
        if (contacts==null) { setContacts(context); }
        ArrayList<ContactModel> contactList = new ArrayList<ContactModel>();
        for (ContactModel c: contacts){
            if(c.hasNumberWhithMatch(number))
                { contactList.add(c); }
        }
        return contactList;
    }

    public static String[] getAllLetters(Context context) {
        Set<String> letters = new HashSet<>();

        ContentResolver cr = context.getContentResolver();

        Cursor pCur = cr.query(
                CommonDataKinds.Phone.CONTENT_URI,
                new String[]{DISPLAY_NAME},
                null,
                null,
                DISPLAY_NAME + " ASC"
        );
        if(pCur != null){
            if(pCur.getCount() > 0) {
                while (pCur.moveToNext()) {
                    String cName =pCur.getString(pCur.getColumnIndex(DISPLAY_NAME));
                    if (Character.isLetter(cName.charAt(0))) {
                        letters.add(""+cName.charAt(0));
                    }
                }

                return (String[]) letters.toArray(new String[letters.size()]);
            }
            pCur.close();
        }
        return null;
    }

    public static class SingleContact{
        private Integer ID;
        private String name;
        private String photoUri = null;
        private String number;
        private Boolean isPPUser;

        public SingleContact(Integer ID, String name, String number, Boolean isPPUser){
            this.ID = ID;
            this.name = name;
            this.number = number;
            this.isPPUser = isPPUser;
        }

        public SingleContact(Integer ID, String name, String number, Boolean isPPUser, String photoUri){
            this(ID, name, number, isPPUser);
            this.photoUri = photoUri;
        }

        public Integer getID() {
            return ID;
        }

        public String getName() {
            return name;
        }

        public String getNumber() {
            return number;
        }

        public String getPhotoUri() {
            return photoUri;
        }

        public Boolean isPPUser(){
            return isPPUser;
        }

        @Override
        public String toString() {
            return this.name+" ("+this.number+")";
        }

    }

    public Boolean hasNumberWhithMatch(String num){
        for (String number: numbers){
            if(number.contains(num)){
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    public ArrayList<SingleContact> getSingleContactsWhithMatchNumber(String number){
        ArrayList<SingleContact> singleContacts = new ArrayList<SingleContact>();
        for (String aNumber: numbers){
            if(aNumber.contains(number)){
                if(isPPUser){
                    singleContacts.add(new SingleContact(ID, name, aNumber, PhoneNumberUtils.compare(aNumber, ppNum), photo_uri));
                } else {
                    singleContacts.add(new SingleContact(ID, name, aNumber, false, photo_uri));
                }
            }
        }
        return singleContacts;
    }

    public SingleContact getSingleContactWhithNumber(String number){
        for (String aNumber: numbers){
            if(PhoneNumberUtils.compare(aNumber, number)){
                if(isPPUser){
                    return new SingleContact(ID, name, aNumber, PhoneNumberUtils.compare(aNumber, ppNum), photo_uri);
                } else {
                    return new SingleContact(ID, name, aNumber, false, photo_uri);
                }
            }
        }
        return null;
    }

    public ArrayList<SingleContact> getSingleContact(){
        ArrayList<SingleContact> singleContacts = new ArrayList<SingleContact>();
        for(String number: numbers){
            if(isPPUser){
                singleContacts.add(new SingleContact(ID, name, number, PhoneNumberUtils.compare(number, ppNum), photo_uri));
            } else {
                singleContacts.add(new SingleContact(ID, name, number, false, photo_uri));
            }
        }
        return singleContacts;
    }

    @Override
    public String toString() {
        return this.name;
    }

}