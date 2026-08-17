# Desplegar Lupa

Tres servicios gratuitos: **Neon** para la base, **Render** para el backend y
**Netlify** para el frontend. Ninguno pide tarjeta.

El orden importa: la base primero, porque el backend la necesita para arrancar,
y el frontend último, porque necesita saber la dirección del backend.

---

## 1. Base de datos en Neon

1. Entrá a [neon.com](https://neon.com) y creá una cuenta.
2. Creá un proyecto. Elegí la región **AWS us-east-2 (Ohio)** o la más cercana a
   la que uses en Render: si la base y el backend quedan en continentes
   distintos, cada consulta se paga en latencia.
3. En el panel, buscá la cadena de conexión. Neon la da en formato psql:

   ```
   postgresql://usuario:contrasena@ep-algo-123.us-east-2.aws.neon.tech/lupa?sslmode=require
   ```

4. Hay que partirla en tres, porque Spring las pide por separado. De la cadena
   de arriba salen:

   | Variable | Valor |
   |---|---|
   | `LUPA_DB_URL` | `jdbc:postgresql://ep-algo-123.us-east-2.aws.neon.tech/lupa?sslmode=require` |
   | `LUPA_DB_USER` | `usuario` |
   | `LUPA_DB_PASSWORD` | `contrasena` |

   Fijate en tres cosas: se antepone `jdbc:`, se sacan el usuario y la
   contraseña del medio, y **se conserva `?sslmode=require`**. Sin eso Neon
   rechaza la conexión.

No hace falta crear ninguna tabla: Flyway corre las tres migraciones solo la
primera vez que arranca el backend.

---

## 2. Backend en Render

1. Subí este repositorio a GitHub (ver el final de este documento).
2. Entrá a [render.com](https://render.com), creá la cuenta y elegí
   **New → Blueprint**.
3. Conectá el repositorio. Render detecta el `render.yaml` y arma el servicio
   solo, con Docker.
4. Antes de desplegar te va a pedir las variables marcadas como secretas:

   | Variable | De dónde sale |
   |---|---|
   | `LUPA_DB_URL` | Del paso 1 |
   | `LUPA_DB_USER` | Del paso 1 |
   | `LUPA_DB_PASSWORD` | Del paso 1 |
   | `LUPA_GEMINI_API_KEY` | Tu clave de Google AI Studio |
   | `LUPA_CORS_ORIGENES` | La dirección de Netlify. Todavía no la tenés: poné `https://lupa.netlify.app` provisorio y corregilo en el paso 4 |

   `LUPA_JWT_SECRET` la genera Render sola. No la copies de tu máquina: la de
   desarrollo está en el repositorio como valor por defecto y no sirve para
   producción.

5. El primer despliegue tarda varios minutos porque compila la imagen desde
   cero. Cuando termine, probá que responda:

   ```
   https://lupa-api.onrender.com/actuator/health
   ```

   Tiene que devolver `{"status":"UP"}`. Si devuelve 503, el problema es la
   conexión a la base: revisá que la URL tenga `?sslmode=require`.

---

## 3. Frontend en Netlify

1. Entrá a [netlify.com](https://netlify.com) y elegí **Add new site → Import an
   existing project**.
2. Conectá el repositorio del frontend. El `netlify.toml` ya define el comando y
   la carpeta de publicación.
3. En **Site settings → Environment variables**, agregá:

   ```
   VITE_API_URL = https://lupa-api.onrender.com
   ```

   Sin barra al final. Esta variable se lee **al compilar**, no al ejecutar: si
   la cambiás después, hay que volver a desplegar para que tome efecto.

4. Cuando termine, anotá la dirección que te asignó Netlify y **volvé a Render**
   a corregir `LUPA_CORS_ORIGENES` con esa dirección exacta, incluido el
   `https://` y sin barra final. Si no coincide, el navegador bloquea todas las
   llamadas y la aplicación parece rota sin dar ninguna pista.

---

## 4. Que el backend no se duerma

**El plan gratuito de Render apaga el servicio a los 15 minutos sin tráfico y
tarda cerca de un minuto en despertar.** Si alguien abre el informe justo en ese
momento, ve una pantalla en blanco y se va.

La solución es un ping externo:

1. Creá una cuenta gratis en [uptimerobot.com](https://uptimerobot.com).
2. Agregá un monitor **HTTP(s)** apuntando a:

   ```
   https://lupa-api.onrender.com/actuator/health
   ```

3. Poné el intervalo en **5 minutos**.

Render no soporta oficialmente este truco y su recomendación es pagar el plan.
Funciona, pero no es una garantía: para algo que no se puede caer, hay que pagar.

---

## Subir el repositorio a GitHub

El repositorio ya está inicializado y con el primer commit hecho. Falta crearlo
en GitHub y subirlo:

```bash
gh repo create lupa-backend --private --source=. --push
```

Si no tenés la herramienta `gh`, creá el repositorio desde la web y después:

```bash
git remote add origin https://github.com/TU-USUARIO/lupa-backend.git
git push -u origin main
```

Lo mismo para `lupa-frontend`.

**Antes de subir, verificá que no se cuele ningún secreto:**

```bash
git ls-files | findstr /i "env key"
```

No tiene que devolver nada. La clave de Gemini vive en
`%USERPROFILE%\.lupa\gemini.key`, fuera del repositorio, y el `.gitignore`
excluye `.env` y `*.key`.

---

## Si algo falla

| Síntoma | Causa más probable |
|---|---|
| `/actuator/health` devuelve 503 | La base no conecta. Falta `?sslmode=require` en la URL. |
| El frontend carga pero ningún dato aparece | `LUPA_CORS_ORIGENES` no coincide exacto con el dominio de Netlify. |
| El enlace del informe da 404 | Falta la regla de redirección del `netlify.toml`. |
| El análisis con IA no responde nada | Se acabó la cuota de Gemini. La pantalla lo avisa con los segundos de espera. |
| La primera visita tarda un minuto | El servicio estaba dormido. Revisá que el monitor de UptimeRobot esté activo. |
