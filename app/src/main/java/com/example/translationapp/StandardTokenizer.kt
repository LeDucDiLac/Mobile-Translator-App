package com.example.translationapp

import android.content.Context
import org.json.JSONObject
import java.util.Locale

class StandardTokenizer(context: Context) {

    private val wordToId = mutableMapOf<String, Long>()
    private val idToWord = mutableMapOf<Long, String>()

    // Default Special Tokens (Must match your Python training!)
    private val PAD_TOKEN = "<PAD>"
    private val UNK_TOKEN = "<UNK>"
    private var padId = 0L
    private var unkId = 1L

    init {
        try {
            // 1. Load vocab.json from Assets
            val jsonString = context.assets.open("vocab.json")
                .bufferedReader().use { it.readText() }

            val jsonObject = JSONObject(jsonString)

            // 2. Parse into Maps
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val word = keys.next()
                val id = jsonObject.getLong(word)
                wordToId[word] = id
                idToWord[id] = word
            }

            // 3. Set special IDs if they exist in vocab
            padId = wordToId[PAD_TOKEN] ?: 0L
            unkId = wordToId[UNK_TOKEN] ?: 1L

        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback for safety to prevent crash
            wordToId[UNK_TOKEN] = 1L
            idToWord[1L] = UNK_TOKEN
        }
    }

    /**
     * ENCODE: Text -> ID Array
     * Example: "Hello world" -> [24, 99, 0, 0...]
     */
    fun tokenize(text: String, maxLength: Int = 60): LongArray {
        // Simple whitespace tokenizer.
        // Note: For BPE/WordPiece, you would need a more complex splitter here.
        val cleanText = text.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N} ]"), "")
        val words = cleanText.split("\\s+".toRegex()).filter { it.isNotEmpty() }

        val ids = LongArray(maxLength) { padId } // Fill with padding first

        for (i in words.indices) {
            if (i >= maxLength) break
            val word = words[i]
            // Map word to ID, or use UNK_ID if not found
            ids[i] = wordToId[word] ?: unkId
        }
        return ids
    }

    /**
     * DECODE: ID Array -> Text
     * Example: [24, 99, 0] -> "hello world"
     */
    fun decode(ids: LongArray): String {
        val rawText = ids.asSequence()
            .map { id -> idToWord[id] ?: "" }
            .filter { it != PAD_TOKEN && it != "<SOS>" && it != "<EOS>" && it.isNotEmpty() }
            .joinToString(" ") // 1. Join with spaces first (Default behavior)

        // 2. SMART FIX: Detect Khmer characters and remove spaces
        // The Range \u1780-\u17FF covers the Khmer alphabet
        if (rawText.contains(Regex("[\\u1780-\\u17FF]"))) {
            return rawText.replace(" ", "") // Remove spaces for Khmer
        }

        return rawText // Keep spaces for Vi/En
    }
}