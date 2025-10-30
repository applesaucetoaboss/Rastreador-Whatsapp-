package com.example.whatsapponlineviewer.payment

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.whatsapponlineviewer.PaymentActivity
import com.example.whatsapponlineviewer.R
import java.util.UUID

class SpeiPaymentFragment : Fragment(R.layout.fragment_spei_payment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reference = UUID.randomUUID().toString().substring(0, 8).uppercase()
        view.findViewById<TextView>(R.id.tvSpeiReference).text =
            getString(R.string.spei_reference, reference)

        view.findViewById<Button>(R.id.btnConfirmSpei).setOnClickListener {
            (activity as? PaymentActivity)?.onPaymentSuccess()
        }
    }
}