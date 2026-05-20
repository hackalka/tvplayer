// ===============================
// PLAYER TV PRO - MAIN JS
// NETFLIX UI + TVBOX NAV
// ===============================


// ===============================
// ESTADO GLOBAL
// ===============================
let estado = {
    filtro: "inicio",
    foco: null,
    gridIndex: 0,
    items: []
};


// ===============================
// INIT
// ===============================
document.addEventListener("DOMContentLoaded", () => {
    initNav();
    initMandoTV();
    setTimeout(renderCatalogo, 500);
});


// ===============================
// RENDER PRINCIPAL
// ===============================
window.renderCatalogo = function () {
    const cont = document.getElementById("content");
    if (!cont || !window.BASE) return;

    cont.innerHTML = "";

    let data = [];

    if (estado.filtro === "inicio") {
        data = [
            ...window.BASE.peliculas,
            ...window.BASE.series,
            ...window.BASE.mundial
        ];
    } else {
        data = window.BASE[estado.filtro] || [];
    }

    estado.items = data;

    const grid = document.createElement("div");
    grid.className = "grid";

    data.forEach((item, i) => {
        const card = document.createElement("div");
        card.className = "card";
        card.tabIndex = 0;
        card.dataset.index = i;

        card.innerHTML = `
            <img src="${item.portada || item.logo1 || ''}">
            <div class="title">${item.titulo || ''}</div>
        `;

        card.onclick = () => abrirItem(item);

        grid.appendChild(card);
    });

    cont.appendChild(grid);

    setTimeout(() => setFocus(0), 100);
};
const seguir = JSON.parse(localStorage.getItem("seguirViendo")) || [];

if (seguir.length) {
    seguir.forEach(item => {
        const card = document.createElement("div");
        card.className = "card";
        card.tabIndex = 0;

        card.innerHTML = `
            <img src="${item.portada}">
            <div class="title">${item.titulo}</div>
        `;

        card.onclick = () => {
            if (typeof reproducir === "function") {
                reproducir(item.link, [item], 0);
            }
        };

        cont.appendChild(card);
    });
}

// ===============================
// NAVEGACIÓN MENÚ SUPERIOR
// ===============================
function initNav() {
    document.querySelectorAll(".f-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            const filtro = btn.dataset.filtro;
            estado.filtro = filtro;

            document.querySelectorAll(".f-btn").forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            renderCatalogo();
        });
    });
}


// ===============================
// ABRIR ITEM
// ===============================
function abrirItem(item) {
    const modal = document.getElementById("modal");
    if (!modal) return;

    document.getElementById("det-titulo").innerText = item.titulo || "";
    document.getElementById("det-sinopsis").innerText = item.sinopsis || "";

    const box = document.getElementById("linksBox");
    box.innerHTML = "";

    ["link", "link1", "stream"].forEach(k => {
        if (item[k]) {
            const div = document.createElement("div");
            div.className = "link-item";
            div.tabIndex = 0;
            div.innerText = "VER";

            div.onclick = () => window.open(item[k], "_blank");

            box.appendChild(div);
        }
    });

    modal.classList.add("active");
    setFocus(0);
}


// ===============================
// FOCO INTELIGENTE
// ===============================
function setFocus(index) {
    const cards = document.querySelectorAll(".card");

    if (!cards.length) return;

    if (index < 0) index = 0;
    if (index >= cards.length) index = cards.length - 1;

    estado.gridIndex = index;

    cards[index].focus();
    estado.foco = cards[index];

    cards[index].scrollIntoView({
        behavior: "smooth",
        block: "center",
        inline: "center"
    });
}


// ===============================
// MANDO TVBOX (PRO LEVEL)
// ===============================
function initMandoTV() {

    document.addEventListener("keydown", (e) => {

        const cards = document.querySelectorAll(".card");
        const links = document.querySelectorAll(".link-item");

        let elementos = [...cards, ...links];

        let index = elementos.indexOf(document.activeElement);

        // ENTER
        if (e.key === "Enter") {
            if (document.activeElement) {
                document.activeElement.click();
            }
            return;
        }

        // ESC
        if (e.key === "Escape") {
            const modal = document.getElementById("modal");
            if (modal && modal.classList.contains("active")) {
                modal.classList.remove("active");
            }
            return;
        }

        // SI NO HAY FOCO
        if (index === -1) {
            setFocus(0);
            return;
        }

        // DERECHA
        if (e.key === "ArrowRight") {
            index++;
        }

        // IZQUIERDA
        if (e.key === "ArrowLeft") {
            index--;
        }

        // ABAJO
        if (e.key === "ArrowDown") {
            index += 5; // salto tipo grid Netflix
        }

        // ARRIBA
        if (e.key === "ArrowUp") {
            index -= 5;
        }

        if (elementos[index]) {
            elementos[index].focus();
            elementos[index].scrollIntoView({
                behavior: "smooth",
                block: "center"
            });
        }

        e.preventDefault();
    });
}
