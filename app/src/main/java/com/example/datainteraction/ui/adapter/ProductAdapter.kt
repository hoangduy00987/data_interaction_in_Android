package com.example.datainteraction.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.datainteraction.R
import com.example.datainteraction.data.models.Product
import com.bumptech.glide.Glide
class ProductAdapter(private var productList:  MutableList<Product>) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProduct: ImageView = itemView.findViewById(R.id.imgProduct)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)

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
    }

    fun setData(newList: List<Product>) {
        productList.clear()
        productList.addAll(newList)
        notifyDataSetChanged()
    }
}
