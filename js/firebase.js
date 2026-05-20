// ========================================
// PLAYER_TV FIREBASE CONNECT
// ========================================

// FIREBASE CONFIG
const firebaseConfig = {
    databaseURL: "https://player-tv-default-rtdb.europe-west1.firebasedatabase.app/"
};

// INIT
firebase.initializeApp(firebaseConfig);

// DATABASE
const db = firebase.database();

// ========================================
// VARIABLES GLOBALES
// ========================================

let peliculas = [];
let series = [];
let deportes = [];

// ========================================
// CARGAR PELICULAS
// ========================================

function cargarPeliculas() {

    db.ref("peliculas").on("value", (snapshot) => {

        const data = snapshot.val();

        peliculas = [];

        if (data) {

            Object.keys(data).forEach(key => {

                peliculas.push({
                    id: key,
                    ...data[key]
                });

            });

        }

        console.log("PELÍCULAS:", peliculas);

        renderPeliculas();

    });

}

// ========================================
// RENDER PELICULAS
// ========================================

function renderPeliculas() {

    const container = document.getElementById("catalog-grid");

    if (!container) return;

    container.innerHTML = "";

    peliculas.forEach(item => {

        const card = document.createElement("div");

        card.className = "card";

        card.setAttribute("tabindex", "0");

        card.innerHTML = `
            <img src="${item.portada}" class="card-img">

            <div class="card-info">
                <h3>${item.titulo}</h3>
            </div>
        `;

        card.onclick = () => {
            alert(item.titulo);
        };

        container.appendChild(card);

    });

}

// ========================================
// START
// ========================================

window.addEventListener("load", () => {

    cargarPeliculas();

});
