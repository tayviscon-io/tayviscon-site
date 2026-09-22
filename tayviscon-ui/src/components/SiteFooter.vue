<template>
  <footer class="foot ink">
    <div class="foot-cols">
      <div>
        <h4>Навигация</h4>
        <RouterLink to="/">Главная</RouterLink>
        <RouterLink to="/courses">Курсы</RouterLink>
        <RouterLink to="/blog">Блог</RouterLink>
      </div>
      <div>
        <h4>Ресурсы</h4>
        <a href="https://github.com/tayviscon-io/yet-another-course"
          >Yet Another Course</a
        >
        <RouterLink to="/blog">База знаний</RouterLink>
        <a href="https://github.com/tayviscon-io">GitHub org</a>
      </div>
      <div>
        <h4>Связь</h4>
        <div class="socials">
          <a
            class="soc"
            href="https://github.com/tayviscon-io"
            title="GitHub"
            aria-label="GitHub"
          >
            <svg
              width="14"
              height="14"
              viewBox="0 0 16 16"
              fill="#1e1e1e"
              aria-hidden="true"
            >
              <path
                d="M8 0C3.58 0 0 3.58 0 8a8 8 0 0 0 5.47 7.59c.4.07.55-.17.55-.38v-1.33C3.74 14.5 3.27 13 3.27 13c-.36-.91-.88-1.15-.88-1.15-.72-.49.05-.48.05-.48.8.06 1.22.82 1.22.82.71 1.21 1.87.86 2.33.66.07-.52.28-.86.5-1.06-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82A7.6 7.6 0 0 1 8 4.07c.68 0 1.36.09 2 .26 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48v2.2c0 .21.15.46.55.38A8 8 0 0 0 16 8c0-4.42-3.58-8-8-8z"
              />
            </svg>
          </a>
          <a
            class="soc"
            href="https://www.youtube.com/@tayviscon"
            title="YouTube"
            aria-label="YouTube"
          >
            <svg
              width="14"
              height="14"
              viewBox="0 0 16 16"
              fill="#1e1e1e"
              aria-hidden="true"
            >
              <path
                d="M15.7 4.6a2 2 0 0 0-1.4-1.42C13 3 8 3 8 3s-5 0-6.3.18A2 2 0 0 0 .3 4.6 21 21 0 0 0 0 8a21 21 0 0 0 .3 3.4 2 2 0 0 0 1.4 1.42C3 13 8 13 8 13s5 0 6.3-.18a2 2 0 0 0 1.4-1.42A21 21 0 0 0 16 8a21 21 0 0 0-.3-3.4zM6.4 10.4V5.6L10.8 8z"
              />
            </svg>
          </a>
          <a
            class="soc"
            href="https://t.me/tayviscon"
            title="Telegram"
            aria-label="Telegram"
          >
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="#1e1e1e"
              aria-hidden="true"
            >
              <path d="M2.01 21 23 12 2.01 3 2 10l15 2-15 2z" />
            </svg>
          </a>
        </div>
      </div>
    </div>
    <div class="copy">© Tayviscon IO · open source</div>
  </footer>
</template>
