<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const articles = ref([]);

// 더미 데이터 (추후 DB 연동 시 교체)
const dummyArticles = [
    {
        no: 5,
        category: '여행 후기',
        categoryClass: 'cat-review',
        title: '제주도 3박 4일 완벽 여행기',
        author: '여행러버',
        date: '2024-03-15',
        hits: 128,
    },
    {
        no: 4,
        category: '여행 팁',
        categoryClass: 'cat-tip',
        title: '부산 맛집 추천 BEST 10',
        author: '맛집헌터',
        date: '2024-03-14',
        hits: 95,
    },
    {
        no: 3,
        category: '질문',
        categoryClass: 'cat-qna',
        title: '강원도 숙소 추천 부탁드려요',
        author: '초보여행자',
        date: '2024-03-10',
        hits: 45,
    },
    {
        no: 2,
        category: '동행 구함',
        categoryClass: 'cat-mate',
        title: '내일 전주 가실 분?',
        author: '혼자여행',
        date: '2024-03-05',
        hits: 12,
    },
    {
        no: 1,
        category: '공지',
        categoryClass: 'cat-notice',
        title: '커뮤니티 이용 수칙 안내',
        author: '관리자',
        date: '2024-03-01',
        hits: 999,
    },
];

onMounted(() => {
    // 로그인 체크 (필요 시 활성화)
    // const savedUser = localStorage.getItem('loginUser');
    // if (!savedUser) {
    //   alert('로그인이 필요한 서비스입니다.');
    //   router.push('/');
    //   return;
    // }

    articles.value = dummyArticles;
});

const goWrite = () => {
    // 글쓰기 페이지로 이동 (추후 구현)
    alert("글쓰기 기능 준비 중입니다.");
}
</script>

<template>
    <div class="board-page">
        <div class="board-container">
        
        <!-- 헤더 섹션 -->
        <div class="board-header">
            <h2 class="board-title">Trip Articles</h2>
            <p class="board-subtitle">여행 정보를 나누고 추억을 공유해보세요.</p>
        </div>

        <!-- 게시판 리스트 카드 -->
        <div class="board-card">
            <div class="table-wrapper">
            <table class="board-table">
                <thead>
                <tr>
                    <th class="th-no">번호</th>
                    <th class="th-cat">분류</th>
                    <th class="th-title">제목</th>
                    <th class="th-author">작성자</th>
                    <th class="th-date">작성일</th>
                    <th class="th-hits">조회</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="article in articles" :key="article.no">
                    <td class="td-no">{{ article.no }}</td>
                    <td class="td-cat">
                    <span class="category-badge" :class="article.categoryClass">
                        {{ article.category }}
                    </span>
                    </td>
                    <td class="td-title">
                    <a href="#" class="article-link">
                        {{ article.title }}
                    </a>
                    </td>
                    <td class="td-author">{{ article.author }}</td>
                    <td class="td-date">{{ article.date }}</td>
                    <td class="td-hits">{{ article.hits }}</td>
                </tr>
                <tr v-if="articles.length === 0">
                    <td colspan="6" class="no-data">게시글이 없습니다.</td>
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
    color: #64748b; /* 중간 그레이 */
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
    color: #94a3b8;
    font-size: 0.9rem;
    white-space: nowrap;
}

.board-table td {
    color: #e5e7eb;
    font-size: 0.95rem;
}

/* 컬럼별 스타일 */
.th-no, .td-no, .th-hits, .td-hits, .th-cat, .td-cat, .th-author, .td-author, .th-date, .td-date {
    text-align: center;
}

.th-no { width: 60px; }
.th-cat { width: 100px; }
.th-author { width: 120px; }
.th-date { width: 120px; }
.th-hits { width: 70px; }

.td-no { color: #64748b; }
.td-author { color: #cbd5e1; font-size: 0.9rem; }
.td-date, .td-hits { color: #94a3b8; font-size: 0.85rem; }

/* 제목 링크 */
.article-link {
    text-decoration: none;
    color: #e5e7eb;
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

.cat-review { background: #dcfce7; color: #166534; }
.cat-tip { background: #e0f2fe; color: #075985; }
.cat-qna { background: #fef9c3; color: #854d0e; }
.cat-mate { background: #e0e7ff; color: #3730a3; }
.cat-notice { background: #fee2e2; color: #991b1b; }

/* 푸터 영역 */
.board-footer {
    padding: 16px;
    display: flex;
    justify-content: flex-end;
    background: rgba(15, 23, 42, 0.4);
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
    color: #64748b;
}

/* 반응형 */
@media (max-width: 768px) {
    .th-no, .td-no, .th-hits, .td-hits, .th-date, .td-date {
        display: none; /* 모바일에서 덜 중요한 컬럼 숨김 */
    }
    .article-link {
        max-width: 200px;
    }
}
</style>