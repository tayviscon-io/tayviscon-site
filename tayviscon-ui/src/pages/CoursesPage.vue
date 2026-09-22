<script setup>
import { computed, onMounted, ref } from 'vue'
import { list } from '../api/courses.js'
import { courseLogoUrl } from '../api/safeMedia.js'

const courses = ref([])
const error = ref('')
const statusFilter = ref('all')

const visible = computed(() => {
  if (statusFilter.value === 'available') {
    return courses.value.filter(
      (course) => course.status === 'published' || course.status === 'finished',
    )
  }
  if (statusFilter.value === 'in_progress') {
    return courses.value.filter((course) => course.status === 'in_progress')
  }
  if (statusFilter.value === 'finished') {
    return courses.value.filter((course) => course.status === 'finished')
  }
  return courses.value
})

onMounted(async () => {
  try {
    const data = await list()
    courses.value = data.courses || []
  } catch (e) {
    error.value = e.message || 'каталог временно недоступен'
  }
})

function badgeClass(status) {
  if (status === 'published') return 'badge is-published'
  if (status === 'finished') return 'badge is-finished'
  return 'badge is-progress'
}

function onSpot(event) {
  const el = event.currentTarget
  const box = el.getBoundingClientRect()
  el.style.setProperty(
    '--spot-x',
    `${((event.clientX - box.left) / box.width) * 100}%`,
  )
  el.style.setProperty(
    '--spot-y',
    `${((event.clientY - box.top) / box.height) * 100}%`,
  )
}
</script>

<template>
  <div class="course-page">
    <h2>Курсы</h2>
    <p class="lede">Yet Another Course — все курсы в одном месте.</p>
    <p v-if="error" class="catalog-error">{{ error }}</p>
    <template v-else>
      <div class="filters" role="group" aria-label="Фильтр по статусу">
        <button
          type="button"
          class="filter"
          :class="{ on: statusFilter === 'all' }"
          @click="statusFilter = 'all'"
        >
          Все
        </button>
        <button
          type="button"
          class="filter"
          :class="{ on: statusFilter === 'available' }"
          @click="statusFilter = 'available'"
        >
          Доступны
        </button>
        <button
          type="button"
          class="filter"
          :class="{ on: statusFilter === 'in_progress' }"
          @click="statusFilter = 'in_progress'"
        >
          В работе
        </button>
        <button
          type="button"
          class="filter"
          :class="{ on: statusFilter === 'finished' }"
          @click="statusFilter = 'finished'"
        >
          Завершены
        </button>
      </div>
      <section class="catalog-block">
        <h3 class="catalog-heading">Программирование</h3>
        <p v-if="!visible.length" class="lede">Нет курсов с таким статусом.</p>
        <div v-else class="grid">
          <RouterLink
            v-for="course in visible"
            :key="course.id"
            class="ccard ink"
            :to="'/courses/' + course.id"
            @pointermove="onSpot"
          >
            <div class="banner">
              <img
                v-if="course.logoUrl"
                class="logo"
                :src="courseLogoUrl(course.logoUrl)"
                :alt="course.title"
                style="object-fit: contain"
              />
              <div v-else class="fallback">{{ course.fallbackLetter }}</div>
            </div>
            <div class="cbody">
              <h3>{{ course.title }}</h3>
              <p>{{ course.summary }}</p>
              <div class="cfoot">
                <span :class="badgeClass(course.status)">{{
                  course.badge
                }}</span>
              </div>
            </div>
          </RouterLink>
        </div>
      </section>
    </template>
  </div>
</template>
