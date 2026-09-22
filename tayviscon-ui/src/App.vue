<script setup>
import SiteNav from './components/SiteNav.vue'
import SiteFooter from './components/SiteFooter.vue'
</script>

<template>
  <div class="site">
    <div class="site-main">
      <div class="bleed">
        <SiteNav />
      </div>
      <div class="shell">
        <router-view />
      </div>
    </div>
    <div class="bleed">
      <SiteFooter />
    </div>
  </div>
</template>
