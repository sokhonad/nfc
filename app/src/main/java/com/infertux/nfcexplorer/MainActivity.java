package com.infertux.nfcexplorer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity {
    static private ArrayList<TagWrapper> tags = new ArrayList<TagWrapper>();
    static private int currentTagIndex = -1;

    private NfcAdapter adapter = null;
    private PendingIntent pendingIntent = null;

    private TextView currentTagView;
    private ExpandableListView expandableListView;

    private float touchDownX, touchUpX;
    private String name=null;

    @Override
    public void onCreate(final Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.activity_main);

        currentTagView = (TextView) findViewById(R.id.currentTagView);
        currentTagView.setText("Loading...");

        expandableListView = (ExpandableListView) findViewById(R.id.expandableListView);
        expandableListView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final float swipeThreshold = 150;

                switch(event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        touchDownX = event.getX();
                        break;

                    case MotionEvent.ACTION_UP:
                        touchUpX = event.getX();
                        final float deltaX = touchUpX - touchDownX;

                        if (deltaX > swipeThreshold) {
                            showPreviousTag();
                        } else if (deltaX < -swipeThreshold) {
                            showNextTag();
                        }

                        break;
                }

                return false;
            }
        });

        adapter = NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!adapter.isEnabled()) {
            Utils.showNfcSettingsDialog(this);
            return;
        }

        if (pendingIntent == null) {
            pendingIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

            currentTagView.setText("Scan a tag");
        }

        showTag();

        adapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        adapter.disableForegroundDispatch(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.d("onNewIntent", "Discovered tag with intent " + intent);

        Tag tag = intent.getParcelableExtra(adapter.EXTRA_TAG);
        String tagId = Utils.bytesToHex(tag.getId());
        if (tagId!=null){
;            DatabaseHelper dbHelper = new DatabaseHelper(this);
             name = dbHelper.getNameByTag(tagId);
            if (name==null){
                Intent intent1 = new Intent(this, InputActivity.class);
                intent1.putExtra("key", tagId);
                startActivity(intent1);
            }

        }
        TagWrapper tagWrapper = new TagWrapper(tagId);

        ArrayList<String> misc = new ArrayList<String>();
        misc.add("scanned at: " + Utils.now());

        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        String tagData = "";

        if (rawMsgs != null) {

            NdefMessage msg = (NdefMessage) rawMsgs[0];
            NdefRecord cardRecord = msg.getRecords()[0];
            try {
                tagData = readRecord(cardRecord.getPayload());
            } catch (UnsupportedEncodingException e) {
                Log.e("TagScan", e.getMessage());
                return;
            }
        }

        misc.add("tag data: " + tagData);
        tagWrapper.techList.put("Misc", misc);

        for (String tech : tag.getTechList()) {
            tech = tech.replace("android.nfc.tech.", "");
            List<String> info = getTagInfo(tag);
            tagWrapper.techList.put("", info);
        }

        if (tags.size() == 1) {
            Toast.makeText(this, "Swipe right to see previous tags", Toast.LENGTH_LONG).show();
        }

        tags.add(tagWrapper);
        currentTagIndex = tags.size() - 1;
        showTag();
    }

    String readRecord(byte[] payload) throws UnsupportedEncodingException {
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

        int languageCodeLength = payload[0] & 63;

        return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
    }


    private void showPreviousTag() {
        if (--currentTagIndex < 0) currentTagIndex = tags.size() - 1;

        showTag();
    }

    private void showNextTag() {
        if (++currentTagIndex >= tags.size()) currentTagIndex = 0;

        showTag();
    }

    private void showTag() {
        if (tags.size() == 0) return;

        final TagWrapper tagWrapper = tags.get(currentTagIndex);
        final TagTechList techList = tagWrapper.techList;
        final ArrayList<String> expandableListTitle = new ArrayList<String>(techList.keySet());

        expandableListView.setAdapter(
                new CustomExpandableListAdapter(this, expandableListTitle, techList));

        final int count = expandableListView.getCount();
        for (int i = 0; i < count; i++) expandableListView.expandGroup(i);

        currentTagView.setText("Tag " + tagWrapper.getId() +
                " (" + (currentTagIndex+1) + "/" + tags.size() + ")");
    }

    private final List<String> getTagInfo(final Tag tag) {
        List<String> info = new ArrayList<String>();
        NfcA nfcATag = NfcA.get(tag);
        info.add("ID: " + Arrays.toString(nfcATag.getTag().getId()));
        info.add("name: " +name);
        return info;
    }
}