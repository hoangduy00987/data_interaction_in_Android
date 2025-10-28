package com.example.datainteraction.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.datainteraction.R
import com.example.datainteraction.data.api.RetrofitClient
import com.example.datainteraction.data.models.Product
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class AddProductActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null
    private lateinit var imageView: ImageView

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                imageView.visibility = View.VISIBLE
                imageView.setImageURI(it)
            }
        }

    private fun uriToFile(uri: Uri, context: Context): File {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        inputStream.use { input ->
            FileOutputStream(file).use { output ->
                input?.copyTo(output)
            }
        }
        return file
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_product)

        val editName = findViewById<EditText>(R.id.editName)
        val editPrice = findViewById<EditText>(R.id.editPrice)
        val editDescription = findViewById<EditText>(R.id.editDescription)
        val btnChooseImage = findViewById<Button>(R.id.btnChooseImage)
        val btnSave = findViewById<Button>(R.id.btnSave)
        imageView = findViewById(R.id.imgPreview)

        btnChooseImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        btnSave.setOnClickListener {
            val name = editName.text.toString()
            val priceText = editPrice.text.toString()
            val description = editDescription.text.toString()

            if (name.isNotEmpty() && priceText.isNotEmpty() && selectedImageUri != null) {
                val price = priceText.toDoubleOrNull() ?: 0.0

                val namePart = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val pricePart = price.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val descPart = description.toRequestBody("text/plain".toMediaTypeOrNull())

                val file = uriToFile(selectedImageUri!!, this)
                val imagePart = MultipartBody.Part.createFormData(
                    "image", file.name, file.asRequestBody("image/*".toMediaTypeOrNull())
                )

                RetrofitClient.instance.createProduct(namePart, pricePart, descPart, imagePart)
                    .enqueue(object : Callback<Product> {
                        override fun onResponse(call: Call<Product>, response: Response<Product>) {
                            if (response.isSuccessful) {
                                Toast.makeText(this@AddProductActivity, "Product added successfully!", Toast.LENGTH_SHORT).show()
                                setResult(RESULT_OK)
                                finish()
                            } else {
                                Log.e("API_ERROR", "Response code: ${response.code()}")
                                Toast.makeText(this@AddProductActivity, "Failed to add product!", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<Product>, t: Throwable) {
                            Log.e("API_ERROR", "onFailure: ${t.message}")
                            Toast.makeText(this@AddProductActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            } else {
                Toast.makeText(this, "Please fill all fields and choose image!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
