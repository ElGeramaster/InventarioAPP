package com.example.inventario.HistorialVentas

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VentaDao {

    @Insert
    fun insertarVenta(venta: Venta): Long

    @Insert
    fun insertarDetalles(detalles: List<DetalleVenta>)

    @Query("SELECT * FROM ventas WHERE fecha >= :desde ORDER BY fecha DESC")
    fun obtenerVentas(desde: Long): List<Venta>

    @Query("SELECT COUNT(*) FROM ventas WHERE fecha >= :desde")
    fun contarVentas(desde: Long): Int

    @Query("SELECT COALESCE(SUM(total), 0.0) FROM ventas WHERE fecha >= :desde")
    fun totalIngresos(desde: Long): Double

    @Query("""
        SELECT COALESCE(SUM((dv.precioUnitario - dv.precioCompra) * dv.cantidad), 0.0)
        FROM detalle_ventas dv
        INNER JOIN ventas v ON v.id = dv.ventaId
        WHERE v.fecha >= :desde
    """)
    fun totalGanancia(desde: Long): Double

    @Query("""
        SELECT dv.nombreProducto, dv.productoId,
               SUM(dv.cantidad) as totalVendido,
               SUM(dv.precioUnitario * dv.cantidad) as totalIngresos,
               SUM((dv.precioUnitario - dv.precioCompra) * dv.cantidad) as totalGanancia
        FROM detalle_ventas dv
        INNER JOIN ventas v ON v.id = dv.ventaId
        WHERE v.fecha >= :desde
        GROUP BY dv.productoId, dv.nombreProducto
        ORDER BY totalVendido DESC
    """)
    fun estadisticasProductos(desde: Long): List<EstadisticaProducto>

    @Query("SELECT * FROM detalle_ventas WHERE ventaId = :ventaId")
    fun obtenerDetalles(ventaId: Int): List<DetalleVenta>
}
