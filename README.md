# Bsale Check-in API 

API REST para simular el check‑in de pasajeros de Andes Airlines leyendo desde una base MySQL solo lectura y asignando asientos según reglas del desafío.

**Stack**: Java 21 · Spring Boot 3.3 · JDBC (JdbcTemplate) · MySQL · Maven

## 1) Requisitos 
- Java 21
- Maven 3.9+
- Acceso a MySQL (remoto del desafío o local)

## 2) Variables de entorno
La app viene preconfigurada para la BD compartida. En local puedes sobrescribir con variables de entorno:

$env:DB_HOST="mdb-test.c6vunyturrl6.us-west-1.rds.amazonaws.com"

$env:DB_PORT="3306"

$env:DB_NAME="airline"

$env:DB_USER="postulaciones"

$env:DB_PASS="post123456"

$env:PORT="8080"

## 3) Ejecutar en local 
**desde la raíz del proyecto**
mvn spring-boot:run

## 4) Health check
curl.exe -i "http://localhost:8080/status"

*Respuesta*: `{ "status": "ok" }`

## 5) Endpoint principal
# GET /flights/{id}/passengers
Ejemplo: curl.exe -i "http://localhost:8080/flights/1/passengers"

*Respuesta 200 (éxito)*

# Vuelo no encontrado(404)
Ejemplo: curl.exe -i "http://localhost:8080/flights/999999/passengers"

*Respuesta*: `{ "code": 404, "data": {} }`

# Error de conexión a BD (400)
`{ "code": 400, "errors": "could not connect to db" }`

# Error inesperado (500)
`{ "code": 500, "data": {}, "errors": "internal error" }`

## 5) Reglas de asignación de asientos
La asignación se realiza en memoria (sin escribir en BD) respetando:
- Menores junto a un adulto de la misma compra.
- Agrupar la compra (mantener asientos contiguos o cercanos cuando sea posible).
- No cruzar clases: cada pasajero se sienta en un asiento del seatTypeId que indica su boarding pass.
- Contemplar ocupados: si un boarding pass ya tiene seatId, ese asiento queda marcado como ocupado

Si no hay asientos contiguos suficientes, se degradan los intentos (parejas, luego asientos libres más cercanos por fila), siempre sin violar el tipo de asiento.

## 5) Arquitectura y decisiones
- JdbcTemplate + SQL nativo (simple, control de mapeo snake_case → camelCase).
- Conexión robusta: HikariCP con keep-alive, test query y timeouts (entorno compartido que corta por inactividad).
- Health check /status para despliegues PaaS.

## 6) Entorno público (Render)

**Base URL:** https://bsale-checkin-li86.onrender.com/ 

**Status (Healthcheck):** https://bsale-checkin-li86.onrender.com/status

  Respuesta: `{ "status": "ok" }`

**Vuelo de ejemplo (200):** https://bsale-checkin-li86.onrender.com/flights/1/passengers 

**Vuelo no encontrado (404):** https://bsale-check-inli86.onrender.com/flights/999999/passengers 
  
  Respuesta: `{ "code": 404, "data": {} }`

> Nota: si la app estuvo inactiva, la **primera llamada** puede tardar unos segundos por *cold start* del plan Free.
