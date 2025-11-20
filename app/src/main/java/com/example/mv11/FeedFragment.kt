package com.example.mv11

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.mv11.databinding.FragmentFeedBinding
import java.util.concurrent.atomic.AtomicInteger

/**
 * FeedFragment - Fragment pre zobrazenie feedu používateľov.
 * 
 * Používa DataBinding pre väzbu medzi UI a ViewModelom.
 * 
 * Výhody DataBinding:
 * - Automatická aktualizácia UI pri zmene dát
 * - Menej boilerplate kódu (žiadne findViewById)
 * - Typová kontrola v compile-time
 * - Čistejší a čitateľnejší kód
 */
class FeedFragment : Fragment() {

    /**
     * DataBinding objekt - automaticky generovaný z fragment_feed.xml.
     * 
     * FragmentFeedBinding obsahuje:
     * - Všetky UI komponenty z layoutu (napr. binding.btnAdd)
     * - Metódu bind() pre vytvorenie väzby
     * - Property 'viewModel' pre nastavenie ViewModelu
     * - Property 'lifecycleOwner' pre lifecycle-aware binding
     */
    private var binding: FragmentFeedBinding? = null

    private lateinit var feedAdapter: FeedAdapter
    private lateinit var feedViewModel: FeedViewModel  // Pre testovacie tlačidlá
    private lateinit var userFeedViewModel: UserFeedViewModel  // Pre geofence dáta
    private val idCounter = AtomicInteger(4) // Počítadlo pre unikátne ID nových položiek

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicializácia ViewModelov v onCreate (pred vytvorením view)
        feedViewModel = ViewModelProvider(this)[FeedViewModel::class.java]
        userFeedViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UserFeedViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[UserFeedViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // DataBinding automaticky generuje FragmentFeedBinding z fragment_feed.xml
        // Inflate layoutu pomocou DataBindingUtil alebo bind() metódy
        binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nastavenie DataBinding
        binding?.apply {
            // lifecycleOwner - umožňuje DataBinding reagovať na lifecycle zmeny
            // (napr. automaticky zruší observables pri onDestroy)
            lifecycleOwner = viewLifecycleOwner

            // model - nastaví ViewModel do XML layoutu
            // Teraz môžeš používať viewModel v XML (napr. android:onClick="@{() -> viewModel.updateItems()}")
            this.viewModel = feedViewModel

            // Nastavenie bottom navigation
            bottomNavigationWidget.setActiveItem(BottomNavItem.LIST)

            // Nastavenie RecyclerView
            feedRecyclerview.layoutManager = LinearLayoutManager(context)
            feedAdapter = FeedAdapter()
            feedRecyclerview.adapter = feedAdapter

            // PULL-TO-REFRESH: Nastavenie SwipeRefreshLayout
            swipeRefreshLayout.setOnRefreshListener {
                Log.d("FeedFragment", "Pull-to-refresh triggered")
                // Obnov dáta z geofence API
                userFeedViewModel.updateItems()
            }

            // Pozorovanie loading stavu pre SwipeRefreshLayout
            userFeedViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                swipeRefreshLayout.isRefreshing = isLoading
            }

            // Pozorovanie zmien v geofence dát (UserEntity)
            userFeedViewModel.feed_items.observe(viewLifecycleOwner) { users ->
                val validUsers = users.filterNotNull()
                Log.d("FeedFragment", "Aktualizované geofence dáta: ${validUsers.size} používateľov")
                
                // Konvertuj UserEntity na MyItem pre FeedAdapter
                val items = validUsers.mapIndexed { index, user ->
                    MyItem(
                        index + 1,
                        R.drawable.map_foreground,
                        "${user.name} (${user.lat}, ${user.lon})"
                    )
                }
                feedAdapter.resetItems(items)
            }

            // Pozorovanie zmien v feedItems LiveData (pre testovacie tlačidlá)
            feedViewModel.feedItems.observe(viewLifecycleOwner) { items ->
                // Toto sa používa len pre testovacie tlačidlá
                Log.d("FeedFragment", "Test items updated: ${items.size}")
            }

            // Pozorovanie zmien v sampleString LiveData
            feedViewModel.sampleString.observe(viewLifecycleOwner) { stringValue ->
                Log.d("FeedFragment", "Nový text: $stringValue")
            }

            // OBOJSMERNÝ BINDING: Pozorovanie zmien v userInput
            feedViewModel.userInput.observe(viewLifecycleOwner) { input ->
                Log.d("FeedFragment", "User input changed: $input")
                feedViewModel.onUserInputChanged()
            }

            // JEDNOSMERNÝ BINDING: Pozorovanie zmien v displayText
            feedViewModel.displayText.observe(viewLifecycleOwner) { text ->
                Log.d("FeedFragment", "Display text changed: $text")
            }

            // Inicializácia testovacích dát
            val initialItems = listOf(
                MyItem(1, R.drawable.file_foreground, "Prvý"),
                MyItem(2, R.drawable.map_foreground, "Druhý"),
                MyItem(3, R.drawable.profile_foreground, "Tretí"),
            )

            feedViewModel.updateItems(initialItems)
            feedViewModel.updateString("Úvodný text")

            // btnCoroutines ešte používa onClick v kóde (pre navigáciu)
            btnCoroutines.setOnClickListener {
                findNavController().navigate(R.id.numberFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Dôležité: Vymaž binding pri zničení view (zabráni memory leak)
        binding = null
    }
}