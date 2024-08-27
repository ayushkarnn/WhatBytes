package ayush.assignment.whatbytes.api

import ayush.assignment.whatbytes.datamodels.ApiResponse
import retrofit2.http.GET

interface ApiService {
    @GET("fetch/contacts/all")
    suspend fun fetchContacts(): ApiResponse
}
