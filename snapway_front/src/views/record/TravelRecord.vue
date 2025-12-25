<template>
  <div class="record">
    <section class="record-hero">
      <div class="record-card">
        <p class="record-eyebrow">여행 기록</p>
        <h1 class="record-title">사진을 모아 AI 여행 기록을 만들어보세요.</h1>
        <p class="record-subtitle">
          여러 장의 이미지를 올리면 AI가 여행 기록을 정리해줄 수 있도록 준비하고 있어요.
          사진을 올린 뒤 AI 기록을 생성해보세요.
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
        <p v-if="submitError" class="record-error">{{ submitError }}</p>
        <div class="record-actions">
          <button
            class="btn primary"
            :disabled="isSubmitting || !selectedFiles.length"
            @click="onCreateTrip"
          >
            {{ isSubmitting ? 'AI 기록 생성 중...' : 'AI 기록 생성' }}
          </button>
          <button class="btn secondary" @click="goBoard">게시판 둘러보기</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { createAutoTrip } from '@/api/tripApi'

const router = useRouter()
const selectedFiles = ref([])
const isSubmitting = ref(false)
const submitError = ref('')
const isPrivate = ref(false)

const goBoard = () => {
  router.push({ name: 'board', query: { category: 'record' } })
}

const onFileChange = (event) => {
  selectedFiles.value = Array.from(event.target.files || [])
}

const onCreateTrip = async () => {
  submitError.value = ''
  if (!selectedFiles.value.length) {
    submitError.value = '여행 사진을 선택해주세요.'
    return
  }

  const today = new Date().toISOString().slice(0, 10)
  const generatedTitle = `AI 여행 기록 ${today}`
  const formData = new FormData()
  formData.append('title', generatedTitle)
  formData.append('visibility', isPrivate.value ? 'PRIVATE' : 'PUBLIC')
  selectedFiles.value.forEach((file) => {
    formData.append('files', file)
  })

  try {
    isSubmitting.value = true
    const { data } = await createAutoTrip(formData)
    const trip = data
    const content =
      trip?.records?.find((record) => record.aiContent)?.aiContent ?? ''

    if (!trip?.tripId || !content) {
      submitError.value = 'AI 기록 생성에 실패했습니다.'
      return
    }

    sessionStorage.setItem(
      'aiTripDraft',
      JSON.stringify({
        tripId: trip.tripId,
        title: trip.title ?? generatedTitle,
        content,
        visibility: trip.visibility ?? (isPrivate.value ? 'PRIVATE' : 'PUBLIC')
      })
    )

    router.push({ name: 'boardWrite', query: { aiTrip: '1' } })
  } catch (error) {
    if (error?.response?.status === 401) {
      submitError.value = '로그인이 필요합니다.'
    } else {
      submitError.value = 'AI 기록 생성에 실패했습니다.'
    }
    console.error('AI 기록 생성 실패:', error)
  } finally {
    isSubmitting.value = false
  }
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

.record-error {
  color: #dc2626;
  font-size: 0.9rem;
  font-weight: 600;
  margin-bottom: 12px;
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
