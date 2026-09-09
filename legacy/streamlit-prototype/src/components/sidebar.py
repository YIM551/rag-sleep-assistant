import streamlit as st
from src.core.services.cbti_week_prompts import CBTI_WEEK_PROMPTS  # 주차 안내문 사전
# from src.app.constants import ... (추가 상수 필요한 경우 import)

def render_sidebar():
    """사이드바 렌더링"""
    with st.sidebar:
        st.markdown("""
            <h1 style='color: white; margin-bottom: 2rem;'>수면상담 챗봇 💤</h1>
        """, unsafe_allow_html=True)
        
        st.markdown("### 사용 방법")
        st.markdown("""
            1. 채팅창에 수면 고민이나 일상을 입력하세요.<br>
            2. [선택] 음성 파일(WAV)로 상담도 가능합니다.<br>
            3. 챗봇이 고민/감정/상황에 맞는 맞춤 상담을 제공합니다.<br>
            4. CBT-I 8주 프로그램 단계별 실천 안내도 받을 수 있습니다.
        """, unsafe_allow_html=True)

        # 현재 페르소나 표시
        persona = st.session_state.get('selected_persona')
        if persona:
            st.markdown(f"### 👤 현재 상담 페르소나\n- {persona}")

        # 현재 감정 상태
        emotion = st.session_state.get('current_emotion')
        if emotion:
            st.markdown(f"### 감정 분석 결과\n- **{emotion}**")

        # CBT-I 8주차 선택 및 안내
        st.markdown("---")
        st.markdown("### 🗓️ CBT-I 8주차 안내")
        selected_week = st.selectbox(
            "진행 중인 주차를 선택하세요",
            options=list(range(1, 9)),
            index=0,
            format_func=lambda x: f"{x}주차"
        )
        cbti_guide = CBTI_WEEK_PROMPTS.get(selected_week)
        if cbti_guide:
            st.markdown(f"""
                <div style="background:#f6f2ff; color:#574B90; padding:12px; border-radius:8px; margin-bottom:12px;">
                    <b>CBT-I {selected_week}주차 핵심 안내</b><br>
                    {cbti_guide}
                </div>
            """, unsafe_allow_html=True)
        
        # TTS(음성 출력) 옵션
        tts_on = st.checkbox("챗봇 답변 음성 출력 (TTS)", value=True)
        st.session_state["tts_on"] = tts_on

        st.markdown("---")

        # 대화 통계
        st.markdown("### 📊 대화 통계")
        stats = st.session_state.get('conversation_stats', {})
        st.write(f"총 대화 수: {stats.get('total', 0)}")
        st.write(f"긍정적 감정: {stats.get('positive', 0)}")
        st.write(f"부정적 감정: {stats.get('negative', 0)}")
        
        # 참고 문서
        st.markdown("### 📚 참고 문서")
        latest_docs = None
        for msg in reversed(st.session_state.get('messages', [])):
            if msg.get('role') == 'assistant' and msg.get('reference_docs'):
                latest_docs = msg['reference_docs']
                break
        if latest_docs:
            for doc in latest_docs:
                disease = doc['metadata'].get('disease', '')
                tab = doc['metadata'].get('tab', '')
                content = doc.get('content', '').strip()
                with st.expander(f"📑 {disease} - {tab}"):
                    st.markdown(f"""
                        <div style='background:#2d2d2d; color:white; padding:1rem; border-radius:0.5rem; margin-bottom:0.5rem;'>
                            {content}
                        </div>
                    """, unsafe_allow_html=True)
        else:
            st.info("아직 참고한 문서가 없습니다.")
        
        st.markdown("---")

        # 음성 파일 업로드
        st.markdown("### 🎤 음성 파일 업로드 (WAV 지원)")
        uploaded_file = st.file_uploader(
            "Drag and drop file here",
            type=['wav'],
            help="200MB 이하 파일만 업로드 가능"
        )
        # 업로드 후 처리(예: 자동 분석) 로직은 main app에서 처리

