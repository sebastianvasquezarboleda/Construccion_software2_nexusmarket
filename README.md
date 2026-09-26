# NexusMarket

NexusMarket es una aplicación Spring Boot con servicios de dominio, puertos de
salida y adaptadores de persistencia MongoDB para usuarios, compradores,
vendedores, bodegas, productos, inventario y pedidos.

## Pruebas

Desde `nexusmarket/nexusmarket`:

```text
.\mvnw.cmd test
```

Las pruebas unitarias no requieren MongoDB. La prueba de persistencia real es
optativa y usa una base separada, `nexusmarket_test`.

Para ejecutarla con una instancia local activa:

```powershell
.\mvnw.cmd '-Dmongodb.integration=true' '-Dtest=MongoPersistenceIntegrationTest' test
```

## Ejecutar con MongoDB

Por defecto, la aplicación se conecta a MongoDB local en `localhost:27017` y usa
la base `nexusmarket`. Para usar otra instancia, configura `MONGODB_URI` antes de
iniciar:

```powershell
$env:MONGODB_URI = "mongodb://localhost:27017/nexusmarket"
.\mvnw.cmd spring-boot:run
```

Para MongoDB Atlas, asigna a `MONGODB_URI` la cadena de conexión proporcionada
por Atlas; no la guardes en el repositorio.

El dominio (modelos, enumeraciones y puertos de salida) está en
`src/main/java/application/domain`. Los servicios de aplicación están en
`src/main/java/application/services`; los adaptadores de almacenamiento en
memoria y MongoDB están en `src/main/java/application/adapters/out`.