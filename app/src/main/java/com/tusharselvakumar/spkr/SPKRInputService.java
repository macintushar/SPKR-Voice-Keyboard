package com.tusharselvakumar.spkr;

import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;

import android.widget.EditText;

import android.widget.Toast;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.translate.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;


public class SPKRInputService extends InputMethodService {

    private static final int LAYOUT_HEAR = 0;
    private static final int LAYOUT_SPEAK = 1;

    private int currentLayout = LAYOUT_SPEAK;
    private TextToSpeech t1;
    private SharedPreferences sharedPreferences;
    private Translate translate;

    @Override
    public View onCreateInputView() {
        View inputView;
        if (currentLayout == LAYOUT_HEAR) {
            inputView = getLayoutInflater().inflate(R.layout.keyboard_layout_hear, null);

            System.setProperty("GOOGLE_API_KEY", "AIzaSyCiLb6TjmziGfsGAFKaM-bf6tg-yVY3ksE");
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
                    String translatedToUserLanguageText = translateTextToUserLang(userInputText, userLanguage);
                    playTranslatedAudio(translatedToUserLanguageText);

                    String msg = userInputText;
                    Toast.makeText(getApplicationContext(), msg,Toast.LENGTH_SHORT).show();
                }
            });




        } else {
            inputView = getLayoutInflater().inflate(R.layout.keyboard_layout_speak, null);

            Button buttonA = inputView.findViewById(R.id.key_a);
            Button buttonB = inputView.findViewById(R.id.key_b);
            Button buttonC = inputView.findViewById(R.id.key_c);
            Button buttonD = inputView.findViewById(R.id.key_d);
            Button backspaceBtn = inputView.findViewById(R.id.backspaceButton);

            int recordState = 0;

            buttonA.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentLayout == LAYOUT_SPEAK) {
                        Toast.makeText(getApplicationContext(),"Recording",Toast.LENGTH_SHORT).show();
                    }
                }
            });

            buttonB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentLayout == LAYOUT_SPEAK) {
                        onKeyClick(v);
                    }
                }
            });

            buttonC.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentLayout == LAYOUT_SPEAK) {
                        onKeyClick(v);
                    }
                }
            });

            buttonD.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentLayout == LAYOUT_SPEAK) {
                        onKeyClick(v);
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

    public String translateTextToUserLang(final String userText, String outputLanguage) {
        final String[] result = {null};

        new Thread(new Runnable() {
            @Override
            public void run() {
                Translate translate = TranslateOptions.getDefaultInstance().getService();
                try {
                    Translate translateService = TranslateOptions.getDefaultInstance().getService();

                    Detection detection = translateService.detect(userText);
                    String detectedLanguage = detection.getLanguage();

                    Translation translation = translateService.translate(
                            userText,
                            Translate.TranslateOption.sourceLanguage(detectedLanguage),
                            Translate.TranslateOption.targetLanguage(outputLanguage)
                    );

                    final String translatedText = translation.getTranslatedText();

                    // Set the translated text result
                    synchronized (result) {
                        result[0] = translatedText;
                    }

                } catch (com.google.cloud.translate.TranslateException e) {
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
    public void onKeyClick(View view) {
        if (view instanceof Button) {
            String keyText = ((Button) view).getText().toString();
            InputConnection inputConnection = getCurrentInputConnection();
            inputConnection.commitText(keyText, 1);
        }
    }
}
