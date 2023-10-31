package com.tusharselvakumar.spkr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.media.Image;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.api.client.auth.oauth.AbstractOAuthGetToken;
import com.google.cloud.translate.Detection;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateException;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class SPKRInputService extends InputMethodService {

    private static final int LAYOUT_HEAR = 0;
    private static final int LAYOUT_SPEAK = 1;

    private int currentLayout = LAYOUT_HEAR;
    private TextToSpeech t1;
    private SharedPreferences sharedPreferences;
    private Translate translate;
    public static final Integer RecordAudioRequestCode = 1;
    private SpeechRecognizer speechRecognizer;
    private String SpeechToTextOP = "";
    private ImageView micButton;
    private View inputView;
    @Override
    public View onCreateInputView() {
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

        inputView = getLayoutInflater().inflate(R.layout.keyboard_layout, null);

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        Integer keyboardBgColor = sharedPreferences.getInt("keyboardBackground",R.color.black);

        ConstraintLayout keyboardLayoutBG = inputView.findViewById(R.id.background);
        keyboardLayoutBG.setBackgroundColor(keyboardBgColor);

        ImageButton hearBtn = inputView.findViewById(R.id.hearButton);
        ImageButton clearBtn = inputView.findViewById(R.id.clearBtn);
        EditText text = inputView.findViewById(R.id.textToSpeechText);
        Spinner targetLangSpinner = inputView.findViewById(R.id.targetLanguageSpinner);
        Spinner userLangSpinner = inputView.findViewById(R.id.userLanguageSpinner);

        Button buttonSendTranslatedText = inputView.findViewById(R.id.key_send_translated);
        ImageButton buttonHearSpokenAudio = inputView.findViewById(R.id.key_hear_spoken);
        ImageButton backspaceBtn = inputView.findViewById(R.id.backspaceButton);
        ImageButton sendBtn = inputView.findViewById(R.id.key_send);

        TextView tv = inputView.findViewById(R.id.textView3);
        micButton = inputView.findViewById(R.id.button);

        List<Pair<String, String>> languageList = new ArrayList<>();

        languageList.add(new Pair<>("English", "en"));
        languageList.add(new Pair<>("Tamil (தமிழ்)", "ta"));
        languageList.add(new Pair<>("Kannada (ಕನ್ನಡ)", "kn"));
        languageList.add(new Pair<>("Hindi (हिंदी)", "hi"));
        languageList.add(new Pair<>("Malayalam(മലയാളം)", "ml"));
        languageList.add(new Pair<>("Japaneese (日本語)", "ja"));
        languageList.add(new Pair<>("Spanish(Español)", "es"));
        languageList.add(new Pair<>("French(Français)", "fr"));
        languageList.add(new Pair<>("German(Deutsch)", "de"));
        languageList.add(new Pair<>("Dutch", "nl"));

        List<String> displayTexts = new ArrayList<>();
        for (Pair<String, String> languagePair : languageList) {
            displayTexts.add(languagePair.first);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, displayTexts);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String userLanguage = sharedPreferences.getString("userLanguage", "en");
        String outputLanguage = sharedPreferences.getString("outputLanguage", "en");

        buttonHearSpokenAudio.setEnabled(false);
        buttonSendTranslatedText.setEnabled(false);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());

        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, userLanguage);

        targetLangSpinner.setAdapter(adapter);
        userLangSpinner.setAdapter(adapter);

        targetLangSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Pair<String, String> selectedPair = languageList.get(position);
                String selectedLanguage = selectedPair.first;
                String selectedValue = selectedPair.second;

                Toast.makeText(getApplicationContext(), "Selected Language: " + selectedLanguage + " (Value: " + selectedValue + ")", Toast.LENGTH_SHORT).show();

                SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("outputLanguage", selectedValue);
                editor.apply();

                buttonSendTranslatedText.setText(selectedValue);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        userLangSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Pair<String, String> selectedPair = languageList.get(position);
                String selectedLanguage = selectedPair.first;
                String selectedValue = selectedPair.second;

                //Toast.makeText(getApplicationContext(), "Selected Language: " + selectedLanguage + " (Value: " + selectedValue + ")", Toast.LENGTH_SHORT).show();

                SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("userLanguage", selectedValue);
                editor.apply();

                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedValue);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        hearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                String userLanguage = sharedPreferences.getString("userLanguage", "en");

                String userInputText = text.getText().toString();
                if (userInputText.length() > 1) {
                    String translatedToUserLanguageText = translateTextToUserLang(userInputText, userLanguage);
                    playTranslatedAudio(translatedToUserLanguageText);
                } else {
                    Toast.makeText(getApplicationContext(), "No text to hear", Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(getApplicationContext(),"Wrote to history", Toast.LENGTH_SHORT).show();
            }
        });

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                text.setText("");
            }
        });

        micButton.setImageResource(R.drawable.microphone_ready);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

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
                buttonHearSpokenAudio.setEnabled(true);
                buttonSendTranslatedText.setEnabled(true);
            }

            @Override
            public void onError(int i) {
                String errorMeaning = "";

                sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                String userLanguage = sharedPreferences.getString("userLanguage", "en");

                if (userLanguage.equals("en")) {
                    if (i == 3) {
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
                }

                if (userLanguage.equals("ta")) {
                    if (i == 3) {
                        errorMeaning = "ஆடியோ பதிவு பிழை";
                    } else if (i == 2) {
                        errorMeaning = "பிணைய பிழை";
                    } else if (i == 1) {
                        errorMeaning = "பிணைய நேரம் முடிந்தது";
                    } else if (i == 7) {
                        errorMeaning = "பொருத்தம் இல்லை.  மீண்டும் முயற்சி செய்.";
                    } else if (i == 8) {
                        errorMeaning = "அங்கீகாரம் பிஸியாக உள்ளது.  சிறிது நேரம் கழித்து மீண்டும் முயற்சிக்கவும்.";
                    } else if (i == 11) {
                        errorMeaning = "சர்வர் துண்டிக்கப்பட்டது";
                    } else if (i == 6) {
                        errorMeaning = "பேச்சு உள்ளீடு இல்லை";
                    } else if (i == 10) {
                        errorMeaning = " பல கோரிக்கைகள். காத்திருந்து பிறகு முயற்சிக்கவும்.";
                    } else {
                        errorMeaning = "சில பிழை ஏற்பட்டது. காத்திருந்து பின்னர் முயற்சிக்கவும்.";
                    }
                }

                if (userLanguage.equals("ka")) {
                    if (i == 3) {
                        errorMeaning = "ಆಡಿಯೋ ರೆಕಾರ್ಡಿಂಗ್ ದೋಷ";
                    } else if (i == 2) {
                        errorMeaning = "ನೆಟ್ವರ್ಕ್ ದೋಷ";
                    } else if (i == 1) {
                        errorMeaning = "ನೆಟ್ವರ್ಕ್ ಅವಧಿ ಮೀರಿದೆ";
                    } else if (i == 7) {
                        errorMeaning = "ಹೊಂದಾಣಿಕೆ ಇಲ್ಲ.  ಮತ್ತೆ ಪ್ರಯತ್ನಿಸು.";
                    } else if (i == 8) {
                        errorMeaning = "ಗುರುತಿಸುವಿಕೆ ಕಾರ್ಯನಿರತವಾಗಿದೆ.  ಸ್ವಲ್ಪ ಸಮಯದ ನಂತರ ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ.";
                    } else if (i == 11) {
                        errorMeaning = "ಸರ್ವರ್ ಸಂಪರ್ಕ ಕಡಿತಗೊಂಡಿದೆ";
                    } else if (i == 6) {
                        errorMeaning = "ಸ್ಪೀಚ್ ಇನ್ಪುಟ್ ಇಲ್ಲ";
                    } else if (i == 10) {
                        errorMeaning = "ಹಲವಾರು ವಿನಂತಿಗಳು.  ನಿರೀಕ್ಷಿಸಿ ಮತ್ತು ನಂತರ ಪ್ರಯತ್ನಿಸಿ.";
                    } else {
                        errorMeaning = "ಕೆಲವು ದೋಷ ಸಂಭವಿಸಿದೆ.  ನಿರೀಕ್ಷಿಸಿ ಮತ್ತು ನಂತರ ಪ್ರಯತ್ನಿಸಿ.";
                    }
                }

                if (userLanguage.equals("hi")) {
                    if (i == 3) {
                        errorMeaning = "ऑडियो रिकॉर्डिंग त्रुटि";
                    } else if (i == 2) {
                        errorMeaning = "नेटवर्क त्रुटि";
                    } else if (i == 1) {
                        errorMeaning = "नेटवर्क टाइमआउट";
                    } else if (i == 7) {
                        errorMeaning = "कोई मुकाबला नहीं। पुनः प्रयास करें।";
                    } else if (i == 8) {
                        errorMeaning = "पहचानकर्ता व्यस्त है। कुछ देर में पुनः प्रयास करें।";
                    } else if (i == 11) {
                        errorMeaning = "सर्वर डिसकनेक्ट हो गया";
                    } else if (i == 6) {
                        errorMeaning = "कोई भाषण इनपुट नहीं";
                    } else if (i == 10) {
                        errorMeaning = "बहुत सारे अनुरोध. प्रतीक्षा करें और बाद में प्रयास करें।";
                    } else {
                        errorMeaning = "कुछ त्रुटि हुई। प्रतीक्षा करें और बाद में प्रयास करें।";
                    }
                }

                if (userLanguage.equals("fr")) {
                    if (i == 3) {
                        errorMeaning = "Erreur d'enregistrement audio";
                    } else if (i == 2) {
                        errorMeaning = "Erreur réseau";
                    } else if (i == 1) {
                        errorMeaning = "Délai d'expiration du réseau";
                    } else if (i == 7) {
                        errorMeaning = "Aucune concordance. Essayer à nouveau.";
                    } else if (i == 8) {
                        errorMeaning = "La reconnaissance est occupée. Réessayez dans quelques temps.";
                    } else if (i == 11) {
                        errorMeaning = "Serveur déconnecté";
                    } else if (i == 6) {
                        errorMeaning = "Aucune entrée vocale";
                    } else if (i == 10) {
                        errorMeaning = "Trop de demandes. Attends et essaie plus tard.;";
                    } else {
                        errorMeaning = "Une erreur s'est produite. Attendez et essayez plus tard.";
                    }
                }

                if (userLanguage.equals("ja")) {
                    if (i == 3) {
                        errorMeaning = "音声録音エラー、ネットワークエラー";
                    } else if (i == 2) {
                        errorMeaning = "ネットワークタイムアウト";
                    } else if (i == 1) {
                        errorMeaning = "勝ち目がない。もう一度やり直してください。、";
                    } else if (i == 7) {
                        errorMeaning = "認識装置がビジー状態です。しばらくしてからもう一度お試しください。、";
                    } else if (i == 8) {
                        errorMeaning = "サーバーが切断されました、";
                    } else if (i == 11) {
                        errorMeaning = "音声入力がありません";
                    } else if (i == 6) {
                        errorMeaning = ",リクエストが多すぎます。待ってから後で試してください。、";
                    } else if (i == 10) {
                        errorMeaning = "何らかのエラーが発生しました。待ってから後で試してください。";
                    } else {
                        errorMeaning = "何らかのエラーが発生しました。待ってから後で試してください。";
                    }
                }

                if (userLanguage.equals("ml")) {
                    if (i == 3) {
                        errorMeaning = "ഓഡിയോ റെക്കോർഡിംഗ് പിശക്,";
                    } else if (i == 2) {
                        errorMeaning = "നെറ്റ്\u200Cവർക്ക് പിശക്,";
                    } else if (i == 1) {
                        errorMeaning = "നെറ്റ്\u200Cവർക്ക് ടൈംഔട്ട്,";
                    } else if (i == 7) {
                        errorMeaning = "ചേർച്ച ഇല്ല. വീണ്ടും ശ്രമിക്കുക.,";
                    } else if (i == 8) {
                        errorMeaning = "റെക്കഗ്നൈസർ തിരക്കിലാണ്. കുറച്ച് സമയത്തിന് ശേഷം വീണ്ടും ശ്രമിക്കുക.";
                    } else if (i == 11) {
                        errorMeaning = "സെർവർ വിച്ഛേദിച്ചു,";
                    } else if (i == 6) {
                        errorMeaning = "സംഭാഷണ ഇൻപുട്ട് ഇല്ല";
                    } else if (i == 10) {
                        errorMeaning = "വളരെയധികം അഭ്യർത്ഥനകൾ. കാത്തിരുന്ന് പിന്നീട് ശ്രമിക്കുക.";
                    } else {
                        errorMeaning = "ചില പിശക് സംഭവിച്ചു. കാത്തിരുന്ന് പിന്നീട് ശ്രമിക്കുക.";
                    }
                }

                if (userLanguage.equals("es")) {
                    if (i == 3) {
                        errorMeaning = "Error de grabación de audio";
                    } else if (i == 2) {
                        errorMeaning = "Error de red";
                    } else if (i == 1) {
                        errorMeaning = "Tiempo de espera de la red";
                    } else if (i == 7) {
                        errorMeaning = "No Match. Try again.";
                    } else if (i == 8) {
                        errorMeaning = "El reconocedor está ocupado. Inténtalo de nuevo en algún momento.,";
                    } else if (i == 11) {
                        errorMeaning = "Servidor desconectado,";
                    } else if (i == 6) {
                        errorMeaning = "Sin entrada de voz";
                    } else if (i == 10) {
                        errorMeaning = "Too many requests. Wait and try later.";
                    } else {
                        errorMeaning = "Demasiadas solicitudes. Espera e inténtalo más tarde.";
                    }
                }

                if (userLanguage.equals("de")) {
                    if (i == 3) {
                        errorMeaning = "Fehler bei der Audioaufnahme";
                    } else if (i == 2) {
                        errorMeaning = "Netzwerkfehler";
                    } else if (i == 1) {
                        errorMeaning = "Keine Übereinstimmung. Versuchen Sie es erneut.";
                    } else if (i == 7) {
                        errorMeaning = "Die Erkennung ist beschäftigt. Versuchen Sie es später noch einmal.";
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
                }

                if (userLanguage.equals("nl")) {
                    if (i == 3) {
                        errorMeaning = "Audio-opnamefout";
                    } else if (i == 2) {
                        errorMeaning = "Netwerkfout";
                    } else if (i == 1) {
                        errorMeaning = "Netwerktime-out";
                    } else if (i == 7) {
                        errorMeaning = "Geen match. Probeer het nog eens.";
                    } else if (i == 8) {
                        errorMeaning = "Herkenner is bezig. Probeer het over enige tijd opnieuw.";
                    } else if (i == 11) {
                        errorMeaning = "Geen spraakinvoer";
                    } else if (i == 6) {
                        errorMeaning = "Te veel verzoeken. Wacht en probeer het later.";
                    } else if (i == 10) {
                        errorMeaning = "Er is een fout opgetreden. Wacht en probeer het later.";
                    } else {
                        errorMeaning = "Herkenner is bezig. Probeer het over enige tijd opnieuw.";
                    }
                }

                String errMsg = "Error: " + errorMeaning;
                tv.setText(errMsg);

                micButton.setImageResource(R.drawable.mic_ready);
                buttonHearSpokenAudio.setEnabled(false);
                buttonSendTranslatedText.setEnabled(false);
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
                String userLanguage = sharedPreferences.getString("userLanguage", "en");
                String outputLanguage = sharedPreferences.getString("outputLanguage", "en");
                if (SpeechToTextOP.length() > 0) {
                    if (outputLanguage.equals(userLanguage)) {
                        onKeyClick(v, SpeechToTextOP);
                    } else {
                        String translatedToOutputText = translateToOPLang(SpeechToTextOP, outputLanguage, userLanguage);
                        onKeyClick(v, translatedToOutputText);
                    }
                }
            }
        });

        buttonHearSpokenAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SpeechToTextOP.length() > 0) {
                    playUserTextAudio(SpeechToTextOP, userLanguage);
                }
            }
        });

        backspaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackspaceClick(v);
            }
        });


        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSendClick(v);
            }
        });
        return inputView;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        Integer keyboardBgColor = sharedPreferences.getInt("keyboardBackground",R.color.black);

        ConstraintLayout keyboardLayoutBG = inputView.findViewById(R.id.background);
        LinearLayout keyboardLL = inputView.findViewById(R.id.bgLL);

        keyboardLayoutBG.setBackgroundColor(keyboardBgColor);
        keyboardLL.setBackgroundColor(keyboardBgColor);
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

    public void addToHistory(String userLanguage, String targetLanguage, String translatedText, String originalText) {
        // Initialize the Firebase Realtime Database reference.
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference historyRef = database.getReference("history");

        // Create a new entry in the history.
        String key = historyRef.push().getKey();
        HistoryEntry historyEntry = new HistoryEntry(userLanguage, targetLanguage, translatedText, originalText);
        historyRef.child(key).setValue(historyEntry);
        //Toast.makeText(getApplicationContext(),"Wrote to History", Toast.LENGTH_SHORT).show();
    }

    // HistoryEntry class to represent the structure of each history entry.
    public class HistoryEntry {
        public String userLanguage;
        public String targetLanguage;
        public String translatedText;
        public String originalText;

        public HistoryEntry() {
            // Default constructor required for Firebase
        }

        public HistoryEntry(String userLanguage, String targetLanguage, String translatedText, String originalText) {
            this.userLanguage = userLanguage;
            this.targetLanguage = targetLanguage;
            this.translatedText = translatedText;
            this.originalText = originalText;
        }
    }

    public String translateToOPLang(final String userText, String outputLanguage, String inputLanguage) {
        final String[] result = {null};

        if (inputLanguage == outputLanguage) {
            return userText;
        } else {
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
    }

    public String translateTextToUserLang(final String userText, String outputLanguage) {
        final String[] result = {null};

        new Thread(new Runnable() {
            @Override
            public void run() {
                Translate translate = TranslateOptions.getDefaultInstance().getService();
                final String translatedText;
                try {
                    Translate translateService = TranslateOptions.getDefaultInstance().getService();
                    Translation translation = null;
                    
                    Detection detection = translateService.detect(userText);
                    String detectedLanguage = detection.getLanguage();

                    if (detectedLanguage.equals(outputLanguage)) {
                        translatedText = userText;
                    }

                    else {
                        translation = translateService.translate(
                                userText,
                                Translate.TranslateOption.sourceLanguage(detectedLanguage),
                                Translate.TranslateOption.targetLanguage(outputLanguage)
                        );
                        translatedText = translation.getTranslatedText();

                        addToHistory(outputLanguage, detectedLanguage, translatedText, userText);
                    }
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
            if (text.equals("\n")) {
                inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            } else {
                inputConnection.commitText(text, 1);
            }
        }
    }
}
