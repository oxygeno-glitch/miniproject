<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import logoImg from "../assets/logo.png";
import api from "../api";

import DriverList from "../view/driver/DriverList.vue";
import AddDriver from "../view/driver/AddDriver.vue";
import DriverDetails from "../view/driver/DriverDetails.vue";

import VehicleList from "../view/vehicle/VehicleList.vue";
import AddVehicle from "../view/vehicle/AddVehicle.vue";
import VehicleDetails from "../view/vehicle/VehicleDetails.vue";

import DispatchList from "../view/dispatch/DispatchList.vue";
import DispatchDetails from "../view/dispatch/DispatchDetails.vue";

import RouteList from "../view/route/RouteList.vue";
import AddRoute from "../view/route/AddRoute.vue";
import RouteDetails from "../view/route/RouteDetails.vue";

import Operation from "../view/operation/Operation.vue";

const router = useRouter();

const activeMenu = ref("home");
const activeAction = ref("list");
const selectedId = ref(null);

const drivers = ref([]);
const vehicles = ref([]);
const dispatches = ref([]);

const loading = ref(false);

const setMenu = (menu) => {
  activeMenu.value = menu;
  activeAction.value = "list";
  selectedId.value = null;

  if (menu === "home") {
    fetchDashboardData();
  }
};

const goToAdd = () => {
  activeAction.value = "add";
  selectedId.value = null;
};

const goToDetail = (id) => {
  selectedId.value = id;
  activeAction.value = "detail";
};

const goToList = () => {
  activeAction.value = "list";
  selectedId.value = null;
};

const logout = () => {
  sessionStorage.removeItem("isLoggedIn");
  sessionStorage.removeItem("loginUser");

  alert("로그아웃되었습니다.");

  router.push("/login");
};

const fetchDashboardData = async () => {
  loading.value = true;

  try {
    const [driverResponse, vehicleResponse, dispatchResponse] =
      await Promise.all([
        api.get("/drivers"),
        api.get("/vehicles"),
        api.get("/dispatches"),
      ]);

    drivers.value = driverResponse.data;
    vehicles.value = vehicleResponse.data;
    dispatches.value = dispatchResponse.data;
  } catch (error) {
    console.error("대시보드 데이터 조회 실패:", error);
  } finally {
    loading.value = false;
  }
};

const totalDispatchCount = computed(() => {
  return dispatches.value.length;
});

const scheduledDispatchCount = computed(() => {
  return dispatches.value.filter(
    (dispatch) => dispatch.dispatchStatus === "SCHEDULED",
  ).length;
});

const inProgressDispatchCount = computed(() => {
  return dispatches.value.filter(
    (dispatch) => dispatch.dispatchStatus === "IN_PROGRESS",
  ).length;
});

const completedDispatchCount = computed(() => {
  return dispatches.value.filter(
    (dispatch) => dispatch.dispatchStatus === "COMPLETED",
  ).length;
});

const onDutyDriverCount = computed(() => {
  return drivers.value.filter((driver) => driver.workStatus === "ON_DUTY")
    .length;
});

const standbyDriverCount = computed(() => {
  return drivers.value.filter((driver) => driver.workStatus === "STANDBY")
    .length;
});

const offDutyDriverCount = computed(() => {
  return drivers.value.filter((driver) => driver.workStatus === "OFF_DUTY")
    .length;
});

const retiredDriverCount = computed(() => {
  return drivers.value.filter((driver) => driver.workStatus === "RETIRED")
    .length;
});

const activeVehicleCount = computed(() => {
  return vehicles.value.filter((vehicle) => vehicle.status === "ACTIVE").length;
});

const maintenanceVehicleCount = computed(() => {
  return vehicles.value.filter((vehicle) => vehicle.status === "MAINTENANCE")
    .length;
});

const inactiveVehicleCount = computed(() => {
  return vehicles.value.filter((vehicle) => vehicle.status === "INACTIVE")
    .length;
});

const getDriverStatusText = (status) => {
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
      return status || "-";
  }
};

const getDriverStatusClass = (status) => {
  switch (status) {
    case "ON_DUTY":
      return "green";
    case "STANDBY":
      return "blue";
    case "OFF_DUTY":
      return "gray";
    case "RETIRED":
      return "orange";
    default:
      return "gray";
  }
};

const getVehicleStatusText = (status) => {
  switch (status) {
    case "ACTIVE":
      return "운행 중";
    case "MAINTENANCE":
      return "정비";
    case "INACTIVE":
      return "운행 대기";
    default:
      return status || "-";
  }
};

const getVehicleStatusClass = (status) => {
  switch (status) {
    case "ACTIVE":
      return "green";
    case "MAINTENANCE":
      return "orange";
    case "INACTIVE":
      return "gray";
    default:
      return "gray";
  }
};

const getDispatchStatusText = (status) => {
  switch (status) {
    case "SCHEDULED":
      return "배차 대기";
    case "IN_PROGRESS":
      return "운행 중";
    case "COMPLETED":
      return "운행 종료";
    case "CANCELED":
      return "배차 취소";
    default:
      return status || "-";
  }
};

const getDispatchStatusClass = (status) => {
  switch (status) {
    case "SCHEDULED":
      return "blue";
    case "IN_PROGRESS":
      return "green";
    case "COMPLETED":
      return "gray";
    case "CANCELED":
      return "orange";
    default:
      return "gray";
  }
};

const goToDriver = () => {
  setMenu("driver");
};

const goToVehicle = () => {
  setMenu("vehicle");
};

const goToDispatch = () => {
  setMenu("dispatch");
};

const goToOperation = () => {
  setMenu("operation");
};

onMounted(() => {
  fetchDashboardData();
});
</script>

<template>
  <div class="dashboard-wrapper">
    <nav class="dashboard-nav">
      <ul class="nav-links">
        <li class="logo-item">
          <button type="button" class="logo-btn" @click="setMenu('home')">
            <img :src="logoImg" alt="STUDIO G Logo" class="dashboard-logo" />
          </button>
        </li>

        <li class="nav-item">
          <button
            type="button"
            class="nav-btn"
            :class="{ active: activeMenu === 'driver' }"
            @click="setMenu('driver')"
          >
            운전자
          </button>
        </li>

        <li class="nav-item">
          <button
            type="button"
            class="nav-btn"
            :class="{ active: activeMenu === 'vehicle' }"
            @click="setMenu('vehicle')"
          >
            차량
          </button>
        </li>

        <li class="nav-item">
          <button
            type="button"
            class="nav-btn"
            :class="{ active: activeMenu === 'route' }"
            @click="setMenu('route')"
          >
            노선
          </button>
        </li>

        <li class="nav-item">
          <button
            type="button"
            class="nav-btn"
            :class="{ active: activeMenu === 'dispatch' }"
            @click="setMenu('dispatch')"
          >
            배차
          </button>
        </li>

        <li class="nav-item">
          <button
            type="button"
            class="nav-btn"
            :class="{ active: activeMenu === 'operation' }"
            @click="setMenu('operation')"
          >
            운행
          </button>
        </li>

        <li class="logout-item">
          <button type="button" class="logout-btn" @click="logout">
            로그아웃
          </button>
        </li>
      </ul>
    </nav>

    <div class="dashboard-container single-layout">
      <main class="content-body">
        <div v-if="activeMenu === 'home'" class="dashboard-home">
          <div class="home-header">
            <div>
              <h1>운송 관리 대시보드</h1>
              <p>현재 운송 시스템의 전체 현황을 확인합니다.</p>
            </div>

            <button
              type="button"
              class="refresh-btn"
              @click="fetchDashboardData"
            >
              새로고침
            </button>
          </div>

          <div v-if="loading" class="loading-msg">
            데이터를 불러오는 중입니다.
          </div>

          <template v-else>
            <div class="kpi-grid">
              <div class="kpi-card" @click="goToVehicle">
                <span class="kpi-label">배차 가능 차량</span>

                <div class="kpi-value-group">
                  <span class="kpi-value highlight">
                    {{ inactiveVehicleCount }}
                  </span>
                  <span class="kpi-total"> / {{ vehicles.length }}대 </span>
                </div>
              </div>

              <div class="kpi-card" @click="goToOperation">
                <span class="kpi-label">현재 운행 중</span>

                <div class="kpi-value-group">
                  <span class="kpi-value highlight">
                    {{ inProgressDispatchCount }}
                  </span>
                  <span class="kpi-total">건</span>
                </div>
              </div>

              <div class="kpi-card" @click="goToDriver">
                <span class="kpi-label">근무 중 기사</span>

                <div class="kpi-value-group">
                  <span class="kpi-value highlight">
                    {{ onDutyDriverCount }}
                  </span>
                  <span class="kpi-total"> / {{ drivers.length }}명 </span>
                </div>
              </div>

              <div class="kpi-card" @click="goToVehicle">
                <span class="kpi-label">운행 가능 차량</span>

                <div class="kpi-value-group">
                  <span class="kpi-value highlight">
                    {{ inactiveVehicleCount }}
                  </span>
                  <span class="kpi-total"> / {{ vehicles.length }}대 </span>
                </div>
              </div>
            </div>

            <div class="home-sections-grid">
              <section class="status-panel">
                <div class="panel-header">
                  <h3>오늘의 운행 현황</h3>

                  <button type="button" class="more-btn" @click="goToOperation">
                    운행 관리 →
                  </button>
                </div>

                <ul class="status-list">
                  <li>
                    <span class="tag blue">대기</span>
                    <span>배차 대기</span>
                    <strong class="status-number">
                      {{ scheduledDispatchCount }}건
                    </strong>
                  </li>

                  <li>
                    <span class="tag green">운행</span>
                    <span>현재 운행 중</span>
                    <strong class="status-number">
                      {{ inProgressDispatchCount }}건
                    </strong>
                  </li>

                  <li>
                    <span class="tag gray">완료</span>
                    <span>운행 종료</span>
                    <strong class="status-number">
                      {{ completedDispatchCount }}건
                    </strong>
                  </li>
                </ul>
              </section>

              <section class="status-panel">
                <div class="panel-header">
                  <h3>기사 현황</h3>

                  <button type="button" class="more-btn" @click="goToDriver">
                    기사 관리 →
                  </button>
                </div>

                <ul class="status-list">
                  <li v-for="driver in drivers.slice(0, 5)" :key="driver.id">
                    <span
                      :class="['tag', getDriverStatusClass(driver.workStatus)]"
                    >
                      {{ getDriverStatusText(driver.workStatus) }}
                    </span>

                    <span>{{ driver.name }}</span>

                    <span class="status-detail">
                      {{ driver.phoneNumber }}
                    </span>
                  </li>

                  <li v-if="drivers.length === 0">
                    <span class="empty-status"> 등록된 기사가 없습니다. </span>
                  </li>
                </ul>
              </section>

              <section class="status-panel">
                <div class="panel-header">
                  <h3>차량 현황</h3>

                  <button type="button" class="more-btn" @click="goToVehicle">
                    차량 관리 →
                  </button>
                </div>

                <ul class="status-list">
                  <li>
                    <span class="tag green">운행</span>
                    <span>운행 중</span>
                    <strong class="status-number">
                      {{ activeVehicleCount }}대
                    </strong>
                  </li>

                  <li>
                    <span class="tag orange">정비</span>
                    <span>정비 중</span>
                    <strong class="status-number">
                      {{ maintenanceVehicleCount }}대
                    </strong>
                  </li>

                  <li>
                    <span class="tag gray">대기</span>
                    <span>운행 대기</span>
                    <strong class="status-number">
                      {{ inactiveVehicleCount }}대
                    </strong>
                  </li>
                </ul>
              </section>

              <section class="status-panel">
                <div class="panel-header">
                  <h3>기사 근무 현황</h3>

                  <button type="button" class="more-btn" @click="goToDriver">
                    전체 보기 →
                  </button>
                </div>

                <ul class="status-list">
                  <li>
                    <span class="tag green">근무</span>
                    <span>근무 중</span>
                    <strong class="status-number">
                      {{ onDutyDriverCount }}명
                    </strong>
                  </li>

                  <li>
                    <span class="tag blue">대기</span>
                    <span>대기</span>
                    <strong class="status-number">
                      {{ standbyDriverCount }}명
                    </strong>
                  </li>

                  <li>
                    <span class="tag gray">휴무</span>
                    <span>휴무</span>
                    <strong class="status-number">
                      {{ offDutyDriverCount }}명
                    </strong>
                  </li>

                  <li>
                    <span class="tag orange">퇴사</span>
                    <span>퇴사</span>
                    <strong class="status-number">
                      {{ retiredDriverCount }}명
                    </strong>
                  </li>
                </ul>
              </section>
            </div>

            <section class="recent-dispatch-panel">
              <div class="panel-header">
                <h3>최근 배차 현황</h3>

                <button type="button" class="more-btn" @click="goToDispatch">
                  배차 관리 →
                </button>
              </div>

              <div class="recent-table-wrapper">
                <table class="recent-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>노선</th>
                      <th>예정 출발</th>
                      <th>상태</th>
                    </tr>
                  </thead>

                  <tbody>
                    <tr
                      v-for="dispatch in dispatches.slice(0, 5)"
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
                            {{ dispatch.routeName || "-" }}
                          </span>
                        </div>
                      </td>

                      <td class="font-mono">
                        {{
                          dispatch.plannedStartTime
                            ? dispatch.plannedStartTime
                                .replace("T", " ")
                                .substring(0, 16)
                            : "-"
                        }}
                      </td>

                      <td>
                        <span
                          :class="[
                            'tag',
                            getDispatchStatusClass(dispatch.dispatchStatus),
                          ]"
                        >
                          {{ getDispatchStatusText(dispatch.dispatchStatus) }}
                        </span>
                      </td>
                    </tr>

                    <tr v-if="dispatches.length === 0">
                      <td colspan="4" class="empty-msg">
                        등록된 배차가 없습니다.
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
          </template>
        </div>

        <div v-else class="content-card">
          <template v-if="activeMenu === 'driver'">
            <DriverList
              v-if="activeAction === 'list'"
              @go-add="goToAdd"
              @go-detail="goToDetail"
            />

            <AddDriver v-else-if="activeAction === 'add'" @back="goToList" />

            <DriverDetails
              v-else-if="activeAction === 'detail'"
              :id="selectedId"
              @back="goToList"
            />
          </template>

          <template v-else-if="activeMenu === 'vehicle'">
            <VehicleList
              v-if="activeAction === 'list'"
              @go-add="goToAdd"
              @go-detail="goToDetail"
            />

            <AddVehicle v-else-if="activeAction === 'add'" @back="goToList" />

            <VehicleDetails
              v-else-if="activeAction === 'detail'"
              :id="selectedId"
              @back="goToList"
            />
          </template>

          <template v-else-if="activeMenu === 'route'">
            <RouteList
              v-if="activeAction === 'list'"
              @go-add="goToAdd"
              @go-detail="goToDetail"
            />

            <AddRoute v-else-if="activeAction === 'add'" @back="goToList" />

            <RouteDetails
              v-else-if="activeAction === 'detail'"
              :id="selectedId"
              @back="goToList"
            />
          </template>

          <template v-else-if="activeMenu === 'dispatch'">
            <DispatchList v-if="activeAction === 'list'" @detail="goToDetail" />

            <AddDispatch v-else-if="activeAction === 'add'" @back="goToList" />

            <DispatchDetails
              v-else-if="activeAction === 'detail'"
              :id="selectedId"
              @back="goToList"
            />
          </template>

          <template v-else-if="activeMenu === 'operation'">
            <Operation />
          </template>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.dashboard-wrapper {
  min-height: 100vh;
  background-color: #0b131e;
  color: #ffffff;
  display: flex;
  flex-direction: column;
}

.dashboard-nav {
  background-color: #151f2e;
  border-bottom: 1px solid #1e293b;
  padding: 0 24px;
}

.nav-links {
  list-style: none;
  display: flex;
  align-items: center;
  gap: 30px;
  margin: 0;
  padding: 0;
  height: 70px;
}

.logo-item .logo-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
}

.dashboard-logo {
  height: 36px;
  width: auto;
}

.nav-item {
  height: 100%;
}

.nav-btn {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  padding: 0 12px;
  height: 100%;
  transition: color 0.2s;
}

.nav-btn:hover,
.nav-btn.active {
  color: #ffffff;
}

.logout-item {
  height: 100%;
  margin-left: auto;
  display: flex;
  align-items: center;
}

.logout-btn {
  background: transparent;
  border: 1px solid #334155;
  color: #94a3b8;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  padding: 9px 16px;
  border-radius: 7px;
  transition: all 0.2s;
}

.logout-btn:hover {
  color: #ffffff;
  border-color: #ef4444;
  background-color: rgba(239, 68, 68, 0.1);
}

.dashboard-container {
  padding: 30px;
  flex: 1;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}

.content-body {
  width: 100%;
}

.dashboard-home {
  width: 100%;
}

.home-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 30px;
}

.home-header h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
}

.home-header p {
  margin: 8px 0 0;
  font-size: 14px;
  color: #94a3b8;
}

.refresh-btn {
  padding: 10px 18px;
  background-color: #151f2e;
  color: #ffffff;
  border: 1px solid #1e293b;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.refresh-btn:hover {
  border-color: #3b82f6;
  color: #60a5fa;
}

.loading-msg {
  background-color: #151f2e;
  border: 1px solid #1e293b;
  border-radius: 12px;
  padding: 50px;
  text-align: center;
  color: #94a3b8;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.kpi-card {
  background-color: #151f2e;
  border: 1px solid #1e293b;
  border-radius: 12px;
  padding: 24px;
  cursor: pointer;
  transition:
    transform 0.2s,
    border-color 0.2s;
}

.kpi-card:hover {
  transform: translateY(-3px);
  border-color: #3b82f6;
}

.kpi-label {
  font-size: 14px;
  color: #94a3b8;
  display: block;
  margin-bottom: 12px;
}

.kpi-value-group {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.kpi-value {
  font-size: 28px;
  font-weight: bold;
  color: #ffffff;
}

.kpi-value.highlight {
  color: #60a5fa;
}

.kpi-total {
  font-size: 14px;
  color: #64748b;
}

.home-sections-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.status-panel,
.recent-dispatch-panel {
  background-color: #151f2e;
  border: 1px solid #1e293b;
  border-radius: 12px;
  padding: 24px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.panel-header h3 {
  font-size: 18px;
  font-weight: bold;
  margin: 0;
}

.more-btn {
  background: none;
  border: none;
  color: #60a5fa;
  font-size: 13px;
  cursor: pointer;
}

.more-btn:hover {
  text-decoration: underline;
}

.status-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: #0b131e;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
}

.status-number {
  margin-left: auto;
  color: #ffffff;
}

.status-detail {
  margin-left: auto;
  color: #64748b;
  font-size: 12px;
}

.empty-status {
  color: #64748b;
}

.tag {
  min-width: 48px;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  text-align: center;
}

.tag.blue {
  background-color: #1e3a8a;
  color: #60a5fa;
}

.tag.green {
  background-color: #064e3b;
  color: #34d399;
}

.tag.gray {
  background-color: #1e293b;
  color: #94a3b8;
}

.tag.orange {
  background-color: #7c2d12;
  color: #fb923c;
}

.recent-table-wrapper {
  overflow-x: auto;
}

.recent-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.recent-table th {
  padding: 12px;
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
  border-bottom: 1px solid #1e293b;
  white-space: nowrap;
}

.recent-table td {
  padding: 14px 12px;
  font-size: 14px;
  color: #ffffff;
  border-bottom: 1px solid #1e293b;
  white-space: nowrap;
}

.recent-table tr:last-child td {
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
  color: #94a3b8;
  font-size: 12px;
}

.font-mono {
  color: #94a3b8;
  font-family: monospace;
}

.empty-msg {
  text-align: center;
  padding: 40px !important;
  color: #64748b !important;
}

.content-card {
  background-color: #151f2e;
  border: 1px solid #1e293b;
  border-radius: 12px;
  padding: 40px;
}

@media (max-width: 1100px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 800px) {
  .home-sections-grid {
    grid-template-columns: 1fr;
  }

  .nav-links {
    gap: 10px;
  }

  .nav-btn {
    font-size: 14px;
    padding: 0 8px;
  }

  .logout-btn {
    padding: 8px 10px;
    font-size: 12px;
  }
}
</style>
