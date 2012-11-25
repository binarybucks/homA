package st.alr.homA;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;




public class ServerDialogPreference extends DialogPreference {
	private Context context;
	private EditText address;
	
    public ServerDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	    this.context = context;
		setDialogLayoutResource(R.layout.server_dialog_preferences);
	    
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        //persistBoolean(positiveResult);
    }


    @Override
    protected View onCreateDialogView() {
    Log.v(this.toString(), "onCreateDialogView");
      View root = super.onCreateDialogView();
      address = (EditText) root.findViewById(R.id.serverAddress);
      return root;
    }
    
    @Override
    protected void onBindDialogView(View view) {
        Log.v(this.toString(), "onBindDialogView");

      	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      	address.setText(prefs.getString("serverAddress", ""));
      	
      	
    }
    

    @Override
    public void onClick(DialogInterface dialog, int which) {
      switch(which) {
        case DialogInterface.BUTTON_POSITIVE: // User clicked OK!
            Log.v(this.toString(), "onClick BUTTON_POSITIVE");

        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        	SharedPreferences.Editor editor = prefs.edit();
        	editor.putString("serverAddress", address.getText().toString());
        	editor.apply();
        	
        	((App)context.getApplicationContext()).connectMqttOnMqttThread(true);

        break;
      }
      super.onClick(dialog, which);
    }
    
}