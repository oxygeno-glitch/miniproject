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

        <button
          type="button"
          class="btn-add-maintenance"
          @click="openMaintenanceModal"
        >
          정비 등록
        </button>
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

    <div
      v-if="showMaintenanceModal"
      class="modal-overlay"
      @click.self="closeMaintenanceModal"
    >
      <div class="maintenance-modal">
        <div class="modal-header">
          <h3>정비 이력 등록</h3>

          <button
            type="button"
            class="close-button"
            @click="closeMaintenanceModal"
          >
            ×
          </button>
        </div>

        <form
          class="maintenance-form"
          @submit.prevent="submitMaintenance"
        >
          <div class="form-group">
            <label>정비 유형</label>

            <select v-model="maintenanceForm.maintenanceType" required>
              <option value="">선택하세요</option>
              <option value="INSPECTION">점검</option>
              <option value="REPAIR">수리</option>
            </select>
          </div>

          <div class="form-group">
            <label>내용</label>

            <textarea
              v-model="maintenanceForm.description"
              rows="3"
              placeholder="정비 내용을 입력하세요"
            ></textarea>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>비용 (원)</label>

              <input
                v-model.number="maintenanceForm.cost"
                type="number"
                min="0"
                placeholder="0"
              />
            </div>

            <div class="form-group">
              <label>정비일</label>

              <input
                v-model="maintenanceForm.maintenanceDate"
                type="date"
                required
              />
            </div>

            <div class="form-group">
              <label>다음 정비 예정일</label>

              <input
                v-model="maintenanceForm.nextDueDate"
                type="date"
              />
            </div>
          </div>

          <div class="modal-actions">
            <button
              type="button"
              class="btn-secondary"
              @click="closeMaintenanceModal"
              :disabled="isSubmittingMaintenance"
            >
              취소
            </button>

            <button
              type="submit"
              class="btn-primary"
              :disabled="isSubmittingMaintenance"
            >
              {{ isSubmittingMaintenance ? '등록 중...' : '등록하기' }}
            </button>
          </div>
        </form>
      </div>
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
import { ref, reactive, onMounted } from 'vue';
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

const showMaintenanceModal = ref(false);
const isSubmittingMaintenance = ref(false);

const maintenanceForm = reactive({
  maintenanceType: '',
  description: '',
  cost: null,
  maintenanceDate: '',
  nextDueDate: ''
});

const resetMaintenanceForm = () => {
  maintenanceForm.maintenanceType = '';
  maintenanceForm.description = '';
  maintenanceForm.cost = null;
  maintenanceForm.maintenanceDate = '';
  maintenanceForm.nextDueDate = '';
};

const openMaintenanceModal = () => {
  resetMaintenanceForm();
  showMaintenanceModal.value = true;
};

const closeMaintenanceModal = () => {
  if (isSubmittingMaintenance.value) return;
  showMaintenanceModal.value = false;
};

const submitMaintenance = async () => {
  if (isSubmittingMaintenance.value) return;

  if (!maintenanceForm.maintenanceType) {
    alert('정비 유형을 선택해주세요.');
    return;
  }

  if (!maintenanceForm.maintenanceDate) {
    alert('정비일을 입력해주세요.');
    return;
  }

  try {
    isSubmittingMaintenance.value = true;

    await api.post('/maintenances', {
      vehicleId: Number(props.id),
      maintenanceType: maintenanceForm.maintenanceType,
      description: maintenanceForm.description,
      cost: maintenanceForm.cost,
      maintenanceDate: maintenanceForm.maintenanceDate,
      nextDueDate: maintenanceForm.nextDueDate || null
    });

    alert('정비 이력이 등록되었습니다.');

    showMaintenanceModal.value = false;
    await fetchMaintenances();
  } catch (error) {
    console.error('정비 이력 등록 실패:', error);

    alert(
      error.response?.data?.message ||
      '정비 이력 등록에 실패했습니다.'
    );
  } finally {
    isSubmittingMaintenance.value = false;
  }
};

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
      return '운행 대기 중';

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
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--spacing-md);
  gap: var(--spacing-md);
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

.btn-add-maintenance {
  background-color: var(--color-surface-light);
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
  padding: 9px 14px;
  font-size: 13px;
  font-weight: 600;
  border-radius: var(--radius-md);
  cursor: pointer;
  white-space: nowrap;
}

.btn-add-maintenance:hover {
  background-color: var(--color-primary);
  color: white;
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

.modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(2, 6, 23, 0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.maintenance-modal {
  width: 480px;
  max-width: calc(100vw - 40px);
  max-height: 85vh;
  overflow-y: auto;
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-modal);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--color-border);
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--color-text-primary);
}

.close-button {
  border: none;
  background: transparent;
  font-size: 26px;
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: color 0.2s;
}

.close-button:hover {
  color: var(--color-text-primary);
}

.maintenance-form {
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background-color: var(--color-surface-light);
  color: var(--color-text-primary);
  font-size: 14px;
  outline: none;
  font-family: inherit;
  resize: vertical;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: var(--color-primary);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: var(--spacing-md);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
}

.modal-actions .btn-primary {
  background: var(--color-primary-gradient);
  color: white;
  border: none;
  padding: 10px 18px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border-radius: var(--radius-md);
}

.modal-actions .btn-primary:hover:not(:disabled) {
  opacity: 0.9;
}

.modal-actions button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 600px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>