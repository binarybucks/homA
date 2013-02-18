
package st.alr.homA;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import st.alr.homA.support.NfcRecordAdapter;

import android.R.menu;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.opengl.Visibility;
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
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NfcWriteActivity extends FragmentActivity {

    Context ctx;
    static boolean writeMode;
    WriteDialog writeDialog;
    protected Object mActionMode;
    public int selectedItem = -1;
    boolean isCABDestroyed = true;
    ListView listView;
    Menu menu; 
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_nfc, menu);
        this.menu = menu;
        return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                AddDialog addDialog = new AddDialog();
                addDialog.show(getSupportFragmentManager(), "addDialog");

                return true;
            case R.id.write:
                writeDialog = new WriteDialog();
                writeDialog.show(getSupportFragmentManager(), "writeDialog");
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
    
    
    
    
    
    
    
    private OnItemLongClickListener liListener = new OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {                
            isCABDestroyed = false;    
            return false; // so this action does not consume the event!!!
        }
    };

private MultiChoiceModeListener mcListener = new MultiChoiceModeListener() {
    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position,  long id, boolean checked) {
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
        isCABDestroyed = true; // mark readiness to switch back to SINGLE CHOICE after the CABis destroyed  

    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }
};


public boolean onPrepareOptionsMenu(Menu menu) {
    if(App.isRecording()) {
        startRecording();
    } else {
        stopRecording();
    }

    return true;
}

private void startRecording(){
    Log.v(this.toString(), "Recording of MQTT publishes started");
    App.startRecording();
    setRecordingIcons();
}

private void stopRecording(){
    Log.v(this.toString(), "Recording of MQTT publishes stopped");

    App.stopRecording();
    setRecordingIcons();

}

private void setRecordingIcons(){
    menu.findItem(R.id.recordingStart).setVisible(!App.isRecording());
    menu.findItem(R.id.recordingStop).setVisible(App.isRecording());
}

    
    
    
    
    
    
    
    
    
    
    
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);

        listView = (ListView)findViewById(R.id.nfcRecords);
        listView.setAdapter(App.getRecordMapListAdapter());
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        listView.setOnItemLongClickListener(liListener);
        listView.setMultiChoiceModeListener(mcListener);

        ctx = this;
        writeMode = false;

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

    
    
    public static class AddDialog extends DialogFragment {
        TextView topicInput;
        TextView payloadInput;
        
        private View getContentView() {            
            View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_nfc_add, null);
            topicInput= (TextView)view.findViewById(R.id.topicInput);

            payloadInput= (TextView)view.findViewById(R.id.paylodInput);

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
                    App.addToNfcRecordMap(topicInput.getText().toString(), payloadInput.getText().toString());
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

        
     

        
        
        private View getContentView() {
            View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_nfc_write, null);
            tv = (TextView)view.findViewById(R.id.writeTextView);
            
            adapter = NfcAdapter.getDefaultAdapter(getActivity());
            
            if (adapter == null || !adapter.isEnabled()) {
                
                tv.setText("Please enable NFC in your Phones settings to use this feature");
            } else {
                pendingIntent = PendingIntent.getActivity(getActivity(), 0, new Intent(getActivity(), getActivity().getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    
                IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
                tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
                writeTagFilters = new IntentFilter[] {
                    tagDetected
                };
    
                tv.setText("Place phone above tag to write");

            }

            
            return view;             
        }
        
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            NfcWriteActivity.writeMode = true;

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
            .setTitle("Write")
            .setView(getContentView())
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

            //String writeStr = topicInput.getText() + "=" + valueInput.getText();
            
            
            Map<String, String> map =  App.getRecordMapListAdapter().getMap();  
            StringBuffer text = new StringBuffer();
            for (String key : map.keySet()) {
                text.append(key + "=" + map.get(key) + ",");
            }
            text.deleteCharAt(text.length()-1);//strip last ","
            
            Log.v(this.toString(),text.toString());

            
            if (write(text.toString(), mytag)) {
                Log.v(this.toString(), "Write ok");
                publishProgress("Tag written successfully", true);

            } else {
                Log.e(this.toString(), "Write fail");

            }

        }
    }

}
