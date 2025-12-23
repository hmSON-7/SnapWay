import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginMember, logoutMember } from '@/api/memberApi'

export const useAuthStore = defineStore('auth', () => {
    // 1. 상태 (State)
    const accessToken = ref('')
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
        const storedUser = localStorage.getItem('user')

        if (token) {
            accessToken.value = token
        }
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

    return {
        accessToken,
        user,
        isLoggedIn,
        userName,
        login,
        logout,
        loadFromStorage
    }
})