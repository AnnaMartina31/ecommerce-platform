import http from 'k6/http';
import { check } from 'k6';

export const options = {
    stages: [
        { duration: '15s', target: 30 },
        { duration: '20s', target: 30 },
        { duration: '15s', target: 60 },
        { duration: '20s', target: 60 },
        { duration: '15s', target: 100 },
        { duration: '20s', target: 100 },
        { duration: '15s', target: 150 },
        { duration: '20s', target: 150 },
        { duration: '15s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<3000'],
        http_req_failed: ['rate<0.1'],
    },
};

export default function () {
    const payload = JSON.stringify({
        userId: 1,
        items: [
            { productId: 1, quantity: 2 }
        ]
    });

    const params = {
        headers: { 'Content-Type': 'application/json' },
    };

    const res = http.post('http://localhost:8000/api/orders', payload, params);

    check(res, {
        'status è 201': (r) => r.status === 201,
    });

    // niente sleep: carico continuo, senza pause
}