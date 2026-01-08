package com.example.translationapp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryFragment : Fragment(R.layout.fragment_history) {

    // 1. Connect to the same ViewModel/Database
    private val viewModel: TranslationViewModel by viewModels {
        TranslationViewModelFactory((requireActivity().application as TranslationApplication).database.historyDao())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. Setup the RecyclerView
        // Note: You must add a RecyclerView to fragment_history.xml first! (See Step 4 below)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerview_history)
        val adapter = HistoryAdapter()

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // 3. Observe the Database
        // Whenever data changes, this code runs automatically
        viewModel.allHistory.observe(viewLifecycleOwner) { items ->
            // Update the list
            items.let { adapter.submitList(it) }
        }
    }
}