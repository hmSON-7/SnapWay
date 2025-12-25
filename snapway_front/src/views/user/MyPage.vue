<!-- src/views/user/MyPage.vue -->
<template>
    <div class="mypage">
        <!-- 로그인 + 데이터 로드 성공 -->
        <div class="mypage-card" v-if="!loading && member">
            <div class="header">
                <div class="avatar-block">
                    <div class="avatar">
                        <img v-if="avatarUrl || member.profileImg" :src="avatarUrl || member.profileImg" alt="프로필 이미지" />
                        <span v-else-if="initials">{{ initials }}</span>
                    </div>
                    <button class="btn avatar-btn" type="button" @click="triggerAvatarUpload">
                        프로필 이미지 등록
                    </button>
                    <input
                        ref="fileInputRef"
                        class="avatar-input"
                        type="file"
                        accept="image/*"
                        @change="onAvatarSelect"
                    />
                </div>
                <div class="user-main">
                    <div v-if="isEditMode" style="margin-bottom: 4px;">
                        <input v-model="editForm.username" class="edit-input" type="text" placeholder="닉네임" />
                    </div>
                    <h1 v-else class="username">
                        {{ member.username }}
                    </h1>
                    <p class="email">
                        {{ member.email }}
                    </p>
                    <p class="badge" :class="{ admin: member.role === 'ADMIN' }">
                        {{ member.role || 'USER' }}
                    </p>
                </div>
            </div>

            <div class="divider"></div>

            <div class="info-grid">
                <div class="info-item">
                    <span class="label">성별</span>
                    <select v-if="isEditMode" v-model="editForm.gender" class="edit-input">
                        <option value="MALE">남성</option>
                        <option value="FEMALE">여성</option>
                    </select>
                    <span v-else class="value">
                        {{ formatGender(member.gender) }}
                    </span>
                </div>
                <div class="info-item">
                    <span class="label">생년월일</span>
                    <input v-if="isEditMode" v-model="editForm.birthday" type="date" class="edit-input" />
                    <span v-else class="value">
                        {{ member.birthday || '비공개' }}
                    </span>
                </div>
                <div class="info-item">
                    <span class="label">여행 스타일</span>
                    <select v-if="isEditMode" v-model="editForm.style" class="edit-input">
                        <option v-for="(desc, key) in travelStyles" :key="key" :value="key">
                            {{ desc }}
                        </option>
                    </select>
                    <span v-else class="value">
                        {{ travelStyles[member.style] || '아직 선택하지 않았어요' }}
                    </span>
                </div>
            </div>

            <div v-if="isEditMode" style="margin: 20px 0; padding-top:20px; border-top:1px dashed #475569;">
                <p style="margin-bottom:8px; font-size:0.9rem; color:#94a3b8;">비밀번호 변경 (선택)</p>
                <div style="display:flex; gap:10px; flex-wrap:wrap;">
                    <input v-model="editForm.password" type="password" class="edit-input" placeholder="새 비밀번호" />
                    <input v-model="editForm.passwordConfirm" type="password" class="edit-input" placeholder="비밀번호 확인" />
                </div>
            </div>
            <div class="actions">
                <template v-if="!isEditMode">
                    <button class="btn outline" type="button" @click="goHome">홈으로</button>
                    <button class="btn primary" type="button" @click="enterEditMode">정보 수정</button>
                    <button class="btn danger" type="button" @click="onLogout">로그아웃</button>
                </template>

                <template v-else>
                    <button class="btn outline" style="color:#ef4444; border-color:#ef4444;" type="button" @click="handleWithdraw">
                        탈퇴
                    </button>
                    <div style="flex:1;"></div> <button class="btn outline" type="button" @click="cancelEdit">취소</button>
                    <button class="btn primary" type="button" @click="saveChanges" :disabled="isSaving">
                        {{ isSaving ? '저장..' : '저장' }}
                    </button>
                </template>
            </div>
        </div>

        <!-- 로딩 중 -->
        <div v-else-if="loading" class="mypage-empty">
            <h2>로딩 중...</h2>
            <p>회원 정보를 불러오는 중입니다.</p>
        </div>

        <!-- 로그인 안 됐거나 세션 만료 -->
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
import { computed, ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/store/useAuthStore'
import { fetchMyInfo, logoutMember, updateMember, deleteMember } from '@/api/memberApi'

const router = useRouter()
const authStore = useAuthStore()
const { isLoggedIn } = storeToRefs(authStore)

// 서버에서 가져온 회원 정보
const member = ref(null)
const loading = ref(true)
const avatarUrl = ref('')
const fileInputRef = ref(null)
const isEditMode = ref(false)
const isSaving = ref(false)
const passwordError = ref('')

const fallbackMember = {
    username: 'LocalUser',
    email: 'local@snapway.dev',
    role: 'USER',
    gender: null,
    birthday: null,
    style: 'PHOTO',
    profileImg: null,
}

const editForm = reactive({
    username: '',
    gender: 'MALE',
    birthday: '',
    style: null,
    password: '',
    passwordConfirm: ''
})

const travelStyles = {
    NATURE: "자연 힐링", CITY: "도시 탐험", FOOD: "맛집 탐방",
    ACTIVITY: "액티비티", CULTURE: "문화 예술", PHOTO: "인생샷",
    HEALING: "휴양 휴식", HISTORY: "역사 탐방", SHOPPING: "쇼핑 투어",
    LOCAL: "현지 체험", FESTIVAL: "축제/공연", DRIVE: "드라이브",
    DATE: "커플 여행", FAMILY: "가족 여행", PET: "반려동물 동반"
}

const enterEditMode = () => {
    if (!member.value) return
    editForm.username = member.value.username
    editForm.gender = member.value.gender || 'MALE'
    editForm.birthday = member.value.birthday
    editForm.style = member.value.style
    editForm.password = ''
    editForm.passwordConfirm = ''
    isEditMode.value = true
}

const cancelEdit = () => {
    if(confirm('수정을 취소할까요?')) isEditMode.value = false
}

const saveChanges = async () => {
    if (editForm.password && editForm.password !== editForm.passwordConfirm) {
        alert('비밀번호가 일치하지 않습니다.')
        return
    }
    
    try {
        isSaving.value = true
        const payload = {
        username: editForm.username,
        gender: editForm.gender,
        birthday: editForm.birthday,
        style: editForm.style,
        password: editForm.password || null
        }
        
        // 1. 수정 API 호출
        await authStore.updateProfile(payload)
        
        // 2. 화면 갱신
        const { data } = await fetchMyInfo()
        member.value = data
        authStore.user = data
        localStorage.setItem('user', JSON.stringify(data))
        
        alert('수정되었습니다.')
        isEditMode.value = false
    } catch (e) {
        console.error(e)
        alert('수정 실패')
    } finally {
        isSaving.value = false
    }
}

// 8. 탈퇴
const handleWithdraw = async () => {
    const input = prompt(`탈퇴하려면 본인의 이메일 [${member.value.email}]을 똑같이 입력하세요.`)
    if (input === member.value.email) {
        await authStore.withdraw(member.value.email)
        alert('탈퇴되었습니다.')
        router.push('/')
    } else if (input !== null) {
        alert('이메일이 일치하지 않습니다.')
    }
}

// 9. 성별 표시 헬퍼
const formatGender = (g) => g === 'MALE' ? '남성' : (g === 'FEMALE' ? '여성' : '비공개')

// 새로고침 후 직접 /mypage 들어온 경우를 대비해 localStorage에서 복원
onMounted(async () => {
    authStore.loadFromStorage()
    if (authStore.loginUser) {
        member.value = authStore.loginUser
    }

    try {
        const { data } = await fetchMyInfo() // GET /api/member/me
        member.value = data
    } catch (e) {
        if (!member.value) {
            member.value = fallbackMember
        }
        console.error('회원 정보 조회 실패:', e)
    } finally {
        loading.value = false
    }
})

watch(member, (nextMember) => {
    if (nextMember?.profileImg && !avatarUrl.value) {
        avatarUrl.value = nextMember.profileImg
    }
})

// 아바타에 쓸 이니셜
const initials = computed(() => {
    if (!member.value?.username) return ''
    const name = member.value.username.trim()
    return name.length >= 2 ? name.slice(0, 2) : name[0]
})

const goHome = () => {
    router.push({ name: 'home' })
}

const triggerAvatarUpload = () => {
    fileInputRef.value?.click()
}

const onAvatarSelect = (event) => {
    const file = event.target.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (loadEvent) => {
        avatarUrl.value = loadEvent.target?.result ?? ''
    }
    reader.readAsDataURL(file)
}

const onLogout = async () => {
    try {
        await logoutMember() // 세션 삭제 API 호출
    } catch (e) {
        console.error('로그아웃 실패(무시 가능):', e)
    }
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
    gap: 24px;
    margin-bottom: 18px;
}

.avatar-block {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
}

.avatar {
    flex-shrink: 0;
    width: 88px;
    height: 88px;
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

.avatar img {
    width: 100%;
    height: 100%;
    border-radius: 999px;
    object-fit: cover;
}

.avatar-input {
    display: none;
}

.btn.avatar-btn {
    width: auto;
    padding: 6px 12px;
    font-size: 0.8rem;
    border-radius: 999px;
    background: rgba(15, 23, 42, 0.65);
    color: #e5e7eb;
    border: 1px solid rgba(148, 163, 184, 0.6);
}

.btn.avatar-btn:hover {
    background: rgba(30, 64, 175, 0.65);
}

.user-main {
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.username {
    font-size: 1.4rem;
    font-weight: 800;
    color: #e5f0ff;
    margin: 0;
}

.email {
    font-size: 0.9rem;
    color: #94a3b8;
    margin: 0;
}

.badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    align-self: flex-start;
    padding: 2px 10px;
    margin: 4px 0 0;
    border-radius: 999px;
    font-size: 0.78rem;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: #bfdbfe;
    background: rgba(37, 99, 235, 0.22);
    border: 1px solid rgba(59, 130, 246, 0.7);
}

.badge.admin {
    color: #b91c1c;
    background: rgba(254, 226, 226, 0.15);
    border: 1px solid rgba(248, 113, 113, 0.7);
}

.divider {
    height: 1px;
    margin: 18px 0 16px;
    background: linear-gradient(to right, transparent, rgba(148, 163, 184, 0.6), transparent);
}

.info-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 18px 24px;
    margin-bottom: 22px;
}

.info-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 140px;
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
        flex-direction: column;
    }

    .actions {
        justify-content: flex-start;
        flex-wrap: wrap;
    }
}

.edit-input {
    background: rgba(15, 23, 42, 0.6);
    border: 1px solid rgba(148, 163, 184, 0.4);
    color: #fff;
    padding: 8px 12px;
    border-radius: 8px;
    font-size: 0.95rem;
    width: 100%;
    box-sizing: border-box; /* 레이아웃 깨짐 방지 */
}

.edit-input:focus {
    outline: none;
    border-color: #38bdf8;
    background: rgba(15, 23, 42, 0.9);
}
</style>
