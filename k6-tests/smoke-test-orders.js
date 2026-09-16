import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 1,
    duration: '10s',
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
        'risposta contiene id ordine': (r) => JSON.parse(r.body).id !== undefined,
    });

    sleep(1);
}