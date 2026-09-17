<template>
  <div class="operation-container">
    <div class="header">
      <div>
        <h1 class="page-title">운행 관리</h1>
        <p class="page-subtitle">
          현재 배차의 운행 상태를 관리합니다.
        </p>
      </div>

      <div class="header-buttons">
        <button
          v-if="selectedIds.length > 0"
          type="button"
          class="btn-complete-selected"
          @click="completeSelectedOperations"
        >
          선택 완료 ({{ selectedIds.length }})
        </button>

        <button type="button" class="btn-refresh" @click="fetchData">
          새로고침
        </button>
      </div>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <span class="summary-label">전체 배차</span>
        <strong class="summary-value">{{ dispatches.length }}</strong>
      </div>

      <div class="summary-card">
        <span class="summary-label">운행 대기중 차량</span>
        <strong class="summary-value wait">{{ idleVehicleCount }}</strong>
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
            <th class="checkbox-column">
              <label class="checkbox-wrap">
                <input
                  type="checkbox"
                  :checked="isAllSelected"
                  @change="toggleAll"
                />
                <span class="checkbox-box"></span>
              </label>
            </th>
            <th>노선</th>
            <th>차량</th>
            <th>기사</th>
            <th>실제 출발</th>
            <th>실제 도착</th>
            <th>운행 상태</th>
            <th>관리</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="dispatch in dispatches"
            :key="dispatch.id"
          >
            <td class="checkbox-column">
              <label
                v-if="dispatch.dispatchStatus === 'IN_PROGRESS'"
                class="checkbox-wrap"
              >
                <input
                  type="checkbox"
                  :value="dispatch.id"
                  v-model="selectedIds"
                />
                <span class="checkbox-box"></span>
              </label>
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
              {{ formatDateTime(dispatch.actualStartTime) }}
            </td>

            <td class="font-mono">
              {{ formatDateTime(dispatch.actualEndTime) }}
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
const vehicles = ref([]);
const vehicleMap = reactive({});
const driverMap = reactive({});

// 체크박스로 선택한 "운행 중" 배차를 한꺼번에 "운행 종료" 처리하기 위한 상태
const selectedIds = ref([]);

const isAllSelected = computed(() => {
  const inProgressDispatches = dispatches.value.filter(
    dispatch => dispatch.dispatchStatus === 'IN_PROGRESS'
  );

  if (inProgressDispatches.length === 0) {
    return false;
  }

  return inProgressDispatches.every(dispatch =>
    selectedIds.value.includes(dispatch.id)
  );
});

const toggleAll = (event) => {
  if (event.target.checked) {
    selectedIds.value = dispatches.value
      .filter(dispatch => dispatch.dispatchStatus === 'IN_PROGRESS')
      .map(dispatch => dispatch.id);
  } else {
    selectedIds.value = [];
  }
};

const idleVehicleCount = computed(() =>
  vehicles.value.filter(
    vehicle => vehicle.status === 'INACTIVE'
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
    vehicles.value = vehicleResponse.data;

    Object.keys(vehicleMap).forEach(key => delete vehicleMap[key]);
    Object.keys(driverMap).forEach(key => delete driverMap[key]);

    vehicleResponse.data.forEach(vehicle => {
      vehicleMap[vehicle.id] = vehicle.plateNumber;
    });

    driverResponse.data.forEach(driver => {
      driverMap[driver.id] = driver.name || `기사 #${driver.id}`;
    });

    scheduleAutoCompleteForAll(dispatches.value);
  } catch (error) {
    console.error('운행 데이터 조회 실패:', error);
    alert('운행 정보를 불러오지 못했습니다.');
  }
};

// ---------------------------------------------------------------------------
// 운행 중 -> 운행 종료 자동 전환
// "운행 중(IN_PROGRESS)" 상태인 배차는 예상 도착 시간(plannedEndTime) 기준
// 앞뒤로 3분(±3분) 범위 안의 임의의 시점에 자동으로 "운행 종료"로 상태가
// 바뀝니다. 이미 그 범위가 지난 경우에는 잠시 후 바로 종료 처리합니다.
// ---------------------------------------------------------------------------

const AUTO_COMPLETE_WINDOW_MS = 3 * 60 * 1000;

// 이미 자동 종료가 예약된 배차는 다시 예약하지 않도록 추적합니다.
const scheduledAutoCompleteIds = new Set();

const pickAutoCompleteDelayMs = (dispatch) => {
  if (!dispatch.plannedEndTime) {
    // 예상 도착 시간 정보가 없으면 3분 범위 안에서 임의의 시점에 종료합니다.
    return Math.random() * AUTO_COMPLETE_WINDOW_MS;
  }

  const target =
    new Date(dispatch.plannedEndTime).getTime() +
    (Math.random() * 2 - 1) * AUTO_COMPLETE_WINDOW_MS;

  const delay = target - Date.now();

  // 목표 시각이 이미 지났으면 5~15초 뒤에 바로 종료 처리합니다.
  return delay > 0 ? delay : 5000 + Math.random() * 10000;
};

const autoCompleteOperation = async (dispatchId) => {
  const current = dispatches.value.find(item => item.id === dispatchId);

  // 그 사이 수동으로 종료/취소되었거나 목록에서 사라졌으면 자동 종료하지
  // 않습니다.
  if (!current || current.dispatchStatus !== 'IN_PROGRESS') {
    return;
  }

  const updatedDispatch = await updateDispatchStatus(current, 'COMPLETED');

  if (!updatedDispatch) {
    return;
  }

  await updateDriverStatus(current.driverId, 'OFF_DUTY');
};

const ensureAutoCompleteScheduled = (dispatch) => {
  if (
    !dispatch ||
    dispatch.dispatchStatus !== 'IN_PROGRESS' ||
    scheduledAutoCompleteIds.has(dispatch.id)
  ) {
    return;
  }

  scheduledAutoCompleteIds.add(dispatch.id);

  setTimeout(() => {
    scheduledAutoCompleteIds.delete(dispatch.id);
    autoCompleteOperation(dispatch.id);
  }, pickAutoCompleteDelayMs(dispatch));
};

const scheduleAutoCompleteForAll = (list) => {
  list.forEach(dispatch => ensureAutoCompleteScheduled(dispatch));
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

    // 운행 중이 아닌 상태로 바뀐 배차는 체크박스 선택에서도 제외합니다.
    if (response.data.dispatchStatus !== 'IN_PROGRESS') {
      selectedIds.value = selectedIds.value.filter(
        id => id !== dispatch.id
      );
    }

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

  ensureAutoCompleteScheduled(updatedDispatch);

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

// 체크박스로 선택한 "운행 중" 배차를 한꺼번에 "운행 종료" 처리합니다.
const completeSelectedOperations = async () => {
  if (selectedIds.value.length === 0) {
    alert('완료 처리할 운행을 선택해주세요.');
    return;
  }

  const targets = dispatches.value.filter(
    dispatch =>
      selectedIds.value.includes(dispatch.id) &&
      dispatch.dispatchStatus === 'IN_PROGRESS'
  );

  if (targets.length === 0) {
    alert('완료 처리할 수 있는 운행이 없습니다.');
    selectedIds.value = [];
    return;
  }

  if (!confirm(`선택한 ${targets.length}건의 운행을 종료하시겠습니까?`)) {
    return;
  }

  let successCount = 0;
  let failCount = 0;

  for (const dispatch of targets) {
    const updatedDispatch = await updateDispatchStatus(dispatch, 'COMPLETED');

    if (!updatedDispatch) {
      failCount += 1;
      continue;
    }

    await updateDriverStatus(dispatch.driverId, 'OFF_DUTY');
    successCount += 1;
  }

  selectedIds.value = [];

  alert(
    failCount > 0
      ? `${successCount}건 종료 완료, ${failCount}건은 실패했습니다.`
      : `${successCount}건 운행이 종료되었습니다.`
  );
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

.header-buttons {
  display: flex;
  gap: var(--spacing-sm);
}

.btn-complete-selected {
  background-color: rgba(59, 130, 246, 0.12);
  color: #3b82f6;
  border: 1px solid rgba(59, 130, 246, 0.3);
  padding: 10px 18px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border-radius: var(--radius-md);
  white-space: nowrap;
}

.btn-complete-selected:hover {
  background-color: rgba(59, 130, 246, 0.2);
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

.checkbox-column {
  width: 44px;
}

/* ---------- 커스텀 체크박스 ---------- */

.checkbox-wrap {
  position: relative;
  display: inline-flex;
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.checkbox-wrap input[type="checkbox"] {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  opacity: 0;
  cursor: pointer;
}

.checkbox-box {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1.5px solid var(--color-border);
  border-radius: 5px;
  background-color: var(--color-surface-light);
  transition: background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease;
  pointer-events: none;
}

.checkbox-box::after {
  content: "";
  width: 5px;
  height: 9px;
  margin-top: -1px;
  border: solid #ffffff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg) scale(0);
  transition: transform 0.15s ease;
}

.checkbox-wrap input[type="checkbox"]:hover ~ .checkbox-box {
  border-color: var(--color-primary);
}

.checkbox-wrap input[type="checkbox"]:checked ~ .checkbox-box {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
}

.checkbox-wrap input[type="checkbox"]:checked ~ .checkbox-box::after {
  transform: rotate(45deg) scale(1);
}

.checkbox-wrap input[type="checkbox"]:focus-visible ~ .checkbox-box {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.25);
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