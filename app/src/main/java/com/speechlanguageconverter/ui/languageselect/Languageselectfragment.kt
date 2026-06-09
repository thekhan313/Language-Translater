package com.speechlanguageconverter.ui.languageselect

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.speechlanguageconverter.adapter.LanguageSelectAdapter
import com.speechlanguageconverter.databinding.FragmentLanguageselectBinding
import com.speechlanguageconverter.viewmodels.LanguageSelectViewModel

class LanguageSelectFragment : Fragment() {

    private var _binding: FragmentLanguageselectBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LanguageSelectViewModel by viewModels()
    private lateinit var adapter: LanguageSelectAdapter
    private val args: LanguageSelectFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguageselectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupObservers()

        viewModel.loadLanguages(args.selectedLanguage)
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView() {
        adapter = LanguageSelectAdapter(
            onLanguageSelected = { languageName ->
                // Send result back to the fragment that opened this screen
                val bundle = Bundle().apply {
                    putString("selectedLanguage", languageName)
                    putString("requestKey", args.requestKey)
                }
                setFragmentResult(args.requestKey, bundle)
                findNavController().popBackStack()
            },
            onDownloadClicked = { languageName ->
                viewModel.downloadModel(languageName)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.filter(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupObservers() {
        viewModel.displayedLanguages.observe(viewLifecycleOwner) { groups ->
            adapter.submitGroups(groups)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
