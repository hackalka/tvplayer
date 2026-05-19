// ══════════════════════════════════════════
//   1. CONFIGURACIÓN Y VARIABLES
// ══════════════════════════════════════════
const _db = "aHR0cHM6Ly9wbGF5ZXJ0di05NDQ5Yy1kZWZhdWx0LXJ0ZGIuZXVyb3BlLXdlc3QxLmZpcmViYXNlZGF0YWJhc2UuYXBwLw==";
firebase.initializeApp({ databaseURL: atob(_db) });
const db = firebase.database();

// Se cambia 'directos' por 'mundial' en el objeto base original
let base = { peliculas: [], series: [], mundial: [], agenda: [], destacados: null };
let filtroActual = 'inicio';
let subFiltroActual = 'TODOS';
let textoBusqueda = '';

const IMG_CAMPO = "https://blog.marti.mx/wp-content/uploads/2023/06/campo_futbol_Header-770x449.webp";
let hls = null;

// ══════════════════════════════════════════
//   2. CARGA DE DATOS
// ══════════════════════════════════════════
function cargar() {
    db.ref('destacado_manual').on('value', snap => { 
        base.destacados = snap.val(); 
        render(filtroActual); 
    });

    // Se incluye 'mundial' en el array de ramas a escuchar
    ['peliculas', 'series', 'mundial', 'agenda'].forEach(cat => {
        db.ref(cat).on('value', snap => {
            const data = snap.val() || {};
            let list = Object.keys(data).map(k => ({ ...data[k], firebaseKey: k, catAsignada: cat }));
            
            if (cat === 'peliculas') list.reverse(); 
            if (cat === 'agenda') list.sort((a, b) => obtenerValorCronologico(a.extra) - obtenerValorCronologico(b.extra));
            if (cat === 'mundial') list.sort((a, b) => (a.timestamp || 0) - (b.timestamp || 0));
            
            base[cat] = list;
            render(filtroActual);
        });
    });
}

// ══════════════════════════════════════════
//   3. FAVORITOS Y SEGUIR VIENDO
// ══════════════════════════════════════════
function gestionarFavorito(item, event) {
    if (event) event.stopPropagation();
    let favs = JSON.parse(localStorage.getItem('favoritos')) || [];
    const r = getRoot(item.titulo);
    const index = favs.findIndex(i => getRoot(i.titulo) === r);

    if (index > -1) favs.splice(index, 1);
    else favs.push(item);
    
    localStorage.setItem('favoritos', JSON.stringify(favs));
    actualizarEstrellasCards();
    
    const btnModal = document.getElementById('btn-fav-modal');
    if (btnModal) {
        const esFav = favs.some(i => getRoot(i.titulo) === r);
        btnModal.style.color = esFav ? 'gold' : '#ccc';
        btnModal.innerHTML = `<i class="fa ${esFav ? 'fa-star' : 'fa-star-o'}"></i>`;
    }
    render(filtroActual);
}

function actualizarEstrellasCards() {
    const favs = JSON.parse(localStorage.getItem('favoritos')) || [];
    document.querySelectorAll('.card').forEach(card => {
        const tituloRaiz = card.getAttribute('data-root');
        if (tituloRaiz) {
            const esFav = favs.some(f => getRoot(f.titulo) === tituloRaiz);
            const starSpan = card.querySelector('.fav-star');
            if (starSpan) {
                starSpan.innerHTML = `<i class="fa ${esFav ? 'fa-star' : 'fa-star-o'}"></i>`;
                starSpan.style.color = esFav ? 'gold' : '#ccc';
            }
        }
    });
}

function guardarSeguirViendo(item) {
    let lista = JSON.parse(localStorage.getItem('seguirViendo')) || [];
    lista = lista.filter(i => getRoot(i.titulo) !== getRoot(item.titulo));
    lista.unshift(item);
    if (lista.length > 20) lista.pop();
    localStorage.setItem('seguirViendo', JSON.stringify(lista));
}

function eliminarElemento(e, tituloRaiz, claveStorage) {
    e.stopPropagation();
    let lista = JSON.parse(localStorage.getItem(claveStorage)) || [];
    lista = lista.filter(i => getRoot(i.titulo) !== tituloRaiz);
    localStorage.setItem(claveStorage, JSON.stringify(lista));
    render(filtroActual);
}

// ══════════════════════════════════════════
//   4. RENDERIZADO PRINCIPAL
// ══════════════════════════════════════════
function render(modo) {
    filtroActual = modo;
    const container = document.getElementById('content');
    const subNav = document.getElementById('sub-nav');
    if (!container) return;

    container.innerHTML = '';
    
    if (subNav) {
        subNav.innerHTML = '';
        if (modo === 'peliculas' || modo === 'series') {
            subNav.style.display = 'flex';
            generarSubCategorias(modo);
        } else { subNav.style.display = 'none'; }
    }

    renderHero();

    if (modo === 'inicio') {
        const favs = JSON.parse(localStorage.getItem('favoritos')) || [];
        if (favs.length > 0) container.appendChild(crearSeccion("MIS FAVORITOS", favs, 'favoritos'));

        const seguir = JSON.parse(localStorage.getItem('seguirViendo')) || [];
        if (seguir.length > 0) container.appendChild(crearSeccion("SEGUIR VIENDO", seguir, 'seguirViendo'));

        // Se cambia 'directos' por 'mundial' para renderizar la fila de cards en la pantalla inicial
        ['agenda', 'peliculas', 'series', 'mundial'].forEach(cat => {
            if (base[cat] && base[cat].length > 0) {
                let titulo = cat === 'agenda' ? 'DEPORTES EN VIVO' : (cat === 'mundial' ? '🏆 MUNDIAL 2026' : cat.toUpperCase());
                container.appendChild(crearSeccion(titulo, base[cat].slice(0, 20), null));
            }
        });
    } 
    else if (modo === 'favoritos') {
        const favs = JSON.parse(localStorage.getItem('favoritos')) || [];
        const seriesUnicas = unificarSeries(favs);
        const grid = document.createElement('div');
        grid.className = 'grid';
        seriesUnicas.forEach(item => grid.appendChild(crearCard(item, 'favoritos')));
        container.appendChild(grid);
    }
    else {
        const grid = document.createElement('div');
        grid.className = 'grid';
        
        let data = base[modo] || [];
        if (subFiltroActual !== 'TODOS') data = data.filter(i => i.genero && i.genero.toUpperCase() === subFiltroActual);
        if (textoBusqueda !== '') data = data.filter(i => getRoot(i.titulo).includes(textoBusqueda));
        
        let itemsAMostrar = (modo === 'series') ? unificarSeries(data) : data;
        itemsAMostrar.forEach(item => grid.appendChild(crearCard(item)));
        container.appendChild(grid);
    }
}

function unificarSeries(listaEpisodios) {
    const mapSeries = new Map();
    listaEpisodios.forEach(item => {
        const raiz = getRoot(item.titulo);
        if (!mapSeries.has(raiz)) {
            const episodiosMismaRaiz = listaEpisodios.filter(i => getRoot(i.titulo) === raiz);
            const mejorPortada = episodiosMismaRaiz.find(ep => ep.portada && ep.portada.trim() !== '')?.portada || item.portada || '';
            const representante = { ...item, portada: mejorPortada };
            mapSeries.set(raiz, representante);
        }
    });
    return Array.from(mapSeries.values());
}

// ══════════════════════════════════════════
//   5. CREACIÓN DE CARDS
// ══════════════════════════════════════════
function crearSeccion(titulo, items, tipoStorage) {
    const sec = document.createElement('div');
    sec.style.marginBottom = "25px";
    sec.innerHTML = `<h2 class="section-title">${titulo}</h2>`;
    const row = document.createElement('div');
    row.className = 'row-container';
    
    let itemsUnicos = (titulo.toUpperCase() === 'SERIES') ? unificarSeries(items) : items;
    const visto = new Set();
    itemsUnicos.forEach(item => {
        const r = getRoot(item.titulo);
        if (!visto.has(r)) { visto.add(r); row.appendChild(crearCard(item, tipoStorage)); }
    });
    sec.appendChild(row);
    return sec;
}

function crearCard(item, tipoStorage = null) {
    const r = getRoot(item.titulo);
    const card = document.createElement('div');
    card.className = 'card';
    card.setAttribute('tabindex', '0');
    card.setAttribute('role', 'button');
    card.setAttribute('data-root', r);
    card.onclick = (e) => {
        if (e.target.closest('.fav-star')) return;
        guardarSeguirViendo(item);
        abrirModal(r, item.catAsignada, item);
    };

    const btnBorrar = tipoStorage ? `<div tabindex="0" role="button" class="btn-eliminar" onclick="eliminarElemento(event, '${r}', '${tipoStorage}')" style="position:absolute; top:5px; right:5px; background:rgba(255,0,0,0.8); color:white; width:22px; height:22px; border-radius:50%; display:flex; align-items:center; justify-content:center; z-index:10; font-size:14px;">&times;</div>` : '';
    
    const favs = JSON.parse(localStorage.getItem('favoritos')) || [];
    const esFav = favs.some(f => getRoot(f.titulo) === r);
    const btnFav = `<div tabindex="0" role="button" class="fav-star" onclick="gestionarFavorito(${JSON.stringify(item).replace(/"/g, '&quot;')}, event)" style="position:absolute; top:5px; left:5px; background:rgba(0,0,0,0.6); color:${esFav ? 'gold' : '#ccc'}; width:26px; height:26px; border-radius:50%; display:flex; align-items:center; justify-content:center; z-index:10; font-size:14px; backdrop-filter:blur(2px);">
            <i class="fa ${esFav ? 'fa-star' : 'fa-star-o'}"></i>
        </div>`;

    let imgHTML = '';
    if (item.logo1 && item.logo2) {
        imgHTML = `<div class="img-container">
            <div class="fondo-agenda" style="height: 230px !important;">
                <img src="${item.logo1}" class="escudo-mini"><span class="vs-text">VS</span><img src="${item.logo2}" class="escudo-mini">
            </div>
        </div>`;
    } else {
        imgHTML = `<div class="img-container">
            <img src="${item.portada || item.logo1 || 'https://via.placeholder.com/160x230?text=Sin+Imagen'}" class="portada">
        </div>`;
    }

    let momentoDeportivo = '';
    if (item.extra) {
        momentoDeportivo = item.extra;
    } else if (item.dia || item.hora || item.fecha) {
        momentoDeportivo = `${item.dia || item.fecha || ''} ${item.hora || ''}`.trim();
    }

    let infoExtra = '';
    if (momentoDeportivo) {
        infoExtra = `<div class="info-agenda">${momentoDeportivo}</div>`;
    }

    card.innerHTML = `
        ${btnBorrar}
        ${btnFav}
        ${imgHTML}
        <div class="info">
            <div class="info-titulo">${r}</div>
            ${infoExtra}
        </div>`;
    return card;
}

// ══════════════════════════════════════════
//   6. MODAL (con temporadas y capítulos)
// ══════════════════════════════════════════
function abrirModal(nombreRaiz, catKey, itemFallback) {
    const modal = document.getElementById('modal');
    modal.classList.add('active');
    modal.focus();
    
    let lista = [];
    // Se añade 'mundial' para mapear y recuperar los enlaces correctamente en la ventana flotante
    ['agenda', 'peliculas', 'series', 'mundial'].forEach(c => {
        const matches = base[c].filter(i => getRoot(i.titulo) === nombreRaiz);
        if (matches.length > 0) lista = [...lista, ...matches];
    });
    if (lista.length === 0) {
        if (!itemFallback) { cerrarModal(); return; }
        lista = [itemFallback];
    }

    const principal = lista[0];
    const favs = JSON.parse(localStorage.getItem('favoritos')) || [];
    const esFav = favs.some(i => getRoot(i.titulo) === nombreRaiz);

    document.getElementById('det-titulo').innerHTML = `${nombreRaiz} <span id="btn-fav-modal" tabindex="0" role="button" style="margin-left:15px; cursor:pointer; color:${esFav?'gold':'#ccc'}; font-size:24px;"><i class="fa ${esFav?'fa-star':'fa-star-o'}"></i></span>`;
    const favBtn = document.getElementById('btn-fav-modal');
    favBtn.onclick = () => gestionarFavorito(principal);
    favBtn.onkeydown = (e) => { if(e.key === 'Enter') gestionarFavorito(principal); };
    
    document.getElementById('det-sinopsis').textContent = principal.sinopsis || "Sin descripción.";

    const header = document.getElementById('modalHeader');
    if (principal.logo1 && principal.logo2) {
        header.innerHTML = `<div style="background-image:url('${IMG_CAMPO}'); background-size:cover; height:200px; display:flex; align-items:center; justify-content:center; gap:20px;">
            <img src="${principal.logo1}" style="height:90px;"><b style="font-size:25px;">VS</b><img src="${principal.logo2}" style="height:90px;">
        </div>`;
    } else {
        header.innerHTML = `<div style="background-image: linear-gradient(transparent, #000), url('${principal.portada || principal.logo1}'); height:280px; background-size:cover; background-position:center;"></div>`;
    }

    const tabs = document.getElementById('tabsTemporadas');
    tabs.innerHTML = '';
    
    if (catKey === 'series') {
        const temps = {};
        lista.forEach(i => {
            let tempNum = "01";
            let match = i.titulo.match(/S(\d+)(?:\s*E?\d*)?/i);
            if (!match) match = i.titulo.match(/T(\d+)/i);
            if (!match) match = i.titulo.match(/TEMPORADA\s*(\d+)/i);
            if (match) tempNum = match[1].padStart(2,'0');
            if (!temps[tempNum]) temps[tempNum] = [];
            temps[tempNum].push(i);
        });
        const tempsOrdenadas = Object.keys(temps).sort((a,b) => parseInt(a)-parseInt(b));
        tempsOrdenadas.forEach((s, idx) => {
            const b = document.createElement('button');
            b.className = `tab-temp ${idx === 0 ? 'active' : ''}`;
            b.textContent = `TEMP ${parseInt(s)}`;
            b.setAttribute('tabindex', '0');
            b.onclick = () => {
                document.querySelectorAll('.tab-temp').forEach(btn => btn.classList.remove('active'));
                b.classList.add('active');
                mostrarCaps(temps[s], true);
            };
            tabs.appendChild(b);
        });
        if (tempsOrdenadas.length > 0) mostrarCaps(temps[tempsOrdenadas[0]], true);
    } else {
        mostrarCaps(lista, false);
    }

    document.getElementById('closeModalBtn').focus();
}

function mostrarCaps(items, esSerie) {
    const box = document.getElementById('linksBox');
    box.innerHTML = '';
    
    if (esSerie) {
        items.sort((a, b) => {
            let numA = 0, numB = 0;
            const matchA = a.titulo.match(/E(\d+)/i) || a.titulo.match(/CAP[ÍI]TULO\s*(\d+)/i);
            const matchB = b.titulo.match(/E(\d+)/i) || b.titulo.match(/CAP[ÍI]TULO\s*(\d+)/i);
            if (matchA) numA = parseInt(matchA[1]);
            if (matchB) numB = parseInt(matchB[1]);
            return numA - numB;
        });
    }

    items.forEach(item => {
        let label = item.titulo;
        if (esSerie) {
            const eMatch = item.titulo.match(/E(\d+)/i) || item.titulo.match(/CAP[ÍI]TULO\s*(\d+)/i);
            label = eMatch ? `CAPÍTULO ${parseInt(eMatch[1])}` : item.titulo;
        }

        const links = [
            { u: item.link, n: 'LINK 1' },
            { u: item.link1, n: 'LINK 2' },
            { u: item.acestream, n: 'ACESTREAM' },
            { u: item.id, n: 'ID ACESTREAM' }
        ];

        links.forEach(l => {
            if (l.u && l.u.length > 5) {
                const row = document.createElement('div');
                row.className = 'link-item';
                row.setAttribute('tabindex', '0');
                row.setAttribute('role', 'button');
                const esAce = l.u.includes('acestream://') || l.u.length === 40;
                row.innerHTML = `<span><i class="fa ${esAce ? 'fa-bolt' : 'fa-play'}" style="color:gold; margin-right:12px;"></i>${label}</span>
                                <i class="fa fa-chevron-right"></i>`;
                row.onclick = () => {
                    let url = l.u;
                    if (l.u.length === 40 && !l.u.includes('://')) url = 'acestream://' + l.u;
                    abrirReproductor(url, label);
                };
                row.onkeydown = (e) => { if(e.key === 'Enter') row.click(); };
                box.appendChild(row);
            }
        });
    });
}

// ══════════════════════════════════════════
//   7. REPRODUCTOR CON HLS.JS
// ══════════════════════════════════════════
function abrirReproductor(url, titulo) {
    const playerLayer = document.getElementById('player-layer');
    const videoInfo = document.getElementById('video-info');
    const video = document.getElementById('main-video');
    
    videoInfo.textContent = titulo;
    
    if (url.startsWith('acestream://') || url.includes('acestream') || url.length === 40) {
        alert("AceStream no se puede reproducir en el navegador. Se abrirá enlace externo.");
        window.location.href = url;
        return;
    }
    
    if (hls) { hls.destroy(); hls = null; }
    video.pause();
    video.src = '';
    
    if (url.includes('.m3u8') && Hls.isSupported()) {
        hls = new Hls();
        hls.loadSource(url);
        hls.attachMedia(video);
        hls.on(Hls.Events.MANIFEST_PARSED, () => video.play().catch(e => console.log(e)));
        hls.on(Hls.Events.ERROR, () => alert("Error al cargar el stream."));
    } else if (url.includes('.m3u8') && video.canPlayType('application/vnd.apple.mpegurl')) {
        video.src = url;
        video.play().catch(e => console.log(e));
    } else {
        video.src = url;
        video.play().catch(e => console.log(e));
    }
    playerLayer.style.display = 'flex';
}

// ══════════════════════════════════════════
//   8. HERO, SUBNAV Y UTILIDADES
// ══════════════════════════════════════════
function cerrarReproductor() {
    const playerLayer = document.getElementById('player-layer');
    const video = document.getElementById('main-video');
    if (hls) { hls.destroy(); hls = null; }
    video.pause();
    video.src = '';
    playerLayer.style.display = 'none';
}

function renderHero() {
    const hero = document.getElementById('hero-container');
    if (!hero || filtroActual !== 'inicio' || !base.destacados) { if(hero) hero.style.display='none'; return; }
    hero.style.display = 'block';
    const item = base.destacados;
    const r = getRoot(item.titulo);
    
    if (item.logo1 && item.logo2) {
        hero.innerHTML = `<div class="hero-content" style="background-image: linear-gradient(rgba(0,0,0,0.5), #000), url('${IMG_CAMPO}');">
            <div class="hero-details">
                <div class="hero-vs-box">
                    <img src="${item.logo1}"><span>VS</span><img src="${item.logo2}">
                </div>
                <h2>${r}</h2>
                <button class="btn-play-hero" onclick="abrirModal('${r}','${item.catAsignada}',null)"><i class="fa fa-play"></i> VER AHORA</button>
            </div>
        </div>`;
    } else {
        hero.innerHTML = `<div class="hero-content" style="background-image: linear-gradient(transparent, #000), url('${item.portada || item.logo1}');">
            <div class="hero-details">
                <h2>${r}</h2>
                <button class="btn-play-hero" onclick="abrirModal('${r}','${item.catAsignada}',null)"><i class="fa fa-play"></i> VER AHORA</button>
            </div>
        </div>`;
    }
}

function generarSubCategorias(modo) {
    const subNav = document.getElementById('sub-nav');
    const gens = ['TODOS', 'ACCION', 'DRAMA', 'TERROR', 'COMEDIA', 'ANIMACION', 'FANTASIA'];
    gens.forEach(g => {
        const b = document.createElement('button');
        b.className = 'sub-btn';
        b.setAttribute('tabindex', '0');
        if (subFiltroActual === g) b.classList.add('active');
        b.textContent = g;
        b.onclick = () => { subFiltroActual = g; render(modo); };
        subNav.appendChild(b);
    });
}

function cambiarFiltro(m, btn) {
    document.querySelectorAll('.f-btn').forEach(b => b.classList.remove('active'));
    if (btn) btn.classList.add('active');
    subFiltroActual = 'TODOS';
    textoBusqueda = '';
    const searchInput = document.getElementById('mainSearch');
    if (searchInput) searchInput.value = '';
    render(m);
}

function cerrarModal() { 
    document.getElementById('modal').classList.remove('active');
    document.getElementById('content').focus();
}

function getRoot(t) { 
    if (!t) return "";
    let raiz = t.toUpperCase();
    raiz = raiz.replace(/\s*(S\d+(?:\s*E\d+)?|TEMPORADA\s*\d+|T\d+|CAP[ÍI]TULO\s*\d+|C\d+|\d+[Xx]\d+)\s*/gi, '');
    raiz = raiz.replace(/\s+\d+$/, '');
    return raiz.trim();
}

function obtenerValorCronologico(str) {
    if (!str) return 999999;
    if (!isNaN(str) && str.toString().length >= 10) return parseInt(str);
    const nums = str.match(/\d+/g);
    return nums ? parseInt(nums[0])*100 + parseInt(nums[1]) : 999999;
}

function ejecutarBuscador() {
    const input = document.getElementById('mainSearch');
    textoBusqueda = input.value.trim().toUpperCase();
    render(filtroActual);
}

// ══════════════════════════════════════════
//   9. NAVEGACIÓN PRO TVBOX / 
// ══════════════════════════════════════════

let ultimoFocus = null;

// ---------- GUARDAR FOCO ----------
document.addEventListener('focusin', (e) => {
    ultimoFocus = e.target;
});

// ---------- OBTENER ELEMENTOS ----------
function getFocusableElements() {

    const modal = document.getElementById('modal');
    const player = document.getElementById('player-layer');

    // PLAYER
    if (player.style.display === 'flex') {
        return Array.from(
            player.querySelectorAll('button,[tabindex="0"]')
        );
    }

    // MODAL
    if (modal.classList.contains('active')) {
        return Array.from(
            modal.querySelectorAll(
                '.close-btn,.tab-temp,.link-item,#btn-fav-modal,[tabindex="0"]'
            )
        );
    }

    // NORMAL
    return Array.from(document.querySelectorAll(`
        .f-btn,
        .sub-btn,
        .card,
        .btn-play-hero,
        .fav-star,
        #mainSearch
    `));
}

// ---------- BUSCAR MÁS CERCANO ----------
function findClosest(current, direction, elements) {

    const currentRect = current.getBoundingClientRect();

    let best = null;
    let bestScore = Infinity;

    elements.forEach(el => {

        if (el === current) return;

        const rect = el.getBoundingClientRect();

        let valid = false;
        let score = Infinity;

        switch(direction){

            case 'left':
                valid = rect.left < currentRect.left;
                score = Math.abs(currentRect.left - rect.left)
                      + Math.abs(currentRect.top - rect.top);
            break;

            case 'right':
                valid = rect.left > currentRect.left;
                score = Math.abs(rect.left - currentRect.left)
                      + Math.abs(currentRect.top - rect.top);
            break;

            case 'up':
                valid = rect.top < currentRect.top;
                score = Math.abs(currentRect.top - rect.top)
                      + Math.abs(currentRect.left - rect.left);
            break;

            case 'down':
                valid = rect.top > currentRect.top;
                score = Math.abs(rect.top - currentRect.top)
                      + Math.abs(currentRect.left - rect.left);
            break;
        }

        if (valid && score < bestScore) {
            bestScore = score;
            best = el;
        }

    });

    return best;
}

// ---------- FOCO ----------
function setFocus(el){

    if(!el) return;

    el.focus({
        preventScroll:true
    });

    el.scrollIntoView({
        behavior:'smooth',
        block:'center',
        inline:'center'
    });
}

// ---------- KEYDOWN ----------
document.addEventListener('keydown', (e) => {

    const modal = document.getElementById('modal');
    const player = document.getElementById('player-layer');

    const active = document.activeElement;

    // ---------- BACK / ESC ----------
    if (
        e.key === 'Escape' ||
        e.key === 'Backspace' ||
        e.keyCode === 461 ||
        e.keyCode === 10009
    ){

        if(player.style.display === 'flex'){
            cerrarReproductor();
            e.preventDefault();
            return;
        }

        if(modal.classList.contains('active')){
            cerrarModal();

            setTimeout(()=>{
                if(ultimoFocus) setFocus(ultimoFocus);
            },100);

            e.preventDefault();
            return;
        }
    }

    // ---------- ENTER ----------
    if (
        e.key === 'Enter' ||
        e.keyCode === 13 ||
        e.keyCode === 23
    ){

        if(active){
            active.click();
        }

        e.preventDefault();
        return;
    }

    // ---------- FLECHAS ----------
    let direction = null;

    if(e.key === 'ArrowLeft') direction = 'left';
    if(e.key === 'ArrowRight') direction = 'right';
    if(e.key === 'ArrowUp') direction = 'up';
    if(e.key === 'ArrowDown') direction = 'down';

    if(!direction) return;

    const elements = getFocusableElements();

    if(elements.length === 0) return;

    if(!active || !elements.includes(active)){
        setFocus(elements[0]);
        return;
    }

    const next = findClosest(active, direction, elements);

    if(next){
        setFocus(next);
    }

    e.preventDefault();
});

// ---------- FOCO AUTOMÁTICO ----------
function activarPrimerFocus(){

    setTimeout(()=>{

        const first =
            document.querySelector('.f-btn.active') ||
            document.querySelector('.f-btn');

        if(first){
            setFocus(first);
        }

    },700);
}

// ---------- OBSERVER PARA NUEVAS CARDS ----------
const observerTV = new MutationObserver(() => {

    document.querySelectorAll('.card').forEach(card => {

        if(!card.hasAttribute('tabindex')){
            card.setAttribute('tabindex','0');
        }

    });

});

observerTV.observe(document.body,{
    childList:true,
    subtree:true
});

// ---------- INICIO ----------
window.addEventListener('load', () => {

    activarPrimerFocus();

});
// ══════════════════════════════════════════
// 10. INICIALIZACIÓN
// ══════════════════════════════════════════
window.onload = () => {
    cargar();
    const searchInput = document.getElementById('mainSearch');
    if (searchInput) searchInput.addEventListener('input', ejecutarBuscador);
    
    document.querySelectorAll('.f-btn').forEach(btn => {
        btn.setAttribute('tabindex', '0');
        const filtro = btn.getAttribute('data-filtro');
        if (filtro) btn.addEventListener('click', () => cambiarFiltro(filtro, btn));
    });
    
    document.getElementById('closeModalBtn').addEventListener('click', cerrarModal);
    document.getElementById('closePlayerBtn').addEventListener('click', cerrarReproductor);
  const tg = window.Telegram.WebApp;

tg.ready();
tg.expand();

const user = tg.initDataUnsafe?.user;
const userId = user?.id;
};
