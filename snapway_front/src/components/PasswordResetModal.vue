<template>
  <div class="backdrop">
    <div class="login-card">
      <h1 class="login-title">비밀번호 찾기</h1>
      <p class="login-subtitle">
        {{ stepSubtitle }}
      </p>

      <form class="login-form" @submit.prevent="handleNextStep">
        
        <div v-if="step === 1" class="form-group fade-in">
          <label for="reset-email">가입한 이메일</label>
          <input 
            id="reset-email" 
            v-model="email" 
            type="email" 
            placeholder="example@snapway.com" 
            required 
            :disabled="loading"
          />
        </div>

        <div v-if="step === 2" class="form-group fade-in">
          <label for="auth-code">
            인증코드 6자리 
            <span class="timer">({{ formattedTime }})</span>
          </label>
          <div class="code-input-group">
            <input 
              id="auth-code" 
              v-model="authCode" 
              type="text" 
              placeholder="123456" 
              maxlength="6"
              required 
            />
            <button type="button" class="btn outline small" @click="resendCode" :disabled="loading">
              재전송
            </button>
          </div>
        </div>

        <div v-if="step === 3" class="form-group fade-in">
          <div class="form-group" style="margin-bottom: 12px;">
            <label for="new-password">새 비밀번호</label>
            <div class="input-with-toggle">
              <input 
                id="new-password" 
                v-model="newPassword" 
                :type="showPassword ? 'text' : 'password'"
                placeholder="새 비밀번호 입력" 
                required 
              />
              <button class="toggle-btn" type="button" @click="showPassword = !showPassword">
                <i :class="showPassword ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
              </button>
            </div>
          </div>
          <div class="form-group">
            <label for="confirm-password">비밀번호 확인</label>
            <input 
              id="confirm-password" 
              v-model="confirmPassword" 
              type="password" 
              placeholder="비밀번호 재입력" 
              required 
            />
          </div>
        </div>

        <p v-if="error" class="error-message">
          <i class="fas fa-exclamation-circle"></i> {{ error }}
        </p>

        <button class="btn primary" type="submit" :disabled="loading">
          {{ buttonText }}
        </button>

        <button class="btn ghost" type="button" @click="onClose">
          취소
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { sendVerificationCode, verifyCode, resetPassword } from '@/api/authApi'

const emit = defineEmits(['close', 'open-login'])

// --- 상태 변수 ---
const step = ref(1) // 1: 이메일, 2: 인증코드, 3: 비번변경
const loading = ref(false)
const error = ref('')

const email = ref('')
const authCode = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)

// 백엔드 검증 후 받는 토큰
const resetToken = ref('') 

// 타이머 관련
const timeLeft = ref(180) // 3분 (초 단위)
let timerInterval = null

// --- Computed ---
const stepSubtitle = computed(() => {
  if (step.value === 1) return '인증 번호를 받을 이메일을 입력해주세요.'
  if (step.value === 2) return '이메일로 전송된 인증 코드를 입력해주세요.'
  return '새로운 비밀번호를 설정해주세요.'
})

const buttonText = computed(() => {
  if (loading.value) return '처리 중...'
  if (step.value === 1) return '인증 번호 받기'
  if (step.value === 2) return '인증하기'
  return '비밀번호 변경 완료'
})

const formattedTime = computed(() => {
  const m = Math.floor(timeLeft.value / 60)
  const s = timeLeft.value % 60
  return `${m}:${s < 10 ? '0' + s : s}`
})

// --- Methods ---

const onClose = () => {
  stopTimer()
  emit('close')
}

// 단계별 처리 핸들러
const handleNextStep = async () => {
  error.value = ''
  loading.value = true
  
  try {
    if (step.value === 1) {
      // Step 1: 인증 코드 요청
      await sendVerificationCode(email.value)
      step.value = 2
      startTimer()
    } 
    else if (step.value === 2) {
      // Step 2: 코드 검증
      const response = await verifyCode(email.value, authCode.value)
      // 백엔드에서 resetToken을 줍니다. (VerifyResponse 참고)
      // response.data.resetToken 에 위치한다고 가정 (Controller 확인 필요)
      // ResponseEntity.ok(Map.of(... "resetToken", token)) 형태이므로 data.resetToken
      resetToken.value = response.data.resetToken
      stopTimer()
      step.value = 3
    } 
    else if (step.value === 3) {
      // Step 3: 비밀번호 변경 요청
      if (newPassword.value !== confirmPassword.value) {
        error.value = '비밀번호가 서로 일치하지 않습니다.'
        loading.value = false
        return
      }
      await resetPassword(email.value, newPassword.value, resetToken.value)
      alert('비밀번호가 성공적으로 변경되었습니다. 로그인해주세요.')
      emit('close')
      emit('open-login') // 로그인 모달 열기
    }
  } catch (err) {
    // 에러 응답 처리
    const msg = err.response?.data?.message || '오류가 발생했습니다.'
    error.value = msg
  } finally {
    loading.value = false
  }
}

// 타이머 로직
const startTimer = () => {
  timeLeft.value = 180 // 3분 리셋
  stopTimer()
  timerInterval = setInterval(() => {
    if (timeLeft.value > 0) {
      timeLeft.value--
    } else {
      stopTimer()
      error.value = '인증 시간이 만료되었습니다. 재전송해주세요.'
    }
  }, 1000)
}

const stopTimer = () => {
  if (timerInterval) clearInterval(timerInterval)
}

const resendCode = async () => {
  if (loading.value) return
  loading.value = true
  try {
    await sendVerificationCode(email.value)
    alert('인증 코드가 재전송되었습니다.')
    startTimer()
    error.value = ''
  } catch (err) {
    error.value = '재전송 실패: ' + (err.response?.data?.message || '오류 발생')
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  stopTimer()
})
</script>

<style scoped>
/* LoginModal.vue의 스타일을 기반으로 추가/수정 */
.backdrop {
  position: fixed;
  inset: 0;
  background: radial-gradient(circle at top left, #0f172a 0, #020617 45%, #000000 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 32px 28px 36px;
  border-radius: 24px;
  background: rgba(15, 23, 42, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.35);
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(18px) saturate(140%);
  animation: modal-pop 0.22s ease-out;
}

.login-title {
  font-size: 1.8rem;
  font-weight: 800;
  margin-bottom: 8px;
  color: #e5f0ff;
}

.login-subtitle {
  font-size: 0.9rem;
  color: #94a3b8;
  margin-bottom: 24px;
  min-height: 1.2em; /* 높이 고정으로 떨림 방지 */
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-align: left;
}

label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #cbd5f5;
  display: flex;
  justify-content: space-between;
}

.timer {
  color: #fca5a5; /* 붉은색 계열 */
  font-weight: 700;
}

input {
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.4);
  background: rgba(15, 23, 42, 0.65);
  color: #e5e7eb;
  font-size: 0.95rem;
  width: 100%;
  box-sizing: border-box; /* 중요 */
}

input:focus {
  border-color: #38bdf8;
  outline: none;
  background: rgba(15, 23, 42, 0.9);
}

/* 인증코드 입력 그룹 */
.code-input-group {
  display: flex;
  gap: 8px;
}

.code-input-group input {
  flex: 1;
  text-align: center;
  letter-spacing: 4px;
  font-weight: 700;
}

/* 비밀번호 토글 */
.input-with-toggle {
  position: relative;
}
.input-with-toggle input {
  padding-right: 44px;
}
.toggle-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #cbd5f5;
  cursor: pointer;
}

/* 버튼 스타일 */
.btn {
  width: 100%;
  padding: 10px 14px;
  border-radius: 999px;
  border: none;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn.primary {
  background: linear-gradient(135deg, #38bdf8, #2563eb);
  color: #fff;
  margin-top: 8px;
}
.btn.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn.ghost {
  background: rgba(15, 23, 42, 0.7);
  color: #e2e8f0;
  border: 1px solid rgba(148, 163, 184, 0.6);
  margin-top: 4px;
}

.btn.small {
  width: auto;
  padding: 0 16px;
  font-size: 0.85rem;
  white-space: nowrap;
}
.btn.outline {
  background: transparent;
  border: 1px solid #7c8aa2;
  color: #cbd5f5;
}
.btn.outline:hover {
  background: rgba(255,255,255,0.1);
}

.error-message {
  color: #f87171;
  font-size: 0.85rem;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 간단한 페이드인 애니메이션 */
.fade-in {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes modal-pop {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}
</style>