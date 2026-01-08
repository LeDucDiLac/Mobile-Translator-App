package com.example.translationapp

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import java.nio.FloatBuffer

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TranslationFragment : Fragment(R.layout.fragment_translation) {

    // Connect to the ViewModel (Database)
    private val viewModel: TranslationViewModel by viewModels {
        TranslationViewModelFactory((requireActivity().application as TranslationApplication).database.historyDao())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 1. SETUP UI ELEMENTS ---
        val spinnerSource = view.findViewById<Spinner>(R.id.spinner_source_lang)
        val spinnerTarget = view.findViewById<Spinner>(R.id.spinner_target_lang)
        val btnSwap = view.findViewById<ImageView>(R.id.btn_swap_lang)
        val etInput = view.findViewById<EditText>(R.id.et_input)
        val btnTranslate = view.findViewById<Button>(R.id.btn_translate)
        val tvOutput = view.findViewById<TextView>(R.id.tv_output)
        val tvStatus = view.findViewById<TextView>(R.id.tv_model_status)

        // --- 2. SETUP LANGUAGE SPINNERS (Mock Data) ---
        // In the future, these will come from your model metadata
        val languages = arrayOf("Vietnamese", "Khmer", "English")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)

        spinnerSource.adapter = adapter
        spinnerTarget.adapter = adapter
        spinnerTarget.setSelection(1) // Default to Khmer

        // Swap Button Logic
        btnSwap.setOnClickListener {
            val sourcePos = spinnerSource.selectedItemPosition
            spinnerSource.setSelection(spinnerTarget.selectedItemPosition)
            spinnerTarget.setSelection(sourcePos)
        }

        btnTranslate.setOnClickListener {
            val inputText = etInput.text.toString()
            if (inputText.isBlank()) return@setOnClickListener

            tvStatus.text = "Model: Running..."
            btnTranslate.isEnabled = false // Disable button so they don't click twice

            // --- START COROUTINE ---
            lifecycleScope.launch(Dispatchers.Main) { // 1. Start on Main Thread (to update UI)

                // 2. Switch to Background Thread (Worker) for heavy AI math
                val onnxResult = withContext(Dispatchers.Default) {
                    runOnnxDummy() // Now this runs safely in background!
                }

                // 3. Back on Main Thread automatically
                val translatedText = "Translated: $inputText ($onnxResult)"

                tvOutput.text = translatedText
                tvStatus.text = "Model: Success"
                btnTranslate.isEnabled = true

                // Save to DB (ViewModel already handles its own coroutines, so this is safe)
                viewModel.addTranslation(inputText, translatedText)
            }
        }
    }

    private fun runOnnxDummy(): String {
        return try {
            val assetManager = requireContext().assets
            val env = OrtEnvironment.getEnvironment()

            // Read model
            val modelBytes = assetManager.open("dummy.onnx").readBytes()
            val session = env.createSession(modelBytes)

            // Input Data (Dummy numbers)
            val inputData = floatArrayOf(1f, 2f, 3f, 4f, 5f)
            val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), longArrayOf(1, 5))

            // Run Inference
            val inputs = mapOf("input_tensor" to inputTensor)
            val result = session.run(inputs)
            val outputTensor = result[0] as OnnxTensor

            // Get Result
            val outputArray = FloatArray(5)
            outputTensor.floatBuffer.get(outputArray)

            // Cleanup
            result.close()
            session.close()
            env.close()

            // Return a snippet of the result to prove it ran
            "[${outputArray[0].toString().take(3)}..]"

        } catch (e: Exception) {
            "Error"
        }
    }
}