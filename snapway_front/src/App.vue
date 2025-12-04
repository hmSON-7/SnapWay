<!-- src/App.vue -->
<template>
  <div id="app">
    <NavBar />

    <RouterView />
  </div>
</template>

<script setup>
import NavBar from '@/components/Navbar.vue'
</script>

<style>
/* 선택 사항: 전체 레이아웃 기본 스타일 */
#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
</style>
