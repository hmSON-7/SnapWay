<template>
    <div class="backdrop">
        <div class="login-card">
            <h1 class="login-title">로그인</h1>
            <p class="login-subtitle">
                SNAPWAY에 오신 것을 환영합니다.
            </p>

            <form class="login-form" @submit.prevent="onSubmit">
                <div class="form-group">
                    <label for="email">이메일</label>
                    <input id="email" v-model="email" type="email" placeholder="example@snapway.com" required />
                </div>

                <div class="form-group">
                    <label for="password">비밀번호</label>
                    <div class="input-with-toggle">
                        <input id="password" v-model="password" :type="showPassword ? 'text' : 'password'"
                            placeholder="비밀번호를 입력하세요" required />
                        <button class="toggle-btn" type="button" @click="togglePassword"
                            :aria-pressed="showPassword" :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 보기'">
                            <i :class="showPassword ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
                        </button>
                    </div>
                </div>

                <p v-if="error" class="error-message">
                    {{ error }}
                </p>

                <button class="btn primary" type="submit" :disabled="loading">
                    {{ loading ? '로그인 중...' : '로그인' }}
                </button>

                <button class="btn ghost" type="button" @click="onClose">
                    닫기
                </button>
            </form>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/store/useAuthStore'

// 이벤트 정의
const emit = defineEmits(['close'])

// 스토어 사용
const authStore = useAuthStore()

// 상태 변수
const email = ref('')
const password = ref('')
const showPassword = ref(false)
const error = ref('')
const loading = ref(false)

// 비밀번호 토글
const togglePassword = () => {
    showPassword.value = !showPassword.value
}

// 모달 닫기
const onClose = () => {
    emit('close')
}

// 로그인 제출
const onSubmit = async () => {
    loading.value = true
    error.value = '' // 에러 초기화
    
    try {
        // 스토어의 login 액션 호출
        await authStore.login(email.value, password.value)
        
        // 성공 시 모달 닫기
        emit('close')
    } catch (err) {
        // 에러 처리
        if (err.response && err.response.status === 401) {
            error.value = '이메일 또는 비밀번호가 일치하지 않습니다.'
        } else {
            error.value = '로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
        }
    } finally {
        loading.value = false
    }
}
</script>

<style scoped>
.backdrop {
    position: fixed;
    inset: 0;
    background: radial-gradient(circle at top left, #0f172a 0, #020617 45%, #000000 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 100;
}

/* 카드 전체에 글래스모피즘 + 등장 애니메이션 */
.login-card {
    width: 100%;
    max-width: 420px;
    padding: 32px 28px 36px;
    border-radius: 24px;
    background: rgba(15, 23, 42, 0.82);
    border: 1px solid rgba(148, 163, 184, 0.35);
    box-shadow:
        0 20px 45px rgba(15, 23, 42, 0.75),
        0 0 0 1px rgba(148, 163, 184, 0.18);
    backdrop-filter: blur(18px) saturate(140%);
    -webkit-backdrop-filter: blur(18px) saturate(140%);
    animation: modal-pop 0.22s ease-out;
}

/* 제목/텍스트 컬러 조정 */
.login-title {
    font-size: 1.9rem;
    font-weight: 800;
    margin-bottom: 8px;
    color: #e5f0ff;
}

.login-subtitle {
    font-size: 0.95rem;
    color: #94a3b8;
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

.input-with-toggle {
    position: relative;
    display: flex;
    align-items: center;
}

label {
    font-size: 0.85rem;
    font-weight: 600;
    color: #cbd5f5;
}

/* 인풋을 어두운 글래스 스타일로 */
input {
    padding: 10px 12px;
    border-radius: 12px;
    border: 1px solid rgba(148, 163, 184, 0.4);
    background: rgba(15, 23, 42, 0.65);
    color: #e5e7eb;
    font-size: 0.95rem;
    outline: none;
    transition: border-color 0.16s ease-out, box-shadow 0.16s ease-out, background 0.16s ease-out;
}

.input-with-toggle input {
    width: 100%;
    padding-right: 44px;
}

.toggle-btn {
    position: absolute;
    right: 8px;
    top: 50%;
    transform: translateY(-50%);
    width: 32px;
    height: 32px;
    border-radius: 999px;
    border: none;
    background: transparent;
    color: #cbd5f5;
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    transition: background 0.16s ease-out, color 0.16s ease-out;
}

.toggle-btn:hover {
    background: rgba(30, 64, 175, 0.4);
    color: #e5f0ff;
}

input::placeholder {
    color: #64748b;
}

input:focus {
    border-color: #38bdf8;
    box-shadow: 0 0 0 1px rgba(56, 189, 248, 0.55);
    background: rgba(15, 23, 42, 0.9);
}

.error-message {
    margin: 4px 0 0;
    font-size: 0.85rem;
    color: #f97373;
}

/* 버튼 공통 */
.btn {
    margin-top: 4px;
    width: 100%;
    padding: 10px 14px;
    border-radius: 999px;
    border: none;
    cursor: pointer;
    font-weight: 600;
    font-size: 0.95rem;
    transition: all 0.18s ease-out;
    display: inline-flex;
    align-items: center;
    justify-content: center;
}

/* 메인 버튼: 그라디언트 + 살짝 발광 */
.btn.primary {
    background: linear-gradient(135deg, #38bdf8, #2563eb);
    color: #f9fafb;
    box-shadow:
        0 10px 24px rgba(37, 99, 235, 0.65),
        0 0 0 1px rgba(191, 219, 254, 0.35);
}

.btn.primary:disabled {
    opacity: 0.7;
    cursor: default;
    box-shadow: none;
    transform: none;
}

.btn.primary:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow:
        0 14px 32px rgba(37, 99, 235, 0.8),
        0 0 0 1px rgba(191, 219, 254, 0.45);
}

/* 서브 버튼: 투명한 글래스 스타일 */
.btn.ghost {
    margin-top: 8px;
    background: rgba(15, 23, 42, 0.7);
    color: #e2e8f0;
    border: 1px solid rgba(148, 163, 184, 0.6);
}

.btn.ghost:hover {
    background: rgba(30, 64, 175, 0.6);
    border-color: rgba(191, 219, 254, 0.85);
}

/* 살짝 확대되며 나타나는 애니메이션 */
@keyframes modal-pop {
    from {
        opacity: 0;
        transform: translateY(6px) scale(0.97);
    }

    to {
        opacity: 1;
        transform: translateY(0) scale(1);
    }
}

/* 모바일 대응 */
@media (max-width: 768px) {
    .login-card {
        margin: 0 16px;
        padding: 24px 20px 30px;
    }
}
</style>
