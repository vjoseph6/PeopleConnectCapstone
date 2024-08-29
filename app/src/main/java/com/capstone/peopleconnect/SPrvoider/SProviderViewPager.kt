package com.capstone.peopleconnect.SPrvoider

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import com.capstone.peopleconnect.Adapters.ViewPagerAdapter
import com.capstone.peopleconnect.Client.C5LoginClient
import com.capstone.peopleconnect.R

class SProviderViewPager : AppCompatActivity() {

    private lateinit var viewPager: ViewPager
    private lateinit var imageView: ImageView
    private lateinit var button: Button
    private lateinit var skipButton: TextView

    private val pageImages = listOf(
        R.drawable.radiobutton1_sprovider_,
        R.drawable.radiobutton2_sprovider_,
        R.drawable.radiobutton3_sprovider_
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sprovider_view_pager)

        viewPager = findViewById(R.id.viewPager)
        imageView = findViewById(R.id.thisImage)
        button = findViewById(R.id.getstarted1_button_client)
        skipButton = findViewById(R.id.skip)

        // Set up your ViewPager adapter
        val layouts = listOf(
            R.layout.activity_sp2_getstarted1_sprovider,
            R.layout.activity_sp3_getstarted2_sprovider,
            R.layout.activity_sp4_getstarted3_sprovider
        )
        val adapter = ViewPagerAdapter(this, layouts)
        viewPager.adapter = adapter

        // Set up PageChangeListener
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                imageView.setImageResource(pageImages[position])
                button.text = if (position == 2) "Get Started" else "Next"
            }
        })

        // Handle button clicks
        button.setOnClickListener {
            val currentPage = viewPager.currentItem
            if (currentPage == 2) {
                val intent = Intent(this, SP5LoginSProvider::class.java)
                startActivity(intent)
                finish()
            } else {
                viewPager.setCurrentItem(currentPage + 1, true)
            }
        }

        // Handle skip button clicks
        skipButton.setOnClickListener {
            val intent = Intent(this@SProviderViewPager, SP5LoginSProvider::class.java)
            startActivity(intent)
            finish()
        }
    }
}