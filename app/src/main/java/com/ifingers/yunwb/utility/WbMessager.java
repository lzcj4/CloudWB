package com.ifingers.yunwb.utility;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.widget.TextView;

import com.ifingers.yunwb.AlertDialogActivity;

/**
 * Created by macoo on 1/24/2016.
 */
public class WbMessager {

    public static final int DIALOG_RESULT = 1;
    /**
     * show message box
     * @param context
     * @param titleText
     * @param message
     * @param negativeButton
     * @param positiveButton
     * @param negativeListener
     * @param positiveListener
     */
    public static void show(AppCompatActivity context, String titleText, String message, String negativeButton,
                            String positiveButton, DialogInterface.OnClickListener negativeListener, DialogInterface.OnClickListener positiveListener){
//        Intent it = new Intent(context, AlertDialogActivity.class);
//        it.putExtra("title", title);
//        it.putExtra("message", message);
//        it.putExtra("negativeButton", negativeButton);
//        it.putExtra("positiveButton", positiveButton);
//
//        context.startActivityForResult(it, DIALOG_RESULT);
        if (context.isFinishing())
            return;

        TextView title = new TextView(context);
        // You Can Customise your Title here
        title.setText(titleText);
        title.setPadding(10, 10, 10, 10);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(3355443);
        title.setTextSize(20);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleText).
                setMessage(message).
                setCancelable(false).
                setNegativeButton(negativeButton, negativeListener).
                setPositiveButton(positiveButton, positiveListener);

        AlertDialog alertDialog = builder.create();

//        TextView messageText = (TextView)alertDialog.findViewById(android.R.id.message);
//        messageText.setGravity(Gravity.CENTER);
//        messageText.setTextColor(6710886);

        alertDialog.show();
    }
}
