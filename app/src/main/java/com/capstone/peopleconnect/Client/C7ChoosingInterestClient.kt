package com.capstone.peopleconnect.Client

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Adapters.CategoriesAdapter
import com.capstone.peopleconnect.Adapters.InterestAdapter
import com.capstone.peopleconnect.Classes.Category
import com.capstone.peopleconnect.Classes.Interest
import com.capstone.peopleconnect.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class C7ChoosingInterestClient : AppCompatActivity() {

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
        setContentView(R.layout.activity_c7_choosing_interest_client)

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
        val name = findViewById<TextView>(R.id.tvWelcome)
        name.text = "Welcome $firstName"

        // Initialize RecyclerView for categories
        findViewById<RecyclerView>(R.id.rvPopularServices).apply {
            layoutManager = LinearLayoutManager(this@C7ChoosingInterestClient, LinearLayoutManager.HORIZONTAL, false)
            categoriesAdapter = CategoriesAdapter(emptyList()) { category ->
                // Filter interests based on the selected category
                filterInterestsByCategory(category)
            }
            adapter = categoriesAdapter
        }

        // Initialize RecyclerView for interests
        findViewById<RecyclerView>(R.id.rvInterests).apply {
            layoutManager = LinearLayoutManager(this@C7ChoosingInterestClient)
            interestsAdapter = InterestAdapter(mutableListOf()) { selectedInterest ->
                // Handle interest selection
                updateSelectedInterests(selectedInterest)
            }
            adapter = interestsAdapter
        }

        // Fetch categories and interests from Firebase
        fetchCategoriesAndInterests()

        // Setup save button
        findViewById<Button>(R.id.btnStartFinding).setOnClickListener {
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
        val databaseReference = FirebaseDatabase.getInstance().reference.child("users")

        databaseReference.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val userId = userSnapshot.key
                            val selectedInterests = interestsAdapter.getSelectedInterests()
                            userSnapshot.ref.child("interest").setValue(selectedInterests)
                                .addOnSuccessListener {
                                    val intent = Intent(this@C7ChoosingInterestClient, ClientMainActivity::class.java).apply {
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
                                    Toast.makeText(this@C7ChoosingInterestClient, "Failed to update interests.", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this@C7ChoosingInterestClient, "User not found.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@C7ChoosingInterestClient, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
