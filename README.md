# Users Management — Spring Boot, Arquitectura Hexagonal y DDD

Aplicación de gestión de usuarios construida con Java 17 y Spring Boot. La API REST es el punto de entrada activo. El código de la antigua CLI se conserva como adaptador inactivo y no posee un contenedor de dependencias independiente.

Spring es el único *composition root*: `Main` inicia el contexto y las dependencias se resuelven mediante configuración y component scanning de Spring.

## Verificación

`GET /health` es publico y responde HTTP 200 con los metadatos generados desde
`pom.xml` durante el build:

```json
{"status":"UP","name":"users-management","version":"2.2.0"}
```

Indica que la API responde; no comprueba MySQL ni el proveedor de correo.

```bash
./mvnw clean test
./mvnw clean package
```

En Windows se puede utilizar `mvnw.cmd`.

## Ejecución local (Guía 1)

Requisitos: JDK 17 o superior, MySQL 8.4 y Git. Maven no hace falta: el proyecto trae Maven Wrapper.

1. Crear la base de datos con `src/main/resources/schema.sql`. El script crea `crud_usuarios`, la
   tabla `users` y el administrador inicial (`admin@example.com` / `Admin1234!`).

   ```bash
   mysql -u root -p < src/main/resources/schema.sql
   ```

2. Copiar `.env.example` como `.env` y llenar los valores. `.env` está en `.gitignore` y no se sube
   al repositorio. Spring Boot lo lee desde el directorio de trabajo, así que el proyecto se ejecuta
   desde la raíz del repo.
3. Ejecutar:

   | Sistema | Comando |
   |---|---|
   | macOS / Linux | `./mvnw spring-boot:run` |
   | Windows | `mvnw.cmd spring-boot:run` |

4. Abrir Swagger UI en http://localhost:8080/swagger-ui/index.html.

### Abrir en un IDE

| IDE | Cómo abrirlo | Cómo ejecutarlo |
|---|---|---|
| IntelliJ IDEA | *Open* y elegir la carpeta del repo (detecta el `pom.xml`) | Configuración **Ejecutar-Aplicacion** (`.run/Main.run.xml`) |
| VS Code / Antigravity | *Open Folder* e instalar las extensiones recomendadas | *Run and Debug* → **Ejecutar-Aplicacion** (`.vscode/launch.json`) |
| NetBeans | *Open Project* y elegir la carpeta del repo | *Run Project* sobre la clase `com.jcaa.usersmanagement.Main` |

Las configuraciones de IntelliJ y VS Code fijan la raíz del repo como directorio de trabajo para que
se cargue `.env`. Antes de arrancar corren `mvnw process-resources`, que genera
`META-INF/build-info.properties`. El endpoint `/health` lo necesita, y cuando el IDE compila sin
pasar por Maven ese archivo no existe y la aplicación no arranca
(`Consider defining a bean of type 'BuildProperties'`).

En VS Code / Antigravity también quedan tareas en *Terminal → Run Task*: ejecutar con
`spring-boot:run` y correr las pruebas con cobertura (`verify`).

### Colección de peticiones

- `docs/http/users.http`: para REST Client (VS Code/Antigravity) y para el HTTP Client de IntelliJ.
- `docs/http/users-management.postman_collection.json`: para importar en Postman, Insomnia o Boomerang.

Las dos recorren el flujo completo: health, 401 sin token, login, listar, crear (queda `PENDING`),
consultar por id, login rechazado mientras está `PENDING`, activar, login aceptado, eliminar y
consultar el eliminado (404).

## Base de datos remota en Aiven (Guía 2)

1. Crear un servicio MySQL en https://console.aiven.io (plan Developer).
2. Crear la base y las tablas en Aiven con el mismo `schema.sql`. Los datos están en *Connection information*:

   ```bash
   mysql -h <host> -P <puerto> -u avnadmin -p --ssl-mode=REQUIRED < src/main/resources/schema.sql
   ```

3. En `.env` cambiar solo la conexión. El código no cambia:

   ```properties
   DB_HOST=<host>.aivencloud.com
   DB_PORT=<puerto>
   DB_USERNAME=avnadmin
   DB_PASSWORD=<clave de Aiven>
   DB_SSL_MODE=REQUIRED
   ```

   `REQUIRED` es obligatorio de nuestro lado. El servidor de Aiven tiene
   `require_secure_transport=OFF` y acepta también conexiones sin cifrar. Con `DISABLED`, la
   contraseña y los datos viajarían en texto plano por Internet.

### Medir el tiempo de respuesta

`scripts/medir-latencia.sh` inicia sesión y hace N llamadas a `GET /api/users`, que consulta la base
en cada llamada. Muestra el promedio, p50, p95 y el máximo:

```bash
scripts/medir-latencia.sh http://localhost:8080 50
```

| Base de datos | Promedio | p95 |
|---|---|---|
| MySQL local | 1,5 ms | 1,8 ms |
| Aiven (North America), desde Colombia | 107,4 ms | 179,8 ms |

## Integración y despliegue continuos (Guía 3)

```
PR o push a main ──► GitHub Actions (CI) ──► ¿verde? ──► Render construye la imagen Docker y despliega
                     compila · 194 pruebas              (autoDeployTrigger: checksPass)
                     cobertura · docker build
```

- **CI** (`.github/workflows/ci.yml`): en cada PR y en cada push a `main` corre `./mvnw verify` con Java 17,
  construye la imagen Docker y publica el reporte de cobertura de JaCoCo como artefacto.
- **CD** (`render.yaml`): Render despliega `main` automáticamente, pero solo cuando la CI de ese commit pasa.
  Un commit que rompe las pruebas no llega a producción.
- Región `virginia`: es la región de Render más cercana a la base de datos (Aiven, en New Jersey).

> En el plan gratis de Render están bloqueados los puertos SMTP 25, 465 y 587. Desde la guía 5 el
> correo sale por la API HTTPS de Brevo, que no usa esos puertos.

## Secretos locales alternativos (`.environments`)

Copiar `application-secrets.properties.example` como
`.environments/application-secrets.properties` y reemplazar los valores de ejemplo.
Este archivo contiene la conexion local a MySQL, la semilla JWT y las credenciales de
Brevo. La carpeta `.environments` esta excluida de Git y del contexto de Docker.

En produccion no se utiliza este archivo: los secretos deben configurarse mediante las
variables de entorno `DB_*`, `JWT_SECRET` y `BREVO_*` del proveedor de despliegue.

## Docker Compose local

Copiar `compose.env.example` como `.env`, reemplazar todos los valores y ejecutar:

```bash
docker compose up --build
```

Compose se detiene con un error si falta `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`,
`JWT_SECRET`, `BREVO_API_KEY` o `BREVO_FROM_ADDRESS`. El archivo `.env` esta excluido
de Git y del contexto de construccion Docker. Los puertos de MySQL y la API se publican
solo en `127.0.0.1`.

## Despliegue en Render

El archivo `render.yaml` define el servicio web, el build con Docker y el despliegue
automático de `main` cuando la CI del commit pasa (`checksPass`).

1. Crear un Blueprint en Render y seleccionar este repositorio.
2. Completar en el panel los secretos marcados como requeridos: `DB_HOST`,
   `DB_USERNAME`, `DB_PASSWORD`, `BREVO_API_KEY` y `BREVO_FROM_ADDRESS`.
3. Usar una instancia MySQL accesible desde Internet o desde la red privada de
   Render y ejecutar `src/main/resources/schema.sql` una vez para crear el esquema.
4. Desplegar el Blueprint. La API quedará disponible en el subdominio
   `onrender.com` asignado por Render y Swagger UI en `/swagger-ui.html`.

Las credenciales nunca deben guardarse en `application.properties` ni en
`render.yaml`. Para desarrollo local, deben proporcionarse como variables de entorno.

## Base de datos en TiDB Cloud (Guía 4)

El servicio de Render usa TiDB Cloud Starter (AWS N. Virginia, compatible con MySQL). Solo cambia la configuración:
`DB_HOST`, `DB_PORT=4000`, `DB_USERNAME`, `DB_PASSWORD` y `DB_SSL_MODE=VERIFY_IDENTITY`.

`VERIFY_IDENTITY` cifra la conexión y además comprueba el certificado y el nombre del servidor.

> `schema.sql` crea el administrador inicial con una clave pública. En una base nueva, cambia su correo y su clave
> **antes** de apuntar a ella un servicio publicado.

## Notificaciones por correo con Brevo (Guía 5)

Los correos transaccionales se envian con `POST /v3/smtp/email` de Brevo. La llamada
se ejecuta fuera del hilo HTTP en el executor `notificationExecutor` y dispone de
timeouts, reintentos para errores de red/408/429/5xx con backoff exponencial, circuit
breaker e idempotencia para evitar duplicados durante los reintentos.

Variables requeridas: `BREVO_API_KEY` y `BREVO_FROM_ADDRESS`. El remitente debe estar
verificado en Brevo. Los parametros de resiliencia `BREVO_CONNECT_TIMEOUT`,
`BREVO_READ_TIMEOUT`, `BREVO_RETRY_*` y `BREVO_CIRCUIT_*` son opcionales y tienen
valores predeterminados en `application.properties`.

## Pruebas y umbral de cobertura (Guía 5)

`./mvnw verify` corre las pruebas y el `check` de JaCoCo. La CI usa ese mismo comando, así que un cambio que
baje la cobertura **del 90 % de líneas o del 90 % de ramas** deja la CI en rojo y Render no lo despliega.

| Qué cubren | Dónde |
|---|---|
| Dominio, casos de uso y notificaciones | `src/test/java/.../domain`, `.../application` |
| Adaptador de Brevo: reintentos, circuit breaker y tiempos límite | `.../infrastructure/adapter/email` |
| API REST y manejo global de errores (400, 401, 404, 409, 500) | `.../infrastructure/entrypoint/rest` |
| Consola: menú, lectura de datos y los seis manejadores | `.../infrastructure/entrypoint/desktop/cli` |

Los mínimos se cambian con `jacoco.minimum.line` y `jacoco.minimum.branch` en el `pom.xml`. El reporte queda en
`target/site/jacoco/index.html`.
