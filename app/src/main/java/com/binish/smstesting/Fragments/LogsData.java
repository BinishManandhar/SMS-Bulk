package com.binish.smstesting.Fragments;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.binish.smstesting.Adapters.LogsDataAdapter;
import com.binish.smstesting.Database.DatabaseHandler;
import com.binish.smstesting.Models.LogsDataModel;
import com.binish.smstesting.Models.SimInfo;
import com.binish.smstesting.R;
import com.binish.smstesting.Utils.SimUtil;
import com.binish.smstesting.Workers.SMSWorker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class LogsData extends Fragment {
    public static int FROM_LOGS_DATA=677;
    DatabaseHandler databaseHandler;
    RecyclerView recyclerView;
    int logID;
    int simID=0;
    private static final int ONLY_ONE_SIM_AVAILABLE_IN_EITHER_SLOTS = 201;
    private static final int PERMISSION_SEND_SMS = 123;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view;
        view = inflater.inflate(R.layout.fragment_logs_data,container,false);

        logID = getArguments().getInt("logID");
        this.setHasOptionsMenu(true);

        databaseHandler = new DatabaseHandler(getActivity());

        recyclerView = view.findViewById(R.id.logs_data_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(new LogsDataAdapter(getActivity(),databaseHandler.getCSVData(logID),getFragmentManager()));
        return view;

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        ArrayList<LogsDataModel> data = databaseHandler.getFailedSMS(logID);
        if(data.size()!=0)
            inflater.inflate(R.menu.smsbulk_navigation,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.sendAgain) {
            requestSmsPermission();
        }
        return super.onOptionsItemSelected(item);
    }

    protected OneTimeWorkRequest callForWorker(){
        Data data = new Data.Builder()
                .putInt("from",FROM_LOGS_DATA)
                .putInt("logID",logID)
                .putInt("simID",simID)
                .build();
        return new OneTimeWorkRequest.Builder(SMSWorker.class).setInputData(data).build();
    }

    protected void forSIM(){
        final List<SimInfo> simInfos = SimUtil.getSIMInfo(getActivity());
        int simCount = 0;

        for (int i = 0; i < simInfos.size(); i++) {
            simCount++;
        }
        if (simCount == 1) {
            simID = ONLY_ONE_SIM_AVAILABLE_IN_EITHER_SLOTS;
            WorkManager.getInstance().enqueue(callForWorker());
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
                    dialog.dismiss();
                    WorkManager.getInstance().enqueue(callForWorker());
                }
            });
            sim2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    simID = simInfos.get(1).getSlot();
                    dialog.dismiss();
                    WorkManager.getInstance().enqueue(callForWorker());
                }
            });
            dialog.show();
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
            forSIM();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_SEND_SMS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    forSIM();
                } else {
                    // permission denied
                    Toast.makeText(getActivity(),"Permission needed to further continue",Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }
}
