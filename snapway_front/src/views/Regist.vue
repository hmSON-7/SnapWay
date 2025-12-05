<!-- src/views/Regist.vue -->
<template>
    <div class="regist-page">
        <div class="regist-card">
            <h1 class="regist-title">SNAPWAY 회원가입</h1>
            <p class="regist-subtitle">
                여행의 순간들을 기록할 준비가 되셨나요?
            </p>

            <form class="regist-form" @submit.prevent="onSubmit">
                <!-- 이메일 -->
                <div class="form-group">
                    <label for="email">이메일</label>
                    <input id="email" v-model="email" type="email" placeholder="example@snapway.com" required />
                </div>

                <!-- 비밀번호 -->
                <div class="form-group">
                    <label for="password">비밀번호</label>
                    <input id="password" v-model="password" type="password" placeholder="8자 이상, 영문/숫자 포함" required />
                </div>

                <!-- 비밀번호 확인 -->
                <div class="form-group">
                    <label for="passwordConfirm">비밀번호 확인</label>
                    <input id="passwordConfirm" v-model="passwordConfirm" type="password" placeholder="비밀번호를 다시 입력하세요"
                        required />
                </div>

                <!-- 닉네임(username) -->
                <div class="form-group">
                    <label for="username">닉네임</label>
                    <input id="username" v-model="username" type="text" placeholder="여행자님의 닉네임을 입력하세요" required />
                </div>

                <!-- 성별 + 생년월일 -->
                <div class="form-group-inline">
                    <div class="form-group">
                        <label for="gender">성별</label>
                        <select id="gender" v-model="gender">
                            <option value="">선택 안 함</option>
                            <option value="MALE">남성</option>
                            <option value="FEMALE">여성</option>
                            <option value="OTHER">기타</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label for="birthday">생년월일</label>
                        <input id="birthday" v-model="birthday" type="date" />
                    </div>
                </div>

                <!-- 여행 스타일 -->
                <div class="form-group">
                    <label for="style">여행 스타일</label>
                    <select id="style" v-model="style">
                        <option value="">선택 안 함</option>
                        <option value="RELAX">휴양형</option>
                        <option value="ADVENTURE">액티비티</option>
                        <option value="CITY">도시 탐방</option>
                        <option value="FOOD">먹거리 중심</option>
                    </select>
                </div>

                <!-- 메시지 -->
                <p v-if="error" class="error-message">
                    {{ error }}
                </p>
                <p v-if="successMessage" class="success-message">
                    {{ successMessage }}
                </p>

                <!-- 버튼 -->
                <button class="btn primary" type="submit" :disabled="loading">
                    {{ loading ? '가입 처리 중...' : '회원가입' }}
                </button>

                <button class="btn ghost" type="button" @click="openLogin">
                    이미 계정이 있으신가요? 로그인 하기
                </button>
            </form>
        </div>

        <!-- 회원가입 화면 위에서 띄우는 로그인 모달 -->
        <LoginModal v-if="showLoginModal" @close="showLoginModal = false" />
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { registMember } from '@/api/memberApi'
import LoginModal from '@/components/LoginModal.vue'

const router = useRouter()

const email = ref('')
const password = ref('')
const passwordConfirm = ref('')
const username = ref('')
const gender = ref('')
const birthday = ref('')   // 'YYYY-MM-DD'
const style = ref('')

const error = ref('')
const successMessage = ref('')
const loading = ref(false)

const showLoginModal = ref(false)

const validateForm = () => {
    if (!email.value || !password.value || !passwordConfirm.value || !username.value) {
        error.value = '필수 항목을 모두 입력해 주세요.'
        return false
    }
    if (password.value !== passwordConfirm.value) {
        error.value = '비밀번호와 비밀번호 확인이 일치하지 않습니다.'
        return false
    }
    if (password.value.length < 8) {
        error.value = '비밀번호는 최소 8자 이상이어야 합니다.'
        return false
    }
    return true
}

const resetForm = () => {
    email.value = ''
    password.value = ''
    passwordConfirm.value = ''
    username.value = ''
    gender.value = ''
    birthday.value = ''
    style.value = ''
}

const onSubmit = async () => {
    error.value = ''
    successMessage.value = ''

    if (!validateForm()) {
        return
    }

    const member = {
        email: email.value,
        password: password.value,
        username: username.value,
        gender: gender.value || null,
        birthday: birthday.value || null,
        style: style.value || null,
    }

    try {
        loading.value = true

        const res = await registMember(member)

        // 2xx 이면서 서버에서 success라고 내려온 경우만 성공 처리
        if (res.status >= 200 && res.status < 300 && res.data.message === 'success') {
            successMessage.value = '회원가입이 완료되었습니다. 이제 로그인해 주세요.'
            resetForm()

            setTimeout(() => {
                showLoginModal.value = true
                // 또는 router.push({ name: 'login' })
            }, 800)
        } else {
            error.value =
                res.data?.message || '회원가입에 실패했습니다. 입력 정보를 다시 확인해 주세요.'
        }
    } catch (e) {
        const status = e.response?.status
        const serverMessage = e.response?.data?.message

        if (status === 400) {
            error.value = serverMessage || '입력한 정보가 유효하지 않습니다.'
        } else if (status === 409) {
            // 예: 이메일 중복 등
            error.value = serverMessage || '이미 사용 중인 이메일입니다.'
        } else if (status >= 500) {
            error.value = '서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
        } else {
            error.value =
                serverMessage || '회원가입 중 알 수 없는 오류가 발생했습니다.'
        }
    } finally {
        loading.value = false
    }

}

const openLogin = () => {
    showLoginModal.value = true
}
</script>

<style scoped>
.regist-page {
    min-height: calc(100vh - 80px);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40px 16px;
    background: radial-gradient(circle at top left, #0f172a 0, #020617 45%, #000000 100%);
}

.regist-card {
    width: 100%;
    max-width: 520px;
    padding: 32px 28px 36px;
    border-radius: 24px;
    background: rgba(15, 23, 42, 0.9);
    border: 1px solid rgba(148, 163, 184, 0.4);
    box-shadow:
        0 20px 45px rgba(15, 23, 42, 0.85),
        0 0 0 1px rgba(148, 163, 184, 0.22);
    backdrop-filter: blur(18px) saturate(140%);
    -webkit-backdrop-filter: blur(18px) saturate(140%);
    color: #e5e7eb;
}

.regist-title {
    font-size: 2rem;
    font-weight: 800;
    margin-bottom: 8px;
    color: #e5f0ff;
}

.regist-subtitle {
    font-size: 0.95rem;
    color: #94a3b8;
    margin-bottom: 24px;
}

.regist-form {
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

.form-group-inline {
    display: flex;
    gap: 12px;
}

.form-group-inline .form-group {
    flex: 1;
}

label {
    font-size: 0.85rem;
    font-weight: 600;
    color: #cbd5f5;
}

input,
select {
    padding: 10px 12px;
    border-radius: 12px;
    border: 1px solid rgba(148, 163, 184, 0.5);
    background: rgba(15, 23, 42, 0.85);
    color: #e5e7eb;
    font-size: 0.95rem;
    outline: none;
    transition: border-color 0.16s ease-out, box-shadow 0.16s ease-out, background 0.16s ease-out;
}

input::placeholder {
    color: #64748b;
}

input:focus,
select:focus {
    border-color: #38bdf8;
    box-shadow: 0 0 0 1px rgba(56, 189, 248, 0.55);
    background: rgba(15, 23, 42, 0.95);
}

.error-message {
    margin-top: 4px;
    font-size: 0.85rem;
    color: #f97373;
}

.success-message {
    margin-top: 4px;
    font-size: 0.85rem;
    color: #4ade80;
}

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

.btn.primary {
    background: linear-gradient(135deg, #22c55e, #16a34a);
    color: #f9fafb;
    box-shadow:
        0 10px 24px rgba(22, 163, 74, 0.65),
        0 0 0 1px rgba(187, 247, 208, 0.35);
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
        0 14px 32px rgba(22, 163, 74, 0.8),
        0 0 0 1px rgba(187, 247, 208, 0.45);
}

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

@media (max-width: 768px) {
    .regist-page {
        padding: 24px 12px;
    }

    .regist-card {
        padding: 24px 20px 30px;
    }

    .form-group-inline {
        flex-direction: column;
    }
}
</style>
