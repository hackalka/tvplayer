// ===============================
// PLAYER TV PRO - PLAYER FIXED
// ===============================

let video = null;
let hls = null;
let playlist = [];
let currentIndex = 0;
let controlesActivos = false;


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
// ABRIR VIDEO
// ===============================
function reproducir(url, lista = [], index = 0) {

    const layer = document.getElementById("player-layer");
    const info = document.getElementById("video-info");

    playlist = lista;
    currentIndex = index;

    layer.style.display = "flex";

    info.innerText = lista[index]?.titulo || "Reproduciendo...";

    if (!video) initPlayer();

    cargarVideo(url);

    if (!controlesActivos) {
        initControles();
        controlesActivos = true;
    }
}


// ===============================
// CARGAR VIDEO
// ===============================
function cargarVideo(url) {

    if (!video) initPlayer();

    // limpiar HLS
    if (hls) {
        hls.destroy();
        hls = null;
    }

    video.pause();
    video.src = "";

    // ================= HLS =================
    if (url.includes(".m3u8")) {

        if (window.Hls && Hls.isSupported()) {
            hls = new Hls();
            hls.loadSource(url);
            hls.attachMedia(video);
            hls.on(Hls.Events.MANIFEST_PARSED, () => {
                video.play().catch(()=>{});
            });
        }

        else if (video.canPlayType("application/vnd.apple.mpegurl")) {
            video.src = url;
            video.play().catch(()=>{});
        }

    }

    // ================= MP4 =================
    else {
        video.src = url;
        video.play().catch(()=>{});
    }

    guardarProgreso();
}


// ===============================
// SIGUIENTE CAPÍTULO
// ===============================
function nextEpisode() {

    if (currentIndex + 1 < playlist.length) {
        currentIndex++;

        const next = playlist[currentIndex];

        if (!next) return;

        document.getElementById("video-info").innerText = next.titulo;

        cargarVideo(next.link);
    }
}


// ===============================
// GUARDAR PROGRESO
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
// CONTROLES TVBOX PRO
// ===============================
function initControles() {

    document.addEventListener("keydown", (e) => {

        const layer = document.getElementById("player-layer");

        if (!layer || layer.style.display !== "flex") return;

        switch (e.key) {

            case "Escape":
                cerrarPlayer();
                break;

            case "Enter":
                if (video.paused) video.play();
                else video.pause();
                break;

            case "ArrowRight":
                nextEpisode();
                break;

            case "ArrowLeft":
                video.currentTime = Math.max(0, video.currentTime - 10);
                break;

            case "ArrowDown":
                video.pause();
                break;

            case "ArrowUp":
                video.play();
                break;
        }

        e.preventDefault();
    });
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

    layer.style.display = "none";

    controlesActivos = false;
}
