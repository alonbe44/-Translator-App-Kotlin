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

        /* Calling the initialize() function. */
        initialize()

        /* This is a lambda expression. It is a function that is passed as an argument to another
        function, when the output Speaker image is clicked */
        outputSpeakButton.setOnClickListener {
            /* Checking if the outputTextView text view is empty. If it is empty, it will show a
            toast with the message "No Text to be spoken". */
            if (outputTextView.text.isEmpty()) {
                Toast.makeText(this,"No Text to be spoken", Toast.LENGTH_LONG).show()
            }
            else {
                /* Getting the selected language from the spinner. */
                val selectedLang = targetLanguageSpinner.selectedItem.toString()

                /* The above code is checking if the selected language is available in the TextToSpeech
                engine. If it is available, then it is setting the language of the TextToSpeech
                engine to the selected language. Then it is speaking the text that is in the
                outputTextView text view. */
                if (textToSpeech.isLanguageAvailable(Locale(selectedLang)) == TextToSpeech.LANG_AVAILABLE) {
                    /* Setting the language of the TextToSpeech engine to the selected language. */
                    textToSpeech.language = Locale.forLanguageTag(selectedLang.substring(0, 2))
                    /* Speaking the text that is in the outputTextView text view. */
                    textToSpeech.speak(outputTextView.text, TextToSpeech.QUEUE_ADD, null, null)
                }

                /* Checking the language of the device and showing the alert in the same language. */
                else
                {
                    showAlert(this,getString(R.string.language_not_available))
                }
            }
        }

        /* This is a lambda expression. It is a function that is passed as an argument to another
            function, when the input Speaker image is clicked */
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

                /* The above code is checking if the language selected by the user is available on the
                device. If it is available, then the language is set to the selected language and
                the text is spoken. */
                if (textToSpeech.isLanguageAvailable(Locale(selectedLang)) == TextToSpeech.LANG_AVAILABLE) {

                    /* Setting the language of the TextToSpeech engine to the selected language. */
                    textToSpeech.language = Locale.forLanguageTag(selectedLang.substring(0, 2))

                    /* Speaking the text that is in the inputTextView text view. */
                    textToSpeech.speak(inputTextView.text, TextToSpeech.QUEUE_ADD, null, null)
                }

                /* Checking the language of the device and showing the alert in the same language. */
                else
                {
                    showAlert(this,getString(R.string.language_not_available))
                }
            }
        }

        /* This is a lambda expression. It is a function that is passed as an argument to another
         function, when the Translation Button is clicked */
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
                            if (matchesArray.getJSONObject(i).getDouble("match") >= highestMatch) {
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

    /**
     * This function initializes all the variables that will be used in the code
     */
    private fun initialize(){

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

                /* Creating an array of strings including all supported languages . */
                val supportedLanguage= arrayOf(
                    "af-ZA:Afrikaans",
                    "sq-AL:Albanian",
                    "am-ET:Amharic",
                    "ar-SA:Arabic",
                    "hy-AM:Armenian",
                    "az-AZ:Azerbaijani",
                    "bjs-BB:Bajan",
                    "rm-RO:BalkanGipsy",
                    "eu-ES:Basque",
                    "bem-ZM:Bemba",
                    "bn-IN:Bengali",
                    "be-BY:Bielarus",
                    "bi-VU:Bislama",
                    "bs-BA:Bosnian",
                    "br-FR:Breton",
                    "bg-BG:Bulgarian",
                    "my-MM:Burmese",
                    "ca-ES:Catalan",
                    "cb-PH:Cebuano",
                    "ch-GU:Chamorro",
                    "zh-CN:Chinese(Simplified)",
                    "zh-TW:ChineseTraditional",
                    "zdj-KM:Comorian(Ngazidja)",
                    "cop-EG:Coptic",
                    "aig-AG:CreoleEnglish(AntiguaandBarbuda)",
                    "bah-BS:CreoleEnglish(Bahamas)",
                    "gcl-GD:CreoleEnglish(Grenadian)",
                    "gyn-GY:CreoleEnglish(Guyanese)",
                    "jam-JM:CreoleEnglish(Jamaican)",
                    "svc-VC:CreoleEnglish(Vincentian)",
                    "vic-US:CreoleEnglish(VirginIslands)",
                    "ht-HT:CreoleFrench(Haitian)",
                    "acf-LC:CreoleFrench(SaintLucian)",
                    "crs-SC:CreoleFrench(Seselwa)",
                    "pov-GW:CreolePortuguese(UpperGuinea)",
                    "hr-HR:Croatian",
                    "cs-CZ:Czech",
                    "da-DK:Danish",
                    "nl-NL:Dutch",
                    "dz-BT:Dzongkha",
                    "en-GB:English",
                    "eo-EU:Esperanto",
                    "et-EE:Estonian",
                    "fn-FNG:Fanagalo",
                    "fo-FO:Faroese",
                    "fi-FI:Finnish",
                    "fr-FR:French",
                    "gl-ES:Galician",
                    "ka-GE:Georgian",
                    "de-DE:German",
                    "el-GR:Greek",
                    "grc-GR:Greek(Classical)",
                    "gu-IN:Gujarati",
                    "ha-NE:Hausa",
                    "haw-US:Hawaiian",
                    "he-IL:Hebrew",
                    "hi-IN:Hindi",
                    "hu-HU:Hungarian",
                    "is-IS:Icelandic",
                    "id-ID:Indonesian",
                    "kl-GL:Inuktitut(Greenlandic)",
                    "ga-IE:IrishGaelic",
                    "it-IT:Italian",
                    "ja-JP:Japanese",
                    "jv-ID:Javanese",
                    "kea-CV:Kabuverdianu",
                    "kab-DZ:Kabylian",
                    "kn-IN:Kannada",
                    "kk-KZ:Kazakh",
                    "km-KM:Khmer",
                    "rw-RW:Kinyarwanda",
                    "rn-BI:Kirundi",
                    "ko-KR:Korean",
                    "ku-TR:Kurdish",
                    "ckb-IQ:KurdishSorani",
                    "ky-KG:Kyrgyz",
                    "lo-LA:Lao",
                    "la-VA:Latin",
                    "lv-LV:Latvian",
                    "lt-LT:Lithuanian",
                    "lb-LU:Luxembourgish",
                    "mk-MK:Macedonian",
                    "mg-MG:Malagasy",
                    "ms-MY:Malay",
                    "dv-MV:Maldivian",
                    "mt-MT:Maltese",
                    "gv-IM:ManxGaelic",
                    "mi-NZ:Maori",
                    "mh-MH:Marshallese",
                    "men-SL:Mende",
                    "mn-MN:Mongolian",
                    "mfe-MU:Morisyen",
                    "ne-NP:Nepali",
                    "niu-NU:Niuean",
                    "no-NO:Norwegian",
                    "ny-MW:Nyanja",
                    "ur-PK:Pakistani",
                    "pau-PW:Palauan",
                    "pa-IN:Panjabi",
                    "pap-CW:Papiamentu",
                    "ps-PK:Pashto",
                    "fa-IR:Persian",
                    "pis-SB:Pijin",
                    "pl-PL:Polish",
                    "pt-PT:Portuguese",
                    "pot-US:Potawatomi",
                    "qu-PE:Quechua",
                    "ro-RO:Romanian",
                    "ru-RU:Russian",
                    "sm-WS:Samoan",
                    "sg-CF:Sango",
                    "gd-GB:ScotsGaelic",
                    "sr-RS:Serbian",
                    "sn-ZW:Shona",
                    "si-LK:Sinhala",
                    "sk-SK:Slovak",
                    "sl-SI:Slovenian",
                    "so-SO:Somali",
                    "st-ST:Sotho,Southern",
                    "es-ES:Spanish",
                    "srn-SR:SrananTongo",
                    "sw-SZ:Swahili",
                    "sv-SE:Swedish",
                    "de-CH:SwissGerman",
                    "syc-TR:Syriac(Aramaic)",
                    "tl-PH:Tagalog",
                    "tg-TJ:Tajik",
                    "tmh-DZ:Tamashek(Tuareg)",
                    "ta-LK:Tamil",
                    "te-IN:Telugu",
                    "tet-TL:Tetum",
                    "th-TH:Thai",
                    "bo-CN:Tibetan",
                    "ti-TI:Tigrinya",
                    "tpi-PG:TokPisin",
                    "tkl-TK:Tokelauan",
                    "to-TO:Tongan",
                    "tn-BW:Tswana",
                    "tr-TR:Turkish",
                    "tk-TM:Turkmen",
                    "tvl-TV:Tuvaluan",
                    "uk-UA:Ukrainian",
                    "ppk-ID:Uma",
                    "uz-UZ:Uzbek",
                    "vi-VN:Vietnamese",
                    "wls-WF:Wallisian",
                    "cy-GB:Welsh",
                    "wo-SN:Wolof",
                    "xh-ZA:Xhosa",
                    "yi-YD:Yiddish",
                    "zu-ZA:Zulu"
                )

                // Create an adapter for the spinner using the language list
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, supportedLanguage)

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

        // Take instance of Action Bar
        // using getSupportActionBar and
        // if it is not Null
        // then call hide function
        if (supportActionBar != null) {
            supportActionBar!!.hide()
        }
    }

}