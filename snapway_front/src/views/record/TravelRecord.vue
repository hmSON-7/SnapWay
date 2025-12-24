<template>
  <div class="record">
    <section class="record-hero">
      <div class="record-card">
        <p class="record-eyebrow">여행 기록</p>
        <h1 class="record-title">사진을 모아 AI 여행 기록을 만들어보세요.</h1>
        <p class="record-subtitle">
          여러 장의 이미지를 올리면 AI가 여행 기록을 정리해줄 수 있도록 준비하고 있어요.
          지금은 업로드만 가능해요.
        </p>
        <div class="record-upload">
          <label class="upload-label" for="record-files">여행 사진 업로드</label>
          <input
            id="record-files"
            class="upload-input"
            type="file"
            multiple
            accept="image/*"
            @change="onFileChange"
          />
          <p class="upload-help">선택된 파일: {{ selectedFiles.length }}개</p>
          <ul v-if="selectedFiles.length" class="upload-list">
            <li v-for="file in selectedFiles" :key="file.name">
              {{ file.name }}
            </li>
          </ul>
        </div>
        <div class="record-actions">
          <button class="btn primary" disabled>AI 기록 생성(준비중)</button>
          <button class="btn secondary" @click="goBoard">게시판 둘러보기</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const selectedFiles = ref([])

const goBoard = () => {
  router.push({ name: 'board', query: { category: 'record' } })
}

const onFileChange = (event) => {
  selectedFiles.value = Array.from(event.target.files || [])
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
  padding: 44px 28px;
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

.record-upload {
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.upload-label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #475569;
}

.upload-input {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
}

.upload-help {
  font-size: 0.85rem;
  color: #64748b;
}

.upload-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 6px;
  font-size: 0.85rem;
  color: #475569;
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
