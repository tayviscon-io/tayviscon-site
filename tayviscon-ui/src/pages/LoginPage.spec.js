import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import LoginPage from './LoginPage.vue'

async function mountLogin(path) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', component: LoginPage },
      { path: '/courses', component: { template: '<div />' } },
    ],
  })
  await router.push(path)
  await router.isReady()
  return mount(LoginPage, { global: { plugins: [router] } })
}

describe('LoginPage', () => {
  let wrapper

  afterEach(() => {
    wrapper?.unmount()
  })

  it('offers only GitHub OAuth, no password fields', async () => {
    wrapper = await mountLogin('/login')
    expect(wrapper.get('h1').text()).toBe('Войти')
    const github = wrapper.get('a.cta')
    expect(github.text()).toMatch(/продолжить с github/i)
    expect(github.attributes('href')).toBe('/oauth2/authorization/github')
    expect(wrapper.find('input').exists()).toBe(false)
    expect(wrapper.find('.login-error').exists()).toBe(false)
    expect(wrapper.get('.lede').text()).toMatch(/пароля/i)
    expect(wrapper.get('.lede').text()).toMatch(/курсы открыты/i)
    expect(wrapper.get('.login-note').attributes('href')).toBe('/courses')
    expect(wrapper.text()).not.toMatch(/кивок/i)
    expect(wrapper.text()).not.toMatch(/после входа я смогу/i)
    expect(wrapper.text()).not.toMatch(/прищуриваюсь/i)
    expect(wrapper.find('.login-pipeline').exists()).toBe(false)
    expect(wrapper.find('.login-mascot').exists()).toBe(false)
    expect(wrapper.find('.login-person').exists()).toBe(false)
    expect(wrapper.find('.login-unlocks').exists()).toBe(false)
    expect(wrapper.find('.login-window').exists()).toBe(false)
    expect(wrapper.find('.login-counter').exists()).toBe(false)
    expect(wrapper.find('.login-guest').exists()).toBe(false)
    expect(wrapper.text()).not.toMatch(/tayviscon узнал/i)
    expect(wrapper.find('.login-open').exists()).toBe(false)
  })

  it('shows a Russian error when GitHub OAuth fails', async () => {
    wrapper = await mountLogin('/login?error=github')
    expect(wrapper.get('.login-error').text()).toMatch(/не удалось войти/i)
  })
})
