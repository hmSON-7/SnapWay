<!-- src/components/NavBar.vue -->
<template>
    <header class="navbar">
        <!-- 로고/홈 버튼 (이미지 클릭 시 / 로 이동) -->
        <RouterLink to="/" class="logo">
            <img src="@/assets/logoImg.png" alt="SNAPWAY 홈" class="logo-img" />
        </RouterLink>

        <!-- 우측 메뉴 -->
        <nav class="menu">
            <!-- 로그인 안 되어 있을 때 -->
            <button v-if="!isLoggedIn" class="nav-btn primary" @click="goLogin">
                로그인
            </button>

            <!-- 로그인 되어 있을 때 -->
            <template v-else>
                <button class="nav-btn ghost" @click="goMyPage">
                    회원정보
                </button>
                <button class="nav-btn outline" @click="logout">
                    로그아웃
                </button>
            </template>
        </nav>
    </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 예시: localStorage, Pinia, Vuex 등에서 로그인 여부를 가져온다고 가정
const isLoggedIn = computed(() => {
    return !!localStorage.getItem('accessToken')
})

const goLogin = () => {
    router.push({ name: 'login' }) // /login 라우트에 name: 'login' 설정 가정
}

const goMyPage = () => {
    router.push({ name: 'mypage' }) // /mypage 라우트에 name: 'mypage' 설정 가정
}

const logout = () => {
    // 토큰/유저 정보 삭제
    localStorage.removeItem('accessToken')
    // 필요하면 추가 상태값도 정리
    router.push({ name: 'home' })
}
</script>

<style scoped>
.navbar {
    position: sticky;
    /* 항상 상단 고정 (원하면 fixed로 변경 가능) */
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
    /* 패딩 포함해서 100% 처리 */
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

/* 파란색 기본 버튼 (로그인) */
.nav-btn.primary {
    background: linear-gradient(135deg, #1e88e5, #1565c0);
    color: #fff;
    box-shadow: 0 8px 20px rgba(21, 101, 192, 0.35);
}

.nav-btn.primary:hover {
    transform: translateY(-1px);
    box-shadow: 0 12px 26px rgba(21, 101, 192, 0.4);
}

/* 회색 라인 버튼 (로그아웃) */
.nav-btn.outline {
    background: transparent;
    color: #1e293b;
    border: 1px solid #cbd5e1;
}

.nav-btn.outline:hover {
    background: #e2e8f0;
}

/* 연한 배경 버튼 (회원정보) */
.nav-btn.ghost {
    background: #e2e8f0;
    color: #1e293b;
}

.nav-btn.ghost:hover {
    background: #cbd5e1;
}

/* 모바일 대응 */
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
}
</style>
