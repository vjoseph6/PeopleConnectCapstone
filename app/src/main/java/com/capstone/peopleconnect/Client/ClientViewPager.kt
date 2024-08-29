package com.capstone.peopleconnect.Client

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.capstone.peopleconnect.Adapters.ViewPagerAdapter
import com.capstone.peopleconnect.R

class ClientViewPager : AppCompatActivity() {

    private lateinit var viewPager: ViewPager
    private lateinit var imageView: ImageView
    private lateinit var button: Button
    private lateinit var skipButton: TextView

    private val pageImages = listOf(
        R.drawable.radiobutton1_client_,
        R.drawable.radiobutton2_client_,
        R.drawable.radiobutton3_client_
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_view_pager)

        viewPager = findViewById(R.id.viewPager)
        imageView = findViewById(R.id.thisImage)
        button = findViewById(R.id.getstarted1_button_client)
        skipButton = findViewById(R.id.skip)

        // Set up your ViewPager adapter
        val layouts = listOf(
            R.layout.activity_c2_getstarted1_client,
            R.layout.activity_c3_getstarted2_client,
            R.layout.activity_c4_getstarted3_client
        )
        val adapter = ViewPagerAdapter(this, layouts)
        viewPager.adapter = adapter

        // Set up PageChangeListener
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                Log.d("ClientViewPager", "Page changed to: $position")
                imageView.setImageResource(pageImages[position])
                button.text = if (position == 2) "Get Started" else "Next"
            }
        })

        // Handle button clicks
        button.setOnClickListener {
            Log.d("ClientViewPager", "Button clicked: ${button.text}")
            val currentPage = viewPager.currentItem
            if (currentPage == 2) {
                val intent = Intent(this, C5LoginClient::class.java)
                startActivity(intent)
                finish()
            } else {
                viewPager.setCurrentItem(currentPage + 1, true)
            }
        }

        // Handle skip button clicks
        skipButton.setOnClickListener {
            val intent = Intent(this@ClientViewPager, C5LoginClient::class.java)
            startActivity(intent)
            finish()
        }
    }
}

