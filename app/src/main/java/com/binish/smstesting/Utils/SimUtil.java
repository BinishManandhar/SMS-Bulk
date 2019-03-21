package com.binish.smstesting.Utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.binish.smstesting.Models.SimInfo;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SimUtil {

    @SuppressLint("MissingPermission")
    public static boolean sendSMS(Context ctx, int simID, String toNum, String centerNum, String smsText, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        String name;
        try {
            if (simID == 0) {
                name = "isms";
                // for model : "Philips T939" name = "isms0"
            } else if (simID == 1) {
                name = "isms2";
            } else {
                throw new Exception("Cannot get service which for sim '" + simID + "', only 0,1 accepted as values");
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SubscriptionManager localSubscriptionManager = (SubscriptionManager) ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (localSubscriptionManager.getActiveSubscriptionInfoCount() > 1) {
                    if (simID <= 1) {
                        SubscriptionInfo simInfo1 = localSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(simID);
                        SmsManager.getSmsManagerForSubscriptionId(simInfo1.getSubscriptionId()).sendTextMessage(toNum, null, smsText, sentIntent, deliveryIntent);
                    } else {
                        throw new Exception("can not get service which for sim '" + simID + "', only 0,1 accepted as values");
                    }
                }
            } else {

                Method method = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
                method.setAccessible(true);
                Object param = method.invoke(null, name);

                method = Class.forName("com.android.internal.telephony.ISms$Stub").getDeclaredMethod("asInterface", IBinder.class);
                method.setAccessible(true);
                Object stubObj = method.invoke(null, param);
                method = stubObj.getClass().getMethod("sendText", String.class, String.class, String.class, String.class, PendingIntent.class, PendingIntent.class);
                method.invoke(stubObj, ctx.getPackageName(), toNum, centerNum, smsText, sentIntent, deliveryIntent);
            }
            return true;
        } catch (ClassNotFoundException e) {
            Log.e("apipas", "ClassNotFoundException:" + e.getMessage());
        } catch (NoSuchMethodException e) {
            Log.e("apipas", "NoSuchMethodException:" + e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e("apipas", "InvocationTargetException:" + e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e("apipas", "IllegalAccessException:" + e.getMessage());
        } catch (Exception e) {
            Log.e("apipas", "Exception:" + e.getMessage());
        }
        return false;
    }


    @SuppressLint("MissingPermission")
    public static boolean sendMultipartTextSMS(Context ctx, int simID, String toNum, String centerNum, ArrayList<String> smsTextlist, ArrayList<PendingIntent> sentIntentList, ArrayList<PendingIntent> deliveryIntentList) {
        String name;
        try {
            if (simID == 0) {
                name = "isms"; // for model : "Philips T939" name = "isms0"
            } else if (simID == 1) {
                name = "isms2";
            } else {
                throw new Exception("can not get service which for sim '" + simID + "', only 0,1 accepted as values");
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SubscriptionManager localSubscriptionManager = (SubscriptionManager) ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (localSubscriptionManager.getActiveSubscriptionInfoCount() > 1) {
                    if (simID <= 1) {
                        SubscriptionInfo simInfo1 = localSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(simID);
                        SmsManager.getSmsManagerForSubscriptionId(simInfo1.getSubscriptionId()).sendMultipartTextMessage(toNum, null, smsTextlist, sentIntentList, deliveryIntentList);
                    } else {
                        throw new Exception("can not get service which for sim '" + simID + "', only 0,1 accepted as values");
                    }
                }
            } else {
                Method method = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
                method.setAccessible(true);
                Object param = method.invoke(null, name);

                method = Class.forName("com.android.internal.telephony.ISms$Stub").getDeclaredMethod("asInterface", IBinder.class);
                method.setAccessible(true);
                Object stubObj = method.invoke(null, param);
                method = stubObj.getClass().getMethod("sendMultipartText", String.class, String.class, String.class, List.class, List.class, List.class);
                method.invoke(stubObj, ctx.getPackageName(), toNum, centerNum, smsTextlist, sentIntentList, deliveryIntentList);
            }
            return true;
        } catch (ClassNotFoundException e) {
            Log.e("apipas", "ClassNotFoundException:" + e.getMessage());
        } catch (NoSuchMethodException e) {
            Log.e("apipas", "NoSuchMethodException:" + e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e("apipas", "InvocationTargetException:" + e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e("apipas", "IllegalAccessException:" + e.getMessage());
        } catch (Exception e) {
            Log.e("apipas", "Exception:" + e.getMessage());
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    public static List<SimInfo> getSIMInfo(Context context) {
        List<SimInfo> simInfoList = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            int simNumbers = subscriptionManager.getActiveSubscriptionInfoCount();
            for(int i=0;i<simNumbers;i++) {
                SubscriptionInfo subInfo = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(i);
                SimInfo simInfo = new SimInfo(subInfo.getSubscriptionId(), String.valueOf(subInfo.getDisplayName()), subInfo.getIccId(), subInfo.getSimSlotIndex());
                simInfoList.add(simInfo);
            }
        }
        else {
            Uri URI_TELEPHONY = Uri.parse("content://telephony/siminfo/");
            Cursor c = context.getContentResolver().query(URI_TELEPHONY, null, null, null, null);
            if (c.moveToFirst()) {
                do {
                    int id = c.getInt(c.getColumnIndex("_id"));
                    int slot = c.getInt(c.getColumnIndex("sim_id"));
                    String display_name = c.getString(c.getColumnIndex("display_name"));
                    String icc_id = c.getString(c.getColumnIndex("icc_id"));
                    SimInfo simInfo = new SimInfo(id, display_name, icc_id, slot);
                    Log.d("apipas_sim_info", simInfo.toString());
                    simInfoList.add(simInfo);
                } while (c.moveToNext());
            }
            c.close();
        }

        return simInfoList;
    }

}