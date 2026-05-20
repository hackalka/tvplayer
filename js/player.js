// ===============================
// PLAYER TV PRO - PLAYER INTERNO
// ===============================

let video = null;
let hls = null;

let playlist = [];
let currentIndex = 0;


// ===============================
// INIT PLAYER
// ===============================
function initPlayer() {
    video = document.getElementById("main-video");

    if (!video) return;

    video.addEventListener("ended", () => {
        nextEpisode();
    });
}


// ===============================
// REPRODUCIR
// ===============================
function reproducir(url, lista = [], index = 0) {

    const layer = document.getElementById("player-layer");
    const info = document.getElementById("video-info");

    playlist = lista || [];
    currentIndex = index;

    layer.style.display = "flex";

    info.innerText = lista[index]?.titulo || "Reproduciendo...";

    cargarVideo(url);

    initControles();
}


// ===============================
// CARGAR VIDEO
// ===============================
function cargarVideo(url) {

    if (!video) initPlayer();

    if (!video) return;

    // limpiar HLS anterior
    if (hls) {
        hls.destroy();
        hls = null;
    }

    video.pause();
    video.src = "";

    guardarProgreso();

    // ================= HLS =================
    if (url && url.includes(".m3u8")) {

        if (window.Hls && Hls.isSupported()) {

            hls = new Hls();
            hls.loadSource(url);
            hls.attachMedia(video);

            hls.on(Hls.Events.MANIFEST_PARSED, () => {
                video.play().catch(() => {});
            });

        } else if (video.canPlayType("application/vnd.apple.mpegurl")) {

            video.src = url;
            video.play().catch(() => {});
        }

    }

    // ================= MP4 =================
    else if (url) {

        video.src = url;
        video.play().catch(() => {});
    }
}


// ===============================
// SIGUIENTE EPISODIO
// ===============================
function nextEpisode() {

    if (!playlist.length) return;

    if (currentIndex + 1 < playlist.length) {

        currentIndex++;

        const next = playlist[currentIndex];

        if (!next) return;

        cargarVideo(next.link);

        const info = document.getElementById("video-info");
        if (info) info.innerText = next.titulo;
    }
}


// ===============================
// GUARDAR PROGRESO (FOLLOW)
// ===============================
function guardarProgreso() {

    if (!video || !playlist.length) return;

    const item = playlist[currentIndex];

    if (!item) return;

    let data = JSON.parse(localStorage.getItem("seguirViendo")) || [];

    data = data.filter(i => i.titulo !== item.titulo);

    data.unshift({
        titulo: item.titulo,
        link: item.link,
        portada: item.portada || "",
        progreso: video.currentTime || 0
    });

    localStorage.setItem("seguirViendo", JSON.stringify(data));
}


// ===============================
// CONTROLES TVBOX
// ===============================
function initControles() {

    document.onkeydown = (e) => {

        const layer = document.getElementById("player-layer");

        if (!layer || layer.style.display !== "flex") return;

        // ESC
        if (e.key === "Escape") {
            cerrarPlayer();
        }

        // PLAY / PAUSE
        if (e.key === "Enter") {
            if (video.paused) video.play();
            else video.pause();
        }

        // NEXT EPISODE
        if (e.key === "ArrowRight") {
            nextEpisode();
        }

        // BACK 10s
        if (e.key === "ArrowLeft") {
            video.currentTime -= 10;
        }

        // PAUSE
        if (e.key === "ArrowDown") {
            video.pause();
        }

        // PLAY
        if (e.key === "ArrowUp") {
            video.play();
        }

        e.preventDefault();
    };
}


// ===============================
// CERRAR PLAYER
// ===============================
function cerrarPlayer() {

    const layer = document.getElementById("player-layer");

    if (hls) {
        hls.destroy();
        hls = null;
    }

    if (video) {
        video.pause();
        video.src = "";
    }

    if (layer) {
        layer.style.display = "none";
    }
}
