package com.binish.smstesting.Fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.binish.smstesting.Adapters.LogsAdapter;
import com.binish.smstesting.Database.DatabaseHandler;
import com.binish.smstesting.Models.LogsModel;
import com.binish.smstesting.R;

import java.util.ArrayList;

public class ViewLogs extends Fragment {
    DatabaseHandler databaseHandler;
    RecyclerView recyclerView;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_logs,container,false);

        databaseHandler = new DatabaseHandler(getActivity());

        this.setHasOptionsMenu(true);

        recyclerView = view.findViewById(R.id.logs_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(new LogsAdapter(getActivity(),databaseHandler.getCSV(),getFragmentManager()));

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        ArrayList<LogsModel> data = databaseHandler.getCSV();
        if(data.size()!=0)
            inflater.inflate(R.menu.view_logs_menu,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id==R.id.delete_all_logs){
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            alertDialog.setTitle("Delete all logs");
            alertDialog.setMessage("Are you sure you want to delete all logs ?");
            alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    databaseHandler.deleteAllDataFromCSVTable();
                    recyclerView.setAdapter(new LogsAdapter(getActivity(),databaseHandler.getCSV(),getFragmentManager()));
                    Toast.makeText(getActivity(),"All logs deleted",Toast.LENGTH_LONG).show();
                }
            });
            alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alertDialog.show();
        }
        return super.onOptionsItemSelected(item);
    }
}
