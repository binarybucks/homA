package st.alr.homA;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class LicenseDialogPreference extends DialogPreference {
    public LicenseDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.preferences_license);
    }


}