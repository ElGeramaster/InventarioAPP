package com.example.inventario

import androidx.room.*

@Dao
interface FiadoDao {

    @Insert
    fun insertar(fiado: Fiado): Long

    @Update
    fun actualizar(fiado: Fiado)

    @Delete
    fun eliminar(fiado: Fiado)

    @Query("SELECT * FROM fiados ORDER BY timestamp DESC")
    fun obtenerTodos(): List<Fiado>

    @Query(
        "SELECT * FROM fiados WHERE nombre LIKE '%' || :busqueda || '%' " +
            "ORDER BY timestamp DESC"
    )
    fun buscar(busqueda: String): List<Fiado>

    @Query("SELECT COALESCE(SUM(monto), 0) FROM fiados")
    fun obtenerTotalDeuda(): Double
}
