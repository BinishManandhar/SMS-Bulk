package com.binish.smstesting.Fragments;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.binish.smstesting.Adapters.LogsDataAdapter;
import com.binish.smstesting.Database.DatabaseHandler;
import com.binish.smstesting.Models.SimInfo;
import com.binish.smstesting.R;
import com.binish.smstesting.Services.ServiceSMS;
import com.binish.smstesting.Utils.SimUtil;
import com.binish.smstesting.Workers.SMSWorker;
import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;
import static android.database.sqlite.SQLiteDatabase.openOrCreateDatabase;

public class HomeScreen extends Fragment {
    private static final int REQUEST_READ_PHONE_STATE = 103;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int PERMISSION_SEND_SMS = 123;
    private static final int READ_SMS_PERMISSIONS_REQUEST = 11;
    private static final int ONLY_ONE_SIM_AVAILABLE_IN_EITHER_SLOTS = 201;
    ImageButton sendBtn;
    EditText txtphoneNo;
    Button browseButton;
    String phoneNo;
    String FilePath;
    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";
    String FileName;
    TextView textProgressBar;
    TextView totalProgressBar;
    TextView totalMessageSent;
    ProgressBar progressBar;
    ArrayList<PendingIntent> sentPendingIntents;
    ArrayList<PendingIntent> deliveredPendingIntents;
    PendingIntent sentPI;
    PendingIntent deliveredPI;
    BroadcastReceiver sendSMS;
    BroadcastReceiver deliverSMS;
    Intent serviceIntent = null;
    DatabaseHandler databaseHandler;
    List<SimInfo> simInfos;
    public static int len = 0;
    int count = 0;
    int totalCount = 0;
    int messageToBeSent = 0;
    int tableId;
    float dummy; //for converting number of sent messages to float if not exactly divisible
    public static int simID = 0;

    View view;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_home_screen, container, false);
            sendBtn = view.findViewById(R.id.btnSendSMS);
            txtphoneNo = view.findViewById(R.id.editText);
            browseButton = view.findViewById(R.id.browseBtn);
            textProgressBar = view.findViewById(R.id.textProgressBar);
            totalProgressBar = view.findViewById(R.id.totalProgressBar);
            progressBar = view.findViewById(R.id.progressBar);
            totalMessageSent = view.findViewById(R.id.totalMessageSent);
            phoneNo = txtphoneNo.getText().toString();

            requestPhoneStatePermission();
            databaseHandler = new DatabaseHandler(getActivity());
            registerService();

            sendBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    if (txtphoneNo.getText() == null || txtphoneNo.getText().toString().equals("")) {
                        Toast.makeText(getActivity(), "No File Selected", Toast.LENGTH_LONG).show();
                        return;
                    }
                    len = 0;
                    count = 0;
                    textProgressBar.setText(String.valueOf(len));
                    progressBar.setProgress(len);
                    sentPendingIntents = new ArrayList<>();
                    deliveredPendingIntents = new ArrayList<>();

                    if (!databaseHandler.checkExistingCSVTable(tableId)) {
                        len = 0;
                        count = 0;
                        messageToBeSent = 0;
                        textProgressBar.setText(String.valueOf(len));
                        totalProgressBar.setText("0");
                        progressBar.setProgress(len);
                        insertCSVDataFile("FROM_SEND");
                    }

                    databaseHandler.createTableCSVData(); //creating the table for saving logs


                    if (serviceIntent != null) {
                        getActivity().stopService(serviceIntent);
                    }

                    simInfos = SimUtil.getSIMInfo(getActivity());
                    int simCount = 0;
                    for (int i = 0; i < simInfos.size(); i++) {
                        simCount++;
                    }

                    if (simCount == 1) {
                        simID = ONLY_ONE_SIM_AVAILABLE_IN_EITHER_SLOTS;
                        requestSmsPermission();
                    } else if (simCount == 0) {
                        Toast.makeText(getActivity(), "No SIM card detected", Toast.LENGTH_LONG).show();
                    } else {
                        final Dialog dialog = new Dialog(getActivity());
                        dialog.setContentView(R.layout.dialog_sim_select);
                        dialog.setTitle("Select SIM");

                        TextView sim1 = dialog.findViewById(R.id.sim1);
                        TextView sim2 = dialog.findViewById(R.id.sim2);
                        sim1.setText(simInfos.get(0).getDisplay_name());
                        sim2.setText(simInfos.get(1).getDisplay_name());

                        sim1.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                simID = simInfos.get(0).getSlot();
                                requestSmsPermission();
                                dialog.dismiss();
                            }
                        });
                        sim2.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                simID = simInfos.get(1).getSlot();
                                requestSmsPermission();
                                dialog.dismiss();
                            }
                        });
                        dialog.show();
                    }

                }
            });
            browseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getCSV();
                }
            });
        } else {
            try {
                if (len+1 == totalCount - 1) {
                    progressBar = view.findViewById(R.id.progressBar);
                    progressBar.setProgress(100);
                }
            } catch (Exception e) {
            }
        }

        return view;
    }


    protected void sendSMSMessage() {

        File csvfile = new File(FilePath);
        CSVReader reader;
        List<String[]> allData = null;
        totalCount = 0;
        try {
            reader = new CSVReader(new FileReader(csvfile.getAbsolutePath()));
            allData = reader.readAll();
//            totalCount = allData.size();
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Exception:" + e, Toast.LENGTH_LONG).show();
        }
        smsLoop(allData, totalCount);

    }

    public void smsLoop(final List<String[]> allData, final int totalCount) {
        String[] nextLine;
        SmsManager smsManager = SmsManager.getDefault();

        try {
            nextLine = allData.get(count);
            if (!nextLine[0].equalsIgnoreCase("Phone Number")) {
                ArrayList<String> mSMSMessage = smsManager.divideMessage(nextLine[1]);
                Intent intent = new Intent(SENT);
                intent.putExtra("number", nextLine[0]);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                sentPI = PendingIntent.getBroadcast(getActivity(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                if (mSMSMessage.size() == 1) {
                    if (simID == ONLY_ONE_SIM_AVAILABLE_IN_EITHER_SLOTS) {
                        smsManager.sendTextMessage(nextLine[0], null, nextLine[1], sentPI, deliveredPI);
                    } else {
                        SimUtil.sendSMS(getActivity(), simID, nextLine[0], null, nextLine[1], sentPI, deliveredPI);
                    }
//                    Log.i("checking", "SMS length:" + mSMSMessage.size());
                } else if (mSMSMessage.size() > 1) {
                    for (int i = 0; i < mSMSMessage.size(); i++) {
                        sentPendingIntents.add(i, sentPI);

                        deliveredPendingIntents.add(i, deliveredPI);
                    }
                    if (simID == ONLY_ONE_SIM_AVAILABLE_IN_EITHER_SLOTS) {
                        smsManager.sendMultipartTextMessage(nextLine[0], null, mSMSMessage,
                                sentPendingIntents, deliveredPendingIntents);
                        len = len - (mSMSMessage.size() - 1);
                    } else {

                        SimUtil.sendMultipartTextSMS(getActivity(), simID, nextLine[0], null, mSMSMessage,
                                sentPendingIntents, deliveredPendingIntents);
                        len = len - (mSMSMessage.size() - 1);


                    }
//                    Log.i("checking", "SMS length:" + mSMSMessage.size());
                }

            }
            count++;
            if (count == totalCount)
                return;
            else if ((count % 1) == 0) {
                new CountDownTimer(1000, 1000) {

                    public void onTick(long millisUntilFinished) {

                    }

                    public void onFinish() {
                        smsLoop(allData, totalCount);
                    }
                }.start();
            } else {
                smsLoop(allData, totalCount);
            }

        } catch (Exception e) {
            Toast.makeText(getActivity(), "SMS Exception:" + e, Toast.LENGTH_LONG).show();
        }
    }

    public void getCSV() {
        if (Build.VERSION.SDK_INT > 22) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/csv");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            //startActivityForResult(chooser, FILE_SELECT_CODE);
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), 101);
        } catch (Exception ex) {
            System.out.println("browseClick :" + ex);//android.content.ActivityNotFoundException ex
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Fix no activity available
        if (data == null)
            return;
        switch (requestCode) {
            case 101:
                if (resultCode == RESULT_OK) {
//                  FilePath = data.getData().getPath();
                    len = 0;
                    count = 0;
                    messageToBeSent = 0;
                    textProgressBar.setText(String.valueOf(len));
                    totalProgressBar.setText("0");
                    progressBar.setProgress(len);
                    FilePath = getPath(getActivity(), data.getData());
//                    }
                    //FilePath is your file as a string
                    txtphoneNo.setText(FilePath);

                    insertCSVDataFile("FROM_RESULT");

                }
        }
    }

    public void getPermissionToReadSMS() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.READ_SMS)) {
                    Toast.makeText(getActivity(), "Please allow permission!", Toast.LENGTH_SHORT).show();
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_SMS},
                        READ_SMS_PERMISSIONS_REQUEST);
            }
        } else {
//            callForService();
            WorkManager.getInstance().enqueue(callForWorker());
        }
    }


    private void requestSmsPermission() {

        // check permission is given
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            // request permission (see result in onRequestPermissionsResult() method)
            requestPermissions(new String[]{Manifest.permission.SEND_SMS},
                    PERMISSION_SEND_SMS);
        } else {
            // permission already granted run sms send
            getPermissionToReadSMS();
        }
    }

    private void requestPhoneStatePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_PHONE_STATE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
        } else {

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_SEND_SMS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    getPermissionToReadSMS();
                } else {
                    // permission denied
                }
                return;
            }
            case REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                } else {
                    // permission denied
                }
                return;
            }
            case REQUEST_READ_PHONE_STATE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                } else {
                    // permission denied
                }
                return;
            }
            case READ_SMS_PERMISSIONS_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
//                    callForService();

                    WorkManager.getInstance().enqueue(callForWorker());
                } else {
                    // permission denied
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

    protected void registerService() {
        //-------------------------------------------------------------------------//
        // STEP-1___
        // SEND PendingIntent
        /*sentPI = PendingIntent.getBroadcast(getActivity(), 0, new Intent(
                SENT), 0);*/

        // STEP-2___
        // SEND BroadcastReceiver
        sendSMS = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case RESULT_OK:

                        if (len >= 0 && arg1.getExtras().getInt("logID", 0) == tableId) {
                            dummy = (float) (len+1) / (totalCount - 1);
                            progressBar.setProgress(Math.round(dummy * 100));
                            textProgressBar.setText(String.valueOf(len + 1));
                        }
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                    default:

                }
            }
        };
        // STEP-3___
        // ---Notify when the SMS has been sent---
        getActivity().registerReceiver(sendSMS, new IntentFilter(SENT));

        //-------------------------------------------------------------------------//
    }

    protected OneTimeWorkRequest callForWorker() {
        Data data = new Data.Builder()
                .putInt("simID", simID)
                .putInt("len", len)
                .putInt("totalCount", totalCount)
                .putInt("tableId", tableId)
                .putString("FilePath", FilePath)
                .putString("FileName", FileName)
                .build();
        return new OneTimeWorkRequest.Builder(SMSWorker.class).setInputData(data).build();
    }

    protected void insertCSVDataFile(String where) {
        SmsManager smsManager = SmsManager.getDefault();
        File csvfile = new File(FilePath);
        try {
            CSVReader reader = new CSVReader(new FileReader(csvfile.getAbsolutePath()));
            String nextLine[];
            int totalLen = 0;

            FileName = FilePath.substring(FilePath.lastIndexOf("/") + 1);
            tableId = databaseHandler.insertCSV(FileName);

            databaseHandler.createTableCSVData();

            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                if (!nextLine[0].equalsIgnoreCase("Phone Number")) {
                    System.out.println(nextLine[0] + " " + nextLine[1]);
                    totalLen++;
                    ArrayList<String> dummyMultiMessage = smsManager.divideMessage(nextLine[1]);
                    messageToBeSent = messageToBeSent + dummyMultiMessage.size();

                    databaseHandler.insertCSVData(tableId, nextLine[0], nextLine[1], "");
                }
            }
            reader = new CSVReader(new FileReader(csvfile.getAbsolutePath()));
            totalCount = reader.readAll().size();
            totalProgressBar.setText(String.valueOf(totalLen));
            totalMessageSent.setText(String.valueOf(messageToBeSent));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "The specified file was not found " + e, Toast.LENGTH_LONG).show();
        }
    }

}