import streamlit as st
from datetime import datetime
from src.components.message_display import display_message

def render_chat_interface():
    # 보라색 테마 CSS (채팅, 입력창, 버튼, 말풍선 등)
    st.markdown("""
        <style>
        /* 채팅 전체 배경 */
        .main, .stApp {
            background: linear-gradient(135deg, #7749F8 0%, #9B59B6 100%) !important;
        }

        /* 채팅 컨테이너 */
        .chat-container {
            display: flex;
            flex-direction: column;
            height: calc(100vh - 80px);
            margin: -1rem;
            padding: 1rem;
            overflow: hidden;
        }
        /* 메시지 영역 */
        .message-container {
            flex-grow: 1;
            overflow-y: auto;
            padding: 1rem;
            margin-bottom: 80px;
        }
        /* 입력 영역 */
        .input-container {
            position: fixed;
            bottom: 0;
            left: 350px;
            right: 0;
            background: #7E4AFF;
            padding: 1rem;
            border-top: 2px solid #A483FF;
            z-index: 1000;
            box-shadow: 0 0 18px #bca7fc60;
        }
        /* 입력창 스타일 */
        .stTextInput input {
            background: #F1EBFB;
            color: #3E2067;
            border: 1.5px solid #B79BFF;
            border-radius: 16px;
            padding: 12px 18px;
            font-size: 1.1em;
        }
        /* 전송 버튼 */
        .stButton button {
            background: linear-gradient(90deg, #9B59B6 30%, #7749F8 100%);
            color: #fff;
            border-radius: 20px;
            border: none;
            font-weight: bold;
            padding: 8px 24px;
            box-shadow: 0 3px 12px #bca7fc50;
            transition: background 0.2s;
        }
        .stButton button:hover {
            background: linear-gradient(90deg, #a084ee 0%, #bca7fc 100%);
            color: #3E2067;
        }
        /* 유저 말풍선 */
        .user-bubble {
            background: #F6F0FF;
            color: #7749F8;
            border-radius: 18px 18px 0 18px;
            margin: 12px 0 12px auto;
            max-width: 75%;
            padding: 12px 20px;
            font-size: 1.09em;
            box-shadow: 0 2px 8px #bca7fc25;
        }
        /* 어시스턴트 말풍선 */
        .assistant-bubble {
            background: #E6DDFF;
            color: #3E2067;
            border-radius: 18px 18px 18px 0;
            margin: 12px auto 12px 0;
            max-width: 75%;
            padding: 12px 20px;
            font-size: 1.09em;
            box-shadow: 0 2px 8px #bca7fc25;
        }
        </style>

        <div class="chat-container">
            <div class="message-container">
    """, unsafe_allow_html=True)

    # 메시지 표시
    if 'messages' in st.session_state:
        for msg in st.session_state.messages:
            # 채팅 스타일: role에 따라 분기
            if msg.get("role") == "user":
                st.markdown(
                    f'<div class="user-bubble">{msg["content"]}</div>', unsafe_allow_html=True)
            else:
                st.markdown(
                    f'<div class="assistant-bubble">{msg["content"]}</div>', unsafe_allow_html=True)
                # 만약 음성(TTS)나 참고 문서, 이미지 등 추가 출력 시 여기에 구현

    st.markdown('</div>', unsafe_allow_html=True)

    # 입력 영역
    st.markdown('<div class="input-container">', unsafe_allow_html=True)
    with st.form(key="chat_form", clear_on_submit=True):
        cols = st.columns([6, 1])
        with cols[0]:
            user_input = st.text_input(
                "",
                placeholder="메시지를 입력하세요...",
                label_visibility="collapsed"
            )
        with cols[1]:
            submit = st.form_submit_button("전송")
    st.markdown('</div>', unsafe_allow_html=True)

    if submit and user_input and user_input.strip():
        chatbot = st.session_state.chatbot_service
        emotions = chatbot.analyze_emotion(user_input)
        dominant_emotion = max(emotions.items(), key=lambda x: x[1])[0]
        response = chatbot.get_response(user_input)

        current_time = datetime.now().strftime('%p %I:%M')

        if 'messages' not in st.session_state:
            st.session_state.messages = []

        st.session_state.messages.extend([
            {
                "role": "user",
                "content": user_input,
                "emotion": dominant_emotion,
                "timestamp": current_time
            },
            {
                "role": "assistant",
                "content": response,
                "timestamp": current_time
            }
        ])

        st.rerun()
