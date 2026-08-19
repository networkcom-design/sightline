# Sightline

Auditoría de presencia digital para comercios y pymes. Entra un negocio, sale un
diagnóstico con puntaje, un plan de acción priorizado y un presupuesto.

Es la herramienta interna de una agencia de publicidad. Antes de cada reunión de
venta había que revisar a mano cómo se ve el negocio del prospecto en internet:
la ficha de Google, el Instagram, el sitio, el WhatsApp. Dos horas de trabajo que
se repetían idénticas en cada visita. Sightline las convierte en unos minutos y
termina en algo que se le puede mandar al cliente.

Este repositorio es la API. El frontend está en
[sightline-front](https://github.com/networkcom-design/sightline-front).

---

## Qué hace

**Mide 43 señales** repartidas en siete dimensiones —ficha de Google, Instagram,
sitio web, reputación, WhatsApp Business, consistencia de datos y contenido— con
pesos distintos: la ficha de Google vale 25 y el contenido 7, porque no cuestan
lo mismo en clientes perdidos.

**Analiza el sitio de verdad.** Lo descarga y mide tiempo de carga, certificado,
adaptación a celular, título, descripción, encabezados y si hay un canal de
contacto real. Nueve señales resueltas sin preguntarle nada a nadie.

**Delega el resto a la IA.** El auditor pega el texto del perfil de Instagram y
de la ficha de Google, y el modelo dictamina 27 señales más citando la evidencia
en la que se basó. Quedan siete preguntas para responder a mano: las que nadie
puede saber desde afuera, como si el comercio tiene mensaje de bienvenida
configurado en WhatsApp.

**Arma el presupuesto.** Cada servicio del catálogo declara qué señales resuelve,
así que los hallazgos se convierten solos en una propuesta con precio y plazo.

---

## Las decisiones que importan

### La IA junta evidencia; el motor calcula

El puntaje lo produce un motor determinístico con reglas fijas. La IA lee la
evidencia y dice si cada señal se cumple, con su fundamento, pero **nunca toca el
número**.

Es lo que permite que dos auditorías del mismo comercio den el mismo puntaje
aunque el modelo redacte distinto, y que cuando el dueño pregunte de dónde salió
el 42 haya una respuesta que no sea "lo dijo la IA".

### Ante la duda, no inventa

Si el modelo se cae, responde cualquier cosa o se acaba la cuota, esas señales
quedan sin responder y pasan al cuestionario manual. Nunca se completa con una
respuesta plausible: es preferible que el auditor conteste siete preguntas más a
que el informe afirme algo que nadie verificó.

Cuando el análisis queda corto, la aplicación lo dice —con los segundos de espera
que informa el proveedor, si el motivo fue la cuota—. Quedarse sin cuota y que la
IA no encuentre nada se ven idénticos desde la pantalla si nadie lo explica.

### El puntaje provisional no se comparte

El motor solo puntúa lo que se le contestó, así que una auditoría a medias da un
número inflado. Mientras falte responder algo, el informe queda marcado como
provisional y **el enlace público se niega a abrirlo**. Mandarle a un prospecto un
diagnóstico que dice que está mejor de lo que está es peor que no mandarle nada.

### Dos proyecciones, no una

La propuesta muestra a cuánto llega el comercio **al entregar** el trabajo de pago
único, y a cuánto **sosteniéndolo** con el abono mensual. Publicar seguido y juntar
reseñas no se entregan una vez: se caen si se corta el trabajo. Separarlo es más
honesto y además es el argumento de por qué el abono no es opcional.

### Descargar sitios ajenos es una puerta abierta

La API baja el sitio que le indique el usuario, así que alguien podría pasarle
`http://localhost:8080/actuator` o una dirección interna de la red y usar el
servidor de intermediario. Antes de conectar se resuelve el nombre a IP y se
rechaza todo lo que apunte a la propia máquina o a rangos privados.

### Los teléfonos argentinos

Para abrir WhatsApp hace falta el número en un formato exacto, y la gente lo
anota de seis maneras distintas: `3624-556677`, `03624 15-556677`,
`+54 9 3624 556677`. El normalizador resuelve las variantes y, si no puede
interpretarlo con confianza, **no abre nada**: abrir un chat con un número mal
armado manda el mensaje a un desconocido.

---

## Correrlo

Hace falta Java 21, Maven y Docker.

```bash
docker compose up -d
```

```bash
mvn spring-boot:run
```

Queda en `http://localhost:8080`. La base corre en el puerto 5433 para no chocar
con un PostgreSQL instalado en la máquina, y las tablas las crea Flyway sola en
el primer arranque.

Para que funcione el análisis con IA, definí la clave antes de levantar:

```bash
setx LUPA_GEMINI_API_KEY "tu-clave"
```

Sin clave arranca igual: las 27 señales que dictaminaría la IA pasan al
cuestionario manual.

### Tests

```bash
mvn test
```

Son 81 y no tocan la red ni la base: la IA se reemplaza por un proveedor falso y
la persistencia corre en memoria. Hay tres más, etiquetadas `red`, que salen a
internet de verdad y quedan fuera del build normal porque dependen de sitios
ajenos:

```bash
mvn test -Dgroups=red -Dexcluir.grupos=ninguno
```

---

## Stack

Spring Boot 3.4 sobre Java 21, PostgreSQL con Flyway, autenticación propia con
JWT y BCrypt, jsoup para analizar sitios y Gemini para el análisis de evidencia.

La estructura es `domain` / `application` / `infrastructure` / `web`. El dominio
no depende de nada de afuera: el motor de puntaje y el generador de propuestas
son funciones puras, sin base de datos ni framework, y por eso están cubiertos
por tests que corren en milisegundos.

---

## Desplegarlo

Ver [DESPLIEGUE.md](DESPLIEGUE.md). Son dos cuentas gratuitas y dos valores para
pegar; Render crea la base de datos y la conecta solo.
