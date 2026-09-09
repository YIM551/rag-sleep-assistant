import streamlit as st
from functools import wraps
from typing import Callable, Any, Optional
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def handle_streamlit_errors(func: Callable) -> Callable:
    """Streamlit 에러 처리 데코레이터
    - 스트림릿 기반의 함수에서 예외 발생시 UI에 사용자 친화적 에러 메시지를 출력합니다.
    - RAG, TTS, Whisper 등 어떤 모듈에도 범용으로 사용 가능!
    """
    @wraps(func)
    def wrapper(*args, **kwargs) -> Any:
        try:
            return func(*args, **kwargs)
        except Exception as e:
            logger.error(f"Error in {func.__name__}: {str(e)}", exc_info=True)
            st.error(f"오류가 발생했습니다: {str(e)}")
            st.error("페이지를 새로고침하거나 다시 시도해주세요.")
            return None
    return wrapper

def safe_get_session_state(key: str, default: Any = None) -> Any:
    """세션 상태 안전하게 가져오기
    - 감정분석, 페르소나, CBT-I 주차, TTS On/Off 등 여러 상태값에 활용
    """
    try:
        return st.session_state[key]
    except KeyError:
        logger.warning(f"Session state key '{key}' not found")
        return default
