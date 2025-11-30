package com.example.mv11

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mv11.databinding.FragmentFeedBinding
import com.google.android.material.snackbar.Snackbar

class UserListFragment : Fragment(R.layout.fragment_feed) {

    private var binding: FragmentFeedBinding? = null
    private lateinit var viewModel: UserFeedViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UserFeedViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[UserFeedViewModel::class.java]

        binding = FragmentFeedBinding.bind(view).apply {
            lifecycleOwner = viewLifecycleOwner
            this.viewModel = this@UserListFragment.viewModel
        }

        binding?.let { bnd ->
            bnd.feedRecyclerview.layoutManager = LinearLayoutManager(context)

            viewModel.feed_items.observe(viewLifecycleOwner) { users ->
                users.filterNotNull().forEach { user ->
                    android.util.Log.d("UserListFragment", "User: ${user.name}, lat: ${user.lat}, lon: ${user.lon}")
                }
            }

            viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                android.util.Log.d("UserListFragment", "Loading: $isLoading")
            }

            viewModel.message.observe(viewLifecycleOwner) { evento ->
                evento.getContentIfNotHandled()?.let { message ->
                    if (message.isNotEmpty()) {
                        Snackbar.make(bnd.root, message, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}

