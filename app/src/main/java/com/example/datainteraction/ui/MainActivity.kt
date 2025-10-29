package com.example.datainteraction.ui // Thay thế bằng package của bạn

import android.content.Intent
import android.util.Log
import android.widget.Button
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import com.example.datainteraction.R
import com.example.datainteraction.ui.adapter.ProductAdapter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.datainteraction.data.api.RetrofitClient
import com.example.datainteraction.data.models.Product
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() { // Giả sử đây là Activity sau khi đăng nhập
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter
    private lateinit var productList: MutableList<Product>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Sử dụng layout bạn vừa tạo ở trên
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        productList = mutableListOf()
        adapter = ProductAdapter(productList) { product ->
            val intent = Intent(this, AddProductActivity::class.java)
            intent.putExtra("product_id", product.id)
            intent.putExtra("product_name", product.name)
            intent.putExtra("product_price", product.price)
            intent.putExtra("product_description", product.description)
            intent.putExtra("product_image", product.image)
            addProductLauncher.launch(intent) // dùng launcher để nhận result
        }
        recyclerView.adapter = adapter


        loadProducts()
        val signOutButton = findViewById<Button>(R.id.logout_button)

        // Gắn sự kiện click cho nút
        signOutButton.setOnClickListener {
            signOutCompletely()
        }
       val add_button = findViewById<Button>(R.id.add_button)
        add_button.setOnClickListener {
            val intent = Intent(this, AddProductActivity::class.java)
            addProductLauncher.launch(intent)
        }
    }
    private val addProductLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val updatedProduct = result.data?.getSerializableExtra("updated_product") as? Product
                updatedProduct?.let { product ->
                    val index = productList.indexOfFirst { it.id == product.id }
                    if (index != -1) {
                        productList[index] = product
                        adapter.notifyItemChanged(index) // cập nhật item
                    } else {
                        productList.add(product)
                        adapter.notifyItemInserted(productList.size - 1)
                    }
                } ?: loadProducts() // fallback: load lại toàn bộ nếu null
            }
        }

    private fun loadProducts() {
        RetrofitClient.instance.getProducts().enqueue(object : Callback<List<Product>> {
            override fun onResponse(
                call: Call<List<Product>>,
                response: Response<List<Product>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    Log.d("API_SUCCESS", "Response: ${response.body()}")
                    adapter.setData(response.body()!!)
                } else {
                    // Khi server trả về lỗi (ví dụ 404, 500)
                    Log.e("API_ERROR", "Response code: ${response.code()}")
                    Log.e("API_ERROR", "Error body: ${response.errorBody()?.string()}")
                    Toast.makeText(this@MainActivity, "Server error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Product>>, t: Throwable) {
                Log.e("API_FAILURE", "Network error: ${t.message}", t)
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    /**
     * Hàm đăng xuất toàn diện:
     * 1. Đăng xuất khỏi Firebase.
     * 2. Xóa trạng thái đăng nhập của Google để không tự động gợi ý tài khoản cũ.
     * 3. Chuyển về màn hình đăng nhập.
     */
    private fun signOutCompletely() {
        // Bước 1: Đăng xuất khỏi Firebase
        FirebaseAuth.getInstance().signOut()

        // Bước 2: Yêu cầu Credential Manager xóa trạng thái đăng nhập
        lifecycleScope.launch {
            try {
                val credentialManager = CredentialManager.create(this@MainActivity)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                Log.d("SignOut", "Credential state cleared successfully.")
            } catch (e: Exception) {
                Log.e("SignOut", "Failed to clear credential state", e)
            }
        }

        // Bước 3: Chuyển người dùng về màn hình đăng nhập (MainActivity)
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        // Cờ này xóa hết các Activity cũ và tạo một task mới,
        // ngăn người dùng bấm nút back để quay lại màn hình Home.
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Đóng HomeActivity lại
    }
}