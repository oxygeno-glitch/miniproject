<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import api from '../api';

const router = useRouter();

const regName = ref('');
const regAccount = ref('');
const regPassword = ref('');
const regPasswordConfirm = ref('');

const handleRegister = async () => {
  if (regPassword.value !== regPasswordConfirm.value) {
    alert('비밀번호가 일치하지 않습니다.');
    return;
  }

  try {
    await api.post('/auth/signup', {
      name: regName.value,
      account: regAccount.value,
      password: regPassword.value
    });

    alert('회원가입이 완료되었습니다. 로그인해주세요.');
    router.push('/login');
  } catch (error) {
    console.error('회원가입 실패:', error);
    alert(error.response?.data || '회원가입 중 오류가 발생했습니다.');
  }
};

const goToLogin = () => {
  router.push('/login');
};
</script>

<template>
  <div class="page-center-wrapper">
    <div class="base-card">
      <div class="auth-header">
        <img src="../assets/main_sec1_img1.png" alt="StudioGalilei Logo" class="auth-logo" />
        <span class="brand-kicker">STUDIO G</span>
        <h1 class="auth-title"><span class="brand-gradient-text">관제 시스템</span> 회원가입</h1>
        <p class="auth-subtitle">필요한 정보를 입력하여 계정을 신청하세요</p>
      </div>

      <form class="base-form" @submit.prevent="handleRegister">
        <div class="input-group">
          <label class="input-label" for="regName">이름</label>
          <input
            id="regName"
            v-model="regName"
            type="text"
            class="base-input"
            placeholder="이름을 입력하세요"
            required
          />
        </div>

        <div class="input-group">
          <label class="input-label" for="regAccount">아이디</label>
          <input
            id="regAccount"
            v-model="regAccount"
            type="text"
            class="base-input"
            placeholder="사용할 아이디를 입력하세요"
            required
          />
        </div>

        <div class="input-group">
          <label class="input-label" for="regPassword">비밀번호</label>
          <input
            id="regPassword"
            v-model="regPassword"
            type="password"
            class="base-input"
            placeholder="비밀번호를 입력하세요"
            required
          />
        </div>

        <div class="input-group">
          <label class="input-label" for="regPasswordConfirm">비밀번호 확인</label>
          <input
            id="regPasswordConfirm"
            v-model="regPasswordConfirm"
            type="password"
            class="base-input"
            placeholder="비밀번호를 다시 입력하세요"
            required
          />
        </div>

        <div class="form-actions">
          <button type="submit" class="btn-primary">가입 요청</button>
        </div>
      </form>

      <div class="auth-prompt">
        <span>이미 계정이 있으신가요?</span>
        <button type="button" class="auth-link-btn" @click="goToLogin">
          로그인하러 가기
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
  background-color: var(--color-background);
  color: var(--color-text-primary);
}

.base-card {
  width: 100%;
  max-width: 420px;
  padding: 40px;
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-modal);
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

.auth-header .brand-kicker {
  display: block;
  margin-bottom: 10px;
}

.auth-title {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: var(--tracking-tight);
  margin-bottom: 8px;
}

.auth-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.base-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.input-label {
  font-size: 13px;
  color: #cbd5e1;
}

.base-input {
  width: 100%;
  padding: 12px 16px;
  background-color: var(--color-background);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-primary);
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.base-input:focus {
  outline: none;
  border-color: var(--color-accent-light);
  box-shadow: 0 0 0 3px rgba(0, 163, 196, 0.2);
}

.btn-primary {
  width: 100%;
  padding: 12px;
  background: var(--color-primary-gradient);
  color: #ffffff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  margin-top: 10px;
  transition: opacity 0.2s;
}

.btn-primary:hover {
  opacity: 0.9;
}

.auth-prompt {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 24px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.auth-link-btn {
  background: none;
  border: none;
  color: var(--color-accent-light);
  cursor: pointer;
  font-size: 14px;
  font-weight: bold;
}

.auth-link-btn:hover {
  text-decoration: underline;
}
</style>