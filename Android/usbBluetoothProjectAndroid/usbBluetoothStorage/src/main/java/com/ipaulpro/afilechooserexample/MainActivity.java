package com.ipaulpro.afilechooserexample;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity  {
    ImageView startBtnImg;
    Animation anim = null;
    TextView stText;
    EditText editBLu;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stText = (TextView) findViewById(R.id.stTxt);
        startBtnImg = (ImageView) findViewById(R.id.startBtnImg);
        editBLu = (EditText) findViewById(R.id.editBlu);

    }
    public void edBLUClick(View v){
        editBLu.setText("HC-05");
    }

    public void startNewActivity(View view) {

        if(editBLu.getText().toString().isEmpty()) {
            Toast.makeText(this, "Введите имя ВТ устройства", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(this, FileChooserExampleActivity.class);
            intent.putExtra("BTname", editBLu.getText().toString());
            startActivity(intent);
            finish();
        }

    }
    public void setAnim()
    {
        anim = AnimationUtils.loadAnimation(this, R.anim.mycombo);
        startBtnImg.startAnimation(anim);
        anim = AnimationUtils.loadAnimation(this, R.anim.mycombo);
        stText.startAnimation(anim);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setAnim();
    }

}
