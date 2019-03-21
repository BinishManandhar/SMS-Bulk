package com.binish.smstesting;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_READ_PHONE_STATE = 103;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int PERMISSION_SEND_SMS = 123;
    private static final int READ_SMS_PERMISSIONS_REQUEST = 1;
    Button sendBtn;
    EditText txtphoneNo;
    Button browseButton;
    String phoneNo;
    String message;
    String FilePath;
    int len = 0;
    TextView textProgressBar;
    TextView totalProgressBar;
    ProgressBar progressBar;
    ArrayList<PendingIntent> sentPendingIntents;
    ArrayList<PendingIntent> deliveredPendingIntents;
    int count=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sendBtn = findViewById(R.id.btnSendSMS);
        txtphoneNo = findViewById(R.id.editText);
        browseButton = findViewById(R.id.browseBtn);
        textProgressBar = findViewById(R.id.textProgressBar);
        totalProgressBar = findViewById(R.id.totalProgressBar);
        progressBar = findViewById(R.id.progressBar);
        phoneNo = txtphoneNo.getText().toString();


        sendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                len = 0;
                count=0;
                textProgressBar.setText(String.valueOf(len));
                progressBar.setProgress(len);
                requestSmsPermission();
            }
        });
        browseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCSV();
            }
        });
    }

    protected void sendSMSMessage() {

        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";
        // STEP-1___
        // SEND PendingIntent
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(
                SENT), 0);

        // DELIVER PendingIntent
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        sentPendingIntents = new ArrayList<>();
        deliveredPendingIntents = new ArrayList<>();

        // STEP-2___
        // SEND BroadcastReceiver
        BroadcastReceiver sendSMS = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent",
                                Toast.LENGTH_SHORT).show();
                        len++;
                        textProgressBar.setText(String.valueOf(len));
                        progressBar.setProgress(len);
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        // DELIVERY BroadcastReceiver
        BroadcastReceiver deliverSMS = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        // STEP-3___
        // ---Notify when the SMS has been sent---
        registerReceiver(sendSMS, new IntentFilter(SENT));

        // ---Notify when the SMS has been delivered---
        registerReceiver(deliverSMS, new IntentFilter(DELIVERED));


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            getPermissionToReadSMS();
        } else {

            File csvfile = new File(FilePath);
            CSVReader reader = null;
            List<String[]> allData = null;
            int totalCount = 0;
            try {
                reader = new CSVReader(new FileReader(csvfile.getAbsolutePath()));
                allData = reader.readAll();
                totalCount = allData.size();
            } catch (Exception e) {
                Toast.makeText(this, "Exception:"+ e, Toast.LENGTH_LONG).show();
            }
            smsLoop(allData,totalCount,sentPI, deliveredPI);

        }
    }

    public void smsLoop(final List<String[]> allData, final int totalCount, final PendingIntent sentPI, final PendingIntent deliveredPI) {
        String[] nextLine;
        SmsManager smsManager = SmsManager.getDefault();
        float dummy;
        try {
            nextLine = allData.get(count);
            if (!nextLine[0].equalsIgnoreCase("Phone Number")) {
                Toast.makeText(this, "Phone Number:"+nextLine[0]+" Message:"+nextLine[1], Toast.LENGTH_SHORT).show();

                ArrayList<String> mSMSMessage = smsManager.divideMessage(nextLine[1]);
                if(mSMSMessage.size()==1){
//                    smsManager.sendTextMessage(nextLine[0], null, nextLine[1], sentPI, deliveredPI);
                    Log.i("checking","SMS length:"+mSMSMessage.size());
                }
                else if(mSMSMessage.size()>1){
                    for (int i = 0; i < mSMSMessage.size(); i++) {
                        sentPendingIntents.add(i, sentPI);

                        deliveredPendingIntents.add(i, deliveredPI);
                    }
                    /*smsManager.sendMultipartTextMessage(nextLine[0], null, mSMSMessage,
                            sentPendingIntents, deliveredPendingIntents);*/
                    Log.i("checking","SMS length:"+mSMSMessage.size());
                }
                dummy = (float) count / (totalCount-1);
                progressBar.setProgress(Math.round(dummy*100));
                textProgressBar.setText(String.valueOf(count));
            }
            count++;
            if(count==totalCount)
                return;
            else if((count%2)==0){
                new CountDownTimer(10000, 1000) {

                    public void onTick(long millisUntilFinished) {

                    }

                    public void onFinish() {
                        smsLoop(allData,totalCount,sentPI,deliveredPI);
                    }
                }.start();
            }
            else {
                smsLoop(allData,totalCount,sentPI,deliveredPI);
            }


            /*while ((nextLine = reader.readNext()) != null) {
                if (!nextLine[0].equalsIgnoreCase("Phone Number")) {
                    count++;
                    System.out.println(nextLine[0] + " " + nextLine[1]);
//                smsManager.sendTextMessage(nextLine[0], null, nextLine[1], sentPI, deliveredPI);
                }
            }*/
        } catch (Exception e) {
            Toast.makeText(this, "SMS Exception:" + e, Toast.LENGTH_LONG).show();
        }
    }

    public void getCSV() {
        if (Build.VERSION.SDK_INT > 22) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/csv");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        //intent.putExtra("browseCoa", itemToBrowse);
        //Intent chooser = Intent.createChooser(intent, "Select a File to Upload");
        try {
            //startActivityForResult(chooser, FILE_SELECT_CODE);
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), 101);
        } catch (Exception ex) {
            System.out.println("browseClick :" + ex);//android.content.ActivityNotFoundException ex
        }

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Fix no activity available
        if (data == null)
            return;
        switch (requestCode) {
            case 101:
                if (resultCode == RESULT_OK) {
//                  FilePath = data.getData().getPath();
                    len = 0;
                    count=0;
                    textProgressBar.setText(String.valueOf(len));
                    totalProgressBar.setText("0");
                    progressBar.setProgress(len);
                    FilePath = getPath(this, data.getData());
//                    }
                    //FilePath is your file as a string
                    txtphoneNo.setText(FilePath);
                    try {
                        File csvfile = new File(FilePath);
                        CSVReader reader = new CSVReader(new FileReader(csvfile.getAbsolutePath()));
                        String nextLine[];
                        SmsManager smsManager = SmsManager.getDefault();
                        int totalLen = 0;
                        while ((nextLine = reader.readNext()) != null) {
                            // nextLine[] is an array of values from the line
                            if (!nextLine[0].equalsIgnoreCase("Phone Number")) {
                                System.out.println(nextLine[0] + " " + nextLine[1]);
                                totalLen++;
                            }
                        }
                        totalProgressBar.setText(String.valueOf(totalLen));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "The specified file was not found " + e, Toast.LENGTH_LONG).show();
                    }
                }
        }
    }

    public void getPermissionToReadSMS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.READ_SMS)) {
                    Toast.makeText(this, "Please allow permission!", Toast.LENGTH_SHORT).show();
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_SMS},
                        READ_SMS_PERMISSIONS_REQUEST);
            }
        }
    }


    private void requestSmsPermission() {

        // check permission is given
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            // request permission (see result in onRequestPermissionsResult() method)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    PERMISSION_SEND_SMS);
        } else {
            // permission already granted run sms send
            requestPhoneStatePermission();
        }
    }

    private void requestPhoneStatePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
        } else {
            sendSMSMessage();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_SEND_SMS: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    sendSMSMessage();
                } else {
                    // permission denied
                }
                return;
            }
            case REQUEST_READ_EXTERNAL_STORAGE: {
                /*if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Permission denied to access your location.", Toast.LENGTH_SHORT).show();
                }*/
                return;
            }
            case REQUEST_READ_PHONE_STATE: {

                return;
            }
        }
    }

    public static String getPath(Context context, Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

}
