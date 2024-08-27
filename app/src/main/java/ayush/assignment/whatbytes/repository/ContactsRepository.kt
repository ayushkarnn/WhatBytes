package ayush.assignment.whatbytes.repository

import ayush.assignment.whatbytes.api.RetrofitInstance
import ayush.assignment.whatbytes.datamodels.ApiResponse

class ContactsRepository {
    suspend fun fetchContacts(): ApiResponse {
        return RetrofitInstance.api.fetchContacts()
    }
}
