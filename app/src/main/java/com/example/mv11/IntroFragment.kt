package com.example.mv11

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.mv11.databinding.FragmentIntroBinding

class IntroFragment : Fragment(R.layout.fragment_intro) {

    private var binding: FragmentIntroBinding? = null
    private lateinit var viewModel: IntroViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return IntroViewModel(requireActivity().application) as T
            }
        })[IntroViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentIntroBinding.bind(view).apply {
            lifecycleOwner = viewLifecycleOwner
            this.viewModel = this@IntroFragment.viewModel
        }.also { bnd ->
            bnd.button1.setOnClickListener {
                findNavController().navigate(R.id.action_intro_to_prihlasenie)
            }

            bnd.button2.setOnClickListener {
                findNavController().navigate(R.id.action_intro_to_signup)
            }

            bnd.buttonMap.setOnClickListener {
                findNavController().navigate(R.id.mapFragment)
            }

            bnd.buttonUsers.setOnClickListener {
                findNavController().navigate(R.id.feedFragment)
            }

            bnd.buttonProfile.setOnClickListener {
                findNavController().navigate(R.id.profileFragment)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshUserStatus()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}

