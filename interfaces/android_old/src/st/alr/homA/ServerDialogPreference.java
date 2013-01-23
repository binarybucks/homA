package st.alr.homA;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class ServerDialogPreference extends DialogPreference {
	private Context context;
	private EditText address;
	private EditText port;

	public ServerDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;

		setDialogLayoutResource(R.layout.server_dialog_preferences);

	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
	}

	@Override
	protected View onCreateDialogView() {
		View root = super.onCreateDialogView();
		address = (EditText) root.findViewById(R.id.serverAddress);
		port = (EditText) root.findViewById(R.id.serverPort);

		return root;
	}

	@Override
	protected void onBindDialogView(View view) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		address.setText(prefs.getString("serverAddress", "192.168.8.2"));
		port.setText(prefs.getString("serverPort", "1883"));
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
			case DialogInterface.BUTTON_POSITIVE: // User clicked ok

				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
				SharedPreferences.Editor editor = prefs.edit();

				String oldAddress = prefs.getString("serverAddress", "192.168.8.2");
				String oldPort = prefs.getString("serverPort", "1883");

				String newAdress = address.getText().toString();
				String newPort = port.getText().toString();

				editor.putString("serverAddress", newAdress);
				editor.putString("serverPort", newPort);

				editor.apply();

				if (!oldAddress.equals(newAdress) || !oldPort.equals(newPort)) {
					Log.v(toString(), "Server address changed");
					Intent i = new Intent(App.SERVER_SETTINGS_CHANGED);
					context.sendBroadcast(i);
					App.bootstrapAndConnectMqtt(true, false); // Server changed, clean
														// up everything from
														// old server
				} else {
					App.bootstrapAndConnectMqtt(false, false); // Connect to same
														// server, no cleanup
														// needed
				}

				break;
		}
		super.onClick(dialog, which);
	}

}