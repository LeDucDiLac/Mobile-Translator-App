package com.example.translationapp

import androidx.core.content.res.ResourcesCompat
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.LongBuffer

class TranslationFragment : Fragment(R.layout.fragment_translation) {

    private val viewModel: TranslationViewModel by viewModels {
        TranslationViewModelFactory((requireActivity().application as TranslationApplication).database.historyDao())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // UI Setup
        val etInput = view.findViewById<EditText>(R.id.et_input)
        val btnTranslate = view.findViewById<Button>(R.id.btn_translate)
        val tvOutput = view.findViewById<TextView>(R.id.tv_output)
        val tvStatus = view.findViewById<TextView>(R.id.tv_model_status)
        val spinnerSource = view.findViewById<Spinner>(R.id.spinner_source_lang)

        // Setup simple language spinner
        val languages = arrayOf("Vietnamese", "Khmer", "English")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)
        spinnerSource.adapter = adapter

        btnTranslate.setOnClickListener {
            val inputText = etInput.text.toString().trim()
            if (inputText.isEmpty()) {
                etInput.error = "Please enter text"
                return@setOnClickListener
            }

            // Disable UI while processing
            btnTranslate.isEnabled = false
            tvStatus.text = "Status: Loading..."
            tvOutput.text = "..."

            // --- START ASYNC TRANSLATION ---
            lifecycleScope.launch(Dispatchers.Main) {
                try {
                    val result = withContext(Dispatchers.Default) {
                        performTranslation(inputText)
                    }

                    // --- UPDATE UI ---
                    val tvOutput = view.findViewById<TextView>(R.id.tv_output)
                    tvOutput.text = result

                    // SWITCH FONT HERE
                    updateOutputFont(result)

                    tvStatus.text = "Status: Success"
                    viewModel.addTranslation(inputText, result)

                } catch (e: Exception) {
                    Log.e("TranslationApp", "Inference Failed", e)
                    tvStatus.text = "Error: ${e.message}"
                    tvOutput.text = "Translation Failed"
                } finally {
                    btnTranslate.isEnabled = true
                }
            }
        }
    }

    // --- THE REAL AI LOGIC ---
    private fun performTranslation(text: String): String {
        val env = OrtEnvironment.getEnvironment()
        val tokenizer = StandardTokenizer(requireContext())

        // 1. Tokenize Input
        // Convert "Hello" -> [34, 55, 1, 0...]
        val inputIds = tokenizer.tokenize(text, maxLength = 60)

        // 2. Prepare ONNX Tensor
        // Shape: [1, 20] (Batch Size 1, Sequence Length 20)
        val shape = longArrayOf(1, inputIds.size.toLong())
        val inputBuffer = LongBuffer.wrap(inputIds)
        val inputTensor = OnnxTensor.createTensor(env, inputBuffer, shape)

        // 3. Load Session & Run Inference
        // Note: In production, load the session ONCE in onCreate, not every click.
        val modelBytes = requireContext().assets.open("model.onnx").readBytes()
        val session = env.createSession(modelBytes)

        // "input_ids" must match the name in your Python script:
        // torch.onnx.export(..., input_names=['input_ids'], ...)
        val inputs = mapOf("input_ids" to inputTensor)

        val results = session.run(inputs)

        // 4. Extract Output
        // Assuming model returns [1, Sequence_Length] of IDs
        val outputTensor = results[0] as OnnxTensor
        val outputBuffer = outputTensor.longBuffer
        val outputArray = LongArray(outputBuffer.remaining())
        outputBuffer.get(outputArray)

        // 5. Cleanup
        inputTensor.close()
        results.close()
        session.close() // Close session to free RAM
        env.close()

        // 6. Decode Output
        // Convert [55, 99...] -> "Xin chào"
        return tokenizer.decode(outputArray)
    }
    private fun updateOutputFont(text: String) {
        // 1. Regex to check for ANY Khmer character
        val isKhmer = text.contains(Regex("[\\u1780-\\u17FF]"))

        // 2. Load the font dynamically
        val typeface = if (isKhmer) {
            // Load Battambang for Khmer
            ResourcesCompat.getFont(requireContext(), R.font.battambang_regular)
        } else {
            // Reset to standard Roboto/Default for En/Vi
            android.graphics.Typeface.DEFAULT
        }

        // 3. Apply to TextView
        val tvOutput = requireView().findViewById<TextView>(R.id.tv_output)
        tvOutput.typeface = typeface
    }
}