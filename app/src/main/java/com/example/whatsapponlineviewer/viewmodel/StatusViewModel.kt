package com.example.whatsapponlineviewer.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.whatsapponlineviewer.R
import com.example.whatsapponlineviewer.util.DateUtils
import com.example.whatsapponlineviewer.payment.PaymentRepository
import java.util.*
import kotlin.random.Random

data class StatusResult(val status: String, val lastSeen: String)

class StatusViewModel(application: Application) : AndroidViewModel(application) {

    private val _statusResult = MutableLiveData<StatusResult>()
    val statusResult: LiveData<StatusResult> = _statusResult

    // Deprecated daily checks; now enforce tracked-number limits
    private val _remainingChecks = MutableLiveData<Int>()
    val remainingChecks: LiveData<Int> = _remainingChecks

    private val _isPremium = MutableLiveData<Boolean>()
    val isPremium: LiveData<Boolean> = _isPremium

    private val _trackedNumbers = MutableLiveData<Set<String>>()
    val trackedNumbers: LiveData<Set<String>> = _trackedNumbers

    private val _userPhoneNumber = MutableLiveData<String?>()
    val userPhoneNumber: LiveData<String?> = _userPhoneNumber

    private val _premiumUpsellShown = MutableLiveData<Boolean>()
    val premiumUpsellShown: LiveData<Boolean> = _premiumUpsellShown

    private val sharedPrefs = application.getSharedPreferences("whatsapp_tracker", Context.MODE_PRIVATE)
    private val dateUtils = DateUtils()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        val isPremiumValue = sharedPrefs.getBoolean("is_premium", false)
        _isPremium.value = isPremiumValue

        _userPhoneNumber.value = sharedPrefs.getString("user_phone_number", null)
        val tracked = sharedPrefs.getStringSet("tracked_numbers", emptySet()) ?: emptySet()
        _trackedNumbers.value = tracked
        _premiumUpsellShown.value = sharedPrefs.getBoolean("premium_upsell_shown", false)

        val lastResetDate = sharedPrefs.getLong("last_reset_date", 0)
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Reset counter if it's a new day
        // Keep a legacy counter visible but set to 1 for free tier
        if (lastResetDate < today) {
            sharedPrefs.edit()
                .putInt("remaining_checks", 1)
                .putLong("last_reset_date", today)
                .apply()
        }
        _remainingChecks.value = sharedPrefs.getInt("remaining_checks", 1)
    }

    fun checkStatus(phoneNumber: String) {
        // Generate random status (online/offline)
        val isOnline = Random.nextBoolean()
        val status = if (isOnline) {
            getApplication<Application>().getString(R.string.online)
        } else {
            getApplication<Application>().getString(R.string.offline)
        }

        // Generate random last seen time within the last 24 hours
        val lastSeen = dateUtils.getRandomLastSeen()

        _statusResult.value = StatusResult(status, lastSeen)
        
        // PREMIUM-ONLY: No need to track remaining checks
    }

    fun canCheckStatus(): Boolean {
        return _isPremium.value == true
    }

    fun isOwnNumber(phoneNumber: String): Boolean {
        return _userPhoneNumber.value != null && _userPhoneNumber.value == phoneNumber
    }

    // PREMIUM-ONLY: No tracking limits needed

    fun setPremium() {
        _isPremium.value = true
        sharedPrefs.edit().putBoolean("is_premium", true).apply()
    }

    fun syncPremiumFromBackend() {
        val phone = _userPhoneNumber.value ?: return
        val ctx = getApplication<Application>()
        val repo = PaymentRepository(ctx)
        viewModelScope.launch {
            val result = repo.getPremiumStatus(phone)
            result.fold(
                onSuccess = { isPremiumRemote ->
                    if (isPremiumRemote) setPremium()
                },
                onFailure = { /* ignore errors, stay local */ }
            )
        }
    }
}