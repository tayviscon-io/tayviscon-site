package io.tayviscon.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Точка входа Spring Boot для сайта Tayviscon. */
@SpringBootApplication
public class SiteApplication {

  /** Запускает встроенный сервлет-контейнер. */
  public static void main(String[] args) {
    SpringApplication.run(SiteApplication.class, args);
  }
}
