<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { list } from '../api/courses.js'
import { me } from '../api/me.js'

const route = useRoute()
const courses = ref([])
const user = ref()
const open = ref(false)
const canHover = ref(false)
const toggleEl = ref(null)
let media

function hoverQuery() {
  return typeof window.matchMedia === 'function'
    ? window.matchMedia('(hover: hover)')
    : null
}

function syncHover() {
  canHover.value = Boolean(media?.matches)
  if (canHover.value) {
    open.value = false
  }
}

function onEscape(e) {
  if (e.key !== 'Escape' || !open.value) return
  open.value = false
  toggleEl.value?.focus()
}

onMounted(() => {
  media = hoverQuery()
  syncHover()
  media?.addEventListener?.('change', syncHover)
  window.addEventListener('keydown', onEscape)
  list()
    .then((data) => {
      courses.value = data.courses || []
    })
    .catch(() => {
      courses.value = []
    })
  me()
    .then((data) => {
      user.value = data.authenticated ? data : null
    })
    .catch(() => {
      user.value = null
    })
})

onUnmounted(() => {
  media?.removeEventListener?.('change', syncHover)
  window.removeEventListener('keydown', onEscape)
})

watch(
  () => route.fullPath,
  () => {
    open.value = false
  },
)

function onEnter() {
  if (canHover.value) open.value = true
}

function onLeave() {
  if (canHover.value) open.value = false
}

function onToggle() {
  if (canHover.value) {
    open.value = true
    return
  }
  open.value = !open.value
}
</script>

<template>
  <div class="nav-wrap">
    <nav class="nav ink" aria-label="Главное меню">
      <RouterLink to="/" class="brand">Tayviscon</RouterLink>
      <div class="links">
        <RouterLink to="/">Главная</RouterLink>
        <div
          class="mega"
          :class="{ 'is-open': open }"
          @mouseenter="onEnter"
          @mouseleave="onLeave"
        >
          <button
            ref="toggleEl"
            type="button"
            class="mega-toggle"
            aria-haspopup="true"
            :aria-expanded="open"
            @click="onToggle"
          >
            Курсы ▾
          </button>
          <div class="mega-panel ink">
            <p class="mega-label">Программирование</p>
            <div class="mega-col">
              <RouterLink
                v-for="course in courses"
                :key="course.id"
                :to="'/courses/' + course.id"
              >
                {{ course.title }}
              </RouterLink>
            </div>
            <RouterLink to="/courses" class="all">Все курсы</RouterLink>
          </div>
        </div>
        <RouterLink to="/blog">Блог</RouterLink>
        <a href="https://github.com/tayviscon-io">GitHub</a>
        <span
          v-if="user === undefined"
          class="auth-slot"
          aria-hidden="true"
        ></span>
        <template v-else-if="user">
          <span class="profile">
            <img
              class="avatar"
              :src="user.avatarUrl"
              :alt="user.name"
              width="36"
              height="36"
            />
            <span class="user-name">{{ user.name }}</span>
          </span>
          <a class="login" href="/logout">Выйти</a>
        </template>
        <RouterLink v-else class="login" to="/login">Войти</RouterLink>
      </div>
    </nav>
  </div>
</template>
