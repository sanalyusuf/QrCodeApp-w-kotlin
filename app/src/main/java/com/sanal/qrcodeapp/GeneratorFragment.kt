package com.sanal.qrcodeapp

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.sanal.qrcodeapp.databinding.FragmentGeneratorBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class GeneratorFragment : Fragment() {

    private var _binding: FragmentGeneratorBinding? = null
    private val binding get() = _binding!!
    private var currentQRBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGeneratorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonGenerate.setOnClickListener {
            val content = binding.editTextContent.text.toString()
            if (content.isNotEmpty()) {
                generateQRCode(content)
            } else {
                Toast.makeText(
                    context,
                    "Lütfen QR koduna dönüştürülecek içeriği girin",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.buttonSave.setOnClickListener {
            currentQRBitmap?.let { bitmap ->
                saveQRCodeToGallery(bitmap)
            } ?: run {
                Toast.makeText(
                    context,
                    "Önce bir QR kodu oluşturun",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.buttonShare.setOnClickListener {
            currentQRBitmap?.let { bitmap ->
                shareQRCode(bitmap)
            } ?: run {
                Toast.makeText(
                    context,
                    "Önce bir QR kodu oluşturun",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun generateQRCode(content: String) {
        try {
            val multiFormatWriter = MultiFormatWriter()
            val bitMatrix: BitMatrix = multiFormatWriter.encode(
                content,
                BarcodeFormat.QR_CODE,
                800,
                800
            )

            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)

            binding.imageViewQrCode.setImageBitmap(bitmap)
            binding.layoutQrResult.visibility = View.VISIBLE
            currentQRBitmap = bitmap

        } catch (e: Exception) {
            Toast.makeText(
                context,
                "QR kod oluşturulurken bir hata oluştu: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun saveQRCodeToGallery(bitmap: Bitmap) {
        val filename = "QRCode_${System.currentTimeMillis()}.jpg"
        var outputStream: OutputStream? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 ve üzeri için
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                val uri = requireContext().contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                uri?.let {
                    outputStream = requireContext().contentResolver.openOutputStream(it)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream!!)
                    Toast.makeText(context, "QR kod galeriye kaydedildi", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Android 9 ve altı için
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val image = File(imagesDir, filename)
                outputStream = FileOutputStream(image)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                Toast.makeText(context, "QR kod galeriye kaydedildi", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "QR kod kaydedilemedi: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        } finally {
            outputStream?.close()
        }
    }

    private fun shareQRCode(bitmap: Bitmap) {
        try {
            val share = Intent(Intent.ACTION_SEND)
            share.type = "image/jpeg"

            val imagesDir = requireContext().cacheDir
            val image = File(imagesDir, "qr_code_share.jpg")
            val outputStream = FileOutputStream(image)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            share.putExtra(
                Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().packageName + ".fileprovider",
                    image
                )
            )


            startActivity(Intent.createChooser(share, "QR Kodu Paylaş"))
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "QR kod paylaşılamadı: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}