 package com.saishachhabria.theseefooddiet;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

 public class ItemDescription extends AppCompatActivity {

    static int item = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_description);

        TextView textView = (TextView) findViewById(R.id.itemName);
        TextView quantity = (TextView) findViewById(R.id.quantityValue);
        TextView calorie = (TextView) findViewById(R.id.calorieValue);

        Intent intent = getIntent();
        final int pos = ((Intent) intent).getIntExtra("item",-1);
        item = pos;
        if(pos != -1)
        {
            textView.setText(MainActivity.arrayList.get(pos));
            quantity.setText(MainActivity.quantityList.get(pos));
            calorie.setText(MainActivity.calorieList.get(pos));
        }
    }
     public void changeQuant(View view)
     {
         final TextView quantity = (TextView) findViewById(R.id.quantityValue);
         AlertDialog.Builder bb = new AlertDialog.Builder(this);
         bb.setTitle("Enter quantity:");
         final EditText input = new EditText(this);
         input.setInputType(InputType.TYPE_CLASS_NUMBER);
         bb.setView(input);
         bb.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
             @Override
             public void onClick(DialogInterface dialog, int which) {
                 TextView quantity = (TextView) findViewById(R.id.quantityValue);
                 quantity.setText(input.getText().toString());
             }
         });
         bb.show();
     }

     public void confirm(View view)
     {
         TextView quantity = (TextView) findViewById(R.id.quantityValue);

         int previousQuantity = Integer.parseInt(MainActivity.quantityList.get(item));
         int cal = Integer.parseInt(MainActivity.calorieList.get(item));
         int newQuantity = Integer.parseInt(quantity.getText().toString());

         int total = MainActivity.total;

         total  = total - (cal*previousQuantity) + (cal*newQuantity);

         MainActivity.quantityList.set(item,quantity.getText().toString());

         SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.saishachhabria.theseefooddiet",Context.MODE_PRIVATE);
         try {
             sharedPreferences.edit().putString("Quantities",ObjectSerializer.serialize(MainActivity.quantityList)).apply();
         } catch (IOException e) {
             e.printStackTrace();
         }
         Intent intent = new Intent();
         intent.putExtra("Cal",Integer.toString(total));
         setResult(6,intent);
         finish();
     }
     public void goBack(View view)
     {

     }
}
