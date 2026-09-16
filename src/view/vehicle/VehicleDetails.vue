<template>
  <div
    v-if="vehicle"
    class="vehicle-details-container"
  >
    <div class="header">
      <h1 class="page-title">차량 상세 정보</h1>

      <p class="page-subtitle">
        선택한 차량의 상세 스펙 정보입니다.
      </p>
    </div>

    <div class="details-card">
      <div class="detail-row">
        <span class="label">ID</span>

        <span class="value font-mono">
          {{ vehicle.id }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">차량 번호</span>

        <span class="value highlight-text">
          {{ vehicle.plateNumber }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">모델명</span>

        <span class="value">
          {{ vehicle.modelName }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">제조사</span>

        <span class="value">
          {{ vehicle.manufacturer }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">연식</span>

        <span class="value">
          {{ vehicle.modelYear }}년식
        </span>
      </div>

      <div class="detail-row">
        <span class="label">상태</span>

        <span :class="['value', 'status-badge', getStatusClass(vehicle.status)]">
          {{ getStatusText(vehicle.status) }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">등록일</span>

        <span class="value font-mono">
          {{ formatDateTime(vehicle.createdAt) }}
        </span>
      </div>
    </div>

    <div class="maintenance-section">
      <div class="section-header">
        <div>
          <h2 class="section-title">정비 이력</h2>
          <p class="section-subtitle">
            해당 차량의 정비 및 점검 이력입니다.
          </p>
        </div>
      </div>

      <div
        v-if="maintenances.length === 0"
        class="empty-maintenance"
      >
        등록된 정비 이력이 없습니다.
      </div>

      <div
        v-else
        class="maintenance-card"
      >
        <table class="maintenance-table">
          <thead>
            <tr>
              <th>정비일</th>
              <th>정비 유형</th>
              <th>내용</th>
              <th>비용</th>
              <th>다음 정비일</th>
            </tr>
          </thead>

          <tbody>
            <tr
              v-for="maintenance in maintenances"
              :key="maintenance.id"
            >
              <td class="font-mono">
                {{ formatDate(maintenance.maintenanceDate) }}
              </td>

              <td>
                <span class="maintenance-type">
                  {{ getMaintenanceType(maintenance.maintenanceType) }}
                </span>
              </td>

              <td>
                {{ maintenance.description || '-' }}
              </td>

              <td class="cost">
                {{ formatCost(maintenance.cost) }}
              </td>

              <td class="font-mono">
                {{ formatDate(maintenance.nextDueDate) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="actions">
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
    class="loading"
  >
    차량 정보를 불러오는 중입니다...
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

const emit = defineEmits([
  'back'
]);

const vehicle = ref(null);
const maintenances = ref([]);

const fetchVehicleDetail = async () => {
  try {
    const response = await api.get(`/vehicles/${props.id}`);

    vehicle.value = response.data;
  } catch (error) {
    console.error('차량 상세 정보 조회 실패:', error);

    alert('차량 정보를 불러오지 못했습니다.');
  }
};

const fetchMaintenances = async () => {
  try {
    const response = await api.get(
      `/maintenances/vehicle/${props.id}`
    );

    maintenances.value = response.data;
  } catch (error) {
    console.error('정비 이력 조회 실패:', error);

    maintenances.value = [];
  }
};

const getStatusClass = (status) => {
  switch (status) {
    case 'ACTIVE':
      return 'status-on';

    case 'MAINTENANCE':
      return 'status-danger';

    default:
      return 'status-off';
  }
};

const getStatusText = (status) => {
  switch (status) {
    case 'ACTIVE':
      return '운행 중';

    case 'INACTIVE':
      return '대기 중';

    case 'MAINTENANCE':
      return '정비';

    default:
      return status || '-';
  }
};

const getMaintenanceType = (type) => {
  switch (type) {
    case 'INSPECTION':
      return '점검';

    case 'REPAIR':
      return '수리';

    default:
      return type || '-';
  }
};

const formatDate = (dateString) => {
  if (!dateString) {
    return '-';
  }

  return dateString;
};

const formatDateTime = (dateString) => {
  if (!dateString) {
    return '-';
  }

  return dateString.replace('T', ' ').substring(0, 16);
};

const formatCost = (cost) => {
  if (cost === null || cost === undefined) {
    return '-';
  }

  return `${Number(cost).toLocaleString()}원`;
};

const goToList = () => {
  emit('back');
};

onMounted(async () => {
  await Promise.all([
    fetchVehicleDetail(),
    fetchMaintenances()
  ]);
});
</script>

<style scoped>
.vehicle-details-container {
  padding: var(--spacing-lg);
  max-width: 900px;
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
  margin-bottom: var(--spacing-xl);
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.highlight-text {
  color: #00a3ff;
}

.font-mono {
  color: var(--color-text-secondary);
  font-family: monospace;
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

.maintenance-section {
  margin-bottom: var(--spacing-xl);
}

.section-header {
  margin-bottom: var(--spacing-md);
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.section-subtitle {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.maintenance-card {
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-card);
  overflow-x: auto;
}

.maintenance-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.maintenance-table th {
  padding: 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  border-bottom: 1px solid var(--color-border);
}

.maintenance-table td {
  padding: 14px 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  border-bottom: 1px solid var(--color-border);
}

.maintenance-table tr:last-child td {
  border-bottom: none;
}

.maintenance-type {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 6px;
  background-color: var(--color-surface-light);
  color: var(--color-text-primary);
  font-size: 12px;
  font-weight: 600;
}

.cost {
  font-weight: 600;
}

.empty-maintenance {
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 32px;
  text-align: center;
  color: var(--color-text-secondary);
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.btn-secondary {
  background-color: var(--color-surface-light);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  padding: var(--spacing-md) var(--spacing-lg);
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