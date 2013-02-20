
package st.alr.homA;

import java.io.IOException;
import java.util.Map;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NfcWriteActivity extends FragmentActivity {
    private static boolean writeMode;
    private ListView listView;
    private Menu menu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);

        listView = (ListView) findViewById(R.id.nfcRecords);
        listView.setAdapter(App.getRecordMapListAdapter());
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);

        listView.setMultiChoiceModeListener(multiChoiceListener);

        writeMode = false;

    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_nfc, menu);
        this.menu = menu;
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (App.isRecording()) {
            startRecording();
        } else {
            stopRecording();
        }

        return true;
    }
   
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                AddDialog addDialog = new AddDialog();
                getFragmentManager().beginTransaction().add(addDialog, "addDialog").commit();
                return true;
            case R.id.write:
                WriteDialog writeDialog = new WriteDialog();
                getFragmentManager().beginTransaction().add(writeDialog, "writeDialog").commit();
                return true;
            case R.id.recordingStart:
                startRecording();
                return true;

            case R.id.recordingStop:
                stopRecording();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startRecording() {
        Log.v(this.toString(), "Recording of MQTT publishes started");
        App.startRecording();
        setRecordingIcons();
    }

    private void stopRecording() {
        Log.v(this.toString(), "Recording of MQTT publishes stopped");

        App.stopRecording();
        setRecordingIcons();

    }

    private void setRecordingIcons() {
        menu.findItem(R.id.recordingStart).setVisible(!App.isRecording());
        menu.findItem(R.id.recordingStop).setVisible(App.isRecording());
    }

    private MultiChoiceModeListener multiChoiceListener = new MultiChoiceModeListener() {
        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                boolean checked) {
            final int checkedCount = listView.getCheckedItemCount();
            switch (checkedCount) {
                case 0:
                    mode.setTitle(null);
                    break;
                case 1:
                    mode.setTitle("One item selected");
                    break;
                default:
                    mode.setTitle(checkedCount + " items selected");
                    break;
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.discard:
                    App.getRecordMapListAdapter().remove(listView.getCheckedItemPositions());
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.activity_nfc_actionmode, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    };

    private boolean write(String text, Tag tag) {
        boolean success = true;

        NdefRecord aar = NdefRecord.createApplicationRecord("st.alr.homA");
        NdefRecord[] records = { createRecord(text), aar };
        NdefMessage ndefMsg = new NdefMessage(records);

        
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();

                if (!ndef.isWritable()) {
                    publishProgress("Tag is read-only.", false);
                    success = false;
                }
                if (ndef.getMaxSize() < ndefMsg.getByteArrayLength()) {
                    publishProgress("Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + ndefMsg.getByteArrayLength() + " bytes.", false);
                    success = false;
                }

                ndef.writeNdefMessage(ndefMsg);
                publishProgress("Wrote message to pre-formatted tag.", true);
                success = true;
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(ndefMsg);
                        publishProgress("Formatted tag and wrote message.", true);
                        success = true;
                    } catch (IOException e) {
                        publishProgress("Failed to format tag.", false);
                        success = false;
                    }
                } else {
                    publishProgress("Tag doesn't support NDEF.", false);
                    success = false;
                }
            }
        } catch (Exception e) {
            Log.e(this.toString(), "Failed to write tag ("+ e.getMessage()+")", e);
        }

        return success;
    }

    private NdefRecord createRecord(String text) {
        byte[] textBytes = text.getBytes();
        NdefRecord recordNFC = NdefRecord.createExternal("st.alr.homa", "nfc", textBytes);
        return recordNFC;
    }
        
    @Override
    protected void onNewIntent(Intent intent) {
        Log.v(this.toString(), "write mode: " + writeMode);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) && writeMode == true) {
            Tag mytag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Log.v(this.toString(), "Detected tag: " + mytag.toString());
            Log.v(this.toString(), "techlist:" + mytag.getTechList());

            
            Map<String, String> map = App.getRecordMapListAdapter().getMap();
            if (map.size() < 1) {
                publishProgress("No content to write", false);
                return;
            }
            
            
            
            
            
            StringBuffer text = new StringBuffer();
            for (String key : map.keySet()) {
                text.append(key + "=" + map.get(key) + ",");
            }
            text.deleteCharAt(text.length() - 1);// strip last ","

            Log.v(this.toString(), text.toString());

            if (write(text.toString(), mytag)) {
                Log.v(this.toString(), "Write ok");
                publishProgress("Tag written successfully", true);

            } else {
                Log.e(this.toString(), "Write fail");
            }
        }
    }

    private void publishProgress(String message, boolean success) {
        WriteDialog f = (WriteDialog ) (getFragmentManager().findFragmentByTag("writeDialog"));
        Log.v(this.toString(), "publishProgress to fragment: " + f);
        if (f != null) {
            f.setText(message);            
        }
    }    

    public static class AddDialog extends DialogFragment {
        TextView topicInput;
        TextView payloadInput;

        private View getContentView() {
            View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_nfc_add, null);
            topicInput = (TextView) view.findViewById(R.id.topicInput);

            payloadInput = (TextView) view.findViewById(R.id.paylodInput);

            return view;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle("Add")
                    .setView(getContentView())
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    })
                    .setPositiveButton("Add", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            App.addToNfcRecordMap(topicInput.getText().toString(), payloadInput
                                    .getText().toString());
                            dismiss();
                        }
                    });
            Dialog dialog = builder.create();
            return dialog;
        }

    }

    public static class WriteDialog extends DialogFragment {
        TextView tv;
        View view;
        NfcAdapter adapter;
        PendingIntent pendingIntent;
        IntentFilter writeTagFilters[];
        Tag mytag;

        
        
        private View getContentView(Bundle savedInstance) {
            Log.v(this.toString(), "getContentView: " + savedInstance);
            String savedMessage = savedInstance != null ? savedInstance.getString("savedMessage") : null;
            
            View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_nfc_write, null);
            tv = (TextView) view.findViewById(R.id.writeTextView);

            adapter = NfcAdapter.getDefaultAdapter(getActivity());

            if (adapter == null || !adapter.isEnabled()) {
                tv.setText(savedMessage != null ? savedMessage : "Please enable NFC in your Phones settings to use this feature");
            } else {
                pendingIntent = PendingIntent.getActivity(getActivity(), 0, new Intent(
                        getActivity(), getActivity().getClass())
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

                IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
                tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
                writeTagFilters = new IntentFilter[] {
                        tagDetected
                };

                tv.setText(savedMessage != null ? savedMessage : "Place phone above a NFC tag to write");
            }

            return view;
        }
        
        public void onStart () {
            super.onStart();
            Log.v(this.toString(), "WriteFragment onStart. Tag is " + getTag());
        }


        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            NfcWriteActivity.writeMode = true;

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle("Write")
                    .setView(getContentView(savedInstanceState))
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    });
            Dialog dialog = builder.create();
            return dialog;
        }

        public void setText(String text) {
            tv.setText(text);
            tv.requestLayout();
        }

        @Override
        public void onPause() {
            WriteModeOff();
            super.onPause();
        }

        @Override
        public void onResume() {
            super.onResume();
            WriteModeOn();
        }

        private void WriteModeOn() {
            if (adapter != null && adapter.isEnabled() && pendingIntent != null && writeTagFilters != null) {
                adapter.enableForegroundDispatch(getActivity(), pendingIntent, writeTagFilters, null);
            }
        }

        private void WriteModeOff() {
            Log.v(this.toString(), "activit: " + getActivity());
            if (adapter != null && adapter.isEnabled() && getActivity() != null) {
                adapter.disableForegroundDispatch(getActivity());                
            }
        }
        
        public void onSaveInstanceState (Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString("savedMessage", (String) tv.getText());
        }


    }
}
