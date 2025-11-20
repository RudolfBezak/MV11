package com.example.mv11

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

class UserListFragment : Fragment() {

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

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UserFeedViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[UserFeedViewModel::class.java]

        val recyclerView = view.findViewById<RecyclerView>(R.id.feed_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(context)

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
                    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}

