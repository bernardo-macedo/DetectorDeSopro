package br.com.orb.detectordesopro

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*


val REQUEST_MICROPHONE = 1
var shouldRecord: Boolean = false

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        init()
        button.setOnClickListener { if (shouldRecord) stopListening() else init() }
    }

    private fun init() {
        if (hasToAskPermission()) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MICROPHONE)
        } else {
            startListening()
        }
    }

    private fun hasToAskPermission() = ContextCompat.checkSelfPermission(this,
            Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening()
        }
    }

    fun startListening() {
        if (hasToAskPermission()) {
            stopListening()
            return
        }
        button.text = "Parar"
        textView.text = "Ouvindo"
        shouldRecord = true
        MicrofoneTask { s -> onBlowDetected(s) }.execute()
    }

    fun stopListening() {
        shouldRecord = false
        button.text = "Iniciar"
        textView.text = "Não ouvindo"
    }

    fun onBlowDetected(text: String) {
        textView.text = text
    }


    class MicrofoneTask(val listener: (String) -> Unit) : AsyncTask<Void, Void, Void?>() {

        override fun doInBackground(vararg p0: Void?): Void? {
            val minSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val ar = AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minSize)


            val buffer = ShortArray(minSize)

            ar.startRecording()
            while (shouldRecord) {

                ar.read(buffer, 0, minSize)
                for (s in buffer) {
                    if (isBlowing(s)) {
                        val blowValue = Math.abs(s.toInt())
                        Handler(Looper.getMainLooper()).post {
                            listener("Valor do sopro = $blowValue")
                        }
                    }

                }
            }
            ar.stop()
            return null
        }

        private fun isBlowing(s: Short) = Math.abs(s.toInt()) > 27000
    }
}
