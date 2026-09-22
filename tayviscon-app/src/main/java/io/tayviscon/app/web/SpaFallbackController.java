package io.tayviscon.app.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Проксирует пути Vue Router на {@code index.html}, не перехватывая хешированные ассеты. */
@Controller
public class SpaFallbackController {

  /**
   * Перенаправляет клиентские маршруты без расширения на оболочку Vue. Шаблон последнего
   * сегмента {@code [^.]*} оставляет хешированные ассеты Vite ({@code /assets/index-….js})
   * на обработчике ресурсов Boot. Шаблон {@code /**} в середине пути недопустим в Spring
   * PathPattern, поэтому глубины перечислены явно.
   */
  @GetMapping({
    "/",
    "/{path:^(?!api$)[^.]*}",
    "/{path:^(?!api$)[^.]*}/{file:[^.]*}",
    "/{path:^(?!api$)[^.]*}/{mid:[^.]*}/{file:[^.]*}"
  })
  public String forward() {
    return "forward:/index.html";
  }
}
