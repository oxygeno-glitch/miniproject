<template>
  <div class="driver-list-container">
    <div class="header">
      <div>
        <h1 class="page-title">기사 관리</h1>
        <p class="page-subtitle">
          등록된 운전 기사 정보 및 운행 상태를 관리합니다.
        </p>
      </div>

      <button type="button" class="btn-primary" @click="goToAdd">
        + 신규 기사 등록
      </button>
    </div>

    <div class="table-card">
      <table class="driver-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>이름</th>
            <th>면허 번호</th>
            <th>면허 종류</th>
            <th>연락처</th>
            <th>상태</th>
            <th>액션</th>
          </tr>
        </thead>

        <tbody>
          <tr v-if="drivers.length === 0">
            <td colspan="7" class="empty-msg">
              등록된 기사 데이터가 없습니다.
            </td>
          </tr>

          <tr v-for="driver in drivers" :key="driver.id">
            <td class="font-mono">{{ driver.id }}</td>
            <td class="highlight-text">{{ driver.name }}</td>
            <td>{{ driver.licenseNumber }}</td>
            <td>{{ driver.licenseType }}</td>
            <td>{{ driver.phoneNumber }}</td>
            <td>
              <span
                class="status-badge clickable"
                :class="{
                  'status-on': driver.workStatus === 'ON_DUTY',
                  'status-standby': driver.workStatus === 'STANDBY',
                  'status-off': driver.workStatus === 'OFF_DUTY',
                  'status-retired': driver.workStatus === 'RETIRED',
                }"
                @click="toggleStatus(driver)"
              >
                {{ formatStatus(driver.workStatus) }}
              </span>
            </td>
            <td>
              <div class="action-buttons">
                <button
                  type="button"
                  class="btn-secondary"
                  @click="goToDetail(driver.id)"
                >
                  상세보기
                </button>

                <button
                  type="button"
                  class="btn-danger"
                  @click="deleteDriver(driver.id)"
                >
                  삭제
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import api from "../../api";

const emit = defineEmits(["go-add", "go-detail"]);

const drivers = ref([]);

const fetchDrivers = async () => {
  try {
    const response = await api.get("/drivers");
    drivers.value = response.data;
  } catch (error) {
    console.error("기사 목록 조회 실패:", error);
  }
};

const formatStatus = (status) => {
  switch (status) {
    case "ON_DUTY":
      return "근무 중";
    case "STANDBY":
      return "대기";
    case "OFF_DUTY":
      return "휴무";
    case "RETIRED":
      return "퇴사";
    default:
      return status || "미지정";
  }
};

const toggleStatus = async (driver) => {
  let nextStatus = "ON_DUTY";

  if (driver.workStatus === "ON_DUTY") {
    nextStatus = "STANDBY";
  } else if (driver.workStatus === "STANDBY") {
    nextStatus = "OFF_DUTY";
  } else if (driver.workStatus === "OFF_DUTY") {
    nextStatus = "RETIRED";
  } else if (driver.workStatus === "RETIRED") {
    nextStatus = "ON_DUTY";
  }

  try {
    const response = await api.patch(
      `/drivers/${driver.id}/status`,
      {
        workStatus: nextStatus
      }
    );

    driver.workStatus = response.data.workStatus;
  } catch (error) {
    console.error("상태 변경 실패:", error);
    alert(
      error.response?.data?.message ||
      "상태 변경 중 오류가 발생했습니다."
    );
  }
};

const goToAdd = () => {
  emit("go-add");
};

const goToDetail = (id) => {
  emit("go-detail", id);
};

const deleteDriver = async (id) => {
  if (!confirm(`ID ${id}번 기사를 정말 삭제하시겠습니까?`)) {
    return;
  }

  try {
    await api.delete(`/drivers/${id}`);
    alert("기사가 성공적으로 삭제되었습니다.");
    await fetchDrivers();
  } catch (error) {
    console.error("기사 삭제 실패:", error);
    alert("기사 삭제 중 오류가 발생했습니다.");
  }
};

onMounted(() => {
  fetchDrivers();
});
</script>

<style scoped>
.driver-list-container {
  padding: var(--spacing-lg);
  max-width: 1400px;
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

.table-card {
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.driver-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.driver-table th {
  background-color: var(--color-surface-light);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 600;
  padding: var(--spacing-md);
  border-bottom: 1px solid var(--color-border);
}

.driver-table td {
  padding: var(--spacing-md);
  border-bottom: 1px solid var(--color-border);
  font-size: 14px;
  color: var(--color-text-primary);
  vertical-align: middle;
}

.driver-table td:nth-child(6) {
  width: 130px;
  text-align: center;
}

.driver-table tbody tr:hover {
  background-color: var(--color-surface-light);
}

.highlight-text {
  font-weight: 600;
  color: var(--color-text-primary);
}

.font-mono {
  color: var(--color-text-secondary);
  font-family: monospace;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 95px;
  padding: 5px 0;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 650;
  letter-spacing: -0.02em;
  box-sizing: border-box;
}

.status-badge.clickable {
  cursor: pointer;
  user-select: none;
  transition: opacity 0.2s;
}

.status-badge.clickable:hover {
  opacity: 0.8;
}

.status-on {
  background-color: rgba(34, 197, 94, 0.15);
  color: #4ade80;
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.status-standby {
  background-color: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.status-off {
  background-color: rgba(148, 163, 184, 0.15);
  color: var(--color-text-secondary);
  border: 1px solid rgba(148, 163, 184, 0.3);
}

.status-retired {
  background-color: rgba(239, 68, 68, 0.15);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.action-buttons {
  display: flex;
  gap: var(--spacing-sm);
}

.btn-primary-header {
  background: var(--color-primary-gradient);
  color: #ffffff;
  padding: var(--spacing-sm) var(--spacing-md);
  border: none;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-primary-header:hover {
  opacity: 0.9;
}

.btn-secondary {
  background-color: var(--color-surface-light);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
}

.btn-secondary:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.btn-danger {
  background-color: transparent;
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.4);
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
}

.btn-danger:hover {
  background-color: #ef4444;
  color: #ffffff;
  border-color: #ef4444;
}

.empty-msg {
  text-align: center;
  padding: var(--spacing-xxl) !important;
  color: var(--color-text-secondary);
}

.btn-primary {
  background: var(--color-primary-gradient);
  color: #ffffff;
  padding: var(--spacing-sm) var(--spacing-md);
  border: none;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-primary:hover {
  opacity: 0.9;
}
</style>