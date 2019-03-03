package com.smartdeviceny.smsmailbridge;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.smartdeviceny.smsmailbridge.misc.Constants;
import com.smartdeviceny.smsmailbridge.misc.PermissionUtils;
import com.smartdeviceny.smsmailbridge.misc.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Main Activity
 * <p>
 * inspired by: http://www.jondev.net/articles/Sending_Emails_without_User_Intervention_%28no_Intents%29_in_Android
 */
public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private EditText etRecipients;
    private FloatingActionButton sendFabButton;
    private TextView textViewAccountName;
    private Button buttonSimulateSMS;


    GoogleAccountCredential mCredential;
    private static final String[] SCOPES = {GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_COMPOSE, GmailScopes.GMAIL_INSERT, GmailScopes.GMAIL_MODIFY, GmailScopes.GMAIL_READONLY, GmailScopes.MAIL_GOOGLE_COM};
   // private InternetDetector internetDetector;
    public String fileName = "";

    private void init() {
        // Initializing Internet Checker
        //internetDetector = new InternetDetector(getApplicationContext());
        mCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());
        sendFabButton = findViewById(R.id.fab);
        textViewAccountName = findViewById(R.id.accountNameView);
        buttonSimulateSMS = findViewById(R.id.buttonSimulateSMS);

        sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Activity.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        etRecipients = findViewById(R.id.et_recipients);
        restorePreferences();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        textViewAccountName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePreferences();
                String accountName = sharedPreferences.getString(Constants.ACCOUNT_NAME, "");
                if (!accountName.isEmpty()) {
                    mCredential.setSelectedAccountName(accountName);
                }
                startActivityForResult(mCredential.newChooseAccountIntent(), Utils.REQUEST_ACCOUNT_PICKER_NO_MAIL);
            }
        });

        sendFabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePreferences();
                //getResultsFromApi(view);
                String accountName = sharedPreferences.getString(Constants.ACCOUNT_NAME, "");
                if (accountName.isEmpty()) {
                    showMessage(view, "Sender not configured");
                    return;
                }
                if (Utils.isEmpty(etRecipients)) {
                    showMessage(view, "Email Recipient not configured");
                }
                if (!PermissionUtils.isSMSReceivePermissionAvailable(
                        MainActivity.this) || !isGooglePlayServicesAvailable() || (mCredential.getSelectedAccountName() == null) || Utils.isEmpty(etRecipients)) {
                    showMessage(view, "Please run verify, do not have proper permissions/config");
                    return;
                }
                new MakeRequestTask(MainActivity.this, mCredential).execute();
            }
        });


        Button button = findViewById(R.id.button_verify);
        assert button != null;
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                savePreferences();
                getResultsFromApi(view);


            }
        });
        buttonSimulateSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePreferences();

                Intent intent = new Intent(getApplicationContext(), SendMailIntentService.class);
                String accountName = sharedPreferences.getString(Constants.ACCOUNT_NAME, "");
                if (accountName.isEmpty()) {
                    showMessage(view, "Google Account not configured");
                    return;
                }
                mCredential.setSelectedAccountName(accountName);
                if (Utils.isEmpty(etRecipients)) {
                    showMessage(view, "Email Recipient not configured");
                }
                if (!PermissionUtils.isSMSReceivePermissionAvailable(
                        MainActivity.this) || !isGooglePlayServicesAvailable() || (mCredential.getSelectedAccountName() == null) || Utils.isEmpty(etRecipients)) {

                    showMessage(view, "Please run verify, do not have proper permissions/config");
                    return;
                }
                intent.putExtra(Constants.ACCOUNT_NAME, accountName);
                intent.putExtra(Constants.EXTRA_SUBJECT, getString(R.string.default_mail_subject));
                intent.putExtra(Constants.EXTRA_BODY, getString(R.string.default_mail_body));
                startService(intent);
            }
        });

    }



    private void restorePreferences() {
        Set<String> recipientsSet = sharedPreferences.getStringSet(Constants.SHARED_PREF_RECIPIENTS, new HashSet<String>());
        StringBuilder recipients = new StringBuilder("");
        int recipientsCount = 1;
        for (String recipient : recipientsSet) {
            recipients.append(recipient);
            if (recipientsSet.size() > recipientsCount) {
                recipients.append(";");
            }
            recipientsCount++;
        }
        if (!recipients.toString().isEmpty()) {
            etRecipients.setText(recipients);
        }
        String accountName = sharedPreferences.getString(Constants.ACCOUNT_NAME, "");
        if (!accountName.isEmpty()) {
            textViewAccountName.setText(accountName);
        }
    }

    private void savePreferences() {
        if (etRecipients.getText() != null) {
            editor.putStringSet(Constants.SHARED_PREF_RECIPIENTS, new HashSet<String>(Arrays.asList(etRecipients.getText().toString().split(";"))));
        }
        editor.apply();
    }

    // Method for Checking Google Play Service is Available
    private boolean isGooglePlayServicesAvailable() {
        return PermissionUtils.isGooglePlayServicesAvailable(this);
    }

    // Method to Show Info, If Google Play Service is Not Available.
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    // Method for Google Play Services Error Info
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(MainActivity.this, connectionStatusCode, Utils.REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    // Storing Mail ID using Shared Preferences
    private void chooseAccount(View view) {
        if (Utils.checkPermission(getApplicationContext(), Manifest.permission.GET_ACCOUNTS)) {
            String accountName = sharedPreferences.getString(Constants.ACCOUNT_NAME, "");

            if (!accountName.isEmpty()) {
                mCredential.setSelectedAccountName(accountName);
                textViewAccountName.setText(accountName);
                getResultsFromApi(view);
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(mCredential.newChooseAccountIntent(), Utils.REQUEST_ACCOUNT_PICKER);
            }
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.GET_ACCOUNTS}, Utils.REQUEST_PERMISSION_GET_ACCOUNTS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Utils.REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    showMessage(sendFabButton, "This app requires Google Play Services. Please install " + "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi(sendFabButton);
                }
                break;
            case Utils.REQUEST_ACCOUNT_PICKER:
            case Utils.REQUEST_ACCOUNT_PICKER_NO_MAIL:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(Constants.ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        textViewAccountName.setText(accountName);
                        //if( requestCode != Utils.REQUEST_ACCOUNT_PICKER_NO_MAIL) {
                        getResultsFromApi(sendFabButton);
                        //}
                    } else {
                        chooseAccount(sendFabButton);
                    }
                }
                break;
            case Utils.REQUEST_PERMISSION_GET_ACCOUNTS:
                if (resultCode != RESULT_OK) {
                    showMessage(sendFabButton, "This App requires the SMS Read ability");
                } else {
                    getResultsFromApi(sendFabButton);
                }
                break;
            case Utils.REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi(sendFabButton);
                }
                break;
            case Utils.REQUEST_PERMISSION_RECV_SMS:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi(sendFabButton);
                } else {
                    showMessage(sendFabButton, "This App requires the SMS Recv ability");
                }
                break;
            case Utils.REQUEST_PERMISSION_READ_PHONE_NUMBERS:
                if(resultCode == RESULT_OK) {
                    getResultsFromApi(sendFabButton);
                } else {
                    showMessage(sendFabButton, "The App need access to the phone number to send in the email.");
                }
        }
    }

    private void showMessage(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }


    private void getResultsFromApi(View view) {
////        if (!PermissionUtils.isSMSReadPermissionGranted(this)) {
//            requestReadAndSendSmsPermission();
//        } else
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_NUMBERS}, Utils.REQUEST_PERMISSION_READ_PHONE_NUMBERS);
            } else if (!PermissionUtils.isSMSReceivePermissionAvailable(this)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, Utils.REQUEST_PERMISSION_RECV_SMS);
            } else if (!isGooglePlayServicesAvailable()) {
                acquireGooglePlayServices();
            } else if (mCredential.getSelectedAccountName() == null) {
                chooseAccount(view);
            }  else if (Utils.isEmpty(etRecipients)) {
                showMessage(view, "Email Recipient not configured");
            } else {
                //new MakeRequestTask(this, mCredential).execute();
                showMessage(view, "All permissions are correct.");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    private class MakeRequestTask extends AsyncTask<Void, Void, String> {

        private com.google.api.services.gmail.Gmail mService = null;
        private Exception mLastError = null;
        private View view = sendFabButton;
        private MainActivity activity;

        MakeRequestTask(MainActivity activity, GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(transport, jsonFactory, credential).setApplicationName(getResources().getString(R.string.app_name)).build();
            this.activity = activity;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        private String getDataFromApi() throws IOException {
            // getting Values for to Address, from Address, Subject and Body
            String user = "me";
            String to = Utils.getString(etRecipients);
            String from = mCredential.getSelectedAccountName();
            String subject = "Test Email";
            String body = "Empty Body";
            MimeMessage mimeMessage;
            String response = "";
            try {
                mimeMessage = createEmail(to, from, subject, body);
                response = sendMessage(mService, user, mimeMessage);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
            return response;
        }

        // Method to send email
        private String sendMessage(Gmail service, String userId, MimeMessage email) throws MessagingException, IOException {
            Message message = createMessageWithEmail(email);
            // GMail's official method to send email with oauth2.0
            message = service.users().messages().send(userId, message).execute();
//            System.out.println("Message id: " + message.getId());
//            System.out.println(message.toPrettyString());
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

            if (!(activity.fileName.equals(""))) {
                // Create new MimeBodyPart object and set DataHandler object to this object
                MimeBodyPart attachmentBody = new MimeBodyPart();
                String filename = activity.fileName; // change accordingly
                DataSource source = new FileDataSource(filename);
                attachmentBody.setDataHandler(new DataHandler(source));
                attachmentBody.setFileName(filename);
                multipart.addBodyPart(attachmentBody);
            }

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

        @Override
        protected void onPreExecute() {
            // mProgress.show();
        }

        @Override
        protected void onPostExecute(String output) {
            //mProgress.hide();
            if (output == null || output.length() == 0) {
                showMessage(view, "Failed sending an email");
            } else {
                showMessage(view, "Successfully sent an email");
            }
        }

        @Override
        protected void onCancelled() {
            //mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(((GooglePlayServicesAvailabilityIOException) mLastError).getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(((UserRecoverableAuthIOException) mLastError).getIntent(), Utils.REQUEST_AUTHORIZATION);
                } else {
                    showMessage(view, "The following error occurred:\n" + mLastError);
                    Log.v("Error failure", mLastError + "");
                }
            } else {
                showMessage(view, "Request Cancelled.");
            }
        }
    }
}
