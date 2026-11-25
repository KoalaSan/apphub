# AppHub — cola de tareas

Proyecto: tienda de apps basada en GitHub (package `com.buddyapps.apphub`).

| ID | Título | Por qué importa | Criterio de aceptación | Estado | Evidencia |
|----|--------|-----------------|------------------------|--------|-----------|
| A1 | Base: navegación y diseño | Necesita una estructura tipo tienda sobre la que crecer | Pestañas Explorar, Buscar, Creador, Mis apps, Actualizaciones navegables en el teléfono | implementado · verificado v1.2 | UI mostrada en dispositivo (v1.2) |
| A2 | Explorar y buscar apps reales en GitHub | Sin esto no hay tienda | La API de GitHub devuelve resultados; búsqueda por texto, categoría y desarrollador | implementado | build v1.2/v1.5 OK |
| A3 | Detalle + descarga e instalación de APK | Permite obtener apps de otros usuarios | Detalle con README y capturas; descarga con barra de progreso y aviso «Instalar ahora» | implementado | build v1.5 OK · verificación en curso |
| A4 | Mis apps con token de GitHub | El usuario quiere ver sus apps | Con token guardado, se listan sus repositorios desde `/user/repos` | implementado | build v1.2 OK |
| A5 | Actualizaciones tipo Play Store | El usuario pidió actualizaciones vía GitHub | Detectar apps instaladas del dispositivo, comparar versiones contra releases y ofrecer descargar; AppHub comprueba su propio repo | implementado v1.5 | build v1.5 OK · verificación pendiente |
| A6 | Publicar una app en el catálogo | El usuario lo pidió tras dar el token | Elegir repo propio y publicarlo en `catalog.json` de `KoalaSan/apphub` con el token; visible para todos en la pestaña Creador | implementado v1.3 | build v1.3/v1.5 OK · push pendiente |
| A7 | Auditoría visual y funcional en dispositivo | "Terminado" significa verificado | Capturas de las 5 pestañas, detalle con fotos, estados vacío/error, descarga e instalación | **en curso — bloqueado por uso activo del teléfono** | — |

## Estado actual (v1.7, versionCode 8)
- Compila correctamente (`build-apk.sh` → `build/app.apk`, firmado).
- Nuevo en v1.7: visor de capturas propio (`ImageViewerActivity`) con zoom, arrastre y carga segura de imágenes;
  las capturas ya no abren el navegador.
- De v1.5/v1.6: `InstalledAppsActivity` escanea las apps del usuario, comprueba actualizaciones contra GitHub,
  animaciones y descarga con progreso en vivo y diálogo «¿Instalar ahora?».
- Pendiente: instalar v1.7 en el teléfono y verificar A7; subir `catalog.json` y la release a GitHub.

Notas: el token se introduce en la propia app (nunca lo teclea Buddy ni se guarda en ficheros).
Buddy usa la sesión `gh` ya autenticada para publicar en `KoalaSan/apphub`.
