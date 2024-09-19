package com.capstone.peopleconnect.SPrvoider

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.capstone.peopleconnect.Classes.SkillItem
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.SPrvoider.Fragments.SkillsFragmentSProvider

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddSkillsProviderRate : AppCompatActivity() {

    private lateinit var email: String
    private lateinit var skillName: String
    private lateinit var profileImage: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_skills_provider_rate)

        // Retrieve the skill name and email from the intent
        skillName = intent.getStringExtra("SUBCATEGORY_NAME").toString()
        email = intent.getStringExtra("EMAIL").toString()
        profileImage = intent.getStringExtra("PROFILE_IMAGE_URL").toString()
        Log.d("AddSkillProviderRate", email)
        Log.d("AddSkillProviderRate", skillName)

        // Set the skill name to the popularText TextView
        findViewById<TextView>(R.id.popularText).text = skillName

        val saveButton: Button = findViewById(R.id.btnSave)
        saveButton.setOnClickListener {
            saveSkill()
        }

        val backButton: ImageButton = findViewById(R.id.btnBackSProviderSKills)
        backButton.setOnClickListener {
            onBackPressed()
        }

        // Retrieve skill details to populate EditText fields and manage delete button visibility
        retrieveSkillDetails()

        // Set up the delete button with a confirmation dialog
        val deleteButton: ImageButton = findViewById(R.id.deleteSKillBtn)
        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun retrieveSkillDetails() {
        val skillsReference = FirebaseDatabase.getInstance().getReference("skills")
        val deleteButton: ImageButton = findViewById(R.id.deleteSKillBtn)

        // Set delete button to GONE by default
        deleteButton.visibility = View.GONE

        skillsReference.orderByChild("user").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (snapshot in dataSnapshot.children) {
                        val skillItemsSnapshot = snapshot.child("skillItems")
                        for (skillItemSnapshot in skillItemsSnapshot.children) {
                            val skillItem = skillItemSnapshot.getValue(SkillItem::class.java)
                            if (skillItem?.name == skillName) {
                                // Populate EditText fields with skill details
                                findViewById<EditText>(R.id.rateEditText).setText(skillItem.skillRate.toString())
                                findViewById<EditText>(R.id.expEditText).setText(skillItem.description)
                                // Set delete button to VISIBLE
                                deleteButton.visibility = View.VISIBLE
                                return // Exit once the skill is found
                            }
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("AddSkillProviderRate", "Database error: ${databaseError.message}")
            }
        })
    }

    private fun saveSkill() {
        val skillRate = findViewById<EditText>(R.id.rateEditText).text.toString().toIntOrNull() ?: 0
        val skillDescription = findViewById<EditText>(R.id.expEditText).text.toString()

        val skillItem = SkillItem(
            name = skillName,
            visible = true,
            description = skillDescription,
            skillRate = skillRate
        )

        val skillsReference = FirebaseDatabase.getInstance().getReference("skills")

        skillsReference.orderByChild("user").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (snapshot in dataSnapshot.children) {
                        val currentSkillsRef = snapshot.ref.child("skillItems")
                        val existingSkills = mutableListOf<SkillItem>()
                        for (skillSnapshot in snapshot.child("skillItems").children) {
                            val existingSkill = skillSnapshot.getValue(SkillItem::class.java)
                            if (existingSkill != null) {
                                existingSkills.add(existingSkill)
                            }
                        }

                        val existingSkill = existingSkills.find { it.name == skillItem.name }
                        if (existingSkill != null) {
                            existingSkill.description = skillItem.description
                            existingSkill.skillRate = skillItem.skillRate
                            existingSkill.visible = true
                        } else {
                            existingSkills.add(skillItem)
                        }

                        currentSkillsRef.setValue(existingSkills).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val intent = Intent(this@AddSkillsProviderRate, SProviderMainActivity::class.java).apply {
                                    putExtra("FRAGMENT_TO_LOAD", "SkillsFragmentSProvider")
                                    putExtra("EMAIL", email)
                                    putExtra("PROFILE_IMAGE_URL", profileImage)
                                }
                                startActivity(intent)
                                finish() // Optionally finish this activity

                                Toast.makeText(this@AddSkillsProviderRate, "Skill saved successfully!", Toast.LENGTH_SHORT).show()

                            } else {
                                Log.e("AddSkillProviderRate", "Error updating skills: ${task.exception?.message}")
                            }
                        }
                    }
                } else {
                    val newUserSkills = hashMapOf(
                        "user" to email,
                        "skillItems" to listOf(skillItem)
                    )
                    skillsReference.push().setValue(newUserSkills).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Send an intent to SProviderMainActivity to load SkillsFragmentSProvider
                            val intent = Intent(this@AddSkillsProviderRate, SProviderMainActivity::class.java).apply {
                                putExtra("FRAGMENT_TO_LOAD", "SkillsFragmentSProvider")
                                putExtra("EMAIL", email)
                            }
                            startActivity(intent)
                            finish() // Optionally finish this activity

                            Toast.makeText(this@AddSkillsProviderRate, "New skill added successfully!", Toast.LENGTH_SHORT).show()

                        } else {
                            Log.e("AddSkillProviderRate", "Error adding new user: ${task.exception?.message}")
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("AddSkillProviderRate", "Database error: ${databaseError.message}")
            }
        })
    }

    private fun showDeleteConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.delete_skill_dialog, null)
        val builder = AlertDialog.Builder(this).apply {
            setView(dialogView)
            setCancelable(false)
        }

        val dialog = builder.create()

        // Set the animation style
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        // Make the dialog background transparent
        dialog.window?.setBackgroundDrawable(ColorDrawable(0))  // Transparent background

        dialog.show()

        dialogView.findViewById<Button>(R.id.btnDeleteSkillDialog).setOnClickListener {
            deleteSkill()
            dialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.btnCancelDialog).setOnClickListener {
            dialog.dismiss()
        }
    }


    private fun deleteSkill() {
        val skillsReference = FirebaseDatabase.getInstance().getReference("skills")

        skillsReference.orderByChild("user").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (snapshot in dataSnapshot.children) {
                        val currentSkillsRef = snapshot.ref.child("skillItems")
                        for (skillSnapshot in snapshot.child("skillItems").children) {
                            val skillItem = skillSnapshot.getValue(SkillItem::class.java)
                            if (skillItem?.name == skillName) {
                                skillSnapshot.ref.removeValue().addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(this@AddSkillsProviderRate, "Skill deleted successfully!", Toast.LENGTH_SHORT).show()

                                        // Send an intent to SProviderMainActivity to load SkillsFragmentSProvider
                                        val intent = Intent(this@AddSkillsProviderRate, SProviderMainActivity::class.java).apply {
                                            putExtra("FRAGMENT_TO_LOAD", "SkillsFragmentSProvider")
                                            putExtra("EMAIL", email)
                                        }
                                        startActivity(intent)
                                        finish() // Optionally finish this activity
                                    } else {
                                        Log.e("AddSkillProviderRate", "Error deleting skill: ${task.exception?.message}")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("AddSkillProviderRate", "Database error: ${databaseError.message}")
            }
        })
    }
}

