<template>
  <div v-if="dispatch" class="dispatch-details-container">
    <div class="header">
      <h1 class="page-title">배차 상세 정보</h1>
      <p class="page-subtitle">
        선택한 배차의 상세 정보입니다.
      </p>
    </div>

    <div class="details-card">
      <div class="detail-row">
        <span class="label">배차 ID</span>
        <span class="value font-mono">
          {{ dispatch.id }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">노선</span>

        <div class="value route-info">
          <span class="route-number">
            {{ dispatch.routeNumber }}
          </span>

          <span class="route-name">
            {{ dispatch.routeName || '-' }}
          </span>
        </div>
      </div>

      <div class="detail-row">
        <span class="label">차량</span>
        <span class="value">
          {{ vehicleName }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">기사</span>
        <span class="value">
          {{ driverName }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">예정 출발</span>
        <span class="value font-mono">
          {{ formatDateTime(dispatch.plannedStartTime) }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">예정 도착</span>
        <span class="value font-mono">
          {{ formatDateTime(dispatch.plannedEndTime) }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">실제 출발</span>
        <span class="value font-mono">
          {{ formatDateTime(dispatch.actualStartTime) }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">실제 도착</span>
        <span class="value font-mono">
          {{ formatDateTime(dispatch.actualEndTime) }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">배차 상태</span>

        <span
          :class="[
            'status-badge',
            getStatusClass(dispatch.dispatchStatus)
          ]"
        >
          {{ getStatusText(dispatch.dispatchStatus) }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">등록일</span>
        <span class="value font-mono">
          {{ formatDateTime(dispatch.createdAt) }}
        </span>
      </div>
    </div>

    <div class="actions">
      <button
        type="button"
        class="btn-secondary"
        @click="goBack"
      >
        목록으로 돌아가기
      </button>
    </div>
  </div>

  <div v-else class="loading">
    배차 정보를 불러오는 중입니다...
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import api from '../../api';

const props = defineProps({
  id: {
    type: [String, Number],
    required: true
  }
});

const emit = defineEmits(['back']);

const dispatch = ref(null);
const vehicleName = ref('-');
const driverName = ref('-');

const fetchDispatchDetail = async () => {
  try {
    const response = await api.get(
      `/dispatches/${props.id}`
    );

    dispatch.value = response.data;

    await Promise.all([
      fetchVehicle(dispatch.value.vehicleId),
      fetchDriver(dispatch.value.driverId)
    ]);
  } catch (error) {
    console.error('배차 상세 정보 조회 실패:', error);
    alert('배차 정보를 불러오지 못했습니다.');
  }
};

const fetchVehicle = async (id) => {
  try {
    const response = await api.get(`/vehicles/${id}`);

    vehicleName.value =
      response.data.plateNumber ||
      `차량 #${id}`;
  } catch (error) {
    console.error('차량 정보 조회 실패:', error);
    vehicleName.value = `차량 #${id}`;
  }
};

const fetchDriver = async (id) => {
  try {
    const response = await api.get(`/drivers/${id}`);

    driverName.value =
      response.data.name ||
      response.data.driverName ||
      `기사 #${id}`;
  } catch (error) {
    console.error('기사 정보 조회 실패:', error);
    driverName.value = `기사 #${id}`;
  }
};

const getStatusText = (status) => {
  switch (status) {
    case 'SCHEDULED':
      return '배차 대기중';
    case 'IN_PROGRESS':
      return '운행 중';
    case 'COMPLETED':
      return '운행 종료';
    case 'CANCELED':
      return '배차 취소';
    default:
      return status || '-';
  }
};

const getStatusClass = (status) => {
  switch (status) {
    case 'SCHEDULED':
      return 'status-wait';
    case 'IN_PROGRESS':
      return 'status-on';
    case 'COMPLETED':
      return 'status-complete';
    case 'CANCELED':
      return 'status-danger';
    default:
      return 'status-off';
  }
};

const formatDateTime = (dateString) => {
  if (!dateString) {
    return '-';
  }

  return dateString
    .replace('T', ' ')
    .substring(0, 16);
};

const goBack = () => {
  emit('back');
};

onMounted(() => {
  fetchDispatchDetail();
});
</script>

<style scoped>
.dispatch-details-container {
  padding: var(--spacing-lg);
  max-width: 1000px;
  margin: 0 auto;
}

.header {
  margin-bottom: var(--spacing-xl);
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-top: var(--spacing-xs);
}

.details-card {
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-card);
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 52px;
  padding: var(--spacing-md) 0;
  border-bottom: 1px solid var(--color-border);
}

.detail-row:last-child {
  border-bottom: none;
}

.label {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.value {
  color: var(--color-text-primary);
  font-size: 15px;
  font-weight: 600;
}

.font-mono {
  color: var(--color-text-secondary);
  font-family: monospace;
}

.route-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.route-number {
  color: #00a3ff;
  font-weight: 700;
}

.route-name {
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 400;
}

.status-badge {
  display: inline-block;
  min-width: 90px;
  padding: 6px 12px;
  border-radius: 20px;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
}

.status-wait {
  background-color: rgba(234, 179, 8, 0.15);
  color: #eab308;
}

.status-on {
  background-color: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.status-complete {
  background-color: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
}

.status-danger {
  background-color: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.status-off {
  background-color: rgba(148, 163, 184, 0.15);
  color: #94a3b8;
}

.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--spacing-xl);
}

.btn-secondary {
  background-color: var(--color-surface-light);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  padding: 11px 20px;
  font-size: 14px;
  cursor: pointer;
  border-radius: var(--radius-md);
}

.btn-secondary:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.loading {
  text-align: center;
  padding: var(--spacing-xxl);
  color: var(--color-text-secondary);
}
</style>