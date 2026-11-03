# NexusMarket

NexusMarket es una API REST Spring Boot para un marketplace, organizada con
arquitectura hexagonal. Incluye gestión de usuarios y roles, vendedores,
productos, bodegas e inventario, pedidos, facturas internas, envíos,
devoluciones, reembolsos manuales y reportes administrativos. MongoDB es el
adaptador de persistencia de la aplicación.

## Pruebas

Desde `nexusmarket/nexusmarket`:

```text
.\mvnw.cmd test
```

Para llegar a esa carpeta desde la raíz del proyecto en PowerShell:

```powershell
Set-Location .\nexusmarket\nexusmarket
```

La suite normal no requiere MongoDB. Para ejecutar también la prueba de
persistencia real, con MongoDB en replica set activo, usa la base separada
`nexusmarket_test`:

```powershell
.\mvnw.cmd '-Dmongodb.integration=true' test
```

Para ejecutar solo esa prueba:

```powershell
.\mvnw.cmd '-Dmongodb.integration=true' '-Dtest=MongoPersistenceIntegrationTest' test
```

## Ejecutar con MongoDB

La aplicación necesita MongoDB configurado como replica set (`rs0`) para que
las transacciones mantengan sincronizados pedidos e inventario. En una
instalación local, inicia MongoDB con `--replSet rs0` e inicialízalo una vez
con `mongosh --eval "rs.initiate()"`. Configura
`MONGODB_URI` para apuntar a ese replica set; la URI local predeterminada usa
`localhost:27017`, la base `nexusmarket` y `replicaSet=rs0`. La API requiere
HTTP Basic y utiliza contraseñas BCrypt:

```powershell
$env:MONGODB_URI = "mongodb://localhost:27017/nexusmarket?replicaSet=rs0"
.\mvnw.cmd spring-boot:run
```

Para MongoDB Atlas, asigna a `MONGODB_URI` la cadena de conexión proporcionada
por Atlas; no la guardes en el repositorio.

La base debe contener datos compatibles con los modelos actuales. Los
identificadores de usuario se manejan como `Long`; documentos antiguos con IDs
de texto requieren migración antes de usar endpoints que listan usuarios.
Los productos existentes deben tener un precio asignado antes de permitir que
se agreguen a pedidos. Si hay perfiles de comprador/vendedor duplicados por
usuario, deben reconciliarse antes de que MongoDB cree los índices únicos.

## Autenticación inicial

Al iniciar la aplicación, se puede crear o reactivar un administrador inicial
configurando ambas variables de entorno. Mantén las credenciales fuera del
repositorio:

```powershell
$env:NEXUSMARKET_ADMIN_EMAIL = "admin@example.com"
$env:NEXUSMARKET_ADMIN_PASSWORD = "una-clave-segura-de-al-menos-12"
.\mvnw.cmd spring-boot:run
```

Si las variables no están configuradas, la aplicación arranca sin crear un
administrador. Los usuarios nuevos requieren una contraseña de al menos 12
caracteres. El endpoint `PUT /api/users/{id}/password` permite al administrador
restablecer contraseñas. Las respuestas API nunca incluyen el hash.

Todos los endpoints `/api` requieren autenticación. Los roles son `BUYER`,
`SELLER`, `LOGISTICS_OPERATOR`, `ADMINISTRATOR` y `SUPERVISOR`; además de las
reglas HTTP, las operaciones de compradores, pedidos, productos e inventario
comprueban la propiedad del recurso. Los usuarios bloqueados no pueden
autenticarse. Las operaciones que cambian datos requieren un token CSRF: el
cliente debe llamar a `GET /api/csrf` con autenticación y enviar el token
devuelto en el header `X-XSRF-TOKEN`, además de devolver la cookie `XSRF-TOKEN`.
La colección Postman obtiene y añade ambos automáticamente antes de cada request.

Los pedidos cancelados quedan en estado terminal `CANCELLED`. La reserva,
eliminación y cancelación de artículos ejecutan el cambio del pedido y del
inventario en una transacción MongoDB; si una parte falla, ambas se revierten.
Los pedidos sin pagar liberan automáticamente sus reservas después de 30 minutos
sin actividad; la revisión se ejecuta cada minuto. Configura
`NEXUSMARKET_CART_EXPIRATION` (duración ISO-8601, por ejemplo `PT45M`) y
`NEXUSMARKET_CART_EXPIRATION_SCAN_MS` si necesitas cambiar el plazo o la
frecuencia. Los pedidos existentes sin fecha de actualización reciben una
fecha inicial al arrancar y se mantienen durante el plazo completo.

Para probar la API sin afectar `nexusmarket`, usa el perfil `demo`, que guarda
los datos en `nexusmarket_demo`. Configura también las variables de
administrador descritas arriba para poder autenticarte:

```powershell
$env:MONGODB_DEMO_URI = "mongodb://localhost:27017/nexusmarket_demo?replicaSet=rs0"
$env:NEXUSMARKET_ADMIN_EMAIL = "admin@example.com"
$env:NEXUSMARKET_ADMIN_PASSWORD = "una-clave-segura-de-al-menos-12"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

El perfil demo no borra datos ni modifica la base predeterminada. Las variables
definidas en PowerShell solo aplican a esa ventana; no guardes contraseñas en
los archivos del proyecto.

## API REST y Postman

La aplicación expone la API en `/api`. Además de los módulos base, incluye
facturas internas por pedido pagado, estados de envío internos, solicitudes de
devolución con reposición de existencias y un resumen administrativo. El stock
dañado se registra separado del stock disponible. Los pedidos físicos reservan
existencias repartidas en varias bodegas de forma determinista (por ID de
inventario), y guardan esas asignaciones para devolverlas a las mismas bodegas
al quitar productos, cancelar o aprobar una devolución.
El precio se define en el catálogo, no en la solicitud del comprador; los
pedidos guardan el precio vigente al agregarse el producto y solo aceptan
productos publicados.
Las cantidades de devolución se reservan atómicamente por pedido y producto.
La aprobación guarda primero su transición de estado y repone el inventario de
forma idempotente, para que reintentar una operación no duplique las unidades.

Ejemplo de arranque:

```powershell
cd nexusmarket/nexusmarket
$env:MONGODB_URI = "mongodb://localhost:27017/nexusmarket?replicaSet=rs0"
.\mvnw.cmd spring-boot:run
```

Endpoint base:

```text
http://localhost:8080/api
```

La colección Postman contiene 62 solicitudes organizadas por módulo. Configura
`adminEmail` y `adminPassword` en las variables de colección antes de ejecutar.
La colección solicita automáticamente el token CSRF antes de cada request. Los
requests deben ejecutarse en el orden indicado; generan los identificadores que
usan los siguientes y cubren autenticación, una comprobación de acceso denegado,
inventario, pedidos, facturas, envíos, devoluciones y reportes. Las pruebas
MockMvc verifican permisos para varios roles, propiedad de cuentas y pedidos, y
que no se creen pedidos sin un perfil de comprador existente.
Las rutas de listado devuelven un objeto paginado con `content`, `page`,
`size`, `totalElements` y `totalPages`. Por defecto `page=0` y `size=50`; el
tamaño máximo es 100. Por ejemplo, `GET /api/orders?page=1&size=25` devuelve la
segunda página de pedidos.

La colección está en:

```text
nexusmarket/nexusmarket/postman/NexusMarket.postman_collection.json
```

Importa esa colección en Postman y utiliza `baseUrl` con el valor
`http://localhost:8080`.
La colección compartida deja `adminEmail` y `adminPassword` vacíos
intencionalmente: completa esos valores localmente en Postman con las mismas
credenciales configuradas al iniciar la API. No exportes ni compartas una
colección que contenga una contraseña.

## HTTPS detrás de un proxy

En despliegue, termina TLS en un proxy inverso (por ejemplo Nginx) y no publiques
el puerto interno de Spring Boot. El proxy debe sobrescribir los headers
`X-Forwarded-*` recibidos del cliente y el backend debe aceptar tráfico solo
desde el proxy o desde la red privada. La aplicación procesa esos headers para
reconocer HTTPS y establece la cookie CSRF como `Secure`, `SameSite=Lax` y
`Path=/api`.

Ejemplo mínimo de Nginx; configura certificados válidos y la redirección HTTP a
HTTPS según el entorno:

```nginx
server {
    listen 443 ssl;
    server_name api.example.com;

    ssl_certificate /etc/letsencrypt/live/api.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.example.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header Forwarded "";
    }
}
```

Configura `NEXUSMARKET_CORS_ALLOWED_ORIGINS` con el origen HTTPS exacto del
frontend. El navegador debe hacer las solicitudes con credenciales habilitadas,
obtener el token de `GET /api/csrf`, conservar la cookie y copiar `token` al
header `X-XSRF-TOKEN` en cada operación que modifica datos. Si el frontend y la
API están en sitios distintos, configura `NEXUSMARKET_CSRF_COOKIE_SAME_SITE=None`
(requiere HTTPS). Para pruebas locales en HTTP, el perfil `demo` desactiva el
atributo Secure; en otros perfiles se puede usar
`NEXUSMARKET_CSRF_COOKIE_SECURE=false` solo en desarrollo. Nunca lo desactives
en producción.

## Alcance y límites

- Las facturas son registros internos sin cálculo de impuestos ni integración
  con un proveedor fiscal o pasarela de pago.
- El reembolso es un registro manual de estado; no se transfiere dinero.
- El envío tiene un flujo interno (`CREATED`, `PACKED`, `DISPATCHED`,
  `DELIVERED`); no integra transportistas externos.
- Los productos digitales no reservan inventario físico y se finalizan al
  marcar el pedido como pagado; la entrega/descarga del archivo digital no está
  incluida.
- Las reservas físicas se distribuyen por ID de inventario ascendente; todavía
  no existe selección logística por distancia, costo o capacidad.
- La reserva de inventario usa control optimista en MongoDB y devuelve HTTP 409
  si otro proceso modifica el mismo inventario simultáneamente. El cliente debe
  recargar y reintentar.
- Solo pueden cancelarse pedidos sin pago (`CART` o `PENDING_PAYMENT`). Los
  pedidos pagados se completan mediante logística y posventa, evitando devolver
  al stock artículos que ya fueron despachados.
- Configure `NEXUSMARKET_CORS_ALLOWED_ORIGINS` con los orígenes exactos del
  frontend antes del despliegue. El valor local predeterminado permite
  `http://localhost:3000` y `http://localhost:5173`; no despliegue Basic Auth
  sin HTTPS.
- Mantenga CSRF habilitado al usar Basic Auth desde navegador. En producción,
  exponga Spring Boot solo detrás del proxy TLS confiable y configure el frontend
  para enviar cookie y header CSRF.

El dominio (modelos, enumeraciones y puertos de salida) está en
`src/main/java/application/domain`. Los servicios de aplicación están en
`src/main/java/application/services`; los adaptadores de almacenamiento en
memoria y MongoDB están en `src/main/java/application/adapters/out`.

Los puertos de entrada están en `application/domain/ports/in`; el controlador
REST depende de esas interfaces. Cada cambio de inventario registra el tipo de
movimiento, la cantidad, el saldo resultante y la fecha, y el historial se
persiste junto al inventario en MongoDB.
El inventario inicial positivo también queda registrado como movimiento `ENTRY`.