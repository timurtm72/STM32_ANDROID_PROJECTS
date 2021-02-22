package com.ipaulpro.afilechooserexample;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;

public class Functions {

    //==============================================================================================
    public void clearInputStream(InputStream mm,boolean status) {
        int bytesAvailable = 0;
        try {
            if(status)
            {
                bytesAvailable = mm.available();
                if (bytesAvailable > 0) {
                    byte[] packetBytes = new byte[bytesAvailable];
                    mm.read(packetBytes);
                    Arrays.fill(packetBytes, (byte) 0);
            }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    //==============================================================================================
    public void clearOutPutStream(OutputStream os,boolean status)
    {
        if (status) {
            try {
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    //==============================================================================================
    private String load(File target) throws IOException {
        String result = "";
        try {
            InputStream in = new FileInputStream(target);
            if (in != null) {
                try {
                    InputStreamReader tmp = new InputStreamReader(in);
                    BufferedReader reader = new BufferedReader(tmp);
                    String str;
                    StringBuilder buf = new StringBuilder();
                    while ((str = reader.readLine()) != null) {
                        buf.append(str);
                        buf.append("\n");
                    }
                    result = buf.toString();
                } finally {
                    in.close();
                }
            }
        } catch (java.io.FileNotFoundException e) {
// that's OK, we probably haven't created it yet
        }
        return (result);
    }

//==================================================================================================
    public byte[] read(File file) throws IOException {
        byte[] buffer = new byte[(int) file.length()];
        InputStream ios = null;
        try {
            ios = new FileInputStream(file);
            if (ios.read(buffer) == -1) {
                throw new IOException(
                        "EOF reached while trying to read the whole file");
            }
        } finally {
            try {
                if (ios != null)
                    ios.close();
            } catch (IOException e) {
            }
        }
        return buffer;
    }


//==================================================================================================
    public void sendData(byte[] data,OutputStream os,boolean status) {

        try {
            if (status) {
                for (byte dataBuffer : data) {

                    os.write(dataBuffer);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //==============================================================================================

    public void sendData(byte data,OutputStream os,boolean status) {

        try {
            if (status) {
                os.write(data);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //==============================================================================================
    public int readInStream(String containsString,InputStream is,boolean status) throws IOException {
        int bytesAvailable;
        String rs;
        byte[] packetBytes;

            while (status) {
                bytesAvailable = is.available();
                if (bytesAvailable >= (containsString.length())) {
                    packetBytes = new byte[bytesAvailable];
                    is.read(packetBytes);
                    rs = new String(packetBytes);
                    if (rs.contains(containsString) == true) {
                        Arrays.fill(packetBytes, (byte) 0);
                        rs = "";
                        return 1;
                    }
                }
            }

        return 0;
    }
    //==============================================================================================
    public static byte[] readFile(String fn)   throws IOException
    {
        File f = new File(fn);

        byte[] buffer = new byte[(int) f.length()];
        FileInputStream is = new FileInputStream(fn);
        is.read(buffer);
        is.close();

        return   buffer; // use desired encoding
    }
    //==============================================================================================
}