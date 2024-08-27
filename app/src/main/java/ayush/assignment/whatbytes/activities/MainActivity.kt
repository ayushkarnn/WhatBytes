package ayush.assignment.whatbytes.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import ayush.assignment.whatbytes.R
import ayush.assignment.whatbytes.utills.Resource
import ayush.assignment.whatbytes.viewmodel.ContactsViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ContactsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val syncContactBtn = findViewById<Button>(R.id.syncContactBtn)
        val progressTv = findViewById<TextView>(R.id.progressTv)

        // Initialize the ViewModel with content resolver
        viewModel = ContactsViewModel(contentResolver)

        // Observe the LiveData from the ViewModel to update UI based on contact fetch status
        viewModel.contactsLiveData.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    progressTv.text = "Loading contacts..." // Show loading state
                }

                is Resource.Success -> {
                    progressTv.text = "Contacts loaded successfully"
                    resource.data?.let { contacts ->
                        viewModel.addContactsToPhone(contacts) // Add contacts to the phone if successfully fetched
                    }
                }

                is Resource.Error -> {
                    progressTv.text = "Error: ${resource.message}"
                }
            }
        }

        syncContactBtn.setOnClickListener {
            if (checkPermissions()) {
                viewModel.fetchContacts() // Fetch contacts if permissions are granted
            } else {
                requestPermissions() // Request necessary permissions
            }
        }
    }

    // Method to check if the required permissions are granted
    private fun checkPermissions(): Boolean {
        val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
        val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS)
        return readPermission == PackageManager.PERMISSION_GRANTED &&
                writePermission == PackageManager.PERMISSION_GRANTED
    }

    // Register for activity result to handle permission request response
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_CONTACTS] == true &&
            permissions[Manifest.permission.WRITE_CONTACTS] == true) {
            viewModel.fetchContacts() // Fetch contacts if permissions are granted
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Method to request the required permissions
    private fun requestPermissions() {
        requestPermissionLauncher.launch(
            arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
        )
    }
}
