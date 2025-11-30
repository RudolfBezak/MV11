package com.example.mv11

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

class FeedFragment : Fragment() {

    private lateinit var feedAdapter: FeedAdapter
    private lateinit var viewModel: UserFeedViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNav = view.findViewById<BottomNavigationWidget>(R.id.bottomNavigationWidget)
        bottomNav.setActiveItem(BottomNavItem.LIST)

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UserFeedViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[UserFeedViewModel::class.java]

        val recyclerView = view.findViewById<RecyclerView>(R.id.feed_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(context)
        feedAdapter = FeedAdapter()
        recyclerView.adapter = feedAdapter

        viewModel.feed_items.observe(viewLifecycleOwner) { users ->
            val userList = users.filterNotNull()
            feedAdapter.updateUsers(userList)
            Log.d("FeedFragment", "Aktualizované používateľov: ${userList.size}")
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("FeedFragment", "Loading: $isLoading")
        }

        viewModel.message.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { message ->
                if (message.isNotEmpty()) {
                    Log.e("FeedFragment", "Error: $message")
                    if (message == "MUSIS_SI_ZAPNUT_GEOFENCE") {
                        Snackbar.make(
                            view.findViewById(R.id.feed_recyclerview),
                            getString(R.string.error_enable_geofence),
                            Snackbar.LENGTH_LONG
                        ).show()
                    } else {
                        Snackbar.make(
                            view.findViewById(R.id.feed_recyclerview),
                            message,
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        // Load users on fragment creation
        val user = PreferenceData.getInstance().getUser(requireContext())
        if (user != null && user.access.isNotEmpty()) {
            viewModel.updateItems(user.access)
        }
    }
}