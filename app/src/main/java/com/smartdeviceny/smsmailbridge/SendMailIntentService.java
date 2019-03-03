package com.smartdeviceny.smsmailbridge;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;


import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.smartdeviceny.smsmailbridge.misc.Constants;
import com.smartdeviceny.smsmailbridge.misc.NotificationUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


public class SendMailIntentService extends IntentService {

    public static final String SEND_MAIL_INTENT_SERVICE = "SendMailIntentService";

    private Handler toastHandler;
    private SharedPreferences sharedPreferences;
    private static final String[] SCOPES = {GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_COMPOSE, GmailScopes.GMAIL_INSERT, GmailScopes.GMAIL_MODIFY, GmailScopes.GMAIL_READONLY, GmailScopes.MAIL_GOOGLE_COM};

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public SendMailIntentService() {
        super(SEND_MAIL_INTENT_SERVICE);
        toastHandler = new Handler(Looper.getMainLooper());
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Activity.MODE_PRIVATE);

        String subject = intent.getStringExtra(Constants.EXTRA_SUBJECT);
        String body = intent.getStringExtra(Constants.EXTRA_BODY);
        String from = intent.getStringExtra(Constants.ACCOUNT_NAME);
        Set<String> to = sharedPreferences.getStringSet(Constants.SHARED_PREF_RECIPIENTS, new HashSet<String>());
        boolean showNotification = sharedPreferences.getBoolean(Constants.SHARED_PREF_SHOW_NOTIFICATION, true);
        String toRecipient = "";
        if (!to.isEmpty()) {
            toRecipient = to.toArray(new String[0])[0].trim();
        }
        if (from == null || from.isEmpty() || to.isEmpty() || toRecipient.isEmpty()) {
            NotificationUtils.notify_user(this.getApplicationContext(), Constants.NOTIFICATION_CHANNEL, Constants.NOTIFICATION_GROUP_MAIN, "Not Configured", "Please run the SMS Mailer App", 2, true);
            return;
        }


        // new
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());
        ;


        credential.setSelectedAccountName(from);
        Gmail mService = new com.google.api.services.gmail.Gmail.Builder(transport, jsonFactory, credential).setApplicationName(
                getResources().getString(R.string.app_name)).build();


        try {

            String result = SendGmailEmail(mService, toRecipient, credential.getSelectedAccountName(), subject, body);
            toastHandler.post(new ToastRunnable(getString(R.string.mail_sent_successfully)));
            if( showNotification) {
                NotificationUtils.notify_user(this.getApplicationContext(), Constants.NOTIFICATION_CHANNEL, Constants.NOTIFICATION_GROUP_MAIN, "Mail Bridged", body + "\n" + result,
                        3);
            }
            //NotificationUtils.notify_user(this.getApplicationContext(), Constants.NOTIFICATION_CHANNEL, Constants.NOTIFICATION_GROUP_MAIN, "Mail Bridged", body, 4);

        } catch (Exception e) {
            toastHandler.post(new ToastRunnable(getString(R.string.mail_send_problem)));
            Log.e(SEND_MAIL_INTENT_SERVICE, getString(R.string.mail_could_not_send_email), e);
            NotificationUtils.notify_user(this.getApplicationContext(), Constants.NOTIFICATION_CHANNEL, Constants.NOTIFICATION_GROUP_MAIN, "Mailer Error", body, 4);
        }

    }

    private class ToastRunnable implements Runnable {
        String message;

        public ToastRunnable(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }


    private String SendGmailEmail(Gmail mService, String to, String from, String subject, String body) throws IOException {
        // getting Values for to Address, from Address, Subject and Body
        String user = "me";
        MimeMessage mimeMessage;
        String response = "";
        try {
            mimeMessage = createEmail(to, from, subject, body);
            response = sendMessage(mService, user, mimeMessage);
        }  catch (MessagingException e) {
            e.printStackTrace();
        }
        return response;
    }

    // Method to send email
    private String sendMessage(Gmail service, String userId, MimeMessage email) throws MessagingException, IOException {
        Message message = createMessageWithEmail(email);
        // GMail's official method to send email with oauth2.0
        message = service.users().messages().send(userId, message).execute();

        System.out.println("Message id: " + message.getId());
        System.out.println(message.toPrettyString());
        return message.getId();
    }

    // Method to create email Params
    private MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        InternetAddress tAddress = new InternetAddress(to);
        InternetAddress fAddress = new InternetAddress(from);

        email.setFrom(fAddress);
        email.addRecipient(javax.mail.Message.RecipientType.TO, tAddress);
        email.setSubject(subject);

        // Create Multipart object and add MimeBodyPart objects to this object
        Multipart multipart = new MimeMultipart();

        // Changed for adding attachment and text
        // email.setText(bodyText);

        BodyPart textBody = new MimeBodyPart();
        textBody.setText(bodyText);
        multipart.addBodyPart(textBody);

        //Set the multipart object to the message object
        email.setContent(multipart);
        return email;
    }

    private Message createMessageWithEmail(MimeMessage email) throws MessagingException, IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        email.writeTo(bytes);
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray());
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
}
