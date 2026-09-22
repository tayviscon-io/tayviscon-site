<script setup>
defineOptions({ name: 'CourseOutline' })

defineProps({
  node: {
    type: Object,
    required: true,
  },
})
</script>

<template>
  <li class="outline-item">
    <details v-if="node.children && node.children.length">
      <summary>{{ node.title }}</summary>
      <ul class="sections nested">
        <CourseOutline
          v-for="(child, index) in node.children"
          :key="child.title + '-' + index"
          :node="child"
        />
      </ul>
    </details>
    <span v-else class="outline-leaf">{{ node.title }}</span>
  </li>
</template>
