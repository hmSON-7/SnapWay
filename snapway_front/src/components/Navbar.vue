<template>
    <header class="navbar">
        <RouterLink to="/" class="logo">
            <img src="@/assets/logoImg.png" alt="SNAPWAY 홈" class="logo-img" />
        </RouterLink>

        <nav class="menu">
            <!-- 로그인 안 된 상태 -->
            <button v-if="!isLoggedIn" class="nav-btn primary" @click="openLogin">
                로그인
            </button>
            <button v-if="!isLoggedIn" class="nav-btn primary" @click="goRegist">
                회원가입
            </button>

            <!-- 로그인 된 상태 -->
            <template v-else>
                <span class="welcome-text">
                    환영합니다 {{ userName }}님!
                </span>
                <button class="nav-btn ghost" @click="goMyPage">
                    회원정보
                </button>
                <button class="nav-btn outline" @click="logout">
                    로그아웃
                </button>
            </template>
        </nav>
    </header>

    <!-- 로그인 모달 -->
    <LoginModal v-if="showLoginModal" @close="closeLogin" />
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import LoginModal from '@/components/LoginModal.vue'
import { useAuthStore } from '@/store/useAuthStore'
import { logoutMember } from '@/api/memberApi'

const router = useRouter()
const authStore = useAuthStore()
const { isLoggedIn, userName } = storeToRefs(authStore)

const showLoginModal = ref(false)

const openLogin = () => {
    showLoginModal.value = true
}

const closeLogin = () => {
    showLoginModal.value = false
}

const goRegist = () => {
    router.push({ name: 'regist' })
}

const goMyPage = () => {
    router.push({ name: 'myPage' })
}

const logout = async () => {
    try {
        // 1) 백엔드에 세션 삭제 요청
        await logoutMember()

        // 2) 프론트 전역 상태 + localStorage 정리
        authStore.logout()

        // 3) 홈으로 이동
        router.push({ name: 'home' })
    } catch (e) {
        // 실패했더라도 프론트는 그냥 로그아웃 처리.. 백엔드가 알아서 하겠지...
        authStore.logout()
        router.push({ name: 'home' })
        console.error('로그아웃 실패:', e)
    }
}
</script>

<style scoped>
/* 기존 스타일 유지 + 환영 문구 살짝 스타일링 */
.navbar {
    position: sticky;
    top: 0;
    z-index: 50;
    width: 100%;
    height: 72px;
    padding: 0 32px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid rgba(226, 232, 240, 0.9);
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
    box-sizing: border-box;
}

.logo {
    display: inline-flex;
    align-items: center;
    text-decoration: none;
}

.logo-img {
    height: 180px;
    width: auto;
    cursor: pointer;
    transition: transform 0.15s ease-out;
}

.logo-img:hover {
    transform: translateY(-1px) scale(1.02);
}

.menu {
    display: flex;
    align-items: center;
    gap: 10px;
}

.welcome-text {
    margin-right: 8px;
    font-size: 0.95rem;
    font-weight: 600;
    color: #1e293b;
}

.nav-btn {
    padding: 8px 16px;
    border-radius: 999px;
    font-size: 0.95rem;
    font-weight: 600;
    cursor: pointer;
    border: none;
    transition: all 0.18s ease-out;
    display: inline-flex;
    align-items: center;
    justify-content: center;
}

.nav-btn.primary {
    background: linear-gradient(135deg, #1e88e5, #1565c0);
    color: #fff;
    box-shadow: 0 8px 20px rgba(21, 101, 192, 0.35);
}

.nav-btn.primary:hover {
    transform: translateY(-1px);
    box-shadow: 0 12px 26px rgba(21, 101, 192, 0.4);
}

.nav-btn.outline {
    background: transparent;
    color: #1e293b;
    border: 1px solid #cbd5e1;
}

.nav-btn.outline:hover {
    background: #e2e8f0;
}

.nav-btn.ghost {
    background: #e2e8f0;
    color: #1e293b;
}

.nav-btn.ghost:hover {
    background: #cbd5e1;
}

@media (max-width: 768px) {
    .navbar {
        padding: 0 16px;
        height: 64px;
    }

    .logo-img {
        height: 44px;
    }

    .nav-btn {
        padding: 6px 12px;
        font-size: 0.9rem;
    }

    .welcome-text {
        display: none;
        /* 모바일에서 공간 부족할 경우 숨겨도 됨 */
    }
}
</style>
