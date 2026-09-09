import streamlit as st
from src.utils.state_management import clear_session_state, initialize_session_state
from src.app.constants import PERSONA_IMAGES, PERSONA_URL_MAPPING

# 페르소나 정보
PERSONAS = [
    {
        "name": "수면 전문가", 
        "img": PERSONA_IMAGES["수면 전문가"],
        "desc": "수면의학 전문 상담가",
        "explanation": "불면증, 수면장애 등 수면 문제에 대해 과학적이고 근거 기반으로 상담합니다. 따뜻하고 신뢰할 수 있는 조언을 드립니다."
    },
    {
        "name": "감성 친구", 
        "img": PERSONA_IMAGES["감성 친구"],
        "desc": "공감 · 위로 중심의 친구",
        "explanation": "힘든 이야기를 따뜻하게 들어주고, 감정에 공감하며 마음의 위로와 용기를 전해주는 든든한 친구 역할을 합니다."
    },
    {
        "name": "CBT-I 코치", 
        "img": PERSONA_IMAGES["CBT-I 코치"],
        "desc": "8주 인지행동치료 전문 코치",
        "explanation": "8주 CBT-I(불면증 인지행동치료) 단계별 안내와 피드백, 실전 행동 실습까지 책임지는 코치입니다. 꾸준한 실천을 돕습니다."
    },
]

def get_page_styles() -> str:
    return """
        <style>
        .main-header, .persona-container {
            max-width: 1100px;    /* 전체 본문 폭 통일 */
            margin: 0 auto;       /* 중앙 정렬 */
        }
        .main-header {
            text-align: center;
            padding: 2rem 0;
            background: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 100%);
            margin: -1rem auto 2rem auto;
            color: white;
        }
        .main-header h1 {
            font-size: 2.5rem;
            font-weight: 600;
            margin-bottom: 1rem;
            background: linear-gradient(120deg, #b3d9ff, #80bfff);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }
        .main-header p {
            font-size: 1.2rem;
            color: rgba(255, 255, 255, 0.8);
            max-width: 600px;
            margin: 0 auto;
        }
        .persona-container {
            display: flex;
            flex-direction: row;
            justify-content: space-between;   /* 카드가 양 끝 맞닿게! */
            gap: 2rem;                       /* 카드 사이 간격 */
            padding: 0;                      /* 불필요한 좌우 패딩 제거 */
            margin-top: 2rem;
            flex-wrap: nowrap;
            overflow-x: auto;
        }
        .persona-card {
            background: #2d2d2d;
            border-radius: 12px;
            padding: 1.5rem;
            transition: all 0.3s ease;
            border: 1px solid rgba(255, 255, 255, 0.1);
            min-width: 300px;
            max-width: 300px;
            height: 430px;
            display: flex;
            flex-direction: column;
            cursor: pointer;
        }
        .persona-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 8px 16px rgba(0,0,0,0.2);
            border-color: rgba(255,255,255,0.2);
        }
        .persona-content {
            flex-grow: 1;
            margin-bottom: 1rem;
        }
        .persona-explanation {
            font-size: 0.95rem;
            color: rgba(255,255,255,0.7);
            margin: 0.8rem 0;
            line-height: 1.6rem;
            overflow: hidden;
            display: -webkit-box;
            -webkit-line-clamp: 6;
            -webkit-box-orient: vertical;
        }
        .persona-image {
            width: 100%;
            height: 180px;
            border-radius: 8px;
            margin-bottom: 1.5rem;
            object-fit: cover;
            object-position: center;
        }
        .persona-title {
            font-size: 1.2rem;
            font-weight: 600;
            color: white;
            margin-bottom: 1rem;
            height: 2.2rem;
            line-height: 2.2rem;
        }
        .persona-desc {
            font-size: 1rem;
            color: rgba(255,255,255,0.8);
            margin-bottom: 1.2rem;
            height: 2.2rem;
            line-height: 1.2rem;
        }
        /* 모바일 대응 */
        @media (max-width: 900px) {
            .main-header, .persona-container {
                max-width: 98vw;
            }
            .persona-container {
                flex-direction: column;
                align-items: center;
                gap: 2rem;
            }
        }
        </style>
    """

def render_home():
    """홈페이지 렌더링"""
    # 헤더
    st.markdown("""
        <div class="main-header">
            <h1>AI 수면/감정 상담 챗봇</h1>
            <p>상담을 도와줄 캐릭터(페르소나)를 선택하세요</p>
        </div>
    """, unsafe_allow_html=True)

    # 스타일 적용
    st.markdown(get_page_styles(), unsafe_allow_html=True)

    # 카드 리스트 (HTML로 직접 렌더)
    card_html = '<div class="persona-container">'
    for persona in PERSONAS:
        card_html += f"""
        <a href="?page=chat&persona={PERSONA_URL_MAPPING[persona['name']]}" style="text-decoration: none;">
            <div class="persona-card">
                <div class="persona-content">
                    <img src="{persona['img']}" class="persona-image" alt="{persona['name']}"/>
                    <div class="persona-title">{persona['name']}</div>
                    <div class="persona-desc">{persona['desc']}</div>
                    <div class="persona-explanation">{persona['explanation']}</div>
                </div>
            </div>
        </a>
        """
    card_html += '</div>'
    st.markdown(card_html, unsafe_allow_html=True)

# 페이지 실행 시 (예시)
if __name__ == "__main__":
    render_home()
