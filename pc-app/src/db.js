const path = require('path');
const { app } = require('electron');
const Database = require('better-sqlite3');

// Mismas tablas y campos que la app Android (Room, versión 9) para poder
// migrar/sincronizar datos entre ambas en el futuro.
const dbPath = path.join(app.getPath('userData'), 'inventario.db');
const db = new Database(dbPath);

db.exec(`
  CREATE TABLE IF NOT EXISTS productos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    categoria TEXT NOT NULL,
    precioCompra REAL NOT NULL DEFAULT 0,
    precio REAL NOT NULL DEFAULT 0,
    cantidad INTEGER NOT NULL DEFAULT 0,
    stockMinimo INTEGER NOT NULL DEFAULT 0,
    imagenUri TEXT,
    codigoBarras TEXT,
    favorito INTEGER NOT NULL DEFAULT 0,
    vendePorPeso INTEGER NOT NULL DEFAULT 0,
    precioKilo REAL NOT NULL DEFAULT 0,
    precioCompraKilo REAL NOT NULL DEFAULT 0,
    fechaCaducidad INTEGER
  );

  CREATE TABLE IF NOT EXISTS ventas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER NOT NULL,
    total REAL NOT NULL,
    ganancia REAL NOT NULL,
    totalArticulos INTEGER NOT NULL
  );

  CREATE TABLE IF NOT EXISTS venta_detalles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ventaId INTEGER NOT NULL,
    productoId INTEGER NOT NULL,
    productoNombre TEXT NOT NULL,
    cantidad INTEGER NOT NULL,
    precioUnitario REAL NOT NULL,
    precioCompra REAL NOT NULL,
    subtotal REAL NOT NULL
  );

  CREATE TABLE IF NOT EXISTS proveedores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    telefono TEXT,
    producto TEXT,
    direccion TEXT,
    notas TEXT,
    imagenUri TEXT,
    timestamp INTEGER NOT NULL
  );

  CREATE TABLE IF NOT EXISTS fiados (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    telefono TEXT,
    monto REAL NOT NULL,
    descripcion TEXT,
    timestamp INTEGER NOT NULL
  );

  CREATE TABLE IF NOT EXISTS config (
    clave TEXT PRIMARY KEY,
    valor TEXT
  );
`);

// Si la base viene de una versión anterior del programa, agrega columnas nuevas
// sin perder datos (equivalente a las migraciones de Room).
const columnasProductos = db.prepare('PRAGMA table_info(productos)').all().map((c) => c.name);
function agregarColumnaSiFalta(nombre, definicion) {
  if (!columnasProductos.includes(nombre)) {
    db.exec(`ALTER TABLE productos ADD COLUMN ${nombre} ${definicion}`);
  }
}
agregarColumnaSiFalta('imagenUri', 'TEXT');
agregarColumnaSiFalta('vendePorPeso', 'INTEGER NOT NULL DEFAULT 0');
agregarColumnaSiFalta('precioKilo', 'REAL NOT NULL DEFAULT 0');
agregarColumnaSiFalta('precioCompraKilo', 'REAL NOT NULL DEFAULT 0');
agregarColumnaSiFalta('fechaCaducidad', 'INTEGER');

// ---------- Productos ----------

const CAMPOS_PRODUCTO = `nombre, categoria, precioCompra, precio, cantidad, stockMinimo,
  imagenUri, codigoBarras, favorito, vendePorPeso, precioKilo, precioCompraKilo, fechaCaducidad`;

function normalizarProducto(p) {
  return {
    nombre: p.nombre,
    categoria: p.categoria,
    precioCompra: p.precioCompra || 0,
    precio: p.precio || 0,
    cantidad: p.cantidad || 0,
    stockMinimo: p.stockMinimo || 0,
    imagenUri: p.imagenUri || null,
    codigoBarras: p.codigoBarras || null,
    favorito: p.favorito ? 1 : 0,
    vendePorPeso: p.vendePorPeso ? 1 : 0,
    precioKilo: p.precioKilo || 0,
    precioCompraKilo: p.precioCompraKilo || 0,
    fechaCaducidad: p.fechaCaducidad ?? null,
  };
}

function listarProductos() {
  return db.prepare('SELECT * FROM productos ORDER BY nombre ASC').all();
}

function buscarProductos(busqueda) {
  return db
    .prepare(
      `SELECT * FROM productos
       WHERE nombre LIKE '%' || ? || '%' OR categoria LIKE '%' || ? || '%'
       ORDER BY nombre ASC`
    )
    .all(busqueda, busqueda);
}

function agregarProducto(producto) {
  const p = normalizarProducto(producto);
  const info = db
    .prepare(
      `INSERT INTO productos (${CAMPOS_PRODUCTO})
       VALUES (@nombre, @categoria, @precioCompra, @precio, @cantidad, @stockMinimo,
               @imagenUri, @codigoBarras, @favorito, @vendePorPeso, @precioKilo,
               @precioCompraKilo, @fechaCaducidad)`
    )
    .run(p);
  return info.lastInsertRowid;
}

function actualizarProducto(producto) {
  const p = normalizarProducto(producto);
  db.prepare(
    `UPDATE productos SET nombre=@nombre, categoria=@categoria, precioCompra=@precioCompra,
       precio=@precio, cantidad=@cantidad, stockMinimo=@stockMinimo, imagenUri=@imagenUri,
       codigoBarras=@codigoBarras, favorito=@favorito, vendePorPeso=@vendePorPeso,
       precioKilo=@precioKilo, precioCompraKilo=@precioCompraKilo, fechaCaducidad=@fechaCaducidad
     WHERE id=@id`
  ).run({ ...p, id: producto.id });
}

function eliminarProducto(id) {
  db.prepare('DELETE FROM productos WHERE id = ?').run(id);
}

function marcarFavorito(id, favorito) {
  db.prepare('UPDATE productos SET favorito = ? WHERE id = ?').run(favorito ? 1 : 0, id);
}

function obtenerCategorias() {
  return db
    .prepare('SELECT DISTINCT categoria FROM productos ORDER BY categoria ASC')
    .all()
    .map((r) => r.categoria);
}

function buscarPorCodigoBarras(codigo) {
  return db.prepare('SELECT * FROM productos WHERE codigoBarras = ? LIMIT 1').get(codigo) || null;
}

// Igual que la app: los productos que SOLO se venden por peso no llevan control de stock.
function obtenerStockBajo() {
  return db
    .prepare(
      `SELECT * FROM productos
       WHERE cantidad <= stockMinimo AND NOT (vendePorPeso = 1 AND precio <= 0)
       ORDER BY nombre ASC`
    )
    .all();
}

function obtenerPorCaducar(limite) {
  return db
    .prepare(
      `SELECT * FROM productos
       WHERE fechaCaducidad IS NOT NULL AND fechaCaducidad <= ?
       ORDER BY fechaCaducidad ASC`
    )
    .all(limite);
}

// ---------- Ventas ----------

// Guarda la venta completa (venta + detalles + descuento de stock) en una
// transacción: o se guarda todo o no se guarda nada.
const registrarVentaTx = db.transaction((venta, detalles, descuentos) => {
  const info = db
    .prepare('INSERT INTO ventas (timestamp, total, ganancia, totalArticulos) VALUES (?, ?, ?, ?)')
    .run(venta.timestamp, venta.total, venta.ganancia, venta.totalArticulos);
  const ventaId = info.lastInsertRowid;

  const insertarDetalle = db.prepare(
    `INSERT INTO venta_detalles (ventaId, productoId, productoNombre, cantidad, precioUnitario, precioCompra, subtotal)
     VALUES (?, ?, ?, ?, ?, ?, ?)`
  );
  for (const d of detalles) {
    insertarDetalle.run(ventaId, d.productoId, d.productoNombre, d.cantidad, d.precioUnitario, d.precioCompra, d.subtotal);
  }

  const descontar = db.prepare('UPDATE productos SET cantidad = cantidad - ? WHERE id = ?');
  for (const desc of descuentos) {
    descontar.run(desc.cantidad, desc.productoId);
  }
  return ventaId;
});

function registrarVenta({ venta, detalles, descuentos }) {
  return registrarVentaTx(venta, detalles, descuentos);
}

function obtenerVentasDesde(desde) {
  if (desde == null) {
    return db.prepare('SELECT * FROM ventas ORDER BY timestamp DESC').all();
  }
  return db.prepare('SELECT * FROM ventas WHERE timestamp >= ? ORDER BY timestamp DESC').all(desde);
}

function obtenerDetallesVenta(ventaId) {
  return db.prepare('SELECT * FROM venta_detalles WHERE ventaId = ?').all(ventaId);
}

function obtenerResumenVentas(desde) {
  return db
    .prepare(
      `SELECT COUNT(*) AS conteo, COALESCE(SUM(total), 0) AS ingresos, COALESCE(SUM(ganancia), 0) AS ganancia
       FROM ventas WHERE timestamp >= ?`
    )
    .get(desde);
}

function obtenerMasVendidos(desde, limite) {
  return db
    .prepare(
      `SELECT vd.productoNombre, SUM(vd.cantidad) AS totalCantidad
       FROM venta_detalles vd INNER JOIN ventas v ON vd.ventaId = v.id
       WHERE v.timestamp >= ?
       GROUP BY vd.productoNombre ORDER BY totalCantidad DESC LIMIT ?`
    )
    .all(desde, limite);
}

function obtenerMenosVendidos(desde, limite) {
  return db
    .prepare(
      `SELECT vd.productoNombre, SUM(vd.cantidad) AS totalCantidad
       FROM venta_detalles vd INNER JOIN ventas v ON vd.ventaId = v.id
       WHERE v.timestamp >= ?
       GROUP BY vd.productoNombre ORDER BY totalCantidad ASC LIMIT ?`
    )
    .all(desde, limite);
}

function limpiarHistorialVentas() {
  db.prepare('DELETE FROM venta_detalles').run();
  db.prepare('DELETE FROM ventas').run();
}

// ---------- Proveedores ----------

function listarProveedores() {
  return db.prepare('SELECT * FROM proveedores ORDER BY nombre ASC').all();
}

function agregarProveedor(p) {
  const info = db
    .prepare(
      `INSERT INTO proveedores (nombre, telefono, producto, direccion, notas, imagenUri, timestamp)
       VALUES (?, ?, ?, ?, ?, ?, ?)`
    )
    .run(p.nombre, p.telefono || null, p.producto || null, p.direccion || null, p.notas || null, null, Date.now());
  return info.lastInsertRowid;
}

function actualizarProveedor(p) {
  db.prepare(
    `UPDATE proveedores SET nombre=?, telefono=?, producto=?, direccion=?, notas=? WHERE id=?`
  ).run(p.nombre, p.telefono || null, p.producto || null, p.direccion || null, p.notas || null, p.id);
}

function eliminarProveedor(id) {
  db.prepare('DELETE FROM proveedores WHERE id = ?').run(id);
}

// ---------- Fiados ----------

function listarFiados() {
  return db.prepare('SELECT * FROM fiados ORDER BY nombre ASC').all();
}

function obtenerTotalDeuda() {
  return db.prepare('SELECT COALESCE(SUM(monto), 0) AS total FROM fiados').get().total;
}

function agregarFiado(f) {
  const info = db
    .prepare('INSERT INTO fiados (nombre, telefono, monto, descripcion, timestamp) VALUES (?, ?, ?, ?, ?)')
    .run(f.nombre, f.telefono || null, f.monto, f.descripcion || null, Date.now());
  return info.lastInsertRowid;
}

function actualizarFiado(f) {
  db.prepare('UPDATE fiados SET nombre=?, telefono=?, monto=?, descripcion=? WHERE id=?').run(
    f.nombre,
    f.telefono || null,
    f.monto,
    f.descripcion || null,
    f.id
  );
}

function eliminarFiado(id) {
  db.prepare('DELETE FROM fiados WHERE id = ?').run(id);
}

// ---------- Config (nombre de la tienda, etc.) ----------

function obtenerConfig(clave) {
  const fila = db.prepare('SELECT valor FROM config WHERE clave = ?').get(clave);
  return fila ? fila.valor : null;
}

function guardarConfig(clave, valor) {
  db.prepare('INSERT INTO config (clave, valor) VALUES (?, ?) ON CONFLICT(clave) DO UPDATE SET valor = excluded.valor').run(clave, valor);
}

module.exports = {
  listarProductos,
  buscarProductos,
  agregarProducto,
  actualizarProducto,
  eliminarProducto,
  marcarFavorito,
  obtenerCategorias,
  buscarPorCodigoBarras,
  obtenerStockBajo,
  obtenerPorCaducar,
  registrarVenta,
  obtenerVentasDesde,
  obtenerDetallesVenta,
  obtenerResumenVentas,
  obtenerMasVendidos,
  obtenerMenosVendidos,
  limpiarHistorialVentas,
  listarProveedores,
  agregarProveedor,
  actualizarProveedor,
  eliminarProveedor,
  listarFiados,
  obtenerTotalDeuda,
  agregarFiado,
  actualizarFiado,
  eliminarFiado,
  obtenerConfig,
  guardarConfig,
};
