// ===============================
// PLAYER TV PRO - FIREBASE INIT
// ===============================

// 🔐 Firebase base64
const _db = "aHR0cHM6Ly9wbGF5ZXJ0di05NDQ5Yy1kZWZhdWx0LXJ0ZGIuZXVyb3BlLXdlc3QxLmZpcmViYXNlZGF0YWJhc2UuYXBwLw==";

// ===============================
// INIT FIREBASE
// ===============================
firebase.initializeApp({
    databaseURL: atob(_db)
});

const db = firebase.database();


// ===============================
// BASE GLOBAL
// ===============================
window.BASE = {
    peliculas: [],
    series: [],
    mundial: [],
    agenda: [],
    favoritos: [],
    destacados: null
};


// ===============================
// HELPERS
// ===============================
function ordenarPorUltimo(data, cat) {

    return Object.keys(data || {})
        .map(key => ({
            id: key,
            ...data[key],
            categoria: cat,
            _key: key
        }))
        .sort((a, b) => b._key.localeCompare(a._key));
}


// ===============================
// CARGAR DATOS EN TIEMPO REAL
// ===============================
function cargarFirebase() {

    const rutas = ["peliculas", "series", "mundial", "agenda"];

    rutas.forEach(cat => {

        db.ref(cat).on("value", snap => {

            const data = snap.val() || {};

            const lista = ordenarPorUltimo(data, cat);

            window.BASE[cat] = lista;

            console.log(`✔ ${cat} cargado:`, lista.length);

            if (typeof window.renderCatalogo === "function") {
                window.renderCatalogo();
            }

            if (typeof window.renderHero === "function") {
                window.renderHero();
            }
        });
    });


    // ===============================
    // DESTACADO MANUAL (HERO)
    // ===============================
    db.ref("destacado_manual").on("value", snap => {

        window.BASE.destacados = snap.val() || null;

        if (typeof window.renderHero === "function") {
            window.renderHero();
        }
    });
}


// ===============================
// INIT
// ===============================
document.addEventListener("DOMContentLoaded", () => {
    cargarFirebase();
});
