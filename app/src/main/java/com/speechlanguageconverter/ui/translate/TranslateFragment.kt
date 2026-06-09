package com.speechlanguageconverter.ui.translate

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.speechlanguageconverter.databinding.FragmentTranslateBinding
import com.speechlanguageconverter.utils.SpeechUtils
import com.speechlanguageconverter.viewmodels.TranslateViewModel

class TranslateFragment : Fragment() {

    private var _binding: FragmentTranslateBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TranslateViewModel by viewModels()
    private lateinit var speechUtils: SpeechUtils

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTranslateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        setupSpeech()
        setupObservers()
        setupButtons()
    }

    private fun setupSpinners() {
        updateSpinnerLabels()

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
            val action = TranslateFragmentDirections
                .actionTranslateToLanguageSelect(
                    selectedLanguage = viewModel.sourceLanguageName,
                    requestKey = "source"
                )
            findNavController().navigate(action)
        }

        binding.spinnerTarget.setOnClickListener {
            val action = TranslateFragmentDirections
                .actionTranslateToLanguageSelect(
                    selectedLanguage = viewModel.targetLanguageName,
                    requestKey = "target"
                )
            findNavController().navigate(action)
        }

        binding.btnSwap.setOnClickListener {
            val tmp = viewModel.sourceLanguageName
            viewModel.sourceLanguageName = viewModel.targetLanguageName
            viewModel.targetLanguageName = tmp
            updateSpinnerLabels()
        }
    }

    private fun updateSpinnerLabels() {
        binding.spinnerSource.text = viewModel.sourceLanguageName
        binding.spinnerTarget.text = viewModel.targetLanguageName
    }

    private fun setupSpeech() {
        speechUtils = SpeechUtils(requireContext())
        speechUtils.initTTS {}
    }

    private fun setupObservers() {
        viewModel.translatedText.observe(viewLifecycleOwner) {
            binding.tvTranslatedOutput.text = it
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnTranslate.isEnabled = !loading
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupButtons() {
        binding.btnTranslate.setOnClickListener {
            val text = binding.etInputText.text.toString()
            viewModel.translate(text, viewModel.sourceLanguageName, viewModel.targetLanguageName)
        }

        binding.btnMic.setOnClickListener { startVoiceInput() }
        binding.btnSpeakOutput.setOnClickListener {
            val text = binding.tvTranslatedOutput.text.toString()
            if (text.isNotBlank()) speechUtils.speak(text, viewModel.targetLanguageName)
        }
    }

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                binding.etInputText.setText(matches[0])
                viewModel.translate(
                    matches[0],
                    viewModel.sourceLanguageName,
                    viewModel.targetLanguageName
                )
            }
        }
    }

    private fun startVoiceInput() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.RECORD_AUDIO), 101
            )
            return
        }
        try {
            speechLauncher.launch(
                speechUtils.createRecognizerIntent(viewModel.sourceLanguageName)
            )
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No speech service found. Install Google App.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechUtils.release()
        _binding = null
    }
}
