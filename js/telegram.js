// ===============================
// PLAYER TV - TELEGRAM MINI APP
// ===============================

let tg = null;
let user = null;

// INIT TELEGRAM
function initTelegram() {

    if (!window.Telegram || !window.Telegram.WebApp) {
        console.log("❌ No Telegram WebApp (modo navegador)");
        return;
    }

    tg = window.Telegram.WebApp;

    tg.ready();
    tg.expand();

    user = tg.initDataUnsafe?.user;

    console.log("✅ USER TELEGRAM:", user);

    if (user) {
        guardarUsuario(user);
        mostrarUsuario(user);
    }
}


// ===============================
// GUARDAR USUARIO (FIREBASE)
// ===============================
function guardarUsuario(user) {

    if (!window.db) return;

    const id = user.id;

    db.ref("users/" + id).update({
        id: user.id,
        username: user.username || "",
        first_name: user.first_name || "",
        last_name: user.last_name || "",
        last_login: Date.now()
    });
}


// ===============================
// UI USER
// ===============================
function mostrarUsuario(user) {

    const header = document.querySelector("header");

    if (!header) return;

    const div = document.createElement("div");

    div.style.color = "#22c55e";
    div.style.fontSize = "12px";
    div.innerText = "👤 " + (user.username || user.first_name);

    header.appendChild(div);
}


// INIT
document.addEventListener("DOMContentLoaded", () => {
    initTelegram();
});
