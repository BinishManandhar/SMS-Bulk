package com.binish.smstesting.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import com.binish.smstesting.Fragments.LogsData;
import com.binish.smstesting.Models.LogsDataModel;
import com.binish.smstesting.Models.LogsModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "SMSBulk";
    private static final String TABLE_CSV = "CSV";
    private static final String TABLE_CSV_DATA = "CSV_DATA";
    private static final String CSV_id = "id";
    private static final String CSV_name = "Filename";
    private static final String CSV_date = "date";
    private static final String CSV_DATA_id = "id";
    private static final String CSV_DATA_unique_id = "unique_id";
    private static final String CSV_DATA_PhnNumber = "phone_number";
    private static final String CSV_DATA_message = "message";
    private static final String CSV_DATA_status = "status";

    SQLiteDatabase db;

    Date c;
    String formattedDate;
    Context context;

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;

        c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM-dd", Locale.UK);
        formattedDate = df.format(c);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableCsv = "CREATE TABLE IF NOT EXISTS " + TABLE_CSV + "("
                + CSV_id + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + CSV_name + " TEXT,"
                + CSV_date + " TEXT" + ")";
        db.execSQL(createTableCsv);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void createTableCSVData() {
        db = this.getWritableDatabase();
        String createTableCsvData = "CREATE TABLE IF NOT EXISTS " + TABLE_CSV_DATA + "("
                + CSV_DATA_unique_id + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + CSV_DATA_id + " INTEGER,"
                + CSV_DATA_PhnNumber + " TEXT,"
                + CSV_DATA_message + " TEXT,"
                + CSV_DATA_status + " TEXT" + ")";
        db.execSQL(createTableCsvData);
    }

    public int insertCSV(String FileName) {
        db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(CSV_name, FileName);
        contentValues.put(CSV_date, formattedDate);

        db.insert(TABLE_CSV, null, contentValues);

        db = this.getReadableDatabase();
        String read = "SELECT * FROM " + TABLE_CSV;
        Cursor c = db.rawQuery(read, null);
        c.moveToLast();
        db.close();
        return c.getInt(c.getColumnIndex(CSV_DATA_id));
    }

    public ArrayList<LogsModel> getCSV() {
        db = this.getReadableDatabase();
        String read = "SELECT * FROM " + TABLE_CSV;
        Cursor c = db.rawQuery(read, null);
        ArrayList<LogsModel> list = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                LogsModel logsModel = new LogsModel();
                logsModel.setLogName(c.getString(c.getColumnIndex(CSV_name)));
                logsModel.setLogDate(c.getString(c.getColumnIndex(CSV_date)));
                logsModel.setLogID(c.getInt(c.getColumnIndex(CSV_id)));
                list.add(logsModel);
            }
        } catch (Exception e) {
            list = null;
        }
        c.close();
        db.close();
        return list;
    }

    public void insertCSVData(int tableId, String phNumber, String message, String status) {
        db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(CSV_DATA_id, tableId);
        contentValues.put(CSV_DATA_PhnNumber, phNumber);
        contentValues.put(CSV_DATA_message, message);
        contentValues.put(CSV_DATA_status, status);

        db.insert(TABLE_CSV_DATA, null, contentValues);
        db.close();
    }

    public void insertCSVDataStatus(int tableId, String phNumber,String uniqueID, String status) {
        db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(CSV_DATA_status, status);

        Log.i("updateCSV","Table ID:"+tableId+" Phone Number:"+phNumber+" Status:"+status);

        db.update(TABLE_CSV_DATA, contentValues, CSV_DATA_id + "=" + tableId + " AND " + CSV_DATA_unique_id + "='" + uniqueID + "'", null);
        db.close();
    }

    public ArrayList<LogsDataModel> getCSVData(int logID) {
        db = this.getReadableDatabase();
        String read = "SELECT * FROM " + TABLE_CSV_DATA + " WHERE " + CSV_DATA_id + "=" + logID;
        Cursor c = db.rawQuery(read, null);
        ArrayList<LogsDataModel> list = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                LogsDataModel logsModel = new LogsDataModel();
                logsModel.setPhNumber(c.getString(c.getColumnIndex(CSV_DATA_PhnNumber)));
                logsModel.setUniqueID(c.getInt(c.getColumnIndex(CSV_DATA_unique_id)));
                logsModel.setMessage(c.getString(c.getColumnIndex(CSV_DATA_message)));
                logsModel.setMessageStatus(c.getString(c.getColumnIndex(CSV_DATA_status)));
                list.add(logsModel);
            }
        } catch (Exception e) {
            list = null;
        }
        c.close();
        db.close();
        return list;
    }

    public ArrayList<LogsDataModel> getFailedSMS(int logID) {
        db = this.getReadableDatabase();
        String read = "SELECT * FROM " + TABLE_CSV_DATA + " WHERE " + CSV_DATA_id + "=" + logID;
        Cursor c = db.rawQuery(read, null);
        ArrayList<LogsDataModel> list = new ArrayList<>();
        while (c.moveToNext()) {
            LogsDataModel logsModel = new LogsDataModel();
            String check = c.getString(c.getColumnIndex(CSV_DATA_status));
            if(check.equalsIgnoreCase("")) {
                logsModel.setPhNumber(c.getString(c.getColumnIndex(CSV_DATA_PhnNumber)));
                logsModel.setMessage(c.getString(c.getColumnIndex(CSV_DATA_message)));
                logsModel.setUniqueID(c.getInt(c.getColumnIndex(CSV_DATA_unique_id)));
                list.add(logsModel);
            }
        }
        c.close();

        db.close();
        return list;
    }

    public boolean checkExistingCSVTable(int logID){
        db = this.getReadableDatabase();

        String find = "SELECT * FROM "+TABLE_CSV+" WHERE "+CSV_id+"="+logID;
        Cursor c = db.rawQuery(find, null);
        c.moveToFirst();
        try {
            Log.i("Database Check", "Table ID: " + c.getString(c.getColumnIndex(CSV_id)));
        }
        catch (CursorIndexOutOfBoundsException e){
            return false;
        }
        c.close();
        return true;
    }

    public void deleteSpecificLog(int logID){
        db = this.getWritableDatabase();
        db.delete(TABLE_CSV,CSV_id + "=?",new String[]{String.valueOf(logID)});
        db.delete(TABLE_CSV_DATA,CSV_DATA_id + "=?",new String[]{String.valueOf(logID)});
        db.close();
    }

    public void deleteAllDataFromCSVTable(){
        db = this.getWritableDatabase();
        String del = "DROP TABLE "+TABLE_CSV;
        db.execSQL(del);
        del = "DROP TABLE "+TABLE_CSV_DATA;
        db.execSQL(del);
        onCreate(db);
        db.close();
    }
}
