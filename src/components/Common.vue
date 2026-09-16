<script setup>
import { useRouter } from 'vue-router';
import '../style.css';

const router = useRouter();

const goToLoginPage = () => {
  router.push('/login');
};
</script>

<template>
  <div class="main-wrapper">
    <div class="video-background-container">
      <video class="video-bg" autoplay loop muted playsinline>
        <source src="../assets/main.mp4" type="video/mp4" />
      </video>
      <div class="video-overlay"></div>
    </div>

    <div class="center-content">
      <div class="logo-tooltip-container" @click="goToLoginPage">
        <img src="../assets/main_sec1_img1.png" alt="logo" class="nav-logo" />
        <div class="tooltip">
          <span class="live-dot"></span>
          대시보드 로그인
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.main-wrapper {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background-color: #0b131e;
}

/* 배경 비디오 컨테이너 */
.video-background-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.video-bg {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(11, 19, 30, 0.6);
  z-index: 2;
}

/* 정중앙 콘텐츠 (화면 전체를 기준으로 완벽한 가운데 정렬) */
.center-content {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.logo-tooltip-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  gap: 16px;
  transition: transform 0.3s ease;
}

.logo-tooltip-container:hover {
  transform: scale(1.05);
}

.nav-logo {
  width: 180px;
  height: auto;
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.5));
}

/* 툴팁 디자인 */
.tooltip {
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: rgba(21, 31, 46, 0.85);
  border: 1px solid rgba(59, 130, 246, 0.4);
  padding: 8px 16px;
  border-radius: 20px;
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(4px);
}

/* 라이브 애니메이션 닷 */
.live-dot {
  width: 8px;
  height: 8px;
  background-color: #34d399;
  border-radius: 50%;
  box-shadow: 0 0 8px #34d399;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(52, 211, 153, 0.7);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 6px rgba(52, 211, 153, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(52, 211, 153, 0);
  }
}
</style>