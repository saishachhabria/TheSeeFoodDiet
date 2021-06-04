package com.saishachhabria.theseefooddiet;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import android.content.SharedPreferences;

import org.w3c.dom.Text;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int CAMERA_REQUEST = 1888;

    private static final int INPUT_SIZE = 299;
    private static final int IMAGE_MEAN = 0;
    private static final float IMAGE_STD = 255;
    private static final String INPUT_NAME = "Placeholder";
    private static final String OUTPUT_NAME = "output";

    private static final String MODEL_FILE = "file:///android_asset/output_graph.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/output_labels.txt";

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();
    private TextView textViewResult;
    private ImageView imageViewResult;
    SharedPreferences sharedPreferences;

    static ArrayList<String> arrayList = new ArrayList<String>();
    static ArrayList<String> quantityList = new ArrayList<String>();
    static ArrayList<String> calorieList = new ArrayList<String>();
    static ArrayAdapter<String> arrayAdapter;
    static List scoreList;

    static int total;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        InputStream inputStream = getResources().openRawResource(R.raw.calorie_count);
        CSVFile csvFile = new CSVFile(inputStream);
        scoreList = csvFile.read();

        sharedPreferences = this.getSharedPreferences("com.saishachhabria.theseefooddiet",Context.MODE_PRIVATE);
        try {
            arrayList = (ArrayList<String>)(ObjectSerializer.deserialize(sharedPreferences.getString("FoodItems",ObjectSerializer.serialize(new ArrayList<String>()))));
            quantityList = (ArrayList<String>)(ObjectSerializer.deserialize(sharedPreferences.getString("Quantities",ObjectSerializer.serialize(new ArrayList<String>()))));
            calorieList = (ArrayList<String>)(ObjectSerializer.deserialize(sharedPreferences.getString("Calories",ObjectSerializer.serialize(new ArrayList<String>()))));
            total = sharedPreferences.getInt("CalorieCount",0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final TextView calCount = (TextView)findViewById(R.id.calCount);
        calCount.setText(Integer.toString(total));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCamera(view);
            }
        });

        final ListView itemList = (ListView) findViewById(R.id.itemList);

        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrayList);

        itemList.setAdapter(arrayAdapter);

        itemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(),ItemDescription.class);
                intent.putExtra("item",position);
                //startActivity(intent);
                startActivityForResult(intent,6);
            }
        });

        itemList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final int i = position;
                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_delete)
                        .setTitle("Are you sure you want to delete this meal? ")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                total -= Integer.parseInt(quantityList.get(i)) * Integer.parseInt(calorieList.get(i));
                                TextView calCount = (TextView)findViewById(R.id.calCount);
                                calCount.setText(Integer.toString(total));
                                arrayList.remove(i);
                                quantityList.remove(i);
                                calorieList.remove(i);
                                arrayAdapter.notifyDataSetChanged();
                                sharedPreferences = MainActivity.this.getSharedPreferences("com.saishachhabria.theseefooddiet",Context.MODE_PRIVATE);
                                try {
                                    sharedPreferences.edit().putString("FoodItems",ObjectSerializer.serialize(arrayList)).apply();
                                    sharedPreferences.edit().putString("Quantities",ObjectSerializer.serialize(quantityList)).apply();
                                    sharedPreferences.edit().putString("Calories",ObjectSerializer.serialize(calorieList)).apply();
                                    sharedPreferences.edit().putInt("CalorieCount",total).apply();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                        .setNegativeButton("No",null)
                        .show();

                return true;
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        initTensorFlowAndLoadModel();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    public void openSettings(View view) {

    }

    public void help(View view) {

    }
    public void contactus(View view) {

    }
    public void openCamera(View view){
        Snackbar.make(view, "Take a photo!", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);

            final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
            final CharSequence[] items = new CharSequence[results.size()];
            int count = 0, x = 0;
            while (count < results.size()) {
                String s = results.get(count).toString();
                int i = 0;
                while (!(Character.isLetter(s.charAt(i))))
                    i++;
                s = Character.toUpperCase(s.charAt(i)) + s.substring(i + 1);
                count++;
                items[x++] = s;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Confirm food item:");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    int i = 0;
                    for (i = 0; i < items[item].length(); i++)
                        if (items[item].charAt(i) == '(')
                            break;
                    arrayList.add(items[item].toString().substring(0, i-1));
                    arrayAdapter.notifyDataSetChanged();

                    String it = arrayList.get(arrayList.size() - 1);
                    for (int y = 0; y < scoreList.size(); y++) {
                        String s[] = (String[]) scoreList.get(y);
                        if (s[0].equalsIgnoreCase(it))
                            calorieList.add(s[1]);
                    }

                    try {
                        sharedPreferences.edit().putString("FoodItems", ObjectSerializer.serialize(arrayList)).apply();
                        sharedPreferences.edit().putString("Calories", ObjectSerializer.serialize(calorieList)).apply();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    AlertDialog.Builder bb = new AlertDialog.Builder(MainActivity.this);
                    bb.setTitle("Enter quantity:");
                    final EditText input = new EditText(MainActivity.this);

                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    bb.setView(input);
                    bb.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (input.getText().toString() != null)
                                quantityList.add(input.getText().toString());
                            else
                                quantityList.add("1");
                            total += Integer.parseInt(quantityList.get(quantityList.size() - 1)) * Integer.parseInt(calorieList.get(calorieList.size() - 1));
                            sharedPreferences = MainActivity.this.getSharedPreferences("com.saishachhabria.theseefooddiet", Context.MODE_PRIVATE);
                            try {
                                sharedPreferences.edit().putString("Quantities", ObjectSerializer.serialize(quantityList)).apply();
                                sharedPreferences.edit().putInt("CalorieCount", total).apply();
                                TextView calCount = (TextView) findViewById(R.id.calCount);
                                calCount.setText(Integer.toString(total));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(MainActivity.this, "Meal Added!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    bb.show();
                }
            });
            builder.show();

        } else if (requestCode == 6) {
            TextView calCount = (TextView)findViewById(R.id.calCount);
            String s = data.getStringExtra("Cal");
            MainActivity.total = Integer.parseInt(s);
            sharedPreferences = MainActivity.this.getSharedPreferences("com.saishachhabria.theseefooddiet", Context.MODE_PRIVATE);
            sharedPreferences.edit().putInt("CalorieCount",MainActivity.total).apply();
            Toast.makeText(MainActivity.this, "New calorie count: "+Integer.toString(MainActivity.total), Toast.LENGTH_SHORT).show();
            calCount.setText(Integer.toString(total));
        }
        else {
        }

    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE,
                            IMAGE_MEAN,
                            IMAGE_STD,
                            INPUT_NAME,
                            OUTPUT_NAME);
                } catch (final Exception e) {
                    throw new RuntimeException("Error Initializing TensorFlow!", e);
                }
            }
        });
    }


}
