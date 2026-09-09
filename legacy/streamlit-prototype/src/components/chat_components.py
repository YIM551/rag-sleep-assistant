import streamlit as st
from src.app.constants import EMOTION_COLORS, PERSONA_IMAGES
from src.core.services.cbti_week_prompts import CBTI_WEEK_PROMPTS  # 주차별 안내문 import

def render_emotion_indicator(emotion: str):
    """감정 상태 표시 컴포넌트"""
    emotion_color = EMOTION_COLORS.get(emotion, EMOTION_COLORS['Neutral'])
    return st.markdown(f"""
        <div style="
            display: flex;
            align-items: center;
            gap: 8px;
            margin-top: 16px;
        ">
            <span style="
                background-color: {emotion_color};
                color: white;
                padding: 4px 12px;
                border-radius: 12px;
                font-weight: 600;
            ">{emotion}</span>
        </div>
    """, unsafe_allow_html=True)

def render_conversation_stats(stats: dict):
    """대화 통계 표시 컴포넌트"""
    st.markdown("### 대화 통계")
    st.write(f"- 총 대화 수: {stats.get('total', 0)}")
    st.write(f"- 긍정적 감정: {stats.get('positive', 0)}")
    st.write(f"- 부정적 감정: {stats.get('negative', 0)}")

def render_cbti_guide(cbti_week: int):
    """CBTI 8주차별 안내문 표시"""
    guide = CBTI_WEEK_PROMPTS.get(cbti_week)
    if guide:
        st.markdown(f"""
        <div style="margin-top: 12px; margin-bottom: 12px; padding: 12px; border-radius: 10px; background: #f1f3f6;">
            <b>CBT-I {cbti_week}주차 안내</b><br>
            <span style="font-size: 1em;">{guide}</span>
        </div>
        """, unsafe_allow_html=True)

def display_message(
    message: str,
    is_user: bool,
    persona_name: str = None,
    audio_bytes: bytes = None,    # TTS 음성파일 내용 (옵션)
    reference_docs: list = None   # 참고 문서(옵션)
):
    if is_user:
        st.chat_message("user").write(message)
    else:
        with st.chat_message("assistant", avatar=PERSONA_IMAGES.get(persona_name)):
            st.write(message)
            # === TTS 음성 출력 ===
            if audio_bytes:
                st.audio(audio_bytes, format="audio/mp3")
            # === 참고 문서 ===
            if reference_docs:
                st.markdown("##### 참고 문서:")
                for i, doc in enumerate(reference_docs):
                    st.markdown(f"- {doc['content'][:120]}{'...' if len(doc['content'])>120 else ''}")
