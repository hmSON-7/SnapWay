<!-- src/App.vue -->
<template>
  <div id="app">
    <NavBar />

    <RouterView />

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
/* 선택 사항: 전체 레이아웃 기본 스타일 */
#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
</style>
