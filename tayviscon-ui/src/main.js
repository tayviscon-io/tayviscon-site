import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import HomePage from './pages/HomePage.vue'
import CoursesPage from './pages/CoursesPage.vue'
import CoursePage from './pages/CoursePage.vue'
import BlogPage from './pages/BlogPage.vue'
import LoginPage from './pages/LoginPage.vue'
import { applyRouteSeo } from './seo.js'
import './styles/sketch.css'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomePage },
    { path: '/login', component: LoginPage },
    { path: '/courses', component: CoursesPage },
    { path: '/courses/:id', component: CoursePage },
    { path: '/blog', component: BlogPage },
    { path: '/blog/:pathMatch(.*)*', component: BlogPage },
  ],
})

router.afterEach((to) => {
  applyRouteSeo(to)
})

createApp(App).use(router).mount('#app')
