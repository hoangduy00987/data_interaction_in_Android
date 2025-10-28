package com.example.datainteraction.data.api
import com.example.datainteraction.data.models.Product
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface APIService {
    @GET("products")
    fun getProducts(): Call<List<Product>>

    @Multipart
    @POST("products")
    fun createProduct(
        @Part("name") name: RequestBody,
        @Part("price") price: RequestBody,
        @Part("description") description: RequestBody,
        @Part image: MultipartBody.Part
    ): Call<Product>
}