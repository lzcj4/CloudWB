package com.ifingers.yunwb;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class AlertDialogActivity extends Activity {

    private Context context;
    private String title;
    private String message;
    private String negativeButton;
    private String positiveButton;
    private DialogInterface.OnClickListener negativeListener;
    private DialogInterface.OnClickListener positiveListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_dialog);
        //getActionBar().hide();

        this.title = getIntent().getStringExtra("title");
        this.message = getIntent().getStringExtra("message");
        this.negativeButton = getIntent().getStringExtra("negativeButton");
        this.positiveButton = getIntent().getStringExtra("positiveButton");

        TextView titleView = (TextView) findViewById(R.id.alert_title);
        titleView.setText(title);

        TextView msg = (TextView) findViewById(R.id.alert_msg);
        msg.setText(message);

//        this.negativeListener = negativeListener;
//        this.positiveListener = positiveListener;

        Button ok = (Button) findViewById(R.id.alert_ok);
        ok.setText(positiveButton);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //positiveListener.onClick(null, 1);
                finish();
            }
        });
        Button cancel = (Button) findViewById(R.id.alert_cancel);
        if (negativeButton == null || "".equals(negativeButton)){
            cancel.setVisibility(View.GONE);
        } else {
            cancel.setVisibility(View.VISIBLE);
            cancel.setText(positiveButton);
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }
}
