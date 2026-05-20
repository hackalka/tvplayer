// ===============================
// FIREBASE INIT
// ===============================

const _db = "aHR0cHM6Ly9wbGF5ZXJ0di05NDQ5Yy1kZWZhdWx0LXJ0ZGIuZXVyb3BlLXdlc3QxLmZpcmViYXNlZGF0YWJhc2UuYXBwLw==";

firebase.initializeApp({
    databaseURL: atob(_db)
});

const db = firebase.database();
const auth = firebase.auth();

let editId = null;

// ===============================
// LOGIN
// ===============================
function login() {

    const email = document.getElementById("email").value;
    const pass = document.getElementById("pass").value;

    auth.signInWithEmailAndPassword(email, pass)
    .then(() => {
        document.getElementById("login").style.display = "none";
        document.getElementById("panel").style.display = "block";
        cargar();
    })
    .catch(err => alert("Error login"));
}

// ===============================
// LOGOUT
// ===============================
function logout() {
    auth.signOut();
    location.reload();
}

// ===============================
// CREAR / EDITAR
// ===============================
function crear() {

    const data = {
        titulo: titulo.value,
        sinopsis: sinopsis.value,
        portada: portada.value,
        link: link.value
    };

    if (editId) {
        db.ref("peliculas/" + editId).update(data);
        editId = null;
    } else {
        db.ref("peliculas").push(data);
    }

    limpiar();
    cargar();
}

// ===============================
// CARGAR
// ===============================
function cargar() {

    const cont = document.getElementById("listado");
    cont.innerHTML = "";

    db.ref("peliculas").on("value", snap => {

        const data = snap.val() || {};

        Object.keys(data).forEach(id => {

            const item = data[id];

            const div = document.createElement("div");
            div.className = "card";

            div.innerHTML = `
                <strong>${item.titulo}</strong><br>
                <span class="small">${item.sinopsis}</span><br><br>
                <button onclick="editar('${id}', '${item.titulo}', '${item.sinopsis}', '${item.portada}', '${item.link}')">Editar</button>
                <button onclick="borrar('${id}')">Borrar</button>
            `;

            cont.appendChild(div);
        });
    });
}

// ===============================
// EDITAR
// ===============================
function editar(id, t, s, p, l) {

    editId = id;

    titulo.value = t;
    sinopsis.value = s;
    portada.value = p;
    link.value = l;
}

// ===============================
// BORRAR
// ===============================
function borrar(id) {
    db.ref("peliculas/" + id).remove();
}

// ===============================
// LIMPIAR
// ===============================
function limpiar() {
    titulo.value = "";
    sinopsis.value = "";
    portada.value = "";
    link.value = "";
}
