// ================= CONFIGURACIÓN =================
// REEMPLAZA CON TUS CREDENCIALES DE my.telegram.org
const API_ID = 8952741;            // TU API ID
const API_HASH = "693fb2da124662dad85b2b337c53a386";   // TU API HASH
// =================================================

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
const currentChatTitleSpan = document.getElementById('currentChatTitle');
const inputMessageContainer = document.getElementById('inputMessageContainer');
const messageInput = document.getElementById('messageInput');
const sendMessageBtn = document.getElementById('sendMessageBtn');

function updateStatus(msg, isError = false) {
    connectionStatus.innerHTML = msg;
    if (isError) console.error(msg);
}

// Esperar a que TdClient exista (carga local)
function waitForTdClient(callback) {
    if (typeof TdClient !== 'undefined') {
        callback();
    } else {
        updateStatus("Cargando librería tdweb...");
        setTimeout(() => waitForTdClient(callback), 500);
    }
}

function initTdlib() {
    try {
        client = new TdClient({
            apiId: API_ID,
            apiHash: API_HASH,
            databaseDirectory: "tdlib_db",
            useDatabase: true,
            useChatInfoDatabase: true,
            useMessageDatabase: true,
            deviceModel: "Web Client",
            systemVersion: "1.0",
            applicationVersion: "1.0.0",
            onUpdate: handleUpdate,
            onError: (err) => {
                console.error(err);
                updateStatus("Error TDLib: " + JSON.stringify(err), true);
            }
        });
        updateStatus("Cliente creado, esperando estado...");
    } catch (err) {
        updateStatus("Fatal: " + err.message, true);
    }
}

function handleUpdate(update) {
    console.log("Update:", update['@type']);
    switch(update['@type']) {
        case 'updateAuthorizationState':
            handleAuthState(update.authorization_state);
            break;
        case 'updateNewMessage':
            if (currentChatId === update.message.chat_id) appendMessageToUI(update.message);
            renderChatsList();
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
        const result = await client.send({ '@type': 'getChats', offset_order: '9223372036854775807', offset_chat_id: 0, limit: 100 });
        if (result && result['@type'] === 'chats') {
            const chatIds = result.chat_ids;
            if (!chatIds.length) { chatsListContainer.innerHTML = '<div>No hay chats</div>'; return; }
            let html = '';
            for (let id of chatIds) {
                const chat = await client.send({ '@type': 'getChat', chat_id: id });
                if (chat && chat['@type'] === 'chat') {
                    chatsMap.set(id, chat);
                    html += `<div class="chat-item" data-chat-id="${id}">
                                <div class="chat-avatar"><i class="fa-solid fa-users"></i></div>
                                <div class="chat-info">
                                    <div class="chat-name">${escapeHtml(chat.title)}</div>
                                    <div class="chat-last-msg">${escapeHtml(chat.last_message?.content?.text?.text || '')}</div>
                                </div>
                            </div>`;
                }
            }
            chatsListContainer.innerHTML = html;
            document.querySelectorAll('.chat-item').forEach(el => {
                el.addEventListener('click', () => openChat(parseInt(el.dataset.chatId)));
            });
        }
    } catch(e) { console.error(e); }
}

async function openChat(chatId) {
    currentChatId = chatId;
    const chat = chatsMap.get(chatId);
    currentChatTitleSpan.innerText = chat ? chat.title : 'Chat';
    inputMessageContainer.style.display = 'flex';
    messagesList.innerHTML = '<div class="loading-placeholder">Cargando mensajes...</div>';
    try {
        const history = await client.send({ '@type': 'getChatHistory', chat_id: chatId, limit: 50 });
        if (history && history['@type'] === 'messages') {
            messagesList.innerHTML = '';
            history.messages.reverse().forEach(msg => appendMessageToUI(msg));
        }
    } catch(e) { messagesList.innerHTML = '<div>Error al cargar</div>'; }
}

function appendMessageToUI(msg) {
    const isOut = msg.is_outgoing;
    let text = msg.content['@type'] === 'messageText' ? msg.content.text.text : '[Otro tipo]';
    const date = new Date(msg.date * 1000);
    const timeStr = date.toLocaleTimeString([], { hour:'2-digit', minute:'2-digit' });
    const div = document.createElement('div');
    div.className = `message-bubble ${isOut ? 'outgoing' : ''}`;
    div.innerHTML = `<div class="message-text">${escapeHtml(text)}</div><div class="message-meta">${timeStr}</div>`;
    messagesList.appendChild(div);
    messagesList.scrollTop = messagesList.scrollHeight;
}

async function sendCurrentMessage() {
    if (!currentChatId || !client) return;
    const text = messageInput.value.trim();
    if (!text) return;
    messageInput.value = '';
    await client.send({ '@type': 'sendMessage', chat_id: currentChatId, input_message_content: { '@type': 'inputMessageText', text: { '@type': 'formattedText', text: text } } });
}

async function onAuthAction() {
    if (!client) { alert("Cliente no listo, espera..."); return; }
    if (currentAuthState === 'authorizationStateWaitPhoneNumber') {
        const phone = phoneInput.value.trim();
        if (!phone) { alert("Introduce número con código país"); return; }
        await client.send({ '@type': 'setAuthenticationPhoneNumber', phone_number: phone, settings: { '@type': 'phoneNumberAuthenticationSettings', allow_flash_call: false, is_current_phone_number: true } });
    } 
    else if (currentAuthState === 'authorizationStateWaitCode') {
        const code = codeInput.value.trim();
        if (!code) { alert("Introduce el código"); return; }
        await client.send({ '@type': 'checkAuthenticationCode', code: code });
    }
}

function escapeHtml(str) { if (!str) return ''; return str.replace(/[&<>]/g, m => m==='&'?'&amp;': m==='<'?'&lt;':'&gt;'); }

authButton.addEventListener('click', onAuthAction);
sendMessageBtn.addEventListener('click', sendCurrentMessage);
messageInput.addEventListener('keypress', e => { if (e.key === 'Enter') sendCurrentMessage(); });

// Inicio
waitForTdClient(() => {
    initTdlib();
});
