<template>
  <div class="record">
    <section class="record-hero">
      <div class="record-card">
        <p class="record-eyebrow">여행 기록하기</p>
        <h1 class="record-title">사진과 메모로 여행을 남겨보세요.</h1>
        <p class="record-subtitle">
          헤더에서 사진을 올려 AI로 분석하는 기능을 준비 중입니다.
          지금은 여행 메모 흐름을 정리하는 페이지로 시작합니다.
        </p>
        <div class="record-actions">
          <button class="btn primary" disabled>사진 업로드 (준비중)</button>
          <button class="btn secondary" @click="goBoard">게시판 둘러보기</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goBoard = () => {
  router.push({ name: 'board' })
}
</script>

<style scoped>
.record {
  min-height: calc(100vh - 80px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  background: radial-gradient(circle at top right, #e0f2fe 0, #f8fafc 45%, #ffffff 100%);
}

.record-hero {
  width: min(960px, 100%);
}

.record-card {
  background: #ffffffcc;
  border-radius: 24px;
  padding: 36px 28px;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(10px);
}

.record-eyebrow {
  display: inline-block;
  padding: 6px 14px;
  margin-bottom: 16px;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 0.85rem;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.record-title {
  font-size: 2.2rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 16px;
}

.record-subtitle {
  font-size: 1rem;
  color: #475569;
  line-height: 1.7;
  margin-bottom: 28px;
}

.record-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.btn {
  padding: 10px 20px;
  border-radius: 999px;
  border: 0;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease-out;
}

.btn.primary {
  background: linear-gradient(135deg, #1d4ed8, #2563eb);
  color: #fff;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.35);
}

.btn.primary:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
}

.btn.secondary {
  background: #e2e8f0;
  color: #1e293b;
}

.btn.secondary:hover {
  background: #cbd5f5;
}

@media (max-width: 768px) {
  .record {
    padding: 32px 16px;
  }

  .record-card {
    padding: 28px 20px;
  }

  .record-title {
    font-size: 1.8rem;
  }
}
</style>
