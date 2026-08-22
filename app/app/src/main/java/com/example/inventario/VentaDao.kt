package com.example.inventario

import androidx.room.*

@Dao
interface VentaDao {

    @Insert
    fun insertarVenta(venta: Venta): Long

    @Insert
    fun insertarDetalles(detalles: List<VentaDetalle>)

    @Query("SELECT * FROM ventas WHERE timestamp >= :desde ORDER BY timestamp DESC")
    fun obtenerVentasDesde(desde: Long): List<Venta>

    @Query("SELECT * FROM ventas ORDER BY timestamp DESC")
    fun obtenerTodas(): List<Venta>

    @Query("SELECT * FROM venta_detalles WHERE ventaId = :ventaId")
    fun obtenerDetallesVenta(ventaId: Int): List<VentaDetalle>

    @Query("SELECT COALESCE(SUM(total), 0) FROM ventas WHERE timestamp >= :desde")
    fun obtenerTotalIngresos(desde: Long): Double

    @Query("SELECT COALESCE(SUM(ganancia), 0) FROM ventas WHERE timestamp >= :desde")
    fun obtenerTotalGanancia(desde: Long): Double

    @Query("SELECT COUNT(*) FROM ventas WHERE timestamp >= :desde")
    fun obtenerConteoVentas(desde: Long): Int

    @Query("SELECT COALESCE(SUM(total), 0) FROM ventas")
    fun obtenerTotalIngresosGlobal(): Double

    @Query("SELECT COALESCE(SUM(ganancia), 0) FROM ventas")
    fun obtenerTotalGananciaGlobal(): Double

    @Query("SELECT COUNT(*) FROM ventas")
    fun obtenerConteoVentasGlobal(): Int

    @Query("""
        SELECT vd.productoNombre, SUM(vd.cantidad) as totalCantidad
        FROM venta_detalles vd
        INNER JOIN ventas v ON vd.ventaId = v.id
        WHERE v.timestamp >= :desde
        GROUP BY vd.productoNombre
        ORDER BY totalCantidad DESC
        LIMIT :limit
    """)
    fun obtenerMasVendidos(desde: Long, limit: Int): List<ProductoVendidoResumen>

    @Query("""
        SELECT vd.productoNombre, SUM(vd.cantidad) as totalCantidad
        FROM venta_detalles vd
        INNER JOIN ventas v ON vd.ventaId = v.id
        WHERE v.timestamp >= :desde
        GROUP BY vd.productoNombre
        ORDER BY totalCantidad ASC
        LIMIT :limit
    """)
    fun obtenerMenosVendidos(desde: Long, limit: Int): List<ProductoVendidoResumen>

    @Query("""
        SELECT vd.productoNombre, SUM(vd.cantidad) as totalCantidad
        FROM venta_detalles vd
        GROUP BY vd.productoNombre
        ORDER BY totalCantidad DESC
        LIMIT :limit
    """)
    fun obtenerMasVendidosGlobal(limit: Int): List<ProductoVendidoResumen>

    @Query("""
        SELECT vd.productoNombre, SUM(vd.cantidad) as totalCantidad
        FROM venta_detalles vd
        GROUP BY vd.productoNombre
        ORDER BY totalCantidad ASC
        LIMIT :limit
    """)
    fun obtenerMenosVendidosGlobal(limit: Int): List<ProductoVendidoResumen>

    @Query("SELECT * FROM ventas WHERE timestamp >= :desde ORDER BY timestamp ASC")
    fun obtenerVentasParaGrafica(desde: Long): List<Venta>

    @Query("SELECT * FROM ventas ORDER BY timestamp ASC")
    fun obtenerTodasParaGrafica(): List<Venta>

    @Query("DELETE FROM ventas")
    fun eliminarTodas()

    @Query("DELETE FROM venta_detalles")
    fun eliminarTodosDetalles()
}
