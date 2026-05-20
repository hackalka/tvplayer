// ===============================
// PLAYER TV PRO - FIREBASE INIT
// ===============================

// 🔐 Firebase base64 (tu URL actual)
const _db = "aHR0cHM6Ly9wbGF5ZXJ0di05NDQ5Yy1kZWZhdWx0LXJ0ZGIuZXVyb3BlLXdlc3QxLmZpcmViYXNlZGF0YWJhc2UuYXBwLw==";

// ===============================
// INIT FIREBASE
// ===============================
firebase.initializeApp({
    databaseURL: atob(_db)
});

const db = firebase.database();


// ===============================
// BASE GLOBAL (CATÁLOGO)
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
// CARGAR DATOS EN TIEMPO REAL
// ===============================
function cargarFirebase() {

    const rutas = ["peliculas", "series", "mundial", "agenda"];

    rutas.forEach(cat => {
        db.ref(cat).on("value", snap => {

            const data = snap.val() || {};

            const lista = Object.keys(data).map(key => ({
    id: key,
    ...data[key],
    categoria: cat,
    _key: key
}))
.sort((a, b) => {
    return b._key.localeCompare(a._key);
});

            window.BASE[cat] = lista;

            console.log(`✔ ${cat} cargado:`, lista.length);

            // refrescar UI si existe
            if (window.renderCatalogo) {
                window.renderCatalogo();
            }
        });
    });

    // ===============================
    // DESTACADO HERO
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
