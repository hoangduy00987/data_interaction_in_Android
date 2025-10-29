package com.example.datainteraction.data.models

import java.io.Serializable

data class Product (
    val id : Int,
    val name: String,
    val price: Double,
    val description:String,
    val image: String?
): Serializable
