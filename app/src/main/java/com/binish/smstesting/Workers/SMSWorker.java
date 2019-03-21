package com.binish.smstesting.Workers;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.binish.smstesting.Database.DatabaseHandler;
import com.binish.smstesting.Models.LogsDataModel;
import com.binish.smstesting.Navigation.SMSBulkNavigation;
import com.binish.smstesting.R;
import com.binish.smstesting.Utils.SimUtil;

import java.util.ArrayList;
import java.util.List;

import androidx.work.Worker;
import androidx.work.WorkerParameters;
import static com.binish.smstesting.Fragments.HomeScreen.len;

import static android.app.Activity.RESULT_OK;

public class SMSWorker extends Worker {
    Context context;
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
    String FileName;
//    int len;
    int count=0;
    int totalCount;
    int tableId;
    float dummy; //for converting number of sent messages to float if not exactly divisible
    public static int simID;
    public SMSWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        if(Looper.myLooper()!=null)
            Looper.myLooper().quitSafely();

        databaseHandler = new DatabaseHandler(context);

        if(getInputData().getInt("from",0)==0) {
            len = getInputData().getInt("len", 0);
//            totalCount = getInputData().getInt("totalCount", 0);
            simID = getInputData().getInt("simID", 0);
            tableId = getInputData().getInt("tableId", 0);
            FilePath = getInputData().getString("FilePath");
            FileName = getInputData().getString("FileName");
            initialNotificationBuild();
            registerService();
            sendSMSMessage();
        }
        else {
            simID = getInputData().getInt("simID", 0);
            int logID = getInputData().getInt("logID", 0);
            List<String[]> list = new ArrayList<>();
            ArrayList<LogsDataModel> data = databaseHandler.getFailedSMS(logID);

            for (LogsDataModel logsData: data){
                String[] nextLine = new String[]{logsData.getPhNumber(),logsData.getMessage(),String.valueOf(logsData.getUniqueID())};
                list.add(nextLine);
            }
            initialNotificationBuild();
            registerService();
            tableId=logID;
            totalCount=list.size()+1;
            smsLoop(list,totalCount);
        }

        return Result.success();
    }

    protected void sendSMSMessage() {
        /*File csvfile = new File(FilePath);
        CSVReader reader;
        List<String[]> allData = null;
        totalCount = 0;
        try {
            reader = new CSVReader(new FileReader(csvfile.getAbsolutePath()));
            allData = reader.readAll();
            totalCount = allData.size();
        } catch (Exception e) {
            Toast.makeText(context, "Exception:" + e, Toast.LENGTH_LONG).show();
        }*/

        List<String[]> list = new ArrayList<>();
        ArrayList<LogsDataModel> data = databaseHandler.getFailedSMS(tableId);

        for (LogsDataModel logsData: data){
            String[] nextLine = new String[]{logsData.getPhNumber(),logsData.getMessage(),String.valueOf(logsData.getUniqueID())};
            list.add(nextLine);
        }
        totalCount=list.size()+1;
        smsLoop(list, totalCount);



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
                intent.putExtra("logID",tableId);
                if(nextLine[2]!=null){intent.putExtra("uniqueID", nextLine[2]);}
                sentPI = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                if (mSMSMessage.size() == 1) {
                    if (simID == ONLY_ONE_SIM_AVAILABLE_IN_EITHER_SLOTS) {
                        smsManager.sendTextMessage(nextLine[0], null, nextLine[1], sentPI, deliveredPI);
                    } else {
                        SimUtil.sendSMS(context, simID, nextLine[0], null, nextLine[1], sentPI, deliveredPI);
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

                        SimUtil.sendMultipartTextSMS(context, simID, nextLine[0], null, mSMSMessage,
                                sentPendingIntents, deliveredPendingIntents);

                    }
                }
            }
            count++;
            if (count == totalCount-1){count=0;}
            else {
                HandlerThread handlerThread = new HandlerThread("SMSBulk");
                handlerThread.start();
                Handler handler = new Handler(handlerThread.getLooper());
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        smsLoop(allData,totalCount);
                    }
                };
                handler.postDelayed(runnable, 3000);
            }

        } catch (Exception e) {
            Log.i("ExceptionLooper","Exception:"+e);
        }
    }


    protected void registerService() {
        //-------------------------------------------------------------------------//
        // STEP-1___
        // SEND PendingIntent
        /*sentPI = PendingIntent.getBroadcast(context, 0, new Intent(
                SENT), 0);*/

        // DELIVER PendingIntent
        deliveredPI = PendingIntent.getBroadcast(context, 0,
                new Intent(DELIVERED), 0);

        // STEP-2___
        // SEND BroadcastReceiver
        sendSMS = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case RESULT_OK:
                        len++;
                        databaseHandler.insertCSVDataStatus(tableId,arg1.getExtras().getString("number"),arg1.getExtras().getString("uniqueID"),"SENT");
                        if (len >= 0) {
                            dummy = (float) len / (totalCount - 1);
                            notificationBuilder.setProgress(100, Math.round(dummy * 100), false);
                            notificationBuilder.setContentText(String.valueOf(len) + " out of " + String.valueOf(totalCount - 1));
                            notification = notificationBuilder.build();
                            notificationManager.notify(notificationID, notification);
                        }
                        if (len == totalCount - 1) {
                            Toast.makeText(context, "All SMS successfully sent",
                                    Toast.LENGTH_SHORT).show();
                            context.unregisterReceiver(sendSMS);
                            notificationBuilder.setOngoing(false);
//                                    .setDeleteIntent(createOnDismissedIntent(context));
                            notification = notificationBuilder.build();
                            notificationManager.notify(notificationID, notification);
                        }
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(context, "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(context, "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(context, "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(context, "Radio off",
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
                        Toast.makeText(context, "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(context, "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        // STEP-3___
        // ---Notify when the SMS has been sent---
        context.registerReceiver(sendSMS, new IntentFilter(SENT));

        // ---Notify when the SMS has been delivered---
        context.registerReceiver(deliverSMS, new IntentFilter(DELIVERED));
        //-------------------------------------------------------------------------//
    }

    private PendingIntent createOnContentIntent(Context context) {
        Intent intent = new Intent(context, SMSBulkNavigation.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationID, intent, 0);

        return pendingIntent;
    }

//    private PendingIntent createOnDismissedIntent(Context context) {
//        Intent intent = new Intent(context, ServiceSMS.class);
//        intent.putExtra("notificationID", notificationID);
//        PendingIntent pendingIntent = PendingIntent.getService(context, notificationID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        return pendingIntent;
//    }

    protected void initialNotificationBuild(){
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        //Set notification information:
        notificationBuilder = new Notification.Builder(context);
        notificationBuilder.setOngoing(false)
                .setContentTitle("Messages Sent")
                .setSmallIcon(R.drawable.ic_message_black_24dp)
                .setContentIntent(createOnContentIntent(context))
                .setProgress(100, 0, true);

        //Send the notification:
        notification = notificationBuilder.build();
        notificationManager.notify(notificationID, notification);
    }

}
