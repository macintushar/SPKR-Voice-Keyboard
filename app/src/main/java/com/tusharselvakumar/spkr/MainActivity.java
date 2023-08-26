package com.tusharselvakumar.spkr;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    public static final Integer RecordAudioRequestCode = 1;
    private SpeechRecognizer speechRecognizer;
    private EditText editText;
    private ImageView micButton;
    private String SpeechToTextResult;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }

        Spinner languageSpinner = (Spinner) findViewById(R.id.languageSpinner);
        List<Pair<String, String>> languageList = new ArrayList<>();
        String[] languages = getResources().getStringArray(R.array.language_options);

        languageList.add(new Pair<>("English", "en"));
        languageList.add(new Pair<>("Spanish", "es"));
        languageList.add(new Pair<>("French", "fr"));
        languageList.add(new Pair<>("German", "de"));
        languageList.add(new Pair<>("Italian", "it"));
        languageList.add(new Pair<>("Portuguese", "pt"));
        languageList.add(new Pair<>("Dutch", "nl"));
        languageList.add(new Pair<>("Russian", "ru"));
        languageList.add(new Pair<>("Chinese (Simplified)", "zh-CN"));
        languageList.add(new Pair<>("Chinese (Traditional)", "zh-TW"));
        languageList.add(new Pair<>("Japanese", "ja"));
        languageList.add(new Pair<>("Korean", "ko"));
        languageList.add(new Pair<>("Arabic", "ar"));
        languageList.add(new Pair<>("Turkish", "tr"));
        languageList.add(new Pair<>("Hindi", "hi"));
        languageList.add(new Pair<>("Bengali", "bn"));
        languageList.add(new Pair<>("Urdu", "ur"));
        languageList.add(new Pair<>("Vietnamese", "vi"));
        languageList.add(new Pair<>("Thai", "th"));
        languageList.add(new Pair<>("Greek", "el"));
        languageList.add(new Pair<>("Hebrew", "he"));
        languageList.add(new Pair<>("Polish", "pl"));
        languageList.add(new Pair<>("Romanian", "ro"));
        languageList.add(new Pair<>("Czech", "cs"));
        languageList.add(new Pair<>("Swedish", "sv"));
        languageList.add(new Pair<>("Danish", "da"));
        languageList.add(new Pair<>("Norwegian", "no"));
        languageList.add(new Pair<>("Finnish", "fi"));
        languageList.add(new Pair<>("Hungarian", "hu"));
        languageList.add(new Pair<>("Ukrainian", "uk"));
        languageList.add(new Pair<>("Malay", "ms"));
        languageList.add(new Pair<>("Indonesian", "id"));
        languageList.add(new Pair<>("Filipino", "fil"));
        languageList.add(new Pair<>("Malayalam", "ml"));
        languageList.add(new Pair<>("Tamil", "ta"));
        languageList.add(new Pair<>("Telugu", "te"));
        languageList.add(new Pair<>("Marathi", "mr"));
        languageList.add(new Pair<>("Kannada", "kn"));
        languageList.add(new Pair<>("Punjabi", "pa"));
        languageList.add(new Pair<>("Gujarati", "gu"));


        // Extract display text for ArrayAdapter
        List<String> displayTexts = new ArrayList<>();
        for (Pair<String, String> languagePair : languageList) {
            displayTexts.add(languagePair.first);
        }

        // Set up ArrayAdapter for the Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, displayTexts);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);


        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Pair<String, String> selectedPair = languageList.get(position);
                String selectedLanguage = selectedPair.first;
                String selectedValue = selectedPair.second;

                Toast.makeText(getApplicationContext(), "Selected Language: " + selectedLanguage + " (Value: " + selectedValue + ")", Toast.LENGTH_SHORT).show();

                SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                editor.putString("userLanguage", selectedValue);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });
        editText = findViewById(R.id.text);
        micButton = findViewById(R.id.button);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ta");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {
                editText.setText("Listening...");
                //Toast.makeText(getApplicationContext(),"Listening...",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                editText.setText(data.get(0));
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        micButton.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                speechRecognizer.stopListening();
            }
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                speechRecognizer.startListening(speechRecognizerIntent);
            }
            return false;
        });

    Button translateBtn = (Button) findViewById(R.id.button2);
    translateBtn.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String textToTranslate = editText.getText().toString();
            Toast.makeText(getApplicationContext(),textToTranslate,Toast.LENGTH_SHORT).show();
        }
    });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechRecognizer.destroy();
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},RecordAudioRequestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RecordAudioRequestCode && grantResults.length > 0 ){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
        }
    }

}