# AppHub — cola de tareas

Proyecto: tienda de apps basada en GitHub (package `com.buddyapps.apphub`).

| ID | Título | Por qué importa | Criterio de aceptación | Estado | Evidencia |
|----|--------|-----------------|------------------------|--------|-----------|
| A1 | Base: navegación y diseño | Necesita una estructura tipo tienda sobre la que crecer | 4 pestañas (Explorar, Buscar, Mis apps, Actualizaciones) visibles y navegables en el teléfono | pendiente | — |
| A2 | Explorar y buscar apps reales en GitHub | Sin esto no hay tienda | La API de GitHub devuelve resultados y se muestran en pantalla con nombre, autor, descripción y estrellas | pendiente | — |
| A3 | Detalle + descarga e instalación de APK | Permite obtener apps de otros usuarios | Al tocar una app se ve su release y un APK se descarga; la notificación abre el instalador | pendiente | — |
| A4 | Mis apps con token de GitHub | El usuario quiere ver sus apps | Con token guardado, se listan sus repositorios desde `/user/repos` | pendiente | — |
| A5 | Actualizaciones (apps instaladas y la propia AppHub) | El usuario pidió actualizaciones vía GitHub | Se comparan versiones instaladas contra releases y se ofrece descargar; AppHub comprueba su propio repo configurado | pendiente | — |
| A6 | Publicar una app en el GitHub del usuario | El usuario lo pidió para más adelante, cuando dé el token | Crear/actualizar repo y subir APK a un release con el token | pendiente (espera token) | — |
| A7 | Auditoría visual y funcional en dispositivo | "Terminado" significa verificado | Capturas de las 4 pestañas, detalle, estados vacío/error y flujo de descarga | pendiente | — |

Notas: el token se introduce en la propia app (nunca lo teclea Buddy). La publicación (A6) queda para la siguiente entrega, tras recibir el token.
