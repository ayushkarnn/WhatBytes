package ayush.assignment.whatbytes.viewmodel

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.provider.ContactsContract
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ayush.assignment.whatbytes.datamodels.ApiResponseItem
import ayush.assignment.whatbytes.repository.ContactsRepository
import ayush.assignment.whatbytes.utills.Resource
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ContactsViewModel(private val contentResolver: ContentResolver) : ViewModel() {

    private val repository = ContactsRepository() // Initialize the repository for fetching contacts

    // LiveData to communicate the status of contacts fetch to the UI
    val contactsLiveData = MutableLiveData<Resource<List<ApiResponseItem>>>()

    // Function to fetch contacts using the repository
    fun fetchContacts() {
        contactsLiveData.postValue(Resource.Loading()) // Indicate loading state
        viewModelScope.launch {
            try {
                val response = repository.fetchContacts() // Fetch contacts from the repository
                val todayDate = getTodayDate() // Get today's date

                // Filter contacts added today
                val filteredContacts = response.filter {
                    convertTimestampToDate(it.date_added) == todayDate
                }
                contactsLiveData.postValue(Resource.Success(filteredContacts)) // Post the filtered contacts as success
            } catch (e: Exception) {
                contactsLiveData.postValue(Resource.Error(e.message ?: "Unknown Error")) // Post an error message if fetch fails
            }
        }
    }

    // Convert Unix timestamp to a date string in 'yyyy-MM-dd' format
    @SuppressLint("SimpleDateFormat")
    private fun convertTimestampToDate(timestamp: Int): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        val date = Date(timestamp.toLong() * 1000)
        return sdf.format(date)
    }

    // Get today's date as a string in 'yyyy-MM-dd' format
    @SuppressLint("SimpleDateFormat")
    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        return sdf.format(Date())
    }

    // Function to add a list of contacts to the phone's contact list
    fun addContactsToPhone(contacts: List<ApiResponseItem>) {
        viewModelScope.launch {
            contacts.forEach { contact ->
                if (!isContactAlreadyExists(contact.phone_number)) {
                    addContact(contact.user_name, extractPhoneNumber(contact.phone_number)) // Add contact if it doesn't already exist
                }
            }
        }
    }

    // Extract only the digits from a phone number and return the last 10 digits
    private fun extractPhoneNumber(phoneNumber: String): String {
        return phoneNumber.filter { it.isDigit() }.takeLast(10)
    }

    // Check if a contact already exists in the phone's contacts
    private fun isContactAlreadyExists(phoneNumber: String): Boolean {
        val phoneNumberTrimmed = extractPhoneNumber(phoneNumber) // Clean the phone number to compare
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
            arrayOf(phoneNumberTrimmed),
            null
        )

        val exists = cursor?.count ?: 0 > 0 // Check if any results are returned
        cursor?.close() // Close cursor to avoid memory leaks
        return exists
    }

    // Add a new contact to the phone's contact list
    private fun addContact(displayName: String, mobileNumber: String) {
        // Insert a new raw contact
        val contactValues = ContentValues().apply {
            put(ContactsContract.RawContacts.ACCOUNT_TYPE, null as String?)
            put(ContactsContract.RawContacts.ACCOUNT_NAME, null as String?)
        }

        val rawContactUri = contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, contactValues)
        val rawContactId = rawContactUri?.lastPathSegment?.toLongOrNull() ?: return // Get the new contact ID

        // Insert the display name
        val nameValues = ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
        }
        contentResolver.insert(ContactsContract.Data.CONTENT_URI, nameValues)

        // Insert the phone number
        val phoneValues = ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            put(ContactsContract.CommonDataKinds.Phone.NUMBER, mobileNumber)
            put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
        }
        contentResolver.insert(ContactsContract.Data.CONTENT_URI, phoneValues)
    }
}
