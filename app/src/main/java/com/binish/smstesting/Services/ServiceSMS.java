package com.binish.smstesting.Services;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.media.MediaBrowserCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.binish.smstesting.Database.DatabaseHandler;
import com.binish.smstesting.Navigation.SMSBulkNavigation;
import com.binish.smstesting.R;
import com.binish.smstesting.Utils.SimUtil;
import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class ServiceSMS extends Service {
    private static final int ONLY_ONE_SIM_AVAILABLE_IN_EITHER_SLOTS = 201;
    int notificationID = 100;
    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";

    PendingIntent sentPI;
    PendingIntent deliveredPI;
    BroadcastReceiver sendSMS;
    BroadcastReceiver deliverSMS;

    NotificationManager notificationManager;
    Notification.Builder notificationBuilder;
    Notification notification;

    DatabaseHandler databaseHandler;

    String FilePath;
    int len;
    int count;
    int totalCount;
    float dummy; //for converting number of sent messages to float if not exactly divisible
    public static int simID;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "SMS Service Running", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getExtras() != null && startId==2) {
            int notificationId = intent.getExtras().getInt("notificationID", 0);
            if (notificationId == 100) {
                this.stopSelf();
            }
        }
        else {
            try {
                registerService();
                databaseHandler = new DatabaseHandler(this);
                len = intent.getExtras().getInt("len");
                count = intent.getExtras().getInt("count");
                totalCount = intent.getExtras().getInt("totalCount");
                simID = intent.getExtras().getInt("simID");
                FilePath = intent.getExtras().getString("FilePath");
                sendSMSMessage();
            } catch (Exception e) {
                Log.i("Service", "Exception:" + e);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    protected void sendSMSMessage() {
        File csvfile = new File(FilePath);
        CSVReader reader;
        List<String[]> allData = null;
        totalCount = 0;
        try {
            reader = new CSVReader(new FileReader(csvfile.getAbsolutePath()));
            allData = reader.readAll();
            totalCount = allData.size();
        } catch (Exception e) {
            Toast.makeText(this, "Exception:" + e, Toast.LENGTH_LONG).show();
        }
        smsLoop(allData, totalCount);


        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //Set notification information:
        notificationBuilder = new Notification.Builder(getApplicationContext());
        notificationBuilder.setOngoing(true)
                .setContentTitle("Messages Sent")
                .setSmallIcon(R.drawable.ic_message_black_24dp)
                .setContentIntent(createOnContentIntent(this))
                .setProgress(100, 0, false);

        //Send the notification:
        notification = notificationBuilder.build();
        notificationManager.notify(notificationID, notification);

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
                sentPI = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                if (mSMSMessage.size() == 1) {
                    if (simID == ONLY_ONE_SIM_AVAILABLE_IN_EITHER_SLOTS) {
                        smsManager.sendTextMessage(nextLine[0], null, nextLine[1], sentPI, deliveredPI);
                    } else {
                        SimUtil.sendSMS(this, simID, nextLine[0], null, nextLine[1], sentPI, deliveredPI);
                    }
                } else if (mSMSMessage.size() > 1) {
//                    for (int i = 0; i < mSMSMessage.size(); i++) {
                    ArrayList<PendingIntent> sentPendingIntents = new ArrayList<>();
                    sentPendingIntents.add(sentPI);

                    ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<>();
                    deliveredPendingIntents.add(deliveredPI);
//                    }
                    if (simID == ONLY_ONE_SIM_AVAILABLE_IN_EITHER_SLOTS) {
                        smsManager.sendMultipartTextMessage(nextLine[0], null, mSMSMessage,
                                sentPendingIntents, deliveredPendingIntents);
                    } else {

                        SimUtil.sendMultipartTextSMS(this, simID, nextLine[0], null, mSMSMessage,
                                sentPendingIntents, deliveredPendingIntents);

                    }
                }

            }
            count++;
            if (count == totalCount)
                return;
            else {
                new CountDownTimer(1000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    public void onFinish() {
                        if ((count % 4) == 0) {
                            new CountDownTimer(4000, 1000) {

                                @Override
                                public void onTick(long millisUntilFinished) {
                                }

                                @Override
                                public void onFinish() {
                                    smsLoop(allData, totalCount);
                                }
                            }.start();
                        } else {
                            smsLoop(allData, totalCount);
                        }
                    }
                }.start();
            }

        } catch (Exception e) {
            Toast.makeText(this, "SMS Exception:" + e, Toast.LENGTH_LONG).show();
        }
    }

    protected void registerService() {
        //-------------------------------------------------------------------------//
        // STEP-1___
        // SEND PendingIntent
        /*sentPI = PendingIntent.getBroadcast(this, 0, new Intent(
                SENT), 0);*/

        // DELIVER PendingIntent
        deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        // STEP-2___
        // SEND BroadcastReceiver
        sendSMS = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                Log.i("extras", "Action:" + arg1.getAction() + " Number:" + arg1.getExtras().getString("number", "skipped"));
                switch (getResultCode()) {
                    case RESULT_OK:
                        len++;
                        if (len >= 0) {
                            dummy = (float) len / (totalCount - 1);
                            notificationBuilder.setProgress(100, Math.round(dummy * 100), false);
                            notificationBuilder.setContentText(String.valueOf(len) + " out of " + String.valueOf(totalCount - 1));
                            notification = notificationBuilder.build();
                            notificationManager.notify(notificationID, notification);
                        }
                        if (len == totalCount - 1) {
                            Toast.makeText(getApplicationContext(), "All SMS successfully sent",
                                    Toast.LENGTH_SHORT).show();
                            unregisterReceiver(sendSMS);
                            notificationBuilder.setOngoing(false)
                                    .setDeleteIntent(createOnDismissedIntent(getApplicationContext()));
                            notification = notificationBuilder.build();
                            notificationManager.notify(notificationID, notification);
                        }
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getApplicationContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getApplicationContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getApplicationContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getApplicationContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        // DELIVERY BroadcastReceiver
        deliverSMS = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case RESULT_OK:
                        Toast.makeText(getApplicationContext(), "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getApplicationContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        // STEP-3___
        // ---Notify when the SMS has been sent---
        this.registerReceiver(sendSMS, new IntentFilter(SENT));

        // ---Notify when the SMS has been delivered---
        this.registerReceiver(deliverSMS, new IntentFilter(DELIVERED));
        //-------------------------------------------------------------------------//
    }

    private PendingIntent createOnContentIntent(Context context) {
        Intent intent = new Intent(context, SMSBulkNavigation.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationID, intent, 0);

        return pendingIntent;
    }

    private PendingIntent createOnDismissedIntent(Context context) {
        Intent intent = new Intent(context, ServiceSMS.class);
        intent.putExtra("notificationID", notificationID);
        PendingIntent pendingIntent = PendingIntent.getService(context, notificationID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return pendingIntent;
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        try {
            unregisterReceiver(sendSMS);
        } catch (Exception e) {
            Log.i("Unregister", "Exception:" + e);
        }
        stopSelf();
    }
}
