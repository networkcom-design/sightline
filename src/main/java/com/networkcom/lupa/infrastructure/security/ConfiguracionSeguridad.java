package com.networkcom.lupa.infrastructure.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    private final FiltroAutenticacionJwt filtroJwt;
    private final PropiedadesLupa propiedades;

    public ConfiguracionSeguridad(FiltroAutenticacionJwt filtroJwt, PropiedadesLupa propiedades) {
        this.filtroJwt = filtroJwt;
        this.propiedades = propiedades;
    }

    /**
     * BCrypt con factor 12.
     *
     * El valor por defecto de Spring es 10; se sube a 12 porque hoy el hardware
     * hace que 10 sea barato de atacar por fuerza bruta, y el costo extra al
     * loguearse es de milisegundos.
     */
    @Bean
    public PasswordEncoder codificadorContrasenas() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain cadenaDeFiltros(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                // La API no usa cookies de sesion, asi que no hay superficie para
                // CSRF: el token viaja en un encabezado que el navegador no adjunta solo.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        // Cuando algo falla, Spring redespacha internamente a /error.
                        // Ese redespacho vuelve a pasar por esta cadena sin
                        // autenticacion, asi que sin esta linea un error 400 de
                        // validacion le llega al cliente convertido en 401 y no
                        // hay forma de saber que se rompio de verdad.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/registro", "/api/auth/login").permitAll()
                        // El informe del prospecto se abre sin cuenta: la unica
                        // credencial es el token del enlace, que ya es secreto.
                        .requestMatchers(HttpMethod.GET, "/api/publico/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                // Sin esto, una peticion sin token a un endpoint protegido devuelve
                // una redireccion al formulario de login de Spring en lugar de un 401.
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * El nombre del bean tiene que ser exactamente `corsConfigurationSource`.
     *
     * Spring Security lo busca por nombre y no por tipo: con cualquier otro
     * nombre no lo encuentra, no aplica CORS y el navegador recibe un 403 en el
     * preflight sin ninguna pista de por que. El metodo puede llamarse distinto,
     * pero entonces hay que declarar el nombre del bean explicitamente.
     */
    @Bean("corsConfigurationSource")
    public CorsConfigurationSource configuracionCors() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(propiedades.cors().origenes());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}
