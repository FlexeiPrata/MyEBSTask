package com.flexeiprata.androidmytaskapplication.favorites.presentation.view

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.flexeiprata.androidmytaskapplication.common.LOG_DEBUG
import com.flexeiprata.androidmytaskapplication.databinding.FavFragmentBinding
import com.flexeiprata.androidmytaskapplication.products.data.models.Product
import com.flexeiprata.androidmytaskapplication.products.presentation.adapter.ProductsAdapter
import com.flexeiprata.androidmytaskapplication.products.presentation.uimodels.ProductUIModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class FavoriteFragment : Fragment(), ProductsAdapter.FavoriteSwitch {

    private var _binding: FavFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavViewModel by viewModels()
    private var adapter: ProductsAdapter = ProductsAdapter(this)
    private var mainList = listOf<Product>()
    private var isRefresh = false
    private lateinit var refresher: SwipeRefreshLayout.OnRefreshListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isRefresh = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FavFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRecyclerViewUI()
        setupObservers()
        binding.cartButton.setOnClickListener {
            viewModel.clearCart()
            Toast.makeText(requireContext(), "Test: Clear Cart", Toast.LENGTH_SHORT).show()
        }
        refresher = SwipeRefreshLayout.OnRefreshListener {
            lifecycleScope.launch {
                viewModel.actualizeData(mainList)
                withContext(Dispatchers.Main) {
                    binding.swiper.isRefreshing = false
                }
            }
        }
        binding.swiper.setOnRefreshListener(refresher)

        binding.mainToolbar.setHomeOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launchWhenCreated {
            viewModel.getCart().collectLatest {
                binding.cartButton.setCounter(it.size)
            }
            viewModel.getAllFav().collectLatest {
                updateAdapter(it)
                mainList = it
                binding.favCountText.text = it.size.toString()
            }
        }
    }

    private fun updateAdapter(dataList: List<Product>) {
        adapter.apply {
            lifecycleScope.launch {
                if (isRefresh) {
                    binding.swiper.post {
                        binding.swiper.isRefreshing = true
                        refresher.onRefresh()
                    }
                    isRefresh = false
                }
                Log.d(LOG_DEBUG, "Here is updater UI")
                submitData(PagingData.from(dataList.map {
                    Log.d(LOG_DEBUG, "${it.name}: $${it.price}")
                    ProductUIModel(it, true)
                }))
            }
        }
    }


    private fun setRecyclerViewUI() {
        val magicLayoutManager = GridLayoutManager(requireContext(), 1)
        binding.mainRV.apply {
            layoutManager = magicLayoutManager
            adapter = this@FavoriteFragment.adapter
            addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun deleteFav(fav: Product) {
        viewModel.deleteFav(fav)
    }

    override fun insertFav(fav: Product) {
        viewModel.insertFav(fav)
    }

    override fun addToCart(product: Product) {
        viewModel.addToCart(product)
    }

    override fun navigateToNext(id: Int) {
        findNavController().navigate(
            FavoriteFragmentDirections.actionFavoriteFragmentToDescFragment(
                id
            )
        )
    }

    override fun getSpanCount() = 1
}