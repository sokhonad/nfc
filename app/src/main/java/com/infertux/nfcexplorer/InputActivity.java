package com.infertux.nfcexplorer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class InputActivity extends AppCompatActivity {
;




    @SuppressLint({"WrongViewCast", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);

        EditText nameField = (EditText) findViewById(R.id.name_field);
        Button submitButton = (Button) findViewById(R.id.submit_button);
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        Toast.makeText(InputActivity.this, " inscrivez vous", Toast.LENGTH_SHORT).show();

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = nameField.getText().toString();
                Intent intent = getIntent();
                String tagId = intent.getStringExtra("key");
                boolean query =dbHelper.insertData(name,tagId);
                if (query){
                    Toast.makeText(InputActivity.this, "votre inscription a bien été prise en compte", Toast.LENGTH_SHORT).show();
                    Toast.makeText(InputActivity.this, "Scan votre carte", Toast.LENGTH_SHORT).show();
                    finishAffinity();
                }
            }
        });

    }
}