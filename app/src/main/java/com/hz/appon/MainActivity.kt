package com.hz.appon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hz.appon.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var tapCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        savedInstanceState?.let { tapCount = it.getInt(KEY_TAP_COUNT) }
        updateTapCount()

        binding.btnTap.setOnClickListener {
            tapCount++
            updateTapCount()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_TAP_COUNT, tapCount)
    }

    private fun updateTapCount() {
        binding.textTapCount.text = getString(R.string.tap_count, tapCount)
    }

    companion object {
        private const val KEY_TAP_COUNT = "tap_count"
    }
}
