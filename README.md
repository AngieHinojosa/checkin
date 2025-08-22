# Bsale Check-in API (Prueba técnica)

API en Java 21 + Spring Boot 3.3.  
Endpoints:
- `GET /health` → `{ "status": "ok" }`
- `GET /flights/{id}/passengers` → `{ code, data, errors }` (lectura desde MySQL remota de la prueba)

## Ejecutar local
Variables de entorno:
