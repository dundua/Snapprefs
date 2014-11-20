package com.marz.snapprefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.marz.snapprefs.ColorPickerDialog;

public class Listener extends GestureDetector.SimpleOnGestureListener {
	private static Context SnapContext;
	private static EditText editText;
	
	public Listener(Context context, EditText editText) {
		SnapContext = context;
		Listener.editText = editText;
	}
	
	public boolean onDoubleTap(MotionEvent motionEvent){
		AlertDialog.Builder builder = new AlertDialog.Builder(SnapContext);
		builder.setTitle("SnapPrefs Colour Manager");
		LinearLayout linearLayout = new LinearLayout(SnapContext);
		linearLayout.setOrientation(1);
		Button button_txtcolour = new Button(SnapContext);
		button_txtcolour.setText("Text Colour");
		button_txtcolour.setOnClickListener(new OnClickListener(){
			public void onClick(View view){
				ColorPickerDialog colorPickerDialog = new ColorPickerDialog(Listener.SnapContext, 1, new ColorPickerDialog.OnColorSelectedListener() {
					
					@Override
					public void onColorSelected(int color) {
						// TODO Auto-generated method stub
						Listener.editText.setTextColor(color);
					}
				});
				/* colorPickerDialog.setButton(3, "Default", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialogInterface, int which) {
						// TODO Auto-generated method stub
						Listener.editText.setTextColor(1);
					}
				}); */
				colorPickerDialog.setTitle("Text Colour");
				colorPickerDialog.show();
			}
		});
		
		Button button_bgcolour = new Button(SnapContext);
		button_bgcolour.setText("Background Colour");
		button_bgcolour.setOnClickListener(new OnClickListener(){
			public void onClick(View view){
				ColorPickerDialog colorPickerDialog = new ColorPickerDialog(Listener.SnapContext, 1, new ColorPickerDialog.OnColorSelectedListener() {
					
					@Override
					public void onColorSelected(int color) {
						// TODO Auto-generated method stub
						Listener.editText.setBackgroundColor(color);
					}
				});
				/* colorPickerDialog.setButton(3, "Default", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialogInterface, int which) {
						// TODO Auto-generated method stub
						Listener.editText.setBackgroundColor(0);
					}
				}); */
				colorPickerDialog.setTitle("Background Colour");
				colorPickerDialog.show();
			}
		});
		linearLayout.addView((View)button_txtcolour);
		linearLayout.addView((View)button_bgcolour);
		builder.setView((View)linearLayout);
		builder.setPositiveButton("Done", null);
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
			}
		});
		builder.show();
		return true;
	}
}
