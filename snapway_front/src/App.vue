<!-- src/App.vue -->
<template>
  <div id="app">
    <NavBar />

    <main class="app-main">
      <RouterView />
    </main>

    <Footer />
  </div>
</template>

<script setup>
import NavBar from '@/components/Navbar.vue'
import Footer from '@/components/Footer.vue'
import { onMounted } from 'vue'
import { useAuthStore } from '@/store/useAuthStore'

const authStore = useAuthStore()

onMounted(() => {
  authStore.loadFromStorage()   // 만약 사용자가 새로고침 시 localStorage → Pinia로 복원
})
</script>

<style>
:root {
  --navbar-height: 72px;
}

/* 선택 사항: 전체 레이아웃 기본 스타일 */
#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-main {
  flex: 1 1 auto;
  padding-top: var(--navbar-height);
}

@media (max-width: 768px) {
  :root {
    --navbar-height: 64px;
  }
}
</style>
