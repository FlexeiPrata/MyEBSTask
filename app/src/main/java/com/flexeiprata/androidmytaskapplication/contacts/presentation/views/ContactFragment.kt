package com.flexeiprata.androidmytaskapplication.contacts.presentation.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.flexeiprata.androidmytaskapplication.R
import com.flexeiprata.androidmytaskapplication.contacts.presentation.adapters.ContactsAdapter
import com.flexeiprata.androidmytaskapplication.contacts.presentation.uimodels.ContactsUIModel
import com.flexeiprata.androidmytaskapplication.databinding.ContactFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class ContactFragment : Fragment() {

    private var _binding: ContactFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactViewModel by viewModels()
    private val args: ContactFragmentArgs by navArgs()

    private lateinit var adapter: ContactsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContactFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()
    }

    private fun updateUI() {
        binding.ContactsRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = ContactsAdapter().apply {
            setShareListener { number ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("smsto:")
                    type = "vnd.android-dir/mms-sms"
                    putExtra("address", number)
                    putExtra(
                        "sms_body",
                        String.format(getString(R.string.share_message), args.name, args.link)
                    )
                }
                startActivity(intent)
            }
        }
        binding.ContactsRV.adapter = adapter
        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.contactList.collect { state ->
                    when (state) {
                        is ContactsResult.Success -> updateList(state.data)
                        is ContactsResult.Error -> Toast.makeText(
                            requireContext(),
                            "Error during loading",
                            Toast.LENGTH_SHORT
                        ).show()
                        is ContactsResult.Loading -> {

                        }
                    }
                }
            }
        }
        viewModel.registerObserver(requireContext())
        viewModel.registerRX()
        binding.mainToolbar.setHomeOnClickListener {
            findNavController().popBackStack()
        }

    }

    private fun updateList(list: List<ContactsUIModel?>) {
        adapter.submitList(list)
    }

    override fun onDestroyView() {
        _binding = null
        viewModel.unregisterObserver(requireContext())
        super.onDestroyView()
    }
}
