<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { article, tree } from '../api/blog.js'
import { applyRouteSeo } from '../seo.js'

const AUTHOR = {
  name: 'Tyomych Tovkach',
  github: 'https://github.com/tyomych-tovkach',
  photo: '/brand/tyomych.png',
}

const route = useRoute()
const groups = ref([])
const current = ref(null)
const error = ref('')
const notFound = ref(false)

function joinedPath() {
  const match = route.params.pathMatch
  if (match == null || match === '') return ''
  return Array.isArray(match) ? match.filter(Boolean).join('/') : String(match)
}

const path = computed(() => joinedPath())

function formatDate(iso) {
  if (!iso) return ''
  const [year, month, day] = iso.split('-')
  if (!day) return iso
  return `${day}.${month}.${year}`
}

function groupOf(item) {
  return (item.path || '').split('/')[0]
}

function peelSource(html) {
  if (!html) return { html: '', source: null }
  const doc = new DOMParser().parseFromString(html, 'text/html')
  const quote = doc.body.querySelector(':scope > blockquote')
  if (!quote) return { html, source: null }
  const link = quote.querySelector('a[href]')
  const href = link && link.getAttribute('href')
  if (!href || href.startsWith('/')) return { html, source: null }
  const source = { href, title: link.textContent.trim() }
  quote.remove()
  return { html: doc.body.innerHTML, source }
}

const peeled = computed(() => peelSource(current.value && current.value.html))

async function load() {
  const requested = joinedPath()
  error.value = ''
  notFound.value = false
  try {
    const data = await tree()
    if (joinedPath() !== requested) return
    groups.value = data.groups || []
  } catch (e) {
    if (joinedPath() !== requested) return
    error.value = e.message || 'блог временно недоступен'
    groups.value = []
    current.value = null
    return
  }

  if (!requested || groups.value.some((group) => group.name === requested)) {
    current.value = null
    return
  }

  try {
    const data = await article(requested)
    if (joinedPath() !== requested) return
    if (!data) {
      current.value = null
      notFound.value = true
      return
    }
    current.value = data
    applyRouteSeo(route, {
      title: `${data.title} — Tayviscon IO`,
      description: data.summary || data.title,
    })
  } catch (e) {
    if (joinedPath() !== requested) return
    current.value = null
    error.value = e.message || 'блог временно недоступен'
  }
}

onMounted(load)
watch(() => route.fullPath, load)
</script>

<template>
  <div class="blog-layout">
    <aside class="blog-side">
      <details v-for="group in groups" :key="group.name" open>
        <summary class="blog-group">{{ group.name }}</summary>
        <RouterLink
          v-for="item in group.articles"
          :key="item.path"
          :class="{ cur: item.path === path }"
          :to="'/blog/' + item.path"
        >
          {{ item.title }}
        </RouterLink>
      </details>
    </aside>
    <p v-if="error" class="blog-empty">{{ error }}</p>
    <p v-else-if="notFound" class="blog-empty">статьи нет</p>
    <article v-else-if="current" class="blog-article">
      <h1>{{ current.title }}</h1>
      <p class="blog-meta">
        {{ formatDate(current.date) }} · {{ groupOf(current) }}
      </p>
      <aside class="blog-byline ink">
        <img
          class="blog-byline-photo"
          :src="AUTHOR.photo"
          alt=""
          width="40"
          height="40"
        />
        <div>
          <a
            class="blog-byline-name"
            :href="AUTHOR.github"
            target="_blank"
            rel="noreferrer"
            >{{ AUTHOR.name }}</a
          >
          <p v-if="peeled.source" class="blog-byline-source">
            Перевод и адаптация:
            <a :href="peeled.source.href" rel="nofollow">{{
              peeled.source.title
            }}</a>
          </p>
        </div>
      </aside>
      <p class="blog-abs">{{ current.summary }}</p>
      <div v-html="peeled.html"></div>
    </article>
    <p v-else class="blog-empty">Выбери статью слева — что сейчас интересно.</p>
  </div>
</template>
