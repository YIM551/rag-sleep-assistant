from dataclasses import dataclass
from dotenv import load_dotenv
import os

# .env 파일 자동 로드
load_dotenv()

@dataclass
class OpenAIConfig:
    api_key: str = os.getenv("OPENAI_API_KEY")
    chat_model: str = os.getenv("OPENAI_MODEL", "gpt-4")
    temperature: float = float(os.getenv("OPENAI_TEMPERATURE", 0.7))

@dataclass
class PineconeConfig:
    api_key: str = os.getenv("PINECONE_API_KEY")
    environment: str = os.getenv("PINECONE_ENVIRONMENT")
    index_name: str = os.getenv("PINECONE_INDEX_NAME")

@dataclass
class TTSConfig:
    tts_engine: str = os.getenv("TTS_ENGINE", "gtts")  # gtts, coqui, etc.
    tts_lang: str = os.getenv("TTS_LANG", "ko")
    # 필요한 경우 TTS 관련 API 키 등 추가 가능

# 필요하면 다른 서비스별 Config도 추가

# 사용 예시
# openai_cfg = OpenAIConfig()
# pinecone_cfg = PineconeConfig()
# tts_cfg = TTSConfig()
