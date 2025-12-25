import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { 
    loginMember,
    logoutMember,
    fetchMyInfo, 
    updateMember,
    deleteMember,
} from '@/api/memberApi'

export const useAuthStore = defineStore('auth', () => {
    // 1. 상태 (State)
    const accessToken = ref('')
    const refreshToken = ref('')
    const user = ref(null) // 사용자 정보 (username, role 등)

    // 2. 게터 (Getters)
    // 토큰이 있으면 로그인된 것으로 간주
    const isLoggedIn = computed(() => !!accessToken.value)
    const userName = computed(() => user.value?.username || '')

    // 3. 액션 (Actions)

    // 로그인 처리
    const login = async (email, password) => {
        try {
            // API 호출
            const { data } = await loginMember(email, password)
            
            // 응답 구조: { accessToken, refreshToken, userInfo, message }
            if (data.accessToken) {
                // 상태 업데이트
                accessToken.value = data.accessToken
                refreshToken.value = data.refreshToken
                user.value = data.userInfo
                
                // 로컬 스토리지에 저장 (새로고침 시 유지를 위해)
                localStorage.setItem('accessToken', data.accessToken)
                localStorage.setItem('refreshToken', data.refreshToken)
                localStorage.setItem('user', JSON.stringify(data.userInfo))
            }
            return true
        } catch (error) {
            console.error('로그인 실패:', error)
            throw error // 컴포넌트로 에러 전파 (UI에서 에러 메시지 표시용)
        }
    }

    // 로그아웃 처리
    const logout = async () => {
        try {
            // 서버에 로그아웃 요청 (리프레시 토큰 삭제 등 필요 시)
            // await logoutMember() 
        } catch (e) {
            console.error(e)
        } finally {
            // 클라이언트 상태 초기화
            accessToken.value = ''
            refreshToken.value = ''
            user.value = null
            
            // 스토리지 삭제
            localStorage.removeItem('accessToken')
            localStorage.removeItem('refreshToken')
            localStorage.removeItem('user')
        }
    }

    // 앱 시작(새로고침) 시 스토리지에서 정보 복원
    const loadFromStorage = () => {
        const token = localStorage.getItem('accessToken')
        const refresh = localStorage.getItem('refreshToken')
        const storedUser = localStorage.getItem('user')

        if (token) accessToken.value = token
        if (refresh) refreshToken.value = refresh
        if (storedUser) {
            try {
                user.value = JSON.parse(storedUser)
            } catch (e) {
                console.error('사용자 정보 파싱 오류', e)
                // 파싱 실패 시 잘못된 데이터이므로 삭제
                localStorage.removeItem('user')
            }
        }
    }

    // 토큰 갱신
    const updateTokens = (newAccess, newRefresh) => {
        accessToken.value = newAccess
        refreshToken.value = newRefresh
        localStorage.setItem('accessToken', newAccess)
        localStorage.setItem('refreshToken', newRefresh)
    }

    // 회원 정보 수정
    const updateProfile = async (updateData) => {
        // 1. 서버에 수정 요청
        await updateMember(updateData)

        // 2. 수정 성공 시, 변경된 정보를 다시 조회하여 스토어/스토리지 갱신
        // (토큰 재발급 대신 fetchMyInfo를 통해 최신 User 객체를 확보하여 UI 반영)
        const { data } = await fetchMyInfo()
        if (data) {
            user.value = data
            localStorage.setItem('user', JSON.stringify(data))
        }
    }

    // 회원 탈퇴
    const withdraw = async (email) => {
        // 1. 서버에 탈퇴 요청 (DB 삭제 + Redis 토큰 삭제)
        await deleteMember(email)
        
        // 2. 클라이언트 측 정보 삭제 (로그아웃 처리)
        await logout()
    }

    return {
        accessToken,
        refreshToken,
        user,
        isLoggedIn,
        userName,
        login,
        logout,
        loadFromStorage,
        updateTokens,
        updateProfile,
        withdraw
    }
})