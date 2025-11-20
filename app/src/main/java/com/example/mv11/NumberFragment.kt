package com.example.mv11

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class NumberFragment : Fragment() {
    
    private lateinit var viewModel: NumberViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_number, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[NumberViewModel::class.java]

        val textViewNumber = view.findViewById<TextView>(R.id.textViewNumber)
        val buttonGenerate = view.findViewById<Button>(R.id.buttonGenerate)
        val textViewLoading = view.findViewById<TextView>(R.id.textViewLoading)

        viewModel.randomNumber.observe(viewLifecycleOwner) { number ->
            textViewNumber.text = number.toString()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                textViewLoading.visibility = View.VISIBLE
                buttonGenerate.isEnabled = false
            } else {
                textViewLoading.visibility = View.GONE
                buttonGenerate.isEnabled = true
            }
        }

        buttonGenerate.setOnClickListener {
            viewModel.generateRandomNumber()
        }
    }
}

