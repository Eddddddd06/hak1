# Hackathon #1: Oreo Insight Factory 🍪📈

## Descripción General

¿A quién no le gusta meter una Oreo 🍪 en un vaso con leche 🥛? 

La fábrica de **Oreo** está por lanzar un piloto con UTEC para transformar sus datos de ventas en **insights accionables**. Tu misión es construir un **backend** sencillo y sólido que permita registrar ventas y, a partir de esos datos, **generar resúmenes automáticos** en lenguaje natural usando **GitHub Models** (vía GitHub Marketplace).

El énfasis no está en pantallas ni frontends, sino en la **calidad del contrato de API**, autenticación básica, persistencia, pruebas mínimas y un endpoint de **insights** que consulte un LLM. La validación se hará ejecutando un **Postman Flow** end-to-end sobre tu backend. 🥛🤖

## Duración y Formato

- **Tiempo**: 2 horas
- **Equipos**: Grupos de 4 o 5 estudiantes
- **Recursos**: Uso de IA permitido, documentación y material de Internet

## Contexto del Negocio

Oreo quiere dejar de "mojar la galleta a ciegas" y empezar a **entender qué pasa en cada sucursal**: qué SKU lidera, cuándo hay picos de demanda y cómo evoluciona el ticket promedio. Para ello, busca un backend que reciba ventas, consolide métricas y pida a un **LLM** un **resumen corto y claro** que cualquier analista pueda leer en segundos. 🍪🥛

Tu servicio será el motor de insights: **seguro** (JWT), **consistente** (JPA) y **probado** (testing mínimo). Si el Postman Flow "se la come" completa —login, seed de ventas, consultas y /summary—, ¡estás listo para producción… o al menos para un vaso grande de leche! 🚀

## 💡 ¿Por Qué Este Hackathon Es Tu Mejor Carta de Presentación?

**Este proyecto no es solo un ejercicio académico - es tu portafolio estrella.** 🌟

Imagina estar en una entrevista y poder decir: *"Desarrollé un sistema que integra autenticación JWT, procesamiento asíncrono, integración con LLMs, y envío automatizado de reportes. Todo en 2 horas, trabajando en equipo bajo presión."*

**Lo que demuestras con este proyecto:**
- ✅ Manejo de **arquitecturas modernas** (async, eventos, microservicios)
- ✅ Integración con **IA/LLMs** (la skill más demandada del 2025)
- ✅ **Seguridad** y autenticación empresarial
- ✅ Trabajo con **APIs externas** y servicios de terceros
- ✅ **Colaboración efectiva** bajo presión

Este es el tipo de proyecto que los reclutadores buscan en GitHub. Es real, es complejo, y resuelve un problema de negocio tangible. 🎯

## 🚀 Estrategia para el Éxito: Divide y Vencerás

**¡Ustedes pueden con esto!** El secreto no está en que todos hagan todo, sino en la **comunicación y división inteligente del trabajo**.

### Distribución Sugerida (5 personas):

1. **El Arquitecto** 🏗️: Setup inicial, estructura del proyecto, configuración de Spring Boot
2. **El Guardian** 🔐: JWT, Spring Security, roles y permisos
3. **El Persistente** 💾: JPA, entidades, repositorios, queries
4. **El Comunicador** 📡: Integración con GitHub Models y servicio de email
5. **El Validador** ✅: Postman Collection, testing, documentación

**Pro tip**: Los primeros 20 minutos son CRUCIALES. Úsenlos para:
- Definir interfaces claras entre componentes
- Acordar DTOs y contratos
- Crear branches en Git para cada uno
- Establecer un punto de integración a los 60 minutos

**Recuerden**: La comunicación constante es clave. Un equipo que se comunica bien puede lograr más que 5 genios trabajando aislados. 💪

## Requerimientos Técnicos

### Tecnologías Obligatorias

- Java 21+
- Spring Boot 3.x
- Spring Security con JWT
- Spring Data JPA
- H2 o PostgreSQL (a elección)
- Cliente HTTP o SDK para GitHub Models API
- JavaMail o Spring Boot Mail para envío de correos
- **@Async y @EventListener** para procesamiento asíncrono

### Variables de Entorno Requeridas

```properties
GITHUB_TOKEN=<tu_token_de_GitHub>
GITHUB_MODELS_URL=<endpoint_REST_de_GitHub_Models>
MODEL_ID=<id_del_modelo_del_Marketplace>
JWT_SECRET=<clave_para_firmar_JWT>
# Para envío de correos:
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<tu_email@gmail.com>
MAIL_PASSWORD=<app_password>
# Si usas PostgreSQL:
DB_URL=<jdbc_url>
DB_USER=<usuario_db>
DB_PASS=<password_db>
```

## Roles y Seguridad

Implementar JWT para autenticación y los siguientes roles con sus respectivos permisos:

1. **ROLE_CENTRAL**: Oficina central de Oreo - Acceso completo a todas las ventas de todas las sucursales, reportes globales y gestión de usuarios
2. **ROLE_BRANCH**: Usuario de sucursal - Solo puede ver y crear ventas de su propia sucursal asignada

Cada usuario con `ROLE_BRANCH` debe tener una sucursal asignada al momento del registro.

## Funcionalidades Requeridas

### 1. Autenticación JWT

| Método | Endpoint | Descripción | Roles Permitidos | Request Body | Response |
|--------|----------|-------------|-----------------|--------------|----------|
| POST | `/auth/register` | Crear nuevo usuario | Público | `{"username": "oreo.admin", "email": "admin@oreo.com", "password": "Oreo1234", "role": "CENTRAL"}` o `{"username": "miraflores.user", "email": "mira@oreo.com", "password": "Oreo1234", "role": "BRANCH", "branch": "Miraflores"}` | 201: `{"id": "u_01J...", "username": "oreo.admin", "email": "admin@oreo.com", "role": "CENTRAL", "branch": null, "createdAt": "2025-09-12T18:10:00Z"}` |
| POST | `/auth/login` | Autenticar y obtener JWT | Público | `{"username": "oreo.admin", "password": "Oreo1234"}` | 200: `{"token": "eyJhbGci...", "expiresIn": 3600, "role": "CENTRAL", "branch": null}` |

**Reglas de validación**:
- Username: 3-30 caracteres, alfanumérico + `_` y `.`
- Email: formato válido
- Password: mínimo 8 caracteres
- Role: debe ser uno de ["CENTRAL", "BRANCH"]
- Branch: obligatorio si role es "BRANCH", null si es "CENTRAL"

### 2. Gestión de Ventas

| Método | Endpoint | Descripción | Roles Permitidos | Request Body | Response |
|--------|----------|-------------|-----------------|--------------|----------|
| POST | `/sales` | Crear nueva venta | CENTRAL (cualquier branch), BRANCH (solo su branch) | Ver ejemplo abajo | 201: Venta creada |
| GET | `/sales/{id}` | Obtener detalle de venta | CENTRAL (todas), BRANCH (solo de su branch) | - | 200: Detalle completo |
| GET | `/sales` | Listar ventas con filtros | CENTRAL (todas), BRANCH (solo de su branch) | Query params: `from`, `to`, `branch`, `page`, `size` | 200: Lista paginada |
| PUT | `/sales/{id}` | Actualizar venta | CENTRAL (todas), BRANCH (solo de su branch) | Ver ejemplo abajo | 200: Venta actualizada |
| DELETE | `/sales/{id}` | Eliminar venta | CENTRAL | - | 204: No Content |

**Ejemplo de creación de venta**:
```json
{
  "sku": "OREO_CLASSIC_12",
  "units": 25,
  "price": 1.99,
  "branch": "Miraflores",
  "soldAt": "2025-09-12T16:30:00Z"
}
```

**Nota**: Los usuarios BRANCH solo pueden crear ventas para su sucursal asignada. Si intentan crear para otra sucursal, devolver 403.

**Response esperado** (201):
```json
{
  "id": "s_01K...",
  "sku": "OREO_CLASSIC_12",
  "units": 25,
  "price": 1.99,
  "branch": "Miraflores",
  "soldAt": "2025-09-12T16:30:00Z",
  "createdBy": "miraflores.user"
}
```

### 3. Resumen Semanal ASÍNCRONO con LLM y Email

| Método | Endpoint | Descripción | Roles Permitidos |
|--------|----------|-------------|-----------------|
| POST | `/sales/summary/weekly` | Solicitar generación asíncrona de resumen y envío por email | CENTRAL (cualquier branch), BRANCH (solo su branch) |

**Request** para `/sales/summary/weekly`:
```json
{
  "from": "2025-09-01",
  "to": "2025-09-07",
  "branch": "Miraflores",
  "emailTo": "gerente@oreo.com"
}
```
*Si no se envía `from` y `to`, calcular automáticamente la última semana.*
*Usuarios BRANCH solo pueden generar resúmenes de su propia sucursal.*
*El campo `emailTo` es obligatorio.*

**Response INMEDIATA** (202 Accepted):
```json
{
  "requestId": "req_01K...",
  "status": "PROCESSING",
  "message": "Su solicitud de reporte está siendo procesada. Recibirá el resumen en gerente@oreo.com en unos momentos.",
  "estimatedTime": "30-60 segundos",
  "requestedAt": "2025-09-12T18:15:00Z"
}
```

### 📧 Implementación Asíncrona Requerida

**Este es el corazón del ejercicio**: Implementar procesamiento **ASÍNCRONO** usando las herramientas de Spring que hemos estudiado.

**Flujo requerido**:

1. **Controller** recibe la petición y retorna inmediatamente 202 Accepted
2. **Evento** `ReportRequestedEvent` se publica con `ApplicationEventPublisher`
3. **Listener** con `@EventListener` y `@Async` procesa en background:
   - Calcula agregados de ventas
   - Consulta GitHub Models API
   - Genera el resumen
   - Envía el email
4. **Email** llega al destinatario con el resumen

**Ejemplo de implementación**:
```java
// En el Service
@Async
@EventListener
public void handleReportRequest(ReportRequestedEvent event) {
    // 1. Calcular agregados
    // 2. Llamar a GitHub Models
    // 3. Enviar email
    // 4. Opcionalmente, actualizar status en BD
}
```

### 4. Gestión de Usuarios (Solo CENTRAL)

| Método | Endpoint | Descripción | Roles Permitidos |
|--------|----------|-------------|-----------------|
| GET | `/users` | Listar todos los usuarios | CENTRAL |
| GET | `/users/{id}` | Ver detalle de usuario | CENTRAL |
| DELETE | `/users/{id}` | Eliminar usuario | CENTRAL |

### 5. Requerimiento de Testing Unitario

**OBLIGATORIO**: Implementar tests unitarios para el **servicio de cálculo de agregados de ventas** (SalesAggregationService o similar).

### Tests Requeridos

Debes implementar **mínimo 5 test cases** que cubran:

1. **Test de agregados con datos válidos**: Verificar que se calculen correctamente `totalUnits`, `totalRevenue`, `topSku` y `topBranch` con un dataset conocido
2. **Test con lista vacía**: Verificar comportamiento cuando no hay ventas en el rango de fechas
3. **Test de filtrado por sucursal**: Verificar que solo considere ventas de la sucursal especificada (para usuarios BRANCH)
4. **Test de filtrado por fechas**: Verificar que solo considere ventas dentro del rango de fechas especificado
5. **Test de cálculo de SKU top**: Verificar que identifique correctamente el SKU más vendido cuando hay empates

### Ejemplo de Test Esperado

```java
@ExtendWith(MockitoExtension.class)
class SalesAggregationServiceTest {

    @Mock
    private SalesRepository salesRepository;

    @InjectMocks
    private SalesAggregationService salesAggregationService;

    @Test
    void shouldCalculateCorrectAggregatesWithValidData() {
        // Given
        List<Sale> mockSales = List.of(
            createSale("OREO_CLASSIC", 10, 1.99, "Miraflores"),
            createSale("OREO_DOUBLE", 5, 2.49, "San Isidro"),
            createSale("OREO_CLASSIC", 15, 1.99, "Miraflores")
        );
        when(salesRepository.findByDateRange(any(), any())).thenReturn(mockSales);

        // When
        SalesAggregates result = salesAggregationService.calculateAggregates(
            LocalDate.now().minusDays(7), LocalDate.now(), null
        );

        // Then
        assertThat(result.getTotalUnits()).isEqualTo(30);
        assertThat(result.getTotalRevenue()).isEqualTo(42.43);
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
    }

    // ... más tests
}
```

## 🎯 RETO EXTRA: Carta Mágica de Puntos Bonus 🪄

**¡Para los valientes que quieran puntos extra!** 🏆

### El Desafío Premium

Ya estás enviando resúmenes por email de manera asíncrona... ¿pero qué tal si los gerentes quieren algo más profesional? 📊📄

**El reto**: En lugar de enviar un email con texto plano, envía un **email HTML profesional** con:
- El resumen formateado elegantemente
- **Gráficos embebidos** (bar charts, pie charts)
- **PDF adjunto** con el reporte completo

### Endpoints Bonus

| Método | Endpoint | Descripción | Roles Permitidos |
|--------|----------|-------------|-----------------|
| POST | `/sales/summary/weekly/premium` | Solicitar reporte premium asíncrono | CENTRAL, BRANCH |

**Request**:
```json
{
  "from": "2025-09-01",
  "to": "2025-09-07",
  "branch": "Miraflores",
  "emailTo": "gerente@oreo.com",
  "format": "PREMIUM",
  "includeCharts": true,
  "attachPdf": true
}
```

**Response inmediata** (202 Accepted):
```json
{
  "requestId": "req_premium_01K...",
  "status": "PROCESSING",
  "message": "Su reporte premium está siendo generado. Incluirá gráficos y PDF adjunto.",
  "estimatedTime": "60-90 segundos",
  "features": ["HTML_FORMAT", "CHARTS", "PDF_ATTACHMENT"]
}
```

### Pistas para el Email Premium 🕵️‍♂️

- **Pista #1**: Para gráficos en emails, genera URLs de imágenes con servicios como **QuickChart.io** e insértalas como `<img src="...">`
- **Pista #2**: El LLM puede generar configuraciones de Chart.js que luego conviertes a URLs de QuickChart
- **Pista #3**: Para el PDF, considera **iText** o **Apache PDFBox** en Java
- **Pista #4**: Spring Boot Mail soporta HTML y attachments nativamente
- **Pista #5**: Todo esto también debe ser asíncrono - ¡más razón para usar eventos!

### Ejemplo de Email HTML (simplificado)
```html
<!DOCTYPE html>
<html lang="es">
<head>
    <style>
        .header { background: #6B46C1; color: white; padding: 20px; }
        .metric { display: inline-block; margin: 10px; padding: 15px; background: #f0f0f0; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🍪 Reporte Semanal Oreo</h1>
    </div>
    <div class="content">
        <p>Esta semana vendimos <strong>1,250 unidades</strong>...</p>
        <div class="metric">
            <h3>Total Revenue</h3>
            <p>$4,800.50</p>
        </div>
        <img src="https://quickchart.io/chart?c={type:'bar',data:{...}}" alt="Gráfico de ventas" />
    </div>
</body>
</html>
```

### Criterios de Evaluación del Reto

- **+3 puntos**: Email HTML con formato profesional
- **+5 puntos**: Incluir al menos un gráfico embebido en el email
- **+10 puntos**: Email HTML + múltiples gráficos + PDF adjunto con formato profesional

**Nota**: Este reto es OPCIONAL y los puntos obtenidos se sumarán a su **Hackathon 0**. Los equipos que lo intenten y fallen no serán penalizados. ¡Es puro upside! 🚀

## Integración con GitHub Models

### Documentación y Setup

Para usar GitHub Models en tu proyecto, necesitarás:

1. **Documentación oficial**: [GitHub Models REST API](https://docs.github.com/en/github-models/use-github-models/prototyping-with-ai-models#experimenting-with-ai-models-using-the-api)
2. **Token de acceso**: Crear un Personal Access Token con permisos de `model` en tu cuenta de GitHub
3. **Modelo recomendado para esta hackaton**: `OpenAI gpt-5-mini`

### Proceso Requerido

1. **Calcular agregados** de las ventas:
   - totalUnits
   - totalRevenue
   - topSku (el más vendido por unidades)
   - topBranch (sucursal con más ventas)

2. **Construir prompt** para el LLM:
```json
{
  "model": "${MODEL_ID}",
  "messages": [
    {"role": "system", "content": "Eres un analista que escribe resúmenes breves y claros para emails corporativos."},
    {"role": "user", "content": "Con estos datos: totalUnits=1250, totalRevenue=4800.50, topSku=OREO_DOUBLE, topBranch=Miraflores. Devuelve un resumen ≤120 palabras para enviar por email."}
  ],
  "max_tokens": 200
}
```

3. **Validaciones del resumen**:
   - Máximo 120 palabras
   - Debe mencionar al menos uno: unidades totales, SKU top, sucursal top, o total recaudado
   - En español, claro y sin alucinaciones

4. **Enviar por email** (de manera asíncrona):
   - Subject: "Reporte Semanal Oreo - [fecha_desde] a [fecha_hasta]"
   - Body: El summaryText generado + los aggregates principales

## Manejo de Errores

Formato estándar para todos los errores:
```json
{
  "error": "BAD_REQUEST",
  "message": "Detalle claro del problema",
  "timestamp": "2025-09-12T18:10:00Z",
  "path": "/sales"
}
```

Códigos HTTP esperados:
- 201: Recurso creado
- 202: Accepted (para procesamiento asíncrono)
- 200: OK
- 204: Sin contenido (cuando no hay ventas en el rango)
- 400: Validación fallida
- 401: No autenticado
- 403: Sin permisos (intentando acceder a datos de otra sucursal)
- 404: Recurso no encontrado
- 409: Conflicto (username/email ya existe)
- 503: Servicio no disponible (LLM caído o servicio de email no disponible)

## Validación con Postman Flow

La colección ejecutará esta secuencia:

1. **Register CENTRAL** → Assert 201, guardar userId
2. **Login CENTRAL** → Assert 200, guardar {{centralToken}}
3. **Register BRANCH (Miraflores)** → Assert 201
4. **Login BRANCH** → Assert 200, guardar {{branchToken}}
5. **Crear 5 ventas (con CENTRAL)** → Assert 201 cada una (diferentes sucursales)
6. **Listar todas las ventas (con CENTRAL)** → Assert 200, lista con todas
7. **Listar ventas (con BRANCH)** → Assert 200, solo ventas de Miraflores
8. **Solicitar resumen asíncrono (con BRANCH)** → Assert 202, requestId presente
9. **Intentar crear venta otra sucursal (con BRANCH)** → Assert 403 Forbidden
10. **Eliminar venta (con CENTRAL)** → Assert 204

### Datos de Prueba (Seeds)
```json
[
  {"sku": "OREO_CLASSIC_12", "units": 25, "price": 1.99, "branch": "Miraflores", "soldAt": "2025-09-01T10:30:00Z"},
  {"sku": "OREO_DOUBLE", "units": 40, "price": 2.49, "branch": "Miraflores", "soldAt": "2025-09-02T15:10:00Z"},
  {"sku": "OREO_THINS", "units": 32, "price": 2.19, "branch": "San Isidro", "soldAt": "2025-09-03T11:05:00Z"},
  {"sku": "OREO_DOUBLE", "units": 55, "price": 2.49, "branch": "San Isidro", "soldAt": "2025-09-04T18:50:00Z"},
  {"sku": "OREO_CLASSIC_12", "units": 20, "price": 1.99, "branch": "Miraflores", "soldAt": "2025-09-05T09:40:00Z"}
]
```

## Entregables

1. **Código fuente** completo en un repositorio público de GitHub
2. **Postman Collection** (archivo .json) en el root del repositorio
3. **README.md** con:
   - **Información del equipo**: Nombres completos y códigos UTEC de todos los integrantes
   - Instrucciones para ejecutar el proyecto
   - Instrucciones para correr el Postman workflow 
   - Explicación de la implementación asíncrona
   - (Si intentaste el reto) Documentación del endpoint premium
4. **Variables de entorno**: Entregar por Canvas en formato texto las variables necesarias para ejecutar el proyecto

## Criterios de Evaluación

**Sistema de Evaluación Todo o Nada:**
- ✅ **20 puntos**: Si completan todas las funcionalidades principales:
  - Autenticación JWT con roles
  - CRUD de ventas con permisos por sucursal
  - Resumen asíncrono con email
  - Testing unitario del servicio de agregados (mínimo 5 tests)
  - Postman Collection funcional
- ❌ **0 puntos**: Si no completan alguna de las funcionalidades principales

**El proyecto debe funcionar completamente end-to-end** para obtener los puntos. No hay evaluación parcial.

## Observaciones Adicionales

- **CRÍTICO**: El procesamiento del resumen DEBE ser asíncrono usando `@Async` y eventos
- Habilita async en tu aplicación con `@EnableAsync`
- El prompt al LLM debe ser **corto y explícito** con los números agregados
- Si usas H2, activa la consola en modo dev para debugging
- **NUNCA** subas tokens o secretos al repositorio (especialmente passwords de email)
- El resumen debe reflejar los datos reales (no inventar información)
- Maneja las fallas del LLM y del servicio de email con 503 y mensaje claro
- Los usuarios BRANCH solo ven/modifican datos de su sucursal asignada
- Para testing local de emails, considera usar **MailDev** o **Mailtrap**
- **Recuerden**: La comunicación del equipo es más importante que el código individual

¡Que la galleta esté de tu lado! 🍪✨

**Ustedes pueden con esto. Confíen en sus habilidades, comuníquense, y dividan el trabajo inteligentemente. Este proyecto puede ser la estrella de su portafolio. ¡A por ello!** 💪🚀

Con mucho cariño desde California,

**Gabriel Romero**
❤️

---

# Hackathon #1: Oreo Insight Factory — Resumen de entrega

Este README contiene la información mínima requerida para entregar y validar el proyecto: datos del equipo, cómo ejecutar la aplicación, cómo ejecutar el Postman workflow, explicación de la implementación asíncrona y documentación del endpoint premium (si se intentó).

---

## 1) Información del equipo

> Sustituir los siguientes placeholders por los nombres reales y códigos UTEC de cada integrante.

- Abigail — UTEC: <codigo_abigail>
- Eduardo — UTEC: <codigo_eduardo>
- Zaleth — UTEC: <codigo_zaleth>

> Nota: Reemplazar inmediatamente por la lista real antes de entregar.

---

## 2) Instrucciones para ejecutar el proyecto (local)

Requisitos previos:
- Java 21+
- Maven 3.8+
- Variables de entorno (ver sección siguiente)
- (Opcional para emails) Cuenta SMTP y credenciales de app password

Variables de entorno requeridas (ejemplo):

```
GITHUB_TOKEN=<tu_token_de_GitHub>
GITHUB_MODELS_URL=<endpoint_REST_de_GitHub_Models>
MODEL_ID=<id_del_modelo_del_Marketplace>
JWT_SECRET=<clave_para_firmar_JWT>
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<tu_email@gmail.com>
MAIL_PASSWORD=<app_password>
# Si usas PostgreSQL:
DB_URL=<jdbc_url>
DB_USER=<usuario_db>
DB_PASS=<password_db>
```

Cómo compilar y ejecutar (línea de comandos, Windows CMD / PowerShell):

1) Compilar y descargar dependencias:

```bash
mvn -U clean install -DskipTests
```

2) Ejecutar la aplicación desde Maven:

```bash
mvn spring-boot:run
```

O bien, generar el JAR y ejecutarlo:

```bash
mvn -U clean package -DskipTests
java -jar target/hack1-0.0.1-SNAPSHOT.jar
```

3) Acceder a la API:
- Base URL por defecto: http://localhost:8080
- Endpoints principales:
  - /auth/register
  - /auth/login
  - /sales
  - /sales/summary/weekly
  - /sales/summary/weekly/premium (si fue implementado)

Consejos para desarrolladores:
- Importar el proyecto como proyecto Maven en IntelliJ/Eclipse
- Habilitar "Annotation Processing" si usas Lombok
- Desactivar "Work offline" en Maven si tienes problemas de descarga

---

## 3) Instrucciones para correr el Postman workflow

Se espera que exista una colección de Postman en el root del proyecto llamada `postman_collection.json` y, opcionalmente, un environment `postman_environment.json`.

Pasos manuales en Postman GUI:
1. Importar `postman_collection.json` y `postman_environment.json` en Postman.
2. Ajustar variables del environment (ej.: baseUrl, JWT_SECRET local, emails de prueba).
3. Usar Collection Runner o Postman Flows para ejecutar la secuencia.

Ejemplo con Newman (línea de comandos):

```powershell
# Instalar newman si no lo tienes (Node.js requerido):
npm install -g newman
# Ejecutar colección
newman run postman_collection.json -e postman_environment.json
```

Secuencia esperada (la colección ya debería incluir aserciones):
1. Register CENTRAL → assert 201
2. Login CENTRAL → assert 200, guardar token
3. Register BRANCH → assert 201
4. Login BRANCH → assert 200
5. Crear ventas (varias) → assert 201
6. Listar ventas con CENTRAL → assert 200 (todas)
7. Listar ventas con BRANCH → assert 200 (solo su branch)
8. Solicitar resumen asíncrono → assert 202 y requestId
9. Intentar crear venta para otra sucursal con BRANCH → assert 403
10. Eliminar venta con CENTRAL → assert 204

Si la colección no está presente, crearla siguiendo los endpoints y ejemplos definidos en el enunciado del hackathon.

---

## 4) Explicación de la implementación asíncrona

Resumen técnico del flujo asíncrono implementado en el proyecto:

- Punto de entrada: el `Controller` que recibe la petición de generación de resumen (`/sales/summary/weekly` o la variante premium).
- Respuesta inmediata: el controller publica un evento y retorna `202 Accepted` con un `requestId` y estado `PROCESSING`.
- Evento: `ReportRequestedEvent` (clase de dominio) que encapsula parámetros de la petición (from, to, branch, emailTo, requestId, opciones premium).
- Publicador de eventos: `ApplicationEventPublisher` utilizado por el `Controller` o por un Service para publicar `ReportRequestedEvent`.
- Listener asíncrono: una clase con un método anotado `@EventListener` y `@Async` procesa el evento en background.
  - Ejemplo:
    - `@Async
      @EventListener
      public void handleReportRequest(ReportRequestedEvent event) { ... }`
- En el listener se realizan los pasos:
  1. Calcular agregados de ventas desde `SaleRepository` (totalUnits, totalRevenue, topSku, topBranch).
  2. Construir prompt y llamar a GitHub Models API (HTTP client con `GITHUB_TOKEN` y `GITHUB_MODELS_URL`).
  3. Validar el summary (máx. 120 palabras, en español, sin alucinaciones) — si falla, registrar error y notificar por email con error.
  4. Enviar email al `emailTo` con el summary y, si aplica, incluir gráficos o adjuntos (premium).
  5. (Opcional) Persistir estado del request en BD (ej.: table `report_requests`) para seguimiento.

Configuración necesaria en el código:
- `@EnableAsync` en la clase principal o configuración de Spring.
- Definir un `TaskExecutor` (bean) si se requiere un pool específico y límites de concurrencia.
- Habilitar `ApplicationEventPublisher` (inyectable automáticamente).

Manejo de errores y resiliencia:
- Capturar excepciones del LLM o del envío de email y marcar el request como `FAILED` (o enviar 503 en caso de acceso síncrono requerirlo).
- Reintentos controlados o circuit-breaker (opcional) para llamadas a la API externa.

Pruebas y debugging:
- Para testing de emails, usar Mailtrap, MailDev o un servidor SMTP de pruebas.
- Para probar LLM localmente, inyectar un `GitHubModelsClient` falso / bean mock que devuelva textos controlados.

---

## 5) Documentación del endpoint PREMIUM (si se intentó)

Endpoint: `POST /sales/summary/weekly/premium`

Descripción: Genera un reporte premium asíncrono que incluye:
- Resumen en texto (texto corto)
- Email HTML formateado
- Gráficos embebidos (p. ej. QuickChart) y/o PDF adjunto

Request body (JSON):

```json
{
  "from": "2025-09-01",
  "to": "2025-09-07",
  "branch": "Miraflores",
  "emailTo": "gerente@oreo.com",
  "format": "PREMIUM",
  "includeCharts": true,
  "attachPdf": true
}
```

Respuesta inmediata (202 Accepted):

```json
{
  "requestId": "req_premium_01K...",
  "status": "PROCESSING",
  "message": "Su reporte premium está siendo generado. Incluirá gráficos y PDF adjunto.",
  "estimatedTime": "60-90 segundos",
  "features": ["HTML_FORMAT", "CHARTS", "PDF_ATTACHMENT"]
}
```

Comportamiento en background (listener):
1. Calcular agregados y generar los datos para gráficos.
2. Generar URLs de imágenes de los gráficos (por ejemplo QuickChart) o generar imágenes locales y adjuntarlas.
3. Construir email HTML con layout profesional e incrustar imágenes usando `<img src="...">`.
4. Generar PDF (p. ej. con Apache PDFBox o iText) si `attachPdf=true` y adjuntarlo.
5. Enviar correo con JavaMailSender y marcar request como `COMPLETED` o `FAILED`.

Notas de implementación:
- Para generar gráficos se recomienda QuickChart.io (fácil integración por URL). Para imágenes locales, subir a un storage accesible o adjuntar como inline attachments.
- Para PDF se puede usar Apache PDFBox o iText; asegurar la licencia si se publica.

---

## 6) Checklist de entrega (para el equipo)

- [ ] Reemplazar nombres y códigos UTEC en la sección de Equipo
- [ ] Verificar que las variables de entorno estén definidas en la máquina de test
- [ ] Asegurarse de que la colección de Postman `postman_collection.json` esté en el root del proyecto
- [ ] Ejecutar `mvn -U clean install -DskipTests` y `mvn spring-boot:run` y validar endpoints básicos
- [ ] Ejecutar Postman flow / Newman y confirmar todas las aserciones
- [ ] Confirmar envío de email (usar Mailtrap o MailDev en pruebas)

---

Si quieres, puedo:
- Reemplazar los placeholders del equipo si me das la lista de nombres y códigos UTEC.
- Generar una `postman_collection.json` básica basada en los endpoints del proyecto.
- Añadir un `postman_environment.json` con variables útiles (baseUrl, emails de prueba, etc.).

Indícame qué prefieres que haga ahora.
