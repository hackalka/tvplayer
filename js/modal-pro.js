// ===============================
// PLAYER TV PRO - MODAL NETFLIX
// ===============================

let modalState = {
    serieActual: null,
    temporadas: {},
    temporadaActiva: null
};


// ===============================
// ABRIR ITEM
// ===============================
function abrirItem(item) {

    const modal = document.getElementById("modal");
    if (!modal) return;

    modal.classList.add("active");

    document.getElementById("det-titulo").innerText = item.titulo || "";
    document.getElementById("det-sinopsis").innerText =
        item.sinopsis || "Sin descripción disponible";

    const linksBox = document.getElementById("linksBox");
    const tabs = document.getElementById("tabsTemporadas");

    linksBox.innerHTML = "";
    tabs.innerHTML = "";


    // ===============================
    // PELÍCULA (simple)
    // ===============================
    if (!isSerie(item.titulo)) {

        crearLinks([item], linksBox);
        return;
    }


    // ===============================
    // SERIE (Netflix mode)
    // ===============================
    const raiz = getRoot(item.titulo);

    const episodios = (window.BASE.series || [])
        .filter(i => getRoot(i.titulo) === raiz);

    construirTemporadas(episodios, tabs, linksBox);
}


// ===============================
// CHECK SERIE
// ===============================
function isSerie(titulo) {
    return /S\d+/i.test(titulo || "");
}


// ===============================
// CONSTRUIR TEMPORADAS
// ===============================
function construirTemporadas(lista, tabs, box) {

    modalState.temporadas = {};
    modalState.serieActual = lista;

    // agrupar temporadas
    lista.forEach(ep => {

        let match = ep.titulo.match(/S(\d+)/i);
        let temp = match ? match[1] : "1";

        if (!modalState.temporadas[temp]) {
            modalState.temporadas[temp] = [];
        }

        modalState.temporadas[temp].push(ep);
    });

    const keys = Object.keys(modalState.temporadas)
        .sort((a, b) => parseInt(a) - parseInt(b));

    modalState.temporadaActiva = keys[0];

    // tabs temporadas
    keys.forEach((t, i) => {

        const btn = document.createElement("button");
        btn.className = "tab-btn";
        btn.tabIndex = 0;
        btn.innerText = "T" + t;

        if (i === 0) btn.classList.add("active");

        btn.onclick = () => {

            document.querySelectorAll(".tab-btn")
                .forEach(b => b.classList.remove("active"));

            btn.classList.add("active");

            mostrarCapitulos(modalState.temporadas[t], box);
        };

        tabs.appendChild(btn);
    });

    mostrarCapitulos(modalState.temporadas[keys[0]], box);
}


// ===============================
// MOSTRAR CAPÍTULOS
// ===============================
function mostrarCapitulos(episodios, box) {

    box.innerHTML = "";

    episodios.sort((a, b) => {

        const na = parseInt((a.titulo.match(/E(\d+)/i) || [0, 0])[1]);
        const nb = parseInt((b.titulo.match(/E(\d+)/i) || [0, 0])[1]);

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

                const lista = episodios.map(e => ({
                    titulo: e.titulo,
                    link: e.link,
                    portada: e.portada || ""
                }));

                const index = episodios.indexOf(ep);

                reproducir(ep.link, lista, index);

            } else {
                window.open(ep.link, "_blank");
            }
        };

        box.appendChild(div);
    });
}


// ===============================
// LINKS SIMPLE (PELÍCULAS)
// ===============================
function crearLinks(lista, box) {

    lista.forEach(item => {

        if (item.link) {

            const div = document.createElement("div");
            div.className = "link-item";
            div.tabIndex = 0;

            div.innerHTML = `
                <span>▶ REPRODUCIR</span>
                <small>PLAY</small>
            `;

            div.onclick = () => {

                if (typeof reproducir === "function") {
                    reproducir(item.link, [item], 0);
                } else {
                    window.open(item.link, "_blank");
                }
            };

            box.appendChild(div);
        }
    });
}


// ===============================
// ROOT CLEANER
// ===============================
function getRoot(t) {

    return (t || "")
        .toUpperCase()
        .replace(/\sS\d+.*$/g, "")
        .trim();
}
