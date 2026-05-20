// ===============================
// PLAYER TV PRO - MAIN JS FIXED
// ===============================


// ===============================
// ESTADO GLOBAL
// ===============================
let estado = {
    filtro: "inicio",
    foco: null,
    gridIndex: 0,
    items: [],
    columnas: 5
};


// ===============================
// INIT
// ===============================
document.addEventListener("DOMContentLoaded", () => {
    initNav();
    initMandoTV();

    setTimeout(() => {
        renderCatalogo();
        setTimeout(() => {
            focusNav();
        }, 300);
    }, 600);
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

    setTimeout(() => setFocus(0), 200);
};


// ===============================
// NAVEGACIÓN MENÚ SUPERIOR
// ===============================
function initNav() {
    document.querySelectorAll(".f-btn").forEach(btn => {
        btn.addEventListener("click", () => {

            estado.filtro = btn.dataset.filtro;

            document.querySelectorAll(".f-btn")
                .forEach(b => b.classList.remove("active"));

            btn.classList.add("active");

            renderCatalogo();

            setTimeout(() => focusNav(), 100);
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

    setTimeout(() => {
        const first = modal.querySelector(".link-item");
        if (first) first.focus();
    }, 200);
}


// ===============================
// FOCO GRID
// ===============================
function setFocus(index) {
    const cards = document.querySelectorAll(".card");
    if (!cards.length) return;

    if (index < 0) index = 0;
    if (index >= cards.length) index = cards.length - 1;

    estado.gridIndex = index;
    estado.foco = cards[index];

    cards[index].focus();
    cards[index].scrollIntoView({
        behavior: "smooth",
        block: "center"
    });
}


// ===============================
// FOCO NAV (IMPORTANTE PRO TV)
// ===============================
function focusNav() {
    const first = document.querySelector(".f-btn.active") ||
                  document.querySelector(".f-btn");

    if (first) first.focus();
}


// ===============================
// MANDO TVBOX PRO
// ===============================
function initMandoTV() {

    document.addEventListener("keydown", (e) => {

        const cards = document.querySelectorAll(".card");
        const links = document.querySelectorAll(".link-item");
        const navBtns = document.querySelectorAll(".f-btn");

        const isModal = document.getElementById("modal")?.classList.contains("active");

        // ENTER
        if (e.key === "Enter") {
            document.activeElement?.click();
            return;
        }

        // ESC
        if (e.key === "Escape") {
            const modal = document.getElementById("modal");

            if (modal && modal.classList.contains("active")) {
                modal.classList.remove("active");
                focusNav();
            }
            return;
        }

        // SI MODAL ACTIVO → SOLO LINKS
        if (isModal) {
            let index = Array.from(links).indexOf(document.activeElement);

            if (e.key === "ArrowDown") index++;
            if (e.key === "ArrowUp") index--;

            if (links[index]) links[index].focus();

            e.preventDefault();
            return;
        }

        // GRID NORMAL
        let elementos = [...cards];

        let index = elementos.indexOf(document.activeElement);

        if (index === -1) {
            setFocus(0);
            return;
        }

        const cols = estado.columnas;

        if (e.key === "ArrowRight") index++;
        if (e.key === "ArrowLeft") index--;

        if (e.key === "ArrowDown") index += cols;
        if (e.key === "ArrowUp") index -= cols;

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
