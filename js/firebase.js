// ===============================
// PLAYER TV - FIREBASE INIT
// ===============================

// 🔐 Base64 de tu Firebase (la que ya usabas)
const _db = "aHR0cHM6Ly9wbGF5ZXJ0di05NDQ5Yy1kZWZhdWx0LXJ0ZGIuZXVyb3BlLXdlc3QxLmZpcmViYXNlZGF0YWJhc2UuYXBwLw==";

// Inicializar Firebase
firebase.initializeApp({
    databaseURL: atob(_db)
});

const db = firebase.database();


// ===============================
// BASE GLOBAL DEL CATÁLOGO
// ===============================
window.BASE = {
    peliculas: [],
    series: [],
    mundial: [],
    agenda: [],
    favoritos: []
};


// ===============================
// CARGAR DATOS EN TIEMPO REAL
// ===============================
function cargarFirebase() {

    const rutas = ["peliculas", "series", "mundial", "agenda"];

    rutas.forEach(cat => {
        db.ref(cat).on("value", snap => {
            const data = snap.val() || {};

            // convertir objeto Firebase a array limpio
            const lista = Object.keys(data).map(key => ({
                id: key,
                ...data[key],
                categoria: cat
            }));

            window.BASE[cat] = lista;

            console.log(`✔ ${cat} cargado:`, lista.length);

            // avisar a main.js
            if (window.renderCatalogo) {
                window.renderCatalogo();
            }
        });
    });
}


// ===============================
// INICIALIZAR
// ===============================
document.addEventListener("DOMContentLoaded", () => {
    cargarFirebase();
});
