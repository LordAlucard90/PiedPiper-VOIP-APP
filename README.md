# PiedPiper-VOIP-APP
PiedPiper is a Android VOIP application (name inspired from the homonym TV Show) developed for the Human Computer Interaction university course.

In this project has been tried to add a new small features to a standard phone's call manager and measure the effect on the user experience.

The new feature introduced allows a caller, on call without response, to leave a message (textual or vocal) to the called.

The experiments showed a good index of approval of the proposed functionality.


## Dependences

- `app/libs/`
    - [liblinphone-sdk (version 3.2.7)](http://www.linphone.org/technical-corner/liblinphone/downloads)

- `app/src/"..."/piedpiper/Helpers/`
    - [AppHelper, VolleyMultipartRequest, VolleySingleton](https://gist.github.com/anggadarkprince/a7c536da091f4b26bb4abf2f92926594)
    - [LruBitmapCache](https://gist.github.com/ficusk/5614325)

- A VOIP server (Asterisk)

- A PHP server (Apache2)

## Configuration
- Set on `app/src/"..."/piedpiper/Controllers/Services/VoipService.java`, line 118, the IP of asterisk and php server.

- Set Asterisk: [Hello World Tutorial](https://wiki.asterisk.org/wiki/display/AST/Hello+World).
 
- Copy .conf files from `/server/asterisk/` to `/etc/asterisk/`

- Set Apache2 with PHP and ensure that the directory `/var/www/html/` is writable by **www-user**.

- Copy .php files from `/server/apache/` to `/var/www/html/`.

### 2 Phones Configuration
- Add John or Jane Doe to phone's contacts.

- Set the other number as Piedpiper user at the beginning of the application.

- Start a call.

### 1 Phone Configuration
- Change on Asterisk configurations files the '100' account to John or Jane Doe number and add it to phone's contacts.

- Delete the other configurations with that number.

- Set the other number as Piedpiper user at the beginning of the application.

- Listen the 'hello world' record.
