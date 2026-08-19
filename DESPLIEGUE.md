# Desplegar Sightline

Dos cuentas, las dos gratis y ninguna pide tarjeta: **Render** para el backend y
la base de datos, **Netlify** para el frontend.

No hay que tocar código ni armar ninguna cadena de conexión. Render crea la base
y la conecta al backend por su cuenta.

---

## Antes de empezar

Los dos repositorios ya están en GitHub:

- Backend: [networkcom-design/sightline](https://github.com/networkcom-design/sightline)
- Frontend: [networkcom-design/sightline-front](https://github.com/networkcom-design/sightline-front)

Render y Netlify despliegan cada vez que llega un commit a `main`, así que antes
de subir conviene verificar que no se cuele ningún secreto:

```bash
git ls-files | findstr /i "env key"
```

No tiene que devolver nada. Si devuelve algo, no subas: revisá el `.gitignore`
primero.

---

## 1. Backend y base de datos, en Render

1. Entrá a [render.com](https://render.com) y creá la cuenta.
2. **New → Blueprint** y conectá el repositorio del backend.
3. Render lee el `render.yaml` y te muestra lo que va a crear: la base
   `sightline-db` y el servicio `sightline-api`. Aceptá.
4. Te va a pedir **dos valores**, los únicos que no puede adivinar:

   | Variable | Qué poner |
   |---|---|
   | `LUPA_GEMINI_API_KEY` | Tu clave de Google AI Studio |
   | `LUPA_CORS_ORIGENES` | La dirección de Netlify. Todavía no la tenés: poné `https://sightline.netlify.app` y corregilo en el paso 3 |

   Todo lo demás —las cinco piezas de la conexión a la base y la clave de firma
   de los tokens— lo completa Render solo.

5. El primer despliegue tarda varios minutos porque compila la imagen desde
   cero. Cuando termine, abrí en el navegador:

   ```
   https://sightline-api.onrender.com/actuator/health
   ```

   Tiene que decir `{"status":"UP"}`.

No hace falta crear ninguna tabla: se crean solas la primera vez que arranca.

---

## 2. Frontend en Netlify

1. Entrá a [netlify.com](https://netlify.com) → **Add new site → Import an
   existing project**.
2. Conectá el repositorio del frontend. El `netlify.toml` ya dice cómo
   compilarlo.
3. En **Site settings → Environment variables**, agregá una sola:

   ```
   VITE_API_URL = https://sightline-api.onrender.com
   ```

   Sin barra al final.

4. Si querés que la dirección sea más linda, en **Site settings → Change site
   name** podés poner `sightline`.

---

## 3. Cerrar el círculo

Volvé a Render, a **Environment**, y corregí `LUPA_CORS_ORIGENES` con la
dirección real que te dio Netlify. Con `https://` adelante y sin barra al final.

Si no coincide exacto, el navegador bloquea todas las llamadas y la aplicación
carga pero no muestra ningún dato, sin decir por qué.

---

## 4. Que el backend no se duerma

**El plan gratuito de Render apaga el servicio a los 15 minutos sin tráfico y
tarda cerca de un minuto en despertar.** Si alguien abre el informe justo en ese
momento, ve una pantalla en blanco y se va.

1. Creá una cuenta gratis en [uptimerobot.com](https://uptimerobot.com).
2. Agregá un monitor **HTTP(s)** apuntando a:

   ```
   https://sightline-api.onrender.com/actuator/health
   ```

3. Intervalo: **5 minutos**.

Render no soporta oficialmente este truco y su recomendación es pagar el plan.
Funciona, pero no es una garantía.

---

## Cosas que conviene saber

**La base gratuita de Render vence a los 30 días.** Para la Cup no importa: se
evalúa el 23 de agosto y la base sigue viva hasta mediados de septiembre. Si el
proyecto continúa, se migra a Neon o se pasa al plan pago, y lo único que cambia
es agregar una variable `LUPA_DB_URL` con la dirección nueva.

**La cuota de Gemini es limitada.** Cuando se agota, la aplicación no se rompe:
las señales que iba a dictaminar la IA quedan para el cuestionario manual, y la
pantalla avisa cuántos segundos hay que esperar.

---

## Si algo falla

| Síntoma | Causa más probable |
|---|---|
| El despliegue falla al compilar | Falta el `Dockerfile` o el `render.yaml` en la raíz del repositorio |
| `/actuator/health` devuelve 503 | La base todavía se está creando. Esperá un minuto y reintentá |
| El servicio no levanta y dice "no open ports detected" | El perfil `prod` no se activó. Revisá que `SPRING_PROFILES_ACTIVE=prod` esté en Environment |
| El frontend carga pero no aparece ningún dato | `LUPA_CORS_ORIGENES` no coincide exacto con el dominio de Netlify |
| El enlace del informe da 404 | Falta la regla de redirección del `netlify.toml` |
| La primera visita tarda un minuto | El servicio estaba dormido. Revisá el monitor de UptimeRobot |
