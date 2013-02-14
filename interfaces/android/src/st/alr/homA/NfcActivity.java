
package st.alr.homA;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Locale;

import st.alr.homA.MainActivity.DeviceFragment;
import st.alr.homA.model.Control;
import st.alr.homA.model.Device;
import st.alr.homA.model.Room;
import st.alr.homA.view.ControlView;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NfcActivity extends FragmentActivity {

    Context ctx;
    static boolean writeMode;
    WriteDialog writeDialog;

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);

        ctx=this;
        writeMode = false;
        
        Button btnWrite = (Button) findViewById(R.id.writeButton);

        btnWrite.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                writeDialog = WriteDialog.newInstance("foobar");
                writeDialog.show(getSupportFragmentManager(), "writeDialog");                             
            }
        });

        
    }
    private boolean write(String text, Tag tag)  {
        boolean success = true;
        
        Ndef ndefTag = Ndef.get(tag);
        NdefRecord aar = NdefRecord .createApplicationRecord("st.alr.homA");
        NdefMessage  ndefMsg;


        // Enable I/O
        try {
            
            NdefRecord[] records={ createRecord(text), aar };
            ndefMsg = new NdefMessage(records);
            if (ndefMsg.getByteArrayLength() > ndefTag.getMaxSize()) {
                publishProgress("Message is too long. Max size: " + ndefTag.getMaxSize() + ", current size: " + ndefMsg.getByteArrayLength(), false);
                return false;
            }
                   

      
            
            ndefTag.connect();
            ndefTag.writeNdefMessage(ndefMsg);            
     
   
        } catch (IOException e) {
            e.printStackTrace();
            publishProgress("Unable to write tag: IOException", false);
            success = false;
        } catch (FormatException e) {
            e.printStackTrace();
            publishProgress("Unable to write tag: FormatException", false);
            success = false;
        } finally {
            try {
                ndefTag.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return success;
    }



    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang       = "en";
        byte[] textBytes  = text.getBytes();
        byte[] langBytes  = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1,              langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        
        
      //  NdefRecord recordNFC = new NdefRecord(NdefRecord.,  NdefRecord.RTD_TEXT,  new byte[0], payload);
        NdefRecord recordNFC = NdefRecord.createExternal("st.alr.homa", "nfc", payload);
        
        return recordNFC;
    }
    

//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
//    private boolean write(String text, Tag tag) throws IOException, FormatException {
//
//        
//        boolean result = true;
//
//        if (tag != null && text.length() > 0) {
//            Ndef ndefTag = Ndef.get(tag);
//
//            try {
//
//                ndefTag.connect();
//
//                if (ndefTag.isWritable()) {
//
//                    // TODO:wenn package name unseres programms klar ist
//                    // sollte das auf den tag geschrieben werden so dass
//                    // unsere app bevorzugt ausgewŠhlt wird
//                    // NdefRecord data =
//                    // NdefRecord.createApplicationRecord(packageName)
//
//                    byte[] byteText = text.getBytes(Charset.forName("UTF-8"));
//
//                    NdefRecord data;
//                    NdefRecord appRecord = NdefRecord.createApplicationRecord("st.alr.homA");
//                    
//                    
//                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
//                        data = NdefRecord.createExternal("st.alr", "tagwriter", byteText);
//                    } else {
//                        data = createExternal("st.alr", "tagwriter",  byteText);
//                    }
//
//                    NdefMessage ndefMsg = new NdefMessage(new NdefRecord[] {data, appRecord});
//
//                    if (ndefMsg.getByteArrayLength() > ndefTag.getMaxSize()) {
//                        writeDialog.setText("Message to long. Maximum size is: "
//                                + ndefTag.getMaxSize()
//                                + ". Current size is: "
//                                + ndefMsg.getByteArrayLength() + "'");
//                        return false;
//                    }
//
//                    if (ndefTag.isConnected()) {
//                        try {
//                            long now = System.currentTimeMillis();
//                            ndefTag.writeNdefMessage(ndefMsg);
//                            Log.v(this.toString(), "Write time: " + (System.currentTimeMillis() - now) + " ms");
//                        } catch (TagLostException e) {
//                            writeDialog.setText("The tag was lost. Please try again.");
//                            e.printStackTrace();
//                            result = false;
//                        } catch (FormatException e) {
//                            writeDialog.setText("Format exception");
//                            e.printStackTrace();
//                            result = false;
//                        }
//                    }
//                }
//            } catch (IOException e) {
//                result = false;
//                writeDialog.setText("IOException");
//                e.printStackTrace();
//            } catch (IllegalArgumentException e) {
//
//                Log.e(this.toString(),  "IllegalArgumentException: " + e.toString());
//            } finally {
//                try {
//                    ndefTag.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    result = false;
//                }
//            }
//
//        } else {            
//            writeDialog.setText("The tag was lost. Please try again.");
//            result = false;
//        }
//
//        return result;
//        
////        NdefRecord[] records = { createRecord(text) };
////        NdefMessage  message = new NdefMessage(records);
////        // Get an instance of Ndef for the tag.
////        Ndef ndef = Ndef.get(tag);
////        // Enable I/O
////        ndef.connect();
////        // Write the message
////        ndef.writeNdefMessage(message);
////        // Close the connection
////        ndef.close();
//    }
//
//
//
//
//
//    
//    
//    
    
    public static class WriteDialog extends DialogFragment {
        String payload;
        TextView tv;
        
        NfcAdapter adapter;
        PendingIntent pendingIntent;
        IntentFilter writeTagFilters[];
        boolean writeMode;
        Tag mytag;
        
        static WriteDialog newInstance(String payload) {
            WriteDialog f = new WriteDialog();
            Bundle args = new Bundle();
            args.putString("payload", payload);

            f.setArguments(args);
            return f;
        }



        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            NfcActivity.writeMode = true;

            adapter = NfcAdapter.getDefaultAdapter(getActivity());
            pendingIntent = PendingIntent.getActivity(getActivity(), 0, new Intent(getActivity(), getActivity().getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
            writeTagFilters = new IntentFilter[] { tagDetected };

            payload = getArguments().getString("payload");

            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Write NFC Tag");
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();                    
                }
            });
            LinearLayout view = new LinearLayout(this.getActivity());
            tv = new TextView(getActivity());
            tv.setText("Place phone above tag to write");            
            view.addView(tv);                    
            builder.setView(view);
            return builder.create();
        }
        
        public void setText(String text) {
            tv.setText(text);
            tv.requestLayout();
        }
        
        @Override
        public void onPause(){
            super.onPause();
            WriteModeOff();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            WriteModeOff();
            NfcActivity.writeMode = false;
        }
        
        @Override
        public void onResume(){
            super.onResume();
            WriteModeOn();
        }

        private void WriteModeOn(){
            adapter.enableForegroundDispatch(getActivity(), pendingIntent, writeTagFilters, null);
        }

        private void WriteModeOff(){
            adapter.disableForegroundDispatch(getActivity());
        }        
    }
    
    
    
    private void publishProgress(String message, boolean success) {
        writeDialog.setText(message);
    }
    
    @Override
    protected void onNewIntent(Intent intent){
        Log.v(this.toString(), "write mode: " + writeMode);
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) && writeMode == true){
            Tag mytag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);    
            Log.v(this.toString(), "Detected tag: " + mytag.toString());

            if(write("foobar", mytag)) {
                    Log.v(this.toString(), "Write ok");
                    publishProgress("Tag written successfully", true);
                    
                } else {
                    Log.e(this.toString(), "Write fail");

                }

        }
    }
    
    @Deprecated
    private NdefRecord createExternal(String domain, String type,
            byte[] data) {
        if (domain == null)
            throw new NullPointerException("domain is null");
        if (type == null)
            throw new NullPointerException("type is null");

        domain = domain.trim().toLowerCase(Locale.US);
        type = type.trim().toLowerCase(Locale.US);

        if (domain.length() == 0)
            throw new IllegalArgumentException("domain is empty");
        if (type.length() == 0)
            throw new IllegalArgumentException("type is empty");

        byte[] byteDomain = domain.getBytes(Charset.forName("UTF_8"));
        byte[] byteType = type.getBytes(Charset.forName("UTF_8"));
        byte[] b = new byte[byteDomain.length + 1 + byteType.length];
        System.arraycopy(byteDomain, 0, b, 0, byteDomain.length);
        b[byteDomain.length] = ':';
        System.arraycopy(byteType, 0, b, byteDomain.length + 1, byteType.length);

        return new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE, b, null, data);
    }
}

