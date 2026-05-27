const API_BASE_URL = 'http://localhost:8080/api/v1';
let isTyping = false;

document.addEventListener('DOMContentLoaded', () => {
    setupEventListeners();
    fetchDocumentList();
});

function setupEventListeners() {
    const navButtons = document.querySelectorAll('.nav-btn');
    navButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            switchView(btn.dataset.view);
        });
    });

    const textarea = document.getElementById('userInput');
    textarea.addEventListener('input', autoResize);
}

function switchView(viewName) {
    const navButtons = document.querySelectorAll('.nav-btn');
    navButtons.forEach(btn => btn.classList.remove('active'));
    
    document.querySelector(`[data-view="${viewName}"]`).classList.add('active');
    
    const views = document.querySelectorAll('.chat-view, .documents-view, .settings-view');
    views.forEach(view => view.classList.remove('active-view'));
    
    document.querySelector(`.${viewName}-view`).classList.add('active-view');
}

function autoResize() {
    const textarea = document.getElementById('userInput');
    textarea.style.height = '56px';
    textarea.style.height = Math.min(textarea.scrollHeight, 150) + 'px';
}

function handleKeyDown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

async function sendMessage() {
    const input = document.getElementById('userInput');
    const query = input.value.trim();
    
    if (!query || isTyping) return;
    
    isTyping = true;
    input.value = '';
    input.style.height = '56px';
    
    addMessage(query, 'user');
    
    const typingIndicator = addTypingIndicator();
    
    try {
        const response = await fetch(`${API_BASE_URL}/chat`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ query })
        });
        
        const data = await response.json();
        
        removeTypingIndicator(typingIndicator);
        addMessage(data.content, 'assistant', data.metadata, data.retrievalResults);
        
    } catch (error) {
        removeTypingIndicator(typingIndicator);
        addMessage('抱歉，服务器暂时不可用，请稍后重试。', 'assistant');
        console.error('Error:', error);
    } finally {
        isTyping = false;
        scrollToBottom();
    }
}

function sendQuickQuestion(question) {
    const input = document.getElementById('userInput');
    input.value = question;
    sendMessage();
}

function addMessage(content, sender, metadata = null, retrievalResults = null) {
    const chatHistory = document.getElementById('chatHistory');
    
    const welcomeMsg = document.querySelector('.welcome-message');
    if (welcomeMsg) {
        welcomeMsg.remove();
    }
    
    const messageRow = document.createElement('div');
    messageRow.className = `message-row ${sender}`;
    messageRow.style.animationDelay = '0s';
    
    const avatar = document.createElement('div');
    avatar.className = 'message-avatar';
    avatar.innerHTML = sender === 'user' 
        ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>'
        : '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 3a10.9 10.9 0 0 1-3.14 1.53 4.48 4.48 0 0 0-7.86 3v1A10.66 10.66 0 0 1 3 4s-4 9 5 13a11.64 11.64 0 0 1-7 2c9 5 20 0 20-11.5a4.5 4.5 0 0 0-.08-.83A7.72 7.72 0 0 0 23 3z"></path></svg>';
    
    const messageContent = document.createElement('div');
    messageContent.className = 'message-content';
    
    const bubble = document.createElement('div');
    bubble.className = 'message-bubble';
    bubble.textContent = content;
    
    messageContent.appendChild(bubble);
    
    if (metadata) {
        const meta = document.createElement('div');
        meta.className = 'message-meta';
        meta.innerHTML = `
            <span><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg> ${formatTime(new Date())}</span>
            <span><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"></path><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7.5 18H3v-3.5a2.121 2.121 0 0 1 3-3L16.5 3.5z"></path></svg> ${metadata.engine || 'UNKNOWN'}</span>
        `;
        messageContent.appendChild(meta);
    }
    
    if (retrievalResults && retrievalResults.length > 0) {
        const retrievalDiv = document.createElement('div');
        retrievalDiv.className = 'retrieval-results';
        retrievalDiv.innerHTML = `
            <h5>参考来源:</h5>
            <ul>
                ${retrievalResults.map((result, index) => `
                    <li>来源 ${index + 1}: ${result.source || '知识库'}</li>
                `).join('')}
            </ul>
        `;
        messageContent.appendChild(retrievalDiv);
    }
    
    messageRow.appendChild(avatar);
    messageRow.appendChild(messageContent);
    
    chatHistory.appendChild(messageRow);
    
    scrollToBottom();
}

function addTypingIndicator() {
    const chatHistory = document.getElementById('chatHistory');
    
    const messageRow = document.createElement('div');
    messageRow.className = 'message-row';
    
    const avatar = document.createElement('div');
    avatar.className = 'message-avatar';
    avatar.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 3a10.9 10.9 0 0 1-3.14 1.53 4.48 4.48 0 0 0-7.86 3v1A10.66 10.66 0 0 1 3 4s-4 9 5 13a11.64 11.64 0 0 1-7 2c9 5 20 0 20-11.5a4.5 4.5 0 0 0-.08-.83A7.72 7.72 0 0 0 23 3z"></path></svg>';
    
    const messageContent = document.createElement('div');
    messageContent.className = 'message-content';
    
    const bubble = document.createElement('div');
    bubble.className = 'message-bubble';
    
    const typingIndicator = document.createElement('div');
    typingIndicator.className = 'typing-indicator';
    typingIndicator.innerHTML = '<span></span><span></span><span></span>';
    
    bubble.appendChild(typingIndicator);
    messageContent.appendChild(bubble);
    
    messageRow.appendChild(avatar);
    messageRow.appendChild(messageContent);
    
    chatHistory.appendChild(messageRow);
    scrollToBottom();
    
    return messageRow;
}

function removeTypingIndicator(element) {
    if (element) {
        element.remove();
    }
}

function scrollToBottom() {
    const chatHistory = document.getElementById('chatHistory');
    chatHistory.scrollTop = chatHistory.scrollHeight;
}

function formatTime(date) {
    return date.toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit' 
    });
}

function refreshChat() {
    const chatHistory = document.getElementById('chatHistory');
    chatHistory.innerHTML = `
        <div class="welcome-message">
            <div class="welcome-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M23 3a10.9 10.9 0 0 1-3.14 1.53 4.48 4.48 0 0 0-7.86 3v1A10.66 10.66 0 0 1 3 4s-4 9 5 13a11.64 11.64 0 0 1-7 2c9 5 20 0 20-11.5a4.5 4.5 0 0 0-.08-.83A7.72 7.72 0 0 0 23 3z"></path>
                </svg>
            </div>
            <h4>您好！我是企业知识库助手</h4>
            <p>有什么我可以帮助您的吗？</p>
            <div class="welcome-actions">
                <button class="action-btn" onclick="sendQuickQuestion('公司年假制度是什么？')">年假制度</button>
                <button class="action-btn" onclick="sendQuickQuestion('如何申请报销？')">报销流程</button>
                <button class="action-btn" onclick="sendQuickQuestion('员工手册')">员工手册</button>
            </div>
        </div>
    `;
}

async function handleFileUpload() {
    const fileInput = document.getElementById('fileInput');
    const file = fileInput.files[0];
    
    if (!file) return;
    
    const formData = new FormData();
    formData.append('file', file);
    formData.append('userId', 'current_user');
    
    try {
        const response = await fetch(`${API_BASE_URL}/documents/upload`, {
            method: 'POST',
            body: formData
        });
        
        const result = await response.json();
        
        if (result.status === 'processing') {
            showToast(`文档 "${file.name}" 已开始处理`, 'success');
        } else {
            showToast(result.message, 'error');
        }
        
        fetchDocumentList();
        
    } catch (error) {
        showToast('文件上传失败', 'error');
        console.error('Upload error:', error);
    }
    
    fileInput.value = '';
}

async function fetchDocumentList() {
    try {
        const response = await fetch(`${API_BASE_URL}/documents`);
        const result = await response.json();
        
        const documentList = document.getElementById('documentList');
        
        if (result.totalDocuments === 0) {
            documentList.innerHTML = `
                <div class="empty-state">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                        <polyline points="14 2 14 8 20 8"></polyline>
                        <line x1="16" y1="13" x2="8" y2="13"></line>
                        <line x1="16" y1="17" x2="8" y2="17"></line>
                    </svg>
                    <p>暂无文档，点击上方按钮上传</p>
                </div>
            `;
            return;
        }
        
        documentList.innerHTML = `
            <div class="document-item">
                <div class="doc-info">
                    <div class="doc-icon">📄</div>
                    <div>
                        <h4>示例文档</h4>
                        <div class="doc-meta">${result.totalChunks} 个片段 · PDF · 今天上传</div>
                    </div>
                </div>
                <button class="delete-btn" onclick="deleteDocument('test-doc')">删除</button>
            </div>
        `;
        
    } catch (error) {
        console.error('Error fetching documents:', error);
    }
}

async function deleteDocument(docId) {
    try {
        const response = await fetch(`${API_BASE_URL}/documents/${docId}`, {
            method: 'DELETE'
        });
        
        const result = await response.json();
        
        if (result.status === 'success') {
            showToast('文档已删除', 'success');
            fetchDocumentList();
        } else {
            showToast(result.message, 'error');
        }
        
    } catch (error) {
        showToast('删除失败', 'error');
        console.error('Delete error:', error);
    }
}

function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    
    toast.style.cssText = `
        position: fixed;
        bottom: 30px;
        right: 30px;
        padding: 14px 24px;
        border-radius: 12px;
        font-size: 14px;
        font-weight: 500;
        z-index: 1000;
        animation: slideIn 0.3s ease-out;
        ${type === 'success' ? 'background: rgba(0, 217, 166, 0.9); color: white;' : ''}
        ${type === 'error' ? 'background: rgba(255, 82, 82, 0.9); color: white;' : ''}
        ${type === 'info' ? 'background: rgba(79, 172, 254, 0.9); color: white;' : ''}
    `;
    
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.style.animation = 'fadeOut 0.3s ease-out';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

document.head.insertAdjacentHTML('beforeend', `
    <style>
        @keyframes slideIn {
            from { transform: translateX(100px); opacity: 0; }
            to { transform: translateX(0); opacity: 1; }
        }
        @keyframes fadeOut {
            from { transform: translateX(0); opacity: 1; }
            to { transform: translateX(100px); opacity: 0; }
        }
    </style>
`);

const sendBtn = document.getElementById('sendBtn');
sendBtn.addEventListener('click', function(e) {
    const ripple = document.createElement('span');
    const rect = this.getBoundingClientRect();
    const size = Math.max(rect.width, rect.height);
    const x = e.clientX - rect.left - size / 2;
    const y = e.clientY - rect.top - size / 2;
    
    ripple.style.cssText = `
        position: absolute;
        width: ${size}px;
        height: ${size}px;
        left: ${x}px;
        top: ${y}px;
        background: rgba(255, 255, 255, 0.3);
        border-radius: 50%;
        transform: scale(0);
        animation: ripple-effect 0.6s ease-out;
        pointer-events: none;
    `;
    
    this.appendChild(ripple);
    
    setTimeout(() => ripple.remove(), 600);
});

document.head.insertAdjacentHTML('beforeend', `
    <style>
        @keyframes ripple-effect {
            0% { transform: scale(0); opacity: 0.5; }
            100% { transform: scale(4); opacity: 0; }
        }
    </style>
`);

const navButtons = document.querySelectorAll('.nav-btn');
navButtons.forEach(btn => {
    btn.addEventListener('mouseenter', function() {
        this.style.transform = 'translateX(8px)';
    });
    
    btn.addEventListener('mouseleave', function() {
        if (!this.classList.contains('active')) {
            this.style.transform = 'translateX(0)';
        }
    });
});