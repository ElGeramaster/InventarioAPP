package com.example.inventario

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Producto::class, Venta::class, VentaDetalle::class], version = 4)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao
    abstract fun ventaDao(): VentaDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE productos ADD COLUMN imagenUri TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE productos ADD COLUMN precioCompra REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ventas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        total REAL NOT NULL,
                        ganancia REAL NOT NULL,
                        totalArticulos INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS venta_detalles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ventaId INTEGER NOT NULL,
                        productoId INTEGER NOT NULL,
                        productoNombre TEXT NOT NULL,
                        cantidad INTEGER NOT NULL,
                        precioUnitario REAL NOT NULL,
                        precioCompra REAL NOT NULL,
                        subtotal REAL NOT NULL
                    )
                """)
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inventario_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .allowMainThreadQueries()
                    .build()
                    .also { instancia = it }
            }
        }
    }
}
