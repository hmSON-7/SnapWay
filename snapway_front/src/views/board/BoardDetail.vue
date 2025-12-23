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
          <div ref="viewerRoot" class="viewer-root"></div>
        </div>

        <div class="detail-actions">
          <button class="btn secondary" type="button" @click="goBack">목록으로</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Viewer from '@toast-ui/editor/dist/toastui-editor-viewer';
import '@toast-ui/editor/dist/toastui-editor-viewer.css';
import { fetchArticle } from '@/api/articleApi';

const route = useRoute();
const router = useRouter();
const article = ref(null);
const isLoading = ref(true);
const viewerRoot = ref(null);
let viewerInstance = null;

const categoryLabelMap = {
  review: '여행리뷰',
  tip: '여행 팁',
  qna: '질문',
  mate: '동행 구하기',
  notice: '공지',
  free: '자유',
};

const categoryClassMap = {
  여행리뷰: 'cat-review',
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

const formattedDate = computed(() => {
  const value = article.value?.uploadedAt;
  if (!value) return '';
  const raw = String(value);
  if (raw.includes('T')) return raw.split('T')[0];
  if (raw.includes(' ')) return raw.split(' ')[0];
  return raw;
});

const loadArticle = async () => {
  try {
    const { data } = await fetchArticle(route.params.articleId);
    article.value = data?.article ?? null;
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
  if (viewerInstance) {
    viewerInstance.destroy();
    viewerInstance = null;
  }
});

const goBack = () => {
  router.push({ name: 'board' });
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

.viewer-root :deep(.toastui-editor-contents) {
  font-size: 0.98rem;
  color: #1f2937;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
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

.detail-empty {
  text-align: center;
  padding: 60px 20px;
  color: #64748b;
}

.category-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.cat-review { background: #dcfce7; color: #166534; }
.cat-tip { background: #e0f2fe; color: #075985; }
.cat-qna { background: #fef9c3; color: #854d0e; }
.cat-mate { background: #e0e7ff; color: #3730a3; }
.cat-notice { background: #fee2e2; color: #991b1b; }
.cat-free { background: #f8fafc; color: #475569; }

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
