// /src/composables/useLogin.js
import { ref } from "vue";
import { useRouter } from "vue-router";
import { loginMember } from "@/api/memberApi";
import { useAuthStore } from "@/store/useAuthStore";

export function useLogin(options = {}) {
  const router = useRouter();
  const authStore = useAuthStore();
  const { onSuccess } = options; // ✅ 성공 시 실행할 콜백 (모달 닫기 등)

  const email = ref("");
  const password = ref("");
  const showPassword = ref(false);
  const error = ref("");
  const loading = ref(false);

  const resetForm = () => {
    email.value = "";
    password.value = "";
  };

  const onSubmit = async () => {
    error.value = "";

    if (!email.value || !password.value) {
      error.value = "이메일과 비밀번호를 모두 입력해 주세요.";
      return;
    }

    try {
      loading.value = true;

      const res = await loginMember(email.value, password.value);

      // 2xx + message === 'success' 인 경우만 성공 처리
      if (
        res.status >= 200 &&
        res.status < 300 &&
        res.data.message === "success"
      ) {
        const userInfo = res.data.userInfo;

        // ✅ Pinia에 유저 정보 저장 (전역 상태)
        authStore.setUser(userInfo);

        // ✅ 새로고침 대비를 위해 localStorage에도 저장
        localStorage.setItem("loginUser", JSON.stringify(userInfo));

        resetForm();

        // ✅ 모달에서 콜백을 넘겨줬다면, 우선 콜백 실행
        if (typeof onSuccess === "function") {
          onSuccess();
        } else {
          // 콜백이 없으면 기본 동작으로 홈으로 이동
          await router.push({ name: "home" });
        }
      } else {
        error.value = "이메일 또는 비밀번호를 다시 확인해 주세요.";
      }
    } catch (e) {
      if (e.response && e.response.status === 401) {
        error.value = "이메일 또는 비밀번호가 올바르지 않습니다.";
      } else {
        error.value =
          "로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
      }
    } finally {
      loading.value = false;
    }
  };

  const goHome = () => {
    router.push({ name: "home" });
  };

  const togglePassword = () => {
    showPassword.value = !showPassword.value;
  };

  return {
    email,
    password,
    showPassword,
    error,
    loading,
    onSubmit,
    goHome,
    togglePassword,
  };
}
