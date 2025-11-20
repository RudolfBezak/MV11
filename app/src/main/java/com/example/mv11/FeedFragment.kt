package com.example.mv11

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.atomic.AtomicInteger

class FeedFragment : Fragment() {

    private lateinit var feedAdapter: FeedAdapter
    private lateinit var viewModel: FeedViewModel
    private val idCounter = AtomicInteger(4) // Počítadlo pre unikátne ID nových položiek

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

        viewModel = ViewModelProvider(this)[FeedViewModel::class.java]

        val recyclerView = view.findViewById<RecyclerView>(R.id.feed_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(context)
        feedAdapter = FeedAdapter()
        recyclerView.adapter = feedAdapter

        val initialItems = listOf(
            MyItem(1, R.drawable.file_foreground, "Prvý"),
            MyItem(2, R.drawable.map_foreground, "Druhý"),
            MyItem(3, R.drawable.profile_foreground, "Tretí"),
        )

        viewModel.feedItems.observe(viewLifecycleOwner) { items ->
            feedAdapter.resetItems(items)
            Log.d("FeedFragment", "Aktualizované položky: ${items.size}")
        }

        viewModel.sampleString.observe(viewLifecycleOwner) { stringValue ->
            Log.d("FeedFragment", "Nový text: $stringValue")
        }

        viewModel.updateItems(initialItems)
        viewModel.updateString("Úvodný text")

        view.findViewById<Button>(R.id.btnAdd).setOnClickListener {
            val newItem = MyItem(idCounter.getAndIncrement(), R.drawable.file_foreground, "Nová položka")
            viewModel.addItem(newItem)
        }

        view.findViewById<Button>(R.id.btnRemove).setOnClickListener {
            viewModel.removeFirstItem()
        }

        view.findViewById<Button>(R.id.btnChange).setOnClickListener {
            viewModel.changeFirstItem()
        }

        view.findViewById<Button>(R.id.btnChangeRange).setOnClickListener {
            viewModel.changeRangeOfItems()
        }

        view.findViewById<Button>(R.id.btnReset).setOnClickListener {
            viewModel.updateItems(initialItems)
        }

        view.findViewById<Button>(R.id.btnCoroutines).setOnClickListener {
            findNavController().navigate(R.id.numberFragment)
        }
    }
}