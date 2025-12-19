package com.biblioteca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point do Spring Boot para a versão WEB do Sistema de Biblioteca.
 * 
 * Para executar:
 * - Versão WEB: mvn spring-boot:run (http://localhost:8080)
 * - Versão CONSOLE: java -jar target/sistema-biblioteca-*-jar-with-dependencies.jar
 * 
 * Ambas as versões compartilham:
 * - Mesmo banco de dados MySQL
 * - Mesmas entidades (model/)
 * - Mesmos services (service/)
 * - Mesmas integrações (Stripe, PDF, Email)
 */
@SpringBootApplication
@EntityScan("com.biblioteca.model")
@EnableJpaRepositories("com.biblioteca.repository")
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        // Configurar properties do sistema ANTES de inicializar Spring
        configureSystemProperties();
        
        // Iniciar aplicação Spring Boot
        SpringApplication.run(Application.class, args);
        
        System.out.println("=== SISTEMA BIBLIOTECA WEB ===");
        System.out.println("Aplicação iniciada em: http://localhost:8080");
        System.out.println("Para versão console, execute: java -jar target/*.jar");
        System.out.println("===============================");
    }
    
    /**
     * Configurações específicas do sistema - SEM FRONTEND BUILD
     */
    private static void configureSystemProperties() {
        // Configurações gerais da JVM
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("user.timezone", "America/Sao_Paulo");
        
        System.out.println("🚀 Sistema configurado");
    }
} 