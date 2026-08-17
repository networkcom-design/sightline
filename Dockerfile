# Compilacion en una etapa aparte para que la imagen final no cargue con Maven
# ni con el codigo fuente: queda solo el JRE y el jar.
FROM maven:3.9-eclipse-temurin-21 AS compilacion
WORKDIR /construccion

# Primero solo el pom: si no cambian las dependencias, Docker reutiliza esta
# capa y no vuelve a bajar medio Maven Central en cada despliegue.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# Las pruebas ya corrieron antes de subir; repetirlas aca solo alarga el
# despliegue, y las de red dependen de sitios ajenos que pueden estar caidos.
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario sin privilegios: si alguien logra ejecutar algo dentro del contenedor,
# que no lo haga como root.
RUN addgroup -S lupa && adduser -S lupa -G lupa
USER lupa

COPY --from=compilacion /construccion/target/*.jar app.jar

EXPOSE 8080

# El contenedor no decide cuanta memoria usar: se lo dice el limite del plan.
# Sin esto, la JVM calcula el heap sobre la RAM de la maquina anfitriona y en
# un plan chico la matan por consumo apenas arranca.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
