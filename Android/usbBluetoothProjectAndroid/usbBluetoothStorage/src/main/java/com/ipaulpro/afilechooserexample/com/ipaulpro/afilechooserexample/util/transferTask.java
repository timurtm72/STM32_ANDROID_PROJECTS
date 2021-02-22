package com.ipaulpro.afilechooserexample.com.ipaulpro.afilechooserexample.util;


import android.app.ProgressDialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;

import com.ipaulpro.afilechooserexample.FileChooserExampleActivity;
import com.ipaulpro.afilechooserexample.Functions;

import java.io.IOException;

public   class transferTask extends AsyncTask<byte[], Integer, Integer> {
    FileChooserExampleActivity fs;
    Functions fn = new Functions();
    public final int BUFFER_SIZE = 1023;
    ProgressDialog pdn;
    public transferTask(ProgressDialog pd,FileChooserExampleActivity fs) {
        this.pdn = pd;
        this.fs = fs;
    }



    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        fs.inTask=true;
        if(fs.conectionStatus) {
            fs.myLabel.setText("Файл отправляется");
            fn.sendData("FileTransfer".getBytes(), fs.mmOutputStream,fs.conectionStatus);
            fs.countFile++;
            Log.v("ArduinoBT", "Start file transmision");
            Log.v("ArduinoBT", "isCanceled "+String.valueOf(this.isCancelled()));
            Status r = this.getStatus();
            Log.v("ArduinoBT", "Status "+String.valueOf(r));
            Log.v("ArduinoBT", "BTstatus "+String.valueOf(fs.conectionStatus));

        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        Log.v("ArduinoBT", "OnCancelled");
    }

    @Override
    protected Integer doInBackground(byte[]... params) {
        byte[] data = new byte[params[0].length];
        data = params[0];
        int cnt = 0;

        try{
            if(!fs.conectionStatus)return null;
            if(fs.conectionStatus) {
                for (byte dataBuffer : data) {
                    publishProgress(cnt++);
                    if(fs.taskStatus||isCancelled()){
                        fs.taskStatus=false;
                        break;
                    }
                    if (fs.conectionStatus) {
                            fs.mmOutputStream.write(dataBuffer);
                    }
                    if ((fs.count++) >= (BUFFER_SIZE - 1)) {
                        fs.count = 0;
                        //if(fileSize>=data.length) break;
                        fs.countBuf++;
                        fn.clearInputStream(fs.mmInputStream, fs.conectionStatus);
                        fn.sendData("End".getBytes(), fs.mmOutputStream, fs.conectionStatus);
                        fn.readInStream("ACK", fs.mmInputStream, fs.conectionStatus);
                        fn.sendData("FileTransfer".getBytes(), fs.mmOutputStream, fs.conectionStatus);
                        Log.v("ArduinoBT", "Counter in doInBackground = " + fs.countBuf);
                        if(fs.taskStatus||isCancelled()){
                            fs.taskStatus=false;
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if(isCancelled()) return 0;
        }
        return 1;
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);

        if(result==0||isCancelled())return;
        if (fs.conectionStatus) {
            final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_RING, 500);
            fs.count = 0;
            Log.v("ArduinoBT", "onPostExecute");
            try {
                fn.clearInputStream(fs.mmInputStream,fs.conectionStatus);
                fn.sendData("Stop".getBytes(), fs.mmOutputStream,fs.conectionStatus);
                Log.v("ArduinoBT", "Stop");
                fn.readInStream("ACK", fs.mmInputStream,fs.conectionStatus);
                if(!fs.conectionStatus)return ;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(result!=1)return;
            }
            fs.myLabel.setText("Файл отправлен");
            Log.v("ArduinoBT", "End file transmision");
            //--------------------------------------------------------------------------------------
            if (fs.countFile >= fs.sizeFiles) {
                fs.countFile = 0;
                fs.hideProgressDialog();
                fs.showCDialog(pdn, true);
                fs.resetModem();
                for (int i = 0; i < 10; i++) {
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP2);
                    }
                fs.resetModem();
            } else {
                try {
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP2);
                    SystemClock.sleep(600);
                    fs.hideProgressDialog();
                    fs.sendPacket((int) fs.countFile);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if(result==0)return;
                }

            }

        }
        fs.inTask=false;
    }

    @Override
    protected void onProgressUpdate (Integer...values){
        super.onProgressUpdate(values);
        pdn.setProgress(values[0]);
        if(!fs.conectionStatus)return ;
    }

}
//==================================================================================================
