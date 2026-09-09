import os
DEFAULT_PERSONA = "수면 전문가"
DEFAULT_CBTI_WEEK = 1
DEFAULT_TTS_ENABLED = False
DEFAULT_EMOTION = "Neutral"
WELCOME_MESSAGE_TEMPLATE = "{persona}가 상담을 시작합니다!"


# ✅ 페르소나 이미지: 배포시엔 static 폴더 또는 CDN/클라우드 URL 권장
PERSONA_IMAGES = {
   "수면 전문가": "images/전문가_상담가.png",  # 또는 CDN, github url 등
   "감성 친구": "images/감정_공감_친구.png",
   "CBT-I 코치": "images/CBT-I 8주 프로그램 코치.png"
}

# ✅ 페르소나 시스템 프롬프트: 반드시 '수면상담/CBT-I' 콘셉트에 집중!
PERSONA_PROMPTS = {
    "수면 전문가": """
        당신은 [수면 전문가]입니다.
        수면의학과 심리상담을 전문으로 하며, 불면증, 수면 위생, 수면장애에 대해 과학적·근거 기반의 조언을 제공합니다.
        전문적이지만 따뜻한 어조로, 사용자의 고민을 정확히 듣고 명확하게 안내합니다.
    """,
    "감성 친구": """
        당신은 [감성 친구]입니다.
        사용자의 이야기를 따뜻하게 들어주고, 감정에 깊이 공감하는 편안한 친구입니다.
        전문 정보보다는 마음의 위로, 이해, 격려를 중심으로 답변하세요.
    """,
    "CBT-I 코치": """
        당신은 [CBT-I 코치]입니다.
        인지행동치료(CBT-I) 기반의 8주 수면 개선 프로그램을 진행합니다.
        각 주차별 실습·과제 안내, 구체적 피드백, 단계적 행동 변화 유도, 긍정적 격려를 중심으로 안내하세요.
    """
}

# ✅ 감정 분류
EMOTIONS = {
    'POSITIVE': ['Happy', 'Neutral'],
    'NEGATIVE': ['Anger', 'Disgust', 'Fear', 'Sad']
}

EMOTION_COLORS = {
    'Happy': '#90EE90',    # Light green
    'Neutral': '#FFD700',  # Gold
    'Sad': '#ADD8E6',      # Light blue
    'Anger': '#FF6347',    # Tomato
    'Fear': '#DDA0DD',     # Plum
    'Disgust': '#F0E68C'   # Khaki
}

EMOTION_MAPPING = {
    0: "Anger",
    1: "Disgust", 
    2: "Fear",
    3: "Happy",
    4: "Neutral",
    5: "Sad"
}

# ✅ 기본값
DEFAULT_PERSONA = "수면 전문가"
DEFAULT_EMOTION = "Neutral"

WELCOME_MESSAGE_TEMPLATE = "안녕하세요, {persona}입니다. 편안한 수면 상담을 시작해볼까요? 😴"

# ✅ 페르소나 URL 매핑 (페이지 이동, 세션 경로 등에 사용)
PERSONA_URL_MAPPING = {
    "수면 전문가": "sleep_expert",
    "감성 친구": "empathic_friend",
    "CBT-I 코치": "cbti_coach"
}
PERSONA_NAME_MAPPING = {v: k for k, v in PERSONA_URL_MAPPING.items()}

# ✅ CBT-I 8주차 프롬프트 경로 예시 (만약 파일로 분리했다면 사용)
CBTI_WEEK_PROMPTS_PATH = os.path.join("src", "core", "services", "cbti_week_prompts.py")
