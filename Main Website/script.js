document.addEventListener('DOMContentLoaded', () => {
    // Navbar scroll effect
    const navbar = document.querySelector('.navbar');
    
    window.addEventListener('scroll', () => {
        if (window.scrollY > 50) {
            navbar.classList.add('scrolled');
        } else {
            navbar.classList.remove('scrolled');
        }
    });

    // Scroll reveal animation using IntersectionObserver
    const revealElements = document.querySelectorAll('.reveal');
    
    const revealOptions = {
        threshold: 0.1,
        rootMargin: "0px 0px -50px 0px"
    };

    const revealOnScroll = new IntersectionObserver(function(entries, observer) {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('active');
            }
        });
    }, revealOptions);

    revealElements.forEach(el => {
        revealOnScroll.observe(el);
    });

    // Mouse tracking for interactive cards (Glow effect)
    const interactiveCards = document.querySelectorAll('.interactive-card');
    const featuresGrid = document.getElementById('features-grid');
    
    if(featuresGrid) {
        featuresGrid.addEventListener('mousemove', e => {
            interactiveCards.forEach(card => {
                const rect = card.getBoundingClientRect();
                const x = e.clientX - rect.left;
                const y = e.clientY - rect.top;
                
                card.style.setProperty('--mouse-x', `${x}px`);
                card.style.setProperty('--mouse-y', `${y}px`);
            });
        });
    }

    // Parallax effect on scroll
    const parallaxImages = document.querySelectorAll('.parallax-img');
    const ambientOrbs = document.querySelectorAll('.ambient-orb');
    
    window.addEventListener('scroll', () => {
        const scrolled = window.scrollY;
        
        // Image parallax (slight upward move on scroll)
        parallaxImages.forEach(img => {
            const speed = 0.05;
            img.style.transform = `translateY(${scrolled * speed}px) scale(1.1)`;
        });

        // Ambient background orbs parallax
        ambientOrbs.forEach((orb, index) => {
            const speed = (index + 1) * 0.03;
            // The negative gives it a nice contrary motion
            orb.style.transform = `translateY(${-scrolled * speed}px)`;
        });
    });

    // ─── Robot Mascot Chatbot ────────────────────────────────
    const robotMascot  = document.getElementById('robot-mascot');
    const chatWindow   = document.getElementById('chat-window');
    const closeChat    = document.getElementById('close-chat');
    const chatInput    = document.getElementById('chat-input');
    const sendChatBtn  = document.getElementById('send-chat');
    const chatMessages = document.getElementById('chat-messages');
    const robotBubble  = document.getElementById('robot-bubble');

    // Rotate speech bubble messages when chat is closed
    const bubblePhrases = [
        "Ask me anything!",
        "What's a deepfake?",
        "How does it work?",
        "Am I safe online?",
        "Detect fakes now! 🔍"
    ];
    let bubbleIdx = 0;
    let bubbleTimer;

    function showBubble(text) {
        robotBubble.textContent = text;
        robotBubble.classList.add('visible');
    }
    function hideBubble() {
        robotBubble.classList.remove('visible');
    }

    function startBubbleRotation() {
        showBubble(bubblePhrases[bubbleIdx]);
        bubbleTimer = setInterval(() => {
            hideBubble();
            setTimeout(() => {
                bubbleIdx = (bubbleIdx + 1) % bubblePhrases.length;
                showBubble(bubblePhrases[bubbleIdx]);
            }, 400);
        }, 4000);
    }
    function stopBubbleRotation() {
        clearInterval(bubbleTimer);
        hideBubble();
    }

    // Eye tracking is handled by vera3d.js (Three.js)
    // State setter — delegates to Three.js robot
    function setRobotState(state) {
        if (typeof window.setVeraState === 'function') {
            window.setVeraState(state);
        }
    }

    // Toggle chat window
    robotMascot.addEventListener('click', (e) => {
        if (e.target.closest('#chat-window')) return; // don't close if clicking inside window
        const isOpen = !chatWindow.classList.contains('hidden');
        if (isOpen) {
            chatWindow.classList.add('hidden');
            startBubbleRotation();
        } else {
            chatWindow.classList.remove('hidden');
            stopBubbleRotation();
            chatInput.focus();
        }
    });

    closeChat.addEventListener('click', (e) => {
        e.stopPropagation();
        chatWindow.classList.add('hidden');
        startBubbleRotation();
    });

    // Start bubble rotation on load (after short delay)
    setTimeout(startBubbleRotation, 2000);

    // Chat history (system prompt)
    let chatHistory = [
        { role: "user",  parts: [{ text: "You are Vera, a friendly AI assistant for a Deepfake Detection academic project. Keep answers concise, warm, and helpful." }] },
        { role: "model", parts: [{ text: "Hi! I'm Vera, ready to help with anything about deepfakes!" }] }
    ];

    function addMessage(text, isUser = false) {
        const msgDiv = document.createElement('div');
        msgDiv.className = `message ${isUser ? 'user-message' : 'bot-message'}`;
        msgDiv.innerHTML = text.replace(/\n/g, '<br>');
        chatMessages.appendChild(msgDiv);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    function addTypingIndicator() {
        const ind = document.createElement('div');
        ind.className = 'typing-indicator';
        ind.id = 'typing-indicator';
        ind.innerHTML = '<div class="typing-dot"></div><div class="typing-dot"></div><div class="typing-dot"></div>';
        chatMessages.appendChild(ind);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    function removeTypingIndicator() {
        const ind = document.getElementById('typing-indicator');
        if (ind) ind.remove();
    }

    // Models with confirmed free-tier quota on this API key (in priority order)
    const FALLBACK_MODELS = [
        "gemini-2.5-flash",  // 5 RPM  ✅
        "gemini-3.5-flash",  // 5 RPM  ✅
        "gemma-4-31b-it"     // 15 RPM ✅ (last resort - instruction-tuned Gemma 4)
    ];

    async function callGeminiAPI(history, modelFallbackIndex = 0) {
        const apiKey  = "AIzaSyCGN_AIywl9DJUhIVUEMImWbuLU3syozdk";
        const model   = FALLBACK_MODELS[modelFallbackIndex];
        const response = await fetch(
            `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`,
            { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ contents: history }) }
        );
        const data = await response.json();

        if (data.error) {
            const msg  = (data.error.message || '').toLowerCase();
            const code = data.error.code;
            // Trigger fallback on: 429 quota/rate limit, 503 overload, "high demand"
            const shouldFallback = code === 429 || code === 503 ||
                                   msg.includes('quota') || msg.includes('rate') ||
                                   msg.includes('high demand') || msg.includes('resource_exhausted');

            console.warn(`[Chatbot] Model "${model}" failed (${code}): ${data.error.message}`);

            if (shouldFallback && modelFallbackIndex < FALLBACK_MODELS.length - 1) {
                console.log(`[Chatbot] Falling back to: ${FALLBACK_MODELS[modelFallbackIndex + 1]}`);
                return callGeminiAPI(history, modelFallbackIndex + 1);
            }

            // Extract retry-after seconds if present (e.g. "Please retry in 22.15s")
            const retryMatch = data.error.message.match(/retry in ([\d.]+)s/i);
            const retryAfter = retryMatch ? Math.ceil(parseFloat(retryMatch[1])) : null;

            const err = new Error(data.error.message);
            err.retryAfter = retryAfter;
            err.isQuota = shouldFallback;
            throw err;
        }
        return data;
    }

    async function handleChatSend() {
        const text = chatInput.value.trim();
        if (!text) return;

        addMessage(text, true);
        chatInput.value = '';
        sendChatBtn.disabled = true;
        chatHistory.push({ role: "user", parts: [{ text }] });

        addTypingIndicator();
        setRobotState('thinking'); // 🤔 robot thinks

        try {
            const data = await callGeminiAPI(chatHistory);
            removeTypingIndicator();
            sendChatBtn.disabled = false;

            if (data.candidates && data.candidates[0].content.parts[0].text) {
                const botReply = data.candidates[0].content.parts[0].text;
                addMessage(botReply, false);
                chatHistory.push({ role: "model", parts: [{ text: botReply }] });

                setRobotState('happy'); // 🎉 robot is happy
                setTimeout(() => setRobotState(null), 2000); // back to idle
            } else if (data.error) {
                addMessage("API Error: " + data.error.message, false);
                setRobotState(null);
            } else {
                addMessage("I'm sorry, I couldn't process that response.", false);
                setRobotState(null);
            }
        } catch (error) {
            console.error("Chat API error:", error);
            removeTypingIndicator();
            sendChatBtn.disabled = false;
            setRobotState(null);

            const errDiv = document.createElement('div');
            errDiv.className = 'message bot-message';

            if (error.isQuota && error.retryAfter) {
                // Show live countdown + auto-retry
                let remaining = error.retryAfter;
                errDiv.innerHTML = `⏱️ All models are rate-limited. Auto-retrying in <b id="countdown">${remaining}s</b>…`;
                chatMessages.appendChild(errDiv);
                chatMessages.scrollTop = chatMessages.scrollHeight;

                const timer = setInterval(() => {
                    remaining--;
                    const cd = document.getElementById('countdown');
                    if (cd) cd.textContent = `${remaining}s`;
                    if (remaining <= 0) {
                        clearInterval(timer);
                        errDiv.remove();
                        chatHistory.pop();
                        chatInput.value = text;
                        handleChatSend();
                    }
                }, 1000);

            } else if (error.isQuota) {
                // Quota but no timer — show manual retry
                errDiv.innerHTML = `⚠️ Rate limit hit on all models. <button class="retry-btn" id="retry-btn">Retry in a moment</button>`;
                chatMessages.appendChild(errDiv);
                chatMessages.scrollTop = chatMessages.scrollHeight;
                document.getElementById('retry-btn').addEventListener('click', () => {
                    errDiv.remove();
                    chatHistory.pop();
                    chatInput.value = text;
                    handleChatSend();
                });
            } else {
                errDiv.textContent = "Error: " + error.message;
                chatMessages.appendChild(errDiv);
                chatMessages.scrollTop = chatMessages.scrollHeight;
            }
        }
    }

    sendChatBtn.addEventListener('click', handleChatSend);
    chatInput.addEventListener('keypress', (e) => { if (e.key === 'Enter') handleChatSend(); });
});
