/*
 * Copyright (C) 2012 Paul Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ipaulpro.afilechooserexample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ipaulpro.afilechooser.utils.FileUtils;
import com.ipaulpro.afilechooserexample.com.ipaulpro.afilechooserexample.util.ListViewAdapter;
import com.ipaulpro.afilechooserexample.com.ipaulpro.afilechooserexample.util.RequestCode;
import com.ipaulpro.afilechooserexample.com.ipaulpro.afilechooserexample.util.transferTask;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * @author paulburke (ipaulpro)
 */
public class FileChooserExampleActivity extends Activity {

    private static final String TAG = "FileChooserExampleActivity";
    private ImageButton OpenFileDialoBtn;                                              // code
    private ImageButton StartBTbtn;
    private ImageButton StopBTbtn;
    private ImageView statusBtImg;
    public TextView myLabel;
    private EditText enterTextBox;
    private boolean answerResult = false;
    private boolean findStatus = false;
    private boolean firstStart = false;
    TimerTask mTimerTask;
    connectTask ct;
    final Handler myHandler = new Handler();
    Timer t = new Timer();
    Functions fn = new Functions();
    // Create ListView Object
    private ListView lvList;
    //Create object of custom adapter
    ListViewAdapter adapter;

    BluetoothAdapter mBluetoothAdapter = null;
    BluetoothSocket mmSocket = null;
    BluetoothDevice mmDevice = null;
    public OutputStream mmOutputStream = null;
    public InputStream mmInputStream = null;
    Thread workerThread;
    Handler stHandler;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    public ProgressDialog pd;
    ProgressDialog pdBt;
    final static private int START_TRANSFER = 1;
    final static private int STOP_TRANSFER = 2;
    final static private int TRANSFER_GO = 3;
    private String BTnameTransfered;
    // Defined Array String values
    String[] values = new String[]{
            "Соединиться",
            "Разъединиться",
            "Открыть файлы",
            "Сброс"};

    // Defined Array of images id
    Integer[] imgid = {
            R.drawable.bluetooth_converted,
            R.drawable.close_converted,
            R.drawable.folder_move_converted,
            R.drawable.resetbutton};

    private Activity context;
    //==============================================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Intent intent = getIntent();
        BTnameTransfered = intent.getStringExtra("BTname");
        if (BTnameTransfered == null) {
            BTnameTransfered = "";
        }
        myLabel = (TextView) findViewById(R.id.MyLabel);
        statusBtImg = (ImageView) findViewById(R.id.statusBtImg);
        //==============================================================================================
        firstStart = true;
        //Get ListView object from xml
        lvList = (ListView) findViewById(R.id.lvList);
        //call list view adapter constructor
        adapter = new ListViewAdapter(this, values, imgid);
        //Assign Above Array Adapter to ListView
        lvList.setAdapter(adapter);
        //Create ListView Item click listener
        lvList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(), values[position], Toast.LENGTH_LONG).show();
                switch (position) {
                    case 0:
                        if (!conectionStatus) {
                            onClickStartBtn();
                        }
                        break;
                    case 1:
                        onClickStopBtn();
                        break;
                    case 2:
                        onClickFileDialog();
                        break;
                    case 3:
                        if(conectionStatus){
                            resetModem();
                        }
                        break;
                }
            }
        });

        //==========================================================================================

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            myLabel.setText("Блютуз отсутствует");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, RequestCode.REQUEST_CODE_MY);

        }
        Log.v("ArduinoBT", "onCreate");
    //==============================================================================================
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(bReceiver, filter1);
        this.registerReceiver(bReceiver, filter2);
        this.registerReceiver(bReceiver, filter3);
        doTimerTask();

        if(mBluetoothAdapter.isEnabled()){
            startConnection();
        }


    }
    //==============================================================================================
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
        startActivity(intent);
        finish();
    }
    //==============================================================================================
    public void doTimerTask() {

        Log.v("ArduinoBT", "TimerTask run");
        mTimerTask = new TimerTask() {
            public void run() {
                myHandler.post(new Runnable() {
                    public void run() {

                        if(foundProcess)
                        {
                            myLabel.setText("Поиск..");
                        }
                        else
                        {
                            if (conectionStatus == true) {
                                statusBtImg.setImageResource(R.drawable.blugreen);
                                myLabel.setText("Подключен..");
                            } else {
                                statusBtImg.setImageResource(R.drawable.blured);
                                myLabel.setText("Не подключен..");
                            }
                        }


                    }
                });
            }
        };

       t.schedule(mTimerTask, 0, 100);

    }

    public void stopTask() {

        if (mTimerTask != null) {
            Log.v("ArduinoBT", "timer canceled");
            mTimerTask.cancel();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v("ArduinoBT", "onStart");

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v("ArduinoBT", "onDestroy");
        if(ct!=null) {
            ct.cancel(true);
            while (!ct.isCancelled()) ;
            ct = null;
        }
        if(mt!=null) {
            mt.cancel(true);
            while (!mt.isCancelled()) ;
            mt = null;
        }

        try {
            closeBT();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.unregisterReceiver(bReceiver);
        stopTask();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v("ArduinoBT", "onResume");

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v("ArduinoBT", "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v("ArduinoBT", "onStop");
    }

    public void onClickStartBtn() {
        if(!conectionStatus){
            startConnection();
        }
    }

    //==============================================================================================
    public void onClickStopBtn() {
        try {
            closeBT();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }


    //==============================================================================================
    public void showBTdialog() {
        pdBt = new ProgressDialog(this);
        pdBt.setTitle("Соединение");
        pdBt.setMessage("Подождите, идет соединение..");
        pdBt.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pdBt.setIndeterminate(true);
        pdBt.show();
    }

    //==============================================================================================
    boolean foundStatus = false;
    public boolean conectionStatus = false;
    boolean conectedStatus = false;
    final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name and the MAC address of the object to the arrayAdapter
                if (device.getName().equals(BTnameTransfered)) {
                    mmDevice = device;
                    foundStatus = true;
                    Log.v("ArduinoBT",
                            "findBT found device named " + mmDevice.getName());
                    Log.v("ArduinoBT",
                            "device address is " + mmDevice.getAddress());
                    if(mBluetoothAdapter.isDiscovering())mBluetoothAdapter.cancelDiscovery();
                    try {
                        openBT();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
//                    if (prgDlg!=null) {
//                        prgDlg.dismiss();
//                    }
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Log.v("ArduinoBT", "Action Conected");
                //if(mBluetoothAdapter.isDiscovering())mBluetoothAdapter.cancelDiscovery();
                conectionStatus = true;
                foundProcess =false;
                if (prgDlg!=null) {
                        prgDlg.dismiss();
                    }
//                try {
//                    openBT();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                //resetModem();
                taskStatus=false;

            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.v("ArduinoBT", "Action Disconected");
                conectionStatus = false;
                conectedStatus = false;
                foundStatus=false;
                taskStatus=true;

                if(inTask) {
                    if(mt!=null)
                    {
                       mt.cancel(true);
                        while(!mt.isCancelled());
                    }
                    ResetTransferPacket();
                    hideProgressDialog();
                    //resetModem();
                    showCDialog(pd,false);
                    Log.v("ArduinoBT", "Disconnect Exeption task");
                    if(mt!=null){
                        Log.v("ArduinoBT", "isCanceled "+String.valueOf(mt.isCancelled()));
                        AsyncTask.Status r = mt.getStatus();
                        Log.v("ArduinoBT", "Status "+String.valueOf(r));
                        Log.v("ArduinoBT", "BTstatus "+String.valueOf(conectionStatus));
                    }
                    inTask=false;
                }
            }

        }
    };
    ProgressDialog prgDlg;
public boolean foundProcess =false;
    //==============================================================================================
    public void showConnectDlg(){
        prgDlg = new ProgressDialog(this);
        prgDlg.setTitle("Поиск устройств");
        prgDlg.setMessage("Подождите...");
        prgDlg.setIndeterminate(false);
        prgDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        prgDlg.setCancelable(false);
        prgDlg.setButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                prgDlg.dismiss();
                if(ct!=null){
                    ct.cancel(true);
                    while(!ct.isCancelled());
                    ct=null;
                }
            }
        });
        prgDlg.show();
    }
    //==============================================================================================
    public void findBT() {
        Log.v("ArduinoBT", "findBT");
        showConnectDlg();
        if(mBluetoothAdapter.isEnabled()&&(!mBluetoothAdapter.isDiscovering())) {
            mBluetoothAdapter.startDiscovery();
        }
        myLabel.setText("Поиск...");
        conectedStatus = false;
        foundProcess =true;
        Log.v("ArduinoBT", "Device seaching");
        if(foundStatus) {
            new CountDownTimer(20000, 1000) {
                @Override
                public void onFinish() {
                    Log.v("ArduinoBT", "onFinish");
                    if (prgDlg != null) {
                        prgDlg.dismiss();
                    }

                    if (conectionStatus == false) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(FileChooserExampleActivity.this);
                        builder.setTitle("Предупреждение..")
                                .setCancelable(false)
                                .setMessage("Нет связи с устройством...")
                                .setIcon(R.drawable.info)
                                .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //startMainActIntent();
                                    }
                                });
                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                }

                @Override
                public void onTick(long l) {
                    Log.v("ArduinoBT", "onTick");

                }
            }.start();
        }

    }
    //==============================================================================================
    void startMainActIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    void openBT() throws IOException {

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // Standard
        // SerialPortService
        // ID
        //if(mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();
        if (mmDevice != null) {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            //myLabel.setText("Подключение устройства");
            if (mmSocket.isConnected()) {
                Log.v("ArduinoBT", "Socket conected");
                mmOutputStream = mmSocket.getOutputStream();
                mmInputStream = mmSocket.getInputStream();
                if (prgDlg != null) {
                    prgDlg.dismiss();
                }
                conectedStatus = true;
                //resetModem();

                //beginListenForData();
            } else {
                Log.v("ArduinoBT", "Socket disconect");
            }
        } else {
            Log.v("ArduinoBT", "Device not found");
        }

    }
    //==============================================================================================
    public void startConnection() {
        if(ct==null){
            ct = new connectTask();
        }
        TaskHelper.execute(ct,"Start");

    }

    //==============================================================================================

    public class connectTask extends AsyncTask<String, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mBluetoothAdapter.isEnabled()) {
                if(!mBluetoothAdapter.isDiscovering()){
                    mBluetoothAdapter.startDiscovery();
                    Log.v("ArduinoBT", "ct startDiscovery");
                }

            }
            conectedStatus = false;
            foundProcess =true;
            showConnectDlg();
            Log.v("ArduinoBT", "ct onPreExecute");
        }
        @Override
        protected Integer doInBackground(String... params) {
            while(!conectionStatus)
            {
                if (conectionStatus) break;
                if(isCancelled())    break;

            }
            Log.v("ArduinoBT", "ct out doInBackground");
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            Log.v("ArduinoBT", "ct onPostExecute");
            if (conectionStatus) {
                try {
                    Log.v("ArduinoBT", "ct openBT");
                    openBT();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (prgDlg != null) {
                    prgDlg.dismiss();
                }
            }

            if(mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();
            if(ct!=null) ct=null;

        }

        @Override
        protected void onProgressUpdate(Integer...values) {
            super.onProgressUpdate(values);
            myLabel.setText("Поиск...");
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.v("ArduinoBT", "ct onCancelled");
        }


    }
        //==============================================================================================
        void closeBT() throws IOException {
            stopWorker = true;
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
                conectedStatus = false;
            }
            if (mmOutputStream != null) mmOutputStream.close();
            if (mmInputStream != null) mmInputStream.close();
            if (mmSocket != null) mmSocket.close();
            Log.v("ArduinoBT", "Socket closed");
        }
        //==============================================================================================

        void beginListenForData() {
            final Handler handler = new Handler();
            final byte delimiter = 10; // This is the ASCII code for a newline
            // character

            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];
            workerThread = new Thread(new Runnable() {
                public void run() {
                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                        try {
                            int bytesAvailable = mmInputStream.available();
                            if (bytesAvailable > 0) {
                                byte[] packetBytes = new byte[bytesAvailable];
                                mmInputStream.read(packetBytes);
                                for (int i = 0; i < bytesAvailable; i++) {
                                    byte b = packetBytes[i];
                                    if (b == delimiter) {
                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(readBuffer, 0,
                                                encodedBytes, 0,
                                                encodedBytes.length);
                                        final String data = new String(
                                                encodedBytes, "US-ASCII");
                                        readBufferPosition = 0;

                                        handler.post(new Runnable() {
                                            public void run() {
                                                if (data.equals("normal")) {
                                                    answerResult = true;
                                                    //myLabel.setText("Файл передан успешно");
                                                }
                                                if (data.equals("no normal")) {
                                                    //myLabel.setText("Файл передан с ошибкой");
                                                    answerResult = false;
                                                }
                                            }
                                        });
                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            stopWorker = true;
                        }
                    }
                }
            });

            workerThread.start();
        }


        //==============================================================================================
        public boolean conectionStatus() {
            if (mmDevice != null && mmSocket.isConnected()) {
                return true;
            } else {
                return false;
            }
        }

        //==============================================================================================
        public void onClickFileDialog() {
            showChooser();
        }

        //==============================================================================================
        public boolean connectStatus() {
            if (mmDevice != null && mmSocket.isConnected()) {
                return true;
            } else {
                return false;
            }
        }

        //==============================================================================================
        private void showChooser() {
            hideProgressDialog();
            if (conectionStatus == true) {
                showChooserAnother();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(FileChooserExampleActivity.this);
                builder.setTitle("Предупреждение..")
                        .setCancelable(false)
                        .setMessage("Нет связи с устройством...")
                        .setIcon(R.drawable.info)
                        .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        }

        //==============================================================================================
        private void showChooserAnother() {
            hideProgressDialog();
            Intent target = FileUtils.createGetContentIntent();
            // Create the chooser Intent
            Intent intent = Intent.createChooser(
                    target, getString(R.string.chooser_title));
            try {
                startActivityForResult(intent, RequestCode.REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
            }
        }

        //==============================================================================================
        FilenameFilter jefFilter = new FilenameFilter() {
            public boolean accept(File file, String name) {
                if (name.endsWith(".jef")) {
                    // filters files whose extension is .mp3
                    return true;
                } else {
                    return false;
                }
            }
        };
        //==============================================================================================
        File[] files = null;
        ProgressDialog pdn;

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case RequestCode.REQUEST_CODE:
                    // If the file selection was successful
                    if (resultCode == RESULT_OK) {
                        if (data != null) {
                            // Get the URI of the selected file
                            Uri uri = data.getData();
                            try {
                                // Get the file path from the URI
                                Log.v("ArduinoBT", "Return in File Manager");
                                String path = FileUtils.getPath(this, uri);
                                File dir = new File(path);
                                File dirNew = FileUtils.getPathWithoutFilename(dir);
                                files = dirNew.listFiles(jefFilter);
                                if (files.length == 0) {
                                    showADialog(pdn, false);
                                }
                                ResetTransferPacket();
                                sendPacket(0);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                case RequestCode.REQUEST_CODE_MY:
                    if (resultCode == RESULT_OK) {
                        if (conectionStatus == false) {
                            startConnection();
                        }
                    }
                    if (resultCode == RESULT_CANCELED) {
                        try {
                            closeBT();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Intent intent = new Intent(this, MainActivity.class);
                        startActivity(intent);

                    }
                    break;
            }
            super.onActivityResult(requestCode, resultCode, data);
        }

        public long sizeFiles = 0;
        //byte[] fileDataStr;
        byte[] fileDataStr = null;

        //==============================================================================================
        public void sendPacket(int fileNumber) throws IOException {
            File dirNew = FileUtils.getPathWithoutFilename(files[fileNumber]);
            sizeFiles = files.length;
            Log.v("ArduinoBT", String.valueOf(sizeFiles));
            //for (File aFile : files) {
            String root = dirNew.getPath();
            //String newRoot = root.replace("/", " ");
            String[] newRootAr = root.split("/");
            String fileName = files[fileNumber].getName();
            int size = newRootAr.length;
            fileDataStr = new byte[0];
            Arrays.fill(fileDataStr, (byte) 0);
            fileDataStr = fn.readFile(files[fileNumber].getPath());
            String arSize = Integer.toString(fileDataStr.length);
            String fullPath = newRootAr[size - 2] + "/" + newRootAr[size - 1] + "/" + fileName + "/" + arSize;

            fn.clearInputStream(mmInputStream, conectionStatus);
            fn.clearOutPutStream(mmOutputStream, conectionStatus);
            transferWithDelay(2000, 100, fullPath, fileDataStr, files[fileNumber].getPath());
        }

        //==============================================================================================
        public boolean taskStatus = false;

        public void showStatusDialog(String path, int size) {
            pd = new ProgressDialog(this);
            pd.setTitle("Отправка");
            pd.setCancelable(false);
            pd.setMessage(path);
            pd.setButton("Отмена", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    taskStatus = true;
                    pd.setIndeterminate(true);
                    if (mt != null) {
                        mt.cancel(true);
                        while (!mt.isCancelled()) ;
                        mt = null;
                    }
                    ResetTransferPacket();
                    hideProgressDialog();
                   // resetModem();
                    showCDialog(pd, false);
                }
            });
            // меняем стиль на индикатор
            pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            // устанавливаем максимум
            pd.setMax(size);
            // включаем анимацию ожидания
            pd.setIndeterminate(true);
            pd.setIcon(R.drawable.download);
            pd.show();
            pd.setIndeterminate(false);
        }

        //==============================================================================================
        public void hideProgressDialog() {
            if (pd != null) {
                pd.dismiss();
            }
        }

        //==============================================================================================
        public void resetModem() {
            fn.clearInputStream(mmInputStream, conectionStatus);
            fn.clearOutPutStream(mmOutputStream, conectionStatus);
            fn.sendData("RST".getBytes(), mmOutputStream, conectionStatus);
        }
        //==============================================================================================

        final static String endTransmision = "*";
        public static final byte BEL = 7;
        public static final byte LF = 10;
        public static final byte CR = 13;
        public transferTask mt;

        void transferWithDelay(int period, int msInTic, final String path, final byte[] Data, final String fullPath) {
            final String[] newRootArTransfer = path.split("/");
            //String fullPath = newRootAr[size-2] + " " + newRootAr[size-1] + " " + fileName + " " +arSize + " ";

            new CountDownTimer(period, msInTic) {
                int count = 0;
                int size;
                String strSize;
                String name;

                public void onTick(long millisUntilFinished) {
                    count++;
                    switch (count) {
                        case 1:
                            name = "PacketPath";
                            fn.sendData(name.getBytes(), mmOutputStream, conectionStatus);
                            //fn.sendData(BEL,mmOutputStream);
                            break;
                        case 3:
                            name = "DirName";
                            fn.sendData(name.getBytes(), mmOutputStream, conectionStatus);
                            break;
                        case 5:
                            fn.sendData(newRootArTransfer[0].getBytes(), mmOutputStream, conectionStatus);
                            fn.sendData(endTransmision.getBytes(), mmOutputStream, conectionStatus);
                            break;
                        case 7:
                            name = "SubDirName";
                            fn.sendData(name.getBytes(), mmOutputStream, conectionStatus);
                            break;
                        case 9:
                            fn.sendData(newRootArTransfer[1].getBytes(), mmOutputStream, conectionStatus);
                            fn.sendData(endTransmision.getBytes(), mmOutputStream, conectionStatus);

                            break;
                        case 11:
                            name = "FileName";
                            fn.sendData(name.getBytes(), mmOutputStream, conectionStatus);
                            break;
                        case 13:
                            fn.sendData(newRootArTransfer[2].getBytes(), mmOutputStream, conectionStatus);
                            fn.sendData(endTransmision.getBytes(), mmOutputStream, conectionStatus);

                            break;
                        case 15:
                            fn.sendData("PacketPathEnd".getBytes(), mmOutputStream, conectionStatus);
                            break;
                    }

                }

                public void onFinish() {
                    {
                        count = 0;
                        Log.v("ArduinoBT", "onFinish");
                        fn.clearInputStream(mmInputStream, conectionStatus);
                        showStatusDialog(fullPath, Data.length);
                        if (mt != null) mt = null;
                        if (mt == null) {
                            mt = new transferTask(pd, FileChooserExampleActivity.this);
                            Log.v("ArduinoBT", "Create transferTask");
                        }
                        if (mt != null) {
                            TaskHelper.execute(mt, Data);
                            Log.v("ArduinoBT", "Run transferTask");
                            Log.v("ArduinoBT", String.valueOf(Data.length));
                            Log.v("ArduinoBT", "count " + String.valueOf(count));
                            Log.v("ArduinoBT", "countFile " + String.valueOf(countFile));
                            Log.v("ArduinoBT", "sizeFiles " + String.valueOf(sizeFiles));
                        }
                    }
                }
            }.start();
        }
        //==============================================================================================

        public void ResetTransferPacket() {
            count = 0;
            countFile = 0;
            countBuf = 0;
            sizeFiles = 0;
            fn.clearInputStream(mmInputStream, conectionStatus);
            fn.clearOutPutStream(mmOutputStream, conectionStatus);
        }

        //==============================================================================================
        public boolean inTask = false;
        public int count = 0;
        public long countFile = 0;
        public int countBuf = 0;


        //==============================================================================================
        public void showCDialog(ProgressDialog pd, boolean state) {
            String msg;
            if (state == true) {
                msg = " Файл отправлен нормально\n Для продолжения нажмите \"Да\"\n Для отмены нажмите \"Нет\"";
            } else {
                msg = "Файл отправлен с ошибкой";
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(FileChooserExampleActivity.this);
            builder.setTitle("Информация..")
                    .setCancelable(false)
                    .setMessage(msg)
                    .setIcon(R.drawable.info)
                    .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                closeBT();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showChooser();

                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
            pd.dismiss();
        }

        //==============================================================================================
        private void showADialog(ProgressDialog pd, boolean state) {
            String msg;
            if (state == true) {
                msg = " Выбрали правильный тип файлов";
            } else {
                msg = "Выбрали неправильный тип файлов";
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(FileChooserExampleActivity.this);
            builder.setTitle("Информация..")
                    .setCancelable(false)
                    .setMessage(msg)
                    .setIcon(R.drawable.info)
                    .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                closeBT();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showChooser();

                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
            pd.dismiss();
        }
        //==============================================================================================

        public void sendData(View v) throws IOException {
            try {
                String msg = enterTextBox.getText().toString();
                //msg += "";
                //myLabel.setText("Data Sent " + msg);
                msg = msg;
                byte[] theByteArray = msg.getBytes();
                mmOutputStream.write(theByteArray);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }
    //==============================================================================================
