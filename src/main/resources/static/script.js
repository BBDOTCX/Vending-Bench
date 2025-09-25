document.addEventListener('DOMContentLoaded', () => {
    const startButton = document.getElementById('startButton');
    const pauseButton = document.getElementById('pauseButton');
    const resetButton = document.getElementById('resetButton');
    const exportButton = document.getElementById('exportButton');
    const resumeButton = document.getElementById('resume-button');

    let pollingInterval;
    let fullState = {};

    const API_BASE_URL = 'http://localhost:8080/api/simulation';

    startButton.addEventListener('click', startSimulation);
    pauseButton.addEventListener('click', pauseSimulation);
    resetButton.addEventListener('click', resetSimulation);
    exportButton.addEventListener('click', exportData);
    resumeButton.addEventListener('click', resumeWithHumanInput);

    function startSimulation() {
        const apiKey = document.getElementById('apiKey').value;
        const provider = document.getElementById('providerSelect').value;
        const modelName = document.getElementById('modelName').value;
        const persona = document.getElementById('persona').value;
        const maxTurns = parseInt(document.getElementById('maxTurns').value, 10);

        if (provider !== 'ollama' && !apiKey) {
            alert('API Key is required for the selected provider.');
            return;
        }

        fetch(`${API_BASE_URL}/start`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ provider, apiKey, modelName, persona, maxTurns }),
        })
        .then(response => {
            if (response.ok) {
                setControlsState(true);
                pollingInterval = setInterval(fetchState, 1000);
            } else {
                alert('Failed to start simulation. Check the console for errors.');
            }
        })
        .catch(error => {
            console.error('Error starting simulation:', error);
            alert('Failed to start simulation. Is the Java backend running?');
        });
    }

    function pauseSimulation() {
        fetch(`${API_BASE_URL}/pause`, { method: 'POST' });
    }

    function resetSimulation() {
        fetch(`${API_BASE_URL}/reset`, { method: 'POST' })
        .then(() => {
            clearInterval(pollingInterval);
            setControlsState(false);
            resetUI();
        });
    }

    function resumeWithHumanInput() {
        const prompt = document.getElementById('human-prompt').value;
        if (!prompt.trim()) {
            alert("Please provide an instruction for the agent.");
            return;
        }
        fetch(`${API_BASE_URL}/resume`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ prompt }),
        })
        .then(response => {
            if (response.ok) {
                document.getElementById('human-input-section').classList.add('hidden');
            } else {
                alert("Failed to resume with human input.");
            }
        });
    }


    function fetchState() {
        fetch(`${API_BASE_URL}/state`)
            .then(response => response.json())
            .then(data => {
                fullState = data;
                updateUI(data);
                if (data.status === 'Finished' || data.status.startsWith('Error')) {
                    clearInterval(pollingInterval);
                }
            })
            .catch(error => {
                console.error('Error fetching state:', error);
                clearInterval(pollingInterval);
            });
    }

    function updateUI(data) {
        document.getElementById('sim-status').textContent = data.status || 'N/A';
        document.getElementById('main-agent-status').textContent = data.mainAgentStatus || 'N/A';
        document.getElementById('sub-agent-status').textContent = data.subAgentStatus || 'N/A';
        document.getElementById('agent-thought').textContent = data.mainAgentThought || '...';
        
        if (data.simulationState) {
            document.getElementById('turn-counter').textContent = data.simulationState.turn || 0;
            document.getElementById('day-counter').textContent = data.simulationState.day || 0;
            document.getElementById('cash-balance').textContent = `$${data.simulationState.cashBalance.toFixed(2)}`;
            updateInbox(data.simulationState.emailInbox);
            if (data.simulationState.sentEmails) {
                updateSentEmails(data.simulationState.sentEmails);
            }
        }
        
        document.getElementById('net-worth').textContent = `$${(data.netWorth || 0).toFixed(2)}`;
        
        const mainLog = document.getElementById('mainLog');
        if (data.mainEventLog) {
            mainLog.value = data.mainEventLog.join('\n');
            mainLog.scrollTop = mainLog.scrollHeight;
        }

        if (data.verboseLog) {
            document.getElementById('verboseLog').value = JSON.stringify(data.verboseLog, null, 2);
        }

        const humanInputSection = document.getElementById('human-input-section');
        if (data.status === 'Awaiting Human Input') {
            humanInputSection.classList.remove('hidden');
        } else {
            humanInputSection.classList.add('hidden');
        }
    }
    
    function updateInbox(inbox) {
        const inboxContainer = document.getElementById('inboxContainer');
        inboxContainer.innerHTML = '';
        if (!inbox || inbox.length === 0) {
            const emptyMsg = document.createElement('p');
            emptyMsg.className = 'empty-inbox-message';
            emptyMsg.textContent = 'Inbox is empty.';
            inboxContainer.appendChild(emptyMsg);
        } else {
            inbox.forEach(email => {
                const emailItem = document.createElement('div');
                emailItem.className = 'email-item';
                const header = document.createElement('div');
                header.className = 'email-header';
                header.innerHTML = `From: <span>${email.sender}</span>`;
                const body = document.createElement('div');
                body.className = 'email-body';
                body.textContent = email.body;
                emailItem.appendChild(header);
                emailItem.appendChild(body);
                inboxContainer.appendChild(emailItem);
            });
        }
    }

    function updateSentEmails(sentEmails) {
        const sentEmailsContainer = document.getElementById('sentEmailsContainer');
        if (!sentEmailsContainer) return;
        
        sentEmailsContainer.innerHTML = '';
        if (!sentEmails || sentEmails.length === 0) {
            const emptyMsg = document.createElement('p');
            emptyMsg.className = 'empty-sent-emails-message';
            emptyMsg.textContent = 'No emails sent yet.';
            sentEmailsContainer.appendChild(emptyMsg);
        } else {
            // Display latest first
            sentEmails.slice().reverse().forEach(email => {
                const emailItem = document.createElement('div');
                emailItem.className = 'sent-email-item';
                const header = document.createElement('div');
                header.className = 'sent-email-header';
                header.innerHTML = `To: <span>${email.recipient}</span>`;
                const body = document.createElement('div');
                body.className = 'sent-email-body';
                body.textContent = email.body;
                emailItem.appendChild(header);
                emailItem.appendChild(body);
                sentEmailsContainer.appendChild(emailItem);
            });
        }
    }

    function setControlsState(isRunning) {
        startButton.disabled = isRunning;
        pauseButton.disabled = !isRunning;
        resetButton.disabled = !isRunning;
    }

    function resetUI() {
        document.getElementById('sim-status').textContent = 'Idle';
        document.getElementById('main-agent-status').textContent = 'Idle';
        document.getElementById('sub-agent-status').textContent = 'Idle';
        document.getElementById('turn-counter').textContent = '0';
        document.getElementById('day-counter').textContent = '0';
        document.getElementById('cash-balance').textContent = '$0.00';
        document.getElementById('net-worth').textContent = '$0.00';
        document.getElementById('mainLog').value = '';
        document.getElementById('verboseLog').value = '';
        document.getElementById('agent-thought').textContent = '...';
        updateInbox([]);
        updateSentEmails([]);
    }

    function exportData() {
        const dataStr = JSON.stringify(fullState, null, 2);
        const dataBlob = new Blob([dataStr], { type: 'application/json' });
        const url = URL.createObjectURL(dataBlob);
        const link = document.createElement('a');
        link.download = `vending-bench-export-${new Date().toISOString()}.json`;
        link.href = url;
        link.click();
        URL.revokeObjectURL(url);
    }
});