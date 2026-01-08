package com.example.translationapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // Ensure you have fragment-ktx dependency
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import java.nio.FloatBuffer

class TranslationFragment : Fragment(R.layout.fragment_translation) {

    // 1. Initialize the ViewModel
    private val viewModel: TranslationViewModel by viewModels {
        TranslationViewModelFactory((requireActivity().application as TranslationApplication).database.historyDao())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnTest = view.findViewById<Button>(R.id.btn_test_onnx)

        btnTest?.setOnClickListener {
            runOnnxDummy()
        }
    }

    private fun runOnnxDummy() {
        try {
            // ... (Your existing ONNX code Setup) ...
            val assetManager = requireContext().assets
            val env = OrtEnvironment.getEnvironment()
            val modelBytes = assetManager.open("dummy.onnx").readBytes()
            val session = env.createSession(modelBytes)

            // ... (Your existing ONNX code Inference) ...
            val inputData = floatArrayOf(1f, 2f, 3f, 4f, 5f)
            val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), longArrayOf(1, 5))
            val inputs = mapOf("input_tensor" to inputTensor)
            val result = session.run(inputs)
            val outputTensor = result[0] as OnnxTensor
            val outputArray = FloatArray(5)
            outputTensor.floatBuffer.get(outputArray)
            val resultString = outputArray.joinToString(", ")

            // --- NEW PART: SAVE TO DATABASE ---

            // We save the "Dummy Input" and the "Result"
            viewModel.addTranslation("Input: 1,2,3,4,5", "Output: $resultString")

            Toast.makeText(requireContext(), "Saved to DB: $resultString", Toast.LENGTH_SHORT).show()

            // ... (Cleanup code) ...
            result.close()
            session.close()
            env.close()

        } catch (e: Exception) {
            Log.e("Capstone", "Error", e)
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}