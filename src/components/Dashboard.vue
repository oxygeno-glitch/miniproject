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
const routes = ref([]);

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

// 홈 대시보드의 알림/지표에서 배차·차량 상세 화면으로 바로 이동하기 위한 헬퍼
// (메뉴 전환 + 상세 화면 진입을 한 번에 처리합니다)
const goToDispatchDetail = (id) => {
  activeMenu.value = "dispatch";
  selectedId.value = id;
  activeAction.value = "detail";
};

const goToVehicleDetail = (id) => {
  activeMenu.value = "vehicle";
  selectedId.value = id;
  activeAction.value = "detail";
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
    const [driverResponse, vehicleResponse, dispatchResponse, routeResponse] =
      await Promise.all([
        api.get("/drivers"),
        api.get("/vehicles"),
        api.get("/dispatches"),
        api.get("/routes"),
      ]);

    drivers.value = driverResponse.data;
    vehicles.value = vehicleResponse.data;
    dispatches.value = dispatchResponse.data;
    routes.value = routeResponse.data;
  } catch (error) {
    console.error("대시보드 데이터 조회 실패:", error);
  } finally {
    loading.value = false;
  }
};

// ---------------------------------------------------------------------------
// 공통 유틸
// ---------------------------------------------------------------------------

// plannedStartTime 기준으로 "오늘" 예정된 배차인지 판단합니다.
const isToday = (dateString) => {
  if (!dateString) return false;

  const target = new Date(dateString);
  const now = new Date();

  return (
    target.getFullYear() === now.getFullYear() &&
    target.getMonth() === now.getMonth() &&
    target.getDate() === now.getDate()
  );
};

// 배차 데이터에는 '지연' 상태가 별도로 없기 때문에, 예정 시간과 현재 시간을
// 비교하여 지연 여부를 계산합니다.
// - 배차 대기(SCHEDULED)인데 예정 출발 시간이 이미 지난 경우
// - 운행 중(IN_PROGRESS)인데 예정 도착 시간이 이미 지난 경우
const isDelayedDispatch = (dispatch) => {
  const now = new Date();

  if (dispatch.dispatchStatus === "SCHEDULED") {
    return (
      !!dispatch.plannedStartTime &&
      new Date(dispatch.plannedStartTime) < now
    );
  }

  if (dispatch.dispatchStatus === "IN_PROGRESS") {
    return (
      !!dispatch.plannedEndTime && new Date(dispatch.plannedEndTime) < now
    );
  }

  return false;
};

const isActiveDispatch = (dispatch) =>
  dispatch.dispatchStatus === "SCHEDULED" ||
  dispatch.dispatchStatus === "IN_PROGRESS";

const timeRangesOverlap = (aStart, aEnd, bStart, bEnd) =>
  aStart < bEnd && bStart < aEnd;

// ---------------------------------------------------------------------------
// 1. 실시간 운행 현황 요약 (Status Overview)
// ---------------------------------------------------------------------------

const vehicleStats = computed(() => {
  const total = vehicles.value.length;
  const active = vehicles.value.filter((v) => v.status === "ACTIVE").length;
  const inactive = vehicles.value.filter(
    (v) => v.status === "INACTIVE"
  ).length;
  const maintenance = vehicles.value.filter(
    (v) => v.status === "MAINTENANCE"
  ).length;

  return { total, active, inactive, maintenance };
});

const vehicleActiveRatio = computed(() => {
  const { total, active } = vehicleStats.value;
  return total > 0 ? Math.round((active / total) * 100) : 0;
});

// 차량 상태 비율을 표현하는 도넛 차트 배경(conic-gradient)
const donutStyle = computed(() => {
  const { total, active, inactive } = vehicleStats.value;

  if (total === 0) {
    return { background: "#1e293b" };
  }

  const activeEnd = (active / total) * 100;
  const inactiveEnd = activeEnd + (inactive / total) * 100;

  return {
    background: `conic-gradient(#22c55e 0% ${activeEnd}%, #60a5fa ${activeEnd}% ${inactiveEnd}%, #fb923c ${inactiveEnd}% 100%)`,
  };
});

const todaysDispatches = computed(() =>
  dispatches.value.filter((dispatch) => isToday(dispatch.plannedStartTime))
);

const dispatchSummary = computed(() => {
  const list = todaysDispatches.value;

  return {
    total: list.length,
    completed: list.filter((d) => d.dispatchStatus === "COMPLETED").length,
    inProgress: list.filter((d) => d.dispatchStatus === "IN_PROGRESS").length,
    canceled: list.filter((d) => d.dispatchStatus === "CANCELED").length,
    delayed: list.filter(isDelayedDispatch).length,
  };
});

// ---------------------------------------------------------------------------
// 2. 자원 및 인력 현황 (Resources & Drivers)
// ---------------------------------------------------------------------------

// 정비 중인 차량을 제외한, 당일 투입 가능한 차량 수
const deployableVehicleCount = computed(
  () => vehicleStats.value.active + vehicleStats.value.inactive
);

// 운전자 근무 상태는 ON_DUTY(운행 중) / STANDBY(대기) / OFF_DUTY(휴게) /
// RETIRED(퇴사) 4가지로 관리되고 있어, 이를 각각 "운행 중 / 출근 완료(대기) /
// 휴게 중 / 결근·휴가"에 대응해 표시합니다.
const driverStats = computed(() => ({
  standby: drivers.value.filter((d) => d.workStatus === "STANDBY").length,
  onDuty: drivers.value.filter((d) => d.workStatus === "ON_DUTY").length,
  offDuty: drivers.value.filter((d) => d.workStatus === "OFF_DUTY").length,
  retired: drivers.value.filter((d) => d.workStatus === "RETIRED").length,
}));

const staffRiskWarning = computed(() => {
  if (vehicleStats.value.inactive > 0 && driverStats.value.standby === 0) {
    return "즉시 배차 가능한 대기 차량은 있지만, 배차 가능한 대기 기사가 없습니다. 인력 부족이 우려됩니다.";
  }

  if (driverStats.value.standby === 0 && driverStats.value.onDuty === 0) {
    return "현재 근무 가능한(대기·운행 중) 기사가 없습니다. 인력 현황을 확인해주세요.";
  }

  return "";
});

// ---------------------------------------------------------------------------
// 3. 노선별 운영 지표 (Route Metrics)
// ---------------------------------------------------------------------------

const routeMetricsAll = computed(() => {
  const activeRoutes = routes.value.filter(
    (route) => route.status === "ACTIVE"
  );

  return activeRoutes.map((route) => {
    const routeDispatches = todaysDispatches.value.filter(
      (dispatch) => dispatch.routeId === route.id
    );

    const total = routeDispatches.length;
    const inProgress = routeDispatches.filter(
      (dispatch) => dispatch.dispatchStatus === "IN_PROGRESS"
    ).length;
    const delayed = routeDispatches.filter(isDelayedDispatch).length;
    const onTimeRate =
      total > 0 ? Math.round(((total - delayed) / total) * 100) : null;

    return {
      id: route.id,
      routeNumber: route.routeNumber,
      routeName: route.routeName,
      total,
      inProgress,
      delayed,
      onTimeRate,
    };
  });
});

// 배차량이 많은(=이용객이 많은 핵심 노선으로 간주) 상위 노선을 보여줍니다.
const routeMetrics = computed(() =>
  [...routeMetricsAll.value].sort((a, b) => b.total - a.total).slice(0, 6)
);

// 지연이 발생 중인 노선의 특이사항 경고
const routeAlerts = computed(() =>
  routeMetricsAll.value.filter((route) => route.delayed > 0)
);

// ---------------------------------------------------------------------------
// 4. 긴급 알림 및 실시간 이슈 (Alerts & Notifications)
// ---------------------------------------------------------------------------

const severityRank = { danger: 0, warning: 1, info: 2 };

const alerts = computed(() => {
  const list = [];
  const activeDispatches = dispatches.value.filter(isActiveDispatch);

  // 지연 배차
  activeDispatches.filter(isDelayedDispatch).forEach((dispatch) => {
    list.push({
      id: `delay-${dispatch.id}`,
      severity: "warning",
      badge: "지연",
      message: `${dispatch.routeNumber || "-"} ${
        dispatch.routeName || ""
      } 노선 배차 #${dispatch.id} (차량 ${
        dispatch.vehiclePlateNumber || "-"
      } · 기사 ${dispatch.driverName || "-"}) 운행이 지연되고 있습니다.`,
      action: () => goToDispatchDetail(dispatch.id),
    });
  });

  // 배차 충돌(동일 차량/기사에게 겹치는 시간대로 배차된 경우)
  for (let i = 0; i < activeDispatches.length; i++) {
    for (let j = i + 1; j < activeDispatches.length; j++) {
      const a = activeDispatches[i];
      const b = activeDispatches[j];

      const sameVehicle = a.vehicleId != null && a.vehicleId === b.vehicleId;
      const sameDriver = a.driverId != null && a.driverId === b.driverId;

      if (!sameVehicle && !sameDriver) continue;
      if (!a.plannedStartTime || !a.plannedEndTime) continue;
      if (!b.plannedStartTime || !b.plannedEndTime) continue;

      const overlap = timeRangesOverlap(
        new Date(a.plannedStartTime),
        new Date(a.plannedEndTime),
        new Date(b.plannedStartTime),
        new Date(b.plannedEndTime)
      );

      if (!overlap) continue;

      const conflictTarget = sameVehicle
        ? `차량 ${a.vehiclePlateNumber || "-"}`
        : `기사 ${a.driverName || "-"}`;

      list.push({
        id: `conflict-${a.id}-${b.id}`,
        severity: "danger",
        badge: "배차 충돌",
        message: `${conflictTarget}에 배차 #${a.id}와 #${b.id}의 운행 시간이 겹칩니다. 배차를 확인해주세요.`,
        action: () => goToDispatchDetail(a.id),
      });
    }
  }

  // 정비 중인 차량 (돌발 정비/고장으로 배차에서 제외된 차량으로 간주)
  vehicles.value
    .filter((vehicle) => vehicle.status === "MAINTENANCE")
    .forEach((vehicle) => {
      list.push({
        id: `maintenance-${vehicle.id}`,
        severity: "info",
        badge: "정비 중",
        message: `차량 ${vehicle.plateNumber} (${
          vehicle.modelName || "-"
        })가 정비 중이라 배차에서 제외됩니다.`,
        action: () => goToVehicleDetail(vehicle.id),
      });
    });

  return list.sort(
    (x, y) => severityRank[x.severity] - severityRank[y.severity]
  );
});

const goToDriver = () => setMenu("driver");
const goToVehicle = () => setMenu("vehicle");
const goToRoute = () => setMenu("route");
const goToDispatch = () => setMenu("dispatch");
const goToOperation = () => setMenu("operation");

onMounted(() => {
  fetchDashboardData();
});
</script>

<template>
  <div class="dashboard-wrapper">
    <nav class="dashboard-nav">
      <ul class="nav-links">
        <li class="brand-item">
          <button
            type="button"
            class="brand-link"
            @click="setMenu('home')"
          >
            <img :src="logoImg" alt="STUDIO G Logo" class="nav-logo" />
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
            <!-- 1. 실시간 운행 현황 요약 -->
            <section class="ov-section">
              <div class="ov-section-header">
                <h2>실시간 운행 현황 요약</h2>
                <span class="ov-section-desc">
                  전체 차량 및 당일 배차의 운행 상태를 한눈에 확인합니다.
                </span>
              </div>

              <div class="ov-grid ov-grid-2">
                <div class="ov-card">
                  <div class="ov-card-title">차량 현황</div>

                  <div class="donut-row">
                    <div class="donut-chart" :style="donutStyle">
                      <div class="donut-center">
                        <strong>{{ vehicleActiveRatio }}%</strong>
                        <span>정상 운행</span>
                      </div>
                    </div>

                    <ul class="donut-legend">
                      <li>
                        <span class="dot total"></span>전체
                        <strong>{{ vehicleStats.total }}대</strong>
                      </li>
                      <li>
                        <span class="dot active"></span>운행 중
                        <strong>{{ vehicleStats.active }}대</strong>
                      </li>
                      <li>
                        <span class="dot inactive"></span>대기 중
                        <strong>{{ vehicleStats.inactive }}대</strong>
                      </li>
                      <li>
                        <span class="dot maintenance"></span>정비 중
                        <strong>{{ vehicleStats.maintenance }}대</strong>
                      </li>
                    </ul>
                  </div>
                </div>

                <div class="ov-card">
                  <div class="ov-card-title">
                    오늘의 배차 현황
                    <span class="ov-card-sub">
                      전체 {{ dispatchSummary.total }}건
                    </span>
                  </div>

                  <ul class="mini-stat-list">
                    <li>
                      <span class="tag green">완료</span>
                      <span>운행 완료</span>
                      <strong>{{ dispatchSummary.completed }}건</strong>
                    </li>
                    <li>
                      <span class="tag blue">진행</span>
                      <span>진행 중</span>
                      <strong>{{ dispatchSummary.inProgress }}건</strong>
                    </li>
                    <li>
                      <span class="tag orange">지연</span>
                      <span>지연 발생</span>
                      <strong>{{ dispatchSummary.delayed }}건</strong>
                    </li>
                    <li>
                      <span class="tag gray">취소</span>
                      <span>배차 취소</span>
                      <strong>{{ dispatchSummary.canceled }}건</strong>
                    </li>
                  </ul>
                </div>
              </div>
            </section>

            <!-- 2. 자원 및 인력 현황 -->
            <section class="ov-section">
              <div class="ov-section-header">
                <h2>자원 및 인력 현황</h2>
                <span class="ov-section-desc">
                  투입 가능한 차량과 운전자 인력 현황으로 부족 리스크를 사전에 확인합니다.
                </span>
              </div>

              <div v-if="staffRiskWarning" class="risk-banner">
                ⚠ {{ staffRiskWarning }}
              </div>

              <div class="ov-grid ov-grid-2">
                <div class="ov-card">
                  <div class="ov-card-title">가동 가능한 자원 현황</div>

                  <div class="resource-stat-row">
                    <div class="resource-stat">
                      <span class="resource-value">
                        {{ deployableVehicleCount }}
                        <small>/ {{ vehicleStats.total }}대</small>
                      </span>
                      <span class="resource-label">
                        당일 투입 가능 차량 (정비 제외)
                      </span>
                    </div>

                    <div class="resource-stat">
                      <span class="resource-value highlight-blue">
                        {{ vehicleStats.inactive }}<small>대</small>
                      </span>
                      <span class="resource-label">
                        즉시 배차 가능한 대기 차량
                      </span>
                    </div>
                  </div>
                </div>

                <div class="ov-card">
                  <div class="ov-card-title">운전자 근무 현황</div>

                  <ul class="mini-stat-list">
                    <li>
                      <span class="tag blue">대기</span>
                      <span>출근 완료 (대기)</span>
                      <strong>{{ driverStats.standby }}명</strong>
                    </li>
                    <li>
                      <span class="tag green">운행</span>
                      <span>운행 중</span>
                      <strong>{{ driverStats.onDuty }}명</strong>
                    </li>
                    <li>
                      <span class="tag gray">휴게</span>
                      <span>휴게 중</span>
                      <strong>{{ driverStats.offDuty }}명</strong>
                    </li>
                    <li>
                      <span class="tag orange">결근/휴가</span>
                      <span>결근 / 휴가</span>
                      <strong>{{ driverStats.retired }}명</strong>
                    </li>
                  </ul>
                </div>
              </div>
            </section>

            <!-- 3. 노선별 운영 지표 -->
            <section class="ov-section">
              <div class="ov-section-header">
                <h2>노선별 운영 지표</h2>
                <span class="ov-section-desc">
                  배차가 많은 주요 노선의 실시간 운행률과 지연 현황입니다.
                </span>
                <button type="button" class="more-btn ov-section-link" @click="goToRoute">
                  노선 관리 →
                </button>
              </div>

              <div v-if="routeAlerts.length > 0" class="route-alert-banner">
                <div
                  v-for="alert in routeAlerts"
                  :key="alert.id"
                  class="route-alert-item"
                >
                  ⚠ <strong>{{ alert.routeNumber }} {{ alert.routeName || "" }}</strong>
                  노선에서 배차 {{ alert.delayed }}건이 지연되고 있습니다.
                </div>
              </div>

              <div class="ov-card">
                <table class="route-metric-table">
                  <thead>
                    <tr>
                      <th>노선</th>
                      <th>오늘 배차</th>
                      <th>운행 중</th>
                      <th>지연</th>
                      <th>정시율</th>
                      <th>상태</th>
                    </tr>
                  </thead>

                  <tbody>
                    <tr v-for="route in routeMetrics" :key="route.id">
                      <td>
                        <div class="route-info">
                          <span class="route-number">
                            {{ route.routeNumber }}
                          </span>
                          <span class="route-name">
                            {{ route.routeName || "-" }}
                          </span>
                        </div>
                      </td>

                      <td class="font-mono">{{ route.total }}건</td>
                      <td class="font-mono">{{ route.inProgress }}건</td>
                      <td class="font-mono">{{ route.delayed }}건</td>
                      <td class="font-mono">
                        {{ route.total > 0 ? route.onTimeRate + "%" : "-" }}
                      </td>

                      <td>
                        <span
                          :class="['tag', route.delayed > 0 ? 'orange' : 'green']"
                        >
                          {{ route.delayed > 0 ? "지연 발생" : "정상" }}
                        </span>
                      </td>
                    </tr>

                    <tr v-if="routeMetrics.length === 0">
                      <td colspan="6" class="empty-msg">
                        운행 중인 노선이 없습니다.
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <!-- 4. 긴급 알림 및 실시간 이슈 -->
            <section class="ov-section">
              <div class="ov-section-header">
                <h2>긴급 알림 및 실시간 이슈</h2>
                <span class="ov-section-desc">
                  즉각적인 조치가 필요한 이벤트입니다. 항목을 클릭하면 상세 화면으로 이동합니다.
                </span>
              </div>

              <div class="ov-card alert-card">
                <ul v-if="alerts.length > 0" class="alert-list">
                  <li
                    v-for="alert in alerts"
                    :key="alert.id"
                    class="alert-item"
                    @click="alert.action"
                  >
                    <span class="alert-badge" :class="alert.severity">
                      {{ alert.badge }}
                    </span>
                    <span class="alert-message">{{ alert.message }}</span>
                    <span class="alert-goto">바로가기 →</span>
                  </li>
                </ul>

                <div v-else class="empty-alert">
                  현재 긴급 알림이 없습니다.
                </div>
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
  background-color: var(--color-background);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
}

.dashboard-nav {
  background-color: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
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

.brand-item {
  height: 100%;
  display: flex;
  align-items: center;
  padding-right: 22px;
  margin-right: 8px;
  border-right: 1px solid var(--color-border);
}

.brand-link {
  background: none;
  border: none;
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 0;
  transition: opacity 0.2s;
}

.brand-link:hover {
  opacity: 0.85;
}

.nav-logo {
  height: 32px;
  width: auto;
}

.nav-item {
  height: 100%;
  position: relative;
}

.nav-btn {
  background: none;
  border: none;
  color: var(--color-text-secondary);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  padding: 0 12px;
  height: 100%;
  transition: color 0.2s;
}

.nav-btn:hover,
.nav-btn.active {
  color: var(--color-text-primary);
}

.nav-item::after {
  content: "";
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 0;
  height: 2px;
  border-radius: 2px;
  background: var(--color-primary-gradient);
  opacity: 0;
  transform: scaleX(0.6);
  transition: opacity 0.2s, transform 0.2s;
}

.nav-item:has(.nav-btn.active)::after {
  opacity: 1;
  transform: scaleX(1);
}

.logout-item {
  height: 100%;
  margin-left: auto;
  display: flex;
  align-items: center;
}

.logout-btn {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  padding: 9px 16px;
  border-radius: var(--radius-md);
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
  font-weight: 800;
  letter-spacing: var(--tracking-tight);
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

/* ---------- 홈 대시보드 섹션 공통 ---------- */

.ov-section {
  margin-bottom: 28px;
}

.ov-section-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.ov-section-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #ffffff;
}

.ov-section-desc {
  font-size: 13px;
  color: #64748b;
}

.ov-section-link {
  margin-left: auto;
}

.ov-grid {
  display: grid;
  gap: 20px;
}

.ov-grid-2 {
  grid-template-columns: repeat(2, 1fr);
}

.ov-card {
  background-color: #151f2e;
  border: 1px solid #1e293b;
  border-radius: 12px;
  padding: 24px;
}

.ov-card-title {
  font-size: 15px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ov-card-sub {
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
}

/* ---------- 1. 실시간 운행 현황 요약 ---------- */

.donut-row {
  display: flex;
  align-items: center;
  gap: 28px;
  flex-wrap: wrap;
}

.donut-chart {
  position: relative;
  width: 120px;
  height: 120px;
  min-width: 120px;
  border-radius: 50%;
}

.donut-chart::before {
  content: "";
  position: absolute;
  inset: 16px;
  background-color: #151f2e;
  border-radius: 50%;
}

.donut-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.donut-center strong {
  font-size: 22px;
  color: #ffffff;
}

.donut-center span {
  font-size: 11px;
  color: #64748b;
}

.donut-legend {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13px;
  color: #cbd5e1;
  flex: 1;
  min-width: 140px;
}

.donut-legend li {
  display: flex;
  align-items: center;
  gap: 8px;
}

.donut-legend strong {
  margin-left: auto;
  color: #ffffff;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}

.dot.total {
  background-color: #334155;
}

.dot.active {
  background-color: #22c55e;
}

.dot.inactive {
  background-color: #60a5fa;
}

.dot.maintenance {
  background-color: #fb923c;
}

.mini-stat-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mini-stat-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: #0b131e;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  color: #cbd5e1;
}

.mini-stat-list strong {
  margin-left: auto;
  color: #ffffff;
}

/* ---------- 2. 자원 및 인력 현황 ---------- */

.resource-stat-row {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.resource-stat {
  flex: 1;
  min-width: 160px;
  background-color: #0b131e;
  border-radius: 8px;
  padding: 18px;
  text-align: center;
}

.resource-value {
  display: block;
  font-size: 26px;
  font-weight: 700;
  color: #ffffff;
}

.resource-value small {
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  margin-left: 2px;
}

.resource-value.highlight-blue {
  color: #60a5fa;
}

.resource-label {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: #94a3b8;
}

.risk-banner {
  background-color: rgba(251, 146, 60, 0.12);
  border: 1px solid rgba(251, 146, 60, 0.4);
  color: #fb923c;
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 16px;
}

/* ---------- 3. 노선별 운영 지표 ---------- */

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

.route-alert-banner {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.route-alert-item {
  background-color: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.35);
  color: #f87171;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 13px;
}

.route-metric-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.route-metric-table th {
  padding: 12px;
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
  border-bottom: 1px solid #1e293b;
  white-space: nowrap;
}

.route-metric-table td {
  padding: 14px 12px;
  font-size: 14px;
  color: #ffffff;
  border-bottom: 1px solid #1e293b;
  white-space: nowrap;
}

.route-metric-table tr:last-child td {
  border-bottom: none;
}

/* ---------- 4. 긴급 알림 및 실시간 이슈 ---------- */

.alert-card {
  padding: 12px;
}

.alert-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: #0b131e;
  border: 1px solid #1e293b;
  border-radius: 8px;
  padding: 14px 16px;
  cursor: pointer;
  transition: border-color 0.2s, background-color 0.2s;
}

.alert-item:hover {
  border-color: #3b82f6;
  background-color: #111b29;
}

.alert-badge {
  flex-shrink: 0;
  min-width: 64px;
  text-align: center;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
}

.alert-badge.danger {
  background-color: rgba(239, 68, 68, 0.15);
  color: #f87171;
}

.alert-badge.warning {
  background-color: rgba(251, 146, 60, 0.15);
  color: #fb923c;
}

.alert-badge.info {
  background-color: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
}

.alert-message {
  flex: 1;
  font-size: 14px;
  color: #e2e8f0;
}

.alert-goto {
  flex-shrink: 0;
  font-size: 12px;
  color: #60a5fa;
  font-weight: 600;
}

.empty-alert {
  text-align: center;
  padding: 32px;
  color: #64748b;
  font-size: 14px;
}

/* ---------- 공통 태그/텍스트 ---------- */

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
  .ov-grid-2 {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 800px) {
  .ov-grid-2 {
    grid-template-columns: 1fr;
  }

  .donut-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .ov-section-link {
    margin-left: 0;
  }

  .nav-links {
    gap: 10px;
  }

  .nav-btn {
    font-size: 14px;
    padding: 0 8px;
  }

  .brand-item {
    padding-right: 12px;
    margin-right: 4px;
  }

  .nav-logo {
    height: 26px;
  }

  .logout-btn {
    padding: 8px 10px;
    font-size: 12px;
  }
}
</style>
