import streamlit as st
from src.app.constants import PERSONA_IMAGES

def get_emotion_color(emotion: str) -> str:
    emotion_colors = {
        'Happy': '#90EE90',  # Light green
        'Neutral': '#FFD700',  # Gold
        'Sad': '#ADD8E6',  # Light blue
        'Anger': '#FF6347',  # Tomato
        'Fear': '#DDA0DD',  # Plum
        'Disgust': '#F0E68C'  # Khaki
    }
    return emotion_colors.get(emotion, '#FFD700')

def display_message(
    message: dict, 
    persona: str = "default",
    audio_bytes: bytes = None,
    cbti_guide: str = None
):
    """Display chat message with persona-based styling"""
    try:
        content = message.get('content', '')
        if isinstance(content, tuple):
            content = content[0]
        timestamp = message.get('timestamp', '')
        emotion = message.get('emotion', '')
        reference_docs = message.get('reference_docs', [])

        if message.get('role') == 'assistant':
            persona_image = PERSONA_IMAGES.get(persona)
            with st.container():
                st.markdown(f"""
                    <div style="display: flex; align-items: flex-start; margin: 16px 0; gap: 8px;">
                        <img src="{persona_image}" style="
                            width: 40px;
                            height: 40px;
                            border-radius: 50%;
                            object-fit: cover;
                        "/>
                        <div style="
                            background-color: #F0F0F0;
                            color: black;
                            padding: 12px 18px;
                            border-radius: 18px;
                            border-top-left-radius: 4px;
                            max-width: 80%;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        ">
                            <div style="font-size: 1rem; line-height: 1.4;">{content}</div>
                            <div style="font-size: 0.75rem; color: #666; margin-top: 6px;">{timestamp}</div>
                        </div>
                    </div>
                """, unsafe_allow_html=True)
                # TTS 음성 출력
                if audio_bytes:
                    st.audio(audio_bytes, format="audio/mp3")
                # CBTI 안내문 출력
                if cbti_guide:
                    st.markdown(f"""
                        <div style="margin: 8px 0 16px 54px; background: #f8f9fa; padding: 12px 16px; border-radius: 12px; color: #574B90;">
                            <b>CBT-I 8주차 안내</b><br>{cbti_guide}
                        </div>
                    """, unsafe_allow_html=True)
                # 참고 문서
                if reference_docs:
                    with st.expander("🔍 참고한 문서"):
                        for idx, doc in enumerate(reference_docs, 1):
                            disease = doc['metadata'].get('disease', '')
                            tab = doc['metadata'].get('tab', '')
                            doc_content = doc.get('content', '').strip()
                            st.markdown(f"""
                                <div style='
                                    background-color: #f8f9fa;
                                    padding: 1rem;
                                    border-radius: 0.5rem;
                                    margin-bottom: 0.5rem;
                                '>
                                    <div style='
                                        color: #1a73e8;
                                        font-weight: 600;
                                        margin-bottom: 0.5rem;
                                    '>
                                        {disease} - {tab}
                                    </div>
                                    <div style='
                                        font-size: 0.9rem;
                                        color: #202124;
                                    '>
                                        {doc_content}
                                    </div>
                                </div>
                            """, unsafe_allow_html=True)

        # 사용자 메시지
        else:
            background = get_emotion_color(emotion)
            with st.container():
                st.markdown(f"""
                    <div style="display: flex; justify-content: flex-end; margin: 16px 0;">
                        <div style="
                            background: {background};
                            color: black;
                            padding: 12px 18px;
                            border-radius: 18px;
                            border-top-right-radius: 4px;
                            max-width: 80%;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        ">
                            <div style="font-size: 1rem; line-height: 1.4;">{content}</div>
                            <div style="
                                display: flex;
                                justify-content: flex-end;
                                align-items: center;
                                gap: 8px;
                                margin-top: 6px;
                            ">
                                <span style="
                                    font-size: 0.75rem;
                                    background-color: rgba(0,0,0,0.1);
                                    padding: 2px 8px;
                                    border-radius: 12px;
                                    font-weight: 500;
                                ">{emotion}</span>
                                <span style="font-size: 0.75rem; color: #333;">{timestamp}</span>
                            </div>
                        </div>
                    </div>
                """, unsafe_allow_html=True)
            
    except Exception as e:
        st.error(f"메시지 표시 중 오류 발생: {str(e)}")

def apply_chat_styles():
    st.markdown("""
        <style>
        .stChatMessage .stChatMessageAvatar {
            display: none !important;
        }
        .stChatMessage { padding-left: 0 !important; padding-right: 0 !important; }
        .stTextInput input {
            background-color: #2D2D2D;
            color: white;
            border: none;
            border-radius: 0.5rem;
            padding: 0.8rem;
        }
        .stButton button {
            background-color: #007AFF;
            color: white;
            border: none;
            border-radius: 0.5rem;
            padding: 0.8rem 1.5rem;
        }
        </style>
    """, unsafe_allow_html=True)

def get_emotion_class(emotion: str) -> str:
    positive_emotions = {'joy', 'love', 'surprise'}
    negative_emotions = {'anger', 'sadness', 'fear'}
    if emotion in positive_emotions:
        return 'positive'
    elif emotion in negative_emotions:
        return 'negative'
    return 'neutral'
