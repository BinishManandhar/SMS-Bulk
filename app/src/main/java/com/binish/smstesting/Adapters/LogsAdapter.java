package com.binish.smstesting.Adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.binish.smstesting.Database.DatabaseHandler;
import com.binish.smstesting.Fragments.LogsData;
import com.binish.smstesting.Models.LogsModel;
import com.binish.smstesting.R;
import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;

import java.util.ArrayList;
import java.util.List;

public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.ViewLogs> {
    ArrayList<LogsModel> list;
    Context context;
    FragmentManager fragmentManager;
    private final ViewBinderHelper binderHelper = new ViewBinderHelper();
    public LogsAdapter(Context context,ArrayList<LogsModel>data,FragmentManager fragmentManager){
        list = data;
        this.context = context;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public ViewLogs onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewLogs(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.design_swipe_view_logs,viewGroup,false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewLogs viewLogs,int position) {
        final LogsModel logsModel = list.get(position);

        binderHelper.bind(viewLogs.swipeLayout, String.valueOf(logsModel.getLogID()));

        viewLogs.logs_name.setText(logsModel.getLogName());
        viewLogs.logs_date.setText(logsModel.getLogDate());

        viewLogs.logs_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogsData logsData = new LogsData();
                Bundle bundle = new Bundle();
                bundle.putInt("logID",logsModel.getLogID());
                logsData.setArguments(bundle);
                fragmentManager.beginTransaction().replace(R.id.fragmentScreen,logsData,"LOGS_DATA").addToBackStack(null).commit();
            }
        });

        viewLogs.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                alert.setTitle("Delete this log");
                alert.setMessage("Are you sure you want to delete "+logsModel.getLogName()+"?");
                alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        DatabaseHandler databaseHandler = new DatabaseHandler(context);
                        databaseHandler.deleteSpecificLog(logsModel.getLogID());
//                        viewLogs.logs_linear.setVisibility(View.GONE);
                        list.remove(viewLogs.getAdapterPosition());
                        notifyItemRemoved(viewLogs.getAdapterPosition());
                    }
                });
                alert.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                alert.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        if(list!=null)
            return list.size();
        else
            return 0;
    }

    public class ViewLogs extends RecyclerView.ViewHolder {
        TextView logs_name;
        TextView logs_date;
        LinearLayout logs_linear;
        SwipeRevealLayout swipeLayout;
        ImageView deleteButton;
        ViewLogs(@NonNull View itemView) {
            super(itemView);
            logs_name = itemView.findViewById(R.id.logs_name);
            logs_date = itemView.findViewById(R.id.logs_date);
            logs_linear = itemView.findViewById(R.id.viewLogsLinear);
            swipeLayout = itemView.findViewById(R.id.swipe_layout);
            deleteButton = itemView.findViewById(R.id.view_logs_delete);
        }
    }

    /*@NonNull
    @Override
    public View getView(int position, @NonNull View convertView, @NonNull ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.design_view_logs, null);
        TextView logs_name = view.findViewById(R.id.logs_name);
        TextView logs_date = view.findViewById(R.id.logs_date);
        LogsModel logsModel = getItem(position);
        logs_name.setText(logsModel.getLogName());
        logs_date.setText(logsModel.getLogDate());
        return view;
    }*/

}
