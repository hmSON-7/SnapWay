<template>
  <div class="board-write-page">
    <div class="board-write-container">
      <div class="board-write-header">
        <h2 class="board-write-title">
          {{ isEditing ? '게시글 수정' : '게시글 작성' }}
        </h2>
        <p class="board-write-subtitle">
          매너있게 자신의 여행 팁과 경험을 공유해주세요!
        </p>
      </div>

      <form class="board-write-card" @submit.prevent="onSubmit">
        <div class="field">
          <label class="field-label" for="title">제목</label>
          <input id="title" v-model="form.title" class="field-input" type="text" placeholder="제목을 입력해주세요" />
        </div>

        <div class="field">
          <label class="field-label" for="category">카테고리</label>
          <select id="category" v-model="form.category" class="field-input" :disabled="isAiTrip">
            <option value="">카테고리를 선택하세요</option>
            <option value="여행 기록">여행 기록</option>
            <option value="여행 팁">여행 팁</option>
            <option value="질문">질문</option>
            <option value="동행 구하기">동행구하기</option>
            <option value="자유">자유 주제</option>
            <option value="공지" v-if="isAdmin">공지</option>
          </select>
        </div>

        <div class="field checkbox-field">
            <label class="checkbox-label">
                <input type="checkbox" v-model="isSecret" />
                <span class="text">🔒 나만 보기 (비공개)</span>
            </label>
        </div>

        <div class="field">
          <label class="field-label" id="content-label">내용</label>
          <div v-if="tripData && tripData.records && tripData.records.length" class="trip-map-panel">
            <div class="trip-map-meta">
              <span class="trip-map-title">Trip route</span>
              <span class="trip-map-subtitle">Markers/path from photo time + GPS</span>
            </div>
            <div v-if="hasTripPath" ref="tripMapRoot" class="trip-map"></div>
            <div v-else class="trip-map-empty">
              GPS 정보가 없는 사진이라 경로를 표시할 수 없습니다.
            </div>
          </div>
          <div class="editor-wrap" aria-labelledby="content-label">
            <div ref="editorRoot" class="editor-root"></div>
          </div>
        </div>

        <p class="board-write-note">이미지는 업로드 시 임시 저장됩니다.</p>
        <p v-if="submitError" class="form-error">{{ submitError }}</p>

        <div class="board-write-actions">
          <button type="button" class="btn secondary" @click="goBack">뒤로 가기</button>
          <button type="submit" class="btn primary" :disabled="isSubmitting || (isEditing && !canEdit)">
            {{ isEditing ? '수정하기' : '글 올리기' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import Editor from '@toast-ui/editor';
import '@toast-ui/editor/dist/toastui-editor.css';
import { useAuthStore } from '@/store/useAuthStore';
import { createArticle, updateArticle, fetchArticle, uploadArticleImage } from '@/api/articleApi';
import { fetchTripDetail } from '@/api/tripApi';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081';
const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const isAdmin = computed(() => authStore.isAdmin);
const articleId = computed(() => Number(route.params.articleId) || null);
const isEditing = computed(() => Number.isFinite(articleId.value));
const canEdit = ref(true);
const editorRoot = ref(null);
const tripMapRoot = ref(null);
let editorInstance = null;
let tripMapInstance = null;
let tripMarkers = [];
let tripPolyline = null;
const isSubmitting = ref(false);
const submitError = ref('');
const aiTripId = ref(null);
const tagValue = ref('');
const tripData = ref(null);
const tripPath = ref([]);
const form = ref({
  title: '',
  category: '',
  content: '',
  visibility: 'PUBLIC'
});

const isSecret = ref(false);

const isAiTrip = computed(() => Number.isFinite(aiTripId.value) && aiTripId.value > 0);
const hasTripPath = computed(() => tripPath.value.length > 0);

watch(() => form.value.category, (newVal) => {
    if (newVal !== '여행 기록') {
        isSecret.value = false;
    }
});

const goBack = () => {
  router.push({ name: 'board' });
};

const syncContentFromEditor = () => {
  if (editorInstance) {
    form.value.content = editorInstance.getMarkdown();
  }
};

const normalizeImageUrls = (content) => {
  if (!content) return content;
  const base = apiBaseUrl.replace(/\/$/, '');
  const withMarkdown = content.replace(
    /!\[([^\]]*)\]\((\/img\/[^)]+)\)/g,
    (match, alt, path) => `![${alt}](${base}${path})`,
  );
  return withMarkdown.replace(
    /<img\s+([^>]*?)src=["'](\/img\/[^"']+)["']([^>]*?)>/gi,
    (match, before, path, after) => `<img ${before}src="${base}${path}"${after}>`,
  );
};

const extractTripId = (tags) => {
  if (!tags) return null;
  const match = String(tags).match(/trip:(\d+)/i);
  if (!match) return null;
  const id = Number(match[1]);
  return Number.isFinite(id) ? id : null;
};

const applyAiCategory = () => {
  if (isAiTrip.value) {
    form.value.category = '여행 기록';
  }
};

const loadAiDraft = () => {
  if (isEditing.value) return;
  const raw = sessionStorage.getItem('aiTripDraft');
  if (!raw) return;
  try {
    const parsed = JSON.parse(raw);
    aiTripId.value = Number(parsed.tripId) || null;
    form.value.title = parsed.title ?? '';
    form.value.content = normalizeImageUrls(parsed.content ?? '');

    if (parsed.visibility === 'PRIVATE') {
        form.value.category = '여행 기록'; // 혹시 모르니 카테고리도 세팅
        isSecret.value = true;
    }
    
    if (aiTripId.value) {
      tagValue.value = `trip:${aiTripId.value}`;
    }
    applyAiCategory();
  } catch (error) {
    console.error('AI draft parse failed:', error);
  }
};

const toMillis = (value) => {
  if (!value) return Number.MAX_SAFE_INTEGER;
  const raw = String(value).replace(' ', 'T');
  const time = new Date(raw).getTime();
  return Number.isNaN(time) ? Number.MAX_SAFE_INTEGER : time;
};

const buildTripPath = (records = []) =>
  records
    .map((record) => ({
      latitude: Number(record.latitude),
      longitude: Number(record.longitude),
      placeName: record.placeName,
      visitedDate: record.visitedDate,
    }))
    .filter(
      (record) =>
        Number.isFinite(record.latitude) && Number.isFinite(record.longitude),
    )
    .sort((a, b) => toMillis(a.visitedDate) - toMillis(b.visitedDate));

const clearTripMap = () => {
  if (tripMarkers.length) {
    tripMarkers.forEach((marker) => marker.setMap(null));
    tripMarkers = [];
  }
  if (tripPolyline) {
    tripPolyline.setMap(null);
    tripPolyline = null;
  }
};

const renderTripMap = (pathData) => {
  if (!tripMapRoot.value || !window.kakao || !window.kakao.maps) return;
  if (!pathData.length) return;

  if (!tripMapInstance) {
    tripMapInstance = new window.kakao.maps.Map(tripMapRoot.value, {
      center: new window.kakao.maps.LatLng(pathData[0].latitude, pathData[0].longitude),
      level: 6,
    });
  }

  clearTripMap();

  const bounds = new window.kakao.maps.LatLngBounds();
  const linePath = pathData.map((item) => {
    const latlng = new window.kakao.maps.LatLng(item.latitude, item.longitude);
    const marker = new window.kakao.maps.Marker({
      map: tripMapInstance,
      position: latlng,
      title: item.placeName || 'Trip spot',
    });
    tripMarkers.push(marker);
    bounds.extend(latlng);
    return latlng;
  });

  tripPolyline = new window.kakao.maps.Polyline({
    path: linePath,
    strokeWeight: 4,
    strokeColor: '#2563eb',
    strokeOpacity: 0.9,
    strokeStyle: 'solid',
  });
  tripPolyline.setMap(tripMapInstance);
  tripMapInstance.setBounds(bounds);
};

const loadTripData = async () => {
  if (!aiTripId.value) return;
  try {
    const { data } = await fetchTripDetail(aiTripId.value);
    tripData.value = data;
  } catch (error) {
    console.error('Trip detail load failed:', error);
  }
};

onMounted(() => {
  loadAiDraft();
  editorInstance = new Editor({
    el: editorRoot.value,
    height: '380px',
    initialEditType: 'wysiwyg',
    previewStyle: 'vertical',
    placeholder: 'Write your story',
    usageStatistics: false,
    hideModeSwitch: true,
    initialValue: form.value.content,
  });

  editorInstance.on('change', syncContentFromEditor);
  editorInstance.addHook('addImageBlobHook', async (blob, callback) => {
    try {
      const userId = Number(authStore.loginUser?.id) || 1;
      const { data } = await uploadArticleImage(blob, userId);
      const imageUrl = data?.fileUrl;
      if (imageUrl) {
        callback(imageUrl, blob.name);
        return false;
      }
    } catch (error) {
      console.error('이미지 업로드 실패:', error);
    }
    alert('이미지 업로드에 실패했습니다.');
    return false;
  });

  if (isEditing.value) {
    loadArticle();
  } else if (aiTripId.value) {
    loadTripData();
  }
});

onBeforeUnmount(() => {
  clearTripMap();
  if (editorInstance) {
    editorInstance.off('change', syncContentFromEditor);
    editorInstance.removeHook('addImageBlobHook');
    editorInstance.destroy();
    editorInstance = null;
  }
});

const onSubmit = async () => {
  submitError.value = '';
  if (isEditing.value && !canEdit.value) {
    submitError.value = '작성자만 수정할 수 있습니다.';
    return;
  }
  if (isAiTrip.value) {
    form.value.category = '여행 기록';
    tagValue.value = `trip:${aiTripId.value}`;
  }
  syncContentFromEditor();

  if (!form.value.title.trim()) {
    submitError.value = '제목을 입력해주세요.';
    return;
  }
  if (!form.value.category) {
    submitError.value = '카테고리를 선택해주세요.';
    return;
  }
  if (!form.value.content.trim()) {
    submitError.value = '내용을 입력해주세요.';
    return;
  }

  // FormData 생성
  const formData = new FormData();
  formData.append('title', form.value.title.trim());
  formData.append('content', form.value.content.trim());
  formData.append('category', form.value.category);

  let visibilityValue = 'PUBLIC';
  if (form.value.category === '여행 기록' && isSecret.value) {
      visibilityValue = 'PRIVATE';
  }
  formData.append('visibility', visibilityValue);

  // 디버깅용 로그
  console.log('=== 전송할 데이터 ===');
  console.log('title:', form.value.title.trim());
  console.log('category:', form.value.category);
  console.log('content 길이:', form.value.content.trim().length);

  try {
    isSubmitting.value = true;
    const response = await createArticle(formData);
    console.log('성공:', response.data);
    alert('게시글이 등록되었습니다!');
    router.push({ name: 'board' });
  } catch (error) {
    submitError.value = '게시글 등록에 실패했습니다.';
    console.error('게시글 등록 실패:', error);
    console.error('에러 응답:', error.response?.data);
  } finally {
    isSubmitting.value = false;
  }
};

const loadArticle = async () => {
  try {
    const { data } = await fetchArticle(articleId.value);
    const loaded = data?.article;
    if (!loaded) return;
    form.value.title = loaded.title ?? '';
    form.value.category = loaded.category ?? '';
    form.value.content = normalizeImageUrls(loaded.content ?? '');

    if (loaded.category === '여행 기록' && loaded.visibility === 'PRIVATE') {
        isSecret.value = true;
    }
    
    tagValue.value = loaded.tags ?? '';
    const derivedTripId = extractTripId(tagValue.value);
    if (derivedTripId) {
      aiTripId.value = derivedTripId;
      applyAiCategory();
      await loadTripData();
    }
    if (editorInstance) {
      editorInstance.setMarkdown(form.value.content);
    }
    const currentId = Number(authStore.loginUser?.id);
    canEdit.value = Number.isFinite(currentId) && currentId === Number(loaded.authorId);
  } catch (error) {
    submitError.value = '게시글 정보를 불러오지 못했습니다.';
    console.error('게시글 조회 실패:', error);
  }
};

watch(aiTripId, (value) => {
  if (value && !isEditing.value) {
    loadTripData();
  }
});

watch(
  () => tripData.value?.records,
  (records) => {
    const pathData = buildTripPath(Array.isArray(records) ? records : []);
    tripPath.value = pathData;
    nextTick(() => {
      if (pathData.length) {
        renderTripMap(pathData);
      } else {
        clearTripMap();
      }
    });
  }
);
</script>


<style scoped>
.board-write-page {
  min-height: calc(100vh - 80px);
  display: flex;
  justify-content: center;
  padding: 40px 20px;
  background: radial-gradient(circle at top left, #e3f2fd 0, #f9f9ff 40%, #ffffff 100%);
  color: #1e293b;
}

.board-write-container {
  width: 100%;
  max-width: 920px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.board-write-header {
  text-align: center;
}

.board-write-title {
  font-size: 2rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 8px;
}

.board-write-subtitle {
  font-size: 1rem;
  color: #475569;
}

.board-write-card {
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 20px;
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  backdrop-filter: blur(10px);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-weight: 600;
  color: #475569;
  font-size: 0.9rem;
}

.field-input {
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  padding: 12px 14px;
  font-size: 0.95rem;
  color: #1f2937;
  background: #ffffff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.field-input:focus {
  outline: none;
  border-color: #38bdf8;
  box-shadow: 0 0 0 3px rgba(56, 189, 248, 0.2);
}

.editor-wrap :deep(.toastui-editor-defaultUI) {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
}

.editor-root {
  width: 100%;
}

.editor-wrap :deep(.toastui-editor-defaultUI-toolbar) {
  background: #f8fafc;
}

.editor-wrap :deep(.toastui-editor-contents) {
  font-size: 0.95rem;
  color: #1f2937;
}

.trip-map-panel {
  margin-bottom: 16px;
  padding: 14px;
  border-radius: 12px;
  background: rgba(226, 232, 240, 0.35);
  border: 1px solid rgba(148, 163, 184, 0.25);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.trip-map-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  color: #1e293b;
  font-size: 0.85rem;
  font-weight: 600;
}

.trip-map-title {
  font-size: 0.95rem;
}

.trip-map-subtitle {
  font-weight: 500;
  color: #64748b;
}

.trip-map {
  width: 100%;
  height: 260px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  overflow: hidden;
  background: #e2e8f0;
}

.trip-map-empty {
  width: 100%;
  height: 260px;
  border-radius: 12px;
  border: 1px dashed rgba(148, 163, 184, 0.5);
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 0.9rem;
  text-align: center;
  padding: 12px;
}

.board-write-note {
  font-size: 0.9rem;
  color: #64748b;
}

.form-error {
  font-size: 0.9rem;
  color: #dc2626;
}

.board-write-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.btn {
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.9rem;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.btn.primary {
  background: linear-gradient(135deg, #38bdf8, #2563eb);
  color: #fff;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.4);
}

.btn.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.btn.secondary {
  background: #e2e8f0;
  color: #1e293b;
}

.btn.secondary:hover {
  background: #cbd5e1;
}

@media (max-width: 768px) {
  .board-write-page {
    padding: 28px 16px;
  }

  .board-write-card {
    padding: 22px 18px;
  }

  .board-write-title {
    font-size: 1.7rem;
  }
}
</style>
