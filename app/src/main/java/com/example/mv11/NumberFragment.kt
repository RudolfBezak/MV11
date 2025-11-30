package com.example.mv11

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mv11.databinding.FragmentNumberBinding

class NumberFragment : Fragment(R.layout.fragment_number) {
    
    private var binding: FragmentNumberBinding? = null
    private lateinit var viewModel: NumberViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[NumberViewModel::class.java]

        binding = FragmentNumberBinding.bind(view).apply {
            lifecycleOwner = viewLifecycleOwner
            this.viewModel = this@NumberFragment.viewModel
        }.also { bnd ->
            viewModel.randomNumber.observe(viewLifecycleOwner) { number ->
                bnd.textViewNumber.text = number.toString()
            }

            viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                if (isLoading) {
                    bnd.textViewLoading.visibility = View.VISIBLE
                    bnd.buttonGenerate.isEnabled = false
                } else {
                    bnd.textViewLoading.visibility = View.GONE
                    bnd.buttonGenerate.isEnabled = true
                }
            }

            bnd.buttonGenerate.setOnClickListener {
                viewModel.generateRandomNumber()
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}

