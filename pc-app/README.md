# Mi Mercancía — Punto de venta para PC

Versión de escritorio (Electron) de InventarioAPP, con las mismas funciones que
la app de Android y el mismo modelo de datos (para poder migrar/sincronizar los
datos entre ambas en el futuro).

## Módulos

- **Punto de venta**: categorías, favoritos, buscador, lector de código de
  barras (USB/Bluetooth, funciona como teclado), carrito, venta por pieza y por
  peso (¼, ½, 1 kg), confirmación y registro de la venta con descuento de stock.
  El botón "Agregar" de cada tarjeta suma una pieza directo; al tocar el
  recuadro se abre el selector de cuántas unidades.
- **Mi mercancía**: alta/edición/eliminación de productos con todos los campos
  de la app (precio compra/venta, stock, stock mínimo, código de barras,
  favorito, venta por peso con precio por kilo, fecha de caducidad).
- **Historial de ventas**: filtros semana/mes, ingresos y ganancia, gráfica de
  barras por día/semana, más y menos vendidos, detalle de cada venta, limpiar
  historial.
- **Reportes**: valor del inventario, inversión y ganancia posible; listas de
  bajo stock y por caducar (7 días, igual que la app).
- **Fiados**: libreta de deudas con abonos, aumentar deuda y liquidar.
- **Proveedores**: directorio con teléfono, producto que surte, dirección y notas.
- **Ajustes**: nombre de la tienda, logotipo propio (se elige una imagen de la
  computadora y se guarda achicada a 256px), tamaño de la letra de toda la
  interfaz y sonido de los botones.

## Cómo correr

```bash
npm install
npm start
```

Si al arrancar aparece un error de `better_sqlite3.node` compilado para otra
versión de Node, ejecuta:

```bash
npx electron-rebuild
npm start
```

## Estructura

- `src/main.js` — proceso principal: crea la ventana y expone la base de datos por IPC.
- `src/preload.js` — puente seguro (`window.api`) entre la interfaz y el proceso principal.
- `src/db.js` — SQLite local (better-sqlite3) con las mismas tablas que Room:
  `productos`, `ventas`, `venta_detalles`, `proveedores`, `fiados` (+ `config`).
- `src/index.html`, `src/renderer.js`, `src/style.css` — interfaz completa.

La base de datos se guarda en la carpeta de datos del usuario
(Windows: `%APPDATA%/mi-mercancia/inventario.db`).

## Siguientes pasos

- Imprimir ticket (impresora térmica ESC/POS) y abrir cajón de dinero.
- Importar los datos exportados de la app Android.
- Backend compartido para sincronizar app y PC.
