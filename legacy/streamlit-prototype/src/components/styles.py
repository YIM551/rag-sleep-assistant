import streamlit as st

def apply_custom_styles():
    """수면상담 RAG 챗봇용 커스텀 스타일 적용"""
    st.markdown("""
        <style>
        /* 전체 앱 배경 */
        .stApp {
            background-color: #1E1E1E !important;
        }
        /* 사이드바 */
        section[data-testid="stSidebar"] {
            background: #232946 !important;
        }
        /* 사이드바 텍스트 */
        .sidebar-content {
            color: #fff !important;
        }
        /* 입력창 */
        .stTextInput input {
            background-color: #2D2D2D !important;
            color: #fff !important;
            border: none !important;
            border-radius: 0.5rem !important;
            padding: 0.8rem !important;
        }
        /* 버튼 */
        .stButton button {
            background-color: #6C63FF !important;
            color: white !important;
            border: none !important;
            border-radius: 0.5rem !important;
            padding: 0.8rem 1.5rem !important;
            font-weight: 600;
        }
        .stButton button:hover {
            background-color: #574B90 !important;
            color: #fff !important;
        }
        /* 채팅 컨테이너 */
        .main-container {
            max-width: 800px;
            margin: 0 auto;
            padding: 2rem;
            margin-bottom: 100px;
        }
        /* 스크롤바 */
        ::-webkit-scrollbar {
            width: 8px;
            height: 8px;
        }
        ::-webkit-scrollbar-track {
            background: #1E1E1E;
        }
        ::-webkit-scrollbar-thumb {
            background: #4A4A4A;
            border-radius: 4px;
        }
        ::-webkit-scrollbar-thumb:hover {
            background: #6C63FF;
        }
        /* 참고문서 Expander */
        .streamlit-expanderHeader {
            font-weight: 600;
            color: #574B90 !important;
            background: #f6f2ff !important;
            border-radius: 8px 8px 0 0 !important;
            padding: 10px !important;
        }
        /* 체크박스 등 기타 UI */
        .stCheckbox > label, .stRadio > label {
            color: #fff !important;
        }
        /* 음성파일 업로드 안내 */
        .stFileUploader label {
            color: #BFC4E1 !important;
        }
        /* CBT-I 안내문 강조 */
        .cbti-guide {
            background: #f6f2ff;
            color: #574B90;
            padding: 12px 16px;
            border-radius: 8px;
            font-size: 1rem;
            margin: 10px 0;
        }
        /* 감정 라벨 */
        .emotion-label {
            padding: 4px 12px;
            border-radius: 12px;
            font-weight: 700;
            font-size: 0.9rem;
            display: inline-block;
            margin-left: 4px;
        }
        </style>
    """, unsafe_allow_html=True)
