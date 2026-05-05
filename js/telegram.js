// ================= CONFIGURACIÓN =================
// 🔴 REEMPLAZA ESTOS VALORES CON LOS TUYOS (de my.telegram.org)
const API_ID = 8952741;            // <--- TU API ID (número)
const API_HASH = "693fb2da124662dad85b2b337c53a386";   // <--- TU API HASH (cadena)
// =================================================

const CONFIG = {
    apiId: API_ID,
    apiHash: API_HASH,
    databaseDirectory: "tdlib_data",
    useDatabase: true,
    useChatInfoDatabase: true,
    useMessageDatabase: true,
    deviceModel: "Web Client",
    systemVersion: "1.0",
    applicationVersion: "1.0.0"
};

let client = null;
let currentAuthState = null;
let currentChatId = null;
let chatsMap = new Map();

// Elementos DOM
const loginZone = document.getElementById('loginZone');
const mainApp = document.getElementById('mainApp');
const phoneInput = document.getElementById('phoneInput');
const codeContainer = document.getElementById('codeContainer');
const codeInput = document.getElementById('codeInput');
const authButton = document.getElementById('authButton');
const connectionStatus = document.getElementById('connectionStatus');
const chatsListContainer = document.getElementById('chatsListContainer');
const messagesList = document.getElementById('messagesList');
const currentChatTitleSpan = document.querySelector('#currentChatTitle span');
const inputMessageContainer = document.getElementById('inputMessageContainer');
const messageInput = document.getElementById('messageInput');
const sendMessageBtn = document.getElementById('sendMessageBtn');

function updateStatus(msg, isError = false) {
    connectionStatus.innerHTML = msg;
    if (isError) console.error(msg);
}

// Esperar a que TdClient esté disponible (por si tarda)
function waitForTdClient(callback) {
    if (typeof TdClient !== 'undefined') {
        callback();
    } else {
        updateStatus("Esperando librería tdweb...");
        setTimeout(() => waitForTdClient(callback), 500);
    }
}

function initTdlib() {
    try {
        client = new TdClient({
            onUpdate: handleUpdate,
            onError: (err) => { console.error(err); updateStatus("Error en TDLib: " + JSON.stringify(err), true); },
            ...CONFIG
        });
        updateStatus("Conectando...");
    } catch (err) {
        updateStatus("Fallo al crear cliente: " + err.message, true);
        client = null;
    }
}

function handleUpdate(update) {
    console.log("Update:", update['@type']);
    switch(update['@type']) {
        case 'updateAuthorizationState':
            handleAuthState(update.authorization_state);
            break;
        case 'updateNewMessage':
            if (currentChatId === update.message.chat_id) {
                appendMessageToUI(update.message);
            }
            renderChatsList(); // para actualizar último mensaje
            break;
        case 'updateChatLastMessage':
        case 'updateChatTitle':
            renderChatsList();
            break;
    }
}

function handleAuthState(authState) {
    currentAuthState = authState['@type'];
    updateStatus(`Estado: ${currentAuthState}`);
    if (currentAuthState === 'authorizationStateWaitPhoneNumber') {
        loginZone.classList.remove('hide');
        mainApp.classList.add('hide');
        codeContainer.style.display = 'none';
        authButton.innerText = 'Enviar número';
        // Limpiar inputs
        phoneInput.value = '';
        codeInput.value = '';
    } 
    else if (currentAuthState === 'authorizationStateWaitCode') {
        loginZone.classList.remove('hide');
        mainApp.classList.add('hide');
        codeContainer.style.display = 'block';
        authButton.innerText = 'Verificar código';
    } 
    else if (currentAuthState === 'authorizationStateReady') {
        loginZone.classList.add('hide');
        mainApp.classList.remove('hide');
        updateStatus('Conectado');
        renderChatsList();
    }
}

async function renderChatsList() {
    if (!client) return;
    chatsListContainer.innerHTML = '<div class="loading-placeholder">Cargando chats...</div>';
    try {
        const result = await client.send({
            '@type': 'getChats',
            offset_order: '9223372036854775807',
            offset_chat_id: 0,
            limit: 100
        });
        if (result && result['@type'] === 'chats') {
            const chatIds = result.chat_ids;
            if (!chatIds.length) {
                chatsListContainer.innerHTML = '<div class="loading-placeholder">No hay chats aún</div>';
                return;
            }
            let html = '';
            for (let id of chatIds) {
                const chat = await client.send({ '@type': 'getChat', chat_id: id });
                if (chat && chat['@type'] === 'chat') {
                    chatsMap.set(id, chat);
                    const lastMsgText = chat.last_message?.content?.text?.text || '';
                    html += `
                        <div class="chat-item" data-chat-id="${id}">
                            <div class="chat-avatar"><i class="fa-solid fa-users"></i></div>
                            <div class="chat-info">
                                <div class="chat-name">${escapeHtml(chat.title)}</div>
                                <div class="chat-last-msg">${escapeHtml(lastMsgText.substring(0, 50))}</div>
                            </div>
                        </div>
                    `;
                }
            }
            chatsListContainer.innerHTML = html;
            // Asignar eventos
            document.querySelectorAll('.chat-item').forEach(el => {
                el.addEventListener('click', () => {
                    const chatId = parseInt(el.getAttribute('data-chat-id'));
                    openChat(chatId);
                });
            });
        }
    } catch(e) {
        console.error(e);
        chatsListContainer.innerHTML = '<div class="loading-placeholder">Error al cargar chats</div>';
    }
}

async function openChat(chatId) {
    currentChatId = chatId;
    const chat = chatsMap.get(chatId);
    const title = chat ? chat.title : 'Chat';
    currentChatTitleSpan.innerText = title;
    inputMessageContainer.style.display = 'flex';
    messagesList.innerHTML = '<div class="loading-placeholder">Cargando mensajes...</div>';

    try {
        const history = await client.send({
            '@type': 'getChatHistory',
            chat_id: chatId,
            from_message_id: 0,
            offset: 0,
            limit: 50,
            only_local: false
        });
        if (history && history['@type'] === 'messages') {
            messagesList.innerHTML = '';
            if (history.messages.length === 0) {
                messagesList.innerHTML = '<div class="empty-chat"><i class="fa-regular fa-comment"></i><p>No hay mensajes aún</p></div>';
            } else {
                history.messages.reverse().forEach(msg => appendMessageToUI(msg));
            }
        }
    } catch(e) {
        console.error(e);
        messagesList.innerHTML = '<div class="empty-chat">Error al cargar el historial</div>';
    }
}

function appendMessageToUI(msg) {
    const isOutgoing = msg.is_outgoing;
    let text = '';
    if (msg.content['@type'] === 'messageText') {
        text = msg.content.text.text;
    } else {
        text = '[Mensaje no texto]';
    }
    const date = new Date(msg.date * 1000);
    const timeStr = date.toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'});
    const msgDiv = document.createElement('div');
    msgDiv.className = `message-bubble ${isOutgoing ? 'outgoing' : ''}`;
    msgDiv.innerHTML = `
        <div class="message-text">${escapeHtml(text)}</div>
        <div class="message-meta">${timeStr}</div>
    `;
    messagesList.appendChild(msgDiv);
    messagesList.scrollTop = messagesList.scrollHeight;
}

async function sendCurrentMessage() {
    if (!currentChatId || !client) return;
    const text = messageInput.value.trim();
    if (text === '') return;
    messageInput.value = '';
    try {
        await client.send({
            '@type': 'sendMessage',
            chat_id: currentChatId,
            input_message_content: {
                '@type': 'inputMessageText',
                text: { '@type': 'formattedText', text: text }
            }
        });
    } catch(e) { console.error(e); }
}

async function onAuthAction() {
    if (!client) {
        alert("Cliente no inicializado. Espera un momento.");
        return;
    }
    if (currentAuthState === 'authorizationStateWaitPhoneNumber') {
        const phone = phoneInput.value.trim();
        if (!phone) { alert("Introduce el número con código país (ej: +34666111222)"); return; }
        try {
            await client.send({
                '@type': 'setAuthenticationPhoneNumber',
                phone_number: phone,
                settings: { '@type': 'phoneNumberAuthenticationSettings', allow_flash_call: false, is_current_phone_number: true }
            });
            updateStatus("Código enviado");
        } catch(e) { alert("Error: " + e); }
    } 
    else if (currentAuthState === 'authorizationStateWaitCode') {
        const code = codeInput.value.trim();
        if (!code) { alert("Introduce el código que te llegó por SMS"); return; }
        try {
            await client.send({ '@type': 'checkAuthenticationCode', code: code });
        } catch(e) { alert("Código incorrecto"); }
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>]/g, function(m) {
        if (m === '&') return '&amp;';
        if (m === '<') return '&lt;';
        if (m === '>') return '&gt;';
        return m;
    });
}

// Event listeners
authButton.addEventListener('click', onAuthAction);
sendMessageBtn.addEventListener('click', sendCurrentMessage);
messageInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') sendCurrentMessage();
});

// Inicio: esperar a que TdClient esté definido e inicializar
waitForTdClient(() => {
    initTdlib();
});
