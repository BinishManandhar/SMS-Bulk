package com.binish.smstesting.Adapters;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.binish.smstesting.Database.DatabaseHandler;
import com.binish.smstesting.Models.LogsDataModel;
import com.binish.smstesting.R;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.binish.smstesting.Fragments.HomeScreen.len;

public class LogsDataAdapter extends RecyclerView.Adapter<LogsDataAdapter.ViewHolder> {
    String SENT = "SMS_SENT";
    Context context;
    ArrayList<LogsDataModel> list;
    BroadcastReceiver sendSMS;
    FragmentManager fragmentManager;
    DatabaseHandler databaseHandler;
//    int len=0;

    public LogsDataAdapter(Context context, ArrayList<LogsDataModel> list, FragmentManager fragmentManager){
        this.list=list;
        this.context=context;
        this.fragmentManager = fragmentManager;
        databaseHandler = new DatabaseHandler(context);
        registerService();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.design_logs_data,viewGroup,false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        LogsDataModel logsDataModel = list.get(position);
        viewHolder.snNumber.setText(String.valueOf(position+1));
        viewHolder.phNumber.setText(logsDataModel.getPhNumber());
        if(logsDataModel.getMessageStatus().equalsIgnoreCase("SENT")) {
            viewHolder.status.setImageResource(R.drawable.ic_check_black_24dp);
        }
        else {
            viewHolder.status.setImageResource(R.drawable.ic_close_black_24dp);
        }
    }

    @Override
    public int getItemCount() {
        if(list!=null)
            return list.size();
        else
            return 0;
    }

    protected class ViewHolder extends RecyclerView.ViewHolder{
        TextView snNumber;
        TextView phNumber;
        ImageView status;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
             snNumber = itemView.findViewById(R.id.logs_data_snNumber);
             phNumber = itemView.findViewById(R.id.logs_data_number);
             status = itemView.findViewById(R.id.logs_data_status);
        }
    }

    private void registerService(){
        sendSMS = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case RESULT_OK:
                        final int logID = arg1.getExtras().getInt("logID",0);
                        assert fragmentManager != null;
                        Fragment myFragment = fragmentManager.findFragmentByTag("LOGS_DATA");
                        if(myFragment!=null && myFragment.isVisible()) {
//                            len++;
                            Handler handler = new Handler();
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    refreshingMessageStatus(logID);
                                }
                            };
                            handler.postDelayed(runnable, 500);
                            if(len==list.size()){
                                notifyDataSetChanged();
                                context.unregisterReceiver(sendSMS);
                            }
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
        context.registerReceiver(sendSMS, new IntentFilter(SENT));
    }

    public void refreshingMessageStatus(int logID) {
        final ArrayList<LogsDataModel> newData = databaseHandler.getCSVData(logID);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return list.size();
            }

            @Override
            public int getNewListSize() {
                return newData.size();
            }

            @Override
            public boolean areItemsTheSame(int i, int i1) {
                return (list.get(i).getUniqueID() == newData.get(i1).getUniqueID());
            }

            @Override
            public boolean areContentsTheSame(int i, int i1) {
                LogsDataModel oldLogs = list.get(i);
                LogsDataModel newLogs = newData.get(i);
                return (oldLogs.getMessageStatus().equals(newLogs.getMessageStatus()));
            }

            @Nullable
            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {
                LogsDataModel oldLogs = list.get(oldItemPosition);
                LogsDataModel newLogs = newData.get(newItemPosition);
                Bundle bundle = new Bundle();
                if(!oldLogs.getMessageStatus().equals(newLogs.getMessageStatus())){
                    list.remove(oldItemPosition);
                    list.add(oldItemPosition,newLogs);
                    bundle.putString("status",newLogs.getMessageStatus());
                }
                return bundle;
            }
        });
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            Bundle bundle = (Bundle) payloads.get(0);
            if (bundle.size() != 0) {
                String status = bundle.getString("status");
                if(status!=null && status.equalsIgnoreCase("SENT")) {
                    holder.status.setImageResource(R.drawable.ic_check_black_24dp);
                }
                else {
                    holder.status.setImageResource(R.drawable.ic_close_black_24dp);
                }
            }
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        context.unregisterReceiver(sendSMS);
    }
}
