package com.example.translator

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.*
class MainActivity : AppCompatActivity() {

    /* Declaring the variables that will be used in the code. */

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var httpClient: OkHttpClient
    private lateinit var outputTextView: TextView
    private lateinit var inputTextView: TextView
    private lateinit var outputSpeakButton: ImageButton
    private lateinit var inputSpeakButton: ImageButton
    private lateinit var sourceLanguageSpinner: Spinner
    private lateinit var targetLanguageSpinner: Spinner
    private lateinit var startTranslationButton: Button

    private var sourceLanguageCode = "en"
    private var targetLanguageCode = "ar"


    override fun onCreate(savedInstanceState: Bundle?) {
        /* Calling the super class's onCreate method. */
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Take instance of Action Bar
        // using getSupportActionBar and
        // if it is not Null
        // then call hide function
        if (supportActionBar != null) {
            supportActionBar!!.hide()
        }

        /* Initializing the variables that will be used in the code. */
        httpClient = OkHttpClient()
        outputTextView = findViewById(R.id.output)
        inputTextView = findViewById(R.id.input)
        outputSpeakButton = findViewById(R.id.imageButton)
        inputSpeakButton = findViewById(R.id.imageButton2)
        sourceLanguageSpinner = findViewById(R.id.spinner)
        targetLanguageSpinner = findViewById(R.id.spinner2)
        startTranslationButton = findViewById(R.id.button)


        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this) { status ->
            // Check the initialization status
            if (status == TextToSpeech.SUCCESS) {
                // If successful, get all available languages
                val allLanguages = textToSpeech.availableLanguages
                // Map the languages to display the language code and display name
                val allLang = allLanguages.map { "${it.language}: ${it.displayName}" }

                // Create an adapter for the spinner using the language list
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, allLang)

                // Set the adapter for both sourceLanguageSpinner and targetLanguageSpinner
                sourceLanguageSpinner.adapter = adapter
                targetLanguageSpinner.adapter = adapter

                // Set the initial selection for both spinners
                sourceLanguageSpinner.setSelection(1)
                targetLanguageSpinner.setSelection(1)
            } else {
                // textToSpeech engine initialization failed
            }
        }

        /* This is checking if the device is connected to the internet. If it is not connected to the
        internet, it will show an alert with the message "No internet connection". */
        if(!isOnline(this)) {
            showAlert(this, getString(R.string.error_network_connection))
        }

        /* This is a lambda expression. It is a function that is passed as an argument to another
        function. */
        outputSpeakButton.setOnClickListener {
            /* Checking if the outputTextView text view is empty. If it is empty, it will show a
            toast with the message "No Text to be spoken". */
            if (outputTextView.text.isEmpty()) {
                Toast.makeText(this,"No Text to be spoken", Toast.LENGTH_LONG).show()
            }
            else {
                /* Getting the selected language from the spinner. */
                val selectedLang = targetLanguageSpinner.selectedItem.toString()
                /* Setting the language of the TextToSpeech engine to the selected language. */
                textToSpeech.language = Locale.forLanguageTag(selectedLang.substring(0, 2))
                /* Speaking the text that is in the outputTextView text view. */
                textToSpeech.speak(outputTextView.text, TextToSpeech.QUEUE_ADD, null, null)
            }
        }

        /* This is a lambda expression. It is a function that is passed as an argument to another
        function. */
        inputSpeakButton.setOnClickListener {
            /* This is checking if the outputTextView text view is empty. If it is empty, it will show
            a
            toast with the message "No Text to be spoken". */
            if (inputTextView.text.isEmpty()) {
                Toast.makeText(this, "No Text to be spoken", Toast.LENGTH_LONG).show()
            }
            else {
                /* Getting the selected language from the spinner. */
                val selectedLang = sourceLanguageSpinner.selectedItem.toString()

                /* Setting the language of the TextToSpeech engine to the selected language. */
                textToSpeech.language = Locale.forLanguageTag(selectedLang.substring(0, 2))

                /* Speaking the text that is in the inputTextView text view. */
                textToSpeech.speak(inputTextView.text, TextToSpeech.QUEUE_ADD, null, null)
            }
        }

        /* This is a lambda expression. It is a function that is passed as an argument to another
        function. */
        startTranslationButton.setOnClickListener {
            /* This is checking if the device is connected to the internet. If it is not connected to
            the
            internet, it will show an alert with the message "No internet connection". */
            if (!isOnline(this)) {
                showAlert(this, getString(R.string.error_network_connection))
            }
            /* Checking if the inputTextView is empty. If it is empty, it will show an alert. */
            else if(inputTextView.text.isEmpty()){
                showAlert(this, getString(R.string.empty_text))
            }
            else {
                /* Getting the text from the inputTextView text view and converting it to a string. */
                val text = inputTextView.text.toString()
                /* Getting the selected language from the spinner and setting it to the variable
            `sourceLanguageCode`. */
                sourceLanguageCode = sourceLanguageSpinner.selectedItem.toString().substring(0, 2)
                /* Getting the selected language from the spinner and setting it to the variable
            `targetLanguageCode`. */
                targetLanguageCode = targetLanguageSpinner.selectedItem.toString().substring(0, 2)
                /* Sending the text to the translation API and getting the response. */
                sendQuestion(text)
            }
        }
    }

    /**
     * We create a request for the translation API, enqueue it and handle the response
     *
     * @param inputText The text to be translated.
     */
    private fun sendQuestion(inputText: String) {
        // getting the selected language codes from the spinners
        sourceLanguageCode = sourceLanguageSpinner.selectedItem.toString().substring(0, 2)
        targetLanguageCode = targetLanguageSpinner.selectedItem.toString().substring(0, 2)

        if(sourceLanguageCode==targetLanguageCode) {
            showAlert(this,"You cant translate from $sourceLanguageCode to $targetLanguageCode")
            return
        }

        // creating a request for translation API
        val request = Request.Builder()
            .url("https://api.mymemory.translated.net/get?q=$inputText&langpair=$sourceLanguageCode|$targetLanguageCode")
            .get()
            .build()

        /* The above code is making a request to the Yandex API to translate the text in the
        inputTextView. */
        try {
            // enqueue the request and handle the response
            httpClient.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {

                        // parse the response body and extract the translation
                        val responseJson = JSONObject(response.body.string())
                        val matchesArray = responseJson.getJSONArray("matches")
                        var highestMatch = -9999.0
                        var bestMatchTranslation = ""

                        /* Checking if the matchesArray is empty. If it is, it throws an exception and
                        returns. */
                        if(matchesArray.length()==0){
                            return
                        }

                        /* Looping through the matchesArray and getting the translation with the highest
                    match. */
                        for (i in 0 until matchesArray.length()) {
                            if (matchesArray.getJSONObject(i).getDouble("match") > highestMatch) {
                                bestMatchTranslation =
                                    matchesArray.getJSONObject(i).getString("translation")
                                highestMatch = matchesArray.getJSONObject(i).getDouble("match")
                            }
                        }

                        // set the final translation as the text of the outputTextView
                        outputTextView.text = bestMatchTranslation
                    } else {
                        // handle unsuccessful response
                        outputTextView.text = R.string.error.toString()
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    // handle failure in response
                    outputTextView.text = getString(R.string.error_response, e.message)
                }
            })
        }

        /* Trying to catch an exception and show an alert. */
        catch (e:Exception){
            showAlert(this,e.message.toString())
        }
    }

    /**
     * If the device is connected to a cellular network, wifi network, or Ethernet network, return
     * true. Otherwise, return false
     *
     * @param context The context of the activity or fragment
     * @return A boolean value that indicates whether the device is connected to the internet or not.
     */
    private fun isOnline(context: Context): Boolean {
        // Get the connectivity manager service
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // If the connectivity manager is not null
        // Get the network capabilities of the active network
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        // If the capabilities are not null
        if (capabilities != null) {
            // Check if the device is connected to a cellular network
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                return true
            }
            // Check if the device is connected to a wifi network
            else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                return true
            }
            // Check if the device is connected to an Ethernet network
            else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                return true
            }
        }
        // If none of the above conditions are met, return false
        return false
    }


    /**
     * If the device is online, dismiss the dialog. If the device is offline, show an alert with the
     * "No internet connection" message
     *
     * @param context The context of the activity that is calling the function
     * @param message The message to display in the dialog
     */
    private fun showAlert(context: Context, message: String) {
        // Create an AlertDialog.Builder instance
        val builder = AlertDialog.Builder(context)

        // Set the message to display in the dialog
        builder.setMessage(message)

            // Make the dialog non-cancelable
            .setCancelable(false)

            // Add a "Retry" button to the dialog
            .setNeutralButton("Retry") { dialog, _ ->
                // If the device is online, dismiss the dialog
                if(isOnline(context)){
                    dialog.dismiss()
                }
                // If the device is offline, show an alert with the "No internet connection" message
                else{
                    showAlert(context, message)
                }
            }

        // Create an AlertDialog instance using the builder
        val alert = builder.create()

        // Show the dialog
        alert.show()
    }


}