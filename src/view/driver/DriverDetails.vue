<script setup>
import { ref, onMounted } from 'vue';
import api from '../../api';

const props = defineProps({
  id: {
    type: [String, Number],
    required: true
  }
});

const emit = defineEmits([
  'back'
]);

const driver = ref(null);

const fetchDriverDetail = async () => {
  try {
    const response = await api.get(`/drivers/${props.id}`);
    driver.value = response.data;
  } catch (error) {
    console.error('기사 상세 정보 조회 실패:', error);
    alert('기사 정보를 불러오지 못했습니다.');
  }
};

// 상태 값을 사용자가 읽기 좋은 한글과 클래스로 매핑
const getStatusLabel = (status) => {
  switch (status) {
    case 'ON_DUTY': return '근무 중';
    case 'OFF_DUTY': return '휴무 / 대기';
    case 'RETIRED': return '퇴사';
    default: return status;
  }
};

const getStatusClass = (status) => {
  switch (status) {
    case 'ON_DUTY': return 'status-on';
    case 'OFF_DUTY': return 'status-off';
    case 'RETIRED': return 'status-danger';
    default: return '';
  }
};

const goToList = () => {
  emit('back');
};

onMounted(() => {
  fetchDriverDetail();
});
</script>

<template>
  <div
    v-if="driver"
    class="driver-details-container"
  >
    <div class="header">
      <div>
        <h1 class="page-title">기사 상세 정보</h1>
        <p class="page-subtitle">
          선택한 기사의 상세 정보입니다.
        </p>
      </div>
    </div>

    <div class="details-card">
      <div class="info-grid">
        <div class="info-item">
          <span class="label">ID</span>
          <span class="value font-mono">
            {{ driver.id }}
          </span>
        </div>

        <div class="info-item">
          <span class="label">이름</span>
          <span class="value">
            {{ driver.name }}
          </span>
        </div>

        <div class="info-item">
          <span class="label">면허 번호</span>
          <span class="value">
            {{ driver.licenseNumber }}
          </span>
        </div>

        <div class="info-item">
          <span class="label">면허 종류</span>
          <span class="value">
            {{ driver.licenseType }}
          </span>
        </div>

        <div class="info-item">
          <span class="label">연락처</span>
          <span class="value">
            {{ driver.phoneNumber }}
          </span>
        </div>

        <div class="info-item">
          <span class="label">상태</span>
          <span class="value">
            <span :class="['status-badge', getStatusClass(driver.workStatus)]">
              {{ getStatusLabel(driver.workStatus) }}
            </span>
          </span>
        </div>
      </div>
    </div>

    <div class="card-actions">
      <button
        type="button"
        class="btn-secondary"
        @click="goToList"
      >
        목록으로 돌아가기
      </button>
    </div>
  </div>

  <div
    v-else
    class="loading-msg"
  >
    기사 정보를 불러오는 중입니다...
  </div>
</template>

<style scoped>
.driver-details-container {
  padding: var(--spacing-lg, 24px);
  max-width: 600px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--spacing-xl, 24px);
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary, #ffffff);
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary, #94a3b8);
  margin-top: 4px;
}

.details-card {
  background-color: var(--color-surface, #1e293b);
  border: 1px solid var(--color-border, #334155);
  border-radius: var(--radius-lg, 12px);
  padding: var(--spacing-lg, 24px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.info-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border, #334155);
}

.info-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.label {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary, #94a3b8);
}

.value {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary, #ffffff);
}

.font-mono {
  color: var(--color-text-secondary, #94a3b8);
}

.status-badge {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.status-on {
  background-color: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.status-off {
  background-color: rgba(148, 163, 184, 0.15);
  color: #94a3b8;
}

.status-danger {
  background-color: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.card-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

.btn-secondary {
  background-color: var(--color-surface-light, #334155);
  color: var(--color-text-primary, #ffffff);
  border: 1px solid var(--color-border, #475569);
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border-radius: var(--radius-md, 6px);
  transition: all 0.2s ease;
}

.btn-secondary:hover {
  background-color: #475569;
  border-color: #64748b;
  color: #ffffff;
}

.loading-msg {
  text-align: center;
  padding: 48px;
  color: var(--color-text-secondary, #94a3b8);
}
</style>