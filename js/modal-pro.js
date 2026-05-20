// ===============================
// PLAYER TV PRO - MODAL NETFLIX FIXED
// ===============================

let modalState = {
    serieActual: null,
    temporadas: {},
    temporadaActiva: null
};


// ===============================
// ABRIR MODAL
// ===============================
function abrirItem(item) {

    const modal = document.getElementById("modal");
    if (!modal) return;

    modal.classList.add("active");

    const titulo = document.getElementById("det-titulo");
    const sinopsis = document.getElementById("det-sinopsis");
    const linksBox = document.getElementById("linksBox");
    const tabs = document.getElementById("tabsTemporadas");

    titulo.innerText = item.titulo || "";
    sinopsis.innerText = item.sinopsis || "Sin descripción disponible";

    linksBox.innerHTML = "";
    tabs.innerHTML = "";


    // ===============================
    // DETECCIÓN SIMPLE PERO SEGURA
    // ===============================
    const esSerie = (item.titulo && item.titulo.match(/S\d+/i));


    if (!esSerie) {
        crearLinks([item], linksBox);
        return;
    }


    // ===============================
    // SERIE MODE
    // ===============================
    const raiz = getRoot(item.titulo);

    const episodios = (window.BASE.series || [])
        .filter(i => getRoot(i.titulo) === raiz);

    construirTemporadas(episodios, tabs, linksBox);
}


// ===============================
// TEMPORADAS
// ===============================
function construirTemporadas(lista, tabs, box) {

    modalState.temporadas = {};
    modalState.serieActual = lista;

    lista.forEach(ep => {

        let match = ep.titulo.match(/S(\d+)/i);
        let temp = match ? match[1] : "1";

        if (!modalState.temporadas[temp]) {
            modalState.temporadas[temp] = [];
        }

        modalState.temporadas[temp].push(ep);
    });

    const keys = Object.keys(modalState.temporadas).sort();

    modalState.temporadaActiva = keys[0];

    keys.forEach((t, i) => {

        const btn = document.createElement("button");
        btn.className = "tab-btn";
        btn.tabIndex = 0;
        btn.innerText = "T" + t;

        btn.onclick = () => {
            document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            mostrarCapitulos(modalState.temporadas[t], box);
        };

        if (i === 0) btn.classList.add("active");

        tabs.appendChild(btn);
    });

    mostrarCapitulos(modalState.temporadas[keys[0]], box);
}


// ===============================
// CAPÍTULOS
// ===============================
function mostrarCapitulos(episodios, box) {

    box.innerHTML = "";

    episodios.sort((a, b) => {
        const na = parseInt((a.titulo.match(/E(\d+)/i) || [0,0])[1]);
        const nb = parseInt((b.titulo.match(/E(\d+)/i) || [0,0])[1]);
        return na - nb;
    });

    episodios.forEach(ep => {

        const div = document.createElement("div");
        div.className = "episode-item";
        div.tabIndex = 0;

        div.innerHTML = `
            <span>${ep.titulo}</span>
            <small>▶</small>
        `;

        div.onclick = () => {
            if (typeof reproducir === "function") {
                reproducir(ep.link, episodios, episodios.indexOf(ep));
            } else {
                window.open(ep.link, "_blank");
            }
        };

        box.appendChild(div);
    });
}


// ===============================
// PELÍCULAS
// ===============================
function crearLinks(lista, box) {

    lista.forEach(item => {

        if (!item.link) return;

        const div = document.createElement("div");
        div.className = "link-item";
        div.tabIndex = 0;

        div.innerHTML = `
            <span>▶ REPRODUCIR</span>
            <small>TV</small>
        `;

        div.onclick = () => {
            if (typeof reproducir === "function") {
                reproducir(item.link, [item], 0);
            } else {
                window.open(item.link, "_blank");
            }
        };

        box.appendChild(div);
    });
}


// ===============================
// ROOT
// ===============================
function getRoot(t) {
    return (t || "")
        .toUpperCase()
        .replace(/\sS\d+.*$/g, "")
        .trim();
}
