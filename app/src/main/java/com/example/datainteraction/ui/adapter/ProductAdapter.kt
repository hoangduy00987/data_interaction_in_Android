package com.example.datainteraction.ui.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.datainteraction.R
import com.example.datainteraction.data.models.Product
import com.bumptech.glide.Glide
import com.example.datainteraction.data.api.RetrofitClient
import com.example.datainteraction.ui.AddProductActivity
import retrofit2.Call
import retrofit2.Callback

class ProductAdapter(private var productList:  MutableList<Product>,
                     private val onEditClick: (Product) -> Unit

) :

    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProduct: ImageView = itemView.findViewById(R.id.imgProduct)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val btnUpdate: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int = productList.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        holder.tvName.text = product.name
        holder.tvPrice.text = "${product.price} VND"
        Glide.with(holder.itemView.context)
            .load(product.image)
            .into(holder.imgProduct)
        Log.d("DEBUG_IMAGE_URL", "Image URL: ${product.image}")


        holder.btnDelete.setOnClickListener {
            // Lấy lại vị trí hiện tại của item
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val productId = productList[currentPosition].id

                val apiService = RetrofitClient.instance.deleteProduct(productId)
                apiService.enqueue(object : retrofit2.Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: retrofit2.Response<Void>) {
                        if (response.isSuccessful) {
                            Toast.makeText(
                                holder.itemView.context,
                                "Deleted successfully!",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Cập nhật danh sách hiển thị
                            productList.removeAt(currentPosition)
                            notifyItemRemoved(currentPosition)
                        } else {
                            Toast.makeText(
                                holder.itemView.context,
                                "Delete failed!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Toast.makeText(
                            holder.itemView.context,
                            "Error: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
        }
        holder.btnUpdate.setOnClickListener {
            onEditClick(product)
        }
    }
    fun setData(newList: List<Product>) {
        productList.clear()
        productList.addAll(newList)
        notifyDataSetChanged()
    }


}
