# PROGRESS_LOG — AppHub

## 2026-11-23 — Lote: creación de la base
- Tareas: A1–A5 implementadas como base; A6 queda pendiente del token del usuario; A7 en curso.
- Cambios: proyecto nuevo `apphub` (Java plano, sin AndroidX), manifest, UI por código, cliente GitHub API con caché, descarga vía DownloadManager, seguimiento de versiones en SharedPreferences.
- Compilación: OK.
- Pruebas: sin framework de tests (proyecto Java plano); verificación manual en dispositivo.
- Verificación en dispositivo: pendiente.
- Auditoría visual: pendiente.
- Regresiones: ninguna (proyecto nuevo).

## 2026-11-23 — Lote: catálogo y publicación (v1.3, code 4)
- Cambios: `catalog.json` en el repo, `GitHubApi.catalog()`/`publishCatalog()` (Contents API con token),
  `PublishActivity` para elegir repo y publicarlo, pestaña Creador alimentada por el catálogo,
  `CatalogItem`, `LinearLoading`, cache-busting del catálogo al actualizar.
- Compilación: OK.
- Verificación en dispositivo: pendiente (instalación interrumpida por uso activo del teléfono).

## 2026-11-23 — Lote: descargas con progreso (v1.4, code 5)
- Cambios: `DownloadsActivity` con barra de progreso en vivo y «Instalar ahora», `InstallPromptActivity`
  que pregunta al terminar, `InstallHelper.install/notifyFailed`, `StorePrefs.getDownload/allDownloads/removeDownload`,
  `DownloadReceiver` ya no borra el registro de la descarga y lanza el aviso de instalación.
- Compilación: OK.
- Verificación en dispositivo: pendiente.

## 2026-11-23 — Lote: apps instaladas + animaciones (v1.5, code 6)
- Cambios: `InstalledAppsActivity` (escaneo de apps del dispositivo con `PackageManager`, filtro por nombre/paquete,
  badge «DE APPHUB», comprobación de actualizaciones y descarga); `StorePrefs.Tracked` ahora guarda packageName
  y appName y añade `trackedByPackage()`; `DownloadReceiver` extrae el paquete del APK descargado;
  `Anim` con slide-in de filas, fade de pestañas, pop de navegación, spin del botón actualizar,
  realimentación al pulsar botones y progreso suave de la barra; entrada «Ver dispositivo» en Actualizaciones.
- Compilación: OK (`Built .../apphub/build/app.apk`, firmado).
- Pruebas: pendientes en dispositivo.
- Verificación en dispositivo: **bloqueada** — cada intento de instalación se pausa porque el usuario está usando el teléfono.
  Se le entregó el APK en Descargas para que instale cuando pueda.
- Regresiones: ninguna conocida.
- Siguiente tarea: que el usuario instale v1.5 → verificar las 5 pestañas, detalle con fotos, descarga/instalación
  y detección de apps instaladas (A7); luego publicar `catalog.json` y release v1.5 en GitHub (pedir confirmación).

## 2026-11-25 — Lote: visor de capturas dentro de la app (v1.7, code 8)
- Cambios: `ImageViewerActivity` con ajuste inicial, pellizco, arrastre, botón «Ajustar» y estado de error;
  `RemoteImageLoader.loadFull` decodifica con `inSampleSize` (lado máximo 2400 px) y avisa de carga/fallo;
  las capturas de `AppDetailActivity` abren el visor en lugar del navegador; limpieza de imports y campos sin uso.
- Compilación: OK (`build/app.apk`, firmado, versionCode 8 / v1.7).
- Verificación en dispositivo: pendiente (la v1.7 aún no está instalada).
