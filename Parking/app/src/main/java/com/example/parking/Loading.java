package com.example.parking;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class Loading extends AppCompatActivity {
    Runnable runnable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loading);

        ThemeModifier tm = new ThemeModifier((ImageView) findViewById(R.id.loading_imageview));
        tm.start();

       runnable = new Runnable() {

            @Override
            public void run() {
                StartA();
            }
        };



    }

    void StartA() {
        startActivity(new Intent(this, ActivityA.class));
    }




    class ThemeModifier extends Thread implements Runnable
    {
        ImageView iv;
        float f=0.0f;


        ThemeModifier(ImageView iv)
        {
            this.iv=iv;
        }
        @Override
        public void run()
        {
            FirebaseController.onStarter();

            while(true)
            {


                try {
                    iv.setAlpha(f);
                    f+=0.005f;


                    this.sleep(10);


                    if(FirebaseController.loading_state_value >=0.999)
                    {
                        runOnUiThread(runnable);

                        break;


                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


        }


    }


}
