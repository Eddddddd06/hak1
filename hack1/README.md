# Instrucciones para ejecutar la colección Postman y comprobar el estado del reporte

Este archivo explica cómo usar los scripts incluidos en la raíz del proyecto para ejecutar la colección Postman (con Newman) y hacer polling del estado del reporte asíncrono.

Requisitos
- Java + Maven para ejecutar la aplicación Spring Boot localmente.
- Node.js y npm (para instalar newman).
- Newman (`npm install -g newman`).
- PowerShell (Windows) para ejecutar el script de polling. Ajusta la política de ejecución si es necesario.

Archivos relevantes (en la raíz del repo)
- `postman_collection.json` — colección Postman con el flujo end-to-end.
- `postman_environment.json` — environment con variables usadas por la colección.
- `run_postman_and_poll.bat` — script .bat que ejecuta Newman y luego llama al script PowerShell para polling.
- `run_postman_and_poll.ps1` — script PowerShell que lee un environment exportado y hace polling sobre posibles endpoints de estado.

Uso rápido (recomendado)
1. Arranca la aplicación Spring Boot localmente (desde la raíz del proyecto):

```bat
mvn -U clean install -DskipTests
mvn spring-boot:run
```

2. Ejecuta el script `.bat` desde CMD (abre una ventana de CMD en la carpeta del proyecto):

```bat
run_postman_and_poll.bat
```

El script hará lo siguiente:
- Ejecuta `newman run postman_collection.json -e postman_environment.json` y exporta el environment resultante a `exported_env.json`.
- Genera un reporte HTML `newman-report.html` con los resultados de la ejecución.
- Llama al script PowerShell `run_postman_and_poll.ps1` pasando la ruta del environment exportado como argumento. El script PowerShell intentará extraer `baseUrl` y `lastRequestId` del environment exportado y realizará polling sobre varios endpoints candidatos para detectar si el reporte asíncrono ya está COMPLETADO.

Cómo ejecutar manualmente (si prefieres control total)
- Ejecutar newman manualmente (instala newman si hace falta):

```bat
npm install -g newman
newman run "postman_collection.json" -e "postman_environment.json" --export-environment "exported_env.json" --reporters cli,html --reporter-html-export "newman-report.html" --delay-request 1000
```

- Ejecutar el script PowerShell (desde PowerShell):

```powershell
# Permitir ejecución temporalmente si está deshabilitado
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
# Ejecutar el script (ruta relativa)
.\run_postman_and_poll.ps1 .\exported_env.json
```

Notas importantes sobre el polling
- El script PowerShell busca automáticamente `baseUrl` y `lastRequestId`/`lastPremiumRequestId` dentro del environment exportado por Newman.
- Si no encuentra `baseUrl` o `requestId`, te pedirá que los introduzcas manualmente.
- El script no asume un endpoint fijo: prueba varios paths candidatos (por ejemplo `/reports/{id}`, `/reports/{id}/status`, `/sales/summary/{id}`, etc.) y aplica heurísticas para detectar campos `status`, `state`, `completedAt` o `result` en la respuesta.
- Si tu API expone un endpoint de estado concreto (recomendado), por ejemplo `GET /reports/{requestId}`, edita el array `candidatePaths` dentro de `run_postman_and_poll.ps1` para priorizar o dejar solo esa ruta.
- El polling hará hasta 60 intentos con 5s de espera entre intentos (configurable en el script: variables `$maxAttempts` y `$delaySeconds`).

Qué hacer si el polling no detecta completado
- Revisa los logs de la aplicación para ver errores del listener asíncrono.
- Verifica el email destino (si el listener envía email con el resultado).
- Aumenta `$maxAttempts` o `$delaySeconds` en `run_postman_and_poll.ps1` si la generación tarda más.

Personalizaciones útiles que puedo hacer si lo pides
- Añadir polling directo a un endpoint específico si me indicas la ruta.
- Guardar los resultados del polling en un archivo local o abrir automáticamente la URL del resultado.
- Ajustar la colección para usar credenciales reales o los códigos UTEC.

---

Fin de instrucciones.

