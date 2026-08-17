package com.networkcom.lupa.web;

import com.networkcom.lupa.application.auditoria.ServicioAuditorias;
import com.networkcom.lupa.domain.auditoria.AuditoriaNoEncontradaException;
import com.networkcom.lupa.domain.usuario.CredencialesInvalidasException;
import com.networkcom.lupa.domain.usuario.EmailYaRegistradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones del dominio a respuestas HTTP.
 *
 * Se usa ProblemDetail (RFC 7807), que es el formato estandar de errores y le
 * da al frontend una estructura fija en lugar de mensajes sueltos.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    @ExceptionHandler(EmailYaRegistradoException.class)
    public ProblemDetail emailDuplicado(EmailYaRegistradoException e) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problema.setTitle("Email ya registrado");
        problema.setDetail(e.getMessage());
        return problema;
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ProblemDetail credencialesInvalidas(CredencialesInvalidasException e) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problema.setTitle("Credenciales invalidas");
        problema.setDetail(e.getMessage());
        return problema;
    }

    @ExceptionHandler(AuditoriaNoEncontradaException.class)
    public ProblemDetail auditoriaNoEncontrada(AuditoriaNoEncontradaException e) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problema.setTitle("Auditoria no encontrada");
        problema.setDetail(e.getMessage());
        return problema;
    }

    @ExceptionHandler(ServicioAuditorias.SenalNoEditableException.class)
    public ProblemDetail senalNoEditable(ServicioAuditorias.SenalNoEditableException e) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problema.setTitle("Senal no editable");
        problema.setDetail(e.getMessage());
        return problema;
    }

    @ExceptionHandler(ServicioAuditorias.InformeIncompletoException.class)
    public ProblemDetail informeIncompleto(ServicioAuditorias.InformeIncompletoException e) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problema.setTitle("Informe incompleto");
        problema.setDetail(e.getMessage());
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();

        e.getBindingResult().getFieldErrors().forEach(error ->
                errores.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problema.setTitle("Datos invalidos");
        problema.setDetail("Revisa los campos marcados.");
        problema.setProperty("errores", errores);
        return problema;
    }
}
