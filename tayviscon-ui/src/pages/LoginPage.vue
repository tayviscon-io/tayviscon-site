<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const failed = computed(() => route.query.error !== undefined)
</script>

<template>
  <section class="login-page">
    <article class="login-card ink">
      <h1>Войти</h1>
      <p class="lede">Пароля я не прошу. Курсы открыты и без входа.</p>
      <p v-if="failed" class="login-error" role="alert">
        Не удалось войти через GitHub. Попробуйте ещё раз.
      </p>
      <a class="cta login-cta" href="/oauth2/authorization/github">
        <img src="/brand/login/github-mark.svg" alt="" width="22" height="22" />
        Продолжить с GitHub
      </a>
      <RouterLink class="login-note" to="/courses">
        Можно уйти — вход не обязателен.
      </RouterLink>
    </article>
  </section>
</template>
