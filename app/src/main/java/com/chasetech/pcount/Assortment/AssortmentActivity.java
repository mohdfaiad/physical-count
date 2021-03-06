package com.chasetech.pcount.Assortment;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.chasetech.pcount.BuildConfig;
import com.chasetech.pcount.CaptureSignatureActivity;
import com.chasetech.pcount.MKL.PCount;
import com.chasetech.pcount.MainActivity;
import com.chasetech.pcount.R;
import com.chasetech.pcount.Woosim.WoosimPrinter;
import com.chasetech.pcount.adapter.ReportListViewAdapter;
import com.chasetech.pcount.database.SQLLib;
import com.chasetech.pcount.database.SQLiteHelper;
import com.chasetech.pcount.TSC.BPrinter;
import com.chasetech.pcount.library.MainLibrary;
import com.chasetech.pcount.library.ReportClass;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class AssortmentActivity extends AppCompatActivity {

    private ProgressDialog pDL;
    private AlertDialog mAlertDialog;
    private SQLLib db;

    private final String TAG = "DEBUGGING";

    private ArrayList<Assortment> arrAssortment = new ArrayList<>();
    private ArrayList<Assortment> arrAssortmentAll = new ArrayList<>();
    private HashMap<String, Assortment> hmAssortment = new HashMap<>();

    private ListView lvwAssortment = null;

    private AssortmentAdapter mAssortmentAdapter;

    private Boolean mReupdatePCount = true;

    private EditText editTextSearch = null;

    private BPrinter Printer;
    //    private Boolean lupdate;
    private Boolean lprintall = false;
    private Boolean lprintwithso = false;

    public double len  = 0;
    public int numItems = 0;

    String selectedPrinter = "";
    WoosimPrinter woosimPrinter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.assortment_activity);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        selectedPrinter = prefs.getString("printer_list", "2");

        if(selectedPrinter.equals("1")) {
            MainLibrary.mSelectedPrinter = MainLibrary.PRINTER.WOOSIM;
        }
        else MainLibrary.mSelectedPrinter = MainLibrary.PRINTER.TSC;

        final TextView lblfso = (TextView) findViewById(R.id.lblfso);
        String fsolbl = MainLibrary.gStrCurrentUserName.substring(3,6) + " Unit";
        lblfso.setText(fsolbl);

        db = new SQLLib(AssortmentActivity.this);
        db.open();

        Printer = new BPrinter(this);

/*        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(false);
        getActionBar().setTitle(MainLibrary.gCurrentBranchNameSelected);
        if(MainLibrary.isAssortmentMode) getActionBar().setTitle(MainLibrary.gCurrentBranchNameSelected + " - " + " ASSORTMENT");*/
//        lupdate = getIntent().getExtras().getBoolean("lupdate");

        getSupportActionBar().setTitle(MainLibrary.gSelectedLocation.locationName + " - ASSORTMENT");

        new TaskProcessData().execute();

        editTextSearch = (EditText) findViewById(R.id.enter_search);
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String search = editTextSearch.getText().toString();
                mAssortmentAdapter.filter(99, search);
            }
        });

        lvwAssortment = (ListView) findViewById(R.id.lvwAssortment);
        mAssortmentAdapter = new AssortmentAdapter(AssortmentActivity.this, arrAssortment);
        lvwAssortment.setAdapter(mAssortmentAdapter);

        lvwAssortment.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final AssortmentViewHolder viewHolder = (AssortmentViewHolder) view.getTag();

                final Assortment assortment = viewHolder.assortment;

                final Dialog dialog = new Dialog(AssortmentActivity.this, R.style.Transparent);
                dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(true);
                dialog.setContentView(R.layout.activity_sku_details);

                final TextView textViewDesc = (TextView) dialog.findViewById(R.id.textViewDesc);
                final EditText editTextPcs = (EditText) dialog.findViewById(R.id.pcs);
                final EditText editTextWhPcs = (EditText) dialog.findViewById(R.id.whpcs);
                final EditText editTextWhCs = (EditText) dialog.findViewById(R.id.whcs);
                final Button btnQty = (Button) dialog.findViewById(R.id.btnQtyOk);

                textViewDesc.setText(assortment.desc);
                editTextPcs.setText("");
                editTextWhPcs.setText("");
                editTextWhCs.setText("");

                if (assortment.sapc != 0 || assortment.whpc != 0 || assortment.whcs !=0 ) {
                    editTextPcs.setText(String.valueOf(assortment.sapc));
                    editTextWhPcs.setText(String.valueOf(assortment.whpc));
                    editTextWhCs.setText(String.valueOf(assortment.whcs));
                }

                btnQty.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();

                        String inputPcs = editTextPcs.getText().toString();
                        String inputWhPcs = editTextWhPcs.getText().toString();
                        String inputWhcs = editTextWhCs.getText().toString();

/*                        if (inputPcs.isEmpty() && inputWhPcs.isEmpty() && inputWhcs.isEmpty()) {
                            return;
                        }*/

                        if (inputPcs.isEmpty()) {
                            inputPcs = "0";
                        }
                        if (inputWhPcs.isEmpty()) {
                            inputWhPcs = "0";
                        }
                        if (inputWhcs.isEmpty()) {
                            inputWhcs = "0";
                        }

                        int so = assortment.ig - Integer.parseInt(inputPcs) - Integer.parseInt(inputWhPcs) - (Integer.parseInt(inputWhcs) * assortment.conversion);;

                        int fso = 0;

                        if ((so % assortment.multi) == 0) {
                            fso = so;
                        }
                        else{
                            fso = so - (so % assortment.multi) + assortment.multi;
                        }

                        if (so <= 0) {    //10/27 for negative values
                            so = 0;
                            fso = 0;
                        }

                        assortment.sapc = Integer.parseInt(inputPcs);
                        assortment.whpc = Integer.parseInt(inputWhPcs);
                        assortment.whcs = Integer.parseInt(inputWhcs);
                        assortment.so = so;
                        assortment.fso = fso;
                        assortment.updated = true;

                        for (Assortment assortmentall : arrAssortmentAll) {
                            if(assortmentall.id == assortment.id) {
                                assortmentall.sapc = Integer.parseInt(inputPcs);
                                assortmentall.whpc = Integer.parseInt(inputWhPcs);
                                assortmentall.whcs = Integer.parseInt(inputWhcs);
                                assortmentall.so = so;
                                assortmentall.fso = fso;
                                assortmentall.updated = true;
                                break;
                            }
                        }

                        mAssortmentAdapter.notifyDataSetChanged();

                        new TaskSaveData().execute();
                    }
                });

                dialog.show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(MainLibrary.mSelectedPrinter == MainLibrary.PRINTER.WOOSIM)
            woosimPrinter = new WoosimPrinter(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(MainLibrary.mSelectedPrinter == MainLibrary.PRINTER.WOOSIM)
            woosimPrinter.Close();
    }

    /** DATA PROCESSING *************************************************************/
    public class TaskProcessData extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {

            try {

                if (MainLibrary.gLUpdate) {

                    // STORE ASSORTMENT RECORDS
                    File fTextFile = new File(Environment.getExternalStorageDirectory(), MainLibrary.ASSORTMENT_TXTFILE);
                    if(fTextFile.exists() && mReupdatePCount) {
                        db.TruncateTable(SQLiteHelper.TABLE_ASSORTMENT);

                        BufferedReader br = new BufferedReader(new FileReader(fTextFile));

                        String rQuery = db.getStringBulkInsert(18, SQLiteHelper.TABLE_ASSORTMENT);
                        db.insertBulktoPcount(rQuery, br);

                        mReupdatePCount = false;
                    }
                }

                arrAssortment.clear();

                // SELECTING ASSORTMENT MASTERFILE
                //Cursor cursAssort = db.GetDataCursor(SQLiteHelper.TABLE_ASSORTMENT, SQLiteHelper.COLUMN_ASSORTMENT_STOREID + " = '" + String.valueOf(MainLibrary.gCurrentBranchSelected) + "'");
                Cursor cursAssort = db.queryData("SELECT * FROM " + SQLiteHelper.TABLE_ASSORTMENT + " WHERE storeid = " + String.valueOf(MainLibrary.gSelectedLocation.locationCode));
                cursAssort.moveToFirst();
                if(cursAssort.getCount() > 0) {
                    while (!cursAssort.isAfterLast()) {
                        int id = cursAssort.getInt(cursAssort.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_ID));
                        String barcode = cursAssort.getString(cursAssort.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_BARCODE)).trim();
                        String desc = cursAssort.getString(cursAssort.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_DESC)).trim();
                        String categoryid = cursAssort.getString(cursAssort.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_CATEGORYID)).trim();
                        String brandid = cursAssort.getString(cursAssort.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_BRANDID)).trim();
                        String divisionid = cursAssort.getString(cursAssort.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_DIVISIONID)).trim();
                        String subcategoryid = cursAssort.getString(cursAssort.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_SUBCATEGORYID)).trim();
                        int ig = cursAssort.getInt(cursAssort.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_IG));
                        int conversion = cursAssort.getInt(cursAssort.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_CONVERSION));
                        double fsovalue = cursAssort.getDouble(cursAssort.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_FSOVALUE));
                        int webid = cursAssort.getInt(cursAssort.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_WEBID));
                        int multi = cursAssort.getInt(cursAssort.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_MULTI));
                        String otherbar = cursAssort.getString(cursAssort.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_OTHERBARCODE));

                        arrAssortment.add(new Assortment(id, barcode, desc, categoryid, brandid, divisionid, subcategoryid, ig, conversion, fsovalue, webid, multi, false, otherbar));
                        cursAssort.moveToNext();
                    }
                }
                cursAssort.close();

                arrAssortmentAll.clear();


                // ORDERED ASSORTMENT ITEMS
                String query = "SELECT * FROM " + SQLiteHelper.TABLE_ASSORTMENT + " WHERE storeid = " + String.valueOf(MainLibrary.gSelectedLocation.locationCode);
                if(MainLibrary.gSelectedLocation.channelArea.equals("MDC"))
                    query = "SELECT * FROM " + SQLiteHelper.TABLE_ASSORTMENT + " WHERE storeid = " + String.valueOf(MainLibrary.gSelectedLocation.locationCode) + " ORDER BY " + SQLiteHelper.COLUMN_ASSORTMENT_BARCODE;
                Cursor cursOrdered = db.queryData(query);
                cursOrdered.moveToFirst();
                if(cursOrdered.getCount() > 0) {
                    while (!cursOrdered.isAfterLast()) {
                        int id = cursOrdered.getInt(cursOrdered.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_ID));
                        String barcode = cursOrdered.getString(cursOrdered.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_BARCODE)).trim();
                        String desc = cursOrdered.getString(cursOrdered.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_DESC)).trim();
                        String categoryid = cursOrdered.getString(cursOrdered.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_CATEGORYID)).trim();
                        String brandid = cursOrdered.getString(cursOrdered.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_BRANDID)).trim();
                        String divisionid = cursOrdered.getString(cursOrdered.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_DIVISIONID)).trim();
                        String subcategoryid = cursOrdered.getString(cursOrdered.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_SUBCATEGORYID)).trim();
                        int ig = cursOrdered.getInt(cursOrdered.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_IG));
                        int conversion = cursOrdered.getInt(cursOrdered.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_CONVERSION));
                        double fsovalue = cursOrdered.getDouble(cursOrdered.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_FSOVALUE));
                        int webid = cursOrdered.getInt(cursOrdered.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_WEBID));
                        int multi = cursOrdered.getInt(cursOrdered.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_MULTI));
                        String otherbar = cursOrdered.getString(cursOrdered.getColumnIndex(SQLiteHelper.COLUMN_ASSORTMENT_OTHERBARCODE));

                        arrAssortmentAll.add(new Assortment(id, barcode, desc, categoryid, brandid, divisionid, subcategoryid, ig, conversion, fsovalue, webid, multi, false, otherbar));
                        cursOrdered.moveToNext();
                    }
                }
                cursOrdered.close();

            }
            catch (IOException e) {
                //LogThis.LogToFile("TaskProcessData_IOException : \n"+e.toString());
                e.printStackTrace();
                Log.e("IOException", e.getMessage());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            pDL.dismiss();

            Cursor cursor = db.queryData("select * from " + SQLiteHelper.TABLE_TRANSACTION_ASSORT + " where storeid = " + String.valueOf(MainLibrary.gSelectedLocation.locationCode) + " and date = '" + MainLibrary.gStrCurrentDate +
                    "' and [userid] = " + MainLibrary.gStrCurrentUserID);

            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {

                String barcode = cursor.getString(cursor.getColumnIndex("barcode")).trim();

                for (Assortment assortment : arrAssortment) {

                    if (assortment.barcode.contains(barcode)) {

                        assortment.ig = cursor.getInt(cursor.getColumnIndex("ig"));
                        assortment.conversion = cursor.getInt(cursor.getColumnIndex("conversion"));
                        assortment.sapc = cursor.getInt(cursor.getColumnIndex("sapc"));
                        assortment.whpc = cursor.getInt(cursor.getColumnIndex("whpc"));
                        assortment.whcs = cursor.getInt(cursor.getColumnIndex("whcs"));
                        assortment.so = cursor.getInt(cursor.getColumnIndex("so"));
                        assortment.fso = cursor.getInt(cursor.getColumnIndex("fso"));
                        assortment.multi = cursor.getInt(cursor.getColumnIndex("multi"));
                        assortment.updated = true;
                        hmAssortment.put(assortment.barcode, assortment);
                        break;
                    }
                }

                for (Assortment assortmentAll : arrAssortmentAll) {
                    if (assortmentAll.barcode.contains(barcode)) {

                        assortmentAll.ig = cursor.getInt(cursor.getColumnIndex("ig"));
                        assortmentAll.conversion = cursor.getInt(cursor.getColumnIndex("conversion"));
                        assortmentAll.sapc = cursor.getInt(cursor.getColumnIndex("sapc"));
                        assortmentAll.whpc = cursor.getInt(cursor.getColumnIndex("whpc"));
                        assortmentAll.whcs = cursor.getInt(cursor.getColumnIndex("whcs"));
                        assortmentAll.so = cursor.getInt(cursor.getColumnIndex("so"));
                        assortmentAll.fso = cursor.getInt(cursor.getColumnIndex("fso"));
                        assortmentAll.multi = cursor.getInt(cursor.getColumnIndex("multi"));
                        assortmentAll.updated = true;
                        break;
                    }
                }

                cursor.moveToNext();
            }

            cursor.close();

            mAssortmentAdapter = new AssortmentAdapter(AssortmentActivity.this, arrAssortment);
            lvwAssortment.setAdapter(mAssortmentAdapter);
            mAssortmentAdapter.notifyDataSetChanged();

//            final int mYear = mCurrentDate.get(Calendar.YEAR);
//            final int mMonth = mCurrentDate.get(Calendar.MONTH);
//            final int mDay = mCurrentDate.get(Calendar.DAY_OF_MONTH);
//
//            DatePickerDialog dpd = new DatePickerDialog(PCountActivity.this,
//                    new DatePickerDialog.OnDateSetListener() {
//
//                        @Override
//                        public void onDateSet(DatePicker view, int year,
//                                              int monthOfYear, int dayOfMonth) {
//                            mCurrentDate = Calendar.getInstance();
//                            mCurrentDate.set(year, monthOfYear, dayOfMonth);
//                            mStrCurrentDate = MainLibrary.dateFormatter.format(mCurrentDate.getTime());
//                            Toast.makeText(PCountActivity.this, mStrCurrentDate, Toast.LENGTH_SHORT).show();
//
//
//                            mPCountListViewAdapter = new PCountListViewAdapter(PCountActivity.this, mArrayListPcount);
//                            mListViewPcount.setAdapter(mPCountListViewAdapter);
//                            mPCountListViewAdapter.notifyDataSetChanged();
//
//                        }
//                    }, mYear, mMonth, mDay);
//            dpd.setCancelable(false);
//            dpd.show();
//            Toast.makeText(PCountActivity.this, "Total Number of Record(s) " + String.valueOf(mArrayListPcount.size()), Toast.LENGTH_SHORT).show();

        }

        @Override
        protected void onPreExecute() {
            //Toast.makeText(PCountActivity.this, "Current Location Selected " + String.valueOf(MainLibrary.gCurrentBranchSelected), Toast.LENGTH_SHORT).show();
            pDL = ProgressDialog.show(AssortmentActivity.this, "", "Updating Masterfile. Please Wait...", true);
        }
    }

    /** DATA PROCESSING *************************************************************/
    public class TaskSaveData extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {

            db.DeleteRecord(SQLiteHelper.TABLE_TRANSACTION_ASSORT,"date = ? and storeid = ? and userid = ?", new String[] { MainLibrary.gStrCurrentDate, String.valueOf(MainLibrary.gSelectedLocation.locationCode), String.valueOf(MainLibrary.gStrCurrentUserID) });

            for (Assortment assortment : arrAssortmentAll) {

                if (assortment.sapc != 0 || assortment.whpc != 0 || assortment.whcs != 0 || assortment.fso != 0 || assortment.updated) {

                    String[] afields = {
                            "date",
                            "storeid",
                            "barcode",
                            "ig",
                            "sapc",
                            "whpc",
                            "whcs",
                            "conversion",
                            "so",
                            "fso",
                            "fsovalue",
                            "webid",
                            "userid",
                            "multi",
                            SQLiteHelper.COLUMN_TRANSACTION_ASSORT_MONTH,
                            SQLiteHelper.COLUMN_TRANSACTION_ASSORT_LPOSTED
                    };

                    String[] avalues = { MainLibrary.gStrCurrentDate
                            , String.valueOf(MainLibrary.gSelectedLocation.locationCode)
                            , assortment.barcode
                            , String.valueOf(assortment.ig)
                            , String.valueOf(assortment.sapc)
                            , String.valueOf(assortment.whpc)
                            , String.valueOf(assortment.whcs)
                            , String.valueOf(assortment.conversion)
                            , String.valueOf(assortment.so)
                            , String.valueOf(assortment.fso)
                            , String.valueOf(assortment.fsovalue)
                            , String.valueOf(assortment.webid)
                            , String.valueOf(MainLibrary.gStrCurrentUserID)
                            , String.valueOf(assortment.multi)
                            , MainLibrary.selectedMonth.trim()
                            , "0"
                    };

                    hmAssortment.put(assortment.barcode, assortment);
                    db.AddRecord(SQLiteHelper.TABLE_TRANSACTION_ASSORT, afields, avalues );
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            pDL.dismiss();
        }

        @Override
        protected void onPreExecute() {
            pDL = ProgressDialog.show(AssortmentActivity.this, "", "Saving Transaction dated " + MainLibrary.gStrCurrentDate + ". Please Wait...", true);
        }
    }

    /** DATA PROCESSING *************************************************************/
    public class TaskPrintData extends AsyncTask<String, Void, Boolean> {

        String print = "";
        Boolean lwithbarcode = true;
        String errmsg = "";

        public TaskPrintData(boolean hasBarcode) {
            this.lwithbarcode = hasBarcode;
        }

        @Override
        protected Boolean doInBackground(String... params) {

            Boolean lsuccess = false;

            switch (MainLibrary.mSelectedPrinter) {
                case WOOSIM:
                    if(PrintFormatByWoosim(lwithbarcode)) {
                        lsuccess = true;
                    }
                    break;
                case TSC:
                    print = Printer.GenerateStringTSCPrint(PrintFormat(lwithbarcode), len, numItems, 1);

                    if(Printer.Open()) {

                        String basfile = "DEFAULT.PRN";
                        switch (MainLibrary.eStore) {
                            case SEVEN_ELEVEN:
                                basfile = "711.PRN";
                                break;
                            case MERCURY_DRUG:
                                basfile = "MERCURY.PRN";
                                break;
                            case MINISTOP:
                                basfile = "MINISTOP.PRN";
                                break;
                            case FAMILY_MART:
                                basfile = "FAMILY.PRN";
                                break;
                            case LAWSON:
                                basfile = "LAWSON.PRN";
                                break;
                            case ALFAMART:
                                basfile = "ALFAMART.PRN";
                                break;
                            default:
                                break;
                        };

                        try {

                            Printer.sendcommand("SIZE 4,1\n");
                            Printer.sendcommand("GAP 0,0\n");
                            Printer.sendcommand("DIRECTION 1\n");
                            Printer.sendcommand("SET TEAR ON\n");
                            Printer.sendcommand("CLS\n");

                            Printer.sendfile(basfile);

                            Printer.clearbuffer();
                            Printer.PrintString(print);
                            lsuccess = true;
                        }
                        catch (Exception ex) {
                            errmsg = ex.getMessage();
                        }
                    }
                    break;
                default:
                    break;
            }

            return lsuccess;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            pDL.dismiss();

            if (result) {

                AlertDialog printdialog = new AlertDialog.Builder(AssortmentActivity.this).create();
                printdialog.setTitle("Print");
                printdialog.setMessage("Print successful.");

                DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Printer.Close();
                    }
                };

                DialogInterface.OnCancelListener cancelListerner = new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        Printer.Close();
                    }
                };

                if(MainLibrary.mSelectedPrinter == MainLibrary.PRINTER.WOOSIM) {
                    okListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    };
                    cancelListerner = new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            dialog.dismiss();
                        }
                    };
                }

                printdialog.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", okListener);
                printdialog.setOnCancelListener(cancelListerner);

                printdialog.show();

            }else{
                Toast.makeText(AssortmentActivity.this, "Error Printing. Please Check Connection with the Printer.", Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        protected void onPreExecute() {
            pDL = ProgressDialog.show(AssortmentActivity.this, "", "Printing. Please Wait...", true);
        }
    }

    private boolean PrintFormatByWoosim(boolean hasBarcode) {
        boolean result = false;
        String toPrint = "";

        // PRINT LOGO ------------------------------
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_default, options);

        switch (MainLibrary.eStore) {
            case SEVEN_ELEVEN:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_seveneleven, options);
                break;
            case MERCURY_DRUG:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_mercurydrug, options);
                break;
            case MINISTOP:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_ministop, options);
                break;
            case FAMILY_MART:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_familymart, options);
                break;
            case LAWSON:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_lawson, options);
                break;
            case ALFAMART:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_alfamart, options);
                break;
            default:
                break;
        };

        if(!woosimPrinter.printBMPImage(bmpStore, 0, 0, 580, 180)) {
            return result;
        }
        // -------------------------------

        toPrint += "Store: " + MainLibrary.gSelectedLocation.locationName + "\n";

        int nSkuWithStocks = 0;
        int nTotSku = 0;
        double nOsaScore;
        String osascore = "";
        for (Assortment assortment : arrAssortmentAll) {
            nTotSku++;
            if(assortment.sapc != 0 || assortment.whpc != 0 || assortment.whcs != 0) {
                nSkuWithStocks++;
            }
        }
        nOsaScore = (Double.valueOf(nSkuWithStocks) / Double.valueOf(nTotSku)) * 100;
        osascore = String.format("%.2f", nOsaScore) + " %";

        toPrint += "Date: " + MainLibrary.gStrCurrentDate +"\n";
        toPrint += StringUtils.rightPad("SKU",25,"") +
                StringUtils.rightPad("IG",10,"") +
                StringUtils.rightPad("Invty",8,"") +
                StringUtils.rightPad("Order qty", 12,"") +
                StringUtils.rightPad("Order amt", 5, "") + "\n";
        toPrint += Printer.woosimLines;

        try {
            if(!woosimPrinter.printText(toPrint, false, false, 1))  return result;
        }
        catch (IOException ex) {
            ex.printStackTrace();
            Log.e(TAG, ex.getMessage());
        }

        result = PrintDetailsByWoosim(hasBarcode);

        return result;
    }

    private boolean PrintDetailsByWoosim(boolean hasBarcode) {

        boolean result = false;
        int totsku = 0, totfso = 0;
        double totfsoval = 0;
        try {

            for (Assortment assortment : arrAssortmentAll) {

                if (lprintwithso) {
                    if (assortment.so == 0) {
                        continue;
                    }
                }

                int lenig = 13 - String.valueOf(assortment.ig).length();

                int totig = assortment.sapc + assortment.whpc + (assortment.whcs * assortment.conversion);

                int lenei = 11 - String.valueOf(totig).length();
                int lenfso = 12 - String.valueOf(assortment.fso).length(); // 18
                int lenfsoval = 3 - String.valueOf(assortment.fsovalue * assortment.fso).length(); // 18

                String itemDesc = StringUtils.rightPad(assortment.desc + " " + assortment.barcode, 20, "");
                if(!woosimPrinter.printText(itemDesc, false, false, 1)) return result;

                String strValues = StringUtils.rightPad("", 25, "")
                        + StringUtils.rightPad(String.valueOf(assortment.ig), lenig)
                        + StringUtils.rightPad(String.valueOf(totig), lenei)
                        + StringUtils.rightPad(String.valueOf(assortment.fso), lenfso, "")
                        + StringUtils.rightPad(MainLibrary.priceDec.format(assortment.fsovalue * assortment.fso), lenfsoval, "");

                if(!woosimPrinter.printText(strValues, true, false, 1)) return result;

                if(hasBarcode) {
                    String barcodeType = MainLibrary.GetBarcodeType(assortment.itembarcode);
                    woosimPrinter.print1DBarcode(barcodeType, assortment.itembarcode);
                }

                if(!woosimPrinter.printText(" ", false, false, 1)) return result;

                if (assortment.so > 0) {
                    totsku = totsku + 1;
                }

                totfso = totfso + assortment.fso;
                totfsoval = totfsoval + (assortment.fsovalue * assortment.fso) ;
            }

            // FOOTER
            String footer = "";

            footer += Printer.woosimLines;
            footer += "Total: " + StringUtils.rightPad(String.valueOf(totsku),40/*76*/) + StringUtils.rightPad(String.valueOf(totfso),9)
                    + String.format("%.2f", totfsoval) + "\n";
            footer += "\n" + "\n" + "\n" + "\n" + "\n";
            footer += StringUtils.center(Printer.woosimLines2, 64);
            footer += StringUtils.center("Acknowledged by", 64);
            footer += "\n" + "\n"+ "\n";

            if(!woosimPrinter.printText(footer, true, false, 1)) return result;

            result = true;
        }
        catch (IOException ex) {
            ex.printStackTrace();
            Log.e(TAG, ex.getMessage());
        }

        return result;
    }

    @Override
    public void onBackPressed() {
        setResult(888);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
//                NavUtils.navigateUpFromSameTask(this);
                AlertDialog.Builder logoutdialog = new AlertDialog.Builder(AssortmentActivity.this);
                logoutdialog.setTitle("Log Out");
                logoutdialog.setMessage("Are you sure you want to log out?");
                logoutdialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                logoutdialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        SharedPreferences.Editor spEditor = MainLibrary.sprefUsers.edit();
                        spEditor.putBoolean(getString(R.string.logged_pref_key), false);
                        spEditor.commit();

                        dialog.dismiss();
                        new UserLogout().execute();
                    }
                });

                logoutdialog.show();
                break;
            case R.id.action_submenu_category:
                FilterChanged(0);
                break;
            case R.id.action_submenu_subcateg:
                FilterChanged(1);
                break;
            case R.id.action_submenu_brand:
                FilterChanged(2);
                break;
            case R.id.action_submenu_division:
                FilterChanged(3);
                break;
            case R.id.action_submenu_withso:
                FilterChanged(4);
                break;
            case R.id.action_submenu_woso:
                FilterChanged(5);
                break;
            case R.id.action_submenu_all:
                mAssortmentAdapter.filter(0, "");
                break;
            case R.id.action_detail_summary:
                ViewReports(-1);
                break;
            case R.id.action_category_report:
                ViewReports(0);
                break;
            case R.id.action_subcate_report:
                ViewReports(1);
                break;
            case R.id.action_brand_report:
                ViewReports(2);
                break;
            case R.id.action_division_report:
                ViewReports(3);
                break;
            case R.id.action_withso_report:
                ViewReports(4);
                break;
            case R.id.action_post:
                if (!BuildConfig.DEBUG) {
                    Boolean linvalid = false;
                    for (Assortment assortment : arrAssortmentAll) {
                        if (assortment.sapc == 0 && assortment.whpc == 0 && assortment.whcs == 0) {
                            linvalid = true;
                            break;
                        }
                    }
                    if (linvalid) {
                        Toast.makeText(AssortmentActivity.this, "Cannot Post Transaction.", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }

                new CheckRequiredItems(true).execute();

                //Toast.makeText(AssortmentActivity.this, "Posting of assortment disabled.", Toast.LENGTH_SHORT).show();
/*                Intent intent = new Intent(AssortmentActivity.this, CaptureSignatureActivity.class);
                intent.putExtra("location", MainLibrary.gCurrentBranchSelected);
                intent.putExtra("datepick", MainLibrary.gStrCurrentDate);
                startActivity(intent);*/
                break;
            case R.id.action_save_current:
                new TaskSaveData().execute();
                break;
            case R.id.action_print_all_barcode:
                if(MainLibrary.CheckBluetooth())
                    //new TaskPrintData(true).execute();
                    new CheckRequiredItems(false, true, true).execute();
                else
                    Toast.makeText(AssortmentActivity.this, "Please enable yur bluetooth to proceed.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_print_all_nobarcode:
                if(MainLibrary.CheckBluetooth())
                    //new TaskPrintData(false).execute();
                    new CheckRequiredItems(false, true, false).execute();
                else
                    Toast.makeText(AssortmentActivity.this, "Please enable yur bluetooth to proceed.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_print_withso_barcode:
                if(MainLibrary.CheckBluetooth()) {
                    //lprintwithso = true;
                    //new TaskPrintData().execute(); // PRINT DATA
                    new CheckRequiredItems(false, false, true).execute();
                }
                else
                    Toast.makeText(AssortmentActivity.this, "Please enable yur bluetooth to proceed.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_print_withso_nobarcode:
                if(MainLibrary.CheckBluetooth()) {
                    //lprintwithso = true;
                    //new TaskPrintData().execute(); // PRINT DATA
                    new CheckRequiredItems(false, false, false).execute();
                }
                else
                    Toast.makeText(AssortmentActivity.this, "Please enable yur bluetooth to proceed.", Toast.LENGTH_SHORT).show();
                break;
            case android.R.id.home:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public class CheckRequiredItems extends AsyncTask<Void, Void, Boolean> {

        String strError;
        int nAllItems;
        boolean bPostMode = false;
        boolean bPrintAll = false;
        boolean bHasBarcode = false;

        public CheckRequiredItems(boolean isPostMode) {
            this.bPostMode = isPostMode;
        }

        public CheckRequiredItems(boolean isPostMode, boolean isPrintAll, boolean hasBarcode) {
            this.bPostMode = isPostMode;
            this.bPrintAll = isPrintAll;
            this.bHasBarcode = hasBarcode;
        }

        @Override
        protected void onPreExecute() {
            pDL = ProgressDialog.show(AssortmentActivity.this, "", "Checking required items.", true);
            strError = "";
            nAllItems = arrAssortmentAll.size();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean bReturn = true;

            if(hmAssortment.size() == 0) {
                bReturn = false;
                strError = "No transactions found.";
            }
            else {
                for (Assortment assortment : arrAssortmentAll) {
                    if (!hmAssortment.containsKey(assortment.barcode)) {
                        bReturn = false;
                        strError = hmAssortment.size() + " / " + nAllItems + ". Some required items not transacted.";
                        break;
                    }
                }
            }

            return bReturn;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            pDL.dismiss();
            if(!aBoolean) {
                Toast.makeText(AssortmentActivity.this, strError, Toast.LENGTH_SHORT).show();
                return;
            }

            // FOR POSTING
            Intent intentpost = new Intent(AssortmentActivity.this, CaptureSignatureActivity.class);
            intentpost.putExtra("location", MainLibrary.gSelectedLocation.locationCode);
            intentpost.putExtra("datepick", MainLibrary.gStrCurrentDate);

            // FOR PRINTING
            mAlertDialog = new AlertDialog.Builder(AssortmentActivity.this).create();
            mAlertDialog.setTitle("Print all items");

            if(bHasBarcode)
                mAlertDialog.setMessage("Do you want to print all items with barcode ?");
            else
                mAlertDialog.setMessage("Do you want to print all items without barcode ?");

            mAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mAlertDialog.dismiss();
                }
            });
            mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Print", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mAlertDialog.dismiss();
                    lprintall = true;
                    new TaskPrintData(bHasBarcode).execute();
                }
            });

            if(bPostMode) startActivity(intentpost);
            else {
                if (bPrintAll) mAlertDialog.show();
                else {
                    lprintwithso = true;
                    new TaskPrintData(bHasBarcode).execute(); // PRINT DATA
                }
            }
        }
    }

    private String PrintFormat(boolean hasBarcode) {

        String toPrint = "";

        numItems = 0;
        len = 0;

        toPrint += "\n";
        toPrint += "Store: " + MainLibrary.gSelectedLocation.locationName + "\n";
        toPrint += "Date: " + MainLibrary.gStrCurrentDate +"\n" + "\n" ;
        toPrint += StringUtils.rightPad("SKU",45,"") +
                StringUtils.rightPad("IG",14,"") +
                StringUtils.rightPad("Invty",14,"") +
                StringUtils.rightPad("Order qty", 14,"") +
                StringUtils.rightPad("Order amt", 14, "") + "\n";
        toPrint += Printer.tsclines;

        len += 1.5;

        int totsku = 0, totfso = 0;
        double totfsoval = 0;


        for (Assortment assortment : arrAssortmentAll) {

            if (lprintwithso) {
                if (assortment.so == 0) {
                    continue;
                }
            }

            int lensku = 50 - assortment.barcode.length();
            int lenig = 18 - String.valueOf(assortment.ig).length();
            int totig = assortment.sapc + assortment.whpc + (assortment.whcs * assortment.conversion);
            int lenei = 14 - String.valueOf(totig).length();
            int lenfso = 12 - String.valueOf(assortment.fso).length(); // 18
            int lenfsoval = 12 - String.valueOf(assortment.fsovalue * assortment.fso).length(); // 18

            String barcodeType = MainLibrary.GetBarcodeType(assortment.itembarcode);
            String barcodeCmd = "";
            String endlines = "";
            if(hasBarcode) {
                barcodeCmd = "BARCODE ;\"" + barcodeType + "\",50,2,0,2,2,\"" + assortment.itembarcode + "\"" + "\n";
            }
            else {
                endlines += "\n";
            }

            toPrint += StringUtils.rightPad(assortment.desc + " " + assortment.barcode, 20, "") + "\n"
                    + barcodeCmd
                    + endlines
                    + "\nINFO ;"
                    + StringUtils.rightPad(" ", 47,"")
                    + StringUtils.rightPad(String.valueOf(assortment.ig), lenig)
                    + StringUtils.rightPad(String.valueOf(totig),lenei)
                    + "*"
                    + StringUtils.rightPad(String.valueOf(assortment.fso), lenfso, "")
                    + StringUtils.rightPad(String.format("%.2f", assortment.fsovalue * assortment.fso), lenfsoval, "")
                    + "*"
                    + StringUtils.rightPad("       ", lensku,"")
                    + "\n";

            if (assortment.so > 0) {
                totsku = totsku + 1;
            }

            numItems++;
            len += 0.70;

            totfso = totfso + assortment.fso;
            totfsoval = totfsoval + (assortment.fsovalue * assortment.fso) ;
        }

        toPrint += Printer.tsclines;
        toPrint += "Total: " + StringUtils.rightPad(String.valueOf(totsku),32/*76*/) + StringUtils.rightPad(String.valueOf(totfso),11)
                + StringUtils.rightPad(String.format("%.2f", totfsoval),12) + "\n";
        toPrint += "\n" + "\n" + "\n" + "\n" + "\n";
        toPrint += StringUtils.center(Printer.tsclines2,80);
        toPrint += StringUtils.center("Acknowledge by",80);

        toPrint += "\n";
        toPrint += "\n";
        toPrint += "\n";
        toPrint += "\n";
        toPrint += "\n";

        lprintall = false;
        lprintwithso = false;

        len += 1.60;

        return toPrint;
    }

    public void FilterChanged(final int filterCode) {

        final Dialog dialog = new Dialog(AssortmentActivity.this, R.style.Transparent);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.activity_branch2);

        TextView lvCaption = (TextView) dialog.findViewById(R.id.textViewBranchName);

        if (filterCode > 3) {
            mAssortmentAdapter.filter(filterCode, "xxx");
        } else {

            final String filterId, filterTitle;

            switch (filterCode) {

                case 0:
                    filterId = "categoryid";
                    filterTitle = "Category";
                    break;
                case 1:
                    filterId = "subcategoryid";
                    filterTitle = "Subcategory";
                    break;
                case 2:
                    filterId = "brandid";
                    filterTitle = "Brand";
                    break;
                case 3:
                    filterId = "divisionid";
                    filterTitle = "Division";
                    break;
                default:
                    filterId = "";
                    filterTitle = "";
            }

            Cursor tmpCategory = db.GetGroupby(filterId, SQLiteHelper.TABLE_ASSORTMENT);

            String[] from = new String[] {
                    filterId
            };
            int[] to = new int[] { R.id.itemTextView };

            SimpleCursorAdapter adapter = new SimpleCursorAdapter(AssortmentActivity.this, R.layout.activity_items_filtering,
                    tmpCategory, from, to, 0);
            final ListView lv = (ListView) dialog.findViewById(R.id.listViewBranch);
            lv.setAdapter(adapter);
            lvCaption.setText("Select " + filterTitle);

            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    dialog.dismiss();

                    TextView c = (TextView) view.findViewById(R.id.itemTextView);
                    String name = c.getText().toString();
                    Toast.makeText(AssortmentActivity.this, name, Toast.LENGTH_SHORT).show();
                    mAssortmentAdapter.filter(filterCode, name);

                }
            });

            dialog.show();

        }

    }

    public void ViewReports(int reportType) {

        Boolean lvalid = false;
        Boolean lwso = false;
        Cursor cursorGroup = null;

        final Dialog dialog = new Dialog(AssortmentActivity.this, R.style.Transparent);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.activity_report);

        final TextView textViewTitle = (TextView) dialog.findViewById(R.id.textViewTitle);
        final TextView textViewColumnTitle = (TextView) dialog.findViewById(R.id.textViewColumnTitle);
        final ListView listViewReport = (ListView) dialog.findViewById(R.id.listViewReport);

        final String reportTitle, columnTitle, filterId;

        switch (reportType) {

            case 0:
                reportTitle = "Per Category Report";
                columnTitle = "Category";
                filterId = "categoryid";
                break;
            case 1:
                reportTitle = "Per Subcategory Report";
                columnTitle = "Subcategory";
                filterId = "subcategoryid";
                break;
            case 2:
                reportTitle = "Per Brand Report";
                columnTitle = "Brand";
                filterId = "brandid";
                break;
            case 3:
                reportTitle = "Per Division Report";
                columnTitle = "Division";
                filterId = "divisionid";
                break;
            case 4:
                reportTitle = "With SO Report";
                columnTitle = "With SO";
                filterId = "[desc]";
                lwso = true;
                break;
            default:
                reportTitle = "Items Summary Report";
                columnTitle = "Items";
                filterId = "[desc]";
        }

        textViewTitle.setText(reportTitle);
        textViewColumnTitle.setText(columnTitle);

        ArrayList<ReportClass> arrayListReport = new ArrayList<>();

        cursorGroup = db.queryData("select " + filterId + " as name from " + SQLiteHelper.TABLE_ASSORTMENT + " where storeid = " + String.valueOf(MainLibrary.gSelectedLocation.locationCode) +
                " group by " + filterId);


        cursorGroup.moveToFirst();

        while (!cursorGroup.isAfterLast()) {

            arrayListReport.add(new ReportClass(cursorGroup.getString(cursorGroup.getColumnIndex("name")).trim()));

            cursorGroup.moveToNext();
        }

        for (Assortment assortment : arrAssortmentAll) {

 /*           if (reportType >= 4){
                switch (reportType){
                    case 4:
                        lvalid = pCount.sapc != 0 || pCount.whpc != 0 || pCount.whcs != 0;
                        if (!lvalid) {
                            continue;
                        }
                        break;
                }
            }*/

            for (ReportClass reportClass : arrayListReport) {

                switch (reportType) {

                    case 0:
                        if (!reportClass.name.contains(assortment.category)) {
                            continue;
                        }
                        break;
                    case 1:
                        if (!reportClass.name.contains(assortment.subcate)) {
                            continue;
                        }
                        break;
                    case 2:
                        if (!reportClass.name.contains(assortment.brand)) {
                            continue;
                        }
                        break;
                    case 3:
                        if (!reportClass.name.contains(assortment.division)) {
                            continue;
                        }
                        break;
                    case 4:
 /*                       lvalid = pCount.sapc != 0 || pCount.whpc != 0 || pCount.whcs != 0;
                        if (!lvalid) {
                            continue;
                        }*/
                        if (!reportClass.name.contains(assortment.desc)) {
                            continue;
                        }
                        break;
                    default:
                        if (!reportClass.name.contains(assortment.desc)) {
                            continue;
                        }
                }

                reportClass.ig = reportClass.ig + assortment.ig;
                reportClass.so = reportClass.so + assortment.so;
                reportClass.endinv = reportClass.endinv + (assortment.sapc + assortment.whpc + (assortment.whcs * assortment.conversion));
                reportClass.finalso = reportClass.finalso + assortment.fso;
                reportClass.multi = assortment.multi;

            }

        }

        ArrayList<ReportClass> arrayListReport2 = new ArrayList<>();

        if (reportType == 4){
            Iterator i = arrayListReport.iterator();
            while(i.hasNext()){
                ReportClass reportClass = (ReportClass) i.next();
                if (reportClass.so > 0){
//                    reportClass.finalso = reportClass.finalso; //- (reportClass.so % reportClass.multi) + reportClass.multi;
                    arrayListReport2.add(reportClass);
                }else{
                    i.remove();
                }
            }
        }else{
           /* for (ReportClass reportClass : arrayListReport) {
                if (reportClass.so > 0) reportClass.finalso = reportClass.so - (reportClass.so % reportClass.multi) + reportClass.multi;
            }*/
        }

        /*if (reportType >= 4) {
            ArrayList<ReportClass> arrayListReport2 = new ArrayList<>();
            for (ReportClass reportClass : arrayListReport) {
                switch (reportType) {
                    case 4:
                        if (reportClass.so != 0) arrayListReport2.add(reportClass);
                        break;
                }
            }
            listViewReport.setAdapter(new ReportListViewAdapter(PCountActivity.this, arrayListReport2));
        } else {
            listViewReport.setAdapter(new ReportListViewAdapter(PCountActivity.this, arrayListReport));
        }

        dialog.show();*/


        if ((reportType == 4)) {
            listViewReport.setAdapter(new ReportListViewAdapter(AssortmentActivity.this, arrayListReport2));
        }else{
            listViewReport.setAdapter(new ReportListViewAdapter(AssortmentActivity.this, arrayListReport));
        }

        dialog.show();

    }

    public class UserLogout extends AsyncTask<Void, Void, Boolean> {

        String response;
        String errmsg;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDL = ProgressDialog.show(AssortmentActivity.this, "", "Logging out. Please Wait...", true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean bReturn = false;

            try{

                String urlfinal = MainLibrary.API_URL + "/api/logout?email=" + MainLibrary.gStrCurrentUserName + "&device_id=" + MainLibrary.gStrDeviceId;

                URL url = new URL(urlfinal);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();
                try{
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    urlConnection.disconnect();
                    response = stringBuilder.toString();
                    bReturn = true;
                }
                catch (MalformedURLException mex) {
                    mex.printStackTrace();
                    Log.e("MalformedURLException", mex.getMessage());
                    errmsg += "\n" + mex.getMessage();
                }

            } catch(Exception e){
                e.printStackTrace();
                Log.e("Exception", e.getMessage(), e);
                errmsg += "\n" + e.getMessage();
            }
            return bReturn;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            pDL.dismiss();
            Intent intentMain = new Intent(AssortmentActivity.this, MainActivity.class);
            if(!success) {
                //Toast.makeText(AssortmentActivity.this, errmsg, Toast.LENGTH_LONG).show();
                startActivity(intentMain);
                return;
            }

            try {

                JSONObject data = new JSONObject(response);
                String msg = data.getString("msg");

                MainLibrary.gStrCurrentUserID = 0;
                MainLibrary.gStrCurrentUserName = "";

                Toast.makeText(AssortmentActivity.this, msg, Toast.LENGTH_SHORT).show();
                startActivity(intentMain);
                finish();
            }
            catch (JSONException jex) {
                jex.printStackTrace();
                Log.e("JSONException", jex.getMessage());
            }

        }
    }
}
