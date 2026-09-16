import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 1,           // 1 solo utente virtuale
    duration: '10s',  // per 10 secondi
};

export default function () {
    const res = http.get('http://localhost:8000/api/products');

    check(res, {
        'status è 200': (r) => r.status === 200,
        'risposta non vuota': (r) => r.body.length > 0,
    });

    sleep(1); // pausa di 1 secondo tra una richiesta e l'altra
}