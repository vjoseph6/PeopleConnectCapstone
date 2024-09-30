package com.capstone.peopleconnect.SPrvoider

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Adapters.CategoriesAdapter
import com.capstone.peopleconnect.Adapters.InterestAdapter
import com.capstone.peopleconnect.Classes.Category
import com.capstone.peopleconnect.Classes.Interest
import com.capstone.peopleconnect.Classes.SkillItem
import com.capstone.peopleconnect.Classes.Skills
import com.capstone.peopleconnect.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SP7ChooseSkillSProvider : AppCompatActivity() {

    private lateinit var interestsAdapter: InterestAdapter
    private lateinit var categoriesAdapter: CategoriesAdapter
    private lateinit var database: DatabaseReference
    private lateinit var email: String
    private lateinit var firstName: String
    private lateinit var userName: String
    private lateinit var middleName: String
    private lateinit var lastName: String
    private lateinit var address: String
    private lateinit var profileImage: String
    private val selectedInterests = mutableListOf<String>()
    private val allInterests = mutableListOf<Interest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sp7_choose_skill_sprovider)
// Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().reference.child("category")

        // Retrieve email and other data from intent
        email = intent.getStringExtra("EMAIL") ?: ""

        firstName = intent.getStringExtra("FIRST_NAME") ?: ""
        userName = intent.getStringExtra("USER_NAME") ?: ""
        middleName = intent.getStringExtra("MIDDLE_NAME") ?: ""
        lastName = intent.getStringExtra("LAST_NAME") ?: ""
        address = intent.getStringExtra("USER_ADDRESS") ?: ""
        profileImage = intent.getStringExtra("PROFILE_IMAGE_URL") ?: ""

        // Set welcome message

        // Initialize RecyclerView for categories
        findViewById<RecyclerView>(R.id.rvPopularServices).apply {
            layoutManager = LinearLayoutManager(this@SP7ChooseSkillSProvider, LinearLayoutManager.HORIZONTAL, false)
            categoriesAdapter = CategoriesAdapter(emptyList()) { category ->
                // Filter interests based on the selected category
                filterInterestsByCategory(category)
            }
            adapter = categoriesAdapter
        }

        // Initialize RecyclerView for interests
        findViewById<RecyclerView>(R.id.rvInterests).apply {
            layoutManager = LinearLayoutManager(this@SP7ChooseSkillSProvider)
            interestsAdapter = InterestAdapter(mutableListOf()) { selectedInterest ->
                // Handle interest selection
                updateSelectedInterests(selectedInterest)
            }
            adapter = interestsAdapter
        }

        // Fetch categories and interests from Firebase
        fetchCategoriesAndInterests()

        // Setup save button
        findViewById<Button>(R.id.nextButton).setOnClickListener {
            saveSelectedInterests(email)
        }
    }

    private fun fetchCategoriesAndInterests() {
        val categoriesRef = FirebaseDatabase.getInstance().reference.child("category")

        categoriesRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val categories = mutableListOf<Category>()

                for (categorySnapshot in snapshot.children) {
                    val name = categorySnapshot.key ?: continue
                    val image = categorySnapshot.child("image").getValue(String::class.java) ?: ""

                    val interests = mutableListOf<Interest>()
                    for (interestSnapshot in categorySnapshot.child("Sub Categories").children) {
                        val interestName = interestSnapshot.child("name").getValue(String::class.java) ?: continue
                        val interestImage = interestSnapshot.child("image").getValue(String::class.java) ?: ""
                        val interest = Interest(name = interestName, image = interestImage)
                        interests.add(interest)
                        allInterests.add(interest)
                    }

                    categories.add(Category(image = image, name = name, interests = interests))
                }

                setupCategoriesRecyclerView(categories)
                setupInterestsRecyclerView(allInterests)
            } else {
                Toast.makeText(this, "No categories found in the database.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch categories from the database.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCategoriesRecyclerView(categories: List<Category>) {
        categoriesAdapter.updateData(categories)
    }

    private fun setupInterestsRecyclerView(interests: List<Interest>) {
        interestsAdapter.updateData(interests)
    }

    private fun filterInterestsByCategory(category: Category) {
        if (category.name == "All") {
            // Display all interests if "All" is selected or no category selected yet
            setupInterestsRecyclerView(allInterests)
        } else {
            // Filter interests based on the selected category
            val filteredInterests = category.interests
            setupInterestsRecyclerView(filteredInterests)
        }
    }

    private fun updateSelectedInterests(selectedInterest: Interest) {
        if (selectedInterest.isSelected) {
            if (!selectedInterests.contains(selectedInterest.name)) {
                selectedInterests.add(selectedInterest.name)
            }
        } else {
            selectedInterests.remove(selectedInterest.name)
        }
    }

    private fun saveSelectedInterests(email: String) {
        val databaseReference = FirebaseDatabase.getInstance().reference.child("skills")

        // Check if the user exists based on the email
        databaseReference.orderByChild("user").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // If the user already exists, update their skills list
                        for (userSnapshot in snapshot.children) {
                            // Get the selected interests and create a list of SkillItem objects
                            val selectedInterests = interestsAdapter.getSelectedInterests()

                            // Create a list of SkillItem objects with visible set to true for each interest
                            val skillItems = selectedInterests.map { interestName ->
                                SkillItem(name = interestName, visible = true, rating = 0.0f)
                            }

                            // Create a Skills object with the list of skill items and the user's email
                            val skills = Skills(skillItems = skillItems, user = email)

                            // Save the Skills object under the 'skills' node
                            userSnapshot.ref.setValue(skills)
                                .addOnSuccessListener {
                                    // Navigate to the next screen
                                    val intent = Intent(this@SP7ChooseSkillSProvider, SProviderMainActivity::class.java).apply {
                                        putExtra("USER_NAME", userName)
                                        putExtra("FIRST_NAME", firstName)
                                        putExtra("MIDDLE_NAME", middleName)
                                        putExtra("LAST_NAME", lastName)
                                        putExtra("USER_ADDRESS", address)
                                        putExtra("EMAIL", email)
                                        putExtra("PROFILE_IMAGE_URL", profileImage)
                                    }
                                    startActivity(intent)
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this@SP7ChooseSkillSProvider, "Failed to update skills.", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        // If the user doesn't exist, save a new entry with the list of skill items
                        val selectedInterests = interestsAdapter.getSelectedInterests()

                        // Create a list of SkillItem objects with visible set to true for each interest
                        val skillItems = selectedInterests.map { interestName ->
                            SkillItem(name = interestName, visible = true, rating = 0.0f)
                        }

                        // Create a Skills object with the list of skill items and the user's email
                        val skills = Skills(skillItems = skillItems, user = email)

                        // Push the Skills object to the 'skills' node in Firebase
                        databaseReference.push().setValue(skills)
                            .addOnSuccessListener {
                                val intent = Intent(this@SP7ChooseSkillSProvider, SProviderMainActivity::class.java).apply {
                                    putExtra("USER_NAME", userName)
                                    putExtra("FIRST_NAME", firstName)
                                    putExtra("MIDDLE_NAME", middleName)
                                    putExtra("LAST_NAME", lastName)
                                    putExtra("USER_ADDRESS", address)
                                    putExtra("EMAIL", email)
                                    putExtra("PROFILE_IMAGE_URL", profileImage)
                                }
                                startActivity(intent)
                            }
                            .addOnFailureListener {
                                Toast.makeText(this@SP7ChooseSkillSProvider, "Failed to save skills.", Toast.LENGTH_SHORT).show()
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SP7ChooseSkillSProvider, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }



}