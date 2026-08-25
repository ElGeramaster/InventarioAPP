const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('api', {
  productos: {
    listar: () => ipcRenderer.invoke('productos:listar'),
    buscar: (texto) => ipcRenderer.invoke('productos:buscar', texto),
    agregar: (producto) => ipcRenderer.invoke('productos:agregar', producto),
    actualizar: (producto) => ipcRenderer.invoke('productos:actualizar', producto),
    eliminar: (id) => ipcRenderer.invoke('productos:eliminar', id),
    favorito: (id, favorito) => ipcRenderer.invoke('productos:favorito', id, favorito),
    categorias: () => ipcRenderer.invoke('productos:categorias'),
    porCodigo: (codigo) => ipcRenderer.invoke('productos:porCodigo', codigo),
    stockBajo: () => ipcRenderer.invoke('productos:stockBajo'),
    porCaducar: (limite) => ipcRenderer.invoke('productos:porCaducar', limite),
  },
  ventas: {
    registrar: (datos) => ipcRenderer.invoke('ventas:registrar', datos),
    desde: (desde) => ipcRenderer.invoke('ventas:desde', desde),
    detalles: (ventaId) => ipcRenderer.invoke('ventas:detalles', ventaId),
    resumen: (desde) => ipcRenderer.invoke('ventas:resumen', desde),
    masVendidos: (desde, limite) => ipcRenderer.invoke('ventas:masVendidos', desde, limite),
    menosVendidos: (desde, limite) => ipcRenderer.invoke('ventas:menosVendidos', desde, limite),
    limpiar: () => ipcRenderer.invoke('ventas:limpiar'),
  },
  proveedores: {
    listar: () => ipcRenderer.invoke('proveedores:listar'),
    agregar: (p) => ipcRenderer.invoke('proveedores:agregar', p),
    actualizar: (p) => ipcRenderer.invoke('proveedores:actualizar', p),
    eliminar: (id) => ipcRenderer.invoke('proveedores:eliminar', id),
  },
  fiados: {
    listar: () => ipcRenderer.invoke('fiados:listar'),
    totalDeuda: () => ipcRenderer.invoke('fiados:totalDeuda'),
    agregar: (f) => ipcRenderer.invoke('fiados:agregar', f),
    actualizar: (f) => ipcRenderer.invoke('fiados:actualizar', f),
    eliminar: (id) => ipcRenderer.invoke('fiados:eliminar', id),
  },
  config: {
    obtener: (clave) => ipcRenderer.invoke('config:obtener', clave),
    guardar: (clave, valor) => ipcRenderer.invoke('config:guardar', clave, valor),
  },
});
