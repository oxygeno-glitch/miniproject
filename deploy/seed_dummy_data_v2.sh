#!/usr/bin/env bash
#
# STUDIO G 대규모 더미 데이터 시딩 스크립트 (v2)
#
# 정류장 150개 / 노선 40개 / 차량 87대 / 기사 145명 / 배차 3건을 생성합니다.
# 로그인 계정(users 테이블)은 건드리지 않으므로 기존 데모 계정은 그대로 유지됩니다.
#
# 실행 전제:
#   1) 이 스크립트는 STUDIO G 백엔드가 떠 있는 서버에서 직접 실행합니다 (http://localhost:8080).
#   2) 기존 더미 데이터(정류장/노선/차량/기사/배차/정비이력)를 먼저 비웠어야 합니다.
#      (아래 DEPLOY 안내의 TRUNCATE 명령 참고)
#   3) curl, jq 가 필요합니다. jq가 없으면 자동으로 설치를 시도합니다.
#
# 사용법:
#   chmod +x seed_dummy_data_v2.sh
#   ./seed_dummy_data_v2.sh

set -euo pipefail

BASE_URL="http://localhost:8080/api"

if ! command -v jq >/dev/null 2>&1; then
  echo "jq가 설치되어 있지 않아 설치를 시도합니다..."
  sudo apt-get update -y && sudo apt-get install -y jq
fi

echo "=== STUDIO G 더미 데이터 시딩 시작 ==="

# ---------------------------------------------------------------------------
# 1. 정류장(Stop) 150개 생성
# ---------------------------------------------------------------------------

DISTRICTS=(강남 서초 송파 강동 마포 용산 종로 중구 성동 광진 동대문 중랑 성북 강북 도봉 노원 은평 서대문 양천 강서 구로 금천 영등포 동작 관악 서울역 강남역 홍대입구 잠실 사당)
NUM_DISTRICTS=${#DISTRICTS[@]}

STOP_IDS=()

echo "--- 정류장 150개 생성 중 ---"
for i in $(seq 1 150); do
  district="${DISTRICTS[$(( (i - 1) % NUM_DISTRICTS ))]}"
  stop_code=$(printf "STP-%03d" "$i")
  stop_name="${district} ${i}번 정류장"

  # 서울 인근 좌표 범위(대략 37.42~37.70, 126.76~127.18)에서 임의 좌표 생성
  lat=$(awk -v seed="$i" 'BEGIN { srand(seed); printf "%.7f", 37.42 + rand() * 0.28 }')
  lng=$(awk -v seed="$i" 'BEGIN { srand(seed + 1000); printf "%.7f", 126.76 + rand() * 0.42 }')

  response=$(curl -s -X POST "${BASE_URL}/stops" \
    -H "Content-Type: application/json" \
    -d "{\"stopCode\":\"${stop_code}\",\"stopName\":\"${stop_name}\",\"latitude\":${lat},\"longitude\":${lng}}")

  stop_id=$(echo "$response" | jq -r '.id')
  if [ "$stop_id" = "null" ] || [ -z "$stop_id" ]; then
    echo "정류장 생성 실패 (i=$i): $response"
    exit 1
  fi
  STOP_IDS+=("$stop_id")

  if (( i % 25 == 0 )); then
    echo "  정류장 ${i}/150 생성 완료"
  fi
done

echo "정류장 150개 생성 완료."

# ---------------------------------------------------------------------------
# 2. 노선(Route) 40개 생성 (노선마다 정류장 4~7개 포함)
# ---------------------------------------------------------------------------

ROUTE_TYPES=(CITY CITY CITY EXPRESS SHUTTLE)
NUM_ROUTE_TYPES=${#ROUTE_TYPES[@]}
NUM_STOPS=${#STOP_IDS[@]}

ROUTE_IDS=()

echo "--- 노선 40개 생성 중 ---"
for i in $(seq 1 40); do
  route_number=$(printf "%d" $((100 + i)))
  from_district="${DISTRICTS[$(( (i - 1) % NUM_DISTRICTS ))]}"
  to_district="${DISTRICTS[$(( (i + 4) % NUM_DISTRICTS ))]}"
  route_name="${from_district} - ${to_district}"
  route_type="${ROUTE_TYPES[$(( (i - 1) % NUM_ROUTE_TYPES ))]}"

  # 노선당 정류장 4~7개를 순서대로 선택 (정류장 풀을 순환하며 겹치지 않게 offset)
  stop_count=$(( 4 + (i % 4) ))
  start_offset=$(( ((i - 1) * 5) % NUM_STOPS ))

  stops_json="["
  for s in $(seq 0 $((stop_count - 1))); do
    idx=$(( (start_offset + s) % NUM_STOPS ))
    stop_id="${STOP_IDS[$idx]}"
    minutes=$(( 3 + s * 5 ))
    if [ "$s" -gt 0 ]; then
      stops_json="${stops_json},"
    fi
    stops_json="${stops_json}{\"stopId\":${stop_id},\"stopSequence\":$((s + 1)),\"estimatedMinutes\":${minutes}}"
  done
  stops_json="${stops_json}]"

  response=$(curl -s -X POST "${BASE_URL}/routes" \
    -H "Content-Type: application/json" \
    -d "{\"routeNumber\":\"${route_number}\",\"routeName\":\"${route_name}\",\"routeType\":\"${route_type}\",\"stops\":${stops_json}}")

  route_id=$(echo "$response" | jq -r '.id')
  if [ "$route_id" = "null" ] || [ -z "$route_id" ]; then
    echo "노선 생성 실패 (i=$i): $response"
    exit 1
  fi
  ROUTE_IDS+=("$route_id")
done

echo "노선 40개 생성 완료."

# ---------------------------------------------------------------------------
# 3. 차량(Vehicle) 87대 생성
# ---------------------------------------------------------------------------

MODEL_PAIRS=(
  "현대|스타렉스"
  "현대|카운티"
  "기아|카니발"
  "기아|봉고3"
  "기아|그랜버드"
  "대우버스|BS110"
  "볼보|B8L"
  "자일대우|NEW BS106"
)
NUM_MODELS=${#MODEL_PAIRS[@]}
PLATE_HANGUL=(가 나 다 라 마 바 사 아 자 차)
NUM_HANGUL=${#PLATE_HANGUL[@]}

VEHICLE_IDS=()

echo "--- 차량 87대 생성 중 ---"
for i in $(seq 1 87); do
  pair="${MODEL_PAIRS[$(( (i - 1) % NUM_MODELS ))]}"
  manufacturer="${pair%%|*}"
  model_name="${pair##*|}"

  region_num=$(( 10 + (i % 80) ))
  hangul="${PLATE_HANGUL[$(( (i - 1) % NUM_HANGUL ))]}"
  serial=$(printf "%04d" $((1000 + i)))
  plate_number="${region_num}${hangul}${serial}"

  model_year=$(( 2015 + (i % 11) ))

  response=$(curl -s -X POST "${BASE_URL}/vehicles" \
    -H "Content-Type: application/json" \
    -d "{\"plateNumber\":\"${plate_number}\",\"modelName\":\"${model_name}\",\"manufacturer\":\"${manufacturer}\",\"modelYear\":${model_year}}")

  vehicle_id=$(echo "$response" | jq -r '.id')
  if [ "$vehicle_id" = "null" ] || [ -z "$vehicle_id" ]; then
    echo "차량 생성 실패 (i=$i): $response"
    exit 1
  fi
  VEHICLE_IDS+=("$vehicle_id")

  if (( i % 20 == 0 )); then
    echo "  차량 ${i}/87 생성 완료"
  fi
done

echo "차량 87대 생성 완료."

# 대시보드 통계가 밋밋하지 않도록, 배차용으로 예약된 앞 3대를 제외한 나머지 차량 상태를 다양하게 조정
echo "--- 차량 상태 분포 조정 중 (배차용 3대 제외) ---"
for i in $(seq 4 87); do
  idx=$((i - 1))
  vehicle_id="${VEHICLE_IDS[$idx]}"

  status="INACTIVE"
  if (( i % 6 == 0 )); then
    status="ACTIVE"
  elif (( i % 9 == 0 )); then
    status="MAINTENANCE"
  fi

  if [ "$status" != "INACTIVE" ]; then
    curl -s -X PATCH "${BASE_URL}/vehicles/${vehicle_id}/status" \
      -H "Content-Type: application/json" \
      -d "{\"status\":\"${status}\"}" > /dev/null
  fi
done

# ---------------------------------------------------------------------------
# 4. 기사(Driver) 145명 생성
# ---------------------------------------------------------------------------

SURNAMES=(김 이 박 최 정 강 조 윤 장 임 한 오 서 신 권)
NUM_SURNAMES=${#SURNAMES[@]}
GIVEN_NAMES=(민준 서준 예준 도윤 시우 주원 하준 지호 준서 유준 지훈 성민 준혁 지원 우진 민서 서연 지우 서윤 하은 지민 수빈 채원 나윤 소율)
NUM_GIVEN=${#GIVEN_NAMES[@]}
LICENSE_TYPES=(1종보통 1종대형 2종보통)
NUM_LICENSE_TYPES=${#LICENSE_TYPES[@]}

DRIVER_IDS=()

echo "--- 기사 145명 생성 중 ---"
for i in $(seq 1 145); do
  surname="${SURNAMES[$(( (i - 1) % NUM_SURNAMES ))]}"
  given="${GIVEN_NAMES[$(( (i - 1) % NUM_GIVEN ))]}"
  name="${surname}${given}"

  if (( i % 2 == 0 )); then
    gender="FEMALE"
  else
    gender="MALE"
  fi

  age=$(( 24 + (i % 38) ))
  district="${DISTRICTS[$(( (i - 1) % NUM_DISTRICTS ))]}"
  address="서울시 ${district}구 ${i}동"

  phone_mid=$(printf "%04d" $((1000 + i)))
  phone_end=$(printf "%04d" $((2000 + i)))
  phone_number="010-${phone_mid}-${phone_end}"

  license_number=$(printf "11-24-%06d-%02d" "$i" $((i % 100)))
  license_type="${LICENSE_TYPES[$(( (i - 1) % NUM_LICENSE_TYPES ))]}"

  response=$(curl -s -X POST "${BASE_URL}/drivers" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"${name}\",\"gender\":\"${gender}\",\"age\":${age},\"address\":\"${address}\",\"phoneNumber\":\"${phone_number}\",\"licenseNumber\":\"${license_number}\",\"licenseType\":\"${license_type}\"}")

  driver_id=$(echo "$response" | jq -r '.id')
  if [ "$driver_id" = "null" ] || [ -z "$driver_id" ]; then
    echo "기사 생성 실패 (i=$i): $response"
    exit 1
  fi
  DRIVER_IDS+=("$driver_id")

  if (( i % 25 == 0 )); then
    echo "  기사 ${i}/145 생성 완료"
  fi
done

echo "기사 145명 생성 완료."

# 배차용으로 예약된 앞 3명을 제외한 나머지 기사 근무 상태를 다양하게 조정
echo "--- 기사 근무 상태 분포 조정 중 (배차용 3명 제외) ---"
for i in $(seq 4 145); do
  idx=$((i - 1))
  driver_id="${DRIVER_IDS[$idx]}"

  work_status="OFF_DUTY"
  if (( i % 4 == 0 )); then
    work_status="STANDBY"
  elif (( i % 10 == 0 )); then
    work_status="ON_DUTY"
  elif (( i % 15 == 0 )); then
    work_status="RETIRED"
  fi

  if [ "$work_status" != "OFF_DUTY" ]; then
    curl -s -X PATCH "${BASE_URL}/drivers/${driver_id}/status" \
      -H "Content-Type: application/json" \
      -d "{\"workStatus\":\"${work_status}\"}" > /dev/null
  fi
done

# ---------------------------------------------------------------------------
# 5. 배차(Dispatch) 3건 생성 (예정 / 진행중 / 완료 각 1건)
# ---------------------------------------------------------------------------

echo "--- 배차 3건 생성 중 (예정 1건 / 진행중 1건 / 완료 1건) ---"

ROUTE_1="${ROUTE_IDS[0]}"
ROUTE_2="${ROUTE_IDS[1]}"
ROUTE_3="${ROUTE_IDS[2]}"

VEHICLE_1="${VEHICLE_IDS[0]}"
VEHICLE_2="${VEHICLE_IDS[1]}"
VEHICLE_3="${VEHICLE_IDS[2]}"

DRIVER_1="${DRIVER_IDS[0]}"
DRIVER_2="${DRIVER_IDS[1]}"
DRIVER_3="${DRIVER_IDS[2]}"

# 배차를 만들기 전에는 기사가 STANDBY 상태여야 함 (차량은 기본값 INACTIVE라 그대로 사용)
curl -s -X PATCH "${BASE_URL}/drivers/${DRIVER_1}/status" -H "Content-Type: application/json" -d '{"workStatus":"STANDBY"}' > /dev/null
curl -s -X PATCH "${BASE_URL}/drivers/${DRIVER_2}/status" -H "Content-Type: application/json" -d '{"workStatus":"STANDBY"}' > /dev/null
curl -s -X PATCH "${BASE_URL}/drivers/${DRIVER_3}/status" -H "Content-Type: application/json" -d '{"workStatus":"STANDBY"}' > /dev/null

# 1) 예정(SCHEDULED) - 내일 오전 출발
start1=$(date -d "+1 day 09:00" +"%Y-%m-%dT%H:%M:%S")
end1=$(date -d "+1 day 10:30" +"%Y-%m-%dT%H:%M:%S")

resp1=$(curl -s -X POST "${BASE_URL}/dispatches" \
  -H "Content-Type: application/json" \
  -d "{\"routeId\":${ROUTE_1},\"vehicleId\":${VEHICLE_1},\"driverId\":${DRIVER_1},\"plannedStartTime\":\"${start1}\",\"plannedEndTime\":\"${end1}\"}")
dispatch1_id=$(echo "$resp1" | jq -r '.id')
echo "  배차 1 (예정) 생성: id=${dispatch1_id}"

# 2) 진행중(IN_PROGRESS) - 30분 전 출발, 1시간 후 도착 예정
start2=$(date -d "-30 minutes" +"%Y-%m-%dT%H:%M:%S")
end2=$(date -d "+60 minutes" +"%Y-%m-%dT%H:%M:%S")

resp2=$(curl -s -X POST "${BASE_URL}/dispatches" \
  -H "Content-Type: application/json" \
  -d "{\"routeId\":${ROUTE_2},\"vehicleId\":${VEHICLE_2},\"driverId\":${DRIVER_2},\"plannedStartTime\":\"${start2}\",\"plannedEndTime\":\"${end2}\"}")
dispatch2_id=$(echo "$resp2" | jq -r '.id')

curl -s -X PATCH "${BASE_URL}/dispatches/${dispatch2_id}/status" \
  -H "Content-Type: application/json" -d '{"dispatchStatus":"IN_PROGRESS"}' > /dev/null
echo "  배차 2 (진행중) 생성: id=${dispatch2_id}"

# 3) 완료(COMPLETED) - 어제 운행을 마친 것으로 처리
start3=$(date -d "-1 day 09:00" +"%Y-%m-%dT%H:%M:%S")
end3=$(date -d "-1 day 10:30" +"%Y-%m-%dT%H:%M:%S")

resp3=$(curl -s -X POST "${BASE_URL}/dispatches" \
  -H "Content-Type: application/json" \
  -d "{\"routeId\":${ROUTE_3},\"vehicleId\":${VEHICLE_3},\"driverId\":${DRIVER_3},\"plannedStartTime\":\"${start3}\",\"plannedEndTime\":\"${end3}\"}")
dispatch3_id=$(echo "$resp3" | jq -r '.id')

curl -s -X PATCH "${BASE_URL}/dispatches/${dispatch3_id}/status" \
  -H "Content-Type: application/json" -d '{"dispatchStatus":"IN_PROGRESS"}' > /dev/null
curl -s -X PATCH "${BASE_URL}/dispatches/${dispatch3_id}/status" \
  -H "Content-Type: application/json" -d '{"dispatchStatus":"COMPLETED"}' > /dev/null
echo "  배차 3 (완료) 생성: id=${dispatch3_id}"

echo ""
echo "=== 시딩 완료 ==="
echo "정류장: 150개 / 노선: 40개 / 차량: 87대 / 기사: 145명 / 배차: 3건"
echo "로그인 테스트 계정(demo/demo1234)은 그대로 유지되었습니다."
