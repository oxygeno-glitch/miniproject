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
        <h1 class="auth-title">관제 시스템 회원가입</h1>
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