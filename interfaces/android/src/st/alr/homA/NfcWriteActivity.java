
package st.alr.homA;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NfcWriteActivity extends FragmentActivity {

    Context ctx;
    static boolean writeMode;
    WriteDialog writeDialog;
    TextView valueInput;
    TextView topicInput;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);

        ctx = this;
        writeMode = false;

        Button btnWrite = (Button) findViewById(R.id.writeButton);
        valueInput = (TextView) findViewById(R.id.valueInput);
        topicInput = (TextView) findViewById(R.id.topicInput);

        btnWrite.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                writeDialog = WriteDialog.newInstance();
                writeDialog.show(getSupportFragmentManager(), "writeDialog");
            }
        });

    }

    private boolean write(String text, Tag tag) {
        boolean success = true;

        Ndef ndefTag = Ndef.get(tag);
        NdefRecord aar = NdefRecord.createApplicationRecord("st.alr.homA");
        NdefMessage ndefMsg;

        // Enable I/O
        try {

            NdefRecord[] records = {
                    createRecord(text), aar
            };
            ndefMsg = new NdefMessage(records);
            if (ndefMsg.getByteArrayLength() > ndefTag.getMaxSize()) {
                publishProgress("Message is too long. Max size: " + ndefTag.getMaxSize()
                        + ", current size: " + ndefMsg.getByteArrayLength(), false);
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
        byte[] textBytes = text.getBytes();
        NdefRecord recordNFC = NdefRecord.createExternal("st.alr.homa", "nfc", textBytes);
        return recordNFC;
    }

    public static class WriteDialog extends DialogFragment {
        TextView tv;

        NfcAdapter adapter;
        PendingIntent pendingIntent;
        IntentFilter writeTagFilters[];
        boolean writeMode;
        Tag mytag;

        static WriteDialog newInstance() {
            WriteDialog f = new WriteDialog();
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            NfcWriteActivity.writeMode = true;
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LinearLayout view = new LinearLayout(this.getActivity());
            tv = new TextView(getActivity());

            
            adapter = NfcAdapter.getDefaultAdapter(getActivity());
            
            if (adapter == null || !adapter.isEnabled()) {
                
                builder.setTitle("NFC disabled");
                tv.setText("Please enable NFC in your Phones settings to use this feature");

            } else {
                pendingIntent = PendingIntent.getActivity(getActivity(), 0, new Intent(getActivity(), getActivity().getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    
                IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
                tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
                writeTagFilters = new IntentFilter[] {
                    tagDetected
                };
    
                builder.setTitle("Write NFC Tag");
                tv.setText("Place phone above tag to write");

            }
            
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();
                }
            });
            view.addView(tv);
            builder.setView(view);
            return builder.create();
        }

        public void setText(String text) {
            tv.setText(text);
            tv.requestLayout();
        }

        @Override
        public void onPause() {
            super.onPause();
            WriteModeOff();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            WriteModeOff();
            NfcWriteActivity.writeMode = false;
        }

        @Override
        public void onResume() {
            super.onResume();
                WriteModeOn();

        }

        private void WriteModeOn() {
            if(adapter != null && adapter.isEnabled()) {

                adapter.enableForegroundDispatch(getActivity(), pendingIntent, writeTagFilters, null);
            }

        }

        private void WriteModeOff() {
            if(adapter != null && adapter.isEnabled()) {
                adapter.disableForegroundDispatch(getActivity());                
            }
            
        }
    }

    private void publishProgress(String message, boolean success) {
        writeDialog.setText(message);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.v(this.toString(), "write mode: " + writeMode);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) && writeMode == true) {
            Tag mytag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Log.v(this.toString(), "Detected tag: " + mytag.toString());

            String writeStr = topicInput.getText() + "=" + valueInput.getText();
            Log.v(this.toString(), "Will write: " + writeStr);
            if (write(writeStr, mytag)) {
                Log.v(this.toString(), "Write ok");
                publishProgress("Tag written successfully", true);

            } else {
                Log.e(this.toString(), "Write fail");

            }

        }
    }

}
