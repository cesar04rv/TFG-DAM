package com.cesar.tfg

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class QrCartaFragment : Fragment() {

    private lateinit var qrImage: ImageView
    private lateinit var btnVolver: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_qr_carta, container, false)

        // Bind views
        qrImage = view.findViewById(R.id.qrImage)
        btnVolver = view.findViewById(R.id.btnVolver)

        // Generate and display QR code for the online menu
        val url = "https://tfgdam-407ab.web.app"
        qrImage.setImageBitmap(generarQR(url))

        // Go back to previous screen
        btnVolver.setOnClickListener {
            findNavController().navigateUp()
        }

        return view
    }

    // Generates a QR code bitmap for the given URL or text
    private fun generarQR(text: String): Bitmap {
        val size = 1024
        val bits = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(
                    x, y,
                    if (bits[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        return bmp
    }
}
