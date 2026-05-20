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
    const hero = document.getElementById("catalog-hero");
    const heroTitle = document.getElementById("hero-title");
    const heroMeta = document.getElementById("hero-meta");
    const heroOverview = document.getElementById("hero-overview");
    const heroBackdrop = document.getElementById("hero-backdrop");

    if (!container) return;

    container.innerHTML = "";

    // ==========================
    // HERO (DESTACADO)
    // ==========================
    if (peliculas.length > 0) {
        const featured = peliculas[0];

        if (hero) hero.style.display = "block";

        if (heroTitle) heroTitle.textContent = featured.titulo;
        if (heroOverview) heroOverview.textContent = featured.sinopsis || "Sin descripción";

        if (heroMeta) {
            heroMeta.innerHTML = `
                <span style="color:gold;">🎬 Película</span>
                <span>•</span>
                <span>${featured.genero || "General"}</span>
            `;
        }

        if (heroBackdrop && featured.portada) {
            heroBackdrop.style.backgroundImage = `url(${featured.portada})`;
        }

    }

    // ==========================
    // CARRUSEL STYLE NETFLIX
    // ==========================
    const row = document.createElement("div");
    row.className = "row-container";

    peliculas.forEach((item, index) => {

        const card = document.createElement("div");
        card.className = "card";
        card.tabIndex = 0;

        card.innerHTML = `
            <img src="${item.portada}" class="card-img">
            <div class="card-info">
                <h3>${item.titulo}</h3>
            </div>
        `;

        card.onclick = () => {
            alert("VER: " + item.titulo);
        };

        row.appendChild(card);
    });

    container.appendChild(row);
}

// ========================================
// START
// ========================================

window.addEventListener("load", () => {

    cargarPeliculas();

});
