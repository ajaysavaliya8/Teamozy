package com.example.retrofit

object RetrofitInstance {
}

fun main(){
    println(ABC.name)
    ABC.myName()

}

object ABC{
    val name: String = "ABC";

    fun myName(){
        println("My name ajay");
    }
}
