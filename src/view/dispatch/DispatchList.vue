<template>
  <div class="dispatch-list">
    <div class="header">
      <h2>배차 관리</h2>

      <div class="header-buttons">
        <button class="auto-button" @click="openRouteSelect">
          자동 배차 및 운행 시작
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading">
      배차 정보를 불러오는 중입니다.
    </div>

    <div v-else-if="dispatches.length === 0" class="empty">
      등록된 배차 정보가 없습니다.
    </div>

    <table v-else>
      <thead>
        <tr>
          <th>ID</th>
          <th>노선</th>
          <th>차량</th>
          <th>기사</th>
          <th>예정 출발</th>
          <th>예정 도착</th>
          <th>실제 출발</th>
          <th>실제 도착</th>
          <th>상태</th>
          <th>관리</th>
        </tr>
      </thead>

      <tbody>
        <tr
          v-for="dispatch in dispatches"
          :key="dispatch.id"
          @click="$emit('detail', dispatch.id)"
        >
          <td>{{ dispatch.id }}</td>

          <td>
            {{ dispatch.routeNumber || '-' }}
            <span v-if="dispatch.routeName">
              - {{ dispatch.routeName }}
            </span>
          </td>

          <td>{{ dispatch.vehiclePlateNumber || '-' }}</td>

          <td>{{ dispatch.driverName || '-' }}</td>

          <td>{{ formatDateTime(dispatch.plannedStartTime) }}</td>

          <td>{{ formatDateTime(dispatch.plannedEndTime) }}</td>

          <td>{{ formatDateTime(dispatch.actualStartTime) }}</td>

          <td>{{ formatDateTime(dispatch.actualEndTime) }}</td>

          <td>
            <span
              class="status"
              :class="getStatusClass(dispatch.dispatchStatus)"
            >
              {{ getStatusText(dispatch.dispatchStatus) }}
            </span>
          </td>

          <td @click.stop>
            <button
              v-if="dispatch.dispatchStatus === 'SCHEDULED'"
              class="start-button"
              @click="updateStatus(dispatch.id, 'IN_PROGRESS')"
            >
              운행 시작
            </button>

            <button
              v-if="dispatch.dispatchStatus === 'IN_PROGRESS'"
              class="complete-button"
              @click="updateStatus(dispatch.id, 'COMPLETED')"
            >
              운행 종료
            </button>

            <button
              v-if="dispatch.dispatchStatus === 'SCHEDULED'"
              class="cancel-button"
              @click="updateStatus(dispatch.id, 'CANCELED')"
            >
              취소
            </button>

            <button
              v-if="dispatch.dispatchStatus !== 'IN_PROGRESS'"
              class="delete-button"
              @click="deleteDispatch(dispatch.id)"
            >
              삭제
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <div
      v-if="showRouteModal"
      class="modal-overlay"
      @click.self="closeRouteSelect"
    >
      <div class="route-modal">
        <div class="modal-header">
          <h3>자동 배차 노선 선택</h3>

          <button
            class="close-button"
            @click="closeRouteSelect"
          >
            ×
          </button>
        </div>

        <div v-if="routeLoading" class="modal-loading">
          노선 정보를 불러오는 중입니다.
        </div>

        <div
          v-else-if="routes.length === 0"
          class="modal-empty"
        >
          등록된 노선이 없습니다.
        </div>

        <div v-else class="route-list">
          <button
            v-for="route in routes"
            :key="route.id"
            class="route-item"
            @click="selectRoute(route)"
          >
            <strong>{{ route.routeNumber }}</strong>
            <span>{{ route.routeName }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api'

defineEmits(['detail'])

const dispatches = ref([])
const routes = ref([])

const loading = ref(false)
const routeLoading = ref(false)
const showRouteModal = ref(false)

const fetchDispatches = async () => {
  loading.value = true

  try {
    const response = await api.get('/dispatches')
    dispatches.value = response.data
  } catch (error) {
    console.error(error)
    alert('배차 정보를 불러오지 못했습니다.')
  } finally {
    loading.value = false
  }
}

const fetchRoutes = async () => {
  routeLoading.value = true

  try {
    const response = await api.get('/routes')
    routes.value = response.data
  } catch (error) {
    console.error(error)
    alert('노선 정보를 불러오지 못했습니다.')
  } finally {
    routeLoading.value = false
  }
}

const openRouteSelect = async () => {
  showRouteModal.value = true

  if (routes.value.length === 0) {
    await fetchRoutes()
  }
}

const closeRouteSelect = () => {
  showRouteModal.value = false
}

const selectRoute = async (route) => {
  const confirmed = confirm(
    `${route.routeNumber} - ${route.routeName}\n\n이 노선으로 자동 배차를 시작하시겠습니까?`
  )

  if (!confirmed) {
    return
  }

  try {
    await api.post('/dispatches/auto-start', {
      routeId: route.id
    })

    alert(
      `${route.routeNumber} - ${route.routeName}\n자동 배차 및 운행이 시작되었습니다.`
    )

    closeRouteSelect()
    await fetchDispatches()
  } catch (error) {
    console.error(error)

    const message =
      error.response?.data?.message ||
      '자동 배차에 실패했습니다.'

    alert(message)
  }
}

const updateStatus = async (id, status) => {
  let message = ''

  if (status === 'IN_PROGRESS') {
    message = '운행을 시작하시겠습니까?'
  } else if (status === 'COMPLETED') {
    message = '운행을 종료하시겠습니까?'
  } else if (status === 'CANCELED') {
    message = '배차를 취소하시겠습니까?'
  }

  if (!confirm(message)) {
    return
  }

  try {
    await api.patch(`/dispatches/${id}/status`, {
      dispatchStatus: status
    })

    await fetchDispatches()
  } catch (error) {
    console.error(error)

    const message =
      error.response?.data?.message ||
      '배차 상태 변경에 실패했습니다.'

    alert(message)
  }
}

const deleteDispatch = async (id) => {
  if (!confirm('이 배차 정보를 삭제하시겠습니까?')) {
    return
  }

  try {
    await api.delete(`/dispatches/${id}`)

    alert('배차 정보가 삭제되었습니다.')

    await fetchDispatches()
  } catch (error) {
    console.error(error)

    const message =
      error.response?.data?.message ||
      '배차 삭제에 실패했습니다.'

    alert(message)
  }
}

const formatDateTime = (value) => {
  if (!value) {
    return '-'
  }

  return value.replace('T', ' ').slice(0, 16)
}

const getStatusText = (status) => {
  const statusMap = {
    SCHEDULED: '배차 대기',
    IN_PROGRESS: '운행 중',
    COMPLETED: '운행 완료',
    CANCELED: '배차 취소'
  }

  return statusMap[status] || status
}

const getStatusClass = (status) => {
  return {
    scheduled: status === 'SCHEDULED',
    progress: status === 'IN_PROGRESS',
    completed: status === 'COMPLETED',
    canceled: status === 'CANCELED'
  }
}

onMounted(() => {
  fetchDispatches()
})
</script>

<style scoped>
.dispatch-list {
  width: 100%;
  color: #ffffff;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #ffffff;
}

.header-buttons {
  display: flex;
  gap: 10px;
}

.header button {
  border: 1px solid transparent;
  border-radius: 7px;
  padding: 10px 16px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.2s;
}

.auto-button {
  background-color: #2563eb;
  color: #ffffff;
}

.auto-button:hover {
  background-color: #3b82f6;
}

.loading,
.empty {
  background-color: #151f2e;
  border: 1px solid #1e293b;
  border-radius: 12px;
  padding: 50px;
  text-align: center;
  color: #94a3b8;
}

table {
  width: 100%;
  border-collapse: collapse;
  background-color: #151f2e;
  border: 1px solid #1e293b;
  border-radius: 12px;
  overflow: hidden;
}

th,
td {
  padding: 13px 12px;
  border-bottom: 1px solid #1e293b;
  text-align: center;
  font-size: 13px;
}

th {
  background-color: #111b29;
  color: #94a3b8;
  font-weight: 600;
  white-space: nowrap;
}

td {
  color: #ffffff;
}

tbody tr {
  cursor: pointer;
  transition: background-color 0.2s;
}

tbody tr:hover {
  background-color: #1b293b;
}

tbody tr:last-child td {
  border-bottom: none;
}

.status {
  display: inline-block;
  min-width: 54px;
  padding: 5px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status.scheduled {
  background-color: #78350f;
  color: #fbbf24;
}

.status.progress {
  background-color: #1e3a8a;
  color: #60a5fa;
}

.status.completed {
  background-color: #064e3b;
  color: #34d399;
}

.status.canceled {
  background-color: #7f1d1d;
  color: #fca5a5;
}

.start-button,
.complete-button,
.cancel-button,
.delete-button {
  border: none;
  border-radius: 5px;
  padding: 6px 9px;
  margin: 2px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.2s;
}

.start-button {
  background-color: #2563eb;
  color: #ffffff;
}

.start-button:hover {
  background-color: #3b82f6;
}

.complete-button {
  background-color: #059669;
  color: #ffffff;
}

.complete-button:hover {
  background-color: #10b981;
}

.cancel-button {
  background-color: #b45309;
  color: #ffffff;
}

.cancel-button:hover {
  background-color: #d97706;
}

.delete-button {
  background-color: #dc2626;
  color: #ffffff;
}

.delete-button:hover {
  background-color: #ef4444;
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

.route-modal {
  width: 500px;
  max-width: calc(100vw - 40px);
  max-height: 80vh;
  overflow: hidden;
  background-color: #151f2e;
  border: 1px solid #1e293b;
  border-radius: 12px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  border-bottom: 1px solid #1e293b;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #ffffff;
}

.close-button {
  border: none;
  background: transparent;
  font-size: 26px;
  cursor: pointer;
  color: #64748b;
  transition: color 0.2s;
}

.close-button:hover {
  color: #ffffff;
}

.route-list {
  padding: 12px;
  max-height: 60vh;
  overflow-y: auto;
}

.route-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px;
  margin-bottom: 8px;
  border: 1px solid #1e293b;
  border-radius: 8px;
  background-color: #0b131e;
  cursor: pointer;
  text-align: left;
  color: #ffffff;
  transition: all 0.2s;
}

.route-item:last-child {
  margin-bottom: 0;
}

.route-item:hover {
  background-color: #151f2e;
  border-color: #3b82f6;
}

.route-item strong {
  min-width: 60px;
  font-size: 16px;
  color: #00a3ff;
}

.route-item span {
  color: #94a3b8;
  font-size: 14px;
}

.modal-loading,
.modal-empty {
  padding: 40px;
  text-align: center;
  color: #94a3b8;
}

@media (max-width: 1100px) {
  .dispatch-list {
    overflow-x: auto;
  }

  table {
    min-width: 1100px;
  }
}

@media (max-width: 800px) {
  .header {
    align-items: flex-start;
    flex-direction: column;
    gap: 15px;
  }

  .header-buttons {
    width: 100%;
  }

  .header button {
    flex: 1;
  }

  .route-modal {
    width: calc(100vw - 30px);
  }
}
</style>