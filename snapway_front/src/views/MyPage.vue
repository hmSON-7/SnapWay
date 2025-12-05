<!-- src/views/MyPage.vue -->
<template>
    <div class="mypage">
        <div class="mypage-card" v-if="isLoggedIn && loginUser">
            <div class="header">
                <div class="avatar">
                    <span v-if="initials">{{ initials }}</span>
                </div>
                <div class="user-main">
                    <h1 class="username">
                        {{ loginUser.username }}
                    </h1>
                    <p class="email">
                        {{ loginUser.email }}
                    </p>
                    <p class="badge">
                        {{ loginUser.role || 'USER' }}
                    </p>
                </div>
            </div>

            <div class="divider"></div>

            <div class="info-grid">
                <div class="info-item">
                    <span class="label">성별</span>
                    <span class="value">
                        {{ loginUser.gender || '비공개' }}
                    </span>
                </div>
                <div class="info-item">
                    <span class="label">생년월일</span>
                    <span class="value">
                        {{ loginUser.birthday || '비공개' }}
                    </span>
                </div>
                <div class="info-item">
                    <span class="label">여행 스타일</span>
                    <span class="value">
                        {{ loginUser.style || '아직 선택하지 않았어요' }}
                    </span>
                </div>
                <div class="info-item">
                    <span class="label">프로필 이미지</span>
                    <span class="value">
                        {{ loginUser.profileImg ? '등록됨' : '미등록' }}
                    </span>
                </div>
            </div>

            <div class="actions">
                <button class="btn outline" type="button" @click="goHome">
                    홈으로 돌아가기
                </button>
                <button class="btn danger" type="button" @click="onLogout">
                    로그아웃
                </button>
            </div>
        </div>

        <div v-else class="mypage-empty">
            <h2>로그인이 필요합니다</h2>
            <p>마이페이지를 보려면 먼저 로그인해 주세요.</p>
            <button class="btn primary" type="button" @click="goHome">
                홈으로 이동
            </button>
        </div>
    </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/store/useAuthStore'

const router = useRouter()
const authStore = useAuthStore()
const { loginUser, isLoggedIn } = storeToRefs(authStore)

// 새로고침 후 직접 /mypage 들어온 경우를 대비해 localStorage에서 복원
onMounted(() => {
    authStore.loadFromStorage()
})

// 아바타에 쓸 이니셜
const initials = computed(() => {
    if (!loginUser.value?.username) return ''
    const name = loginUser.value.username.trim()
    return name.length >= 2 ? name.slice(0, 2) : name[0]
})

const goHome = () => {
    router.push({ name: 'home' })
}

const onLogout = () => {
    authStore.logout()
    router.push({ name: 'home' })
}
</script>

<style scoped>
.mypage {
    min-height: calc(100vh - 80px);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40px 16px;
    background: radial-gradient(circle at top left, #0f172a 0, #020617 45%, #000000 100%);
    color: #e5e7eb;
}

.mypage-card {
    width: 100%;
    max-width: 640px;
    padding: 28px 26px 30px;
    border-radius: 24px;
    background: rgba(15, 23, 42, 0.9);
    border: 1px solid rgba(148, 163, 184, 0.4);
    box-shadow:
        0 20px 45px rgba(15, 23, 42, 0.85),
        0 0 0 1px rgba(148, 163, 184, 0.22);
    backdrop-filter: blur(18px) saturate(140%);
    -webkit-backdrop-filter: blur(18px) saturate(140%);
}

.header {
    display: flex;
    align-items: center;
    gap: 18px;
    margin-bottom: 18px;
}

.avatar {
    flex-shrink: 0;
    width: 68px;
    height: 68px;
    border-radius: 999px;
    background: radial-gradient(circle at 30% 0, #38bdf8, #2563eb 45%, #0f172a 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 800;
    font-size: 1.3rem;
    color: #e5f0ff;
    box-shadow:
        0 10px 24px rgba(37, 99, 235, 0.7),
        0 0 0 1px rgba(148, 163, 184, 0.4);
}

.user-main {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.username {
    font-size: 1.4rem;
    font-weight: 800;
    color: #e5f0ff;
}

.email {
    font-size: 0.9rem;
    color: #94a3b8;
}

.badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    margin-top: 4px;
    align-self: flex-start;
    padding: 4px 10px;
    border-radius: 999px;
    font-size: 0.78rem;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: #bfdbfe;
    background: rgba(37, 99, 235, 0.22);
    border: 1px solid rgba(59, 130, 246, 0.7);
}

.divider {
    height: 1px;
    margin: 18px 0 16px;
    background: linear-gradient(to right, transparent, rgba(148, 163, 184, 0.6), transparent);
}

.info-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px 18px;
    margin-bottom: 22px;
}

.info-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.label {
    font-size: 0.78rem;
    font-weight: 600;
    color: #9ca3af;
    text-transform: uppercase;
    letter-spacing: 0.06em;
}

.value {
    font-size: 0.95rem;
    color: #e5e7eb;
}

.actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
}

.btn {
    padding: 8px 16px;
    border-radius: 999px;
    border: none;
    cursor: pointer;
    font-weight: 600;
    font-size: 0.9rem;
    transition: all 0.18s ease-out;
}

.btn.outline {
    background: transparent;
    color: #e5e7eb;
    border: 1px solid rgba(148, 163, 184, 0.7);
}

.btn.outline:hover {
    background: rgba(15, 23, 42, 0.9);
}

.btn.danger {
    background: linear-gradient(135deg, #f97373, #ef4444);
    color: #f9fafb;
    box-shadow:
        0 8px 20px rgba(239, 68, 68, 0.6),
        0 0 0 1px rgba(254, 202, 202, 0.35);
}

.btn.danger:hover {
    transform: translateY(-1px);
    box-shadow:
        0 12px 26px rgba(239, 68, 68, 0.75),
        0 0 0 1px rgba(254, 202, 202, 0.45);
}

/* 로그인 안 된 경우 */
.mypage-empty {
    min-height: calc(100vh - 80px);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;
    padding: 40px 16px;
    background: radial-gradient(circle at top left, #0f172a 0, #020617 45%, #000000 100%);
    color: #e5e7eb;
    text-align: center;
}

.mypage-empty h2 {
    font-size: 1.4rem;
    font-weight: 700;
}

.mypage-empty p {
    font-size: 0.95rem;
    color: #9ca3af;
}

.mypage-empty .btn.primary {
    margin-top: 8px;
    padding: 9px 18px;
    border-radius: 999px;
    background: linear-gradient(135deg, #38bdf8, #2563eb);
    color: #f9fafb;
    border: none;
    cursor: pointer;
    font-weight: 600;
    font-size: 0.9rem;
    transition: all 0.18s ease-out;
}

.mypage-empty .btn.primary:hover {
    transform: translateY(-1px);
    box-shadow:
        0 12px 26px rgba(37, 99, 235, 0.8),
        0 0 0 1px rgba(191, 219, 254, 0.45);
}

@media (max-width: 768px) {
    .mypage {
        padding: 24px 12px;
    }

    .mypage-card {
        padding: 24px 20px 26px;
    }

    .info-grid {
        grid-template-columns: 1fr;
    }

    .actions {
        justify-content: flex-start;
        flex-wrap: wrap;
    }
}
</style>
