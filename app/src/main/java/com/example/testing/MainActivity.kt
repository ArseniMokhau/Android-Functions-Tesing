package com.example.testing

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var selectedContactNameTextView: TextView
    private lateinit var selectedContactNumberTextView: TextView

    companion object {
        const val PICK_CONTACT_REQUEST = 1
        const val PREFS_KEY_SELECTED_CONTACT_NAME = "selected_contact_name"
        const val PREFS_KEY_SELECTED_CONTACT_NUMBER = "selected_contact_number"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getPreferences(MODE_PRIVATE)
        selectedContactNameTextView = findViewById(R.id.selectedContactNameTextView)
        selectedContactNumberTextView = findViewById(R.id.selectedContactNumberTextView)

        val selectContactButton: Button = findViewById(R.id.selectContactButton)
        selectContactButton.setOnClickListener { pickContact() }

        // Load the selected contact from SharedPreferences and display it
        loadSelectedContact()
    }

    private fun pickContact() {
        val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST)
    }

    private fun saveSelectedContact(name: String, number: String) {
        // Save the selected contact name and number to SharedPreferences
        sharedPreferences.edit().apply {
            putString(PREFS_KEY_SELECTED_CONTACT_NAME, name)
            putString(PREFS_KEY_SELECTED_CONTACT_NUMBER, number)
            apply()
        }

        // Display the saved contact in the TextViews
        selectedContactNameTextView.text = name
        selectedContactNumberTextView.text = number
    }

    private fun loadSelectedContact() {
        // Load the selected contact name and number from SharedPreferences
        val selectedContactName = sharedPreferences.getString(PREFS_KEY_SELECTED_CONTACT_NAME, "")
        val selectedContactNumber = sharedPreferences.getString(PREFS_KEY_SELECTED_CONTACT_NUMBER, "")

        // Display the selected contact name and number in the TextViews
        selectedContactNameTextView.text = selectedContactName
        selectedContactNumberTextView.text = selectedContactNumber
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_CONTACT_REQUEST && resultCode == Activity.RESULT_OK) {
            handleSelectedContact(data?.data)
        }
    }

    private fun handleSelectedContact(contactUri: android.net.Uri?) {
        val cursor = contentResolver.query(contactUri!!, null, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                // Retrieve the contact name column index
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

                if (nameIndex != -1) {
                    // Retrieve the contact name from the cursor
                    val contactName = it.getString(nameIndex)

                    // Log the retrieved name
                    Log.d("MainActivity", "Contact Name: $contactName")

                    // Check if the contact has a valid _ID
                    val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                    if (idIndex != -1) {
                        // Query phone numbers associated with the contact
                        val contactId = it.getString(idIndex)
                        val phoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " +
                                    ContactsContract.CommonDataKinds.Phone.NUMBER + " IS NOT NULL",
                            arrayOf(contactId),
                            null
                        )

                        phoneCursor?.use { phoneCursor ->
                            if (phoneCursor.moveToFirst()) {
                                // Retrieve the contact phone number column index
                                val numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                                if (numberIndex != -1) {
                                    // Retrieve the contact phone number from the phoneCursor
                                    val contactNumber = phoneCursor.getString(numberIndex)

                                    // Log the retrieved phone number
                                    Log.d("MainActivity", "Contact Number: $contactNumber")

                                    // Save the selected contact to SharedPreferences
                                    saveSelectedContact(contactName, contactNumber)
                                } else {
                                    // Handle the case where the phone number index is not found
                                    Log.d("MainActivity", "Phone number index not found.")
                                }
                            } else {
                                // Handle the case where no phone number is found
                                Log.d("MainActivity", "No phone number found for the contact.")
                            }
                        }
                    } else {
                        // Handle the case where the _ID index is not found
                        Log.d("MainActivity", "_ID index not found.")
                    }
                }
            }
        }
    }
}
