<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { fetchArticles } from '@/api/articleApi';
import { useAuthStore } from '@/store/useAuthStore';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const articles = ref([]);
const selectedCategory = ref('all');
const myOnly = ref(false);
const isLoading = ref(false);
const loadError = ref('');

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

const toCategoryClass = (category) =>
    categoryClassMap[category] ?? 'cat-free';

const formatDate = (value) => {
    if (!value) return '';
    if (value instanceof Date) {
        return value.toISOString().slice(0, 10);
    }
    const raw = String(value);
    if (raw.includes('T')) return raw.split('T')[0];
    if (raw.includes(' ')) return raw.split(' ')[0];
    return raw;
};

const categories = [
    { label: '전체', value: 'all' },
    { label: '여행 기록', value: '여행 기록' },
    { label: '여행 팁', value: '여행 팁' },
    { label: '질문', value: '질문' },
    { label: '동행 구하기', value: '동행 구하기' },
    { label: '공지', value: '공지' },
    { label: '자유', value: '자유' },
];

const filteredArticles = computed(() => {
    if (selectedCategory.value === 'all') {
        return myOnly.value ? articles.value.filter(isMine) : articles.value;
    }
    return articles.value.filter((article) => {
        const matchesCategory = article.category === selectedCategory.value;
        if (!matchesCategory) return false;
        return myOnly.value ? isMine(article) : true;
    });
});

const isMine = (article) => {
    const currentId = Number(authStore.loginUser?.id);
    if (!Number.isFinite(currentId)) return false;
    return Number(article.authorId) === currentId;
};

const setCategory = (value) => {
    selectedCategory.value = value;
    if (value !== '여행 기록') {
        myOnly.value = false;
    }
    if (!authStore.isLoggedIn) {
        myOnly.value = false;
    }
};

const loadArticles = async () => {
    isLoading.value = true;
    loadError.value = '';
    try {
        const { data } = await fetchArticles();
        const list = Array.isArray(data) ? data : data?.articleList ?? [];
        if (!list.length) {
            articles.value = [];
            return;
        }
        const apiArticles = list.map((article) => {
            const category = normalizeCategory(article.category);
            return {
                articleId: article.articleId,
                category,
                categoryClass: toCategoryClass(category),
                title: article.title,
                author: article.authorId ?? '익명',
                authorId: article.authorId ?? null,
                date: formatDate(article.uploadedAt),
                hits: article.hits ?? 0,
            };
        });
        articles.value = apiArticles;

    } catch (error) {
        loadError.value = '게시글을 불러오지 못했습니다.';
        articles.value = [];
    } finally {
        isLoading.value = false;
    }
};

const applyQueryFilters = () => {
    const categoryParam = route.query.category;
    if (categoryParam) {
        const normalized =
            categoryLabelMap[String(categoryParam)] ?? String(categoryParam);
        const exists = categories.some((item) => item.value === normalized);
        if (exists) {
            selectedCategory.value = normalized;
        }
    }

    if (route.query.my) {
        myOnly.value = route.query.my === '1' || route.query.my === 'true';
    }
    if (selectedCategory.value !== '여행 기록') {
        myOnly.value = false;
    }
    if (!authStore.isLoggedIn) {
        myOnly.value = false;
    }
};

onMounted(() => {
    // 로그인 체크 (필요 시 활성화)
    // const savedUser = localStorage.getItem('loginUser');
    // if (!savedUser) {
    //   alert('로그인이 필요한 서비스입니다.');
    //   router.push('/');
    //   return;
    // }

    applyQueryFilters();
    loadArticles();
});

watch(
    () => route.query,
    () => {
        applyQueryFilters();
    },
);

const goWrite = () => {
    router.push({ name: 'boardWrite' });
};

const goDetail = (articleId) => {
    router.push({ name: 'boardDetail', params: { articleId } });
};
</script>

<template>
    <div class="board-page">
        <div class="board-container">
        
        <!-- 헤더 섹션 -->
        <div class="board-header">
            <h2 class="board-title">여행 게시판</h2>
            <p class="board-subtitle">여행 정보를 나누고 추억을 공유해보세요.</p>
        </div>

        <!-- 게시판 리스트 카드 -->
        <div class="board-card">
            <div class="table-wrapper">
            <table class="board-table">
                <thead>
                <tr class="board-filter-row">
                    <th colspan="5">
                    <div class="filter-tags">
                        <button
                        v-for="category in categories"
                        :key="category.value"
                        type="button"
                        class="filter-tag"
                        :class="{ active: selectedCategory === category.value }"
                        :aria-pressed="selectedCategory === category.value"
                        @click="setCategory(category.value)"
                        >
                        {{ category.label }}
                        </button>
                    </div>
                    </th>
                </tr>
                <tr v-if="selectedCategory === '여행 기록'" class="board-filter-row record-filter-row">
                    <th colspan="5">
                    <label class="record-filter">
                        <input type="checkbox" v-model="myOnly" :disabled="!authStore.isLoggedIn" />
                        내가 작성한 여행 기록만 보기
                    </label>
                    </th>
                </tr>
                <tr>
                    <th class="th-cat">분류</th>
                    <th class="th-title">제목</th>
                    <th class="th-author">작성자</th>
                    <th class="th-date">작성일</th>
                    <th class="th-hits">조회</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="article in filteredArticles" :key="article.articleId">
                    <td class="td-cat">
                    <span class="category-badge" :class="article.categoryClass">
                        {{ article.category }}
                    </span>
                    </td>
                    <td class="td-title">
                    <a href="#" class="article-link" @click.prevent="goDetail(article.articleId)">
                        {{ article.title }}
                    </a>
                    </td>
                    <td class="td-author">{{ article.author }}</td>
                    <td class="td-date">{{ article.date }}</td>
                    <td class="td-hits">{{ article.hits }}</td>
                </tr>
                <tr v-if="isLoading">
                    <td colspan="5" class="no-data">로딩 중...</td>
                </tr>
                <tr v-else-if="filteredArticles.length === 0">
                    <td colspan="5" class="no-data">게시글이 없습니다.</td>
                </tr>
                </tbody>
            </table>
            </div>

            <!-- 하단 액션 버튼 -->
            <div class="board-footer">
            <button class="btn primary" @click="goWrite">
                <i class="fas fa-pen me-1"></i> 글쓰기
            </button>
            </div>
        </div>

        </div>
    </div>
</template>

<style scoped>
/* 전체 페이지 레이아웃 (공통 배경) */
.board-page {
    min-height: calc(100vh - 80px);
    display: flex;
    justify-content: center;
    padding: 40px 20px;
    background: radial-gradient(circle at top left, #e3f2fd 0, #f9f9ff 40%, #ffffff 100%);
    color: #1e293b; /* 텍스트: 다크 블루/그레이 */
}

.board-container {
    width: 100%;
    max-width: 1000px;
    display: flex;
    flex-direction: column;
    gap: 24px;
}

/* 헤더 스타일 */
.board-header {
    text-align: center;
    margin-bottom: 10px;
}

.board-title {
    font-size: 2rem;
    font-weight: 800;
    color: #0f172a; /* 진한 네이비 */
    margin-bottom: 8px;
}

.board-subtitle {
    font-size: 1rem;
    color: #475569; /* 중간 그레이 */
}

/* 게시판 카드 (글래스모피즘) */
.board-card {
    background: rgba(255, 255, 255, 0.8); /* 반투명 흰색 */
    border: 1px solid rgba(255, 255, 255, 0.6);
    border-radius: 20px;
    overflow: hidden;
    backdrop-filter: blur(12px);
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05); /* 부드러운 그림자 */
}

.table-wrapper {
    overflow-x: auto;
}

/* 테이블 스타일 */
.board-table {
    width: 100%;
    border-collapse: collapse;
    text-align: left;
}

.board-table th,
.board-table td {
    padding: 16px;
    border-bottom: 1px solid rgba(148, 163, 184, 0.15);
}

.board-table th {
    background: rgba(241, 245, 249, 0.6); /* 연한 회색 배경 */
    font-weight: 600;
    color: #475569;
    font-size: 0.9rem;
    white-space: nowrap;
}

.board-table td {
    color: #1f2937;
    font-size: 0.95rem;
}

.board-filter-row th {
    padding: 12px 16px;
    background: rgba(226, 232, 240, 0.7);
}

.filter-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.filter-tag {
    padding: 6px 12px;
    border-radius: 999px;
    border: 1px solid #cbd5e1;
    background: #ffffff;
    color: #475569;
    font-size: 0.85rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
}

.filter-tag:hover {
    border-color: #38bdf8;
    color: #1d4ed8;
}

.filter-tag.active {
    background: #1d4ed8;
    border-color: #1d4ed8;
    color: #ffffff;
    box-shadow: 0 6px 16px rgba(29, 78, 216, 0.25);
}

.record-filter-row th {
    padding: 10px 16px;
    background: rgba(226, 232, 240, 0.65);
}

.record-filter {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    font-size: 0.85rem;
    font-weight: 600;
    color: #475569;
}

.record-filter input {
    width: 16px;
    height: 16px;
}

.record-filter input:disabled {
    cursor: not-allowed;
    opacity: 0.6;
}

/* 컬럼별 스타일 */
.th-hits, .td-hits, .th-cat, .td-cat, .th-author, .td-author, .th-date, .td-date {
    text-align: center;
}

.th-cat { width: 100px; }
.th-author { width: 120px; }
.th-date { width: 120px; }
.th-hits { width: 70px; }

.td-author { color: #475569; font-size: 0.9rem; }
.td-date, .td-hits { color: #64748b; font-size: 0.85rem; }

/* 제목 링크 */
.article-link {
    text-decoration: none;
    color: #1f2937;
    font-weight: 600;
    transition: color 0.2s;
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 400px;
}

.article-link:hover {
    color: #1565c0; /* 메인 컬러 블루 */
    text-decoration: underline;
}

/* 카테고리 뱃지 */
.category-badge {
    display: inline-block;
    padding: 4px 10px;
    border-radius: 999px;
    font-size: 0.75rem;
    font-weight: 600;
}

.cat-record { background: #dcfce7; color: #166534; }
.cat-tip { background: #e0f2fe; color: #075985; }
.cat-qna { background: #fef9c3; color: #854d0e; }
.cat-mate { background: #e0e7ff; color: #3730a3; }
.cat-notice { background: #fee2e2; color: #991b1b; }
.cat-free { background: #ccfbf1; color: #0f766e; }

/* 푸터 영역 */
.board-footer {
    padding: 12px 16px;
    display: flex;
    justify-content: flex-end;
    background: rgba(226, 232, 240, 0.7);
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

.btn.primary:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 16px rgba(37, 99, 235, 0.5);
}

.no-data {
    text-align: center;
    padding: 40px;
    color: #475569;
}

/* 반응형 */
@media (max-width: 768px) {
    .th-hits, .td-hits, .th-date, .td-date {
        display: none; /* 모바일에서 덜 중요한 컬럼 숨김 */
    }
    .board-filter-row th {
        padding: 12px;
    }
    .article-link {
        max-width: 200px;
    }
}
</style>
