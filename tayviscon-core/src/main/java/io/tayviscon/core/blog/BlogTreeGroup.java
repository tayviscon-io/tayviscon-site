package io.tayviscon.core.blog;

import java.util.List;

/** Группа статей в дереве сайдбара (имя папки верхнего уровня). */
public record BlogTreeGroup(String name, List<BlogTreeItem> articles) {}
