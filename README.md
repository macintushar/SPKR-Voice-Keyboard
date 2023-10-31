# SPKR-Voice-Keyboard

# Using
Find the latest release from the releases page and download and install the APK.

# Building
To build this project for yourself, you need to do the following steps:
1. create a folder called assets in `SPKRVoiceKeyboard/app/src/main/`
2. Add a file called credentials.json and add the following value:
   ```
    {
       "APIKEY": "API_KEY_FROM_GOOGLE-CLOUD-PLATFORM"
    }
    ```
      To get the API KEY, please follow this <a href="">blog post</a> I made.
3. Go to Firebase and create a new Android Project, get the google-services.json file and store it in the `/app` folder
4. Use AndroidStudio to build the App