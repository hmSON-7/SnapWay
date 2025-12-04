<template>
    <div class="home">
        <section class="home-hero">
            <div class="home-badge">
                SNAPWAY에 오신 것을 환영합니다
            </div>

            <h1 class="home-title">
                당신의 여행을<br />
                한 장의 스냅으로 기록하세요
            </h1>

            <p class="home-subtitle">
                사진을 업로드하면, 여행 경로와 감정을 담은<br />
                나만의 여행 다이어리가 자동으로 만들어집니다.
            </p>

            <div class="home-actions">
                <button class="btn primary" @click="goStart">
                    지금 시작하기
                </button>
                <button class="btn secondary" @click="goMyPage">
                    내 여행 보러가기
                </button>
            </div>
        </section>
    </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goStart = () => {
    router.push({ name: 'login' })
}

const goMyPage = () => {
    router.push({ name: 'mypage' })
}
</script>

<style scoped>
.home {
    min-height: calc(100vh - 80px);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40px 20px;
    background: radial-gradient(circle at top left, #e3f2fd 0, #f9f9ff 40%, #ffffff 100%);
    /* 가로 스크롤 방지 */
    overflow-x: hidden;
}

/* 화면 너비를 절대 넘지 않도록 max-width 조정 */
.home-hero {
    max-width: min(960px, 100%);
    width: 100%;
    margin: 0 auto;
    text-align: center;
    padding: 48px 16px 56px;
    /* 좌우 패딩을 조금 줄여서 여유 확보 */
    border-radius: 24px;
    background: #ffffffcc;
    backdrop-filter: blur(6px);
    box-shadow: 0 18px 40px rgba(15, 23, 42, 0.12);
}

/* 나머지 스타일은 그대로 */
.home-badge {
    display: inline-block;
    padding: 6px 14px;
    margin-bottom: 18px;
    border-radius: 999px;
    background: #e3f2fd;
    color: #1565c0;
    font-size: 0.85rem;
    font-weight: 600;
    letter-spacing: 0.02em;
}

.home-title {
    font-size: 2.6rem;
    line-height: 1.25;
    font-weight: 800;
    margin-bottom: 18px;
    color: #0f172a;
}

.home-title br {
    display: none;
}

.home-subtitle {
    font-size: 1.05rem;
    color: #64748b;
    margin-bottom: 32px;
    line-height: 1.6;
}

.home-actions {
    display: flex;
    justify-content: center;
    gap: 14px;
    flex-wrap: wrap;
}

.btn {
    padding: 11px 24px;
    border-radius: 999px;
    border: 0;
    cursor: pointer;
    font-weight: 600;
    font-size: 0.98rem;
    transition: all 0.18s ease-out;
    display: inline-flex;
    align-items: center;
    gap: 6px;
}

.btn.primary {
    background: linear-gradient(135deg, #1e88e5, #1565c0);
    color: #fff;
    box-shadow: 0 10px 24px rgba(21, 101, 192, 0.4);
}

.btn.primary:hover {
    transform: translateY(-1px);
    box-shadow: 0 14px 30px rgba(21, 101, 192, 0.45);
}

.btn.secondary {
    background: #e2e8f0;
    color: #1e293b;
}

.btn.secondary:hover {
    background: #cbd5e1;
    transform: translateY(-1px);
}

/* 반응형 대응 */
@media (max-width: 768px) {
    .home {
        padding: 24px 12px;
    }

    .home-hero {
        padding: 28px 16px 36px;
    }

    .home-title {
        font-size: 1.9rem;
    }

    .home-title br {
        display: inline;
    }

    .home-subtitle {
        font-size: 0.95rem;
    }
}
</style>
