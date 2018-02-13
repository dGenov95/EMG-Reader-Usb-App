package com.example.dimitar.usbtestapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;


public class TermsAndConditionsActivity extends Activity {

    private CheckBox agreeCheckbox;
    private Button proceedButton;
    private TextView termsAndConditionsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_conditions);

        agreeCheckbox =(CheckBox) findViewById(R.id.agreeCheckBox);
        proceedButton = (Button) findViewById(R.id.proceedButton);
        termsAndConditionsText = (TextView) findViewById(R.id.termsText);

        termsAndConditionsText.setMovementMethod(new ScrollingMovementMethod());

        enableProceedButton(false);

        agreeCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(agreeCheckbox.isChecked()){
                    enableProceedButton(true);
                }
            }
        });
    }

    public void onClickProceed(View view){
        Intent proceedIntent = new Intent(this,MainActivity.class);
        startActivity(proceedIntent);
        finish();
    }

    private void enableProceedButton(boolean status){
        proceedButton.setEnabled(status);
    }
}
