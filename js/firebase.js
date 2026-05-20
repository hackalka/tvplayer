// ===============================
// PLAYER TV - FIREBASE INIT (PRO FIX)
// ===============================

// 🔐 Base64 Firebase
const _db = "aHR0cHM6Ly9wbGF5ZXJ0di05NDQ5Yy1kZWZhdWx0LXJ0ZGIuZXVyb3BlLXdlc3QxLmZpcmViYXNlZGF0YWJhc2UuYXBwLw==";

// Evitar doble inicialización (IMPORTANTE)
if (!firebase.apps.length) {
    firebase.initializeApp({
        databaseURL: atob(_db)
    });
}

const db = firebase.database();


// ===============================
// BASE GLOBAL
// ===============================
window.BASE = {
    peliculas: [],
    series: [],
    mundial: [],
    agenda: [],
    favoritos: []
};


// ===============================
// CARGA FIREBASE
// ===============================
function cargarFirebase() {

    const rutas = ["peliculas", "series", "mundial", "agenda"];

    rutas.forEach(cat => {
        db.ref(cat).on("value", snap => {

            const data = snap.val() || {};

            const lista = Object.keys(data).map(key => ({
                id: key,
                ...data[key],
                categoria: cat
            }));

            window.BASE[cat] = lista;

            console.log(`✔ ${cat} cargado: ${lista.length}`);

            // sincronización segura con main.js
            if (typeof window.renderCatalogo === "function") {
                window.renderCatalogo();
            }
        });
    });
}


// ===============================
// INIT
// ===============================
document.addEventListener("DOMContentLoaded", () => {
    cargarFirebase();
});
