package com.test.eyedis;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SettingActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private Switch flash_swi, focus_swi, gpu_swi, voice_swi;
    private boolean flash, focus,gpu, voice;
    private Button button, data_button, pri_button;
    private Dialog pDialog, info_Dialog;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_screen);

        button = (Button) findViewById(R.id.saveState);
        data_button = (Button) findViewById(R.id.privacy_button);

        pDialog = new Dialog(this);
        pDialog.setCanceledOnTouchOutside(true);
        info_Dialog = new Dialog(this);
        info_Dialog.setCanceledOnTouchOutside(true);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        flash_swi = (Switch) findViewById(R.id.flash_switch);
        focus_swi = (Switch) findViewById(R.id.focus_switch);
        gpu_swi = (Switch) findViewById(R.id.gpu_switch);
        voice_swi = (Switch) findViewById(R.id.voice_switch);

        flash = getIntent().getBooleanExtra("flash", false);
        focus = getIntent().getBooleanExtra("focus", false);
        gpu = getIntent().getBooleanExtra("gpu", false);
        voice = getIntent().getBooleanExtra("voice", false);

        if (flash) {
            flash_swi.setChecked(true);
        }
        if (focus) {
            focus_swi.setChecked(true);
        }
        if (gpu) {
            gpu_swi.setChecked(true);
        }
        if (voice) {
            voice_swi.setChecked(true);
        }

        if (flash_swi != null) {
            flash_swi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (flash_swi.isChecked()) {
                        Toast.makeText(getBaseContext(),"Flash is on", Toast.LENGTH_SHORT).show();
                    }  else  {
                        Toast.makeText(getBaseContext(),"Flash is off", Toast.LENGTH_SHORT).show();
                    }



                }
            });
        }
        if (focus_swi != null) {
            focus_swi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (focus_swi.isChecked()) {
                        Toast.makeText(getBaseContext(),"Auto Focus is on", Toast.LENGTH_SHORT).show();
                    }  else  {
                        Toast.makeText(getBaseContext(),"Auto Focus is off", Toast.LENGTH_SHORT).show();
                    }

                }
            });
        }
        if (gpu_swi != null) {
            gpu_swi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (gpu_swi.isChecked()) {
                        Toast.makeText(getBaseContext(),"using GPU", Toast.LENGTH_SHORT).show();
                    }  else  {
                        Toast.makeText(getBaseContext(),"using CPU", Toast.LENGTH_SHORT).show();
                    }

                }
            });
        }
        if (voice_swi != null) {
            voice_swi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (voice_swi.isChecked()) {
                        Toast.makeText(getBaseContext(),"voice-out enabled", Toast.LENGTH_SHORT).show();
                    }  else  {
                        Toast.makeText(getBaseContext(),"voice-out disabled", Toast.LENGTH_SHORT).show();
                    }

                }
            });
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                i.putExtra("flash", flash_swi.isChecked());
                i.putExtra("focus", focus_swi.isChecked());
                i.putExtra("gpu", gpu_swi.isChecked());
                i.putExtra("voice", voice_swi.isChecked());
                startActivity(i);
                finish();
            }
        });

        data_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(pDialog != null) {
                    pDialog.setContentView(R.layout.data_privacy);
                    pDialog.getWindow()
                            .setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    pDialog.show();
                }
            }
        });
    }

    public void closePrivacyDialog(View v) {
        if (pDialog != null) {
            pDialog.dismiss();
        }
    }

    public void showInfoDialog(View v) {
        if (info_Dialog != null) {
            info_Dialog.setContentView(R.layout.app_info);
            info_Dialog.getWindow()
                    .setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            info_Dialog.show();
        }
    }

    public void closeInfoDialog(View v) {
        info_Dialog.dismiss();
    }
}
