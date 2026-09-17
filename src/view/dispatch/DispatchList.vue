<template>
  <div class="dispatch-list-container">
    <div class="header">
      <div>
        <h1 class="page-title">배차 관리</h1>
        <p class="page-subtitle">
          노선·차량·기사를 배차하고 운행 상태를 관리합니다.
        </p>
      </div>

      <div class="header-buttons">
        <button
          type="button"
          class="btn-generate"
          :disabled="generating"
          @click="generateDispatchRequests"
        >
          {{ generating ? "생성 중..." : "배차" }}
        </button>

        <button
          v-if="selectedIds.length > 0"
          type="button"
          class="btn-danger"
          @click="deleteSelectedDispatches"
        >
          선택 삭제 ({{ selectedIds.length }})
        </button>
      </div>
    </div>

    <div v-if="pendingRequests.length > 0" class="pending-section">
      <div class="pending-header">
        <div>
          <h2 class="pending-title">배차 신청 대기 ({{ pendingRequests.length }}건)</h2>
        </div>

        <button
          type="button"
          class="btn-approve-selected"
          :disabled="selectedRequestIds.length === 0"
          @click="approveSelectedRequests"
        >
          선택 승인 ({{ selectedRequestIds.length }})
        </button>
      </div>

      <table class="pending-table">
        <thead>
          <tr>
            <th class="checkbox-column">
              <label class="checkbox-wrap">
                <input
                  type="checkbox"
                  :checked="isAllRequestsSelected"
                  @change="toggleAllRequests"
                />
                <span class="checkbox-box"></span>
              </label>
            </th>
            <th>노선</th>
            <th>차량</th>
            <th>기사</th>
            <th>예상 운행 시간</th>
            <th>신청 시각</th>
            <th class="col-approve">승인</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="request in pendingRequests" :key="request.requestId">
            <td class="checkbox-column">
              <label class="checkbox-wrap">
                <input
                  type="checkbox"
                  :value="request.requestId"
                  v-model="selectedRequestIds"
                />
                <span class="checkbox-box"></span>
              </label>
            </td>

            <td>
              <div class="route-info">
                <span class="route-number">
                  {{ request.routeNumber || "-" }}
                </span>
                <span v-if="request.routeName" class="route-name">
                  {{ request.routeName }}
                </span>
              </div>
            </td>

            <td>{{ request.vehiclePlateNumber || "-" }}</td>

            <td>{{ request.driverName || "-" }}</td>

            <td class="font-mono">
              {{ formatDuration(request.plannedStartTime, request.plannedEndTime) }}
            </td>

            <td class="font-mono">
              {{ formatDateTime(request.requestedAt) }}
            </td>

            <td class="col-approve">
              <button
                type="button"
                class="btn-approve"
                @click="approveRequest(request)"
              >
                배차 승인
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="loading" class="loading">
      배차 정보를 불러오는 중입니다.
    </div>

    <div v-else class="table-card">
      <table class="dispatch-table">
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
            <th class="col-duration">예상 운행 시간</th>
            <th class="col-status">상태</th>
            <th class="col-actions">관리</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="dispatch in dispatches"
            :key="dispatch.id"
            @click="dispatch.isFailedRequest ? null : $emit('detail', dispatch.id)"
          >
            <td class="checkbox-column" @click.stop>
              <label v-if="!dispatch.isFailedRequest" class="checkbox-wrap">
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
                  {{ dispatch.routeNumber || "-" }}
                </span>
                <span v-if="dispatch.routeName" class="route-name">
                  {{ dispatch.routeName }}
                </span>
              </div>
            </td>

            <td>
              {{ dispatch.vehiclePlateNumber || "-" }}
            </td>

            <td>
              {{ dispatch.driverName || "-" }}
            </td>

            <td class="col-duration font-mono">
              {{ formatDuration(dispatch.plannedStartTime, dispatch.plannedEndTime) }}
            </td>

            <td class="col-status">
              <span
                class="status-badge"
                :class="getStatusClass(dispatch.dispatchStatus)"
              >
                {{ getStatusText(dispatch.dispatchStatus) }}
              </span>
            </td>

            <td class="col-actions" @click.stop>
              <div class="action-buttons">
                <button
                  v-if="dispatch.dispatchStatus === 'SCHEDULED'"
                  type="button"
                  class="btn-cancel"
                  @click="updateStatus(dispatch.id, 'CANCELED')"
                >
                  취소
                </button>

                <span
                  v-else-if="dispatch.dispatchStatus === 'IN_PROGRESS'"
                  class="in-progress-text"
                >
                  운행 중
                </span>

                <span
                  v-else-if="dispatch.dispatchStatus === 'COMPLETED'"
                  class="finished-text"
                >
                  운행 완료
                </span>

                <span
                  v-else-if="dispatch.dispatchStatus === 'CANCELED'"
                  class="canceled-text"
                >
                  취소됨
                </span>

                <span
                  v-else-if="dispatch.dispatchStatus === 'FAILED'"
                  class="failed-text"
                >
                  배차 실패
                </span>
              </div>
            </td>
          </tr>

          <tr v-if="dispatches.length === 0">
            <td colspan="7" class="empty-msg">
              등록된 배차 정보가 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from "vue";
import {
  dispatches,
  selectedIds,
  loading,
  pendingRequests,
  selectedRequestIds,
  generating,
  fetchDispatches,
  generateDispatchRequests,
  isAllRequestsSelected,
  toggleAllRequests,
  approveRequest,
  approveSelectedRequests,
  isAllSelected,
  toggleAll,
  updateStatus,
  deleteSelectedDispatches,
  formatDuration,
  formatDateTime,
  getStatusText,
  getStatusClass,
} from "../../store/dispatchQueue";

defineEmits(["detail"]);

// 배차 목록/배차 신청 대기 상태는 store/dispatchQueue.js(모듈 스코프)에
// 있어, 다른 메뉴로 이동했다가 돌아와 이 컴포넌트가 다시 마운트되어도
// "배차 신청 대기" 카드와 진행 중이던 타이머가 그대로 유지됩니다.
onMounted(() => {
  fetchDispatches();
});
</script>

<style scoped>
.dispatch-list-container {
  padding: var(--spacing-lg);
  max-width: 1500px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--spacing-xl);
  gap: var(--spacing-md);
  flex-wrap: wrap;
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

.btn-danger {
  background-color: transparent;
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.4);
  padding: 10px 18px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border-radius: var(--radius-md);
  white-space: nowrap;
}

.btn-danger:hover {
  background-color: #ef4444;
  color: #ffffff;
  border-color: #ef4444;
}

.btn-generate {
  background: var(--color-primary-gradient);
  color: #ffffff;
  border: none;
  padding: 10px 18px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border-radius: var(--radius-md);
  white-space: nowrap;
  transition: opacity 0.2s;
}

.btn-generate:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-generate:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading {
  text-align: center;
  padding: var(--spacing-xxl);
  color: var(--color-text-secondary);
}

/* ---------- 배차 신청 대기 ---------- */

.pending-section {
  background-color: var(--color-surface);
  border: 1px solid rgba(234, 179, 8, 0.35);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-card);
  margin-bottom: var(--spacing-xl);
  width: 100%;
  box-sizing: border-box;
  overflow-x: auto;
}

.pending-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
  flex-wrap: wrap;
}

.pending-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.btn-approve-selected {
  background: var(--color-primary-gradient);
  color: #ffffff;
  border: none;
  padding: 10px 18px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border-radius: var(--radius-md);
  white-space: nowrap;
}

.btn-approve-selected:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-approve-selected:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pending-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.pending-table th {
  padding: 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  border-bottom: 1px solid var(--color-border);
  white-space: nowrap;
}

.pending-table td {
  padding: 14px 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  border-bottom: 1px solid var(--color-border);
}

.pending-table tr:last-child td {
  border-bottom: none;
}

.col-approve {
  width: 110px;
  text-align: center;
}

.btn-approve {
  background-color: rgba(34, 197, 94, 0.12);
  color: #22c55e;
  border: 1px solid rgba(34, 197, 94, 0.3);
  padding: 8px 12px;
  min-width: 78px;
  border-radius: var(--radius-md);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.btn-approve:hover {
  background-color: rgba(34, 197, 94, 0.2);
}

.table-card {
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-card);
  width: 100%;
  box-sizing: border-box;
}

.dispatch-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  text-align: left;
}

.dispatch-table th {
  padding: 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  border-bottom: 1px solid var(--color-border);
}

.dispatch-table td {
  padding: 14px 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  border-bottom: 1px solid var(--color-border);
  overflow-wrap: break-word;
}

.dispatch-table tbody tr {
  cursor: pointer;
  transition: background-color 0.2s;
}

.dispatch-table tbody tr:hover {
  background-color: var(--color-surface-light);
}

.dispatch-table tr:last-child td {
  border-bottom: none;
}

.checkbox-column {
  width: 44px;
}

.col-duration {
  width: 130px;
}

.col-status {
  width: 110px;
}

.col-actions {
  width: 190px;
}

.font-mono {
  color: var(--color-text-secondary);
  font-family: monospace;
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

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.btn-cancel {
  padding: 8px 12px;
  min-width: 64px;
  border-radius: var(--radius-md);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
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

.in-progress-text {
  color: #22c55e;
  font-size: 13px;
  font-weight: 600;
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

.failed-text {
  color: #ef4444;
  font-size: 13px;
  font-weight: 600;
}

.empty-msg {
  text-align: center;
  padding: 40px !important;
  color: var(--color-text-secondary) !important;
}

@media (max-width: 800px) {
  .header {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-buttons {
    width: 100%;
  }

  .header-buttons button {
    flex: 1;
  }

  .table-card {
    padding: var(--spacing-md);
  }

  .col-duration {
    width: 100px;
  }

  .col-status {
    width: 90px;
  }

  .col-actions {
    width: 150px;
  }
}

@media (max-width: 600px) {
  .dispatch-table,
  .dispatch-table thead,
  .dispatch-table tbody,
  .dispatch-table th,
  .dispatch-table td,
  .dispatch-table tr {
    display: block;
  }

  .dispatch-table thead {
    display: none;
  }

  .dispatch-table tr {
    padding: var(--spacing-md) 0;
    border-bottom: 1px solid var(--color-border);
  }

  .dispatch-table tr:last-child {
    border-bottom: none;
  }

  .dispatch-table td {
    width: auto;
    border-bottom: none;
    padding: 4px 0;
  }

  .checkbox-column {
    position: absolute;
    top: var(--spacing-md);
    right: 0;
  }

  .dispatch-table tr {
    position: relative;
    padding-right: 36px;
  }

  .col-status,
  .col-actions {
    padding-top: 8px;
  }

  .action-buttons {
    justify-content: flex-start;
  }
}
</style>
