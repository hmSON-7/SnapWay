<!-- src/views/Login.vue -->
<template>
    <div class="login-page">
        <div class="login-card">
            <h1 class="login-title">로그인</h1>
            <p class="login-subtitle">
                SNAPWAY에 다시 오신 것을 환영합니다.
            </p>

            <form class="login-form" @submit.prevent="onSubmit">
                <div class="form-group">
                    <label for="email">이메일</label>
                    <input id="email" v-model="email" type="email" placeholder="example@snapway.com" required />
                </div>

                <div class="form-group">
                    <label for="password">비밀번호</label>
                    <input id="password" v-model="password" type="password" placeholder="비밀번호를 입력하세요" required />
                </div>

                <p v-if="error" class="error-message">
                    {{ error }}
                </p>

                <button class="btn primary" type="submit" :disabled="loading">
                    {{ loading ? '로그인 중...' : '로그인' }}
                </button>

                <button class="btn ghost" type="button" @click="goHome">
                    홈으로 돌아가기
                </button>
            </form>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginMember } from '@/api/memberApi' // 🔹 axios 분리한 모듈 사용

const router = useRouter()

const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

const onSubmit = async () => {
    error.value = ''

    if (!email.value || !password.value) {
        error.value = '이메일과 비밀번호를 모두 입력해 주세요.'
        return
    }

    try {
        loading.value = true

        // 🔹 분리한 API 모듈 호출
        const res = await loginMember(email.value, password.value)

        if (res.data.message === 'success') {
            const userInfo = res.data.userInfo

            // TODO: JWT 도입 시 accessToken도 여기에서 저장
            // localStorage.setItem('accessToken', res.data.accessToken)

            // NavBar에서 쓸 회원 정보(비밀번호 제외)를 저장
            localStorage.setItem('userInfo', JSON.stringify(userInfo))

            // 폼 초기화
            email.value = ''
            password.value = ''

            // 홈으로 이동
            await router.push({ name: 'home' })
        } else {
            error.value = '이메일 또는 비밀번호를 다시 확인해 주세요.'
        }
    } catch (e) {
        if (e.response && e.response.status === 401) {
            error.value = '이메일 또는 비밀번호가 올바르지 않습니다.'
        } else {
            error.value = '로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
        }
    } finally {
        loading.value = false
    }
}

const goHome = () => {
    router.push({ name: 'home' })
}
</script>

<style scoped>
.login-page {
    min-height: calc(100vh - 80px);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40px 16px;
    background: radial-gradient(circle at top left, #e3f2fd 0, #f9f9ff 40%, #ffffff 100%);
}

.login-card {
    width: 100%;
    max-width: 420px;
    padding: 32px 28px 36px;
    border-radius: 20px;
    background: #ffffffee;
    box-shadow: 0 18px 40px rgba(15, 23, 42, 0.12);
    backdrop-filter: blur(8px);
}

.login-title {
    font-size: 1.9rem;
    font-weight: 800;
    margin-bottom: 8px;
    color: #0f172a;
}

.login-subtitle {
    font-size: 0.95rem;
    color: #64748b;
    margin-bottom: 24px;
}

.login-form {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.form-group {
    text-align: left;
    display: flex;
    flex-direction: column;
    gap: 6px;
}

label {
    font-size: 0.85rem;
    font-weight: 600;
    color: #475569;
}

input {
    padding: 9px 11px;
    border-radius: 10px;
    border: 1px solid #cbd5e1;
    font-size: 0.95rem;
    outline: none;
    transition: border-color 0.16s ease-out, box-shadow 0.16s ease-out;
}

input:focus {
    border-color: #1e88e5;
    box-shadow: 0 0 0 1px rgba(30, 136, 229, 0.2);
}

.error-message {
    margin: 4px 0 0;
    font-size: 0.85rem;
    color: #dc2626;
}

.btn {
    margin-top: 4px;
    width: 100%;
    padding: 9px 14px;
    border-radius: 999px;
    border: none;
    cursor: pointer;
    font-weight: 600;
    font-size: 0.95rem;
    transition: all 0.18s ease-out;
}

.btn.primary {
    background: linear-gradient(135deg, #1e88e5, #1565c0);
    color: #fff;
    box-shadow: 0 10px 24px rgba(21, 101, 192, 0.4);
}

.btn.primary:disabled {
    opacity: 0.7;
    cursor: default;
    box-shadow: none;
    transform: none;
}

.btn.primary:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 14px 30px rgba(21, 101, 192, 0.45);
}

.btn.ghost {
    margin-top: 6px;
    background: #e2e8f0;
    color: #1e293b;
}

.btn.ghost:hover {
    background: #cbd5e1;
}

/* 반응형 대응 */
@media (max-width: 768px) {
    .login-page {
        padding: 24px 12px;
    }

    .login-card {
        padding: 24px 20px 30px;
    }
}
</style>
