<template>
  <div class="board-detail-page">
    <div class="board-detail-container">
      <div v-if="isLoading" class="detail-empty">
        로딩 중...
      </div>
      <div v-else-if="!article" class="detail-empty">
        게시글을 불러오지 못했습니다.
      </div>
      <div v-else class="board-detail-card">
        <div class="detail-header">
          <span class="category-badge" :class="categoryClass">
            {{ categoryLabel }}
          </span>
          <h2 class="detail-title">{{ article.title }}</h2>
          <div class="detail-meta">
            <span>작성자 {{ article.authorId ?? '익명' }}</span>
            <span>{{ formattedDate }}</span>
            <span>조회 {{ article.hits ?? 0 }}</span>
          </div>
        </div>

        <div class="detail-content">
          <div v-if="tripData && tripData.records && tripData.records.length" class="detail-map">
            <div class="detail-map-header">
              <span class="detail-map-title">Trip route</span>
              <span class="detail-map-subtitle">Markers/path from photo time + GPS</span>
            </div>
            <div v-if="hasTripPath" ref="tripMapRoot" class="detail-trip-map"></div>
            <div v-else class="detail-map-empty">
              GPS 정보가 없는 사진이라 경로를 표시할 수 없습니다.
            </div>
          </div>
          <div ref="viewerRoot" class="viewer-root"></div>
        </div>

        <div class="detail-actions">
          <button class="btn secondary" type="button" @click="goBack">목록으로</button>
          <div class="detail-actions-right">
            <button v-if="canEdit" class="btn outline" type="button" @click="goEdit">수정</button>
            <button v-if="canDelete" class="btn danger" type="button" @click="onDelete">삭제</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Viewer from '@toast-ui/editor/dist/toastui-editor-viewer';
import '@toast-ui/editor/dist/toastui-editor-viewer.css';
import { fetchArticle, deleteArticle } from '@/api/articleApi';
import { fetchTripDetail } from '@/api/tripApi';
import { useAuthStore } from '@/store/useAuthStore';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081';
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const article = ref(null);
const isLoading = ref(true);
const viewerRoot = ref(null);
const tripMapRoot = ref(null);
let viewerInstance = null;
let tripMapInstance = null;
let tripMarkers = [];
let tripPolyline = null;
const tripId = ref(null);
const tripData = ref(null);
const tripPath = ref([]);
const currentUserId = computed(() => Number(authStore.loginUser?.id));
const hasTripPath = computed(() => tripPath.value.length > 0);
const canEdit = computed(() => {
  if (!authStore.isLoggedIn || !article.value) return false;
  return Number(article.value.authorId) === currentUserId.value;
});
const canDelete = computed(() => canEdit.value || authStore.isAdmin);

const categoryLabelMap = {
  review: '여행 기록',
  record: '여행 기록',
  tip: '여행 팁',
  qna: '질문',
  mate: '동행 구하기',
  notice: '공지',
  free: '자유',
};

const categoryClassMap = {
  '여행 기록': 'cat-record',
  '여행 팁': 'cat-tip',
  질문: 'cat-qna',
  '동행 구하기': 'cat-mate',
  공지: 'cat-notice',
  자유: 'cat-free',
};

const normalizeCategory = (value) => {
  if (!value) return '자유';
  return categoryLabelMap[value] ?? value;
};

const categoryLabel = computed(() => normalizeCategory(article.value?.category));
const categoryClass = computed(
  () => categoryClassMap[categoryLabel.value] ?? 'cat-free',
);

const extractTripId = (tags) => {
  if (!tags) return null;
  const match = String(tags).match(/trip:(\d+)/i);
  if (!match) return null;
  const id = Number(match[1]);
  return Number.isFinite(id) ? id : null;
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

const formattedDate = computed(() => {
  const value = article.value?.uploadedAt;
  if (!value) return '';
  const raw = String(value);
  if (raw.includes('T')) return raw.split('T')[0];
  if (raw.includes(' ')) return raw.split(' ')[0];
  return raw;
});

const loadTripData = async () => {
  if (!tripId.value) return;
  try {
    const { data } = await fetchTripDetail(tripId.value);
    tripData.value = data;
  } catch (error) {
    console.error('Trip detail load failed:', error);
  }
};

const loadArticle = async () => {
  try {
    const { data } = await fetchArticle(route.params.articleId);
    article.value = data?.article ?? null;
    if (article.value?.content) {
      article.value.content = normalizeImageUrls(article.value.content);
    }
    tripId.value = extractTripId(article.value?.tags);
    if (tripId.value) {
      await loadTripData();
    }
  } catch (error) {
    console.error('게시글 조회 실패:', error);
  } finally {
    isLoading.value = false;
  }
};

const initViewer = () => {
  if (!viewerRoot.value || !article.value) return;
  viewerInstance = new Viewer({
    el: viewerRoot.value,
    initialValue: article.value.content ?? '',
  });
};

onMounted(async () => {
  await loadArticle();
  initViewer();
});

onBeforeUnmount(() => {
  clearTripMap();
  if (viewerInstance) {
    viewerInstance.destroy();
    viewerInstance = null;
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

const goBack = () => {
  router.push({ name: 'board' });
};

const goEdit = () => {
  if (!article.value) return;
  if (!canEdit.value) {
    alert('작성자만 수정할 수 있습니다.');
    return;
  }
  router.push({ name: 'boardEdit', params: { articleId: article.value.articleId } });
};

const onDelete = async () => {
  if (!article.value) return;
  if (!canDelete.value) {
    alert('작성자 또는 관리자만 삭제할 수 있습니다.');
    return;
  }
  const ok = confirm('게시글을 삭제할까요?');
  if (!ok) return;
  try {
    await deleteArticle(article.value.articleId);
    router.push({ name: 'board' });
  } catch (error) {
    console.error('게시글 삭제 실패:', error);
    alert('게시글 삭제에 실패했습니다.');
  }
};
</script>

<style scoped>
.board-detail-page {
  min-height: calc(100vh - 80px);
  display: flex;
  justify-content: center;
  padding: 40px 20px;
  background: radial-gradient(circle at top left, #e3f2fd 0, #f9f9ff 40%, #ffffff 100%);
  color: #1e293b;
}

.board-detail-container {
  width: 100%;
  max-width: 960px;
}

.board-detail-card {
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 20px;
  padding: 28px;
  backdrop-filter: blur(10px);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-title {
  font-size: 2rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #64748b;
  font-size: 0.9rem;
}

.detail-content {
  border-top: 1px solid rgba(148, 163, 184, 0.2);
  padding-top: 20px;
}

.detail-map {
  margin-bottom: 18px;
  padding: 14px;
  border-radius: 12px;
  background: rgba(226, 232, 240, 0.35);
  border: 1px solid rgba(148, 163, 184, 0.2);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-map-header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #1e293b;
}

.detail-map-subtitle {
  font-weight: 500;
  color: #64748b;
}

.detail-trip-map {
  width: 100%;
  height: 260px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  overflow: hidden;
  background: #e2e8f0;
}

.detail-map-empty {
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

.viewer-root :deep(.toastui-editor-contents) {
  font-size: 0.98rem;
  color: #1f2937;
}

.detail-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.detail-actions-right {
  display: flex;
  gap: 8px;
}

.btn.secondary {
  background: #e2e8f0;
  color: #1e293b;
  padding: 10px 18px;
  border-radius: 999px;
  border: none;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}

.btn.secondary:hover {
  background: #cbd5e1;
}

.btn.outline {
  background: transparent;
  border: 1px solid #cbd5e1;
  color: #1e293b;
  padding: 10px 18px;
  border-radius: 999px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}

.btn.outline:hover {
  background: #e2e8f0;
}

.btn.danger {
  background: linear-gradient(135deg, #f87171, #ef4444);
  color: #fff;
  padding: 10px 18px;
  border-radius: 999px;
  border: none;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}

.btn.danger:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(239, 68, 68, 0.35);
}

.detail-empty {
  text-align: center;
  padding: 60px 20px;
  color: #64748b;
}

.category-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  align-self: flex-start;
  width: auto;
  max-width: max-content;
  white-space: nowrap;
}

.cat-record { background: #dcfce7; color: #166534; }
.cat-tip { background: #e0f2fe; color: #075985; }
.cat-qna { background: #fef9c3; color: #854d0e; }
.cat-mate { background: #e0e7ff; color: #3730a3; }
.cat-notice { background: #fee2e2; color: #991b1b; }
.cat-free { background: #ccfbf1; color: #0f766e; }

@media (max-width: 768px) {
  .board-detail-page {
    padding: 28px 16px;
  }

  .board-detail-card {
    padding: 22px 18px;
  }

  .detail-title {
    font-size: 1.7rem;
  }
}
</style>
