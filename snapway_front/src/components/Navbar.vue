<template>
    <header class="navbar">
        <!-- 로고: 아이콘 + 그라데이션 텍스트 -->
        <RouterLink to="/" class="navbar-brand gradient-logo">
            <i class="fas fa-map-marked-alt me-2"></i>
            SNAPWAY
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
                <button class="nav-btn ghost" @click="router.push({ name: 'map' })">
                    여행지 찾기
                </button>
                <button class="nav-btn ghost" @click="router.push({ name: 'board' })">
                    게시판
                </button>
                <button class="nav-btn ghost" @click="goTravelRecord">
                    여행 기록하기
                </button>
                <button class="nav-btn ghost" @click="goMyPage">
                    마이페이지
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

const goTravelRecord = () => {
    router.push({ name: 'record' })
}

const goMyPage = () => {
    router.push({ name: 'myPage' })
}

const logout = async () => {
    try {
        await logoutMember()
        authStore.logout()
        router.push({ name: 'home' })
    } catch (e) {
        authStore.logout()
        router.push({ name: 'home' })
        console.error('로그아웃 실패:', e)
    }
}
</script>

<style scoped>
.navbar-brand {
    display: flex;
    align-items: center;
    font-weight: 700;
    font-size: 1.6rem;
    text-decoration: none;
    color: inherit;
}

/* 로고 텍스트 + 아이콘에 그라데이션 컬러 */
.gradient-logo {
    background-image: linear-gradient(135deg, #1e88e5, #1565c0, #6a11cb);
    background-clip: text;
    -webkit-background-clip: text;
    color: transparent;
    -webkit-text-fill-color: transparent;
}

.gradient-logo:hover {
    filter: brightness(1.05);
}

.navbar {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
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

/* 이전 이미지 로고용 클래스는 그대로 두되, 지금은 미사용 */
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
    color: #0b1324;
    border: 1px solid #7c8aa2;
}

.nav-btn.outline:hover {
    background: #cfd6e2;
}

.nav-btn.ghost {
    background: transparent;
    color: #0b1324;
    border: 1px solid #7c8aa2;
}

.nav-btn.ghost:hover {
    background: #cfd6e2;
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
    }
}
</style>
