package com.capstone.peopleconnect.SPrvoider

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.SPrvoider.Fragments.ActivityFragmentSProvider
import com.capstone.peopleconnect.SPrvoider.Fragments.HomeFragmentSProvider
import com.capstone.peopleconnect.SPrvoider.Fragments.MicFragmentSProvider
import com.capstone.peopleconnect.SPrvoider.Fragments.ProfileFragmentSProvider
import com.capstone.peopleconnect.SPrvoider.Fragments.SkillsFragmentSProvider
import com.google.android.material.bottomnavigation.BottomNavigationView

class SProviderMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sprovider_main)
        // Reference to BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Set the item selected listener
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    loadFragment(HomeFragmentSProvider())
                    true
                }
                R.id.activities -> {
                    loadFragment(ActivityFragmentSProvider())
                    true
                }
                R.id.mic -> {
                    loadFragment(MicFragmentSProvider())
                    true
                }
                R.id.skills -> {
                    loadFragment(SkillsFragmentSProvider())
                    true
                }
                R.id.profile -> {
                    loadFragment(ProfileFragmentSProvider())
                    true
                }
                else -> false
            }
        }
    }
    private fun loadFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, fragment)
            .commit()
    }
}