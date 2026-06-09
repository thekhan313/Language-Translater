package com.speechlanguageconverter.ui.conversation

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.speechlanguageconverter.databinding.FragmentConversationBinding
import com.speechlanguageconverter.R
import com.speechlanguageconverter.adapter.ConversationAdapter
import com.speechlanguageconverter.data.model.TranslationResult
import com.speechlanguageconverter.utils.SpeechUtils
import com.speechlanguageconverter.viewmodels.ConversationViewModel

class ConversationFragment : Fragment() {

    private var _binding: FragmentConversationBinding? = null
    private var pendingIsSource = true
    private val binding get() = _binding!!
    private val viewModel: ConversationViewModel by viewModels()
    private lateinit var speechUtils: SpeechUtils
    private lateinit var adapter: ConversationAdapter
    private var isListening = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConversationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpeech()
        setupSpinners()
        setupRecyclerView()
        setupObservers()
        setupMicButton()
    }

    private fun setupSpeech() {
        speechUtils = SpeechUtils(requireContext())
        speechUtils.initTTS {}
    }

    // In setupSpinners(), replace the existing spinner setup with:

    private fun setupSpinners() {
        // Show current selection as text only (no dropdown)
        updateSpinnerLabels()

        // Receive result from LanguageSelectFragment
        setFragmentResultListener("source") { _, bundle ->
            val lang = bundle.getString("selectedLanguage") ?: return@setFragmentResultListener
            viewModel.sourceLanguageName = lang
            updateSpinnerLabels()
        }
        setFragmentResultListener("target") { _, bundle ->
            val lang = bundle.getString("selectedLanguage") ?: return@setFragmentResultListener
            viewModel.targetLanguageName = lang
            updateSpinnerLabels()
        }

        binding.spinnerSource.setOnClickListener {
            val action = ConversationFragmentDirections
                .actionConversationToLanguageSelect(
                    selectedLanguage = viewModel.sourceLanguageName,
                    requestKey = "source"
                )
            findNavController().navigate(action)
        }

        binding.spinnerTarget.setOnClickListener {
            val action = ConversationFragmentDirections
                .actionConversationToLanguageSelect(
                    selectedLanguage = viewModel.targetLanguageName,
                    requestKey = "target"
                )
            findNavController().navigate(action)
        }

        binding.btnSwapLanguages.setOnClickListener {
            val tmp = viewModel.sourceLanguageName
            viewModel.sourceLanguageName = viewModel.targetLanguageName
            viewModel.targetLanguageName = tmp
            updateSpinnerLabels()
        }
    }

    private fun updateSpinnerLabels() {
        binding.spinnerSource.text = viewModel.sourceLanguageName  // change to TextView in layout
        binding.spinnerTarget.text = viewModel.targetLanguageName
    }

    private fun setupRecyclerView() {
        adapter = ConversationAdapter(
            onSpeak = { item -> speechUtils.speak(item.translatedText, item.targetLanguage) },
            onCopy = { item -> copyToClipboard(item.translatedText) },
            onShare = { item -> shareText(item) },
            onDelete = { pos -> adapter.removeAt(pos) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.conversations.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list.toList())
            if (list.isNotEmpty()) binding.recyclerView.scrollToPosition(list.size - 1)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupMicButton() {
        binding.btnMicLeft.setOnClickListener { startListening(isSource = true) }
        binding.btnMicRight.setOnClickListener { startListening(isSource = false) }
    }

    private fun startListening(isSource: Boolean) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.RECORD_AUDIO), 100
            )
            return
        }

        pendingIsSource = isSource
        val lang = if (isSource)
            binding.spinnerSource.text.toString()
        else
            binding.spinnerTarget.text.toString()

        if (isSource) binding.btnMicLeft.setImageResource(R.drawable.ic_mic_active)
        else binding.btnMicRight.setImageResource(R.drawable.ic_mic_active)

        try {
            speechLauncher.launch(speechUtils.createRecognizerIntent(lang))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No speech service found. Install Google App.", Toast.LENGTH_LONG).show()
            binding.btnMicLeft.setImageResource(R.drawable.ic_mic)
            binding.btnMicRight.setImageResource(R.drawable.ic_mic)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("translation", text))
        Toast.makeText(requireContext(), "Copied!", Toast.LENGTH_SHORT).show()
    }

    private fun shareText(item: TranslationResult) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${item.originalText}\n→ ${item.translatedText}")
        }
        startActivity(Intent.createChooser(intent, "Share"))
    }


    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val sourceLang = binding.spinnerSource.text.toString()
                val targetLang = binding.spinnerTarget.text.toString()

                if (pendingIsSource) {
                    viewModel.translateAndAdd(
                        spokenText = matches[0],
                        sourceLang = sourceLang,
                        targetLang = targetLang
                    )
                } else {
                    viewModel.translateAndAdd(
                        spokenText = matches[0],
                        sourceLang = targetLang,   // flipped
                        targetLang = sourceLang    // flipped
                    )
                }
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        speechUtils.release()
        _binding = null
    }
}
