package com.example.datainteraction.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import com.bumptech.glide.Glide
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

        // Lấy dữ liệu intent (nếu có)
        val productId = intent.getIntExtra("product_id", -1)
        val productName = intent.getStringExtra("product_name")
        val productPrice = intent.getDoubleExtra("product_price", 0.0)
        val productDescription = intent.getStringExtra("product_description")
        val productImage = intent.getStringExtra("product_image") // URL

        if (productId != -1) {
            // Edit mode: điền sẵn dữ liệu vào form
            editName.setText(productName)
            editPrice.setText(productPrice.toString())
            editDescription.setText(productDescription)
            if (!productImage.isNullOrEmpty()) {
                imageView.visibility = View.VISIBLE
                Glide.with(this)
                    .load(productImage)
                    .into(imageView) // <--- quan trọng: phải .into(imageView)
            }
            btnSave.text = "Update"
        } else {
            btnSave.text = "Add"
        }

        btnSave.setOnClickListener {
            val name = editName.text.toString().trim()
            val priceText = editPrice.text.toString().trim()
            val description = editDescription.text.toString().trim()
            val price = priceText.toDoubleOrNull() ?: 0.0

            // VALIDATION cơ bản
            if (name.isEmpty() || priceText.isEmpty()) {
                Toast.makeText(this, "Please fill name and price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val namePart = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val pricePart = price.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val descPart = description.toRequestBody("text/plain".toMediaTypeOrNull())

            if (productId != -1) {
                val imageFile = when {
                    selectedImageUri != null -> uriToFile(selectedImageUri!!, this) // User chọn ảnh mới
                    imageView.drawable != null -> imageViewToFile(imageView, this)!! // Không chọn ảnh mới -> lấy ảnh hiện tại
                    else -> null // Không có ảnh
                }

                val imagePart = imageFile?.let {
                    MultipartBody.Part.createFormData(
                        "image", it.name, it.asRequestBody("image/*".toMediaTypeOrNull())
                    )
                }

                RetrofitClient.instance.updateProduct(
                    productId,
                    namePart,
                    pricePart,
                    descPart,
                    imagePart
                ).enqueue(object : Callback<Product> {
                    override fun onResponse(call: Call<Product>, response: Response<Product>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@AddProductActivity, "Product Updated successfully!", Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK)
                            finish()
                        } else {
                            Toast.makeText(this@AddProductActivity, "Update failed! code:${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Product>, t: Throwable) {
                        Toast.makeText(this@AddProductActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })

            } else {
                // ===== CREATE ===== (cần ảnh mới)
                if (selectedImageUri == null) {
                    Toast.makeText(this, "Please choose an image to create product", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val file = uriToFile(selectedImageUri!!, this)
                val imagePart = MultipartBody.Part.createFormData(
                    "image", file.name, file.asRequestBody("image/*".toMediaTypeOrNull())
                )

                RetrofitClient.instance.createProduct(
                    namePart,
                    pricePart,
                    descPart,
                    imagePart
                ).enqueue(object : Callback<Product> {
                    override fun onResponse(call: Call<Product>, response: Response<Product>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@AddProductActivity, "Product added successfully!", Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK)
                            finish()
                        } else {
                            Toast.makeText(this@AddProductActivity, "Failed to add product! code:${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Product>, t: Throwable) {
                        Toast.makeText(this@AddProductActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }


    }

    private fun imageViewToFile(imageView: ImageView, context: Context): File {
        val bitmap = (imageView.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            ?: throw Exception("No image in ImageView")
        val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()
        return file
    }

}
