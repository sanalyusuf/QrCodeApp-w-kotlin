package com.sanal.qrcodeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.sanal.qrcodeapp.databinding.FragmentScannerBinding

class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!
    private lateinit var codeScanner: CodeScanner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if ((activity as MainActivity).checkCameraPermission()) {
            setupScanner()
        }

        binding.buttonRescan.setOnClickListener {
            binding.layoutScanResult.visibility = View.GONE
            binding.scannerView.visibility = View.VISIBLE
            codeScanner.startPreview()
        }

        binding.buttonOpenUrl.setOnClickListener {
            val url = binding.textScanResult.text.toString()
            if (url.startsWith("http://") || url.startsWith("https://")) {
                (activity as MainActivity).openUrl(url)
            } else {
                Toast.makeText(context, "Geçerli bir URL değil", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupScanner() {
        val scannerView = binding.scannerView
        val activity = requireActivity()

        codeScanner = CodeScanner(activity, scannerView)
        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.formats = CodeScanner.ALL_FORMATS
        codeScanner.autoFocusMode = AutoFocusMode.SAFE
        codeScanner.scanMode = ScanMode.SINGLE
        codeScanner.isAutoFocusEnabled = true
        codeScanner.isFlashEnabled = false

        // QR kod okunduğunda
        codeScanner.decodeCallback = DecodeCallback { result ->
            activity.runOnUiThread {
                binding.textScanResult.text = result.text
                binding.scannerView.visibility = View.GONE
                binding.layoutScanResult.visibility = View.VISIBLE

                // URL ise butonun görünürlüğünü ayarla
                val isUrl = result.text.startsWith("http://") || result.text.startsWith("https://")
                binding.buttonOpenUrl.visibility = if (isUrl) View.VISIBLE else View.GONE
            }
        }

        codeScanner.errorCallback = ErrorCallback { error ->
            activity.runOnUiThread {
                Toast.makeText(
                    activity,
                    "Tarama hatası: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::codeScanner.isInitialized) {
            codeScanner.startPreview()
        }
    }

    override fun onPause() {
        if (::codeScanner.isInitialized) {
            codeScanner.releaseResources()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}