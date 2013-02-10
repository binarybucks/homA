package st.alr.homA;

import st.alr.homA.support.Events;
import de.greenrobot.event.EventBus;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

public class ServerDialogPreference extends DialogPreference {
	private Context context;
	private EditText address;
	private EditText port;

	public ServerDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;

		setDialogLayoutResource(R.layout.preferences_server);

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
		address.setText(prefs.getString("serverAddress", context.getString(R.string.defaultsServerAddress)));
		port.setText(prefs.getString("serverPort", context.getString(R.string.defaultsServerPort)));
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
			case DialogInterface.BUTTON_POSITIVE: // User clicked ok

				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
				SharedPreferences.Editor editor = prefs.edit();

				String oldAddress = prefs.getString("serverAddress", context.getString(R.string.defaultsServerAddress));
				String oldPort = prefs.getString("serverPort", context.getString(R.string.defaultsServerPort));

				String newAdress = address.getText().toString();
				String newPort = port.getText().toString();

				editor.putString("serverAddress", newAdress);
				editor.putString("serverPort", newPort);

				editor.apply();

				MqttService.getInstance().reconnect();
				break;
			case DialogInterface.BUTTON_NEGATIVE:
			    MqttService.getInstance().disconnect();
		}
		super.onClick(dialog, which);
	}

}