package com.example.inventario

import androidx.room.*

@Dao
interface ProductoDao {

    @Insert
    fun insertar(producto: Producto)

    @Update
    fun actualizar(producto: Producto)

    @Delete
    fun eliminar(producto: Producto)

    @Query("SELECT * FROM productos ORDER BY nombre ASC")
    fun obtenerTodos(): List<Producto>

    @Query("SELECT * FROM productos WHERE nombre LIKE '%' || :busqueda || '%' OR categoria LIKE '%' || :busqueda || '%'")
    fun buscar(busqueda: String): List<Producto>

    @Query("SELECT * FROM productos WHERE cantidad <= stockMinimo")
    fun obtenerStockBajo(): List<Producto>

    @Query("SELECT * FROM productos WHERE id = :id")
    fun obtenerPorId(id: Int): Producto
}