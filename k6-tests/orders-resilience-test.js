import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const created = new Counter('orders_created_201');
const unavailable = new Counter('orders_unavailable_503');
const otherErrors = new Counter('orders_other_errors');
const failDuration = new Trend('duration_of_503_ms', true);

export const options = {
    vus: 10,
    duration: '90s',
};

export default function () {
    const payload = JSON.stringify({
        userId: 1,
        items: [{ productId: 1, quantity: 1 }],
    });

    const res = http.post('http://localhost:8000/api/orders', payload, {
        headers: { 'Content-Type': 'application/json' },
        timeout: '10s',
    });

    if (res.status === 201) {
        created.add(1);
    } else if (res.status === 503) {
        unavailable.add(1);
        failDuration.add(res.timings.duration);
    } else {
        otherErrors.add(1);
    }

    check(res, {
        'risposta 201 oppure 503 (mai 500 o timeout)': (r) => r.status === 201 || r.status === 503,
    });

    sleep(0.5);
}