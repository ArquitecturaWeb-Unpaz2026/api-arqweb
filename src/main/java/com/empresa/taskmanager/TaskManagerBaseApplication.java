package com.empresa.taskmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de Spring Boot. La anotación habilita el autoescaneo de
 * componentes para que Spring construya el controlador, servicio y repositorio.
 * En Arquitectura Web esto inicia el servidor y la aplicación del lado backend.
 */
@SpringBootApplication
public class TaskManagerBaseApplication {
    /** Arranca el contenedor de Spring y el servidor HTTP integrado (Tomcat). */
    public static void main(String[] args) {
        SpringApplication.run(TaskManagerBaseApplication.class, args);
    }
}
