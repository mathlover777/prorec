package com.souravsarkar.prorec;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.view.View;
import java.io.IOException;

import android.widget.EditText;
import java.util.ArrayList;
import android.widget.Toast;
import android.content.Context;
import android.os.Handler;
import android.widget.ToggleButton;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import android.graphics.Color;

public class AudioAppActivity extends AppCompatActivity {
    private static MediaPlayer mediaPlayer;
    private static Button recordButton;
    private static ToggleButton ma_pa_toggle;
    private boolean currently_playing = false;
    private boolean params_computed = false;
    private static EditText pitch_field;
    private static EditText median_pitch_field;
    private static EditText note_field;
    private static EditText is_male_field;
    private static EditText tone_field;

    // output global variables
    private static String note_value = "";
    private static String ma_pa_value = "";
    private static boolean is_male = true;

    List<Float> pitch_values = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_app);
    }
    @Override
    protected void onStart() {
        super.onStart();

        recordButton = (Button) findViewById(R.id.recordButton);
        ma_pa_toggle = (ToggleButton)findViewById(R.id.ma_pa);

        pitch_field = (EditText) findViewById(R.id.pitch_field);
        note_field = (EditText) findViewById(R.id.note_field);
        median_pitch_field = (EditText) findViewById(R.id.median_pitch_field);
        tone_field = (EditText) findViewById(R.id.tone_field);
        is_male_field = (EditText) findViewById(R.id.is_male_field);

        pitch_field.setEnabled(false);
        note_field.setEnabled(false);
        tone_field.setEnabled(false);
        is_male_field.setEnabled(false);
        median_pitch_field.setEnabled(false);

        pitch_field.setTextColor(Color.BLACK);
        note_field.setTextColor(Color.BLACK);
        tone_field.setTextColor(Color.BLACK);
        is_male_field.setTextColor(Color.BLACK);
        median_pitch_field.setTextColor(Color.BLACK);

        if (!hasMicrophone())
        {
            recordButton.setEnabled(false);
            recordButton.setText("Microphone Not Available :(");
        } else {

        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_audio_app, menu);
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
    protected boolean hasMicrophone() {
        PackageManager pmanager = this.getPackageManager();
        return pmanager.hasSystemFeature(
                PackageManager.FEATURE_MICROPHONE);
    }
    public void recordAudio (View view) throws IOException
    {
        recordButton.setEnabled(false);
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        String rate_string = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        String size_string = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);

        Log.d("ss_log", "Size :" + size_string + " & Rate: " + rate_string);

        int size = Integer.parseInt(size_string);
        int rate = Integer.parseInt(rate_string);

        pitch_values.clear();
        AudioDispatcher dispatcher = null;
        try {
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(rate, size, 0);
        }catch (Exception e){
            Log.d("ss_log","cant create dispatcher");
            System.out.print("Exception");
        }
        if (dispatcher == null){
            Log.d("ss_log","dispatcher is null");
            System.out.print("null error !");
        }else{
            Log.d("ss_log","dispatcher is fine");
        }
        assert dispatcher != null;
        dispatcher.addAudioProcessor(new PitchProcessor(PitchEstimationAlgorithm.FFT_YIN, rate, size, new PitchDetectionHandler() {

            @Override
            public void handlePitch(PitchDetectionResult pitchDetectionResult,
                                    AudioEvent audioEvent) {
                final float pitchInHz = pitchDetectionResult.getPitch();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pitch_field.setText("" + pitchInHz);
                        if (pitchInHz > 0) {
                            pitch_values.add(pitchInHz);
                        }
                    }
                });
            }
        }));
        final Thread pitch_finder = new Thread(dispatcher,"Audio Dispatcher");
        pitch_finder.start();
        final Handler handler2 = new Handler();
        handler2.postDelayed(new Runnable() {
            @Override
            public void run() {
                float median_pitch_value = get_median(pitch_values);
                note_value = get_note(median_pitch_value);
                ma_pa_value = get_ma_pa_value();
                is_male = get_male_flag(median_pitch_value);

                note_field.setText(note_value);
                median_pitch_field.setText("" + median_pitch_value);
                tone_field.setText(ma_pa_value);
                is_male_field.setText("" + is_male);

                params_computed = true;
                play_similar_audio();
            }

        }, 2000);
    }
    private float get_median(List<Float> numArray){
        Collections.sort(numArray);
        int middle = ((numArray.size()) / 2);
        float median = (float)0.0;
        if(numArray.size() % 2 == 0){
            float medianA = numArray.get(middle);
            float medianB = numArray.get(middle - 1);
            median = (medianA + medianB) / 2;
        } else{
            median = numArray.get(middle + 1);
        }
        return median;
    }
    private String get_ma_pa_value(){
        if (ma_pa_toggle.isChecked()){
            return "pa";
        }
        return "ma";
    }
    private boolean get_male_flag(float pitch){
        if (pitch > 185){
            return false;
        }
        return true;
    }


    public void play_continuous_music(String music_file) throws IOException,NoSuchFieldException,IllegalAccessException{
        currently_playing = true;
        mediaPlayer = MediaPlayer.create(this,R.raw.class.getField(music_file).getInt(null));
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.start();
        mediaPlayer.setLooping(true);
    }

    public void stop_music(View View){
        if(currently_playing){
            mediaPlayer.release();
            mediaPlayer = null;
            recordButton.setEnabled(true);
            currently_playing = false;
            recordButton.setEnabled(true);
        }
    }
    private String get_ma_pa_part(String ma_pa_value){
        if(ma_pa_value.toLowerCase().equals("ma")){
            return "m";
        }
        return "p";
    }
    private String get_is_male_part(boolean is_male){
        if(is_male){
            return "g";
        }
        return "l";
    }
    private String get_audio_file(String note_value,String ma_pa_value,boolean is_male){
        String audio_file = note_value.toLowerCase() + get_is_male_part(is_male) + get_ma_pa_part(ma_pa_value);
        Toast.makeText(this,"TO PLAY : " + audio_file, Toast.LENGTH_LONG).show();
        return audio_file;
    }
    public void play_similar_audio(){
        if( !params_computed){
            return;
        }
        String audio_file = get_audio_file(note_value,ma_pa_value,is_male);
        try {
            play_continuous_music(audio_file);
        }catch (Exception e){

        }
        return;
    }

    public String get_note(float average_pitch_value){
        while(average_pitch_value > 185){
            average_pitch_value = average_pitch_value / (float)2;
        }
        Map<String, Float> note_values= new HashMap<String, Float>();
        note_values.put("FS",(float)92.5);
        note_values.put("G",(float)98);
        note_values.put("GS",(float)103.83);
        note_values.put("A",(float)110);
        note_values.put("AS",(float)116.54);
        note_values.put("B",(float)123.47);
        note_values.put("C",(float)130.81);
        note_values.put("CS",(float)138.59);
        note_values.put("D",(float)146.83);
        note_values.put("DS",(float)155.56);
        note_values.put("E",(float)164.81);
        note_values.put("F",(float)174.61);
        note_values.put("FS",(float)185);

        Float min_diff = (float)100000.0;
        String min_note = "FS";
        for (Map.Entry<String,Float> entry : note_values.entrySet()) {
            String key = entry.getKey();
            Float value = entry.getValue();
            Float diff = Math.abs(value - average_pitch_value);
            if(diff < min_diff){
                min_diff = diff;
                min_note = key;
            }
        }
        return min_note;
    }
}
