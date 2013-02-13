
package st.alr.homA;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Locale;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class NfcActivity extends Activity {
    NfcAdapter nfcAdapter;
    boolean writeMode =false;
    final Context context = this;
    Tag tag;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        setContentView(R.layout.activity_nfc);
        Button writeButton = (Button) findViewById(R.id.writeButton);
        writeButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
         
                
                    // set title
                    alertDialogBuilder.setTitle("Write NFC Tag");
         
                    // set dialog message
                    alertDialogBuilder
                        .setMessage("Place above NFC tag to write")
                        .setCancelable(true)   
                        .setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                // if this button is clicked, just close
                                // the dialog box and do nothing
                                dialog.cancel();
                                Log.v(this.toString(), "NFC tag write caanceled by user");
                                // TODO: Cancel write
                            }
                        });
         
                        // create alert dialog
                        AlertDialog alertDialog = alertDialogBuilder.create();
         
                        // show it
                        alertDialog.show();
                        Log.v(this.toString(), "Writing tag");
                        PendingIntent pendingIntent = PendingIntent.getActivity(NfcActivity.this, 0, new Intent(NfcActivity.this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
           
                        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
                        IntentFilter[] filters = new IntentFilter[] { tagDetected };
                        nfcAdapter.enableForegroundDispatch( NfcActivity.this, pendingIntent, filters, null);
  
            }

            });
    }    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_nfc, menu);
        return true;
    }
//    
//    private void enableWriteMode() {
//        writeMode = true;
///        }
//    
//    
    @Override
    protected void onNewIntent(Intent intent){
        Log.d(this.toString(), "Discovered tag with intent: " + intent);
        
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) && intent.getStringExtra("topic") != null && intent.getStringExtra("value") != null) {
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//            mText.setText("Wird beschrieben");
            new TagWriter().execute(intent.getStringExtra("topic"), intent.getStringExtra("value"));
        }       
    }
    
    
    private class TagWriter extends AsyncTask<String, String, Boolean> {

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            for (String value : values) {
                /*Toast.makeText(getApplicationContext(), value,
                        Toast.LENGTH_SHORT).show();*/
                //mText.setText(mText.getText()+"\n"+value);
                Log.v(this.toString(), "onProgressUpdate: " + value);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {

            if (result) {
                Toast.makeText(getApplicationContext(), "Tag beschrieben",  Toast.LENGTH_SHORT).show();
                Log.d(this.toString(), "result true");
                finish();
            } else {
                Toast.makeText(getApplicationContext(),
                        "Tag konnte nicht beschrieben werden :-(",
                        Toast.LENGTH_SHORT).show();
                Log.d(this.toString(), "result false");
            }
            super.onPostExecute(result);
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected Boolean doInBackground(String... text) {

            boolean result = true;

            if (tag != null && text.length > 0) {
                Ndef ndefTag = Ndef.get(tag);

                try {

                    ndefTag.connect();

                    if (ndefTag.isWritable()) {

                        // TODO:wenn package name unseres programms klar ist
                        // sollte das auf den tag geschrieben werden so dass
                        // unsere app bevorzugt ausgewählt wird
                        // NdefRecord data =
                        // NdefRecord.createApplicationRecord(packageName)

                        byte[] byteText = text[0].getBytes(Charset
                                .forName("UTF-8"));

                        NdefRecord data;
                        NdefRecord aar = NdefRecord
                                .createApplicationRecord("de.nsvb.writenfc");

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                            data = NdefRecord.createExternal("de.nsvb",
                                    "tagwriter", byteText);
                        } else {
                            data = createExternal("de.nsvb", "tagwriter",
                                    byteText);
                        }

                        NdefRecord records[] = new NdefRecord[1];//new NdefRecord[2];
                        records[0] = data;
                        //records[1] = aar;
                        NdefMessage ndefMsg = new NdefMessage(records);

                        if (ndefMsg.getByteArrayLength() > ndefTag.getMaxSize()) {
                            publishProgress("Nachricht zu lang: max Größe'"
                                    + ndefTag.getMaxSize()
                                    + "' - tatsächliche Größe'"
                                    + ndefMsg.getByteArrayLength() + "'");
                            return false;
                        }

                        if (ndefTag.isConnected()) {
                            try {
                                long now = System.currentTimeMillis();
                                ndefTag.writeNdefMessage(ndefMsg);
                                Log.d(this.toString(), "Write time: " + (System.currentTimeMillis() - now) + " ms");
                            } catch (TagLostException e) {
                                publishProgress("TagLostException");
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                result = false;
                                /*
                                 * Toast.makeText(getApplicationContext(),
                                 * "Tag ist nicht mehr da!", Toast.LENGTH_SHORT)
                                 * .show();
                                 */
                            } catch (FormatException e) {
                                publishProgress("FormatException--");
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                result = false;
                            }
                        }
                    }
                } catch (IOException e) {
                    result = false;
                    publishProgress("IOException--");
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    Log.d(this.toString(),
                            "IllegalArgumentException beim NDEF: "
                                    + e.toString());
                } finally {
                    try {
                        ndefTag.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        result = false;
                    }
                }

            } else {
                publishProgress("kein Tag erkannt");
                result = false;
            }

            return result;
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

    
    

}
