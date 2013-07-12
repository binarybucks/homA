
package st.alr.homA;

import de.greenrobot.event.EventBus;

import st.alr.homA.model.Quickpublish;
import st.alr.homA.support.Defaults;
import st.alr.homA.support.Events.QuickpublishAdded;
import st.alr.homA.support.QuickpublishAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ActivityQuickpublish extends FragmentActivity {
    private ListView listView;
    private Menu menu;
    private QuickpublishAdapter listAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quickpublish);

        listAdapter = new QuickpublishAdapter(this, Quickpublish.fromPreferences(this, Defaults.SETTINGS_KEY_QUICKPUBLISH_NOTIFICATION));
        listView = (ListView) findViewById(R.id.quickpublishList);
        listView.setAdapter(listAdapter);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(multiChoiceListener);

        EventBus.getDefault().register(this);
    }
    
    public void onEventMainThread(QuickpublishAdded event) {
        listAdapter.add(event.getQuickpublish());
        save();
    }

    
    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
    
    public void save() {
        Quickpublish.toPreferences(this, Defaults.SETTINGS_KEY_QUICKPUBLISH_NOTIFICATION, listAdapter.getValues());
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_quickpublish, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                AddDialog addDialog = new AddDialog();
                getFragmentManager().beginTransaction().add(addDialog, "addDialog").commit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
                    mode.setTitle(getResources().getString(R.string.actionModeOneSelected));
                    break;
                default:
                    mode.setTitle(checkedCount + getResources().getString(R.string.actionModeMoreSelected));
                    break;
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.discard:
                    listAdapter.remove(listView.getCheckedItemPositions());
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

    public static class AddDialog extends DialogFragment {
        TextView topicInput;
        TextView payloadInput;
        CheckBox retainedCheckbox;

        private View getContentView() {
            View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_nfc_add, null);
            topicInput = (TextView) view.findViewById(R.id.topicInput);

            payloadInput = (TextView) view.findViewById(R.id.paylodInput);
            retainedCheckbox = (CheckBox) view.findViewById(R.id.retainedCheck);
            
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
            
            return view;
        }

        
        
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(getResources().getString(R.string.add))
                    .setView(getContentView())
                    .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    })
                    .setPositiveButton(getResources().getString(R.string.add), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Quickpublish q = new Quickpublish(topicInput.getText().toString(), payloadInput.getText().toString(), retainedCheckbox.isChecked());
                            EventBus.getDefault().post(new st.alr.homA.support.Events.QuickpublishAdded(q));
                            dismiss();
                        }
                    });
            
        

            Dialog dialog = builder.create();
            dialog.setOnShowListener(new OnShowListener() {
                
                @Override
                public void onShow(DialogInterface dialog) {
                    getDialog().findViewById(android.R.id.button1).setEnabled(false);
                    
                }
            });
            return dialog;
        }

    }
}
