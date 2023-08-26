package com.tusharselvakumar.spkr;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    }
}