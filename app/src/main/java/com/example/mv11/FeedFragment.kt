package com.example.mv11

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mv11.databinding.FragmentFeedBinding
import com.google.android.material.snackbar.Snackbar

class FeedFragment : Fragment(R.layout.fragment_feed) {

    private var binding: FragmentFeedBinding? = null
    private lateinit var feedAdapter: FeedAdapter
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
            this.viewModel = this@FeedFragment.viewModel

            bottomNavigationWidget.setActiveItem(BottomNavItem.LIST)

            feedRecyclerview.layoutManager = LinearLayoutManager(context)
            feedAdapter = FeedAdapter()
            feedRecyclerview.adapter = feedAdapter
        }

        viewModel.feed_items.observe(viewLifecycleOwner) { users ->
            val userList = users.filterNotNull()
            val sortedList = userList.sortedByDescending { it.lat != 0.0 && it.lon != 0.0 }
            feedAdapter.updateUsers(sortedList)
            Log.d("FeedFragment", "Aktualizované používateľov: ${sortedList.size}")
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("FeedFragment", "Loading: $isLoading")
        }

        viewModel.message.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { message ->
                if (message.isNotEmpty()) {
                    Log.e("FeedFragment", "Error: $message")
                    binding?.feedRecyclerview?.let { recyclerView ->
                        if (message == "MUSIS_SI_ZAPNUT_GEOFENCE") {
                            Snackbar.make(
                                recyclerView,
                                getString(R.string.error_enable_geofence),
                                Snackbar.LENGTH_LONG
                            ).show()
                        } else {
                            Snackbar.make(
                                recyclerView,
                                message,
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        val user = PreferenceData.getInstance().getUser(requireContext())
        if (user != null && user.access.isNotEmpty()) {
            viewModel.updateItems(user.access)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}