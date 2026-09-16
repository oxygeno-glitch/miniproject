<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import api from '../api';

const router = useRouter();

const account = ref('');
const password = ref('');

const handleLogin = async () => {
  try {
    const response = await api.post('/auth/login', {
      account: account.value,
      password: password.value
    });

    sessionStorage.setItem('isLoggedIn', 'true');

    if (response.data) {
      sessionStorage.setItem(
        'loginUser',
        JSON.stringify(response.data)
      );
    }

    alert('로그인 성공!');
    router.push('/dashboard');
  } catch (error) {
    console.error('로그인 실패:', error);
    alert(
      error.response?.data ||
      '아이디 또는 비밀번호를 확인해주세요.'
    );
  }
};

const goToSignUp = () => {
  router.push('/signup');
};
</script>

<template>
  <div class="page-center-wrapper">
    <div class="base-card">
      <div class="auth-header">
        <img
          src="../assets/main_sec1_img1.png"
          alt="StudioGalilei Logo"
          class="auth-logo"
        />

        <h1 class="auth-title">
          모빌리티 관제 시스템
        </h1>

        <p class="auth-subtitle">
          계정 정보를 입력하여 로그인하세요
        </p>
      </div>

      <form
        class="base-form"
        @submit.prevent="handleLogin"
      >
        <div class="input-group">
          <label
            class="input-label"
            for="account"
          >
            계정 아이디
          </label>

          <input
            id="account"
            v-model="account"
            type="text"
            class="base-input"
            placeholder="아이디를 입력하세요"
            autocomplete="username"
            required
          />
        </div>

        <div class="input-group">
          <label
            class="input-label"
            for="password"
          >
            비밀번호
          </label>

          <input
            id="password"
            v-model="password"
            type="password"
            class="base-input"
            placeholder="비밀번호를 입력하세요"
            autocomplete="current-password"
            required
          />
        </div>

        <button
          type="submit"
          class="btn-primary"
        >
          로그인
        </button>
      </form>

      <div class="auth-prompt">
        <span>아직 계정이 없으신가요?</span>

        <button
          type="button"
          class="auth-link-btn"
          @click="goToSignUp"
        >
          회원가입
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-center-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #0b131e;
  color: #ffffff;
}

.base-card {
  width: 100%;
  max-width: 420px;
  padding: 40px;
  background-color: #151f2e;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
}

.auth-header {
  text-align: center;
  margin-bottom: 30px;
}

.auth-logo {
  width: 80px;
  height: auto;
  margin-bottom: 16px;
}

.auth-title {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 8px;
}

.auth-subtitle {
  font-size: 14px;
  color: #94a3b8;
}

.base-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-label {
  font-size: 13px;
  color: #cbd5e1;
}

.base-input {
  width: 100%;
  padding: 12px 16px;
  background-color: #0b131e;
  border: 1px solid #334155;
  border-radius: 8px;
  color: #ffffff;
  font-size: 14px;
  box-sizing: border-box;
}

.base-input:focus {
  outline: none;
  border-color: #3b82f6;
}

.btn-primary {
  width: 100%;
  padding: 12px;
  background-color: #2563eb;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  margin-top: 10px;
}

.btn-primary:hover {
  background-color: #1d4ed8;
}

.auth-prompt {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 24px;
  font-size: 14px;
  color: #94a3b8;
}

.auth-link-btn {
  background: none;
  border: none;
  color: #60a5fa;
  cursor: pointer;
  font-size: 14px;
  font-weight: bold;
}

.auth-link-btn:hover {
  text-decoration: underline;
}
</style>