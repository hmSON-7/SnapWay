<template>
  <div class="board-detail-page">
    <div class="board-detail-container">
      <div v-if="isLoading" class="detail-empty">로딩 중...</div>
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
            <span>작성자 {{ article.authorName ?? '익명' }}</span>
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
            <div v-show="hasTripPath" ref="tripMapRoot" class="detail-trip-map"></div>
            <div v-if="!hasTripPath" class="detail-map-empty">
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

        <div class="comment-section">
          <h3 class="comment-header">댓글 <span class="comment-count">{{ replies.length }}</span></h3>

          <div class="comment-form">
            <template v-if="authStore.isLoggedIn">
              <textarea 
                v-model="newReplyContent" 
                placeholder="댓글을 남겨보세요." 
                class="comment-input"
                rows="3"
              ></textarea>
              <div class="comment-form-actions">
                <button class="btn primary small" @click="registReply">등록</button>
              </div>
            </template>
            <template v-else>
              <div class="login-plz" @click="goLogin">
                댓글을 작성하려면 <span class="link-text">로그인</span>이 필요합니다.
              </div>
            </template>
          </div>

          <ul class="comment-list">
            <li v-for="reply in replies" :key="reply.replyId" class="comment-item">
              <div class="comment-row">
                <div class="comment-meta">
                  <span class="writer">{{ reply.replierName || '알 수 없음' }}</span>
                  <span class="date">{{ formatDate(reply.repliedAt) }}</span>
                </div>
                
                <div v-if="canHandleReply(reply)" class="comment-tools">
                  <template v-if="editingReplyId === reply.replyId">
                    <button class="text-btn save" @click="modifyReply(reply.replyId)">저장</button>
                    <button class="text-btn cancel" @click="cancelEdit">취소</button>
                  </template>
                  <template v-else>
                    <button class="text-btn" @click="startEdit(reply)">수정</button>
                    <button class="text-btn delete" @click="removeReply(reply.replyId)">삭제</button>
                  </template>
                </div>
              </div>

              <div v-if="editingReplyId === reply.replyId" class="edit-area">
                <textarea v-model="editingContent" class="edit-input" rows="2"></textarea>
              </div>
              <div v-else class="comment-text">
                {{ reply.content }}
              </div>
            </li>
            <li v-if="replies.length === 0" class="no-reply">
              첫 번째 댓글을 남겨주세요!
            </li>
          </ul>
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

// API import 추가
import { 
  fetchArticle, 
  deleteArticle, 
  addReply, 
  updateReply, 
  deleteReply 
} from '@/api/articleApi';
import { fetchTripDetail } from '@/api/tripApi';
import { useAuthStore } from '@/store/useAuthStore';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081';
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

// --- 게시글 State ---
const article = ref(null);
const isLoading = ref(true);
const viewerRoot = ref(null);
let viewerInstance = null;

// --- 지도 State ---
const tripMapRoot = ref(null);
let tripMapInstance = null;
let tripMarkers = [];
let tripPolyline = null;
const tripId = ref(null);
const tripData = ref(null);
const tripPath = ref([]);

// --- [NEW] 댓글 State ---
const replies = ref([]);            // 댓글 목록
const newReplyContent = ref('');    // 새 댓글 입력
const editingReplyId = ref(null);   // 현재 수정 중인 댓글 ID
const editingContent = ref('');     // 수정 중인 댓글 내용

// Computed
const currentUserId = computed(() => {
  return authStore.user ? Number(authStore.user.id) : -1;
});
const hasTripPath = computed(() => tripPath.value.length > 0);
const canEdit = computed(() => {
  if (!authStore.isLoggedIn || !article.value) return false;
  return Number(article.value.authorId) === currentUserId.value;
});
const canDelete = computed(() => canEdit.value || authStore.isAdmin); // isAdmin은 store 구현에 따라 다름

// --- 카테고리 로직 ---
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
const categoryClass = computed(() => categoryClassMap[categoryLabel.value] ?? 'cat-free');

// --- Helper Functions ---
const extractTripId = (tags) => {
  if (!tags) return null;
  const match = String(tags).match(/trip:(\d+)/i);
  if (!match) return null;
  const id = Number(match[1]);
  return Number.isFinite(id) ? id : null;
};

const normalizeImageUrls = (content) => {
  if (!content) return content;
  const base = apiBaseUrl.replace(/\/$/, ''); // http://localhost:8081

  // 1. 마크다운 형식 처리: ![alt](/files/...) -> ![alt](http://localhost:8081/files/...)
  // 2. <img> 태그 형식 처리: <img src="/files/..."> -> <img src="http://localhost:8081/files/...">
  
  // 이미 http로 시작하는 주소는 건드리지 않고, /files/로 시작하는 상대경로만 찾아서 앞에 base를 붙입니다.
  let fixedContent = content.replace(
    /!\[([^\]]*)\]\((\/files\/[^)]+)\)/g,
    (match, alt, path) => `![${alt}](${base}${path})`
  );

  fixedContent = fixedContent.replace(
    /<img\s+([^>]*?)src=["'](\/files\/[^"']+)["']([^>]*?)>/gi,
    (match, before, path, after) => `<img ${before}src="${base}${path}"${after}>`
  );

  // 기존에 있던 /img/ 처리 로직도 유지하고 싶다면 아래 추가
  fixedContent = fixedContent.replace(
    /!\[([^\]]*)\]\((\/img\/[^)]+)\)/g,
    (match, alt, path) => `![${alt}](${base}${path})`
  );

  return fixedContent;
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

const formattedDate = computed(() => {
  const value = article.value?.uploadedAt;
  if (!value) return '';
  const raw = String(value);
  if (raw.includes('T')) return raw.split('T')[0];
  if (raw.includes(' ')) return raw.split(' ')[0];
  return raw;
});

// 댓글 날짜 포맷
const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${min}`;
};

// --- Map Logic ---
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
  if (!window.kakao || !window.kakao.maps) return;

  window.kakao.maps.load(() => {
    // 1. 다시 한 번 DOM 체크
    const container = tripMapRoot.value;
    if (!container) return;

    // 2. 지도 인스턴스 생성 (없을 경우에만)
    if (!tripMapInstance) {
      const options = {
        center: new window.kakao.maps.LatLng(pathData[0].latitude, pathData[0].longitude),
        level: 4,
      };
      tripMapInstance = new window.kakao.maps.Map(container, options);
    }

    // 3. 기존 마커/선 삭제
    clearTripMap();

    // 4. 경로 및 마커 그리기
    const bounds = new window.kakao.maps.LatLngBounds();
    const linePath = [];

    pathData.forEach((item) => {
      const latlng = new window.kakao.maps.LatLng(item.latitude, item.longitude);
      linePath.push(latlng);

      const marker = new window.kakao.maps.Marker({
        position: latlng,
        title: item.placeName
      });
      marker.setMap(tripMapInstance);
      tripMarkers.push(marker);
      bounds.extend(latlng);
    });

    tripPolyline = new window.kakao.maps.Polyline({
      path: linePath,
      strokeWeight: 5,
      strokeColor: '#3b82f6',
      strokeOpacity: 0.8,
      strokeStyle: 'solid',
    });
    tripPolyline.setMap(tripMapInstance);

    // 5. [중요] 비동기로 레이아웃 재계산
    // v-show로 인해 display: none -> block으로 바뀔 때 지도가 깨지는 것을 방지
    setTimeout(() => {
      if (tripMapInstance) {
        tripMapInstance.relayout(); // 맵 크기 재계산
        tripMapInstance.setBounds(bounds); // 마커가 다 보이게 조정
      }
    }, 200); // 넉넉하게 200ms 대기
  });
};

// --- Data Loading ---
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
    // Controller가 { article: {}, replies: [] } 형태의 Map을 반환함
    const { data } = await fetchArticle(route.params.articleId);
    
    // 게시글 데이터
    article.value = data?.article ?? null;
    // 댓글 데이터
    replies.value = data?.replies ?? [];

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

// --- Lifecycle & Watchers ---
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
  () => tripData.value,
  async (newData) => {
    // records 데이터가 실제로 존재하고 배열인 경우에만 실행
    if (newData && Array.isArray(newData.records) && newData.records.length > 0) {
      const pathData = buildTripPath(newData.records);
      tripPath.value = pathData;
      
      // DOM 렌더링 완료 대기
      await nextTick();
      
      // 0.1초 정도 추가 대기 (CSS 애니메이션이나 렌더링 지연 대응)
      setTimeout(() => {
        if (tripMapRoot.value && pathData.length > 0) {
          renderTripMap(pathData);
        }
      }, 100);
    }
  },
  { deep: true } // immediate: true는 상황에 따라 제거해도 됩니다 (어차피 loadArticle에서 데이터를 가져오므로)
);

// --- Action Handlers (Article) ---
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
  if (!confirm('게시글을 삭제할까요?')) return;
  try {
    await deleteArticle(article.value.articleId);
    router.push({ name: 'board' });
  } catch (error) {
    console.error('게시글 삭제 실패:', error);
    alert('게시글 삭제에 실패했습니다.');
  }
};

const goLogin = () => {
  alert('로그인이 필요한 서비스입니다.');
  router.push('/regist'); // 혹은 로그인 페이지
};

// --- [NEW] Action Handlers (Reply) ---

// 댓글 등록
const registReply = async () => {
  if (!newReplyContent.value.trim()) {
    alert('내용을 입력해주세요.');
    return;
  }
  if (!article.value) return;

  const replyData = {
    articleId: article.value.articleId,
    content: newReplyContent.value
  };

  try {
    await addReply(replyData);
    newReplyContent.value = ''; // 입력창 초기화
    await loadArticle(); // 목록 갱신 (혹은 수동으로 replies에 push해도 됨)
  } catch (error) {
    console.error('댓글 등록 실패:', error);
    alert('댓글 등록 중 오류가 발생했습니다.');
  }
};

// 본인 댓글 여부 확인
const canHandleReply = (reply) => {
  if (!authStore.isLoggedIn) return false;
  // replierId와 로그인 유저 ID 비교
  return reply.replierId === currentUserId.value;
};

// 댓글 수정 모드 진입
const startEdit = (reply) => {
  editingReplyId.value = reply.replyId;
  editingContent.value = reply.content;
};

// 댓글 수정 취소
const cancelEdit = () => {
  editingReplyId.value = null;
  editingContent.value = '';
};

// 댓글 수정 저장
const modifyReply = async (replyId) => {
  if (!editingContent.value.trim()) {
    alert('내용을 입력해주세요.');
    return;
  }
  
  const replyData = {
    replyId: replyId,
    content: editingContent.value
  };

  try {
    await updateReply(replyData);
    cancelEdit();
    await loadArticle(); // 목록 갱신
  } catch (error) {
    console.error('댓글 수정 실패:', error);
    alert('댓글 수정에 실패했습니다.');
  }
};

// 댓글 삭제
const removeReply = async (replyId) => {
  if (!confirm('댓글을 삭제하시겠습니까?')) return;
  try {
    await deleteReply(replyId);
    await loadArticle(); // 목록 갱신
  } catch (error) {
    console.error('댓글 삭제 실패:', error);
    alert('댓글 삭제에 실패했습니다.');
  }
};
</script>

<style scoped>
/* 기존 스타일 유지 (상단 코드 참조) */
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
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
}

.detail-actions-right {
  display: flex;
  gap: 8px;
}

.btn {
  border-radius: 999px;
  border: none;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
  padding: 10px 18px;
}
.btn.small {
  padding: 6px 14px;
  font-size: 0.85rem;
}
.btn.primary {
  background: #3b82f6;
  color: #fff;
}
.btn.primary:hover {
  background: #2563eb;
}
.btn.secondary {
  background: #e2e8f0;
  color: #1e293b;
}
.btn.secondary:hover {
  background: #cbd5e1;
}
.btn.outline {
  background: transparent;
  border: 1px solid #cbd5e1;
  color: #1e293b;
}
.btn.outline:hover {
  background: #e2e8f0;
}
.btn.danger {
  background: linear-gradient(135deg, #f87171, #ef4444);
  color: #fff;
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
.cat-review { background: #dcfce7; color: #166534; }
.cat-tip { background: #e0f2fe; color: #075985; }
.cat-qna { background: #fef9c3; color: #854d0e; }
.cat-mate { background: #e0e7ff; color: #3730a3; }
.cat-notice { background: #fee2e2; color: #991b1b; }
.cat-free { background: #f8fafc; color: #475569; }

/* --- [NEW] Comment Styles --- */
.comment-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 10px;
}

.comment-header {
  font-size: 1.1rem;
  font-weight: 700;
  color: #334155;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-count {
  color: #3b82f6;
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.comment-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  resize: vertical;
  background: #f8fafc;
  font-size: 0.95rem;
  font-family: inherit;
  outline: none;
  transition: border 0.2s;
}
.comment-input:focus {
  border-color: #3b82f6;
  background: #fff;
}

.comment-form-actions {
  display: flex;
  justify-content: flex-end;
}

.login-plz {
  padding: 20px;
  text-align: center;
  background: #f1f5f9;
  border-radius: 12px;
  color: #64748b;
  cursor: pointer;
  font-size: 0.9rem;
}
.link-text {
  color: #3b82f6;
  text-decoration: underline;
  font-weight: 600;
}

.comment-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.comment-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.85rem;
}

.writer {
  font-weight: 700;
  color: #1e293b;
}

.date {
  color: #94a3b8;
}

.comment-tools {
  display: flex;
  gap: 8px;
}

.text-btn {
  background: none;
  border: none;
  color: #64748b;
  font-size: 0.8rem;
  cursor: pointer;
  padding: 4px;
}
.text-btn:hover {
  color: #334155;
  text-decoration: underline;
}
.text-btn.delete {
  color: #ef4444;
}
.text-btn.delete:hover {
  color: #b91c1c;
}
.text-btn.save {
  color: #3b82f6;
  font-weight: 600;
}

.comment-text {
  font-size: 0.95rem;
  line-height: 1.5;
  color: #334155;
  white-space: pre-wrap; /* 줄바꿈 유지 */
}

.edit-area {
  width: 100%;
}
.edit-input {
  width: 100%;
  padding: 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.9rem;
  font-family: inherit;
  resize: vertical;
}

.no-reply {
  text-align: center;
  padding: 30px;
  color: #94a3b8;
  font-style: italic;
}

@media (max-width: 768px) {
  .board-detail-page { padding: 28px 16px; }
  .board-detail-card { padding: 22px 18px; }
  .detail-title { font-size: 1.7rem; }
}
</style>