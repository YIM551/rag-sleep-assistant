import streamlit as st
from src.utils.state_management import initialize_session_state, clear_session_state, ensure_state_initialization
from src.utils.error_handling import handle_streamlit_errors
from src.components.message_display import apply_chat_styles, display_message
from src.components.chat_components import render_emotion_indicator, render_conversation_stats
from src.app.constants import (
    DEFAULT_PERSONA, 
    DEFAULT_EMOTION, 
    EMOTIONS,
    PERSONA_NAME_MAPPING
)
from src.core.services.personas import PERSONAS
from src.core.services.chatbot_service import ChatbotService
from src.app.config import OpenAIConfig
from src.utils.audio_handler import process_recorded_audio
from src.utils.audio_handler import synthesize_speech

from datetime import datetime
import time

# ... (함수 정의 부분 동일)

def main():
    st.set_page_config(
        page_title="수면상담 챗봇 (음성+CBT-I+RAG)",
        page_icon="💤",
        layout="wide",
        initial_sidebar_state="expanded"
    )
    current_page = st.query_params.get("page", "home")
    if current_page == "chat":
        render_chat_page()
    else:
        from src.app.home import render_home
        render_home()

if __name__ == "__main__":
    main()
