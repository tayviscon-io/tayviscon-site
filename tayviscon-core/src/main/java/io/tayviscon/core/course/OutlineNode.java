package io.tayviscon.core.course;

import java.util.List;

/** Узел дерева оглавления курса. */
public record OutlineNode(String title, List<OutlineNode> children) {
  /** Копирует дочерние узлы, чтобы дерево оглавления оставалось неизменяемым. */
  public OutlineNode {
    children = children == null ? List.of() : List.copyOf(children);
  }

  /** Возвращает лист с заголовком {@code title} без дочерних узлов. */
  public static OutlineNode leaf(String title) {
    return new OutlineNode(title, List.of());
  }

  /** Превращает список заголовков в листья в том же порядке. */
  public static List<OutlineNode> leaves(List<String> titles) {
    return titles.stream().map(OutlineNode::leaf).toList();
  }
}
