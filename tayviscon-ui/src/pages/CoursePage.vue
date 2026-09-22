<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { get } from '../api/courses.js'
import { me } from '../api/me.js'
import { courseLogoUrl } from '../api/safeMedia.js'
import CourseOutline from '../components/CourseOutline.vue'
import { applyRouteSeo } from '../seo.js'

const route = useRoute()
const course = ref(null)
const error = ref('')
const notFound = ref(false)
const user = ref()

async function load() {
  const id = route.params.id
  error.value = ''
  notFound.value = false
  try {
    const data = await get(id)
    if (route.params.id !== id) return
    course.value = data
    applyRouteSeo(route, {
      title: `${data.title} — Tayviscon IO`,
      description: data.summary || undefined,
    })
  } catch (e) {
    if (route.params.id !== id) return
    course.value = null
    if (e.status === 404) {
      notFound.value = true
    } else {
      error.value = e.message || 'каталог временно недоступен'
    }
  }
}

onMounted(() => {
  load()
  me()
    .then((data) => {
      user.value = data.authenticated ? data : null
    })
    .catch(() => {
      user.value = null
    })
})
watch(() => route.params.id, load)

const outline = computed(() => {
  if (course.value?.outline?.length) {
    return course.value.outline
  }
  return (course.value?.sections || []).map((title) => ({
    title,
    children: [],
  }))
})

function badgeClass(status) {
  if (status === 'published') return 'badge is-published'
  if (status === 'finished') return 'badge is-finished'
  return 'badge is-progress'
}
</script>

<template>
  <div v-if="notFound" class="course-page">
    <h1>404</h1>
    <p>Курс не найден.</p>
    <RouterLink class="cta" to="/courses">Все курсы</RouterLink>
  </div>
  <div v-else-if="error" class="course-page">
    <p class="catalog-error">{{ error }}</p>
  </div>
  <div v-else-if="course" class="course-page course-detail">
    <div class="course-cover">
      <img
        v-if="course.logoUrl"
        class="logo"
        :src="courseLogoUrl(course.logoUrl)"
        :alt="course.title"
        style="object-fit: contain"
      />
      <div v-else class="fallback cover-fallback">
        {{ course.fallbackLetter }}
      </div>
    </div>
    <div class="course-heading">
      <h1>{{ course.title }}</h1>
      <span :class="badgeClass(course.status)">{{ course.badge }}</span>
    </div>
    <p class="lede">{{ course.summary }}</p>
    <p class="course-enroll">
      <RouterLink v-if="user === null" class="cta" to="/login"
        >Поступить</RouterLink
      >
      <span v-else class="cta">Поступить</span>
    </p>
    <h3>Оглавление</h3>
    <ul class="sections">
      <CourseOutline
        v-for="(node, index) in outline"
        :key="node.title + '-' + index"
        :node="node"
      />
    </ul>
    <p class="ide-hint ink">
      В IntelliJ откройте папку <code>{{ course.idePath }}</code> (не корень
      репозитория) с плагином JetBrains Academy
    </p>
  </div>
</template>
