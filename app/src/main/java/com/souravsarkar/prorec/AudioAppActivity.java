package com.souravsarkar.prorec;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Environment;
import android.widget.Button;
import android.media.MediaPlayer;
import android.view.View;
import java.io.IOException;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;

//import com.androidquery.AQuery;
//import com.souravsarkar.prorec.Melody;
import java.io.File;
//import org.json.JSONObject;
//import com.androidquery.callback.AjaxStatus;
//import com.androidquery.callback.AjaxCallback;
//import com.androidquery.callback.AjaxStatus;
//import com.androidquery.callback.Transformer;
//import com.androidquery.util.AQUtility;
//import com.androidquery.util.XmlDom;
import 	android.os.Handler;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AudioAppActivity extends AppCompatActivity {

    private static MediaRecorder mediaRecorder;
    private static MediaPlayer mediaPlayer;

    private static String audioFilePath;
    private static Button stopButton;
    private static Button playButton;
    private static Button recordButton;
//    private static ListView melodyList;

    private boolean isRecording = false;

    //LIST OF ARRAY STRINGS WHICH WILL SERVE AS LIST ITEMS
//    ArrayList<Melody> listItems=new ArrayList<Melody>();

    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
//    ArrayAdapter<Melody> adapter;

    private static String recordedMelodyName = "myaudio.3gp";
    private static String basePath = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static String baseFolder = "prorec";

    List<Float> pitch_values = new ArrayList<>();
//
//    private static AQuery aq;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_app);
    }
    @Override
    protected void onStart() {
        super.onStart();

        recordButton = (Button) findViewById(R.id.recordButton);
//        playButton = (Button) findViewById(R.id.playButton);
//        stopButton = (Button) findViewById(R.id.stopButton);

        if (!hasMicrophone())
        {
//            stopButton.setEnabled(false);
//            playButton.setEnabled(false);
            recordButton.setEnabled(false);
        } else {
//            playButton.setEnabled(false);
//            stopButton.setEnabled(false);
        }
        init_recording_folder();
        audioFilePath = basePath + "/" + baseFolder + "/" + recordedMelodyName;
//        adapter = new ArrayAdapter<Melody>(this, android.R.layout.simple_list_item_1, listItems);
//        melodyList = (ListView) findViewById(R.id.melodyList);
//        melodyList.setAdapter(adapter);

    }

    public void init_recording_folder(){
        String fullFolderPath = basePath + "/" + baseFolder;
        File folder = new File(fullFolderPath);
        if(folder.exists() && folder.isDirectory()){
            return;
        }
        folder.mkdirs();
        return;
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
//        isRecording = true;
//        stopButton.setEnabled(true);
//        playButton.setEnabled(false);
        recordButton.setEnabled(false);
//
//        try {
//            mediaRecorder = new MediaRecorder();
//            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//            mediaRecorder.setOutputFile(audioFilePath);
//            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//            mediaRecorder.prepare();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        mediaRecorder.start();
//        final Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                //Do something after 100ms
//                stopButton.performClick();
//            }
//        }, 5000);
        pitch_values.clear();
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050,1024,0);
        dispatcher.addAudioProcessor(new PitchProcessor(PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, new PitchDetectionHandler() {

            @Override
            public void handlePitch(PitchDetectionResult pitchDetectionResult,
                                    AudioEvent audioEvent) {
                final float pitchInHz = pitchDetectionResult.getPitch();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView text = (TextView) findViewById(R.id.pitch_value_display);
                        text.setText("" + pitchInHz);
                        pitch_values.add(pitchInHz);
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
                //Do something after 100ms
                TextView msg = (TextView) findViewById(R.id.msg);
                float sum = 0;
                for(float pitch_value : pitch_values){
                    sum = sum + pitch_value;
                }
                float average_pitch_value = sum / ((float)pitch_values.size());
                String note = get_note(average_pitch_value);
                msg.setText(pitch_values.size() + " values recorded ! Average Pitch = " + average_pitch_value + "\n"+
                "Note = " + note);
                recordButton.setEnabled(true);
            }

        }, 5000);
    }
    public void stopClicked (View view)
    {

        stopButton.setEnabled(false);
        playButton.setEnabled(true);

        if (isRecording)
        {
            recordButton.setEnabled(false);
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
        } else {
            mediaPlayer.release();
            mediaPlayer = null;
            recordButton.setEnabled(true);
        }
    }
    public void playAudio (View view) throws IOException
    {
        playButton.setEnabled(false);
        recordButton.setEnabled(false);
        stopButton.setEnabled(true);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(audioFilePath);
        mediaPlayer.prepare();
        mediaPlayer.start();
    }
    public void searchClicked (View view) {
//        adapter.add(new Melody("melody name","melody link"));
        // first find the pitch of the sound.


    }

    public String get_note(float average_pitch_value){
        while(average_pitch_value > 185){
            average_pitch_value = average_pitch_value / (float)2;
        }
        Map<String, Float> note_values= new HashMap<String, Float>();
        note_values.put("F#",(float)92.5);
        note_values.put("G",(float)98);
        note_values.put("G#",(float)103.83);
        note_values.put("A",(float)110);
        note_values.put("A#",(float)116.54);
        note_values.put("B",(float)123.47);
        note_values.put("C",(float)130.81);
        note_values.put("C#",(float)138.59);
        note_values.put("D",(float)146.83);
        note_values.put("D#",(float)155.56);
        note_values.put("E",(float)164.81);
        note_values.put("F",(float)174.61);
        note_values.put("F#",(float)185);

        Float min_diff = (float)10000.0;
        String min_note = "F#";
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


//    public ArrayList<Melody> get_similar_melodies(){
//        ArrayList<Melody> retrievedMelodyList = new ArrayList<Melody>();
//        return retrievedMelodyList;
//    }
}
