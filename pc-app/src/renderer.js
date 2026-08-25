// ===================== Utilidades =====================

const $ = (id) => document.getElementById(id);
const fmt = (n) => `$${Number(n).toFixed(2)}`;
const fmt0 = (n) => `$${Math.round(Number(n))}`;
const DIA_MS = 24 * 60 * 60 * 1000;
const DIAS_ALERTA_CADUCIDAD = 7;

function inicioDeHoy() {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d.getTime();
}

function escapar(texto) {
  const div = document.createElement('div');
  div.textContent = texto ?? '';
  return div.innerHTML;
}

// ----- Sonido de clic (igual que SonidoUI en la app móvil) -----

let audioCtx = null;
let sonidoActivo = true;

/** Clic corto y suave, generado sin archivos de audio. */
function sonarClick() {
  if (!sonidoActivo) return;
  try {
    audioCtx = audioCtx || new AudioContext();
    if (audioCtx.state === 'suspended') audioCtx.resume();

    const ahora = audioCtx.currentTime;
    const osc = audioCtx.createOscillator();
    const vol = audioCtx.createGain();

    osc.type = 'sine';
    osc.frequency.setValueAtTime(880, ahora);
    osc.frequency.exponentialRampToValueAtTime(440, ahora + 0.05);

    vol.gain.setValueAtTime(0.0001, ahora);
    vol.gain.exponentialRampToValueAtTime(0.22, ahora + 0.008);
    vol.gain.exponentialRampToValueAtTime(0.0001, ahora + 0.07);

    osc.connect(vol).connect(audioCtx.destination);
    osc.start(ahora);
    osc.stop(ahora + 0.08);
  } catch {
    // Si el navegador bloquea el audio, la app sigue funcionando igual.
  }
}

// Cualquier botón o tarjeta de producto suena al presionarse.
document.addEventListener('click', (e) => {
  if (e.target.closest('button, .card-producto, .chip, .fila-venta')) sonarClick();
});

let toastTimer = null;
function toast(mensaje) {
  const el = $('toast');
  el.textContent = mensaje;
  el.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (el.hidden = true), 2600);
}

// ----- Sistema de modales -----

function abrirModal(html) {
  $('modalCaja').innerHTML = html;
  $('modalFondo').hidden = false;
}

function cerrarModal() {
  $('modalFondo').hidden = true;
  $('modalCaja').innerHTML = '';
  enfocarLector();
}

$('modalFondo').addEventListener('click', (e) => {
  if (e.target === $('modalFondo')) cerrarModal();
});
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape' && !$('modalFondo').hidden) cerrarModal();
});

/** Modal de confirmación. Llama a alAceptar solo si el usuario acepta. */
function confirmar({ titulo, mensaje, textoAceptar = 'Aceptar', peligro = false }, alAceptar) {
  abrirModal(`
    <h2>${escapar(titulo)}</h2>
    <p class="texto">${mensaje}</p>
    <div class="pie">
      <button class="btn-secundario" id="mCancelar">Cancelar</button>
      <button class="${peligro ? 'btn-primario btn-peligro' : 'btn-primario'}" id="mAceptar">${escapar(textoAceptar)}</button>
    </div>
  `);
  $('mCancelar').onclick = cerrarModal;
  $('mAceptar').onclick = () => {
    cerrarModal();
    alAceptar();
  };
}

// ===================== Navegación =====================

const VISTAS = ['pos', 'inventario', 'historial', 'reportes', 'fiados', 'proveedores'];
let vistaActual = 'pos';

document.querySelectorAll('.nav-item[data-vista]').forEach((btn) => {
  btn.addEventListener('click', () => mostrarVista(btn.dataset.vista));
});

function mostrarVista(vista) {
  vistaActual = vista;
  VISTAS.forEach((v) => {
    $(`vista-${v}`).hidden = v !== vista;
  });
  document.querySelectorAll('.nav-item[data-vista]').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.vista === vista);
  });
  if (vista === 'pos') refrescarPos();
  if (vista === 'inventario') cargarInventario();
  if (vista === 'historial') cargarHistorial();
  if (vista === 'reportes') cargarReportes();
  if (vista === 'fiados') cargarFiados();
  if (vista === 'proveedores') cargarProveedores();
}

async function actualizarDotStock() {
  const bajos = await window.api.productos.stockBajo();
  $('dotStock').hidden = bajos.length === 0;
}

// ===================== Punto de venta =====================

const CAT_TODOS = 'Todos';
const CAT_FAVORITOS = '⭐ Favoritos';

// Carrito: clave "id-U" (por pieza) o "id-P" (por peso) -> item
const carrito = new Map();
let categoriaSeleccionada = CAT_TODOS;
let busquedaPos = '';
let productosPos = [];

const clavePieza = (id) => `${id}-U`;
const clavePeso = (id) => `${id}-P`;

const seVendePorPieza = (p) => !p.vendePorPeso || p.precio > 0;

function subtotalItem(item) {
  return item.porPeso
    ? (item.producto.precioKilo * item.gramos) / 1000
    : item.producto.precio * item.cantidad;
}

function formatearKg(gramos) {
  return `${parseFloat((gramos / 1000).toFixed(3))} kg`;
}

async function refrescarPos() {
  await cargarChipsCategorias();
  await filtrarProductosPos();
  actualizarCarritoUI();
  actualizarDotStock();
  enfocarLector();
}

async function cargarChipsCategorias() {
  const categorias = [CAT_TODOS, CAT_FAVORITOS, ...(await window.api.productos.categorias())];
  if (!categorias.includes(categoriaSeleccionada)) categoriaSeleccionada = CAT_TODOS;
  $('chipsCategorias').innerHTML = categorias
    .map(
      (c, i) =>
        `<button class="chip c${i % 6} ${c === categoriaSeleccionada ? 'active' : ''}" data-cat="${escapar(c)}">${escapar(c)}</button>`
    )
    .join('');
  document.querySelectorAll('.chip').forEach((chip) => {
    chip.onclick = () => {
      categoriaSeleccionada = chip.dataset.cat;
      cargarChipsCategorias();
      filtrarProductosPos();
    };
  });
}

async function filtrarProductosPos() {
  let lista = busquedaPos
    ? await window.api.productos.buscar(busquedaPos)
    : await window.api.productos.listar();

  if (categoriaSeleccionada === CAT_FAVORITOS) {
    lista = lista.filter((p) => p.favorito);
  } else if (categoriaSeleccionada !== CAT_TODOS) {
    lista = lista.filter((p) => p.categoria === categoriaSeleccionada);
  }
  productosPos = lista;

  $('posSinProductos').hidden = lista.length > 0;
  $('gridProductos').innerHTML = lista
    .map((p) => {
      const soloPeso = p.vendePorPeso && p.precio <= 0;
      const agotado = !soloPeso && !p.vendePorPeso && p.cantidad <= 0;
      const precioTxt = p.vendePorPeso && p.precioKilo > 0
        ? `${fmt(p.precioKilo)} MXN/kg`
        : `${fmt(p.precio)} MXN`;
      const stockTxt = soloPeso
        ? 'Por peso'
        : agotado
          ? 'Agotado'
          : `Stock: ${p.cantidad}`;
      return `
        <div class="card-producto ${agotado ? 'agotado' : ''}" data-id="${p.id}">
          <div class="nombre">${p.favorito ? '<span class="fav">❤</span> ' : ''}${escapar(p.nombre)}</div>
          <div class="precio">${precioTxt}</div>
          <div class="stock ${agotado ? 'agotado' : ''}">${stockTxt}</div>
          <button class="btn-agregar">Agregar</button>
        </div>`;
    })
    .join('');

  document.querySelectorAll('.card-producto').forEach((card) => {
    const producto = productosPos.find((p) => p.id === Number(card.dataset.id));
    if (!producto) return;

    // Botón "Agregar": suma 1 pieza directo, sin preguntar.
    card.querySelector('.btn-agregar').onclick = (e) => {
      e.stopPropagation();
      if (producto.vendePorPeso && producto.precio <= 0) {
        modalPeso(producto);
      } else if (agregarAlCarrito(producto)) {
        toast(`Agregado: ${producto.nombre}`);
      }
    };

    // Clic en el recuadro: abre el selector de cuántas unidades.
    card.onclick = () => elegirModoVenta(producto);
  });
}

$('inputBuscarPos').addEventListener('input', (e) => {
  busquedaPos = e.target.value.trim();
  filtrarProductosPos();
});

// ----- Lector de código de barras (funciona como teclado + Enter) -----

function enfocarLector() {
  if (vistaActual === 'pos' && $('modalFondo').hidden && document.activeElement !== $('inputBuscarPos')) {
    $('inputLector').focus();
  }
}

$('inputLector').addEventListener('keydown', async (e) => {
  if (e.key !== 'Enter') return;
  const codigo = $('inputLector').value.trim();
  $('inputLector').value = '';
  if (!codigo) return;

  const producto = await window.api.productos.porCodigo(codigo);
  if (!producto) {
    toast(`Ningún producto tiene el código ${codigo}`);
  } else if (producto.vendePorPeso) {
    elegirModoVenta(producto);
  } else if (agregarAlCarrito(producto)) {
    toast(`Agregado: ${producto.nombre}`);
  }
});

// Al hacer clic en zonas vacías del POS, el foco regresa al lector.
$('vista-pos').addEventListener('click', (e) => {
  if (e.target.tagName !== 'INPUT' && e.target.tagName !== 'BUTTON') enfocarLector();
});

// ----- Carrito -----

function agregarAlCarrito(producto, cantidad = 1, avisarStock = true) {
  const clave = clavePieza(producto.id);
  const existente = carrito.get(clave);
  const enCarrito = existente ? existente.cantidad : 0;

  if (enCarrito + cantidad > producto.cantidad) {
    if (avisarStock) toast(`No hay suficiente stock de ${producto.nombre}`);
    return false;
  }
  if (existente) existente.cantidad += cantidad;
  else carrito.set(clave, { producto, cantidad, gramos: 0, porPeso: false });
  actualizarCarritoUI();
  return true;
}

function agregarPesoAlCarrito(producto, gramos) {
  if (gramos <= 0) return;
  const clave = clavePeso(producto.id);
  const existente = carrito.get(clave);
  if (existente) existente.gramos += gramos;
  else carrito.set(clave, { producto, cantidad: 0, gramos, porPeso: true });
  actualizarCarritoUI();
}

function actualizarCarritoUI() {
  const items = [...carrito.values()];
  const total = items.reduce((s, it) => s + subtotalItem(it), 0);

  $('carritoVacio').hidden = items.length > 0;
  $('btnRealizarVenta').disabled = items.length === 0;
  $('btnRealizarVenta').textContent = `REALIZAR VENTA — ${fmt(total)} MXN`;

  $('carritoItems').innerHTML = items
    .map((it, i) => {
      const detalle = it.porPeso
        ? `${formatearKg(it.gramos)} · ${fmt(it.producto.precioKilo)}/kg`
        : `${it.cantidad} × ${fmt(it.producto.precio)}`;
      return `
        <div class="carrito-item">
          <div class="nombre">${escapar(it.producto.nombre)}</div>
          <div class="linea">
            <span class="detalle">${detalle}</span>
            <span class="subtotal">${fmt(subtotalItem(it))}</span>
            ${it.porPeso ? '' : `<button class="btn-mini" data-accion="menos" data-i="${i}">−</button>
            <button class="btn-mini" data-accion="mas" data-i="${i}">＋</button>`}
            <button class="btn-mini rojo" data-accion="quitar" data-i="${i}">✕</button>
          </div>
        </div>`;
    })
    .join('');

  $('carritoItems').querySelectorAll('button').forEach((btn) => {
    btn.onclick = () => {
      const item = [...carrito.values()][Number(btn.dataset.i)];
      const clave = item.porPeso ? clavePeso(item.producto.id) : clavePieza(item.producto.id);
      const accion = btn.dataset.accion;
      if (accion === 'quitar') {
        carrito.delete(clave);
      } else if (accion === 'menos') {
        item.cantidad -= 1;
        if (item.cantidad <= 0) carrito.delete(clave);
      } else if (accion === 'mas') {
        if (item.cantidad + 1 > item.producto.cantidad) {
          toast(`No hay más stock de ${item.producto.nombre}`);
          return;
        }
        item.cantidad += 1;
      }
      actualizarCarritoUI();
    };
  });
}

// ----- Elegir cómo vender (pieza / peso) -----

function elegirModoVenta(producto) {
  if (!producto.vendePorPeso) {
    modalCantidad(producto);
    return;
  }
  if (seVendePorPieza(producto) && producto.precio > 0) {
    abrirModal(`
      <h2>${escapar(producto.nombre)}</h2>
      <p class="texto">¿Cómo se vende?</p>
      <div class="pie">
        <button class="btn-secundario" id="mCancelar">Cancelar</button>
        <button class="btn-primario" id="mPieza">Por pieza</button>
        <button class="btn-primario" id="mPeso">Por peso (kg)</button>
      </div>
    `);
    $('mCancelar').onclick = cerrarModal;
    $('mPieza').onclick = () => modalCantidad(producto);
    $('mPeso').onclick = () => modalPeso(producto);
  } else {
    modalPeso(producto);
  }
}

function modalCantidad(producto) {
  const enCarrito = carrito.get(clavePieza(producto.id))?.cantidad ?? 0;
  const disponible = producto.cantidad - enCarrito;
  if (disponible <= 0) {
    cerrarModal();
    toast(`No hay stock disponible de ${producto.nombre}`);
    return;
  }

  let cantidad = 1;
  abrirModal(`
    <h2>${escapar(producto.nombre)}</h2>
    <p class="texto">${fmt(producto.precio)} c/u · Disponibles: ${disponible}</p>
    <div class="cantidad-picker">
      <button class="btn-circulo" id="mMenos">−</button>
      <span class="num" id="mNum">1</span>
      <button class="btn-circulo" id="mMas">＋</button>
    </div>
    <div class="total-modal" id="mTotal"></div>
    <div class="pie">
      <button class="btn-secundario" id="mCancelar">Cancelar</button>
      <button class="btn-primario" id="mAgregar">Agregar a la venta</button>
    </div>
  `);

  const pintar = () => {
    $('mNum').textContent = cantidad;
    $('mTotal').textContent = `Total: ${fmt(cantidad * producto.precio)}`;
    $('mMenos').disabled = cantidad <= 1;
    $('mMas').disabled = cantidad >= disponible;
  };
  pintar();
  $('mMenos').onclick = () => { cantidad--; pintar(); };
  $('mMas').onclick = () => { cantidad++; pintar(); };
  $('mCancelar').onclick = cerrarModal;
  $('mAgregar').onclick = () => {
    cerrarModal();
    agregarAlCarrito(producto, cantidad);
  };
}

function modalPeso(producto) {
  abrirModal(`
    <h2>${escapar(producto.nombre)}</h2>
    <p class="texto">${fmt(producto.precioKilo)} / kg</p>
    <div class="botones-peso">
      <button class="btn-secundario" data-kg="0.25">¼ kg</button>
      <button class="btn-secundario" data-kg="0.5">½ kg</button>
      <button class="btn-secundario" data-kg="1">1 kg</button>
    </div>
    <label>Cantidad en kg</label>
    <input type="number" id="mKg" step="0.001" min="0" placeholder="Ej. 0.5" style="width:100%" />
    <div class="total-modal" id="mTotal">Total: $0.00</div>
    <div class="pie">
      <button class="btn-secundario" id="mCancelar">Cancelar</button>
      <button class="btn-primario" id="mAgregar">Agregar a la venta</button>
    </div>
  `);

  const gramosActuales = () => Math.round((parseFloat($('mKg').value) || 0) * 1000);
  const pintar = () => {
    $('mTotal').textContent = `Total: ${fmt((producto.precioKilo * gramosActuales()) / 1000)}`;
  };
  $('mKg').addEventListener('input', pintar);
  document.querySelectorAll('[data-kg]').forEach((btn) => {
    btn.onclick = () => { $('mKg').value = btn.dataset.kg; pintar(); };
  });
  $('mCancelar').onclick = cerrarModal;
  $('mAgregar').onclick = () => {
    const gramos = gramosActuales();
    if (gramos <= 0) {
      toast('Indica una cantidad en kg');
      return;
    }
    cerrarModal();
    agregarPesoAlCarrito(producto, gramos);
  };
  $('mKg').focus();
}

// ----- Realizar venta -----

$('btnRealizarVenta').addEventListener('click', () => {
  const items = [...carrito.values()];
  if (items.length === 0) return;

  const total = items.reduce((s, it) => s + subtotalItem(it), 0);
  // Cada línea por peso cuenta como 1 artículo; las de pieza cuentan sus unidades.
  const totalArticulos = items.reduce((s, it) => s + (it.porPeso ? 1 : it.cantidad), 0);

  confirmar(
    {
      titulo: 'Confirmar venta',
      mensaje: `Se venderán <b>${totalArticulos} artículo(s)</b> por un total de <b>${fmt(total)}</b>.<br/><br/>¿Continuar?`,
      textoAceptar: 'Realizar venta',
    },
    async () => {
      const ganancia = items.reduce((s, it) => {
        return it.porPeso
          ? s + ((it.producto.precioKilo - it.producto.precioCompraKilo) * it.gramos) / 1000
          : s + (it.producto.precio - it.producto.precioCompra) * it.cantidad;
      }, 0);

      const detalles = items.map((it) =>
        it.porPeso
          ? {
              productoId: it.producto.id,
              productoNombre: `${it.producto.nombre} (${formatearKg(it.gramos)})`,
              cantidad: 1,
              precioUnitario: it.producto.precioKilo,
              precioCompra: it.producto.precioCompraKilo,
              subtotal: subtotalItem(it),
            }
          : {
              productoId: it.producto.id,
              productoNombre: it.producto.nombre,
              cantidad: it.cantidad,
              precioUnitario: it.producto.precio,
              precioCompra: it.producto.precioCompra,
              subtotal: subtotalItem(it),
            }
      );

      // Solo las ventas por pieza descuentan stock.
      const descuentos = items
        .filter((it) => !it.porPeso)
        .map((it) => ({ productoId: it.producto.id, cantidad: it.cantidad }));

      await window.api.ventas.registrar({
        venta: { timestamp: Date.now(), total, ganancia, totalArticulos },
        detalles,
        descuentos,
      });

      carrito.clear();
      actualizarCarritoUI();
      filtrarProductosPos();
      actualizarDotStock();
      toast('✅ Venta realizada con éxito');
    }
  );
});

// ===================== Inventario =====================

let busquedaInv = '';

$('inputBuscarInv').addEventListener('input', (e) => {
  busquedaInv = e.target.value.trim();
  cargarInventario();
});

async function cargarInventario() {
  const lista = busquedaInv
    ? await window.api.productos.buscar(busquedaInv)
    : await window.api.productos.listar();

  $('invVacio').hidden = lista.length > 0;
  $('tbodyInventario').innerHTML = lista
    .map((p) => {
      const soloPeso = p.vendePorPeso && p.precio <= 0;
      const stockBajo = !soloPeso && p.cantidad <= p.stockMinimo;
      const stockHtml = soloPeso
        ? '<span class="badge peso">Por peso</span>'
        : `<span class="badge ${stockBajo ? 'bajo' : 'ok'}">${p.cantidad} pzas</span>`;
      const precioVenta = p.vendePorPeso && p.precioKilo > 0 ? `${fmt(p.precioKilo)}/kg` : fmt(p.precio);
      const caducidad = p.fechaCaducidad ? new Date(p.fechaCaducidad).toLocaleDateString('es-MX') : '—';
      return `
        <tr>
          <td><button class="btn-fav" data-fav="${p.id}" title="Favorito">${p.favorito ? '❤️' : '🤍'}</button></td>
          <td><b>${escapar(p.nombre)}</b></td>
          <td>${escapar(p.categoria)}</td>
          <td class="num">${fmt(p.vendePorPeso && p.precio <= 0 ? p.precioCompraKilo : p.precioCompra)}</td>
          <td class="num">${precioVenta}</td>
          <td>${stockHtml}</td>
          <td>${escapar(p.codigoBarras || '—')}</td>
          <td>${caducidad}</td>
          <td>
            <button class="btn-mini" data-editar="${p.id}" title="Editar">✏️</button>
            <button class="btn-mini rojo" data-borrar="${p.id}" title="Eliminar">🗑️</button>
          </td>
        </tr>`;
    })
    .join('');

  $('tbodyInventario').querySelectorAll('[data-fav]').forEach((btn) => {
    btn.onclick = async () => {
      const p = lista.find((x) => x.id === Number(btn.dataset.fav));
      await window.api.productos.favorito(p.id, !p.favorito);
      cargarInventario();
    };
  });
  $('tbodyInventario').querySelectorAll('[data-editar]').forEach((btn) => {
    btn.onclick = () => modalProducto(lista.find((x) => x.id === Number(btn.dataset.editar)));
  });
  $('tbodyInventario').querySelectorAll('[data-borrar]').forEach((btn) => {
    btn.onclick = () => {
      const p = lista.find((x) => x.id === Number(btn.dataset.borrar));
      confirmar(
        {
          titulo: 'Eliminar producto',
          mensaje: `¿Eliminar <b>${escapar(p.nombre)}</b> del inventario? Esta acción no se puede deshacer.`,
          textoAceptar: 'Eliminar',
          peligro: true,
        },
        async () => {
          await window.api.productos.eliminar(p.id);
          cargarInventario();
          actualizarDotStock();
          toast('Producto eliminado');
        }
      );
    };
  });
}

$('btnNuevoProducto').addEventListener('click', () => modalProducto(null));

/** Formulario de alta/edición de producto (mismas reglas que la app Android). */
function modalProducto(producto) {
  const p = producto || {};
  const fechaCad = p.fechaCaducidad
    ? new Date(p.fechaCaducidad).toISOString().slice(0, 10)
    : '';
  abrirModal(`
    <h2>${producto ? 'Editar producto' : 'Agregar producto'}</h2>
    <div class="form">
      <div>
        <label>Nombre *</label>
        <input type="text" id="fNombre" style="width:100%" value="${escapar(p.nombre || '')}" />
      </div>
      <div>
        <label>Categoría *</label>
        <input type="text" id="fCategoria" style="width:100%" value="${escapar(p.categoria || '')}"
               placeholder="Ej. Refrescos, Frutas y verduras..." />
      </div>
      <div class="fila-2">
        <div>
          <label>Precio compra (pieza)</label>
          <input type="number" id="fPrecioCompra" step="0.01" min="0" style="width:100%" value="${p.precioCompra ?? ''}" />
        </div>
        <div>
          <label>Precio venta (pieza)</label>
          <input type="number" id="fPrecio" step="0.01" min="0" style="width:100%" value="${p.precio ?? ''}" />
        </div>
      </div>
      <div class="fila-2">
        <div>
          <label>Cantidad (stock)</label>
          <input type="number" id="fCantidad" min="0" style="width:100%" value="${p.cantidad ?? ''}" />
        </div>
        <div>
          <label>Stock mínimo</label>
          <input type="number" id="fStockMinimo" min="0" style="width:100%" value="${p.stockMinimo ?? ''}" />
        </div>
      </div>
      <div>
        <label>Código de barras (escanéalo aquí)</label>
        <input type="text" id="fCodigo" style="width:100%" value="${escapar(p.codigoBarras || '')}" />
      </div>
      <div>
        <label>Fecha de caducidad (opcional)</label>
        <input type="date" id="fCaducidad" style="width:100%" value="${fechaCad}" />
      </div>
      <div class="check">
        <input type="checkbox" id="fPorPeso" ${p.vendePorPeso ? 'checked' : ''} />
        <label for="fPorPeso" style="margin:0">Se vende por peso (frutas y verduras)</label>
      </div>
      <div class="fila-2" id="fCamposPeso" ${p.vendePorPeso ? '' : 'hidden'}>
        <div>
          <label>Precio compra por kg</label>
          <input type="number" id="fPrecioCompraKilo" step="0.01" min="0" style="width:100%" value="${p.precioCompraKilo || ''}" />
        </div>
        <div>
          <label>Precio venta por kg *</label>
          <input type="number" id="fPrecioKilo" step="0.01" min="0" style="width:100%" value="${p.precioKilo || ''}" />
        </div>
      </div>
    </div>
    <div class="pie">
      <button class="btn-secundario" id="mCancelar">Cancelar</button>
      <button class="btn-primario" id="mGuardar">Guardar</button>
    </div>
  `);

  $('fPorPeso').addEventListener('change', () => {
    $('fCamposPeso').hidden = !$('fPorPeso').checked;
  });
  $('mCancelar').onclick = cerrarModal;

  $('mGuardar').onclick = async () => {
    const nombre = $('fNombre').value.trim();
    const categoria = $('fCategoria').value.trim();
    const vendePorPeso = $('fPorPeso').checked;
    const precioKilo = parseFloat($('fPrecioKilo').value) || 0;

    if (!nombre) return toast('El nombre es obligatorio');
    if (!categoria) return toast('La categoría es obligatoria');
    if (vendePorPeso && precioKilo <= 0) return toast('El precio por kilo es obligatorio');
    if (!vendePorPeso) {
      if ($('fPrecio').value === '') return toast('El precio de venta es obligatorio');
      if ($('fCantidad').value === '') return toast('La cantidad es obligatoria');
      if ($('fStockMinimo').value === '') return toast('El stock mínimo es obligatorio');
    }

    const fechaStr = $('fCaducidad').value;
    const datos = {
      id: p.id,
      nombre,
      categoria,
      precioCompra: parseFloat($('fPrecioCompra').value) || 0,
      precio: parseFloat($('fPrecio').value) || 0,
      cantidad: parseInt($('fCantidad').value, 10) || 0,
      stockMinimo: parseInt($('fStockMinimo').value, 10) || 0,
      imagenUri: p.imagenUri || null,
      codigoBarras: $('fCodigo').value.trim() || null,
      favorito: p.favorito || 0,
      vendePorPeso,
      precioKilo: vendePorPeso ? precioKilo : 0,
      precioCompraKilo: vendePorPeso ? parseFloat($('fPrecioCompraKilo').value) || 0 : 0,
      fechaCaducidad: fechaStr ? new Date(`${fechaStr}T00:00:00`).getTime() : null,
    };

    if (producto) await window.api.productos.actualizar(datos);
    else await window.api.productos.agregar(datos);

    cerrarModal();
    cargarInventario();
    actualizarDotStock();
    toast(producto ? 'Producto actualizado' : 'Producto agregado');
  };

  $('fNombre').focus();
}

// ===================== Historial de ventas =====================

let filtroHistorial = 'semana';

$('btnSemana').addEventListener('click', () => {
  filtroHistorial = 'semana';
  $('btnSemana').classList.add('active');
  $('btnMes').classList.remove('active');
  cargarHistorial();
});
$('btnMes').addEventListener('click', () => {
  filtroHistorial = 'mes';
  $('btnMes').classList.add('active');
  $('btnSemana').classList.remove('active');
  cargarHistorial();
});

function timestampDesde() {
  const dias = filtroHistorial === 'semana' ? 6 : 29;
  const d = new Date();
  d.setDate(d.getDate() - dias);
  d.setHours(0, 0, 0, 0);
  return d.getTime();
}

async function cargarHistorial() {
  const desde = timestampDesde();
  const [resumen, ventas, mas, menos] = await Promise.all([
    window.api.ventas.resumen(desde),
    window.api.ventas.desde(desde),
    window.api.ventas.masVendidos(desde, 3),
    window.api.ventas.menosVendidos(desde, 3),
  ]);

  $('statConteo').textContent = resumen.conteo;
  $('statIngresos').textContent = fmt0(resumen.ingresos);
  $('statGanancia').textContent = fmt0(resumen.ganancia);

  pintarGrafica(ventas);
  pintarTop('listaMasVendidos', mas);
  pintarTop('listaMenosVendidos', menos);
  pintarListaVentas(ventas);
}

function pintarTop(idLista, items) {
  $(idLista).innerHTML = items.length
    ? items
        .map(
          (it) =>
            `<li><b>${escapar(it.productoNombre)}</b><br/><span class="unidades">${it.totalCantidad} unidades vendidas</span></li>`
        )
        .join('')
    : '<li class="unidades">Sin datos</li>';
}

function pintarGrafica(ventas) {
  const esSemana = filtroHistorial === 'semana';
  $('tituloGrafica').textContent = esSemana
    ? 'Ingresos por día (última semana)'
    : 'Ingresos por semana (último mes)';

  $('graficaVacia').hidden = ventas.length > 0;
  $('grafica').hidden = ventas.length === 0;
  if (ventas.length === 0) return;

  const hoy = inicioDeHoy();
  let barras = [];

  if (esSemana) {
    for (let i = 6; i >= 0; i--) {
      const inicio = hoy - i * DIA_MS;
      const total = ventas
        .filter((v) => v.timestamp >= inicio && v.timestamp < inicio + DIA_MS)
        .reduce((s, v) => s + v.total, 0);
      const fecha = new Date(inicio);
      barras.push({ label: `${fecha.getDate()}/${fecha.getMonth() + 1}`, total });
    }
  } else {
    // Bloques de 7 días terminando hoy, igual que la app.
    for (let i = 4; i >= 0; i--) {
      const inicio = hoy - (i * 7 + 6) * DIA_MS;
      const fin = hoy - (i * 7 - 1) * DIA_MS;
      const total = ventas
        .filter((v) => v.timestamp >= inicio && v.timestamp < fin)
        .reduce((s, v) => s + v.total, 0);
      const fecha = new Date(inicio);
      barras.push({ label: `${fecha.getDate()}/${fecha.getMonth() + 1}`, total });
    }
  }

  const max = Math.max(...barras.map((b) => b.total), 1);
  $('grafica').innerHTML = barras
    .map(
      (b) => `
      <div class="barra-grupo">
        <span class="barra-valor">${b.total > 0 ? fmt0(b.total) : ''}</span>
        <div class="barra" style="height:${Math.round((b.total / max) * 100)}%"></div>
        <span class="barra-label">${b.label}</span>
      </div>`
    )
    .join('');
}

function pintarListaVentas(ventas) {
  $('ventasVacio').hidden = ventas.length > 0;
  $('listaVentas').innerHTML = ventas
    .map((v) => {
      const fecha = new Date(v.timestamp).toLocaleString('es-MX', {
        day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
      });
      return `
        <div class="fila-venta" data-venta="${v.id}">
          <div>
            <div><b>${v.totalArticulos} artículo(s)</b></div>
            <div class="fecha">${fecha}</div>
          </div>
          <span class="total">${fmt(v.total)}</span>
        </div>`;
    })
    .join('');

  $('listaVentas').querySelectorAll('.fila-venta').forEach((fila) => {
    fila.onclick = () => mostrarDetalleVenta(ventas.find((v) => v.id === Number(fila.dataset.venta)));
  });
}

async function mostrarDetalleVenta(venta) {
  const detalles = await window.api.ventas.detalles(venta.id);
  const fecha = new Date(venta.timestamp).toLocaleString('es-MX', {
    day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });

  const filas = detalles.length
    ? detalles
        .map(
          (d) => `
          <div class="fila">
            <span>${escapar(d.productoNombre)}<br/><small style="color:var(--texto-suave)">${d.cantidad} × ${fmt(d.precioUnitario)}</small></span>
            <span>${fmt(d.subtotal)}</span>
          </div>`
        )
        .join('')
    : '<p class="texto" style="color:var(--texto-suave)">No se registró el desglose de esta venta.</p>';

  abrirModal(`
    <h2>Detalle de venta</h2>
    <p class="texto">${fecha}<br/>
      Total: <b>${fmt(venta.total)}</b> · Ganancia: <b>${fmt(venta.ganancia)}</b><br/>
      ${venta.totalArticulos} artículo(s)</p>
    <div class="desglose">${filas}</div>
    <div class="pie"><button class="btn-primario" id="mCerrar">Cerrar</button></div>
  `);
  $('mCerrar').onclick = cerrarModal;
}

$('btnLimpiarHistorial').addEventListener('click', () => {
  confirmar(
    {
      titulo: 'Limpiar historial',
      mensaje: '¿Eliminar <b>todo</b> el historial de ventas? Esta acción no se puede deshacer.',
      textoAceptar: 'Eliminar todo',
      peligro: true,
    },
    async () => {
      await window.api.ventas.limpiar();
      cargarHistorial();
      toast('Historial eliminado');
    }
  );
});

// ===================== Reportes =====================

let vistaReporte = 'stock';

$('btnStockBajo').addEventListener('click', () => {
  vistaReporte = 'stock';
  cargarReportes();
});
$('btnPorCaducar').addEventListener('click', () => {
  vistaReporte = 'caducar';
  cargarReportes();
});

async function cargarReportes() {
  const productos = await window.api.productos.listar();
  const stockBajo = await window.api.productos.stockBajo();
  const limite = inicioDeHoy() + DIAS_ALERTA_CADUCIDAD * DIA_MS;
  const porCaducar = await window.api.productos.porCaducar(limite);

  const valorVenta = productos.reduce((s, p) => s + p.precio * p.cantidad, 0);
  const valorCompra = productos.reduce((s, p) => s + p.precioCompra * p.cantidad, 0);

  $('repTotalProductos').textContent = productos.length;
  $('repValorVenta').textContent = fmt0(valorVenta);
  $('repValorCompra').textContent = fmt0(valorCompra);
  $('repGanancia').textContent = fmt0(valorVenta - valorCompra);

  $('btnStockBajo').textContent = `${stockBajo.length} · BAJO STOCK`;
  $('btnPorCaducar').textContent = `${porCaducar.length} · POR CADUCAR`;
  $('btnStockBajo').classList.toggle('active', vistaReporte === 'stock');
  $('btnPorCaducar').classList.toggle('active', vistaReporte === 'caducar');

  const lista = vistaReporte === 'stock' ? stockBajo : porCaducar;
  $('reporteVacio').hidden = lista.length > 0;
  $('listaReporte').innerHTML = lista
    .map((p) => {
      const derecha =
        vistaReporte === 'stock'
          ? `<span class="badge bajo">${p.cantidad} pzas (mín. ${p.stockMinimo})</span>`
          : `<span class="badge ${p.fechaCaducidad <= inicioDeHoy() ? 'bajo' : 'peso'}">${
              p.fechaCaducidad <= inicioDeHoy() ? 'VENCIDO — ' : 'Caduca: '
            }${new Date(p.fechaCaducidad).toLocaleDateString('es-MX')}</span>`;
      return `
        <div class="fila-venta">
          <div><b>${escapar(p.nombre)}</b><div class="fecha">${escapar(p.categoria)}</div></div>
          ${derecha}
        </div>`;
    })
    .join('');
}

// ===================== Fiados =====================

async function cargarFiados() {
  const [lista, total] = await Promise.all([
    window.api.fiados.listar(),
    window.api.fiados.totalDeuda(),
  ]);

  $('totalDeuda').textContent = fmt(total);
  $('fiadosVacio').hidden = lista.length > 0;
  $('listaFiados').innerHTML = lista
    .map(
      (f) => `
      <div class="tarjeta-persona">
        <div class="nombre">${escapar(f.nombre)}</div>
        ${f.telefono ? `<div class="dato">📞 ${escapar(f.telefono)}</div>` : ''}
        ${f.descripcion ? `<div class="dato">${escapar(f.descripcion)}</div>` : ''}
        <div class="monto">Debe ${fmt(f.monto)}</div>
        <div class="botones">
          <button class="btn-primario" data-abonar="${f.id}">Abonar</button>
          <button class="btn-secundario" data-aumentar="${f.id}">Fiar más</button>
          <button class="btn-secundario" data-editarF="${f.id}">✏️</button>
          <button class="btn-secundario" data-liquidar="${f.id}">Liquidar</button>
        </div>
      </div>`
    )
    .join('');

  const de = (attr, btn) => lista.find((x) => x.id === Number(btn.dataset[attr]));
  $('listaFiados').querySelectorAll('[data-abonar]').forEach((btn) => {
    btn.onclick = () => modalMontoFiado(de('abonar', btn), 'abonar');
  });
  $('listaFiados').querySelectorAll('[data-aumentar]').forEach((btn) => {
    btn.onclick = () => modalMontoFiado(de('aumentar', btn), 'aumentar');
  });
  $('listaFiados').querySelectorAll('[data-editarF]').forEach((btn) => {
    btn.onclick = () => modalFiado(de('editarF', btn));
  });
  $('listaFiados').querySelectorAll('[data-liquidar]').forEach((btn) => {
    const f = de('liquidar', btn);
    btn.onclick = () =>
      confirmar(
        {
          titulo: 'Liquidar deuda',
          mensaje: `¿Marcar como pagada la deuda de <b>${escapar(f.nombre)}</b> (${fmt(f.monto)}) y quitarla de la lista?`,
          textoAceptar: 'Liquidar',
        },
        async () => {
          await window.api.fiados.eliminar(f.id);
          cargarFiados();
          toast('Deuda liquidada 🎉');
        }
      );
  });
}

$('btnNuevoFiado').addEventListener('click', () => modalFiado(null));

function modalFiado(fiado) {
  const f = fiado || {};
  abrirModal(`
    <h2>${fiado ? 'Editar fiado' : 'Agregar fiado'}</h2>
    <div class="form">
      <div><label>Nombre del cliente *</label>
        <input type="text" id="gNombre" style="width:100%" value="${escapar(f.nombre || '')}" /></div>
      <div><label>Monto que debe *</label>
        <input type="number" id="gMonto" step="0.01" min="0" style="width:100%" value="${f.monto ?? ''}" /></div>
      <div><label>Teléfono</label>
        <input type="text" id="gTelefono" style="width:100%" value="${escapar(f.telefono || '')}" /></div>
      <div><label>Descripción (qué se llevó)</label>
        <input type="text" id="gDescripcion" style="width:100%" value="${escapar(f.descripcion || '')}" /></div>
    </div>
    <div class="pie">
      <button class="btn-secundario" id="mCancelar">Cancelar</button>
      <button class="btn-primario" id="mGuardar">Guardar</button>
    </div>
  `);
  $('mCancelar').onclick = cerrarModal;
  $('mGuardar').onclick = async () => {
    const nombre = $('gNombre').value.trim();
    const monto = parseFloat($('gMonto').value);
    if (!nombre) return toast('El nombre es obligatorio');
    if (!(monto >= 0)) return toast('Indica el monto');

    const datos = {
      id: f.id,
      nombre,
      monto,
      telefono: $('gTelefono').value.trim() || null,
      descripcion: $('gDescripcion').value.trim() || null,
    };
    if (fiado) await window.api.fiados.actualizar(datos);
    else await window.api.fiados.agregar(datos);
    cerrarModal();
    cargarFiados();
  };
  $('gNombre').focus();
}

function modalMontoFiado(fiado, modo) {
  const esAbono = modo === 'abonar';
  abrirModal(`
    <h2>${esAbono ? 'Abonar' : 'Fiar más'} — ${escapar(fiado.nombre)}</h2>
    <p class="texto">Debe actualmente: <b>${fmt(fiado.monto)}</b></p>
    <label>${esAbono ? 'Cantidad que abona' : 'Cantidad que se agrega a la deuda'}</label>
    <input type="number" id="gCantidad" step="0.01" min="0" style="width:100%" />
    <div class="pie">
      <button class="btn-secundario" id="mCancelar">Cancelar</button>
      <button class="btn-primario" id="mAceptar">${esAbono ? 'Abonar' : 'Agregar'}</button>
    </div>
  `);
  $('mCancelar').onclick = cerrarModal;
  $('mAceptar').onclick = async () => {
    const cantidad = parseFloat($('gCantidad').value) || 0;
    if (cantidad <= 0) return toast('Indica una cantidad');

    const nuevoMonto = esAbono ? fiado.monto - cantidad : fiado.monto + cantidad;
    cerrarModal();
    if (esAbono && nuevoMonto <= 0) {
      await window.api.fiados.eliminar(fiado.id);
      toast(`${fiado.nombre} liquidó su deuda 🎉`);
    } else {
      await window.api.fiados.actualizar({ ...fiado, monto: nuevoMonto });
      toast(esAbono ? `Abono registrado. Ahora debe ${fmt(nuevoMonto)}` : `Ahora debe ${fmt(nuevoMonto)}`);
    }
    cargarFiados();
  };
  $('gCantidad').focus();
}

// ===================== Proveedores =====================

async function cargarProveedores() {
  const lista = await window.api.proveedores.listar();
  $('proveedoresVacio').hidden = lista.length > 0;
  $('listaProveedores').innerHTML = lista
    .map(
      (p) => `
      <div class="tarjeta-persona">
        <div class="nombre">🚚 ${escapar(p.nombre)}</div>
        ${p.producto ? `<div class="dato">📦 Surte: ${escapar(p.producto)}</div>` : ''}
        ${p.telefono ? `<div class="dato">📞 ${escapar(p.telefono)}</div>` : ''}
        ${p.direccion ? `<div class="dato">📍 ${escapar(p.direccion)}</div>` : ''}
        ${p.notas ? `<div class="dato">📝 ${escapar(p.notas)}</div>` : ''}
        <div class="botones">
          <button class="btn-secundario" data-editarP="${p.id}">✏️ Editar</button>
          <button class="btn-secundario" data-borrarP="${p.id}">🗑️ Eliminar</button>
        </div>
      </div>`
    )
    .join('');

  $('listaProveedores').querySelectorAll('[data-editarP]').forEach((btn) => {
    btn.onclick = () => modalProveedor(lista.find((x) => x.id === Number(btn.dataset.editarP)));
  });
  $('listaProveedores').querySelectorAll('[data-borrarP]').forEach((btn) => {
    const p = lista.find((x) => x.id === Number(btn.dataset.borrarP));
    btn.onclick = () =>
      confirmar(
        {
          titulo: 'Eliminar proveedor',
          mensaje: `¿Eliminar a <b>${escapar(p.nombre)}</b> de tus proveedores?`,
          textoAceptar: 'Eliminar',
          peligro: true,
        },
        async () => {
          await window.api.proveedores.eliminar(p.id);
          cargarProveedores();
        }
      );
  });
}

$('btnNuevoProveedor').addEventListener('click', () => modalProveedor(null));

function modalProveedor(proveedor) {
  const p = proveedor || {};
  abrirModal(`
    <h2>${proveedor ? 'Editar proveedor' : 'Agregar proveedor'}</h2>
    <div class="form">
      <div><label>Nombre *</label>
        <input type="text" id="pNombre" style="width:100%" value="${escapar(p.nombre || '')}" /></div>
      <div><label>Producto que surte</label>
        <input type="text" id="pProducto" style="width:100%" value="${escapar(p.producto || '')}" /></div>
      <div><label>Teléfono</label>
        <input type="text" id="pTelefono" style="width:100%" value="${escapar(p.telefono || '')}" /></div>
      <div><label>Dirección</label>
        <input type="text" id="pDireccion" style="width:100%" value="${escapar(p.direccion || '')}" /></div>
      <div><label>Notas</label>
        <textarea id="pNotas" rows="2" style="width:100%">${escapar(p.notas || '')}</textarea></div>
    </div>
    <div class="pie">
      <button class="btn-secundario" id="mCancelar">Cancelar</button>
      <button class="btn-primario" id="mGuardar">Guardar</button>
    </div>
  `);
  $('mCancelar').onclick = cerrarModal;
  $('mGuardar').onclick = async () => {
    const nombre = $('pNombre').value.trim();
    if (!nombre) return toast('El nombre es obligatorio');
    const datos = {
      id: p.id,
      nombre,
      producto: $('pProducto').value.trim() || null,
      telefono: $('pTelefono').value.trim() || null,
      direccion: $('pDireccion').value.trim() || null,
      notas: $('pNotas').value.trim() || null,
    };
    if (proveedor) await window.api.proveedores.actualizar(datos);
    else await window.api.proveedores.agregar(datos);
    cerrarModal();
    cargarProveedores();
  };
  $('pNombre').focus();
}

// ===================== Ajustes =====================

async function cargarNombreTienda() {
  const nombre = await window.api.config.obtener('nombreTienda');
  $('nombreTienda').textContent = nombre || 'Mi Tienda';
}

/** Muestra el logotipo que subió el usuario, o el predeterminado si no hay. */
async function cargarLogo() {
  const logo = await window.api.config.obtener('logo');
  $('logoPersonalizado').hidden = !logo;
  $('logoPredeterminado').hidden = !!logo;
  if (logo) $('logoPersonalizado').src = logo;
}

async function cargarTamanoFuente() {
  const guardado = parseFloat(await window.api.config.obtener('zoom'));
  window.api.zoom(guardado > 0 ? guardado : 1);
}

async function cargarSonido() {
  sonidoActivo = (await window.api.config.obtener('sonido')) !== 'off';
}

/**
 * Achica el logotipo a 256px como máximo para que la base de datos no crezca
 * de más si el usuario elige una fotografía grande.
 */
function redimensionarLogo(dataUri, maximo = 256) {
  return new Promise((resolver) => {
    const img = new Image();
    img.onload = () => {
      if (img.width <= maximo && img.height <= maximo) return resolver(dataUri);

      const escala = maximo / Math.max(img.width, img.height);
      const lienzo = document.createElement('canvas');
      lienzo.width = Math.round(img.width * escala);
      lienzo.height = Math.round(img.height * escala);
      lienzo.getContext('2d').drawImage(img, 0, 0, lienzo.width, lienzo.height);
      resolver(lienzo.toDataURL('image/png'));
    };
    img.onerror = () => resolver(dataUri);
    img.src = dataUri;
  });
}

$('btnAjustes').addEventListener('click', async () => {
  const [nombre, logo, zoomGuardado] = await Promise.all([
    window.api.config.obtener('nombreTienda'),
    window.api.config.obtener('logo'),
    window.api.config.obtener('zoom'),
  ]);
  const zoomActual = parseFloat(zoomGuardado) > 0 ? parseFloat(zoomGuardado) : 1;
  // Logo elegido en este momento: null = sin cambios, '' = quitar, data URI = nuevo.
  let logoNuevo = null;

  abrirModal(`
    <h2>⚙️ Ajustes</h2>
    <div class="form">
      <div>
        <label>Nombre de la tienda</label>
        <input type="text" id="aNombre" style="width:100%" value="${escapar(nombre || '')}" placeholder="Mi Tienda" />
      </div>

      <div>
        <label>Logotipo</label>
        <div class="ajuste-logo">
          <img id="aLogoPreview" class="logo-preview" ${logo ? `src="${logo}"` : 'hidden'} alt="" />
          <span id="aLogoTexto" class="logo-texto">${logo ? 'Logotipo personalizado' : 'Usando el logotipo predeterminado'}</span>
        </div>
        <div class="botones-peso">
          <button class="btn-secundario" id="aElegirLogo">📁 Elegir imagen...</button>
          <button class="btn-secundario" id="aQuitarLogo">Usar el predeterminado</button>
        </div>
      </div>

      <div>
        <label>Tamaño de la letra: <b id="aZoomTexto">${Math.round(zoomActual * 100)}%</b></label>
        <input type="range" id="aZoom" min="0.7" max="1.6" step="0.05" value="${zoomActual}" style="width:100%" />
        <div class="rango-etiquetas"><span>Pequeña</span><span>Normal</span><span>Grande</span></div>
      </div>

      <div class="check">
        <input type="checkbox" id="aSonido" ${sonidoActivo ? 'checked' : ''} />
        <label for="aSonido" style="margin:0">🔊 Sonido al presionar botones</label>
      </div>
    </div>
    <div class="pie">
      <button class="btn-secundario" id="mCancelar">Cancelar</button>
      <button class="btn-primario" id="mGuardar">Guardar</button>
    </div>
  `);

  // El tamaño de letra se aplica en vivo para verlo mientras se ajusta.
  $('aZoom').addEventListener('input', () => {
    const factor = parseFloat($('aZoom').value);
    $('aZoomTexto').textContent = `${Math.round(factor * 100)}%`;
    window.api.zoom(factor);
  });

  $('aSonido').addEventListener('change', () => {
    sonidoActivo = $('aSonido').checked;
    if (sonidoActivo) sonarClick();
  });

  $('aElegirLogo').onclick = async () => {
    const imagen = await window.api.config.elegirLogo();
    if (!imagen) return;
    logoNuevo = await redimensionarLogo(imagen);
    $('aLogoPreview').src = logoNuevo;
    $('aLogoPreview').hidden = false;
    $('aLogoTexto').textContent = 'Logotipo nuevo (sin guardar)';
  };

  $('aQuitarLogo').onclick = () => {
    logoNuevo = '';
    $('aLogoPreview').hidden = true;
    $('aLogoTexto').textContent = 'Usando el logotipo predeterminado';
  };

  $('mCancelar').onclick = async () => {
    // Deshace el tamaño de letra y el sonido si no se guardaron.
    window.api.zoom(zoomActual);
    await cargarSonido();
    cerrarModal();
  };

  $('mGuardar').onclick = async () => {
    await window.api.config.guardar('nombreTienda', $('aNombre').value.trim() || 'Mi Tienda');
    await window.api.config.guardar('zoom', $('aZoom').value);
    await window.api.config.guardar('sonido', $('aSonido').checked ? 'on' : 'off');
    if (logoNuevo !== null) await window.api.config.guardar('logo', logoNuevo);

    cerrarModal();
    cargarNombreTienda();
    cargarLogo();
    toast('Ajustes guardados');
  };

  $('aNombre').focus();
});

// ===================== Arranque =====================

cargarTamanoFuente();
cargarSonido();
cargarNombreTienda();
cargarLogo();
refrescarPos();
