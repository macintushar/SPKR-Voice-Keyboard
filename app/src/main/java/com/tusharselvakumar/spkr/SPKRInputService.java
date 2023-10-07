package com.tusharselvakumar.spkr;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;

import android.widget.EditText;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.cloud.translate.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class SPKRInputService extends InputMethodService {

    private static final int LAYOUT_HEAR = 0;
    private static final int LAYOUT_SPEAK = 1;

    private int currentLayout = LAYOUT_SPEAK;
    private TextToSpeech t1;
    private SharedPreferences sharedPreferences;
    private Translate translate;
    public static final Integer RecordAudioRequestCode = 1;
    private SpeechRecognizer speechRecognizer;
    private String SpeechToTextOP;
    private String userLang = "en";
    private ImageView micButton;

    @Override
    public View onCreateInputView() {
        View inputView;
        AssetManager assetManager = getAssets();
        CredentialsReader credentialsReader = new CredentialsReader(assetManager);
        JSONObject credentialsJson = credentialsReader.readCredentialsJson();

        String apiKey = "";
        try {
            apiKey = credentialsJson.getString("APIKEY");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.setProperty("GOOGLE_API_KEY",apiKey);

        if (currentLayout == LAYOUT_HEAR) {
            inputView = getLayoutInflater().inflate(R.layout.keyboard_layout_hear, null);
            //Toast.makeText(getApplicationContext(),apiKey,Toast.LENGTH_LONG).show();

            sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

            Button switchToSpeakButton = inputView.findViewById(R.id.switchToSpeakButton);
            Button hearBtn = (Button) inputView.findViewById(R.id.hearButton);
            Button clearBtn = (Button) inputView.findViewById(R.id.clearBtn);
            EditText text = (EditText) inputView.findViewById(R.id.textToSpeechText);

            switchToSpeakButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSwitchToSpeakClick(v);
                }
            });

            hearBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String userInputText = text.getText().toString();
                    String userLanguage = sharedPreferences.getString("userLanguage", "en");
                    userLang = userLanguage;
                    String translatedToUserLanguageText = translateTextToUserLang(userInputText, userLanguage);
                    playTranslatedAudio(translatedToUserLanguageText);

                    addToHistory(getApplicationContext(),userLanguage,"",translatedToUserLanguageText,userInputText);
                    Toast.makeText(getApplicationContext(),"Wrote to history", Toast.LENGTH_SHORT).show();
//                    String usageHistory = loadHistory(getApplicationContext());
//                    text.setText(usageHistory);
                }
            });

            clearBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    text.setText("");
                }
            });


        } else {
            inputView = getLayoutInflater().inflate(R.layout.keyboard_layout_speak, null);

            Button buttonSendTranslatedText = inputView.findViewById(R.id.key_send_translated);
            Button buttonSendTextInInputLang = inputView.findViewById(R.id.key_send_spoken);
            Button buttonHearSpokenAudio = inputView.findViewById(R.id.key_hear_spoken);
            Button backspaceBtn = inputView.findViewById(R.id.backspaceButton);
            Button enterBtn = inputView.findViewById(R.id.key_enter);
            Button sendBtn = inputView.findViewById(R.id.key_send);

            TextView tv = inputView.findViewById(R.id.textView3);
            micButton = inputView.findViewById(R.id.button);
            TextView viewLanguage = inputView.findViewById(R.id.textView5);

            sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            String userLanguage = sharedPreferences.getString("userLanguage", "en");
            String outputLanguage = sharedPreferences.getString("outputLanguage", "en");

            String viewLanguageText = userLanguage + " -> " + outputLanguage;
            viewLanguage.setText(viewLanguageText);
            micButton.setImageResource(R.drawable.mic_ready);

            buttonSendTranslatedText.setText(outputLanguage);
            buttonSendTextInInputLang.setText(userLanguage);

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());

            final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, userLanguage);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle bundle) {
                    tv.setText("Listening...");
                }

                @Override
                public void onBeginningOfSpeech() {
                    micButton.setImageResource(R.drawable.mic_active);
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
                    String errorMeaning;
                    if (i == 3){
                        errorMeaning = "Audio Recording Error";
                    } else if (i == 2) {
                        errorMeaning = "Network Error";
                    } else if (i == 1) {
                        errorMeaning = "Network Timeout";
                    } else if (i == 7) {
                        errorMeaning = "No Match. Try again.";
                    } else if (i == 8) {
                        errorMeaning = "Recognizer is busy. Try again in some time.";
                    } else if (i == 11) {
                        errorMeaning = "Server disconnected";
                    } else if (i == 6) {
                        errorMeaning = "No Speech Input";
                    } else if (i == 10) {
                        errorMeaning = "Too many requests. Wait and try later.";
                    } else {
                        errorMeaning = "Some error occured. Wait and try later.";
                    }

                    tv.setText("Error: " + errorMeaning);
                    micButton.setImageResource(R.drawable.mic_ready);
                }

                @Override
                public void onResults(Bundle bundle) {
                    ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    SpeechToTextOP = data.get(0);
                    tv.setText(data.get(0));
                    micButton.setImageResource(R.drawable.mic_ready);
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
            buttonSendTranslatedText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentLayout == LAYOUT_SPEAK) {
                        String translatedToOutputText = translateToOPLang(SpeechToTextOP, outputLanguage, userLanguage);
                        onKeyClick(v,translatedToOutputText);
                    }
                }
            });

            buttonSendTextInInputLang.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentLayout == LAYOUT_SPEAK) {
                        onKeyClick(v,SpeechToTextOP);
                    }
                }
            });

            buttonHearSpokenAudio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentLayout == LAYOUT_SPEAK) {
                        playUserTextAudio(SpeechToTextOP,userLanguage);
                    }
                }
            });

            backspaceBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentLayout == LAYOUT_SPEAK) {
                        onBackspaceClick(v);
                    }
                }
            });

            enterBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentLayout == LAYOUT_SPEAK) {
                        onKeyClick(v, "\n");
                    }
                }
            });

            sendBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentLayout == LAYOUT_SPEAK) {
                        onSendClick(v);
                    }
                }
            });

            Button switchToHearButton = inputView.findViewById(R.id.switchToHearButton);
            switchToHearButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSwitchToHearClick(v);
                }
            });
        }
        return inputView;
    }

    public class CredentialsReader {
        private AssetManager assetManager;

        public CredentialsReader(AssetManager assetManager) {
            this.assetManager = assetManager;
        }

        public JSONObject readCredentialsJson() {
            JSONObject jsonObject = null;
            try {
                InputStream inputStream = assetManager.open("credentials.json");

                int size = inputStream.available();
                byte[] buffer = new byte[size];
                inputStream.read(buffer);
                inputStream.close();
                String jsonString = new String(buffer, StandardCharsets.UTF_8);

                // Convert the JSON String to a JSONObject
                jsonObject = new JSONObject(jsonString);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }
    }

    public String loadHistory(Context context) {
        String json = null;
        try {
            FileInputStream fis = context.openFileInput("local_history.json");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }
            bufferedReader.close();

            json = jsonStringBuilder.toString();
            Toast.makeText(getApplicationContext(),"Getting JSON", Toast.LENGTH_SHORT).show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return json;
    }

    public void addToHistory(Context context, String userLanguage, String targetLanguage, String translatedText, String originalText) {
        try {
            FileInputStream fis = context.openFileInput("local_history.json");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }
            bufferedReader.close();

            String jsonStr = jsonStringBuilder.toString();
            JSONObject jsonObject = new JSONObject(jsonStr);

            JSONObject historyEntry = new JSONObject();
            historyEntry.put("userLanguage", userLanguage);
            historyEntry.put("targetLanguage", targetLanguage);
            historyEntry.put("translatedText", translatedText);
            historyEntry.put("originalText", originalText);

            String timestamp = getCurrentTimestamp();
            jsonObject.put(timestamp, historyEntry);

            String newJsonStr = jsonObject.toString();

            FileOutputStream fos = context.openFileOutput("local_history.json", Context.MODE_PRIVATE);
            fos.write(newJsonStr.getBytes());
            fos.close();

            Toast.makeText(getApplicationContext(),"Wrote to history", Toast.LENGTH_SHORT).show();

        } catch (IOException | JSONException ex) {
            ex.printStackTrace();
        }
    }

    public static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy:HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    public String translateToOPLang(final String userText, String outputLanguage, String inputLanguage) {
        final String[] result = {null};

        new Thread(new Runnable() {
            @Override
            public void run() {
                Translate translate = TranslateOptions.getDefaultInstance().getService();
                try {
                    Translate translateService = TranslateOptions.getDefaultInstance().getService();
                    Translation translation = null;

                    translation = translateService.translate(
                            userText,
                            Translate.TranslateOption.sourceLanguage(inputLanguage),
                            Translate.TranslateOption.targetLanguage(outputLanguage)
                    );
                    final String translatedText = translation.getTranslatedText();

                    // Set the translated text result
                    synchronized (result) {
                        result[0] = translatedText;
                    }

                } catch (TranslateException e) {
                    // Handle the exception
                    e.printStackTrace();
                    result[0] = "Failed: " + e;
                }
            }
        }).start();

        while (result[0] == null) {
            try {
                Thread.sleep(100); // Sleep for a short time
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return result[0];
    }

    public String translateTextToUserLang(final String userText, String outputLanguage) {
        final String[] result = {null};

        new Thread(new Runnable() {
            @Override
            public void run() {
                Translate translate = TranslateOptions.getDefaultInstance().getService();
                try {
                    Translate translateService = TranslateOptions.getDefaultInstance().getService();
                    Translation translation = null;
                    
                    Detection detection = translateService.detect(userText);
                    String detectedLanguage = detection.getLanguage();

                    translation = translateService.translate(
                            userText,
                            Translate.TranslateOption.sourceLanguage(detectedLanguage),
                            Translate.TranslateOption.targetLanguage(outputLanguage)
                    );
                    final String translatedText = translation.getTranslatedText();

                    addToHistory(getApplicationContext(),detectedLanguage,outputLanguage,translatedText,userText);

                    // Set the translated text result
                    synchronized (result) {
                        result[0] = translatedText;
                    }

                } catch (TranslateException e) {
                    // Handle the exception
                    e.printStackTrace();
                    result[0] = "Failed: " + e;
                }
            }
        }).start();

        while (result[0] == null) {
            try {
                Thread.sleep(100); // Sleep for a short time
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return result[0];
    }

    public void playTranslatedAudio(String userTranslatedText) {
        String userLanguage = sharedPreferences.getString("userLanguage", "en"); // Default to English

        Locale locale = new Locale(userLanguage);

        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = t1.setLanguage(locale);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(getApplicationContext(), "Language not supported", Toast.LENGTH_SHORT).show();
                    } else {
                        String toSpeak = userTranslatedText;
                        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "TextToSpeech initialization failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void playUserTextAudio(String userTranslatedText, String language) {
        Locale locale = new Locale(language);

        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = t1.setLanguage(locale);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(getApplicationContext(), "Language not supported", Toast.LENGTH_SHORT).show();
                    } else {
                        String toSpeak = userTranslatedText;
                        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "TextToSpeech initialization failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void onSwitchToSpeakClick(View view) {
        currentLayout = LAYOUT_SPEAK;
        setInputView(onCreateInputView()); // Update the input view
    }

    public void onSwitchToHearClick(View view) {
        currentLayout = LAYOUT_HEAR;
        setInputView(onCreateInputView()); // Update the input view
    }

    public void onBackspaceClick(View view) {
        InputConnection inputConnection = getCurrentInputConnection();
        inputConnection.deleteSurroundingText(1, 0);
    }
    public void onSendClick(View view) {
        InputConnection inputConnection = getCurrentInputConnection();
        EditorInfo editorInfo = getCurrentInputEditorInfo();
        editorInfo.actionId = EditorInfo.IME_ACTION_SEND;
        editorInfo.actionLabel = "SEND";
        inputConnection.performEditorAction(EditorInfo.IME_ACTION_SEND);
    }
    public void onKeyClick(View view, String text) {
        if (view instanceof Button) {
            InputConnection inputConnection = getCurrentInputConnection();
            // Check if the text is the Enter key
            if (text.equals("\n")) {
                // To insert a newline character (Enter key)
                inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            } else {
                // For other text, just commit it
                inputConnection.commitText(text, 1);
            }
        }
    }
}
