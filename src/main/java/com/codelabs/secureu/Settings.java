package com.codelabs.secureu;

import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.ContactsContract;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class  Settings extends PreferenceActivity {

    Preference about;
    Preference add;
    Preference feedback;
    Preference terms;
    Preference view;
    UpdateDB dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.pref_general);

        LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.activity_settings, root, false);
        bar.setTitle("Settings");
        bar.setLogo(getResources().getDrawable(R.mipmap.ic_launcher));
        root.addView(bar, 0);


        dbHelper = new UpdateDB(this);

        add = getPreferenceManager().findPreference("add");
        view = getPreferenceManager().findPreference("view");
        terms = getPreferenceManager().findPreference("terms");
        feedback = getPreferenceManager().findPreference("feedback");
        about = getPreferenceManager().findPreference("about");

        add.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(
                                "android.intent.action.PICK",
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI),
                        1);
                return true;
            }
        });

        view.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                viewContacts();
                return true;
            }
        });

        terms.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showTerms();
                return true;
            }
        });

        feedback.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String[] send = new String[]{"mail.codelabs@gmail.com"};
                Intent mail = new Intent("android.intent.action.SEND");
                mail.setType("message/rfc822");
                mail.putExtra("android.intent.extra.EMAIL", send);
                Settings.this.startActivity(Intent.createChooser(mail, "Send mail..."));
                return true;
            }
        });

        about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Settings.this, About.class);
                startActivity(intent);
                return true;
            }
        });

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == -1) {
            Cursor c = getContentResolver().query(data.getData(), null, null, null, null);
            c.moveToFirst();

            int columnID = c.getColumnIndex("data1");
            int nameID = c.getColumnIndex("display_name");

            String number = c.getString(columnID);
            String name = c.getString(nameID);

            dbHelper.insert(name, number);
        }

    }

    private void viewContacts() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.contact_list);
        dialog.setTitle("Saved Contacts");

        ListView contactList = (ListView) dialog.findViewById(R.id.contacts_list);

        ArrayList<String> list = new ArrayList();
        dialog.show();
        String[] data = dbHelper.extract();

        if(data == null) {
            dialog.dismiss();
            Toast.makeText(Settings.this, "No contacts saved!", Toast.LENGTH_SHORT).show();
            return;
        }
        else
            Toast.makeText(Settings.this, "Long press to delete contacts", Toast.LENGTH_SHORT).show();

        for(String temp : data)
            list.add(temp);

        ArrayAdapter<String> adapter = new ArrayAdapter(Settings.this,
                R.layout.activity_listview,
                list);

        contactList.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        contactList.setOnItemLongClickListener(new ListClick(list, adapter));
    }

    private void showTerms() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_terms);
        dialog.setTitle("Terms and Conditions");

        TextView view = (TextView) dialog.findViewById(R.id.terms_text);
        InputStream text = getResources().openRawResource(R.raw.eula);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            int i = 0;
            while(i != -1){
                i = text.read();
                output.write(i);
            }

            text.close();
            String toWrite = output.toString();
            Log.d("Settings", toWrite);
            if(toWrite != null)
                view.setText(toWrite);
        } catch (IOException e) {
            e.printStackTrace();
        }
        dialog.show();
    }

    private class ListClick implements AdapterView.OnItemLongClickListener {
        private ArrayAdapter adapter;
        private ArrayList list;

        public ListClick(ArrayList<String> list, ArrayAdapter<String> adapter) {
            this.list = list;
            this.adapter = adapter;
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            try {
                String item = (String) parent.getItemAtPosition(position);
                String[] temp = item.split(": ");

                dbHelper.remove(temp[1]);
                list.remove(item);

                adapter.notifyDataSetChanged();
                Toast.makeText(Settings.this, new StringBuilder(String.valueOf(item)).append(" Contact deleted.").toString(), Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Toast.makeText(Settings.this, e.toString(), Toast.LENGTH_LONG).show();
            }
            return true;
        }
    }
}

