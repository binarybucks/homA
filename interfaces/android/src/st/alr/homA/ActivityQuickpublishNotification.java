
package st.alr.homA;

import de.greenrobot.event.EventBus;

import st.alr.homA.ActivityMain.DeviceFragment;
import st.alr.homA.model.Quickpublish;
import st.alr.homA.support.Defaults;
import st.alr.homA.support.Events;
import st.alr.homA.support.QuickpublishAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class ActivityQuickpublishNotification extends FragmentActivity {
    private ListView listView;
    private Menu menu;
    private QuickpublishAdapter listAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quickpublish);

        listAdapter = new QuickpublishAdapter(this, Quickpublish.fromPreferences(this, Defaults.SETTINGS_KEY_QUICKPUBLISH_NOTIFICATION));
        listView = (ListView) findViewById(R.id.records);
        listView.setAdapter(listAdapter);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(multiChoiceListener);
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                
                AddDialog addDialog = AddDialog.newInstance(position);
                getFragmentManager().beginTransaction().add(addDialog, "addDialog").commit();

            }
        });
     }
    

    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    protected void add(Quickpublish q){
        listAdapter.add(q);
        save();
    }
    
    protected void update(Quickpublish q){
        save();
        listAdapter.notifyDataSetChanged();
    }
    
    
    protected void remove(SparseBooleanArray sba) {
        listAdapter.remove(sba);
        save();
    }
    
    private void save() {
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
    
    private QuickpublishAdapter getListAdapter(){
        return this.listAdapter;
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
                    remove(listView.getCheckedItemPositions());
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
        TextView nameInput;
        TextView topicInput;
        TextView payloadInput;
        CheckBox retainedCheckbox;
        Quickpublish q;
        
        

        private View getContentView() {
            Log.v(this.toString(), "getContentView");
            View view = getActivity().getLayoutInflater().inflate(R.layout.preferences_quickpublish, null);
            nameInput = (TextView) view.findViewById(R.id.quickpublishNameInput);
            topicInput = (TextView) view.findViewById(R.id.quickpublishTopicInput);
            payloadInput = (TextView) view.findViewById(R.id.quickpublishPayloadInput);
            retainedCheckbox = (CheckBox) view.findViewById(R.id.quickpublishRetainedCheckbox);
            
            if(q != null) {
                nameInput.setText(q.getName());
                topicInput.setText(q.getTopic());
                payloadInput.setText(q.getPayload());
                retainedCheckbox.setChecked(q.isRetained());
            } 
            
            topicInput.addTextChangedListener(new TextWatcher() {
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void afterTextChanged(Editable s) {
                    conditionallyEnableSaveButton();
                }
            });
            
            return view;
        }


        
        public static AddDialog newInstance(int position) {
            AddDialog f = new AddDialog();
                Bundle args = new Bundle();
                args.putInt("position", position);
                f.setArguments(args);
                return f;
            
        }

        private void conditionallyEnableSaveButton() {
            View v = getDialog().findViewById(android.R.id.button1);

             if(v == null)
                return; 
                
            
            if(topicInput.getText().toString().length() > 0) 
                v.setEnabled(true);
                        else
                v.setEnabled(false);                    
        }
            
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Log.v(this.toString(), "onCreateDialog");

            Bundle b;
            if (savedInstanceState != null)
                b = savedInstanceState;
            else
                b = getArguments();

            if(b != null)
                q = ((ActivityQuickpublishNotification) getActivity()).getListAdapter().getItem(b.getInt("position"));
            
            
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(getResources().getString(q == null? R.string.quickpublishAdd : R.string.quickpublishEdit))
                    .setView(getContentView())
                    .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    })
                    .setPositiveButton(getResources().getString(q != null ? R.string.save : R.string.add), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(q != null) {
                                Log.v(this.toString(), "updating qp");
                                q.setName(nameInput.getText().toString());
                                q.setTopic(topicInput.getText().toString());
                                q.setPayload(payloadInput.getText().toString());
                                q.setRetained(retainedCheckbox.isChecked());
                                ((ActivityQuickpublishNotification) getActivity()).update(q);
                            } else {
                                Log.v(this.toString(), "adding qp");
                                Quickpublish q = new Quickpublish(nameInput.getText().toString(), topicInput.getText().toString(), payloadInput.getText().toString(), retainedCheckbox.isChecked()); 
                                ((ActivityQuickpublishNotification) getActivity()).add(q);

                            }
                            dismiss();
                        }
                    });
            
        

            Dialog dialog = builder.create();
            dialog.setOnShowListener(new OnShowListener() {
                
                @Override
                public void onShow(DialogInterface dialog) {
                    conditionallyEnableSaveButton();

                }
            });
            return dialog;
        }

    }
}
