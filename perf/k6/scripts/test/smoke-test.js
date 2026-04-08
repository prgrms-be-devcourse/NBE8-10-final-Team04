import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://host.docker.internal:8080";
const HEALTH_PATH = __ENV.HEALTH_PATH || "/actuator/health";

export const options = {
  vus: 1,
  duration: "2m",
  thresholds: {
    http_req_failed: ["rate==0"],
    http_req_duration: ["p(95)<1000"],
  },
};

export default function () {
  const res = http.get(`${BASE_URL}${HEALTH_PATH}`, {
    tags: { name: "smoke-health-check" },
    timeout: "5s",
  });

  check(res, {
    "health endpoint status is 200": (r) => r.status === 200,
    "response has status UP": (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.status === "UP";
      } catch (e) {
        return false;
      }
    },
  });

  sleep(1);
}
