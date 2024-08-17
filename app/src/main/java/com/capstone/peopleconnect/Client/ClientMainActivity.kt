package com.capstone.peopleconnect.Client

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.capstone.peopleconnect.Client.Fragments.ActivityFragmentClient
import com.capstone.peopleconnect.Client.Fragments.CategoryFragmentClient
import com.capstone.peopleconnect.Client.Fragments.HomeFragmentClient
import com.capstone.peopleconnect.Client.Fragments.MicFragmentClient
import com.capstone.peopleconnect.Client.Fragments.ProfileFragmentClient
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.databinding.ActivityClientMainBinding

class ClientMainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_main)


    }
}