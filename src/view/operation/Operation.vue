<template>
  <div class="operation-container">
    <div class="header">
      <div>
        <h1 class="page-title">운행 관리</h1>
        <p class="page-subtitle">
          현재 배차의 운행 상태를 관리합니다.
        </p>
      </div>

      <button type="button" class="btn-refresh" @click="fetchData">
        새로고침
      </button>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <span class="summary-label">전체 배차</span>
        <strong class="summary-value">{{ dispatches.length }}</strong>
      </div>

      <div class="summary-card">
        <span class="summary-label">배차 대기</span>
        <strong class="summary-value wait">{{ scheduledCount }}</strong>
      </div>

      <div class="summary-card">
        <span class="summary-label">운행 중</span>
        <strong class="summary-value active">{{ inProgressCount }}</strong>
      </div>

      <div class="summary-card">
        <span class="summary-label">운행 종료</span>
        <strong class="summary-value complete">{{ completedCount }}</strong>
      </div>
    </div>

    <div class="table-card">
      <div class="table-header">
        <h2>오늘의 운행 목록</h2>
      </div>

      <table class="operation-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>노선</th>
            <th>차량</th>
            <th>기사</th>
            <th>예정 출발</th>
            <th>예정 도착</th>
            <th>운행 상태</th>
            <th>관리</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="dispatch in dispatches"
            :key="dispatch.id"
          >
            <td class="font-mono">
              {{ dispatch.id }}
            </td>

            <td>
              <div class="route-info">
                <span class="route-number">
                  {{ dispatch.routeNumber }}
                </span>
                <span class="route-name">
                  {{ dispatch.routeName || '-' }}
                </span>
              </div>
            </td>

            <td>
              {{ vehicleMap[dispatch.vehicleId] || `차량 #${dispatch.vehicleId}` }}
            </td>

            <td>
              {{ driverMap[dispatch.driverId] || `기사 #${dispatch.driverId}` }}
            </td>

            <td class="font-mono">
              {{ formatDateTime(dispatch.plannedStartTime) }}
            </td>

            <td class="font-mono">
              {{ formatDateTime(dispatch.plannedEndTime) }}
            </td>

            <td>
              <span
                :class="[
                  'status-badge',
                  getStatusClass(dispatch.dispatchStatus)
                ]"
              >
                {{ getStatusText(dispatch.dispatchStatus) }}
              </span>
            </td>

            <td>
              <div class="action-buttons">
                <button
                  v-if="dispatch.dispatchStatus === 'SCHEDULED'"
                  type="button"
                  class="btn-start"
                  @click="startOperation(dispatch)"
                >
                  운행 시작
                </button>

                <button
                  v-if="dispatch.dispatchStatus === 'SCHEDULED'"
                  type="button"
                  class="btn-cancel"
                  @click="cancelDispatch(dispatch)"
                >
                  취소
                </button>

                <button
                  v-if="dispatch.dispatchStatus === 'IN_PROGRESS'"
                  type="button"
                  class="btn-complete"
                  @click="completeOperation(dispatch)"
                >
                  운행 종료
                </button>

                <span
                  v-if="dispatch.dispatchStatus === 'COMPLETED'"
                  class="finished-text"
                >
                  운행 완료
                </span>

                <span
                  v-if="dispatch.dispatchStatus === 'CANCELED'"
                  class="canceled-text"
                >
                  취소됨
                </span>
              </div>
            </td>
          </tr>

          <tr v-if="dispatches.length === 0">
            <td colspan="8" class="empty-msg">
              등록된 배차가 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import api from '../../api';

const dispatches = ref([]);
const vehicleMap = reactive({});
const driverMap = reactive({});

const scheduledCount = computed(() =>
  dispatches.value.filter(
    dispatch => dispatch.dispatchStatus === 'SCHEDULED'
  ).length
);

const inProgressCount = computed(() =>
  dispatches.value.filter(
    dispatch => dispatch.dispatchStatus === 'IN_PROGRESS'
  ).length
);

const completedCount = computed(() =>
  dispatches.value.filter(
    dispatch => dispatch.dispatchStatus === 'COMPLETED'
  ).length
);

const fetchData = async () => {
  try {
    const [dispatchResponse, vehicleResponse, driverResponse] =
      await Promise.all([
        api.get('/dispatches'),
        api.get('/vehicles'),
        api.get('/drivers')
      ]);

    dispatches.value = dispatchResponse.data;

    Object.keys(vehicleMap).forEach(key => delete vehicleMap[key]);
    Object.keys(driverMap).forEach(key => delete driverMap[key]);

    vehicleResponse.data.forEach(vehicle => {
      vehicleMap[vehicle.id] = vehicle.plateNumber;
    });

    driverResponse.data.forEach(driver => {
      driverMap[driver.id] = driver.name || `기사 #${driver.id}`;
    });
  } catch (error) {
    console.error('운행 데이터 조회 실패:', error);
    alert('운행 정보를 불러오지 못했습니다.');
  }
};

const updateDispatchStatus = async (dispatch, status) => {
  try {
    const response = await api.patch(
      `/dispatches/${dispatch.id}/status`,
      {
        dispatchStatus: status
      }
    );

    dispatches.value = dispatches.value.map(item =>
      item.id === dispatch.id ? response.data : item
    );

    return response.data;
  } catch (error) {
    console.error('배차 상태 변경 실패:', error);
    alert(
      error.response?.data?.message ||
      '배차 상태 변경에 실패했습니다.'
    );
    return null;
  }
};

const updateDriverStatus = async (driverId, workStatus) => {
  try {
    await api.patch(`/drivers/${driverId}/status`, {
      workStatus
    });

    return true;
  } catch (error) {
    console.error('기사 상태 변경 실패:', error);
    alert(
      error.response?.data?.message ||
      '기사 근무 상태 변경에 실패했습니다.'
    );
    return false;
  }
};

const startOperation = async (dispatch) => {
  const confirmed = confirm(
    `${driverMap[dispatch.driverId] || `기사 #${dispatch.driverId}`} 기사의 운행을 시작하시겠습니까?`
  );

  if (!confirmed) return;

  const updatedDispatch = await updateDispatchStatus(
    dispatch,
    'IN_PROGRESS'
  );

  if (!updatedDispatch) return;

  const updatedDriver = await updateDriverStatus(
    dispatch.driverId,
    'ON_DUTY'
  );

  if (!updatedDriver) {
    await updateDispatchStatus(dispatch, 'SCHEDULED');
    return;
  }

  alert('운행이 시작되었습니다.');
};

const completeOperation = async (dispatch) => {
  const confirmed = confirm('운행을 종료하시겠습니까?');

  if (!confirmed) return;

  const updatedDispatch = await updateDispatchStatus(
    dispatch,
    'COMPLETED'
  );

  if (!updatedDispatch) return;

  const updatedDriver = await updateDriverStatus(
    dispatch.driverId,
    'OFF_DUTY'
  );

  if (!updatedDriver) {
    return;
  }

  alert('운행이 종료되었습니다.');
};

const cancelDispatch = async (dispatch) => {
  const confirmed = confirm('해당 배차를 취소하시겠습니까?');

  if (!confirmed) return;

  const updatedDispatch = await updateDispatchStatus(
    dispatch,
    'CANCELED'
  );

  if (!updatedDispatch) return;

  await updateDriverStatus(
    dispatch.driverId,
    'OFF_DUTY'
  );

  alert('배차가 취소되었습니다.');
};

const getStatusText = status => {
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

const getStatusClass = status => {
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

const formatDateTime = dateString => {
  if (!dateString) return '-';

  return dateString
    .replace('T', ' ')
    .substring(0, 16);
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.operation-container {
  padding: var(--spacing-lg);
  max-width: 1500px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
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

.btn-refresh {
  padding: 10px 18px;
  background-color: var(--color-surface-light);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.btn-refresh:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-xl);
}

.summary-card {
  padding: 20px;
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.summary-label {
  display: block;
  margin-bottom: 8px;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.summary-value {
  font-size: 28px;
  color: var(--color-text-primary);
}

.summary-value.wait {
  color: #eab308;
}

.summary-value.active {
  color: #22c55e;
}

.summary-value.complete {
  color: #3b82f6;
}

.table-card {
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-card);
  overflow-x: auto;
}

.table-header {
  margin-bottom: var(--spacing-lg);
}

.table-header h2 {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.operation-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.operation-table th {
  padding: 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  border-bottom: 1px solid var(--color-border);
  white-space: nowrap;
}

.operation-table td {
  padding: 14px 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  border-bottom: 1px solid var(--color-border);
  white-space: nowrap;
}

.operation-table tr:last-child td {
  border-bottom: none;
}

.route-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.route-number {
  color: #00a3ff;
  font-weight: 700;
}

.route-name {
  color: var(--color-text-secondary);
  font-size: 12px;
}

.status-badge {
  display: inline-block;
  min-width: 90px;
  padding: 5px 0;
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

.operation-table td:nth-child(8) {
  width: 180px;
  text-align: center;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.btn-start,
.btn-complete,
.btn-cancel {
  padding: 9px 14px;
  min-width: 78px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.btn-start {
  background-color: rgba(34, 197, 94, 0.12);
  color: #22c55e;
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.btn-start:hover {
  background-color: rgba(34, 197, 94, 0.2);
}

.btn-complete {
  background-color: rgba(59, 130, 246, 0.12);
  color: #3b82f6;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.btn-complete:hover {
  background-color: rgba(59, 130, 246, 0.2);
}

.btn-cancel {
  background-color: transparent;
  color: #ef4444;
  border: 1px solid var(--color-border);
}

.btn-cancel:hover {
  border-color: #ef4444;
  background-color: rgba(239, 68, 68, 0.08);
}

.finished-text {
  color: #3b82f6;
  font-size: 13px;
  font-weight: 600;
}

.canceled-text {
  color: #ef4444;
  font-size: 13px;
  font-weight: 600;
}

.font-mono {
  color: var(--color-text-secondary);
  font-family: monospace;
}

.empty-msg {
  text-align: center;
  padding: 40px !important;
  color: var(--color-text-secondary) !important;
}

@media (max-width: 1000px) {
  .summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>