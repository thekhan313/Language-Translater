package com.speechlanguageconverter.ui.ocr

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.speechlanguageconverter.databinding.FragmentOcrBinding
import com.speechlanguageconverter.viewmodels.OcrViewModel
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OcrFragment : Fragment() {

    private var _binding: FragmentOcrBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OcrViewModel by viewModels()
    private var cameraImageUri: Uri? = null

    // ── Camera launcher ───────────────────────────────────────────────────────
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            binding.tvStatus.text = "Opening crop screen…"
            cameraImageUri?.let { launchCrop(it) }
        }
    }

    // ── Gallery launcher ──────────────────────────────────────────────────────
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            binding.tvStatus.text = "Opening crop screen…"
            launchCrop(it)
        }
    }

    // ── PDF launcher ──────────────────────────────────────────────────────────
    private val pdfLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.extractFromPdf(it) }
    }

    // ── Speech launcher ───────────────────────────────────────────────────────
    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@registerForActivityResult
        val spoken = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull() ?: return@registerForActivityResult
        val current = binding.etExtractedText.text.toString()
        val updated = if (current.isEmpty()) spoken else "$current $spoken"
        binding.etExtractedText.setText(updated)
        binding.etExtractedText.setSelection(updated.length)
        binding.tvStatus.text = "Speech captured ✓"
    }

    // ── uCrop launcher ────────────────────────────────────────────────────────
    private val cropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@registerForActivityResult
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(data)
            if (resultUri != null) viewModel.runOcr(resultUri)
            else binding.tvStatus.text = "Crop failed — no output URI"
        } else {
            val error = UCrop.getError(data)
            binding.tvStatus.text = "Crop error: ${error?.message ?: "unknown"}"
        }
    }

    // ── Permission launchers ──────────────────────────────────────────────────
    private val cameraPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    private val storagePermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) galleryLauncher.launch("image/*")
        else Toast.makeText(requireContext(), "Storage permission denied", Toast.LENGTH_SHORT).show()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOcrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    // ── Click listeners ───────────────────────────────────────────────────────
    private fun setupClickListeners() {
        binding.btnCamera.setOnClickListener { checkCameraPermission() }
        binding.btnGallery.setOnClickListener { checkStoragePermission() }
        binding.btnPdf.setOnClickListener { pdfLauncher.launch(arrayOf("application/pdf")) }
        binding.btnCheck.setOnClickListener {
            viewModel.checkGrammar(binding.etExtractedText.text.toString().trim())
        }
        binding.btnSpeech.setOnClickListener { startSpeechInput() }
        binding.btnCopyInput.setOnClickListener { copyText(binding.etExtractedText.text.toString(), "Extracted Text") }
        binding.btnShareInput.setOnClickListener { shareText(binding.etExtractedText.text.toString()) }
        binding.btnDeleteInput.setOnClickListener {
            binding.etExtractedText.setText("")
            binding.tvStatus.text = "Text cleared"
        }
        binding.btnCopy.setOnClickListener { copyText(binding.tvCorrectedText.text.toString(), "Corrected Text") }
        binding.btnShareResult.setOnClickListener { shareText(binding.tvCorrectedText.text.toString()) }
        binding.btnDeleteResult.setOnClickListener { viewModel.dismissResult() }
    }

    // ── Observers ─────────────────────────────────────────────────────────────
    private fun observeViewModel() {
        viewModel.extractedText.observe(viewLifecycleOwner) { text ->
            binding.etExtractedText.setText(text)
            binding.etExtractedText.setSelection(text.length)
        }
        viewModel.correctedText.observe(viewLifecycleOwner) { binding.tvCorrectedText.text = it }
        viewModel.suggestions.observe(viewLifecycleOwner) { text ->
            binding.tvSuggestions.text = text
            binding.tvSuggestions.visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.statusMessage.observe(viewLifecycleOwner) { binding.tvStatus.text = it }
        viewModel.showResultCard.observe(viewLifecycleOwner) { show ->
            binding.cardResult.visibility = if (show) View.VISIBLE else View.GONE
            if (!show) { binding.tvCorrectedText.text = ""; binding.tvSuggestions.text = "" }
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    // ── Permissions ───────────────────────────────────────────────────────────
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> openCamera()

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(
                    requireContext(),
                    "Camera permission is needed to take photos for OCR",
                    Toast.LENGTH_LONG
                ).show()
                cameraPermLauncher.launch(Manifest.permission.CAMERA)
            }

            else -> cameraPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun checkStoragePermission() {
        val permissionsToRequest = getStoragePermissions()

        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(requireContext(), it) ==
                    PackageManager.PERMISSION_GRANTED
        }

        when {
            allGranted -> galleryLauncher.launch("image/*")

            permissionsToRequest.any {
                shouldShowRequestPermissionRationale(it)
            } -> {
                Toast.makeText(
                    requireContext(),
                    "Storage permission is needed to pick images",
                    Toast.LENGTH_LONG
                ).show()
                storagePermLauncher.launch(permissionsToRequest)
            }

            else -> storagePermLauncher.launch(permissionsToRequest)
        }
    }

    private fun getStoragePermissions(): Array<String> {
        return when {
            // Android 14, 15, 16 (API 34+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            // Android 13 (API 33)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            )
            // Android 12 and below
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    // ── Camera ────────────────────────────────────────────────────────────────
    private fun openCamera() {
        val photoFile = createImageFile()
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", photoFile
        )
        cameraLauncher.launch(cameraImageUri)
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("IMG_${timestamp}_", ".jpg", storageDir)
    }

    // ── Speech ────────────────────────────────────────────────────────────────
    private fun startSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now…")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Speech not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    // ── uCrop ─────────────────────────────────────────────────────────────────
    private fun launchCrop(sourceUri: Uri) {
        val destFile = File(requireContext().cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        val destUri = Uri.fromFile(destFile)
        val intent = UCrop.of(sourceUri, destUri)
            .withAspectRatio(0f, 0f)
            .withMaxResultSize(2048, 2048)
            .withOptions(UCrop.Options().apply {
                setCompressionQuality(90)
                setFreeStyleCropEnabled(true)
            })
            .getIntent(requireContext())
        cropLauncher.launch(intent)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun copyText(text: String, label: String) {
        if (text.isEmpty()) { Toast.makeText(requireContext(), "Nothing to copy", Toast.LENGTH_SHORT).show(); return }
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(requireContext(), "Copied!", Toast.LENGTH_SHORT).show()
    }

    private fun shareText(text: String) {
        if (text.isEmpty()) { Toast.makeText(requireContext(), "Nothing to share", Toast.LENGTH_SHORT).show(); return }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }, "Share via"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}