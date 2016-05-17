package com.ifingers.yunwb.utility;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.ifingers.yunwb.R;

/**
 *
 * Created by Macoo on 2/27/2016.
 */
public class GuiHelper {
    public static void setActionBarTitle(AppCompatActivity context, String title){
        //Customize the ActionBar
        ActionBar abar = context.getSupportActionBar();
        View viewActionBar = context.getLayoutInflater().inflate(R.layout.actionbar_title, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(//Center the textview in the ActionBar !
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        TextView textviewTitle = (TextView) viewActionBar.findViewById(R.id.actionbar_title);
        textviewTitle.setText(title);
        textviewTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        abar.setCustomView(viewActionBar, params);
        abar.setDisplayShowCustomEnabled(true);
        abar.setDisplayShowTitleEnabled(false);

        // for refreshing UI
        abar.setDisplayHomeAsUpEnabled(true);
        abar.setDisplayHomeAsUpEnabled(false);
    }
}
