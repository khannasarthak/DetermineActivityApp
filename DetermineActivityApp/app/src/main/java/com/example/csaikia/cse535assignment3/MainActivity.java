package com.example.csaikia.cse535assignment3;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.StringTokenizer;
import java.util.Vector;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_print_interface;
import libsvm.svm_problem;

public class MainActivity extends AppCompatActivity implements SensorEventListener, OnClickListener {
    static int ACCE_FILTER_DATA_MIN_TIME = 100;
    boolean started = false;
    Button walkButton;
    Button runButton;
    Button jumpButton;
    Button testButton;
    Button predictButton;
    Button trainButton;
    String columns = "";
    Sensor accelerometer;
    SQLiteDatabase db;
    String table_name;
    long lastSaved = System.currentTimeMillis();
    String query_vals = "";
    String query_cols = "";
    int activity;
    int j;



    Cursor dbCursorTest;

    FileOutputStream fout ;
    BufferedWriter bw ;
    File nfile;


    RunnableDemo runnableDemo = new RunnableDemo();
    double arr_x[] = new double[50];
    double arr_y[] = new double[50];
    double arr_z[] = new double[50];
    double min_x, min_y, min_z, max_x, max_y, max_z;

    // SVM stuff
    private svm_parameter param;		// set by parse_command_line
    private svm_problem prob;		// set by read_problem
    private svm_model model;
    private String input_file_name;		// set by parse_command_line
    private String model_file_name;		// set by parse_command_line
    private String error_msg;
    private int cross_validation;
    private int nr_fold;

    public String packagename;
    public String accuracy, svm_params;

    TextView accuracy_text, svm_parameters;



    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        final File file = new File(Environment.getExternalStorageDirectory()+ File.separator+"Android/Data/CSE535_ASSIGNMENT3");
        //Android/Data/CSE535_ASSIGNMENT3
        // /data/data/





        if (!file.exists()) {
            file.mkdirs();
            Toast.makeText(MainActivity.this, "directory created", Toast.LENGTH_LONG).show();
        }

        String[] axes = new String[] {"x","y","z"};
        for (int i=1;i<=50;i++) {
            for (String s: axes) {
                columns = columns + "Accel_"+s+"_"+i+" double, ";
            }
        }

        columns = columns + "Activity_Label varchar);";
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);


        for (int i=1;i<=50;i++) {
            for (String s: axes) {
                query_cols = query_cols + "Accel_"+s+"_"+i+",";
            }
        }
        query_cols = query_cols + "Activity_label";

        setContentView(R.layout.activity_main);
        try {
            db = SQLiteDatabase.openDatabase(file.toString()+"/group2", null, SQLiteDatabase.CREATE_IF_NECESSARY);
            db.beginTransaction();
            table_name = "assignment_3";
            try {
                db.execSQL("create table if not exists " + table_name + " (" + " ID integer PRIMARY KEY autoincrement, " + columns);
                db.setTransactionSuccessful();
            } catch (SQLiteException e) {
                Toast.makeText(MainActivity.this, "Unable to connect to database", Toast.LENGTH_LONG).show();
            } finally {
                db.endTransaction();
            }
        } catch (SQLException e) {
            Toast.makeText(MainActivity.this, "Unable to create the database", Toast.LENGTH_LONG).show();
        }


        // Buttons initialize
        runButton = (Button) findViewById(R.id.run);
        runButton.setOnClickListener(this);
        walkButton = (Button) findViewById(R.id.walk);
        walkButton.setOnClickListener(this);
        jumpButton = (Button) findViewById(R.id.jump);
        jumpButton.setOnClickListener(this);
        predictButton = (Button) findViewById(R.id.predict);
        predictButton.setOnClickListener(this);
        trainButton = (Button) findViewById(R.id.train);
        trainButton.setOnClickListener(this);


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.run: {
                activity = 1;
                j = 0;
                min_x = Double.MAX_VALUE;
                min_y = Double.MAX_VALUE;
                min_z = Double.MAX_VALUE;
                max_x = Double.MIN_VALUE;
                max_y = Double.MIN_VALUE;
                max_z = Double.MIN_VALUE;
                started = true;
                Toast.makeText(MainActivity.this, "Start running", Toast.LENGTH_LONG).show();
                Log.d("chaynika", "running");
                Thread thread = new Thread(runnableDemo);
                thread.start();
                break;
            }
            case R.id.jump: {
                activity = 2;
                j = 0;
                min_x = Double.MAX_VALUE;
                min_y = Double.MAX_VALUE;
                min_z = Double.MAX_VALUE;
                max_x = Double.MIN_VALUE;
                max_y = Double.MIN_VALUE;
                max_z = Double.MIN_VALUE;
                started = true;
                Toast.makeText(MainActivity.this, "Start jumping", Toast.LENGTH_LONG).show();
                Log.d("chaynika", "jumping");
                Thread thread = new Thread(runnableDemo);
                thread.start();
                break;
            }
            case R.id.walk: {
                activity = 3;
                j = 0;
                min_x = Double.MAX_VALUE;
                min_y = Double.MAX_VALUE;
                min_z = Double.MAX_VALUE;
                max_x = Double.MIN_VALUE;
                max_y = Double.MIN_VALUE;
                max_z = Double.MIN_VALUE;
                started = true;
                Toast.makeText(MainActivity.this, "Start walking", Toast.LENGTH_LONG).show();
                Log.d("chaynika", "walking");
                Thread thread = new Thread(runnableDemo);
                thread.start();
                break;
            }


            // .TRAIN BUTTON
            case R.id.train: {
                System.out.println("------------PACKAGE NAME"+getPackageName());
                //classifier = (TextView) findViewById(R.id.predictDisp);
                accuracy_text = (TextView) findViewById(R.id.accuracyDisp);
                svm_parameters = (TextView) findViewById(R.id.paramDisp);


                try {
                    Toast.makeText(MainActivity.this, "Training...", Toast.LENGTH_LONG).show();
                    System.out.println("------------CREATED FILES: training_data.txt, test_data.txt");
                    nfile = new File("/data/data/" + getPackageName() + "/training_data.txt");


                    fout = new FileOutputStream(nfile);


                    bw = new BufferedWriter(new OutputStreamWriter(fout));

                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println("---------TRAIN: "+table_name);

                dbCursorTest = db.rawQuery("select * from " + table_name, null);

                dbCursorTest.moveToFirst();


                dbCursorTest.moveToFirst();
                while (!dbCursorTest.isAfterLast()) {

                    String output = "+" + dbCursorTest.getString(151);
                        for (int i = 1; i <= 150; i++)
                            output = output + " " + dbCursorTest.getString(i);
                    System.out.println(output);
                        try {


                            bw.write(output);

                            bw.newLine();
                        } catch (Exception e) {
                            e.printStackTrace();

                    }

                    dbCursorTest.moveToNext();
            }



                try {
                    if (bw != null) bw.close();


                } catch (IOException e) {
                    Log.e("READER.CLOSE()", e.toString());
                }
                dbCursorTest.close();



                try {
                    System.out.println("Inside Predict...");
                    svm_model model = run();
                    System.out.println("Model created");

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                try {

                    accuracy_text.setText(String.valueOf(accuracy));
                    System.out.println("----ACCURACY"+accuracy);
                    svm_params = "svm_type: c_svc, " +
                            "kernel_type: rbf, " +
                            "gamma: 5.0E-4, " +
                            "degree: 2 " +
                            "C: 100" +
                            "coef0: 0 " +
                            "mu: 0.5 " +
                            "cache_size: 100 " +
                            "eps: 1E-3 " +
                            "p: 0.1 " +
                            "probability: 0 " +
                            "shrinking: 1 " +
                            "nr_weight: 0 " +
                            "cross_validation: 4-fold";
                    svm_parameters.setText(svm_params);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

            }




    public void populate_database (int activity,int k){
        Log.d("chaynika", "j is " + k);
        while (k < 50) {
            arr_x[k] = min_x + Math.random() * (max_x - min_x);
            arr_y[k] = min_y + Math.random() * (max_y - min_y);
            arr_z[k] = min_z + Math.random() * (max_z - min_z);
            k++;
        }
        query_vals = "";
        for (int i = 0; i < 50; i++) {
            query_vals = query_vals + arr_x[i] + "," + arr_y[i] + "," + arr_z[i] + ",";
        }
        query_vals = query_vals + "'" + activity + "'";
        db.execSQL("INSERT INTO " + table_name + "(" + query_cols + ")" + " values (" + query_vals + ");");
        //db.close();
    }

    @Override
    public void onSensorChanged (SensorEvent event){
        if (started) {if (j == 50) {
            started = false;
        } else {
            if ((System.currentTimeMillis() - lastSaved) >= ACCE_FILTER_DATA_MIN_TIME) {

                lastSaved = System.currentTimeMillis();
                arr_x[j] = event.values[0];
                arr_y[j] = event.values[1];
                arr_z[j] = event.values[2];
                min_x = Math.min(min_x, arr_x[j]);
                min_y = Math.min(min_y, arr_y[j]);
                min_z = Math.min(min_z, arr_z[j]);
                max_x = Math.max(max_x, arr_x[j]);
                max_y = Math.max(max_y, arr_y[j]);
                max_z = Math.max(max_z, arr_z[j]);
                j++;
            }
        }
    }}


    // ---------------------------------------------- SVM -----------------------------------------------------------------------------------

    private static double atof(String s)
    {
        double d = Double.valueOf(s).doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d))
        {
            System.err.print("NaN or Infinity in input\n");
            System.exit(1);
        }
        return(d);
    }

    private static int atoi(String s)
    {
        try
        {
            return Integer.parseInt(s);
        }
        catch(Exception e)
        {
            return 0;
        }
    }
    private static svm_print_interface svm_print_null = new svm_print_interface()
    {
        public void print(String s) {}
    };


    private StringBuilder parse_command_line()
    {
        int i;
        svm_print_interface print_func = null;	// default printing to stdout
        StringBuilder sb=new StringBuilder();
        param = new svm_parameter();
        // default values
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        param.degree = 2;
        param.gamma = 0.0005;	// 1/num_features
        param.coef0 = 0;
        param.nu = 0.5;
        param.cache_size = 100;
        param.C = 100;
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 1;
        param.probability = 0;
        param.nr_weight = 0;
        param.weight_label = new int[0];
        param.weight = new double[0];
        cross_validation = 1;
        nr_fold=4;
        sb.append(param.svm_type+",").append(param.kernel_type+",").append(param.degree+",")
                .append(param.gamma+",").append(param.coef0+",").append(param.nu+",").append(param.cache_size+",")
                .append(param.C+",").append(param.eps+",").append(param.p+",").append(param.shrinking+",")
                .append(param.probability+",").append(param.nr_weight+",").append(param.weight_label+",")
                .append(param.weight+",").append(cross_validation+",").append(nr_fold);

        svm.svm_set_print_string_function(print_func);



        input_file_name = "/data/data/" + getPackageName() + "/training_data.txt";
        model_file_name = "/data/data/" + getPackageName() + "/model.txt";
        return sb;
    }

    private void read_problem() throws IOException
    {
        System.out.println("Inside Read Prob...");
        BufferedReader fp = new BufferedReader(new FileReader(input_file_name ));
        Vector<Double> vy = new Vector<Double>();
        Vector<svm_node[]> vx = new Vector<svm_node[]>();
        int max_index = 0;

        while(true)
        {
            String line = fp.readLine();
            if(line == null) break;

            StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");

            vy.addElement(atof(st.nextToken()));
            int m = st.countTokens()/2;
            svm_node[] x = new svm_node[m];
            for(int j=0;j<m;j++)
            {
                x[j] = new svm_node();
                x[j].index = atoi(st.nextToken());
                x[j].value = atof(st.nextToken());
            }
            if(m>0) max_index = Math.max(max_index, x[m-1].index);
            vx.addElement(x);
        }

        prob = new svm_problem();
        prob.l = vy.size();
        prob.x = new svm_node[prob.l][];
        for(int i=0;i<prob.l;i++)
            prob.x[i] = vx.elementAt(i);
        prob.y = new double[prob.l];
        for(int i=0;i<prob.l;i++)
            prob.y[i] = vy.elementAt(i);

        if(param.gamma == 0 && max_index > 0)
            param.gamma = 1.0/max_index;

        if(param.kernel_type == svm_parameter.PRECOMPUTED)
            for(int i=0;i<prob.l;i++)
            {
                if (prob.x[i][0].index != 0)
                {
                    System.err.print("Wrong kernel matrix: first column must be 0:sample_serial_number\n");
                    System.exit(1);
                }
                if ((int)prob.x[i][0].value <= 0 || (int)prob.x[i][0].value > max_index)
                {
                    System.err.print("Wrong input format: sample_serial_number out of range\n");
                    System.exit(1);
                }
            }

        fp.close();
    }

    public svm_model run() throws IOException
    {
        System.out.println("Inside SVM Run..");
        StringBuilder sb=parse_command_line();

        read_problem();
        error_msg = svm.svm_check_parameter(prob,param);
        double ret=0;
        if(error_msg != null)
        {
            System.err.print("ERROR: "+error_msg+"\n");
            System.exit(1);
        }

        if(cross_validation != 0)
        {
            ret = do_cross_validation();
            accuracy = String.valueOf(ret);
            //System.out.println(ret);
        }
        //else
        model = svm.svm_train(prob,param);

        svm.svm_save_model(model_file_name,model);

        return model;
    }

    private double do_cross_validation()
    {
        int i;
        int total_correct = 0;
        double total_error = 0;
        double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
        double[] target = new double[prob.l];

        svm.svm_cross_validation(prob,param,nr_fold,target);
        if(param.svm_type == svm_parameter.EPSILON_SVR ||
                param.svm_type == svm_parameter.NU_SVR)
        {
            for(i=0;i<prob.l;i++)
            {
                double y = prob.y[i];
                double v = target[i];
                total_error += (v-y)*(v-y);
                sumv += v;
                sumy += y;
                sumvv += v*v;
                sumyy += y*y;
                sumvy += v*y;
            }
            System.out.print("Cross Validation Mean squared error = "+total_error/prob.l+"\n");
            System.out.print("Cross Validation Squared correlation coefficient = "+
                    ((prob.l*sumvy-sumv*sumy)*(prob.l*sumvy-sumv*sumy))/
                            ((prob.l*sumvv-sumv*sumv)*(prob.l*sumyy-sumy*sumy))+"\n"
            );
        }
        else
        {
            for(i=0;i<prob.l;i++)
                if(target[i] == prob.y[i])
                    ++total_correct;
            System.out.print("Cross Validation Accuracy = "+100.0*total_correct/prob.l+"%\n");
        }
        return 100.0*total_correct/prob.l;
    }


    //---------------------------------------------------------------------------------------------------------------------------------------------------










    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    class RunnableDemo implements Runnable {
        //public Thread thread;

        public void run() {
            try {
                Thread.sleep(7000);
                populate_database(activity, j);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            started = false;
        }
    }
    @Override
    public void onStop() {
        super.onStop();
        //db.close();
    }

}
