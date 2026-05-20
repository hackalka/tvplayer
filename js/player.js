// ===============================
// PLAYER TV PRO - PLAYER INTERNO
// HLS + MP4 + TVBOX CONTROL
// ===============================


let video = null;
let hls = null;
let playlist = [];
let currentIndex = 0;


// ===============================
// INICIAR PLAYER
// ===============================
function initPlayer() {
    video = document.getElementById("main-video");
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

    cargarVideo(url);

    initControles();
}


// ===============================
// CARGAR VIDEO
// ===============================
function cargarVideo(url) {

    if (!video) initPlayer();

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
            hls.on(Hls.Events.MANIFEST_PARSED, () => video.play());
        }

        else if (video.canPlayType("application/vnd.apple.mpegurl")) {
            video.src = url;
            video.play();
        }

    }

    // ================= MP4 =================
    else {
        video.src = url;
        video.play();
    }
}


// ===============================
// SIGUIENTE CAPÍTULO
// ===============================
function nextEpisode() {

    if (currentIndex + 1 < playlist.length) {
        currentIndex++;
        const next = playlist[currentIndex];
        cargarVideo(next.link);
        document.getElementById("video-info").innerText = next.titulo;
    }
}


// ===============================
// CONTROLES TVBOX
// ===============================
function initControles() {

    document.onkeydown = (e) => {

        const layer = document.getElementById("player-layer");

        if (layer.style.display !== "flex") return;

        // ESC
        if (e.key === "Escape") {
            cerrarPlayer();
        }

        // ENTER = play/pause
        if (e.key === "Enter") {
            if (video.paused) video.play();
            else video.pause();
        }

        // NEXT CHAPTER
        if (e.key === "ArrowRight") {
            nextEpisode();
        }

        // BACKWARD
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

    video.pause();
    video.src = "";

    layer.style.display = "none";
}
