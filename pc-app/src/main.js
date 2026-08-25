const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');

function crearVentana() {
  const win = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 1000,
    minHeight: 640,
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
    },
  });
  win.loadFile(path.join(__dirname, 'index.html'));
}

app.whenReady().then(() => {
  // La base de datos usa app.getPath(), así que se carga cuando la app está lista.
  const db = require('./db');

  // Productos
  ipcMain.handle('productos:listar', () => db.listarProductos());
  ipcMain.handle('productos:buscar', (e, texto) => db.buscarProductos(texto));
  ipcMain.handle('productos:agregar', (e, producto) => db.agregarProducto(producto));
  ipcMain.handle('productos:actualizar', (e, producto) => db.actualizarProducto(producto));
  ipcMain.handle('productos:eliminar', (e, id) => db.eliminarProducto(id));
  ipcMain.handle('productos:favorito', (e, id, favorito) => db.marcarFavorito(id, favorito));
  ipcMain.handle('productos:categorias', () => db.obtenerCategorias());
  ipcMain.handle('productos:porCodigo', (e, codigo) => db.buscarPorCodigoBarras(codigo));
  ipcMain.handle('productos:stockBajo', () => db.obtenerStockBajo());
  ipcMain.handle('productos:porCaducar', (e, limite) => db.obtenerPorCaducar(limite));

  // Ventas
  ipcMain.handle('ventas:registrar', (e, datos) => db.registrarVenta(datos));
  ipcMain.handle('ventas:desde', (e, desde) => db.obtenerVentasDesde(desde));
  ipcMain.handle('ventas:detalles', (e, ventaId) => db.obtenerDetallesVenta(ventaId));
  ipcMain.handle('ventas:resumen', (e, desde) => db.obtenerResumenVentas(desde));
  ipcMain.handle('ventas:masVendidos', (e, desde, limite) => db.obtenerMasVendidos(desde, limite));
  ipcMain.handle('ventas:menosVendidos', (e, desde, limite) => db.obtenerMenosVendidos(desde, limite));
  ipcMain.handle('ventas:limpiar', () => db.limpiarHistorialVentas());

  // Proveedores
  ipcMain.handle('proveedores:listar', () => db.listarProveedores());
  ipcMain.handle('proveedores:agregar', (e, p) => db.agregarProveedor(p));
  ipcMain.handle('proveedores:actualizar', (e, p) => db.actualizarProveedor(p));
  ipcMain.handle('proveedores:eliminar', (e, id) => db.eliminarProveedor(id));

  // Fiados
  ipcMain.handle('fiados:listar', () => db.listarFiados());
  ipcMain.handle('fiados:totalDeuda', () => db.obtenerTotalDeuda());
  ipcMain.handle('fiados:agregar', (e, f) => db.agregarFiado(f));
  ipcMain.handle('fiados:actualizar', (e, f) => db.actualizarFiado(f));
  ipcMain.handle('fiados:eliminar', (e, id) => db.eliminarFiado(id));

  // Config
  ipcMain.handle('config:obtener', (e, clave) => db.obtenerConfig(clave));
  ipcMain.handle('config:guardar', (e, clave, valor) => db.guardarConfig(clave, valor));

  crearVentana();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) crearVentana();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
