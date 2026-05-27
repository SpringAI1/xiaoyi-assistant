const API_BASE_URL = '/api/v1';
let isTyping = false;
let documents = [];
let uploadedFile = null;
let currentSessionId = null;

document.addEventListener('DOMContentLoaded', () => {
    setupEventListeners();
    fetchDocumentList();
    createSession();
});

function setupEventListeners() {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(btn => {
        btn.addEventListener('click', () => {
            switchView(btn.dataset.view);
        });
    });

    const textarea = document.getElementById('userInput');
    if (textarea) {
        textarea.addEventListener('input', autoResize);
    }
}

async function createSession() {
    try {
        const response = await fetch(`${API_BASE_URL}/chat/session`, {
            method: 'POST'
        });
        const data = await response.json();
        if (data.sessionId) {
            currentSessionId = data.sessionId;
        }
    } catch (error) {
        console.error('Failed to create session:', error);
    }
}

function switchView(viewName) {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(btn => btn.classList.remove('active'));

    document.querySelector(`[data-view="${viewName}"]`).classList.add('active');

    const views = document.querySelectorAll('.view-panel');
    views.forEach(view => view.classList.remove('active'));

    const viewElement = document.getElementById(`${viewName}-view`);
    if (viewElement) {
        viewElement.classList.add('active');
    }

    if (viewName === 'documents') {
        fetchDocumentList();
    }
}

function autoResize() {
    const textarea = document.getElementById('userInput');
    if (textarea) {
        textarea.style.height = '40px';
        textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px';
    }
}

function handleKeyDown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

function handleChatFileUpload() {
    const fileInput = document.getElementById('chatFileInput');
    if (!fileInput || !fileInput.files[0]) return;

    const file = fileInput.files[0];
    uploadedFile = file;
    
    const fileInfo = document.getElementById('uploadedFileInfo');
    if (fileInfo) {
        fileInfo.innerHTML = `
            <span>${file.name}</span>
            <button class="remove-file-btn" onclick="removeUploadedFile()">×</button>
        `;
        fileInfo.style.display = 'flex';
    }
    
    showToast('文件已选择，请输入问题后一起发送', 'info');
}

function removeUploadedFile() {
    uploadedFile = null;
    const fileInput = document.getElementById('chatFileInput');
    const fileInfo = document.getElementById('uploadedFileInfo');
    
    if (fileInput) {
        fileInput.value = '';
    }
    if (fileInfo) {
        fileInfo.style.display = 'none';
    }
}

async function sendMessage() {
    const input = document.getElementById('userInput');
    if (!input) return;

    const query = input.value.trim();

    if (!query && !uploadedFile) return;
    if (isTyping) return;

    isTyping = true;
    input.value = '';
    input.style.height = '40px';

    if (uploadedFile) {
        addMessage(`[文件] ${uploadedFile.name}\n\n${query || '请分析这个文件的内容'}`, 'user');
    } else {
        addMessage(query, 'user');
    }

    const typingIndicator = addTypingIndicator();

    try {
        if (uploadedFile) {
            await uploadAndProcessFile(typingIndicator);
        } else {
            const searchMode = window.currentSearchMode || 'knowledge';
            
            const response = await fetch(`${API_BASE_URL}/chat`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({
                    query: query,
                    searchMode: searchMode,
                    sessionId: currentSessionId
                })
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();

            removeTypingIndicator(typingIndicator);

            if (data.content) {
                addMessage(data.content, 'assistant', data.metadata, data.retrievalResults);
            } else {
                addMessage('抱歉，我无法理解您的问题。', 'assistant');
            }
        }

    } catch (error) {
        removeTypingIndicator(typingIndicator);
        addMessage('抱歉，服务器暂时不可用，请稍后重试。\n错误信息: ' + error.message, 'assistant');
        console.error('Error:', error);
    } finally {
        isTyping = false;
        removeUploadedFile();
        scrollToBottom();
    }
}

async function uploadAndProcessFile(typingIndicator) {
    const formData = new FormData();
    formData.append('file', uploadedFile);
    formData.append('userId', 'current_user');

    try {
        const response = await fetch(`${API_BASE_URL}/documents/upload`, {
            method: 'POST',
            body: formData
        });

        const result = await response.json();

        if (result.status === 'processing') {
            documents.push({
                id: 'doc_' + Date.now(),
                name: uploadedFile.name,
                type: getFileExtension(uploadedFile.name),
                size: formatFileSize(uploadedFile.size),
                uploadedAt: new Date().toLocaleString()
            });
            
            await sendMessageWithFile(typingIndicator);
        } else {
            removeTypingIndicator(typingIndicator);
            addMessage('文件上传失败: ' + (result.message || '未知错误'), 'assistant');
        }

    } catch (error) {
        removeTypingIndicator(typingIndicator);
        addMessage('文件处理失败: ' + error.message, 'assistant');
    }
}

async function sendMessageWithFile(typingIndicator) {
    const formData = new FormData();
    formData.append('file', uploadedFile);
    formData.append('query', document.getElementById('userInput').value.trim());
    if (currentSessionId) {
        formData.append('sessionId', currentSessionId);
    }

    try {
        const response = await fetch(`${API_BASE_URL}/chat/upload`, {
            method: 'POST',
            body: formData
        });

        const data = await response.json();

        removeTypingIndicator(typingIndicator);

        if (data.content) {
            addMessage(data.content, 'assistant', data.metadata, data.retrievalResults);
        } else {
            addMessage('抱歉，我无法处理这个文件。', 'assistant');
        }

    } catch (error) {
        removeTypingIndicator(typingIndicator);
        addMessage('文件处理失败: ' + error.message, 'assistant');
    }
}

function sendQuickQuestion(question) {
    const input = document.getElementById('userInput');
    if (input) {
        input.value = question;
        sendMessage();
    }
}

function addMessage(content, sender, metadata = null, retrievalResults = null) {
    const chatMessages = document.getElementById('chatMessages');
    if (!chatMessages) return;

    const welcomePanel = document.getElementById('welcomePanel');
    if (welcomePanel) {
        welcomePanel.remove();
    }

    const messageRow = document.createElement('div');
    messageRow.className = `message-row ${sender}`;

    const avatar = document.createElement('div');
    avatar.className = 'message-avatar';
    avatar.innerHTML = sender === 'user'
        ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>'
        : '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 3a10.9 10.9 0 0 1-3.14 1.53 4.48 4.48 0 0 0-7.86 3v1A10.66 10.66 0 0 1 3 4s-4 9 5 13a11.64 11.64 0 0 1-7 2c9 5 20 0 20-11.5a4.5 4.5 0 0 0-.08-.83A7.72 7.72 0 0 0 23 3z"></path></svg>';

    const messageContent = document.createElement('div');
    messageContent.className = 'message-content';

    const bubble = document.createElement('div');
    bubble.className = 'message-bubble';
    
    // 处理内容中的链接
    let formattedContent = content;
    
    // 查找API链接并转换为可点击的链接
    if (content.includes('/api/v1/generate/ppt/download')) {
        const urlPattern = /(\/api\/v1\/generate\/ppt\/download\?topic=[^\s]+)/g;
        formattedContent = content.replace(urlPattern, function(match) {
            const fullUrl = window.location.origin + match;
            return '<a href="' + fullUrl + '" download style="color: #1890ff; text-decoration: underline; cursor: pointer;" target="_blank">' + fullUrl + '</a>';
        });
    }
    
    // 使用innerHTML以支持链接
    bubble.innerHTML = formattedContent.replace(/\n/g, '<br>');

    messageContent.appendChild(bubble);

    if (metadata) {
        const meta = document.createElement('div');
        meta.className = 'message-meta';
        meta.innerHTML = `
            <span>${formatTime(new Date())}</span>
            <span>${metadata.engine || 'UNKNOWN'}</span>
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

    chatMessages.appendChild(messageRow);

    scrollToBottom();
}

function addTypingIndicator() {
    const chatMessages = document.getElementById('chatMessages');
    if (!chatMessages) return null;

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

    chatMessages.appendChild(messageRow);
    scrollToBottom();

    return messageRow;
}

function removeTypingIndicator(element) {
    if (element && element.parentNode) {
        element.parentNode.removeChild(element);
    }
}

function scrollToBottom() {
    const chatMessages = document.getElementById('chatMessages');
    if (chatMessages) {
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }
}

function formatTime(date) {
    return date.toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit'
    });
}

function refreshChat() {
    const chatMessages = document.getElementById('chatMessages');
    if (!chatMessages) return;

    chatMessages.innerHTML = `
        <div class="welcome-panel" id="welcomePanel">
            <div class="welcome-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M23 3a10.9 10.9 0 0 1-3.14 1.53 4.48 4.48 0 0 0-7.86 3v1A10.66 10.66 0 0 1 3 4s-4 9 5 13a11.64 11.64 0 0 1-7 2c9 5 20 0 20-11.5a4.5 4.5 0 0 0-.08-.83A7.72 7.72 0 0 0 23 3z"></path>
                </svg>
            </div>
            <h3>欢迎使用小易助手</h3>
            <p id="welcomeDesc">有什么我可以帮助您的吗？</p>
            <div class="quick-actions" id="quickActions">
                <button class="quick-btn" onclick="sendQuickQuestion('公司年假制度')">年假制度</button>
                <button class="quick-btn" onclick="sendQuickQuestion('报销流程')">报销流程</button>
                <button class="quick-btn" onclick="sendQuickQuestion('考勤政策')">考勤政策</button>
            </div>
        </div>
    `;
    
    createSession();
}

async function handleFileUpload() {
    const fileInput = document.getElementById('fileInput');
    if (!fileInput || !fileInput.files[0]) return;

    const file = fileInput.files[0];

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
            documents.push({
                id: 'doc_' + Date.now(),
                name: file.name,
                type: getFileExtension(file.name),
                size: formatFileSize(file.size),
                uploadedAt: new Date().toLocaleString()
            });
            showToast(`文档 "${file.name}" 已开始处理`, 'success');
            renderDocumentList();
        } else {
            showToast(result.message || '上传失败', 'error');
        }

    } catch (error) {
        showToast('文件上传失败: ' + error.message, 'error');
        console.error('Upload error:', error);
    }

    fileInput.value = '';
}

function getFileExtension(filename) {
    const lastDot = filename.lastIndexOf('.');
    return lastDot > 0 ? filename.substring(lastDot + 1).toUpperCase() : 'UNKNOWN';
}

function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

async function fetchDocumentList() {
    try {
        const response = await fetch(`${API_BASE_URL}/documents`);
        const result = await response.json();

        const docCountEl = document.getElementById('docCount');
        if (docCountEl) {
            docCountEl.textContent = result.totalDocuments || documents.length;
        }

        if (result.totalDocuments === 0 && documents.length === 0) {
            renderEmptyDocumentList();
            return;
        }

        if (result.totalDocuments > 0 && documents.length === 0) {
            for (let i = 0; i < result.totalDocuments; i++) {
                documents.push({
                    id: 'doc_' + i,
                    name: `文档 ${i + 1}`,
                    type: 'PDF',
                    size: '1.2 MB',
                    uploadedAt: new Date().toLocaleString()
                });
            }
        }

        renderDocumentList();

    } catch (error) {
        console.error('Error fetching documents:', error);
        if (documents.length === 0) {
            renderEmptyDocumentList();
        } else {
            renderDocumentList();
        }
    }
}

function renderDocumentList() {
    const documentGrid = document.getElementById('documentGrid');
    if (!documentGrid) return;

    if (documents.length === 0) {
        renderEmptyDocumentList();
        return;
    }

    documentGrid.innerHTML = documents.map(doc => `
        <div class="document-card">
            <div class="document-icon">${getFileIcon(doc.type)}</div>
            <h4>${doc.name}</h4>
            <p>${doc.type} · ${doc.size} · ${doc.uploadedAt}</p>
            <div class="document-actions">
                <button class="doc-action-btn" onclick="viewDocument('${doc.id}')">查看</button>
                <button class="doc-action-btn" onclick="deleteDocument('${doc.id}')">删除</button>
            </div>
        </div>
    `).join('');
}

function renderEmptyDocumentList() {
    const documentGrid = document.getElementById('documentGrid');
    if (!documentGrid) return;

    documentGrid.innerHTML = `
        <div class="empty-state">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
            </svg>
            <p>暂无文档</p>
            <p class="empty-hint">点击上方按钮上传文档到知识库</p>
        </div>
    `;
}

function getFileIcon(type) {
    const icons = {
        'PDF': '📄',
        'DOCX': '📝',
        'DOC': '📝',
        'PPTX': '📑',
        'PPT': '📑',
        'XLSX': '📊',
        'XLS': '📊',
        'TXT': '📃',
        'MD': '📖',
        'JPG': '🖼️',
        'JPEG': '🖼️',
        'PNG': '🖼️',
        'GIF': '🖼️',
        'BMP': '🖼️'
    };
    return icons[type] || '📄';
}

function deleteDocument(docId) {
    documents = documents.filter(doc => doc.id !== docId);
    renderDocumentList();
    
    const docCountEl = document.getElementById('docCount');
    if (docCountEl) {
        docCountEl.textContent = documents.length;
    }
    
    showToast('文档已删除', 'success');
}

function viewDocument(docId) {
    const doc = documents.find(d => d.id === docId);
    if (doc) {
        showToast(`查看文档: ${doc.name}`, 'info');
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
        min-width: 200px;
        max-width: 400px;
        word-break: break-word;
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
        @keyframes slideIn { from { transform: translateX(100px); opacity: 0; } to { transform: translateX(0); opacity: 1; } }
        @keyframes fadeOut { from { transform: translateX(0); opacity: 1; } to { transform: translateX(100px); opacity: 0; } }
    </style>
`);

const sendBtn = document.getElementById('sendBtn');
if (sendBtn) {
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

        setTimeout(() => {
            if (ripple.parentNode) {
                ripple.parentNode.removeChild(ripple);
            }
        }, 600);
    });
}

document.head.insertAdjacentHTML('beforeend', `
    <style>
        @keyframes ripple-effect {
            0% { transform: scale(0); opacity: 0.5; }
            100% { transform: scale(4); opacity: 0; }
        }
    </style>
`);

const navItems = document.querySelectorAll('.nav-item');
navItems.forEach(btn => {
    btn.addEventListener('mouseenter', function() {
        if (!this.classList.contains('active')) {
            this.style.transform = 'translateX(4px)';
        }
    });

    btn.addEventListener('mouseleave', function() {
        if (!this.classList.contains('active')) {
            this.style.transform = 'translateX(0)';
        }
    });
});

document.head.insertAdjacentHTML('beforeend', `
    <style>
        .upload-icon-btn {
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            width: 36px;
            height: 36px;
            border-radius: 50%;
            background: #f0f0f0;
            transition: all 0.2s;
        }
        .upload-icon-btn:hover {
            background: #e0e0e0;
            transform: scale(1.05);
        }
        .upload-icon-btn svg {
            width: 20px;
            height: 20px;
            stroke: #666;
        }
        .uploaded-file-info {
            display: flex;
            align-items: center;
            gap: 8px;
            background: #f5f5f5;
            padding: 6px 12px;
            border-radius: 8px;
            font-size: 13px;
            color: #666;
        }
        .remove-file-btn {
            background: none;
            border: none;
            cursor: pointer;
            font-size: 18px;
            color: #999;
            padding: 0;
            line-height: 1;
            transition: color 0.2s;
        }
        .remove-file-btn:hover {
            color: #ff6b6b;
        }
        .input-actions {
            display: flex;
            align-items: center;
            gap: 8px;
            padding-right: 8px;
        }
    </style>
`);
