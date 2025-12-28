package com.powersense.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.powersense.R

object FeedbackUtils {

    // SoundPool holds the sounds in memory for instant playback
    private var soundPool: SoundPool? = null
    private var soundOnId: Int = 0
    private var soundOffId: Int = 0
    private var isLoaded = false

    // Initialize the sound engine lazily
    private fun initSoundPool(context: Context) {
        if (soundPool == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(2) // Allow overlapping sounds (e.g., fast tapping)
                .setAudioAttributes(audioAttributes)
                .build()

            // Pre-load the sounds
            // Ensure 'switch_on' and 'switch_off' exist in res/raw
            soundOnId = soundPool?.load(context, R.raw.switch_on, 1) ?: 0
            soundOffId = soundPool?.load(context, R.raw.switch_off, 1) ?: 0

            isLoaded = true
        }
    }

    fun triggerFeedback(context: Context, isTurningOn: Boolean, hapticsEnabled: Boolean) {
        // 1. PLAY SOUND (Instant)
        try {
            // Ensure engine is ready
            initSoundPool(context)

            if (isLoaded) {
                val soundId = if (isTurningOn) soundOnId else soundOffId
                // play(soundID, leftVol, rightVol, priority, loop, rate)
                soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. VIBRATE (Only if enabled)
        if (hapticsEnabled) {
            try {
                playVibration(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playVibration(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // FIX: Use the System "Click" effect (Guaranteed to be felt)
            try {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } catch (e: Exception) {
                // Fallback if predefined effect fails
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // FIX: Increase duration to 50ms for older devices
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            // Legacy fallback
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    // Optional: Call this to free memory when app closes (though for a singleton object it usually persists)
    fun release() {
        soundPool?.release()
        soundPool = null
        isLoaded = false
    }
}