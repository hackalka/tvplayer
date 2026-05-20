// ===============================
// PLAYER TV PRO - HERO NETFLIX
// ===============================

let heroIndex = 0;
let heroItems = [];

// INICIAR HERO
function initHero() {
    renderHero();
    setInterval(cambiarHero, 6000);
}

// OBTENER ITEMS DESTACADOS
function getHeroItems() {
    if (!window.BASE) return [];

    return [
        ...(window.BASE.peliculas || []),
        ...(window.BASE.series || []),
        ...(window.BASE.mundial || [])
    ].slice(0, 10);
}

// RENDER HERO
function renderHero() {
    const hero = document.getElementById("hero-container");
    if (!hero) return;

    heroItems = getHeroItems();

    if (!heroItems.length) return;

    const item = heroItems[heroIndex];

    hero.innerHTML = `
        <div class="hero">
            <div class="hero-bg" style="background-image:url('${item.portada || item.logo1 || ''}')"></div>
            
            <div class="hero-overlay"></div>

            <div class="hero-content">
                <h1 class="hero-title">${item.titulo || ''}</h1>
                <p class="hero-desc">${item.sinopsis || 'Contenido disponible en Player TV'}</p>

                <div class="hero-buttons">
                    <button class="hero-btn" onclick="abrirHero(item)">▶ VER AHORA</button>
                </div>
            </div>
        </div>
    `;
}

// CAMBIAR HERO AUTOMÁTICO
function cambiarHero() {
    if (!heroItems.length) return;

    heroIndex++;
    if (heroIndex >= heroItems.length) heroIndex = 0;

    renderHero();
}

// ABRIR DESDE HERO
function abrirHero() {
    const item = heroItems[heroIndex];
    if (!item) return;

    if (typeof abrirItem === "function") {
        abrirItem(item);
    }
}

// INICIAR CUANDO BASE LISTA
document.addEventListener("DOMContentLoaded", () => {
    setTimeout(initHero, 1500);
});
