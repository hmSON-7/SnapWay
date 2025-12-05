// /src/composables/useLogin.js
import { ref } from "vue";
import { useRouter } from "vue-router";
import { loginMember } from "@/api/memberApi";
import { useAuthStore } from "@/store/useAuthStore";

export function useLogin() {
  const router = useRouter();
  const authStore = useAuthStore();

  const email = ref("");
  const password = ref("");
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

      if (res.data.message === "success") {
        // ✅ 백엔드에서 온 유저 정보(JSON 객체)
        const userInfo = res.data.userInfo;

        // ✅ Pinia에 유저 정보 저장 (전역 상태)
        authStore.setUser(userInfo);

        // ✅ 새로고침 대비를 위해 localStorage에도 저장
        localStorage.setItem("loginUser", JSON.stringify(userInfo));

        resetForm();

        await router.push({ name: "home" });
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

  return {
    email,
    password,
    error,
    loading,
    onSubmit,
    goHome,
  };
}
