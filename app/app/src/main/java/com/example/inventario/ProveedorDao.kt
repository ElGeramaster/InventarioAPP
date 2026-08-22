package com.example.inventario

import androidx.room.*

@Dao
interface ProveedorDao {

    @Insert
    fun insertar(proveedor: Proveedor): Long

    @Update
    fun actualizar(proveedor: Proveedor)

    @Delete
    fun eliminar(proveedor: Proveedor)

    @Query("SELECT * FROM proveedores ORDER BY nombre ASC")
    fun obtenerTodos(): List<Proveedor>

    @Query(
        "SELECT * FROM proveedores WHERE nombre LIKE '%' || :busqueda || '%' " +
            "OR producto LIKE '%' || :busqueda || '%' ORDER BY nombre ASC"
    )
    fun buscar(busqueda: String): List<Proveedor>

    @Query("SELECT * FROM proveedores WHERE id = :id")
    fun obtenerPorId(id: Int): Proveedor?
}
