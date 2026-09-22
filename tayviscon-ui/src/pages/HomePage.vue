<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { latest as blogLatest } from '../api/blog.js'
import { latest as youtubeLatest } from '../api/youtube.js'
import { youtubeThumbUrl, youtubeWatchUrl } from '../api/safeMedia.js'

const words = ['открытыми', 'доступными', 'практичными', 'свободными', 'своими']
const index = ref(0)
const blogState = ref('loading')
const posts = ref([])
const videos = ref([])
const tubeState = ref('loading')
let timer

function publishedLabel(iso) {
  if (!iso) {
    return ''
  }
  const [year, month, day] = iso.split('-')
  return `${day}.${month}.${year}`
}

onMounted(() => {
  timer = setInterval(() => {
    index.value = (index.value + 1) % words.length
  }, 2600)
  blogLatest()
    .then((data) => {
      posts.value = data.posts || []
      blogState.value = 'ok'
    })
    .catch(() => {
      posts.value = []
      blogState.value = 'down'
    })
  youtubeLatest()
    .then((data) => {
      videos.value = data.videos || []
      tubeState.value = videos.value.length ? 'ok' : 'empty'
    })
    .catch(() => {
      tubeState.value = 'down'
    })
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>

<template>
  <div>
    <section class="hero">
      <img
        class="mascot"
        src="/brand/tayviscon-mascots-clear.svg"
        alt="Tayviscon"
      />
      <h1>делает знания</h1>
      <div class="stage" aria-live="polite">
        <div
          v-for="(word, i) in words"
          :key="word"
          class="word"
          :class="{ on: i === index, off: i !== index }"
        >
          {{ word }}.
        </div>
      </div>
      <RouterLink class="cta" to="/courses">Открыть курсы</RouterLink>
    </section>

    <section class="section">
      <h2>Что умеет Tayviscon</h2>
      <div class="caps">
        <article class="cap ink">
          <svg
            class="glyph"
            viewBox="0 0 36 36"
            fill="#63b17522"
            stroke="#1e1e1e"
            stroke-width="1.8"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path
              d="M6.2 10.4c4.2-1.4 7.4.2 11.8 1.6 4.5-1.6 7.6-2.9 11.6-1.5v16.2c-4.1-1.5-7.2-.1-11.6 1.6-4.3-1.6-7.5-3.1-11.8-1.6z"
            />
            <path d="M18 12.1v16.4" />
          </svg>
          <h3>Учиться без барьеров</h3>
          <p>Открытые курсы. Без ценников и искусственных ограничений.</p>
        </article>
        <article class="cap ink">
          <svg
            class="glyph"
            viewBox="0 0 36 36"
            fill="#63b17522"
            stroke="#1e1e1e"
            stroke-width="1.8"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="M14 22.5 8.4 28a2.4 2.4 0 0 1-3.4-3.4L11 19" />
            <path d="M12.8 17.2 24.6 6.2l5.2 5.1-11.6 11.2z" />
            <path d="M23.2 7.6l5.1 5.1" />
          </svg>
          <h3>Практика, не маркетинг</h3>
          <p>
            Реальные задачи и структурированный путь, а не «быстрый старт в IT».
          </p>
        </article>
        <article class="cap ink">
          <svg
            class="glyph"
            viewBox="0 0 36 36"
            fill="#63b17522"
            stroke="#1e1e1e"
            stroke-width="1.8"
            stroke-linecap="round"
            aria-hidden="true"
          >
            <circle cx="18" cy="18" r="11.2" />
            <circle cx="18" cy="18" r="2.1" fill="#1e1e1e" stroke="none" />
            <path d="M18 8.6v3.2M18 24.2v3.1M8.6 18h3.1M24.3 18h3.1" />
            <path d="M18 18.2 13.4 12.8" />
          </svg>
          <h3>Идти своим путём</h3>
          <p>Проекты, которые помогают осваивать технологии и вдохновляют.</p>
        </article>
        <article class="cap ink">
          <svg
            class="glyph"
            viewBox="0 0 36 36"
            fill="#63b17522"
            stroke="#1e1e1e"
            stroke-width="1.8"
            stroke-linecap="round"
            aria-hidden="true"
          >
            <circle cx="13.2" cy="13" r="4.1" />
            <circle cx="23.4" cy="13.4" r="4.1" />
            <path d="M6.4 26.6c.6-4.4 3.4-6.7 6.8-6.7s6.1 2.4 6.7 6.6" />
            <path d="M16.8 26.4c.8-3.8 3.4-6 6.6-6 3.3 0 5.9 2.3 6.6 6.2" />
          </svg>
          <h3>Сообщество</h3>
          <p>
            Учиться, делиться опытом и улучшать открытые репозитории вместе.
          </p>
        </article>
      </div>
    </section>

    <blockquote class="quote">
      <div class="qmark" aria-hidden="true">“</div>
      <p>
        Мы уважаем платные платформы, но убеждены, что основные знания должны
        быть доступны каждому бесплатно. Мы не конкурируем, а предлагаем
        альтернативу — для тех, кто хочет учиться без финансовых и социальных
        ограничений.
      </p>
      <div class="who">
        <img
          class="who-photo"
          src="/brand/tyomych.png"
          alt=""
          width="38"
          height="38"
        />
        <div>
          <b>Tyomych Tovkach</b>
          <span>CEO, Tayviscon IO</span>
        </div>
      </div>
    </blockquote>

    <section class="doors ink">
      <div class="door">
        <h3>Курсы</h3>
        <p>Весь каталог Yet Another Course в одном месте.</p>
        <RouterLink to="/courses">Открыть →</RouterLink>
      </div>
      <div class="door">
        <h3>GitHub</h3>
        <p>Исходники курсов и проектов, которые можно улучшать.</p>
        <a href="https://github.com/tayviscon-io">Перейти →</a>
      </div>
      <div class="door">
        <h3>Статьи</h3>
        <p>База знаний рядом с витриной.</p>
        <RouterLink to="/blog">Читать →</RouterLink>
      </div>
    </section>

    <section class="section latest">
      <h2>Последние обновления</h2>
      <p class="latest-lead">Следи за блогом и YouTube — здесь самое свежее.</p>
      <div class="latest-grid">
        <div v-if="blogState === 'ok'" class="from-blog ink">
          <h3>Из блога</h3>
          <RouterLink
            v-for="post in posts"
            :key="post.path"
            class="post"
            :to="'/blog/' + post.path"
          >
            <span class="post-main">
              {{ post.title }}
              <small>{{ post.summary }}</small>
            </span>
            <span class="pill">{{ publishedLabel(post.date) }}</span>
          </RouterLink>
          <RouterLink class="blog-more" to="/blog">Все статьи →</RouterLink>
        </div>
        <div class="tube ink">
          <h3>С YouTube</h3>
          <p v-if="tubeState === 'loading'" class="yt-note">смотрим канал</p>
          <p v-else-if="tubeState === 'empty'" class="yt-note">
            пока нет роликов
          </p>
          <p v-else-if="tubeState === 'down'" class="yt-note">
            канал временно недоступен
          </p>
          <a
            v-for="video in videos"
            :key="video.id"
            class="yt"
            :href="youtubeWatchUrl(video.url)"
          >
            <span class="yt-thumb">
              <img
                :src="youtubeThumbUrl(video.thumbnailUrl)"
                alt=""
                width="96"
                height="54"
              />
              <span class="play" aria-hidden="true"></span>
            </span>
            <span class="yt-title">{{ video.title }}</span>
            <span class="pill">{{ publishedLabel(video.published) }}</span>
          </a>
          <a class="yt-more" href="https://www.youtube.com/@tayviscon"
            >Канал →</a
          >
        </div>
      </div>
    </section>
  </div>
</template>
