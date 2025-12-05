// src/store/useAuthStore.js
import { defineStore } from "pinia";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    loginUser: null,
    isLoggedIn: false,
  }),
  getters: {
    userName: (state) => state.loginUser?.nickname ?? "",
  },
  actions: {
    setUser(user) {
      this.loginUser = user;
      this.isLoggedIn = !!user;
    },
    logout() {
      this.loginUser = null;
      this.isLoggedIn = false;
      localStorage.removeItem("loginUser");
    },
    loadFromStorage() {
      const saved = localStorage.getItem("loginUser");
      if (saved) {
        this.loginUser = JSON.parse(saved);
        this.isLoggedIn = true;
      }
    },
  },
});
