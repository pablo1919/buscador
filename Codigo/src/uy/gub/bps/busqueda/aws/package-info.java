/**
 * Paquete destinado a la interacción con los webhook disponibilizados sobre AWS para los servicios de búsqueda del sitio web de BPS
 * Incluye seguridad, consumo de API rest y envío de datos
 * 
 * 
 * # Cliente BPS API para Java 1.8 / Tomcat 8

Cliente combinado para las API de indexado, administracion, historial y busqueda. Las respuestas JSON se entregan como String dentro de ApiResponse.

## HMAC
No se calcula firma HMAC y no se inserta `X-Signature`. La URL base debe apuntar al Datapower como responsable de la firma.

## Endpoints incluidos
- GET `/salud`, GET `/openapi.json`
- GET `/api/v1/buscar`, GET `/api/v1/predictiva`
- POST `/api/v1/indexar`, GET estado/items, GET `/api/v1/dlq`
- GET historial por dia y contenido
- GET/PUT pesos, GET/PUT/DELETE glosario, GET/PUT/DELETE destacados

 * 
 */
package uy.gub.bps.busqueda.aws;