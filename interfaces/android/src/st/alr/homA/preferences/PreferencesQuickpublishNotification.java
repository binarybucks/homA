package st.alr.homA.preferences;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import st.alr.homA.App;
import st.alr.homA.R;
import st.alr.homA.model.Quickpublish;
import st.alr.homA.services.ServiceMqtt;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class PreferencesQuickpublishNotification extends DialogPreference {
    TextView topicInput;
    TextView payloadInput;
    CheckBox retainedCheckbox;

    public PreferencesQuickpublishNotification(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preferences_quickpublish);
    }

    @Override
    protected View onCreateDialogView() {
        View root = super.onCreateDialogView();
        topicInput = (TextView) root.findViewById(R.id.quickpublishTopicInput);
        payloadInput = (TextView) root.findViewById(R.id.quickpublishPayloadInput);
        retainedCheckbox = (CheckBox) root.findViewById(R.id.quickpublishRetainedCheckbox);
        return root;
    }

    @Override
    protected void onBindDialogView(View view) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        topicInput.setText(prefs.getString("serverAddress", ""));
        payloadInput.setText(prefs.getString("serverPort", ""));
    }
    
    @Override
    protected void showDialog(Bundle state) {
           super.showDialog(state);

           //TODO: validate exisitig input conditionallyEnableSaveButton();
        
          topicInput.addTextChangedListener(new TextWatcher() {
          
          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}
          
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
          
          @Override
          public void afterTextChanged(Editable s) {

              View v = getDialog().findViewById(android.R.id.button1);
              if(v == null)
                  return; 
              
              if(s.toString().length() > 0)
                  v.setEnabled(true);
              else
                  v.setEnabled(false);
    
          }
      });

      
        
    }



    @Override
    public void onClick(DialogInterface dialog, int which) {
//        switch (which) {
//            case DialogInterface.BUTTON_POSITIVE: // User clicked ok
//                //TODO: save
//                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//                SharedPreferences.Editor editor = prefs.edit();
//
//                String newAdress = address.getText().toString();
//                String newPort = port.getText().toString();
//
//                editor.putString("serverAddress", newAdress);
//                editor.putString("serverPort", newPort);
//
//                editor.apply();
//                break;
//            case DialogInterface.BUTTON_NEGATIVE:
//
//        }
        super.onClick(dialog, which);
    }
}
